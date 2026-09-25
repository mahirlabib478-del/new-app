package com.aistudio.studyos.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.studyos.service.AdManager
import kotlinx.coroutines.delay

enum class SponsorRewardMode {
    CLAIM_2X,
    CLAIM_3X_SECRET_KEY
}

/**
 * Fullscreen In-App Sponsor Viewer.
 * - Claim 2X: Loads direct sponsor page. Strictly 7s countdown, then "Done" button appears.
 * - Claim 3X: Loads Adsterra 300x250 Banner Portal. When the user CLICKS the banner,
 *   a 7-second countdown runs, then "🔑 Secret Key Generated: XP-XXXXXX" and "Back Now" button appear.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DirectSponsorRewardDialog(
    rewardXP: Int,
    mode: SponsorRewardMode = SponsorRewardMode.CLAIM_2X,
    url: String = if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) AdManager.ADSTERRA_DIRECT_LINK_URL else AdManager.PROFITABLE_DIRECT_LINK_URL,
    existingSecretCode: String? = null,
    onDismiss: () -> Unit,
    onRewardEarned: (String?) -> Unit
) {
    val sessionToken = remember { AdManager.generateSessionToken() }
    val generatedKey = remember {
        existingSecretCode ?: AdManager.calculateSecretCode(sessionToken)
    }

    var remainingSeconds by remember { mutableIntStateOf(7) }
    var rewardGranted by remember { mutableStateOf(false) }
    // In Claim 2X, timer starts immediately. In Claim 3X, timer starts ONLY when the banner is clicked!
    var bannerClicked by remember { mutableStateOf(mode == SponsorRewardMode.CLAIM_2X) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoading by remember { mutableStateOf(true) }

    // 7-second countdown starts once bannerClicked is true
    LaunchedEffect(bannerClicked) {
        if (bannerClicked) {
            for (sec in 7 downTo 1) {
                remainingSeconds = sec
                delay(1000L)
            }
            remainingSeconds = 0
            if (!rewardGranted) {
                rewardGranted = true
                onRewardEarned(if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) generatedKey else null)
            }
        }
    }

    // Dismiss only permitted after 7 seconds
    BackHandler {
        if (rewardGranted) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (rewardGranted) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = rewardGranted,
            dismissOnClickOutside = false
        )
    ) {
        val bannerBgColor by animateColorAsState(
            targetValue = if (rewardGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.surface,
            animationSpec = tween(400),
            label = "bannerBg"
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Header / XP Added Banner right over the ad page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerBgColor)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (!rewardGranted) {
                        // ⏳ Waiting or 7-second countdown: Strictly NO Done / Back / Close button!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (!bannerClicked) Icons.Default.TouchApp else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY && !bannerClicked) {
                                        Text(
                                            text = "Tap Banner Below",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Click banner to start 7s key generation",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) "Generating 3X Key..." else "Viewing Sponsor Offer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY)
                                                "Secret key ready in ${remainingSeconds}s..."
                                            else
                                                "Done button appears in ${remainingSeconds}s...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Timer Badge (No close button allowed until 7s)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = if (!bannerClicked) "Tap Ad" else "${remainingSeconds}s",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        // ⭐ Exactly at 7 seconds: The button appears!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) Icons.Default.Key else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) {
                                        Text(
                                            text = "🔑 Secret Key Generated!",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Key: $generatedKey • 3X XP Ready",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    } else {
                                        Text(
                                            text = "🎉 +$rewardXP XP Added!",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Credited to your balance",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }

                            // The button strictly appears after 7 seconds!
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) "Back Now" else "Done",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Progress Bar below header
                if (!rewardGranted && bannerClicked) {
                    val progress = ((7 - remainingSeconds) / 7f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // In-App Webview displaying the sponsor ad page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = settings.userAgentString + " StudyOS/Mobile"
                                }
                                webChromeClient = WebChromeClient()

                                // JavaScript Bridge for Website Communication
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onBannerClick() {
                                            post {
                                                if (!bannerClicked) {
                                                    bannerClicked = true
                                                }
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onKeyUnlocked(code: String) {
                                            post {
                                                rewardGranted = true
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onBackNow() {
                                            post {
                                                if (rewardGranted) {
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    },
                                    "StudyOSBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        pageLoading = true
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        pageLoading = false
                                        // Inject safety bridge hooks in case external webpage is loaded
                                        evaluateJavascript(
                                            """
                                            (function() {
                                                var adEl = document.getElementById('ad-wrapper');
                                                if (adEl) {
                                                    adEl.addEventListener('click', function() {
                                                        if (window.StudyOSBridge) window.StudyOSBridge.onBannerClick();
                                                    });
                                                }
                                                window.addEventListener('blur', function() {
                                                    if (window.StudyOSBridge) window.StudyOSBridge.onBannerClick();
                                                });
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }

                                // Load appropriate content
                                if (mode == SponsorRewardMode.CLAIM_3X_SECRET_KEY) {
                                    val portalHtml = AdManager.generatePortalHtml(sessionToken, 3)
                                    loadDataWithBaseURL(
                                        AdManager.ADSTERRA_APPROVED_BASE_URL,
                                        portalHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                } else {
                                    loadUrl(url)
                                }
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (pageLoading && remainingSeconds > 5 && mode == SponsorRewardMode.CLAIM_2X) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "Loading sponsor offer...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
