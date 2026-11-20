package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages AdMob interstitial ads on trailer playback with a strict 3-minute (180s) cooldown.
 * Never blocks trailer playback if an ad is unavailable, loading, or fails to display.
 */
object TrailerInterstitialAdManager {
    private const val TAG = "TrailerInterstitialAd"
    private const val PREFS_NAME = "watchpilot_ad_prefs"
    private const val KEY_LAST_SHOWN_MS = "trailer_interstitial_last_shown_ms"

    const val INTERSTITIAL_COOLDOWN_MS: Long = 3L * 60L * 1000L // 3 minutes

    @Volatile
    private var loadedInterstitial: InterstitialAd? = null

    @Volatile
    private var isLoading: Boolean = false

    fun shouldShowInterstitialNow(
        nowEpochMs: Long,
        lastShownEpochMs: Long,
        cooldownMs: Long = INTERSTITIAL_COOLDOWN_MS
    ): Boolean {
        if (lastShownEpochMs <= 0L) return true
        val elapsed = nowEpochMs - lastShownEpochMs
        // Handle clock adjustments gracefully
        if (elapsed < 0L) return true
        return elapsed >= cooldownMs
    }

    fun releasePreloaded() {
        if (!isLoading) {
            loadedInterstitial = null
        }
    }

    fun preload(context: Context, isPro: Boolean = false) {
        if (isPro) {
            loadedInterstitial = null
            return
        }
        if (loadedInterstitial != null || isLoading) return

        val appContext = context.applicationContext
        val adUnitId = runCatching { appContext.getString(R.string.admob_interstitial_unit_id) }
            .getOrDefault("")
            .trim()
        if (adUnitId.isBlank()) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    loadedInterstitial = ad
                    Log.d(TAG, "Trailer interstitial ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    loadedInterstitial = null
                    Log.w(TAG, "Trailer interstitial load skipped (${loadAdError.code}): ${loadAdError.message}")
                }
            }
        )
    }

    fun maybeShowBeforeTrailer(
        activity: Activity?,
        isPro: Boolean,
        nowEpochMs: Long = System.currentTimeMillis(),
        onContinuePlayback: () -> Unit
    ) {
        if (isPro || activity == null || activity.isFinishing || activity.isDestroyed) {
            onContinuePlayback()
            return
        }

        val prefs = activity.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownMs = prefs.getLong(KEY_LAST_SHOWN_MS, 0L)

        if (!shouldShowInterstitialNow(nowEpochMs = nowEpochMs, lastShownEpochMs = lastShownMs)) {
            // Still inside 3-minute cooldown window — open trailer immediately
            if (loadedInterstitial == null) {
                preload(activity.applicationContext, isPro = false)
            }
            onContinuePlayback()
            return
        }

        val ad = loadedInterstitial
        if (ad == null) {
            // Ad not ready yet — never delay user's trailer click
            preload(activity.applicationContext, isPro = false)
            onContinuePlayback()
            return
        }

        loadedInterstitial = null
        var continued = false
        val proceedOnce: () -> Unit = {
            if (!continued) {
                continued = true
                onContinuePlayback()
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                prefs.edit().putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis()).apply()
            }

            override fun onAdDismissedFullScreenContent() {
                ad.fullScreenContentCallback = null
                preload(activity.applicationContext, isPro = false)
                proceedOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                ad.fullScreenContentCallback = null
                Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                preload(activity.applicationContext, isPro = false)
                proceedOnce()
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to present interstitial: ${e.message}")
            ad.fullScreenContentCallback = null
            preload(activity.applicationContext, isPro = false)
            proceedOnce()
        }
    }
}
