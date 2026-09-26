package com.aistudio.studyos.service

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SponsorWebViewActivity : Activity() {
    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_CONFIRMATION_MESSAGE = "extra_confirmation_message"
        private const val COUNTDOWN_MS = 7000L
    }

    private lateinit var webView: WebView
    private lateinit var timerText: TextView
    private lateinit var backButton: Button
    private var canClose = false
    private var countdownTimer: CountDownTimer? = null
    private var confirmationMessage = "🎉 XP Added!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmationMessage = intent.getStringExtra(EXTRA_CONFIRMATION_MESSAGE) ?: "🎉 XP Added!"
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 8, 12, 8)
            setBackgroundColor(Color.WHITE)
        }

        timerText = TextView(this).apply {
            text = "Please wait 7s"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER_VERTICAL
        }

        backButton = Button(this).apply {
            text = "Back"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { closeSponsorPage() }
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.TRANSPARENT)
            }
        }

        toolbar.addView(timerText, LinearLayout.LayoutParams(0, 52, 1f))
        toolbar.addView(backButton, LinearLayout.LayoutParams(100, 52))

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.scheme != "http" && request.url.scheme != "https"
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64))
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        countdownTimer = object : CountDownTimer(COUNTDOWN_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999L) / 1000L).toInt()
                timerText.text = "Please wait ${seconds}s"
            }

            override fun onFinish() {
                canClose = true
                timerText.text = "Ready"
                backButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeSponsorPage() {
        if (!canClose) return
        Toast.makeText(this, confirmationMessage, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onBackPressed() {
        if (!canClose) return
        closeSponsorPage()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
