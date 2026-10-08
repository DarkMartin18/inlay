package com.example.gridime

import android.content.Context
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** Which controller's button icons the keyboard shows. */
enum class ButtonStyle(val label: String) {
    XBOX("Xbox"),
    PLAYSTATION("PlayStation"),
    SWITCH("Switch")
}

/** Letter font choices. The first two ship inside the app (res/font). */
enum class FontChoice(val label: String) {
    HYPERLEGIBLE("Hyperlegible"),
    INTER("Inter"),
    SYSTEM("System")
}

/** Key arrangement. Grid lines every key up in columns, which suits a D-pad best. */
enum class KeyLayout(val label: String) {
    GRID("Grid"),
    CLASSIC("Classic")
}

/** How the highlight travels between keys. */
enum class HighlightMotion(val label: String) {
    INSTANT("Instant"),
    SUBTLE("Subtle"),
    FLUID("Fluid")
}

/** Which set of sounds plays. Soft is the default: a short thud plus a crisp click. */
enum class SoundPack(val label: String) {
    SOFT("Soft"),
    SOFT_DEEP("Soft Deep"),
    TACTILE("Tactile"),
    CHIME("Chime")
}

/** How the selected key is marked (after Android TV's focus system: fill, glow, outline, lift). */
enum class HighlightStyle(val label: String) {
    GLOW("Glow"),
    SOLID("Solid"),
    OUTLINE("Outline"),
    LIFT("Lift")
}

/** Key corner roundness. */
enum class KeyShape(val label: String, val radiusDp: Float) {
    SHARP("Sharp", 5f),
    SOFT("Soft", 9f),
    ROUND("Round", 14f)
}

/** Adjusts the hold time relative to the existing default for each input method. */
enum class VariantMenuDelay(val label: String, private val adjustmentMs: Long) {
    SLOWEST("Slowest", 300L),
    SLOW("Slow", 150L),
    MEDIUM("Medium", 0L),
    FAST("Fast", -100L),
    FASTEST("Fastest", -200L);

    fun from(baseMs: Long): Long = (baseMs + adjustmentMs).coerceAtLeast(0L)
}

/**
 * The physical buttons, named by Android's keycodes.
 * A = bottom face button, B = right, X = left, Y = top (Xbox positions).
 */
enum class ControllerButton { A, B, X, Y, L1, R1, L2, R2, SELECT, START, L3, R3 }

/** A key colour set: keyboard background, letter keys, special keys. */
data class Tone(val name: String, val background: Int, val key: Int, val special: Int)

object Palette {
    val accents: List<Pair<String, Int>> = listOf(
        "Sky" to "#8AB4F8",
        "Ocean" to "#4FA8FF",
        "Cyan" to "#4DD9E8",
        "Teal" to "#3FD1B0",
        "Mint" to "#7EE6A8",
        "Lime" to "#B5E853",
        "Lemon" to "#F5E46A",
        "Amber" to "#FFC857",
        "Tangerine" to "#FF9F5A",
        "Coral" to "#FF8A80",
        "Rose" to "#F48FB1",
        "Orchid" to "#E27AE8",
        "Lavender" to "#B39DFF",
        "Indigo" to "#8C9EFF",
        "Sand" to "#D9C7A7",
        "Ice" to "#E8EAED"
    ).map { it.first to Color.parseColor(it.second) }

    val tones: List<Tone> = listOf(
        Tone("OLED", Color.BLACK, Color.parseColor("#1B1B1F"), Color.parseColor("#111114")),
        Tone("Graphite", Color.parseColor("#0E0E10"), Color.parseColor("#26262B"), Color.parseColor("#1A1A1E")),
        Tone("Midnight", Color.parseColor("#03060C"), Color.parseColor("#172030"), Color.parseColor("#0F1622")),
        Tone("Forest", Color.parseColor("#030805"), Color.parseColor("#16231B"), Color.parseColor("#0E1812")),
        Tone("Mocha", Color.parseColor("#0B0806"), Color.parseColor("#261E19"), Color.parseColor("#1A1511")),
        Tone("Slate", Color.parseColor("#05070A"), Color.parseColor("#1E242C"), Color.parseColor("#14191F")),
        Tone("Plum", Color.parseColor("#08040A"), Color.parseColor("#241A2B"), Color.parseColor("#18111D")),
        Tone("Ocean", Color.parseColor("#02080A"), Color.parseColor("#132429"), Color.parseColor("#0C191D"))
    )

    /** A custom accent from a hue (0..359), kept as bright and soft as the preset colours. */
    fun customAccent(hue: Int): Int = Color.HSVToColor(floatArrayOf(hue.toFloat().mod(360f), 0.48f, 0.98f))
}

/** Everything the Settings app can change. Saved on the device, read by the keyboard. */
data class KeyboardSettings(
    // Look
    val buttonStyle: ButtonStyle = ButtonStyle.XBOX,
    val accentIndex: Int = 0,           // -1 = custom colour from customHue
    val customHue: Int = 210,           // 0 to 359
    val toneIndex: Int = 0,
    val font: FontChoice = FontChoice.HYPERLEGIBLE,
    val letterWeight: Int = 4,          // 0 to 10
    val sizeLevel: Int = 0,             // 0 normal, 1 large, 2 largest
    val floatingKeyboard: Boolean = false,
    val layout: KeyLayout = KeyLayout.GRID,
    val showNumberRow: Boolean = false,
    val keyShape: KeyShape = KeyShape.SOFT,
    val motion: HighlightMotion = HighlightMotion.SUBTLE,
    val highlightStyle: HighlightStyle = HighlightStyle.GLOW,
    // Controls
    val stickSpeed: Int = 2,            // 0 slow to 4 fast
    val dpadSpeed: Int = 2,             // 0 slow to 4 fast
    val floatingKeyboardSpeed: Int = 2, // 0 slow to 4 fast
    val showHints: Boolean = true,      // badges on keys
    val comboHints: Boolean = true,     // chips while holding RT
    val selectAllButton: Boolean = false,
    val doubleSpacePeriod: Boolean = true,
    val undoCombo: Boolean = true,
    val commitVariantOnRelease: Boolean = true,
    val variantMenuDelay: Int = VariantMenuDelay.MEDIUM.ordinal,
    // Sound & haptics
    val vibration: Boolean = true,
    val moveStrength: Int = 5,          // 0 to 10
    val pressStrength: Int = 6,         // 0 to 10
    val soundVolume: Int = 7,           // 0 (off) to 10
    val soundPack: SoundPack = SoundPack.SOFT,
    val moveSounds: Boolean = true,
    val soundInSilent: Boolean = false,
    // Clipboard
    val saveClipHistory: Boolean = true,
    // Settings app
    val showPreview: Boolean = true
) {
    val accent: Int
        get() = if (accentIndex < 0) Palette.customAccent(customHue)
        else Palette.accents[accentIndex.coerceIn(0, Palette.accents.lastIndex)].second
    val tone: Tone get() = Palette.tones[toneIndex.coerceIn(0, Palette.tones.lastIndex)]

    fun variantMenuDelayFrom(baseMs: Long): Long =
        VariantMenuDelay.values()[variantMenuDelay.coerceIn(0, VariantMenuDelay.values().lastIndex)].from(baseMs)

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(K_STYLE, buttonStyle.name)
            .putInt(K_ACCENT, accentIndex)
            .putInt(K_HUE, customHue)
            .putInt(K_TONE, toneIndex)
            .putString(K_FONT, font.name)
            .putInt(K_WEIGHT, letterWeight)
            .putInt(K_SIZE, sizeLevel)
            .putBoolean(K_FLOATING_KEYBOARD, floatingKeyboard)
            .putString(K_LAYOUT, layout.name)
            .putBoolean(K_SHOW_NUMBER_ROW, showNumberRow)
            .putString(K_SHAPE, keyShape.name)
            .putString(K_MOTION, motion.name)
            .putString(K_HIGHLIGHT, highlightStyle.name)
            .putInt(K_STICK, stickSpeed)
            .putInt(K_DPAD, dpadSpeed)
            .putInt(K_FLOATING_KEYBOARD_SPEED, floatingKeyboardSpeed)
            .putBoolean(K_HINTS, showHints)
            .putBoolean(K_COMBO_HINTS, comboHints)
            .putBoolean(K_SELECT_ALL_BUTTON, selectAllButton)
            .putBoolean(K_DOUBLE_SPACE, doubleSpacePeriod)
            .putBoolean(K_UNDO, undoCombo)
            .putBoolean(K_COMMIT_VARIANT_ON_RELEASE, commitVariantOnRelease)
            .putInt(K_VARIANT_MENU_DELAY, variantMenuDelay)
            .putBoolean(K_VIBRATION, vibration)
            .putInt(K_MOVE, moveStrength)
            .putInt(K_PRESS, pressStrength)
            .putInt(K_VOLUME, soundVolume)
            .putString(K_PACK, soundPack.name)
            .putBoolean(K_MOVE_SOUNDS, moveSounds)
            .putBoolean(K_SILENT, soundInSilent)
            .putBoolean(K_CLIP_HISTORY, saveClipHistory)
            .putBoolean(K_PREVIEW, showPreview)
            .apply()
    }

    companion object {
        const val PREFS = "gridime"
        const val KEY_CLIPS = "clip_history"

        private const val K_STYLE = "button_style"
        private const val K_ACCENT = "accent"
        private const val K_HUE = "custom_hue"
        private const val K_PACK = "sound_pack"
        private const val K_SHAPE = "key_shape"
        private const val K_MOTION = "highlight_motion"
        private const val K_HIGHLIGHT = "highlight_style"
        private const val K_TONE = "tone"
        private const val K_FONT = "font"
        private const val K_WEIGHT = "letter_weight"
        private const val K_SIZE = "size_level"
        private const val K_FLOATING_KEYBOARD = "floating_keyboard"
        private const val K_LAYOUT = "layout"
        private const val K_SHOW_NUMBER_ROW = "show_number_row"
        private const val K_STICK = "stick_speed"
        private const val K_DPAD = "dpad_speed"
        private const val K_FLOATING_KEYBOARD_SPEED = "floating_keyboard_speed"
        private const val K_HINTS = "show_hints"
        private const val K_COMBO_HINTS = "combo_hints"
        private const val K_SELECT_ALL_BUTTON = "select_all_button"
        private const val K_DOUBLE_SPACE = "double_space_period"
        private const val K_UNDO = "undo_combo"
        private const val K_COMMIT_VARIANT_ON_RELEASE = "commit_variant_on_release"
        private const val K_VARIANT_MENU_DELAY = "variant_menu_delay"
        private const val K_VIBRATION = "vibration"
        private const val K_MOVE = "move_strength_v7"     // v7: vibration scale changed
        private const val K_PRESS = "press_strength_v7"
        private const val K_VOLUME = "sound_volume_v7"    // v7: sounds remastered
        private const val K_MOVE_SOUNDS = "move_sounds"
        private const val K_SILENT = "sound_in_silent"
        private const val K_CLIP_HISTORY = "save_clip_history"
        private const val K_PREVIEW = "show_preview"

        fun load(context: Context): KeyboardSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = KeyboardSettings()
            return KeyboardSettings(
                buttonStyle = enumOr(p.getString(K_STYLE, null), d.buttonStyle),
                accentIndex = p.getInt(K_ACCENT, d.accentIndex),
                customHue = p.getInt(K_HUE, d.customHue),
                toneIndex = p.getInt(K_TONE, d.toneIndex),
                font = enumOr(p.getString(K_FONT, null), d.font),
                letterWeight = p.getInt(K_WEIGHT, d.letterWeight),
                sizeLevel = p.getInt(K_SIZE, d.sizeLevel),
                floatingKeyboard = p.getBoolean(K_FLOATING_KEYBOARD, d.floatingKeyboard),
                layout = enumOr(p.getString(K_LAYOUT, null), d.layout),
                showNumberRow = p.getBoolean(K_SHOW_NUMBER_ROW, d.showNumberRow),
                keyShape = enumOr(p.getString(K_SHAPE, null), d.keyShape),
                motion = enumOr(p.getString(K_MOTION, null), d.motion),
                highlightStyle = enumOr(p.getString(K_HIGHLIGHT, null), d.highlightStyle),
                stickSpeed = p.getInt(K_STICK, d.stickSpeed),
                dpadSpeed = p.getInt(K_DPAD, d.dpadSpeed),
                floatingKeyboardSpeed = p.getInt(K_FLOATING_KEYBOARD_SPEED, d.floatingKeyboardSpeed),
                showHints = p.getBoolean(K_HINTS, d.showHints),
                comboHints = p.getBoolean(K_COMBO_HINTS, d.comboHints),
                selectAllButton = p.getBoolean(K_SELECT_ALL_BUTTON, d.selectAllButton),
                doubleSpacePeriod = p.getBoolean(K_DOUBLE_SPACE, d.doubleSpacePeriod),
                undoCombo = p.getBoolean(K_UNDO, d.undoCombo),
                commitVariantOnRelease = p.getBoolean(K_COMMIT_VARIANT_ON_RELEASE, d.commitVariantOnRelease),
                variantMenuDelay = p.getInt(K_VARIANT_MENU_DELAY, d.variantMenuDelay),
                vibration = p.getBoolean(K_VIBRATION, d.vibration),
                moveStrength = p.getInt(K_MOVE, d.moveStrength),
                pressStrength = p.getInt(K_PRESS, d.pressStrength),
                soundVolume = p.getInt(K_VOLUME, d.soundVolume),
                soundPack = enumOr(p.getString(K_PACK, null), d.soundPack),
                moveSounds = p.getBoolean(K_MOVE_SOUNDS, d.moveSounds),
                soundInSilent = p.getBoolean(K_SILENT, d.soundInSilent),
                saveClipHistory = p.getBoolean(K_CLIP_HISTORY, d.saveClipHistory),
                showPreview = p.getBoolean(K_PREVIEW, d.showPreview)
            )
        }

        private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
            name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: fallback
    }
}

/** Picks readable colours automatically, using the WCAG contrast formula. */
object Contrast {
    val DARK: Int = Color.parseColor("#0B0F17")

    fun ratio(a: Int, b: Int): Double {
        val la = Color.luminance(a) + 0.05
        val lb = Color.luminance(b) + 0.05
        return max(la, lb) / min(la, lb)
    }

    /** White or near-black, whichever reads better on this background. */
    fun inkFor(background: Int): Int =
        if (ratio(background, Color.WHITE) >= ratio(background, DARK)) Color.WHITE else DARK

    /** The colour you actually see when a see-through colour sits on a solid one. */
    fun over(foreground: Int, background: Int): Int {
        val a = Color.alpha(foreground) / 255f
        return Color.rgb(
            (Color.red(foreground) * a + Color.red(background) * (1 - a)).toInt(),
            (Color.green(foreground) * a + Color.green(background) * (1 - a)).toInt(),
            (Color.blue(foreground) * a + Color.blue(background) * (1 - a)).toInt()
        )
    }

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
}
