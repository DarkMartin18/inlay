package com.example.gridime

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Shared look for the Settings app: black, calm, one white focus ring. */
object SettingsStyle {
    val background: Int = Design.SURFACE_BG
    val surface: Int = Design.SURFACE_CARD
    val surfaceFocused: Int = Design.SURFACE_RAISED
    val track: Int = Color.parseColor("#2E2E35")
    val textPrimary: Int = Design.TEXT_PRIMARY
    val textSecondary: Int = Design.TEXT_SECONDARY
    val divider: Int = Design.EDGE_DIVIDER
    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    /**
     * True on narrow screens (portrait, small handhelds). Wide controls then sit under
     * their title instead of beside it, so nothing gets squeezed.
     */
    fun isNarrow(context: Context): Boolean = context.resources.configuration.screenWidthDp < 600

    private val argb = ArgbEvaluator()
    fun blend(from: Int, to: Int, t: Float): Int = argb.evaluate(t.coerceIn(0f, 1f), from, to) as Int

    /** Rounded background: filled + white ring when focused, soft fill when pressed. */
    fun focusBackground(context: Context, radiusDp: Float): Drawable {
        val dp = context.resources.displayMetrics.density
        fun shape(fill: Int, ring: Boolean) = GradientDrawable().apply {
            cornerRadius = radiusDp * dp
            setColor(fill)
            if (ring) setStroke((Design.STROKE_FOCUS * dp).roundToInt(), Color.WHITE)
        }
        return StateListDrawable().apply {
            setEnterFadeDuration(Design.MOTION_FADE.toInt())
            setExitFadeDuration(Design.MOTION_CONTROL.toInt())
            addState(intArrayOf(android.R.attr.state_focused), shape(surfaceFocused, ring = true))
            addState(intArrayOf(android.R.attr.state_pressed), shape(surfaceFocused, ring = false))
            addState(intArrayOf(), shape(Color.TRANSPARENT, ring = false))
        }
    }
}

// =========================================================================
// ROWS: Up/Down moves between rows, Left/Right changes the value, A acts
// =========================================================================

/**
 * One settings row. [stacked] puts the control under the text instead of beside it,
 * for controls that need the full width.
 */
abstract class SettingRow(
    context: Context,
    title: String,
    subtitle: String?,
    stacked: Boolean = false
) : LinearLayout(context) {

    companion object {
        /** Button icons used in subtitles. Set by the Settings screen before building rows. */
        var buttonStyle: ButtonStyle = ButtonStyle.XBOX
        /** Sounds and vibration for changes made in Settings. */
        var sounds: Feedback? = null
    }

    /** Called when Left is pressed on a row that has nothing to change. */
    var onLeaveLeft: (() -> Unit)? = null

    protected val dp = resources.displayMetrics.density
    protected fun px(v: Float) = (v * dp).roundToInt()
    private val stackedLayout = stacked

    init {
        orientation = if (stacked) VERTICAL else HORIZONTAL
        gravity = if (stacked) Gravity.START else Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        // Android's own click sound would play on top of the keyboard's sound pack
        isSoundEffectsEnabled = false
        minimumHeight = px(64f)
        background = SettingsStyle.focusBackground(context, Design.RADIUS_ROW)
        setPadding(px(Design.SPACE_L), px(Design.SPACE_M), px(Design.SPACE_L), px(Design.SPACE_M))

        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(TextView(context).apply {
                text = title
                textSize = Design.TYPE_ROW
                typeface = SettingsStyle.medium
                setTextColor(SettingsStyle.textPrimary)
            })
            subtitle?.let {
                addView(subtitleView(context, it), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = px(Design.SPACE_XS / 2) })
            }
        }
        addView(texts, if (stacked) LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        else LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        setOnClickListener { activate() }
    }

    private fun subtitleView(context: Context, subtitle: String): View {
        val parts = HintText.parse(subtitle)
        return if (parts.none { it is HintPart.Badge }) {
            TextView(context).apply {
                text = subtitle
                textSize = Design.TYPE_CAPTION
                setTextColor(SettingsStyle.textSecondary)
                setLineSpacing(0f, 1.15f)
            }
        } else {
            BadgeLineView(context, parts, buttonStyle, Design.TYPE_CAPTION, SettingsStyle.textSecondary, badgeScale = 1.25f)
        }
    }

    protected fun setControl(view: View) {
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        if (stackedLayout) params.topMargin = px(Design.SPACE_M) else params.marginStart = px(Design.SPACE_L)
        addView(view, params)
    }

    /** Left (-1) or Right (+1). Returns true if this row used the press. */
    protected open fun step(dir: Int): Boolean = false

    /** A button or a tap. */
    protected open fun activate() = Unit

    /** False for rows that only show information: A does nothing there, so it makes no sound. */
    protected open val interactive: Boolean get() = true

    /** Redraw anything that looks different while focused. */
    protected open fun onFocusLook(focused: Boolean) = Unit

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!step(-1)) onLeaveLeft?.invoke()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                step(1)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.repeatCount == 0 && interactive) {
                    pressBounce()
                    activate()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        onFocusLook(gainFocus)
    }

    /** A small press-in and release, so every action feels physical. */
    protected fun pressBounce() {
        animate().cancel()
        scaleX = 0.985f
        scaleY = 0.985f
        animate().scaleX(1f).scaleY(1f).setDuration(Design.MOTION_PRESS).setInterpolator(Design.EASE_OUT).start()
    }
}

/** Pick one of a few options. Left/Right or A to change, or tap an option. */
class ChoiceRow(
    context: Context,
    title: String,
    subtitle: String?,
    options: List<String>,
    selected: Int,
    private val onChange: (Int) -> Unit
) : SettingRow(context, title, subtitle, stacked = options.size >= 3 || SettingsStyle.isNarrow(context)) {

    private val segments = SegmentedView(context, options, selected) {
        sounds?.key()
        onChange(it)
    }
    private val count = options.size

    init {
        setControl(segments)
    }

    override fun step(dir: Int): Boolean {
        choose((segments.selectedIndex + dir).coerceIn(0, count - 1))
        return true
    }

    override fun activate() = choose((segments.selectedIndex + 1) % count)

    private fun choose(index: Int) {
        if (index == segments.selectedIndex) return
        segments.select(index)
        sounds?.key()
        onChange(index)
    }
}

/** On/off switch. Left = off, Right = on, A = flip. */
class ToggleRow(
    context: Context,
    title: String,
    subtitle: String?,
    on: Boolean,
    private val onChange: (Boolean) -> Unit
) : SettingRow(context, title, subtitle) {

    private val switch = SwitchView(context, on)

    init {
        setControl(switch)
    }

    override fun step(dir: Int): Boolean {
        set(dir > 0)
        return true
    }

    override fun activate() = set(!switch.isOn)

    private fun set(on: Boolean) {
        if (on == switch.isOn) return
        switch.set(on)
        if (on) sounds?.on() else sounds?.off()
        onChange(on)
    }
}

/** A value on a scale. Left/Right to adjust, or drag. */
class SliderRow(
    context: Context,
    title: String,
    subtitle: String?,
    min: Int,
    max: Int,
    value: Int,
    private val label: (Int) -> String,
    private val silent: Boolean = false,       // true when onChange plays its own demo
    private val onChange: (Int) -> Unit
) : SettingRow(context, title, subtitle, stacked = SettingsStyle.isNarrow(context)) {

    private val valueText = TextView(context).apply {
        textSize = 15f
        typeface = SettingsStyle.medium
        setTextColor(SettingsStyle.textPrimary)
        gravity = Gravity.END
        text = label(value)
    }
    private val slider = SliderView(context, min, max, value) { update(it) }

    init {
        val control = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(slider)
            addView(valueText, LayoutParams(px(72f), LayoutParams.WRAP_CONTENT))
        }
        setControl(control)
    }

    override fun step(dir: Int): Boolean {
        val next = (slider.value + dir).coerceIn(slider.min, slider.max)
        if (next != slider.value) {
            slider.value = next
            if (!silent) sounds?.move()
            update(next)
        }
        return true
    }

    private fun update(v: Int) {
        valueText.text = label(v)
        onChange(v)
    }
}

/** Colour dots. Left/Right walks through them live; a ring marks the one in focus. */
class SwatchRow(
    context: Context,
    title: String,
    subtitle: String?,
    colours: List<Int>,
    selected: Int,
    private val onChange: (Int) -> Unit
) : SettingRow(context, title, subtitle, stacked = true) {

    private val grid = SwatchGridView(context, colours, selected) { onChange(it) }

    init {
        setControl(grid)
    }

    /** Clears the tick when a custom colour is chosen instead. */
    fun clearSelection() {
        grid.selectedIndex = -1
    }

    override fun step(dir: Int): Boolean {
        val current = grid.selectedIndex
        val next = if (current < 0) (if (dir > 0) 0 else grid.count - 1)
        else (current + dir).coerceIn(0, grid.count - 1)
        if (next != current) {
            grid.selectedIndex = next
            sounds?.move()
            onChange(next)
        }
        return true
    }

    override fun onFocusLook(focused: Boolean) {
        grid.showCursor = focused
    }
}

/** Does something when pressed. */
class ActionRow(
    context: Context,
    title: String,
    subtitle: String?,
    private val onPress: () -> Unit
) : SettingRow(context, title, subtitle) {
    init {
        setControl(TextView(context).apply {
            text = "›"
            textSize = 26f
            setTextColor(SettingsStyle.textSecondary)
        })
    }

    override fun activate() {
        sounds?.confirm()
        onPress()
    }
}

/**
 * Pick any colour: a rainbow track, Left/Right steps 10° around the colour wheel.
 * The thumb shows the colour itself.
 */
class HueRow(
    context: Context,
    title: String,
    subtitle: String?,
    hue: Int,
    active: Boolean,
    private val onChange: (Int) -> Unit
) : SettingRow(context, title, subtitle, stacked = SettingsStyle.isNarrow(context)) {

    private val hueSlider = HueSliderView(context, hue, active) { onChange(it) }

    init {
        setControl(hueSlider)
    }

    fun setActive(active: Boolean) {
        hueSlider.active = active
    }

    override fun step(dir: Int): Boolean {
        hueSlider.hue = (hueSlider.hue + dir * 10 + 360) % 360
        hueSlider.active = true
        sounds?.move()
        onChange(hueSlider.hue)
        return true
    }
}

/** Read-only row with a value on the right. */
class InfoRow(context: Context, title: String, value: String) : SettingRow(context, title, null) {
    override val interactive get() = false

    init {
        setControl(TextView(context).apply {
            text = value
            textSize = 15f
            setTextColor(SettingsStyle.textSecondary)
        })
    }
}

/** One line of the button guide: what it does, and the buttons that do it. */
class GuideRow(
    context: Context,
    title: String,
    buttons: String
) : SettingRow(context, title, null) {
    override val interactive get() = false

    init {
        minimumHeight = px(52f)
        setControl(BadgeLineView(context, HintText.parse(buttons), buttonStyle, 14f, SettingsStyle.textSecondary, badgeScale = 1.45f))
    }
}

// =========================================================================
// CONTROLS (drawn by hand so they look the same everywhere)
// =========================================================================

/** Pill-shaped option picker with a sliding white indicator. */
class SegmentedView(
    context: Context,
    private val options: List<String>,
    initial: Int,
    private val onTap: (Int) -> Unit
) : View(context) {

    private val dp = resources.displayMetrics.density
    var selectedIndex = initial
        private set
    private var indicator = initial.toFloat()
    private var anim: ValueAnimator? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 14f * resources.displayMetrics.scaledDensity
        typeface = SettingsStyle.medium
    }
    private val pad = 4f * dp
    private val baseTextSize = text.textSize
    /** Room each option would like: its longest label plus comfortable padding. */
    private val naturalSegment: Float get() = options.maxOf { text.measureText(it) } + 32f * dp
    /** Room each option actually gets (less on narrow screens). */
    private val segmentWidth: Float get() = (width - 2 * pad) / options.size

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        text.textSize = baseTextSize
        val natural = (naturalSegment * options.size + 2 * pad).roundToInt()
        // On a narrow screen, shrink to the space there is instead of running off the edge
        setMeasuredDimension(resolveSize(natural, widthSpec), (40f * dp).roundToInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // If the labels no longer fit, make the text a little smaller (never below 80 %)
        text.textSize = baseTextSize
        val widest = options.maxOf { text.measureText(it) }
        val room = segmentWidth - 12f * dp
        if (widest > room && widest > 0f) text.textSize = baseTextSize * maxOf(0.8f, room / widest)
    }

    fun select(index: Int) {
        if (index == selectedIndex) return
        selectedIndex = index
        anim?.cancel()
        anim = ValueAnimator.ofFloat(indicator, index.toFloat()).apply {
            duration = Design.MOTION_CONTROL
            interpolator = Design.EASE_OUT
            addUpdateListener {
                indicator = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        fill.color = SettingsStyle.track
        canvas.drawRoundRect(0f, 0f, width.toFloat(), h, r, r, fill)

        val left = pad + indicator * segmentWidth
        fill.color = Color.WHITE
        canvas.drawRoundRect(left, pad, left + segmentWidth, h - pad, r - pad, r - pad, fill)

        val baseline = h / 2f - (text.descent() + text.ascent()) / 2f
        options.forEachIndexed { i, label ->
            val onIndicator = 1f - min(1f, abs(indicator - i))
            text.color = SettingsStyle.blend(SettingsStyle.textPrimary, Color.BLACK, onIndicator)
            canvas.drawText(label, pad + segmentWidth * (i + 0.5f), baseline, text)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val index = ((e.x - pad) / segmentWidth).toInt().coerceIn(0, options.lastIndex)
            if (index != selectedIndex) {
                select(index)
                onTap(index)
            }
        }
        return true
    }
}

/** On/off switch with a sliding knob. */
class SwitchView(context: Context, initial: Boolean) : View(context) {
    private val dp = resources.displayMetrics.density
    var isOn = initial
        private set
    private var position = if (initial) 1f else 0f
    private var anim: ValueAnimator? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension((50f * dp).roundToInt(), (30f * dp).roundToInt())
    }

    fun set(on: Boolean) {
        isOn = on
        anim?.cancel()
        anim = ValueAnimator.ofFloat(position, if (on) 1f else 0f).apply {
            duration = Design.MOTION_CONTROL
            interpolator = Design.EASE_OUT
            addUpdateListener {
                position = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        paint.color = SettingsStyle.blend(Color.parseColor("#3A3A42"), Color.WHITE, position)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), h, r, r, paint)
        val knobR = r - 4f * dp
        val x = r + (width - 2 * r) * position
        paint.color = SettingsStyle.blend(Color.parseColor("#C8C8CE"), Color.BLACK, position)
        canvas.drawCircle(x, r, knobR, paint)
    }
}

/** Slider: white fill on a dark track, draggable by touch. */
class SliderView(
    context: Context,
    val min: Int,
    val max: Int,
    initial: Int,
    private val onDrag: (Int) -> Unit
) : View(context) {

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbR = 9f * dp

    var value = initial.coerceIn(min, max)
        set(v) {
            field = v.coerceIn(min, max)
            invalidate()
        }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension(resolveSize((200f * dp).roundToInt(), widthSpec), (32f * dp).roundToInt())
    }

    private fun xFor(v: Int): Float {
        val span = width - 2 * thumbR
        return thumbR + span * (v - min) / (max - min).coerceAtLeast(1)
    }

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f
        val t = 2f * dp
        paint.color = SettingsStyle.track
        canvas.drawRoundRect(thumbR, cy - t, width - thumbR, cy + t, t, t, paint)
        val x = xFor(value)
        paint.color = Color.WHITE
        canvas.drawRoundRect(thumbR, cy - t, x, cy + t, t, t, paint)
        canvas.drawCircle(x, cy, thumbR, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val span = (width - 2 * thumbR).coerceAtLeast(1f)
                val fraction = ((e.x - thumbR) / span).coerceIn(0f, 1f)
                val v = (min + fraction * (max - min)).roundToInt()
                if (v != value) {
                    value = v
                    onDrag(v)
                }
            }
        }
        return true
    }
}

/** Colour dots in rows of eight (fewer on narrow screens). */
class SwatchGridView(
    context: Context,
    private val colours: List<Int>,
    initial: Int,
    private val onTap: (Int) -> Unit
) : View(context) {

    private val dp = resources.displayMetrics.density
    private val cell = 40f * dp
    private var columns = MAX_COLUMNS
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val count: Int get() = colours.size

    var selectedIndex = initial.coerceIn(-1, colours.lastIndex)       // -1 = none chosen
        set(v) {
            field = v
            invalidate()
        }

    var showCursor = false
        set(v) {
            field = v
            invalidate()
        }

    private val rows get() = (colours.size + columns - 1) / columns

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val room = if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
        else MeasureSpec.getSize(widthSpec)
        columns = (room / cell).toInt().coerceIn(1, MAX_COLUMNS)
        setMeasuredDimension((cell * columns).roundToInt(), (cell * rows).roundToInt())
    }

    private companion object {
        const val MAX_COLUMNS = 8
    }

    override fun onDraw(canvas: Canvas) {
        val r = 13f * dp
        colours.forEachIndexed { i, colour ->
            val cx = cell * (i % columns) + cell / 2f
            val cy = cell * (i / columns) + cell / 2f
            paint.style = Paint.Style.FILL
            paint.color = colour
            canvas.drawCircle(cx, cy, r, paint)
            if (i == selectedIndex) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f * dp
                paint.color = Contrast.inkFor(colour)
                val g = r * 0.45f
                canvas.drawPath(Path().apply {
                    moveTo(cx - g, cy)
                    lineTo(cx - g * 0.25f, cy + g * 0.7f)
                    lineTo(cx + g, cy - g * 0.6f)
                }, paint)
                if (showCursor) {
                    paint.strokeWidth = 2f * dp
                    paint.color = Color.WHITE
                    canvas.drawCircle(cx, cy, r + 4.5f * dp, paint)
                }
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val index = (e.y / cell).toInt() * columns + (e.x / cell).toInt()
            if (index in colours.indices && index != selectedIndex) {
                selectedIndex = index
                SettingRow.sounds?.move()
                onTap(index)
            }
        }
        return true
    }
}

/** Draws a line of controller badges and words, e.g. "Hold [RT] and press [X]". */
class BadgeLineView(
    context: Context,
    private val parts: List<HintPart>,
    style: ButtonStyle,
    textSizeSp: Float,
    colour: Int,
    badgeScale: Float
) : View(context) {

    private val painter = BadgePainter(resources.displayMetrics.density).apply {
        this.style = style
        scale = badgeScale
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = textSizeSp * resources.displayMetrics.scaledDensity
        color = colour
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val textHeight = text.descent() - text.ascent()
        setMeasuredDimension(
            painter.measureLine(parts, text).roundToInt() + 2,
            (maxOf(painter.height, textHeight) + 4).roundToInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        painter.drawLine(canvas, parts, 1f, height / 2f, text)
    }
}

/** Rainbow slider for a custom colour. The thumb is filled with the chosen colour. */
class HueSliderView(
    context: Context,
    initial: Int,
    initialActive: Boolean,
    private val onDrag: (Int) -> Unit
) : View(context) {

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val thumbR = 11f * dp
    private val trackH = 6f * dp

    var hue = initial.coerceIn(0, 359)
        set(v) {
            field = v
            invalidate()
        }

    /** Dimmed while a preset colour is chosen. */
    var active = initialActive
        set(v) {
            field = v
            invalidate()
        }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension(resolveSize((272f * dp).roundToInt(), widthSpec), (32f * dp).roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f
        val left = thumbR
        val right = width - thumbR
        val stops = IntArray(7) { Palette.customAccent(it * 60) }
        paint.shader = android.graphics.LinearGradient(left, 0f, right, 0f, stops, null, android.graphics.Shader.TileMode.CLAMP)
        paint.alpha = if (active) 255 else 110
        canvas.drawRoundRect(left, cy - trackH / 2, right, cy + trackH / 2, trackH / 2, trackH / 2, paint)
        paint.shader = null
        paint.alpha = 255

        val x = left + (right - left) * hue / 359f
        paint.color = Palette.customAccent(hue)
        canvas.drawCircle(x, cy, thumbR, paint)
        ring.color = if (active) Color.WHITE else Contrast.withAlpha(Color.WHITE, 90)
        ring.strokeWidth = 2f * dp
        canvas.drawCircle(x, cy, thumbR, ring)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val span = (width - 2 * thumbR).coerceAtLeast(1f)
                val v = (((e.x - thumbR) / span).coerceIn(0f, 1f) * 359).roundToInt()
                if (v != hue || !active) {
                    hue = v
                    active = true
                    onDrag(v)
                }
            }
        }
        return true
    }
}
