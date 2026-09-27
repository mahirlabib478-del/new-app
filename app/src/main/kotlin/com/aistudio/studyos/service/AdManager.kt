package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import android.util.Log
import com.aistudio.studyos.service.SponsorWebViewActivity

/**
 * Manages Adsterra Direct Links.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Use the actual Smart Direct Link, not the Blogspot landing page.
    const val PROFITABLE_DIRECT_LINK_URL = "https://www.profitableratecpmnetwork.com/jvkt09pgb?key=298992f0599b6af9f3dc8d8b5f30e40c"

    fun openDirectLink(
        context: Context,
        url: String = PROFITABLE_DIRECT_LINK_URL,
        confirmationMessage: String = "🎉 XP Added!"
    ): Boolean {
        if (!isInternetAvailable(context)) {
            Toast.makeText(context, "Internet connection required", Toast.LENGTH_SHORT).show()
            return false
        }
        try {
            val intent = Intent(context, SponsorWebViewActivity::class.java).apply {
                putExtra(SponsorWebViewActivity.EXTRA_URL, url)
                putExtra(SponsorWebViewActivity.EXTRA_CONFIRMATION_MESSAGE, confirmationMessage)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch in-app direct link: ${e.message}", e)
            Toast.makeText(context, "Unable to open sponsor page", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private const val PREFS_NAME = "ad_rate_limit_prefs"
    private const val KEY_FOCUS_CLAIM_TIMESTAMPS = "focus_claim_timestamps"
    const val MAX_FOCUS_CLAIMS_PER_HOUR = 3
    private const val ONE_HOUR_MS = 60 * 60 * 1000L

    /**
     * Checks if the Focus Session XP claim bonus can be shown (max 3 times in 1 hour).
     */
    fun canShowFocusClaimBonus(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FOCUS_CLAIM_TIMESTAMPS, "") ?: ""
        val now = System.currentTimeMillis()
        val validTimestamps = raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { now - it < ONE_HOUR_MS }
        return validTimestamps.size < MAX_FOCUS_CLAIMS_PER_HOUR
    }

    /**
     * Records a Focus Session XP claim bonus appearance (sliding window of 1 hour).
     */
    fun recordFocusClaimAppearance(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FOCUS_CLAIM_TIMESTAMPS, "") ?: ""
        val now = System.currentTimeMillis()
        val validTimestamps = (raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { now - it < ONE_HOUR_MS } + now)
        prefs.edit().putString(KEY_FOCUS_CLAIM_TIMESTAMPS, validTimestamps.joinToString(",")).apply()
    }
}
