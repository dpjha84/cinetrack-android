package com.example.ui.components

import android.util.Log
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.CinemaGold
import com.example.util.TrailerInterstitialAdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun SponsoredBannerCard(
    isPro: Boolean,
    onRemoveAdsClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagSuffix: String = "home"
) {
    var adInitFailed by remember { mutableStateOf(false) }
    if (isPro || adInitFailed) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsored_banner_$tagSuffix"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SPONSORED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemoveAdsClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("remove_ads_cta_$tagSuffix")
                ) {
                    Text(
                        text = "Remove Ads • WatchPilot Pro",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                contentAlignment = Alignment.Center
            ) {
                AdMobBannerHost(onAdInitFailed = { adInitFailed = true })
            }
        }
    }
}

@Composable
private fun AdMobBannerHost(
    onAdInitFailed: () -> Unit
) {
    val isAdsSdkReady by TrailerInterstitialAdManager.isAdsSdkInitialized.collectAsStateWithLifecycle()
    if (!isAdsSdkReady) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(lifecycleOwner, adViewRef) {
        val currentAdView = adViewRef ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { currentAdView.resume() }
                Lifecycle.Event.ON_PAUSE -> runCatching { currentAdView.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            val unitId = runCatching { context.getString(R.string.admob_banner_unit_id) }
                .getOrDefault("")
                .trim()
            if (unitId.isBlank()) {
                onAdInitFailed()
                return@AndroidView View(context)
            }
            try {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = unitId
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("SponsoredBannerCard", "Banner ad skipped (${error.code}): ${error.message}")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }.also { adViewRef = it }
            } catch (t: Throwable) {
                Log.w("SponsoredBannerCard", "AdView creation skipped safely: ${t.message}")
                onAdInitFailed()
                View(context)
            }
        },
        onRelease = { view ->
            if (view is AdView) {
                if (adViewRef === view) {
                    adViewRef = null
                }
                runCatching { view.pause() }
                runCatching { view.destroy() }
            }
        }
    )
}
