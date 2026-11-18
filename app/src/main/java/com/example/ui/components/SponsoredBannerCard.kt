package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.DarkSurfaceVariant
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun SponsoredBannerCard(
    isPro: Boolean,
    onRemoveAdsClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagSuffix: String = "home"
) {
    if (isPro) return

    if (BuildConfig.IS_V1_EDITION) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("sponsored_banner_$tagSuffix"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = context.getString(R.string.admob_banner_unit_id)
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { adView ->
                    runCatching { adView.destroy() }
                }
            )
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsored_banner_$tagSuffix"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.72f)
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
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = context.getString(R.string.admob_banner_unit_id)
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    onRelease = { adView ->
                        runCatching { adView.destroy() }
                    }
                )
            }
        }
    }
}
