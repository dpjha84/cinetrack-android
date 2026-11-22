package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages AdMob Rewarded Video Ads for WatchPilot's capped
 * "Watch an Ad to Try Pro Once" hybrid pass.
 */
object RewardedTrialAdManager {
    private const val TAG = "RewardedTrialAdManager"

    @Volatile
    private var rewardedAd: RewardedAd? = null

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun isRewardedAdConfigured(context: Context): Boolean {
        val unitId = runCatching { context.getString(R.string.admob_rewarded_unit_id) }
            .getOrDefault("")
            .trim()
        return unitId.isNotBlank()
    }

    fun preload(context: Context, isPro: Boolean) {
        if (isPro || rewardedAd != null || _isLoading.value) return
        if (!TrailerInterstitialAdManager.isAdsSdkInitialized.value) return

        val appContext = context.applicationContext ?: context
        val unitId = runCatching { appContext.getString(R.string.admob_rewarded_unit_id) }
            .getOrDefault("")
            .trim()
        if (unitId.isBlank()) return

        _isLoading.value = true
        try {
            RewardedAd.load(
                appContext,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        _isAdReady.value = true
                        _isLoading.value = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        _isAdReady.value = false
                        _isLoading.value = false
                        Log.w(TAG, "Rewarded ad failed to load (${error.code}): ${error.message}")
                    }
                }
            )
        } catch (t: Throwable) {
            rewardedAd = null
            _isAdReady.value = false
            _isLoading.value = false
            Log.w(TAG, "RewardedAd.load skipped safely: ${t.message}")
        }
    }

    fun showRewardedTrialAd(
        activity: Activity?,
        canWatchNow: Boolean,
        onRewardEarned: () -> Unit,
        onUnavailable: (String) -> Unit,
        onDismissedWithoutReward: () -> Unit = {}
    ) {
        if (!canWatchNow) {
            onUnavailable("Ad trial pass cap reached. Upgrade to WatchPilot Pro for unlimited access.")
            return
        }
        if (activity == null) {
            onUnavailable("Unable to display ad right now.")
            return
        }

        val unitId = runCatching { activity.getString(R.string.admob_rewarded_unit_id) }
            .getOrDefault("")
            .trim()
        if (unitId.isBlank()) {
            onUnavailable("Rewarded ads are not configured for this build yet. Upgrade to WatchPilot Pro to unlock.")
            return
        }

        val loadedAd = rewardedAd
        if (loadedAd == null) {
            preload(activity.applicationContext, isPro = false)
            onUnavailable("Preparing short video ad — please tap again in a moment.")
            return
        }

        rewardedAd = null
        _isAdReady.value = false
        var earnedReward = false

        loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earnedReward) {
                    onRewardEarned()
                } else {
                    onDismissedWithoutReward()
                }
                preload(activity.applicationContext, isPro = false)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                onUnavailable("Video ad couldn't start right now. Please try again.")
                preload(activity.applicationContext, isPro = false)
            }
        }

        try {
            loadedAd.show(activity) { _ ->
                earnedReward = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "RewardedAd.show skipped safely: ${t.message}")
            onUnavailable("Video ad unavailable right now. Please try again.")
        }
    }
}
