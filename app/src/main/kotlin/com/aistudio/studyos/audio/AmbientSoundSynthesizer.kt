package com.aistudio.studyos.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSound(val displayName: String) {
    NONE("None"),
    WHITE_NOISE("White Noise"),
    RAIN("Gentle Rain"),
    FOCUS_TONE("Deep Focus (Binaural)"),
    FOREST_STREAM("Forest Stream")
}

class AmbientSoundSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentSound: AmbientSound = AmbientSound.NONE
        private set

    fun play(sound: AmbientSound) {
        if (sound == currentSound) return
        stop()
        if (sound == AmbientSound.NONE) {
            currentSound = AmbientSound.NONE
            return
        }
        currentSound = sound

        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        synthJob = scope.launch {
            val buffer = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            var pinkState = 0.0

            while (isActive) {
                when (sound) {
                    AmbientSound.WHITE_NOISE -> {
                        for (i in buffer.indices) {
                            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.15
                            buffer[i] = (noise * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    AmbientSound.RAIN -> {
                        // Filtered pink noise with droplet peaks
                        for (i in buffer.indices) {
                            val white = Random.nextDouble() * 2.0 - 1.0
                            pinkState = 0.95 * pinkState + 0.05 * white
                            var sample = pinkState * 0.25
                            if (Random.nextDouble() < 0.002) {
                                sample += (Random.nextDouble() * 0.4)
                            }
                            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    AmbientSound.FOCUS_TONE -> {
                        // 196 Hz (G3) soft sine wave
                        val freq = 196.0
                        val step = (2.0 * Math.PI * freq) / sampleRate
                        for (i in buffer.indices) {
                            phase1 += step
                            if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                            val sample = sin(phase1) * 0.2
                            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    AmbientSound.FOREST_STREAM -> {
                        // Modulated gentle pink noise simulating trickling stream
                        for (i in buffer.indices) {
                            val white = Random.nextDouble() * 2.0 - 1.0
                            pinkState = 0.90 * pinkState + 0.10 * white
                            phase2 += 0.0005
                            val mod = (sin(phase2) + 1.2) * 0.15
                            val sample = pinkState * mod
                            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    AmbientSound.NONE -> break
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        currentSound = AmbientSound.NONE
    }
}
