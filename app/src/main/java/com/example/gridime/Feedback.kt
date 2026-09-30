package com.example.gridime

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.random.Random

/**
 * Sound and vibration for every keyboard action, in one place.
 *
 * Four sound packs share the same ten cues and the same meanings:
 *  - Soft (default) and Soft Deep: a short low thud for weight plus a crisp click for
 *    texture, together, gone within ~15 ms. Letters rotate between four takes.
 *  - Tactile: dry mechanical clicks with no notes
 *  - Chime: soft mallet notes in C major pentatonic
 * The rule in every pack: ONE action = ONE hit, and one vibration pulse.
 * Actions differ by weight, pitch and length, never by counting hits:
 * keys are short, switches (on / off, triggers) are a heavier "latch" that bends up
 * for on and down for off, lock is the heaviest with a short ring, confirm / back ring softly.
 *
 * Vibration and sound always fire together, and every pulse starts with a short
 * full-power kick so small vibration motors spin up and the pulse is actually felt.
 */
class Feedback(context: Context) {

    private var settings = KeyboardSettings()

    // ---- Vibration -----------------------------------------------------------
    // Android 12+ has a VibratorManager; older versions hand out the Vibrator directly.
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    // ---- Sound ---------------------------------------------------------------
    private val audio: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)                 // follows media volume
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** One sound per event, loaded for the chosen pack. */
    private enum class Cue { MOVE, KEY, SPACE, DELETE, ON, OFF, LOCK, SELECT_ALL, CONFIRM, BACK }

    private val appContext = context.applicationContext
    private var loadedPack: SoundPack? = null
    private val ids = IntArray(Cue.values().size)

    /** Extra "takes" for letters: real keys never sound identical twice. */
    private val keyTakes = mutableListOf<Int>()
    private var lastTake = -1

    /** The ten cues of a pack, in [Cue] order, by file prefix (e.g. "soft" -> soft_move ...). */
    private fun resourcesFor(pack: SoundPack): IntArray = when (pack) {
        SoundPack.SOFT -> intArrayOf(
            R.raw.soft_move, R.raw.soft_type, R.raw.soft_space, R.raw.soft_delete, R.raw.soft_on,
            R.raw.soft_off, R.raw.soft_lock, R.raw.soft_select_all, R.raw.soft_confirm, R.raw.soft_back
        )
        SoundPack.SOFT_DEEP -> intArrayOf(
            R.raw.softdeep_move, R.raw.softdeep_type, R.raw.softdeep_space, R.raw.softdeep_delete, R.raw.softdeep_on,
            R.raw.softdeep_off, R.raw.softdeep_lock, R.raw.softdeep_select_all, R.raw.softdeep_confirm, R.raw.softdeep_back
        )
        SoundPack.TACTILE -> intArrayOf(
            R.raw.tactile_move, R.raw.tactile_type, R.raw.tactile_space, R.raw.tactile_delete, R.raw.tactile_on,
            R.raw.tactile_off, R.raw.tactile_lock, R.raw.tactile_select_all, R.raw.tactile_confirm, R.raw.tactile_back
        )
        SoundPack.CHIME -> intArrayOf(
            R.raw.chime_move, R.raw.chime_type, R.raw.chime_space, R.raw.chime_delete, R.raw.chime_on,
            R.raw.chime_off, R.raw.chime_lock, R.raw.chime_select_all, R.raw.chime_confirm, R.raw.chime_back
        )
    }

    private fun takesFor(pack: SoundPack): IntArray = when (pack) {
        SoundPack.SOFT -> intArrayOf(R.raw.soft_type1, R.raw.soft_type2, R.raw.soft_type3)
        SoundPack.SOFT_DEEP -> intArrayOf(R.raw.softdeep_type1, R.raw.softdeep_type2, R.raw.softdeep_type3)
        else -> intArrayOf()
    }

    /** Loads the pack's sounds, releasing the old ones. Only one pack is in memory at a time. */
    private fun loadPack(pack: SoundPack) {
        if (pack == loadedPack) return
        ids.forEach { if (it != 0) pool.unload(it) }
        keyTakes.forEach { pool.unload(it) }
        keyTakes.clear()
        resourcesFor(pack).forEachIndexed { i, res -> ids[i] = pool.load(appContext, res, 1) }
        keyTakes += ids[Cue.KEY.ordinal]
        takesFor(pack).forEach { keyTakes += pool.load(appContext, it, 1) }
        lastTake = -1
        loadedPack = pack
    }

    /** A different take from last time, so fast typing never sounds machine-gunned. */
    private fun nextKeyTake(): Int {
        if (keyTakes.size <= 1) return ids[Cue.KEY.ordinal]
        var i = Random.nextInt(keyTakes.size - 1)
        if (i >= lastTake && lastTake >= 0) i++
        lastTake = i
        return keyTakes[i]
    }

    fun update(newSettings: KeyboardSettings) {
        settings = newSettings
        loadPack(newSettings.soundPack)
    }

    init {
        loadPack(settings.soundPack)
    }

    fun release() = pool.release()

    // =====================================================================
    // EVENTS
    // =====================================================================

    /** The highlight moved to another key. */
    fun move() {
        tick()
        if (settings.moveSounds) play(Cue.MOVE, Design.SOUND_MOVE)
    }

    /** A letter, number or symbol was typed. */
    fun key() {
        click()
        playId(nextKeyTake(), Design.SOUND_KEY)
    }

    fun space() {
        click()
        play(Cue.SPACE, Design.SOUND_KEY)
    }

    fun delete() {
        click()
        play(Cue.DELETE, Design.SOUND_KEY)
    }

    /** Held delete: lighter, so a long run of deletes isn't tiring. */
    fun deleteRepeat() {
        tick()
        play(Cue.DELETE, Design.SOUND_KEY * 0.6f)
    }

    /** Something switched on: shift, symbols, select mode, clipboard. */
    fun on() {
        latch(heavy = false)
        play(Cue.ON, Design.SOUND_MODIFIER)
    }

    /** Something switched off. */
    fun off() {
        click()
        play(Cue.OFF, Design.SOUND_MODIFIER)
    }

    /** Caps lock. */
    fun lock() {
        latch(heavy = true)
        play(Cue.LOCK, Design.SOUND_MODIFIER)
    }

    fun selectAll() {
        latch(heavy = false)
        play(Cue.SELECT_ALL, Design.SOUND_MODIFIER)
    }

    /** Enter, paste, picking a clipboard item. */
    fun confirm() {
        click()
        play(Cue.CONFIRM, Design.SOUND_CONFIRM)
    }

    /** Leaving a mode or a view with B. */
    fun back() {
        tick()
        play(Cue.BACK, Design.SOUND_CONFIRM)
    }

    // =====================================================================
    // VIBRATION PATTERNS
    // =====================================================================
    // Strength 0..10 sets both how long and how hard the motor runs.

    private fun tick() {
        val level = settings.moveStrength
        if (level <= 0) return
        pulse(bodyMs = 10L + level * 3L, amplitude = 120 + level * 13)
    }

    private fun click() {
        val level = settings.pressStrength
        if (level <= 0) return
        pulse(bodyMs = 18L + level * 4L, amplitude = 140 + level * 11)
    }

    /**
     * A single, weightier pulse for switches and triggers: longer and stronger than a
     * key press, with a gentle fade at the end instead of a second bump.
     * Rule: one action = one pulse. Actions differ by weight, never by counting pulses.
     */
    private fun latch(heavy: Boolean) {
        val level = settings.pressStrength
        if (level <= 0) return
        val body = 24L + level * 4L + if (heavy) 14L else 0L
        val amp = amplitude(165 + level * 8 + if (heavy) 15 else 0)
        waveform(
            longArrayOf(Design.HAPTIC_KICK_MS, body, 14L),
            intArrayOf(255, amp, amp / 3)                       // settle, don't stop dead
        )
    }

    /** One pulse: a full-power kick, then the body at the chosen strength. */
    private fun pulse(bodyMs: Long, amplitude: Int) {
        waveform(longArrayOf(Design.HAPTIC_KICK_MS, bodyMs), intArrayOf(255, amplitude(amplitude)))
    }

    private fun amplitude(value: Int) = value.coerceIn(1, 255)

    private fun waveform(timings: LongArray, amplitudes: IntArray) {
        if (!settings.vibration) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        runCatching {
            val effect = if (v.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                // Motor can only switch on and off: use the same timing, full power
                VibrationEffect.createWaveform(timings, IntArray(amplitudes.size) { if (amplitudes[it] > 0) 255 else 0 }, -1)
            }
            vibrateAsTouch(v, effect)
        }
    }

    /**
     * Tag the pulse as touch feedback so it keeps working in silent mode.
     * Android 13+ has a dedicated "touch" tag; older versions use the closest audio tag.
     */
    private fun vibrateAsTouch(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
    }

    // =====================================================================
    // SOUND
    // =====================================================================
    private fun play(cue: Cue, gain: Float) = playId(ids[cue.ordinal], gain)

    private fun playId(soundId: Int, gain: Float) {
        if (settings.soundVolume <= 0) return
        val silent = audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL
        if (silent && !settings.soundInSilent) return
        // Gentle curve: every slider step sounds like an even change in loudness
        val level = settings.soundVolume / 10f
        // A tiny loudness change keeps repeats from sounding robotic. Pitch never
        // changes: every sound is tuned to the same scale and must stay in tune.
        val jitter = 1f + (Random.nextFloat() * 2f - 1f) * Design.SOUND_GAIN_JITTER
        val volume = ((level * level * 0.35f + level * 0.65f) * gain * jitter).coerceIn(0f, 1f)
        pool.play(soundId, volume, volume, 1, 0, 1f)
    }
}
