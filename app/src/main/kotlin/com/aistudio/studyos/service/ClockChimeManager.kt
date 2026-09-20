package com.aistudio.studyos.service

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Short clock-like transition chimes for automatic Focus/Break changes.
 * Uses Android's built-in tone generator, so no audio asset or permission is needed.
 */
object ClockChimeManager {
    private const val TONE_DURATION_MS = 140
    private const val GAP_MS = 110

    fun playFocusToBreak() {
        playSequence(2)
    }

    fun playBreakToFocus() {
        playSequence(1)
    }

    private fun playSequence(count: Int) {
        Thread {
            val tone = runCatching {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            }.getOrNull() ?: return@Thread

            try {
                repeat(count) { index ->
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
                    if (index < count - 1) Thread.sleep(GAP_MS.toLong())
                }
                Thread.sleep(TONE_DURATION_MS.toLong())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                tone.release()
            }
        }.start()
    }
}
