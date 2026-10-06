package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance, zero-external-dependency Sound Manager.
 * Synthesizes crisp retro-arcade and modern game sound effects on-the-fly via AudioTrack PCM.
 */
class SoundManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("money_tycoon_audio", Context.MODE_PRIVATE)
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sfx_enabled", true)
        set(value) = prefs.edit().putBoolean("sfx_enabled", value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 44100

    private fun playPcm(samples: ShortArray) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val bufferSize = samples.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Let it finish then release
                val durationMs = (samples.size * 1000L) / sampleRate
                Thread.sleep(durationMs + 20)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio hardware transients
            }
        }
    }

    /** Fast "cha-ching" register sound (cash register chime + coin cascade) */
    fun playChaChing() {
        val duration = 0.35
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            // Tone 1 at 987 Hz, Tone 2 at 1318 Hz + metallic harmonic at 2636 Hz
            val f1 = if (t < 0.12) 987.77 else 1318.51
            val f2 = if (t < 0.12) 1975.53 else 2637.02
            val s1 = sin(2.0 * PI * f1 * t)
            val s2 = sin(2.0 * PI * f2 * t) * 0.4
            val mixed = (s1 + s2) * env * 0.65
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Double "cha-ching" for mission rewards */
    fun playDoubleChaChing() {
        val duration = 0.55
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val tSub = if (t < 0.25) t else (t - 0.25)
            val env = (1.0 - (tSub / 0.25)).coerceIn(0.0, 1.0)
            val f1 = if (tSub < 0.1) 1046.50 else 1567.98
            val f2 = if (tSub < 0.1) 2093.00 else 3135.96
            val s1 = sin(2.0 * PI * f1 * tSub)
            val s2 = sin(2.0 * PI * f2 * tSub) * 0.45
            val mixed = (s1 + s2) * env * 0.6
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Metallic coin click with pitch variation */
    fun playCoinClick(pitchVariation: Float = 1.0f) {
        val duration = 0.08
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        val baseFreq = 1650.0 * pitchVariation

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - (t / duration)).coerceIn(0.0, 1.0)
            // Metallic ring: base frequency + harmonic + tiny noise click at start
            val s1 = sin(2.0 * PI * baseFreq * t)
            val s2 = sin(2.0 * PI * (baseFreq * 2.3) * t) * 0.35
            val click = if (t < 0.005) (Random.nextDouble(-0.3, 0.3)) else 0.0
            val mixed = (s1 + s2 + click) * env * 0.7
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Subtle bubble pop for floating text */
    fun playPop() {
        val duration = 0.06
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 450.0 + (1200.0 * (1.0 - t / duration))
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * freq * t) * env * 0.5
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Mechanical construction punch / hammer thump */
    fun playConstructionPunch() {
        val duration = 0.14
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val thump = sin(2.0 * PI * (120.0 - t * 400.0).coerceAtLeast(40.0) * t)
            val metallic = sin(2.0 * PI * 880.0 * t) * 0.25
            val noise = if (t < 0.03) Random.nextDouble(-0.3, 0.3) else 0.0
            val mixed = (thump + metallic + noise) * env * 0.75
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Level-up chime (C5, E5, G5, C6 ascending arpeggio) */
    fun playLevelUpChime() {
        val duration = 0.32
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIndex = ((t / duration) * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex]
            val subT = t - (noteIndex * (duration / notes.size))
            val env = (1.0 - (subT / (duration / notes.size))).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * freq * t) * env * 0.65
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Page flip / rubber stamp when hiring managers */
    fun playManagerHiredStamp() {
        val duration = 0.2
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            // Heavy stamp thud + rustle
            val thud = sin(2.0 * PI * 90.0 * t) * (if (t < 0.08) 0.8 else 0.2)
            val snap = if (t in 0.02..0.06) sin(2.0 * PI * 2400.0 * t) * 0.35 else 0.0
            val mixed = (thud + snap) * env * 0.7
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Luxury engine rev / solid door chime for luxury purchases */
    fun playLuxuryRev() {
        val duration = 0.45
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - (t / duration) * 0.7).coerceIn(0.0, 1.0)
            // Low rumble pitch rises like revving supercar V12
            val revFreq = 75.0 + (t * 220.0)
            val rumble = sin(2.0 * PI * revFreq * t)
            val harmonic = sin(2.0 * PI * (revFreq * 2.0) * t) * 0.4
            val chime = if (t > 0.2) sin(2.0 * PI * 1318.0 * t) * 0.2 else 0.0
            val mixed = (rumble + harmonic + chime) * env * 0.75
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Rising synth sweep for LONG position */
    fun playLongSynth() {
        val duration = 0.22
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 440.0 + (660.0 * (t / duration)) // 440 -> 1100 Hz rising
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * freq * t) * env * 0.6
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Descending synth sweep for SHORT position */
    fun playShortSynth() {
        val duration = 0.22
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 900.0 - (500.0 * (t / duration)) // 900 -> 400 Hz falling
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * freq * t) * env * 0.6
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Coin-cascade for trading profit */
    fun playProfitCascade() {
        val duration = 0.4
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val step = ((t * 8.0).toInt()) % 4
            val f = when (step) {
                0 -> 1200.0
                1 -> 1500.0
                2 -> 1800.0
                else -> 2200.0
            }
            val mixed = sin(2.0 * PI * f * t) * env * 0.65
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Muted thud for trading loss */
    fun playLossThud() {
        val duration = 0.16
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * 110.0 * t) * env * 0.7
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Digital tick for timeframe / chips switches */
    fun playTick() {
        val duration = 0.03
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * 2200.0 * t) * env * 0.4
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Keypad beep during code entry */
    fun playKeypadBeep() {
        val duration = 0.05
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val mixed = sin(2.0 * PI * 1336.0 * t) * env * 0.4
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Futuristic access chime for unlocking secret admin mode */
    fun playFuturisticChime() {
        val duration = 0.4
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val f = 800.0 + (t * 1600.0)
            val s1 = sin(2.0 * PI * f * t)
            val s2 = sin(2.0 * PI * (f * 1.5) * t) * 0.3
            val mixed = (s1 + s2) * env * 0.65
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Power-down sweep for resetting game progress */
    fun playPowerDown() {
        val duration = 0.5
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = (1.0 - t / duration).coerceIn(0.0, 1.0)
            val f = 1200.0 * (1.0 - (t / duration) * 0.8)
            val mixed = sin(2.0 * PI * f * t) * env * 0.7
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }
}
