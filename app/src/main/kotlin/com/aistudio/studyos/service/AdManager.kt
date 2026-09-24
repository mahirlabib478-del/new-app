package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Manages Adsterra Direct Link sponsor ads safely.
 * Uses Chrome Custom Tabs for highest fill rate and proper browser headers.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Adsterra Direct Link URL (Bridge Page on Blogspot to bypass ISP & anti-fraud filtering)
    const val ADSTERRA_DIRECT_LINK_URL = "https://studyosblog.blogspot.com/2026/09/study-os-2x-xp-reward-margin-0-padding.html"

    /**
     * Safely opens the Adsterra Direct Link using Chrome Custom Tabs or Chrome Browser.
     * Ad networks prefer Custom Tabs over standard raw Intents because it carries full
     * browser cookies, user agent, and JavaScript capabilities without bot-filtering.
     */
    fun openDirectLink(context: Context, url: String = ADSTERRA_DIRECT_LINK_URL) {
        val uri = Uri.parse(url)
        
        // 1. Try launching with Chrome Custom Tabs (Industry Best Practice)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            
            // Set referrer to Google or web so Adsterra treats it as legitimate web traffic
            customTabsIntent.intent.putExtra(
                Intent.EXTRA_REFERRER,
                Uri.parse("https://www.google.com")
            )
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Prefer Chrome package if available
            customTabsIntent.intent.setPackage("com.android.chrome")
            customTabsIntent.launchUrl(context, uri)
            return
        } catch (e: Exception) {
            Log.d(TAG, "Chrome Custom Tabs with Chrome package not available: ${e.message}")
        }

        // 2. Fallback to generic Custom Tabs (any browser supporting Custom Tabs)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.intent.putExtra(
                Intent.EXTRA_REFERRER,
                Uri.parse("https://www.google.com")
            )
            customTabsIntent.launchUrl(context, uri)
            return
        } catch (e: Exception) {
            Log.d(TAG, "Generic Custom Tabs failed: ${e.message}")
        }

        // 3. Fallback to standard ACTION_VIEW Browser Intent
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_REFERRER, Uri.parse("https://www.google.com"))
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch direct link: ${e.message}", e)
        }
    }
}


