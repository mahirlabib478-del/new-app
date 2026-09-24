package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Manages Adsterra Direct Link sponsor ads.
 * Opens directly in the user's default browser app.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Adsterra Direct Link URL (Bridge Page on Blogspot to bypass ISP & anti-fraud filtering)
    const val ADSTERRA_DIRECT_LINK_URL = "https://studyosblog.blogspot.com/2026/09/study-os-2x-xp-reward-margin-0-padding.html"

    /**
     * Safely opens the Adsterra Direct Link in the device's external default browser.
     */
    fun openDirectLink(context: Context, url: String = ADSTERRA_DIRECT_LINK_URL) {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch direct link: ${e.message}", e)
        }
    }
}



