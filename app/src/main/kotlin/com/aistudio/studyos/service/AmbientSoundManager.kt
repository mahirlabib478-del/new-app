package com.aistudio.studyos.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates ambient audio locally; no audio assets or network are required.
 * The worker thread owns the AudioTrack so playback is not tied to Compose.
 */
object AmbientSoundManager {
    enum class Preset(val label: String) {
        WHITE_NOISE("White Noise"),
        RAIN("Gentle Rain"),
        DEEP_FOCUS("Deep Focus 196Hz"),
        FOREST_STREAM("Forest Stream")
    }

    @Volatile
    private var track: AudioTrack? = null
    @Volatile
    private var worker: Thread? = null
    @Volatile
    private var running = false
    @Volatile
    private var currentVolume = 0.35f
    @Volatile
    private var currentPreset = Preset.RAIN

    @Synchronized
    fun play(preset: Preset = currentPreset, volume: Float = currentVolume) {
        currentPreset = preset
        currentVolume = volume.coerceIn(0f, 1f)
        stop()
        val sampleRate = 44_100
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return

        val bufferSize = maxOf(minBuffer, sampleRate / 2)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            return
        }

        track = audioTrack
        running = true
        audioTrack.play()

        worker = Thread({
            val samples = ShortArray(sampleRate / 10)
            val random = Random.Default
            var phase = 0.0
            var rainState = 0.0
            while (running && track === audioTrack) {
                for (i in samples.indices) {
                    val value = when (currentPreset) {
                        Preset.WHITE_NOISE -> random.nextDouble(-1.0, 1.0)
                        Preset.RAIN -> {
                            val impulse = if (random.nextFloat() < 0.018f) random.nextDouble(-1.0, 1.0) else 0.0
                            rainState = rainState * 0.94 + random.nextDouble(-0.18, 0.18) + impulse
                            rainState.coerceIn(-1.0, 1.0)
                        }
                        Preset.DEEP_FOCUS -> sin(phase).also {
                            phase += 2.0 * PI * 196.0 / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                        }
                        Preset.FOREST_STREAM -> {
                            rainState = rainState * 0.985 + random.nextDouble(-0.25, 0.25)
                            (rainState * 0.65 + random.nextDouble(-0.08, 0.08)).coerceIn(-1.0, 1.0)
                        }
                    }
                    samples[i] = (value * currentVolume * Short.MAX_VALUE).toInt().coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    ).toShort()
                }
                if (audioTrack.write(samples, 0, samples.size) < 0) break
            }
        }, "StudyOS-AmbientSound")
        worker?.start()
    }

    @Synchronized
    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        track?.setVolume(currentVolume)
    }

    @Synchronized
    fun stop() {
        running = false
        val oldTrack = track
        track = null
        worker?.interrupt()
        worker = null
        runCatching {
            oldTrack?.pause()
            oldTrack?.flush()
            oldTrack?.release()
        }
    }

    fun isPlaying(): Boolean = running
}
