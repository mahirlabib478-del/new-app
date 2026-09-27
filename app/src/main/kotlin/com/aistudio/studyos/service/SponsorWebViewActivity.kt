package com.aistudio.studyos.service

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.aistudio.studyos.service.CompactToast
import androidx.core.view.WindowInsetsCompat

class SponsorWebViewActivity : Activity() {
    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_CONFIRMATION_MESSAGE = "extra_confirmation_message"
        private const val COUNTDOWN_MS = 7000L
        private const val TICK_MS = 1000L
    }

    private lateinit var webView: WebView
    private lateinit var countdownLabel: TextView
    private lateinit var countdownValue: TextView
    private lateinit var progressTrack: View
    private lateinit var backButton: TextView
    private var canClose = false
    private var countdownTimer: CountDownTimer? = null
    private var confirmationMessage = "🎉 XP Added!"

    private val backCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.window.OnBackInvokedCallback { if (canClose) handleBackNavigation() }
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmationMessage =
            intent.getStringExtra(EXTRA_CONFIRMATION_MESSAGE) ?: "🎉 XP Added!"
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(238, 241, 255))
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(0, top, 0, bottom)
            insets
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.rgb(248, 249, 255))
                setStroke(dp(1), Color.rgb(211, 217, 247))
            }
            elevation = dp(3).toFloat()
        }

        val timerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        countdownLabel = TextView(this).apply {
            text = "Please wait"
            textSize = 13f
            setTextColor(Color.rgb(74, 82, 120))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        countdownValue = TextView(this).apply {
            text = "7s"
            textSize = 21f
            setTextColor(Color.rgb(50, 58, 120))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.rgb(225, 229, 255))
                setStroke(dp(1), Color.rgb(194, 202, 244))
            }
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        backButton = TextView(this).apply {
            text = "Back"
            textSize = 14f
            setTextColor(Color.rgb(35, 39, 45))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.rgb(242, 244, 247))
                setStroke(dp(1), Color.rgb(225, 228, 233))
            }
            setPadding(dp(18), dp(8), dp(18), dp(8))
            setOnClickListener { handleBackNavigation() }
        }

        timerRow.addView(
            countdownLabel,
            LinearLayout.LayoutParams(0, dp(40), 1f)
        )
        timerRow.addView(
            countdownValue,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40))
        )
        timerRow.addView(
            backButton,
            LinearLayout.LayoutParams(dp(78), dp(40)).apply {
                leftMargin = dp(8)
            }
        )

        progressTrack = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(3).toFloat()
                setColor(Color.rgb(202, 208, 239))
            }
        }

        toolbar.addView(
            timerRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40))
        )
        toolbar.addView(
            progressTrack,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)).apply {
                topMargin = dp(7)
            }
        )

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return request.url.scheme != "http" && request.url.scheme != "https"
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            ).apply {
                leftMargin = dp(10)
                rightMargin = dp(10)
                bottomMargin = dp(8)
            }
        )
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        setContentView(root)

        startCountdown()
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(COUNTDOWN_MS, TICK_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999L) / 1000L).toInt()
                countdownValue.text = "${seconds}s"

                val progress = ((COUNTDOWN_MS - millisUntilFinished).toFloat() / COUNTDOWN_MS)
                    .coerceIn(0f, 1f)
                progressTrack.scaleX = progress.coerceAtLeast(0.02f)
                progressTrack.pivotX = 0f
            }

            override fun onFinish() {
                canClose = true
                countdownLabel.text = "Ready"
                countdownValue.text = "✓"
                countdownValue.setTextColor(Color.rgb(16, 185, 129))
                progressTrack.scaleX = 1f
                backButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun handleBackNavigation() {
        if (!canClose) return
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            closeSponsorPage()
        }
    }

    private fun closeSponsorPage() {
        if (!canClose) return
        canClose = false
        countdownTimer?.cancel()
        CompactToast.show(applicationContext, compactConfirmationMessage(confirmationMessage), Toast.LENGTH_LONG)
        finish()
    }

    private fun compactConfirmationMessage(message: String): String {
        return message.replace(Regex(" XP Added!.*$"), " XP")
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && canClose) {
            handleBackNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback?.let {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    it
                )
            }
        }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
