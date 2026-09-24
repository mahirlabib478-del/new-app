package com.aistudio.studyos.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aistudio.studyos.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages Google AdMob Interstitial Ad loading and presentation safely.
 *
 * In DEBUG builds, it uses Google's official test interstitial ad unit ID
 * to guarantee compliance with AdMob policies and prevent account penalties.
 * In RELEASE builds, it uses the production ad unit ID configured by the owner.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google sample/test interstitial ad unit ID
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    // Production interstitial ad unit ID created in AdMob console
    private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6286643676497425/2777765557"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    private val adUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad successfully loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows the interstitial ad if loaded.
     * Always invokes [onDismissOrUnavailable] so the UI flow and user journey
     * are never blocked or broken, whether an ad is ready or not.
     */
    fun showInterstitial(activity: Activity, onDismissOrUnavailable: () -> Unit) {
        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissOrUnavailable()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissOrUnavailable()
                }
            }
            currentAd.show(activity)
        } else {
            // No ad available at this moment; continue without interruption
            loadInterstitial(activity)
            onDismissOrUnavailable()
        }
    }
}
