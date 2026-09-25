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
     * Generates the complete HTML for the StudyOS Reward Portal matching the website,
     * embedding the 300x250 Adsterra banner, click detection, 7-second countdown,
     * and seamless JavaScript bridge communication with Android.
     */
    fun generatePortalHtml(sessionToken: String, multiplier: Int = 3): String {
        val multiplierText = "${multiplier}X"
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Study OS Reward Portal</title>
                <style>
                    * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
                    body {
                        margin: 0;
                        padding: 16px 12px;
                        background: #020617;
                        color: #f8fafc;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    }
                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                    @keyframes pulseGlow {
                        0% { transform: scale(1); box-shadow: 0 0 15px rgba(245, 158, 11, 0.4); }
                        50% { transform: scale(1.04); box-shadow: 0 0 30px rgba(245, 158, 11, 0.8), 0 0 45px rgba(245, 158, 11, 0.3); }
                        100% { transform: scale(1); box-shadow: 0 0 15px rgba(245, 158, 11, 0.4); }
                    }
                    .badge-pulsing { animation: pulseGlow 1.8s infinite ease-in-out; }
                    #ad-wrapper:hover { border-color: rgba(245, 158, 11, 0.8) !important; }
                    #copy-btn:active { transform: scale(0.98); }
                </style>
            </head>
            <body>
                <div id="studyos-portal" style="max-width: 520px; margin: 0 auto; background: radial-gradient(circle at 50% 0%, #1e1b4b 0%, #0f172a 60%, #020617 100%); border-radius: 24px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.1); padding: 24px 16px; text-align: center; position: relative; overflow: hidden;">
                    <!-- Ambient Glow -->
                    <div style="position: absolute; top: -80px; left: 50%; transform: translateX(-50%); width: 280px; height: 180px; background: #6366f1; opacity: 0.25; filter: blur(60px); pointer-events: none;"></div>

                    <!-- Header -->
                    <div style="margin-bottom: 18px; position: relative; z-index: 2;">
                        <div style="display: inline-flex; align-items: center; gap: 6px; background: rgba(99, 102, 241, 0.15); border: 1px solid rgba(99, 102, 241, 0.3); padding: 5px 14px; border-radius: 30px; font-size: 11px; font-weight: 800; letter-spacing: 1px; color: #a5b4fc; text-transform: uppercase; margin-bottom: 10px;">
                            <span>⚡ STUDY OS KEY GENERATOR</span>
                        </div>
                        <h1 style="margin: 0 0 6px 0; font-size: 22px; font-weight: 900; background: linear-gradient(135deg, #ffffff 0%, #c7d2fe 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
                            Unlock $multiplierText XP Booster Key
                        </h1>
                        <p style="color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.4;">
                            Tap the sponsor banner below to generate your reward key.
                        </p>
                    </div>

                    <!-- Session Tracker -->
                    <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 14px; padding: 8px 14px; margin-bottom: 16px;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span style="width: 8px; height: 8px; background: #10b981; border-radius: 50%; box-shadow: 0 0 8px #10b981;"></span>
                            <span style="font-size: 12px; color: #cbd5e1; font-weight: 600;">Session: <b id="session-display" style="color: #38bdf8;">#$sessionToken</b></span>
                        </div>
                        <div style="font-size: 11px; background: rgba(56, 189, 248, 0.15); color: #38bdf8; padding: 4px 10px; border-radius: 8px; font-weight: 700;">
                            STEP <span id="current-step-num">1</span> OF 2
                        </div>
                    </div>

                    <!-- Step 1 Card -->
                    <div id="step1-card" style="background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; padding: 18px 12px; margin-bottom: 16px; position: relative;">
                        <!-- Status Badge -->
                        <div style="margin-bottom: 14px;">
                            <div id="status-badge" class="badge-pulsing" style="display: inline-block; background: linear-gradient(135deg, rgba(245, 158, 11, 0.25) 0%, rgba(217, 119, 6, 0.35) 100%); border: 2px solid #f59e0b; color: #fef08a; font-size: 14px; font-weight: 900; padding: 8px 20px; border-radius: 30px; letter-spacing: 0.5px; text-shadow: 0 0 12px rgba(245, 158, 11, 0.8);">
                                🔑 TAP BANNER TO GET KEY
                            </div>
                        </div>

                        <!-- 300x250 Banner Frame -->
                        <div id="ad-wrapper" style="position: relative; width: 300px; height: 250px; margin: 0 auto; background: #0b0f19; border: 2px solid #f59e0b; border-radius: 16px; overflow: hidden; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 0 25px rgba(245, 158, 11, 0.3);">
                            <div id="ad-frame-holder" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 2;">
                                <script type="text/javascript">
                                    atOptions = {
                                        'key' : '$ADSTERRA_BANNER_KEY',
                                        'format' : 'iframe',
                                        'height' : 250,
                                        'width' : 300,
                                        'params' : {}
                                    };
                                </script>
                                <script type="text/javascript" src="$ADSTERRA_BANNER_SCRIPT_URL"></script>
                            </div>
                        </div>

                        <p id="helper-text" style="color: #fbbf24; font-size: 12px; font-weight: 600; margin: 12px 0 0 0;">
                            👉 Tap the banner above to start the 7s unlock timer!
                        </p>
                    </div>

                    <!-- 7 Seconds Countdown Card -->
                    <div id="countdown-card" style="display: none; background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.25); border-radius: 18px; padding: 16px; margin-bottom: 16px;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                            <span style="color: #34d399; font-weight: 800; font-size: 13px;">⚡ Generating Reward Key...</span>
                            <span id="countdown-number" style="color: #34d399; font-size: 18px; font-weight: 900;">7s</span>
                        </div>
                        <div style="width: 100%; height: 8px; background: rgba(255, 255, 255, 0.1); border-radius: 10px; overflow: hidden;">
                            <div id="progress-bar-fill" style="width: 0%; height: 100%; background: linear-gradient(90deg, #10b981, #38bdf8); border-radius: 10px; transition: width 1s linear;"></div>
                        </div>
                    </div>

                    <!-- Unlocked Secret Code Card -->
                    <div id="code-result-card" style="display: none; background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(6, 95, 70, 0.2) 100%); border: 1px solid #10b981; border-radius: 20px; padding: 20px 16px; box-shadow: 0 0 35px rgba(16, 185, 129, 0.25); position: relative;">
                        <div style="font-size: 36px; margin-bottom: 4px;">🎉</div>
                        <div style="color: #34d399; font-size: 18px; font-weight: 900; letter-spacing: -0.3px; margin-bottom: 4px;">
                            KEY UNLOCKED!
                        </div>
                        <p style="color: #a7f3d0; font-size: 12px; margin: 0 0 14px 0; line-height: 1.4;">
                            Your $multiplierText XP secret key is ready! Tap below to return:
                        </p>

                        <div id="generated-code" style="background: rgba(0, 0, 0, 0.5); border: 2px dashed #34d399; border-radius: 12px; padding: 12px; font-size: 24px; font-weight: 900; letter-spacing: 2px; color: #6ee7b7; text-shadow: 0 0 16px rgba(52, 211, 153, 0.4); margin-bottom: 14px;">
                            XP-000000
                        </div>

                        <button id="copy-btn" onclick="onBackNowClicked()" style="background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #ffffff; border: none; padding: 14px 20px; border-radius: 12px; font-size: 15px; font-weight: 800; cursor: pointer; width: 100%; box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4); display: flex; align-items: center; justify-content: center; gap: 8px;">
                            <span>✨</span> <span id="copy-text">Claim $multiplierText XP & Back Now</span>
                        </button>
                    </div>
                </div>

                <script>
                    var sid = "$sessionToken";
                    var isBannerLoaded = true;
                    var adClicked = false;
                    var timerStarted = false;

                    function calculateSecretCode(sessionToken) {
                        var clean = sessionToken.replace(/\D/g, '');
                        var num = parseInt(clean, 10) || 54321;
                        var codeNum = ((num * 379 + 128471) % 900000) + 100000;
                        return "XP-" + codeNum;
                    }

                    var adWrapper = document.getElementById('ad-wrapper');
                    adWrapper.addEventListener('click', function() { onAdClickConfirmed(); });
                    adWrapper.addEventListener('touchstart', function() { onAdClickConfirmed(); }, {passive: true});

                    window.addEventListener('blur', function() { onAdClickConfirmed(); });

                    function onAdClickConfirmed() {
                        if (adClicked) return;
                        adClicked = true;

                        if (window.StudyOSBridge && window.StudyOSBridge.onBannerClick) {
                            window.StudyOSBridge.onBannerClick();
                        }

                        var badge = document.getElementById('status-badge');
                        badge.innerHTML = '⚡ GENERATING KEY... (7s)';
                        badge.classList.remove('badge-pulsing');
                        badge.style.background = 'linear-gradient(135deg, #10b981 0%, #059669 100%)';
                        badge.style.borderColor = '#34d399';
                        badge.style.color = '#ffffff';

                        document.getElementById('countdown-card').style.display = 'block';
                        startCountdown();
                    }

                    function startCountdown() {
                        if (timerStarted) return;
                        timerStarted = true;

                        var totalTime = 7;
                        var timeLeft = 7;
                        var countdownNum = document.getElementById('countdown-number');
                        var progressFill = document.getElementById('progress-bar-fill');

                        var interval = setInterval(function() {
                            timeLeft--;
                            countdownNum.innerText = timeLeft + 's';
                            var percent = ((totalTime - timeLeft) / totalTime) * 100;
                            progressFill.style.width = percent + '%';

                            if (timeLeft <= 0) {
                                clearInterval(interval);
                                revealCode();
                            }
                        }, 1000);
                    }

                    function revealCode() {
                        document.getElementById('current-step-num').innerText = '2';
                        document.getElementById('countdown-card').style.display = 'none';
                        document.getElementById('step1-card').style.opacity = '0.35';
                        document.getElementById('step1-card').style.pointerEvents = 'none';

                        var secretCode = calculateSecretCode(sid);
                        document.getElementById('generated-code').innerText = secretCode;
                        document.getElementById('code-result-card').style.display = 'block';

                        if (window.StudyOSBridge && window.StudyOSBridge.onKeyUnlocked) {
                            window.StudyOSBridge.onKeyUnlocked(secretCode);
                        }
                    }

                    function onBackNowClicked() {
                        var codeText = document.getElementById('generated-code').innerText;
                        navigator.clipboard.writeText(codeText);
                        if (window.StudyOSBridge && window.StudyOSBridge.onBackNow) {
                            window.StudyOSBridge.onBackNow();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

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



