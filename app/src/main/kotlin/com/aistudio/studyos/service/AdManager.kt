package com.aistudio.studyos.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Manages Adsterra ads (300x250 In-App Rewarded Banner and Direct Links).
 */
object AdManager {
    private const val TAG = "AdManager"

    // Adsterra 300x250 Banner Unit Configuration
    const val ADSTERRA_BANNER_KEY = "b8de7d3f49db030cbee79a8f1f39dbf6"
    const val ADSTERRA_BANNER_SCRIPT_URL = "https://www.highrevenueformat.com/b8de7d3f49db030cbee79a8f1f39dbf6/invoke.js"
    const val ADSTERRA_APPROVED_BASE_URL = "https://studyosblog.blogspot.com/"

    // Adsterra Direct Link URL (Bridge Page on Blogspot to bypass ISP & anti-fraud filtering)
    const val ADSTERRA_DIRECT_LINK_URL = "https://studyosblog.blogspot.com/2026/09/study-os-2x-xp-reward-margin-0-padding.html"

    // High CPM Direct Link for 70% direct traffic
    const val PROFITABLE_DIRECT_LINK_URL = "https://www.profitableratecpmnetwork.com/jvkt09pgb?key=298992f0599b6af9f3dc8d8b5f30e40c"

    /**
     * Opens the high CPM direct sponsor page directly in the device browser.
     */
    fun openDirectSponsorLink(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PROFITABLE_DIRECT_LINK_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening direct sponsor link", e)
        }
    }

    /**
     * Generates a unique 5-digit numeric session token.
     */
    fun generateSessionToken(): String {
        return (10000..99999).random().toString()
    }

    /**
     * Calculates the deterministic secret code for a given session token.
     * Uses the exact mathematical algorithm shared with the Blogspot reward page.
     */
    fun calculateSecretCode(sessionToken: String): String {
        val cleanDigits = sessionToken.filter { it.isDigit() }
        val num = cleanDigits.toLongOrNull() ?: 54321L
        val codeNum = ((num * 379L + 128471L) % 900000L) + 100000L
        return "XP-$codeNum"
    }

    /**
     * Verifies if the user-entered secret code matches the session token.
     * Tolerant to optional 'XP-' prefix, spacing, and case.
     */
    fun verifySecretCode(sessionToken: String, inputCode: String): Boolean {
        val expected = calculateSecretCode(sessionToken)
        val cleanExpected = expected.replace("XP-", "").trim()
        val cleanInput = inputCode.uppercase()
            .replace("XP-", "")
            .replace("XP", "")
            .trim()
        return cleanInput.isNotEmpty() && cleanInput == cleanExpected
    }

    /**
     * Opens the Blogspot verification page in the device browser with the unique session parameter.
     */
    fun openBlogRewardSession(context: Context, sessionToken: String, baseUrl: String = ADSTERRA_DIRECT_LINK_URL) {
        val urlWithSession = if (baseUrl.contains("?")) {
            "$baseUrl&sid=$sessionToken"
        } else {
            "$baseUrl?sid=$sessionToken"
        }
        openDirectLink(context, urlWithSession)
    }

    /**
     * Generates a pristine, responsive HTML page embedding the Adsterra 300x250 iframe banner.
     * Includes a sleek fallback creative if the ad network has a temporary fill shortage.
     */
    fun getBanner300x250Html(
        key: String = ADSTERRA_BANNER_KEY,
        scriptUrl: String = ADSTERRA_BANNER_SCRIPT_URL
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * {
                        box-sizing: border-box;
                        -webkit-tap-highlight-color: transparent;
                        margin: 0;
                        padding: 0;
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: transparent;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        overflow: hidden;
                    }
                    .ad-frame-wrapper {
                        position: relative;
                        width: 300px;
                        height: 250px;
                        border-radius: 12px;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto;
                        background: #0f172a;
                    }
                    .ad-frame-wrapper iframe {
                        position: absolute !important;
                        top: 0 !important;
                        left: 0 !important;
                        width: 300px !important;
                        height: 250px !important;
                        z-index: 10 !important;
                        border: none !important;
                    }
                    .fallback-sponsor {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        text-align: center;
                        padding: 20px;
                        background: linear-gradient(145deg, #1e293b, #0f172a);
                        color: #ffffff;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        cursor: pointer;
                        z-index: 1;
                    }
                    .fallback-icon {
                        width: 48px;
                        height: 48px;
                        border-radius: 50%;
                        background: linear-gradient(135deg, #f59e0b, #d97706);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 24px;
                        margin-bottom: 12px;
                        box-shadow: 0 4px 14px rgba(245, 158, 11, 0.4);
                    }
                    .fallback-title {
                        font-size: 16px;
                        font-weight: 700;
                        color: #ffffff;
                        margin-bottom: 4px;
                    }
                    .fallback-desc {
                        font-size: 12px;
                        color: #94a3b8;
                        margin-bottom: 16px;
                        max-width: 220px;
                        line-height: 1.4;
                    }
                    .fallback-btn {
                        background: #f59e0b;
                        color: #ffffff;
                        padding: 8px 18px;
                        border-radius: 10px;
                        font-size: 12px;
                        font-weight: 700;
                        letter-spacing: 0.5px;
                        box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
                    }
                </style>
            </head>
            <body>
                <div class="ad-frame-wrapper">
                    <div class="fallback-sponsor" onclick="window.location.href='${ADSTERRA_DIRECT_LINK_URL}'">
                        <div class="fallback-icon">⚡</div>
                        <div class="fallback-title">Study OS Sponsor</div>
                        <div class="fallback-desc">Support Study OS and double your XP bonus for this session!</div>
                        <div class="fallback-btn">Visit Sponsor ↗</div>
                    </div>
                    <script type="text/javascript">
                        atOptions = {
                            'key' : '$key',
                            'format' : 'iframe',
                            'height' : 250,
                            'width' : 300,
                            'params' : {}
                        };
                    </script>
                    <script type="text/javascript" src="$scriptUrl"></script>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

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



