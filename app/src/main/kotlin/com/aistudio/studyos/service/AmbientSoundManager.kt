package com.aistudio.studyos.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎵 Dual-Engine Ambient Sound & Uploaded Audio Manager.
 * Allows playing procedural background ambient noise (Rain, White Noise, Deep Focus, Forest Stream)
 * SIMULTANEOUSLY alongside uploaded audio files (Audiobooks, study podcasts, lectures, or music).
 * Each channel has its own independent Play/Pause toggle and Volume control.
 */
object AmbientSoundManager {
    enum class Preset(val label: String) {
        RAIN("Gentle Rain"),
        WHITE_NOISE("White Noise"),
        DEEP_FOCUS("Deep Focus 196Hz"),
        FOREST_STREAM("Forest Stream"),
        CUSTOM_AUDIO("Custom Audio")
    }

    // ==========================================
    // 🌧️ CHANNEL 1: Procedural Ambient Sound
    // ==========================================
    @Volatile
    private var track: AudioTrack? = null
    @Volatile
    private var worker: Thread? = null
    @Volatile
    private var isAmbientRunning = false
    @Volatile
    private var ambientVolume = 0.50f
    @Volatile
    private var currentPreset = Preset.RAIN

    @Synchronized
    fun isAmbientPlaying(): Boolean = isAmbientRunning

    @Synchronized
    fun getCurrentPreset(): Preset = currentPreset

    @Synchronized
    fun getAmbientVolume(): Float = ambientVolume

    @Synchronized
    fun setAmbientPreset(preset: Preset) {
        currentPreset = preset
    }

    @Synchronized
    fun playAmbient(preset: Preset = currentPreset, volume: Float = ambientVolume) {
        currentPreset = preset
        ambientVolume = volume.coerceIn(0f, 1f)
        stopAmbient()

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
        isAmbientRunning = true
        audioTrack.play()

        worker = Thread({
            val samples = ShortArray(sampleRate / 10)
            val random = Random.Default
            var phase = 0.0
            var rainState = 0.0
            while (isAmbientRunning && track === audioTrack) {
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
                        Preset.CUSTOM_AUDIO -> 0.0
                    }
                    samples[i] = (value * ambientVolume * Short.MAX_VALUE).toInt().coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    ).toShort()
                }
                if (audioTrack.write(samples, 0, samples.size) < 0) break
            }
        }, "StudyOS-AmbientEngine")
        worker?.start()
    }

    @Synchronized
    fun stopAmbient() {
        isAmbientRunning = false
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

    @Synchronized
    fun setAmbientVolume(volume: Float) {
        ambientVolume = volume.coerceIn(0f, 1f)
        track?.setVolume(ambientVolume)
    }

    // ==========================================
    // 🎧 CHANNEL 2: Uploaded Custom Audio
    // ==========================================
    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var customAudioUri: String? = null
    @Volatile
    private var customAudioName: String? = null
    @Volatile
    private var customVolume = 0.80f
    @Volatile
    private var isCustomAudioPlaying = false

    @Synchronized
    fun isCustomAudioPlaying(): Boolean = isCustomAudioPlaying && mediaPlayer?.isPlaying == true

    @Synchronized
    fun getCustomAudioName(): String? = customAudioName

    @Synchronized
    fun getCustomAudioUri(): String? = customAudioUri

    @Synchronized
    fun getCustomAudioVolume(): Float = customVolume

    @Synchronized
    fun setCustomAudioMetadata(uri: String?, displayName: String?) {
        customAudioUri = uri
        customAudioName = displayName
    }

    @Synchronized
    fun playCustomAudio(
        context: Context,
        uriString: String = customAudioUri ?: "",
        displayName: String? = customAudioName,
        volume: Float = customVolume
    ) {
        if (uriString.isBlank()) return
        val uriChanged = (customAudioUri != uriString)
        customAudioUri = uriString
        if (displayName != null) customAudioName = displayName
        customVolume = volume.coerceIn(0f, 1f)

        // If already playing the same URI, just adjust volume and return
        if (!uriChanged && mediaPlayer != null && isCustomAudioPlaying) {
            mediaPlayer?.setVolume(customVolume, customVolume)
            return
        }

        stopCustomAudio()

        var candidateMp: MediaPlayer? = null
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (uriString.startsWith("content://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else if (uriString.startsWith("file://")) {
                    val path = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                    setDataSource(path)
                } else {
                    setDataSource(uriString)
                }
                isLooping = true
                setVolume(customVolume, customVolume)
                setOnErrorListener { _, _, _ ->
                    isCustomAudioPlaying = false
                    runCatching {
                        mediaPlayer?.reset()
                        mediaPlayer?.release()
                    }
                    mediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            candidateMp = mp
            mediaPlayer = mp
            isCustomAudioPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
            isCustomAudioPlaying = false
            runCatching {
                candidateMp?.release()
            }
        }
    }

    @Synchronized
    fun stopCustomAudio() {
        isCustomAudioPlaying = false
        val mp = mediaPlayer
        mediaPlayer = null
        runCatching {
            if (mp?.isPlaying == true) {
                mp.stop()
            }
            mp?.release()
        }
    }

    @Synchronized
    fun setCustomAudioVolume(volume: Float) {
        customVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(customVolume, customVolume)
    }

    @Synchronized
    fun setCustomAudio(uri: String?, displayName: String?) {
        setCustomAudioMetadata(uri, displayName)
    }

    @Synchronized
    fun setVolume(volume: Float) {
        setAmbientVolume(volume)
        setCustomAudioVolume(volume)
    }

    @Synchronized
    fun play(
        context: Context,
        preset: Preset = currentPreset,
        volume: Float = ambientVolume,
        customUri: String? = customAudioUri
    ) {
        if (preset == Preset.CUSTOM_AUDIO) {
            val uriToUse = customUri ?: customAudioUri ?: ""
            if (uriToUse.isNotBlank()) {
                playCustomAudio(context, uriToUse, customAudioName, customVolume)
            }
        } else {
            playAmbient(preset, volume)
        }
    }

    // ==========================================
    // 🌐 Unified Stop & Status
    // ==========================================
    @Synchronized
    fun stopAll() {
        stopAmbient()
        stopCustomAudio()
    }

    @Synchronized
    fun stop() {
        stopAll()
    }

    fun isPlaying(): Boolean = isAmbientPlaying() || isCustomAudioPlaying()
}
