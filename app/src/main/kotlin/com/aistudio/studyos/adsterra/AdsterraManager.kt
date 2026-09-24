package com.aistudio.studyos.adsterra

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AdsterraManager handles post-focus-session ad scheduling and direct link execution.
 * Triggers exactly 10 seconds after user finishes a focus session and taps Done on the celebration screen.
 */
object AdsterraManager {

    // User's verified Adsterra Direct Link
    const val DIRECT_LINK_URL = "https://www.profitableratecpmnetwork.com/e8vebdqa?key=daa23000512567adaa7bbb3efc276252"

    private val _isAdVisible = MutableStateFlow(false)
    val isAdVisible: StateFlow<Boolean> = _isAdVisible.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Called when the user clicks "Done" on the congratulation / session complete screen.
     * Launches a 5-second timer, then triggers the ad dialog.
     */
    fun onSessionDoneClicked(scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            delay(5_000L) // 5 seconds delay after celebration completion
            _isAdVisible.value = true
        }
    }

    /**
     * Closes the ad dialog.
     */
    fun dismissAd() {
        _isAdVisible.value = false
    }

    /**
     * Safely opens the Adsterra direct link in an external browser.
     */
    fun openDirectLink(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DIRECT_LINK_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Gracefully ignore if no browser is available
        } finally {
            dismissAd()
        }
    }
}
