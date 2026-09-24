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

    /**
     * Generates a pristine, responsive HTML page embedding the Adsterra 300x250 iframe banner.
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
                    }
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: transparent;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        overflow: hidden;
                    }
                    .ad-frame-wrapper {
                        width: 300px;
                        height: 250px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto;
                        overflow: hidden;
                    }
                </style>
            </head>
            <body>
                <div class="ad-frame-wrapper">
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



