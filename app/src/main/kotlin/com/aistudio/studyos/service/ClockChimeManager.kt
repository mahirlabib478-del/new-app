package com.aistudio.studyos.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-fidelity synthesized brass bell chime for study transitions.
 * Synthesizes rich acoustic bell harmonics, natural strike transient,
 * and warm exponential decay using AudioTrack at clear, audible volume.
 */
object ClockChimeManager {
    private const val SAMPLE_RATE = 44_100

    fun playFocusToBreak() {
        playBell(singleChimeData)
    }

    fun playBreakToFocus() {
        playBell(doubleChimeData)
    }

    fun playTestChime() {
        playBell(singleChimeData)
    }

    private fun playBell(pcmData: ShortArray) {
        Thread({
            val audioTrack = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return@Thread

            try {
                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.setVolume(1.0f)
                    audioTrack.write(pcmData, 0, pcmData.size)
                    audioTrack.play()
                    val durationMs = (pcmData.size * 1000L) / SAMPLE_RATE
                    Thread.sleep(durationMs + 100)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                runCatching {
                    audioTrack.stop()
                    audioTrack.release()
                }
            }
        }, "StudyOS-BellChime").start()
    }

    /**
     * Single resonant bronze temple bell strike (D5 note, 587.3 Hz).
     */
    private val singleChimeData: ShortArray by lazy {
        synthesizeBell(
            strikeTimes = doubleArrayOf(0.0),
            baseFreqs = doubleArrayOf(587.33),
            durationSec = 1.8
        )
    }

    /**
     * Two uplifting bell chimes (G5 then D5: ting-dong).
     */
    private val doubleChimeData: ShortArray by lazy {
        synthesizeBell(
            strikeTimes = doubleArrayOf(0.0, 0.32),
            baseFreqs = doubleArrayOf(783.99, 587.33),
            durationSec = 2.0
        )
    }

    private fun synthesizeBell(
        strikeTimes: DoubleArray,
        baseFreqs: DoubleArray,
        durationSec: Double
    ): ShortArray {
        val totalSamples = (durationSec * SAMPLE_RATE).toInt()
        val floatSamples = FloatArray(totalSamples)

        for (s in strikeTimes.indices) {
            val strikeOffset = strikeTimes[s]
            val f0 = baseFreqs[s]
            val startSample = (strikeOffset * SAMPLE_RATE).toInt()

            for (i in startSample until totalSamples) {
                val t = (i - startSample).toDouble() / SAMPLE_RATE

                // Fast attack envelope (3ms) to avoid clicks while keeping crisp impact
                val attack = (t / 0.003).coerceIn(0.0, 1.0)

                // Bell partials (fundamental + minor tierce + octave nominal + super-nominal + clink)
                val p1 = 0.45 * sin(2.0 * PI * f0 * t) * exp(-2.0 * t)
                val p2 = 0.28 * sin(2.0 * PI * (f0 * 1.2) * t) * exp(-2.6 * t)
                val p3 = 0.20 * sin(2.0 * PI * (f0 * 2.0) * t) * exp(-3.5 * t)
                val p4 = 0.12 * sin(2.0 * PI * (f0 * 3.1) * t) * exp(-5.0 * t)
                val p5 = 0.08 * sin(2.0 * PI * (f0 * 4.8) * t) * exp(-16.0 * t)

                val bellSample = attack * (p1 + p2 + p3 + p4 + p5)
                floatSamples[i] += bellSample.toFloat()
            }
        }

        // Normalize to ~90% of 16-bit max range for loud, clear, distortion-free output
        var maxAbs = 0f
        for (sample in floatSamples) {
            val absVal = kotlin.math.abs(sample)
            if (absVal > maxAbs) maxAbs = absVal
        }

        val scale = if (maxAbs > 0f) (Short.MAX_VALUE * 0.90f) / maxAbs else 1f
        val out = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val scaled = (floatSamples[i] * scale).toInt()
            out[i] = scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}

