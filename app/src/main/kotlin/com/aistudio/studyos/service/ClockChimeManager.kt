package com.aistudio.studyos.service

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Short soft transition chime for automatic Focus/Break changes.
 * Uses Android's built-in tone generator for a crisp, soft "ting".
 */
object ClockChimeManager {
    private const val TING_DURATION_MS = 100

    fun playFocusToBreak() {
        playTing()
    }

    fun playBreakToFocus() {
        playTing()
    }

    private fun playTing() {
        Thread {
            val tone = runCatching {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 36)
            }.getOrNull() ?: return@Thread

            try {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, TING_DURATION_MS)
                Thread.sleep(TING_DURATION_MS.toLong() + 20)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                tone.release()
            }
        }.start()
    }
}
