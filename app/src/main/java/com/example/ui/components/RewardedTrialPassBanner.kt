package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonetizationState
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.DarkBackground
import com.example.util.RewardedTrialAdManager

/**
 * Hybrid "Watch an Ad to Try Pro Once" banner with transparent anti-abuse cap status
 * (max 1 ad/day, 3 ads/month, max 2 banked credits).
 */
@Composable
fun RewardedTrialPassBanner(
    monetizationState: MonetizationState,
    sourceTag: String,
    onRewardEarned: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (monetizationState.isPro) return
    val context = LocalContext.current
    val configured = remember(context) { RewardedTrialAdManager.isRewardedAdConfigured(context) }
    if (!configured) return

    val isAdReady by RewardedTrialAdManager.isAdReady.collectAsStateWithLifecycle()
    val isLoading by RewardedTrialAdManager.isLoading.collectAsStateWithLifecycle()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monetizationState.isPro) {
        RewardedTrialAdManager.preload(context, monetizationState.isPro)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rewarded_trial_banner_$sourceTag"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CinemaCyan.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, CinemaCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (monetizationState.rewardedTrialCredits > 0) {
                            "${monetizationState.rewardedTrialCredits} Ad Trial Pass Active"
                        } else {
                            "Watch 1 Short Ad • Try Pro Once"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (monetizationState.rewardedTrialCredits > 0) CinemaGreen else CinemaCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = monetizationState.rewardedCapStatusText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (monetizationState.canWatchRewardedTrialAd) {
                    FilledTonalButton(
                        onClick = {
                            feedbackMessage = null
                            RewardedTrialAdManager.showRewardedTrialAd(
                                activity = context as? Activity,
                                canWatchNow = monetizationState.canWatchRewardedTrialAd,
                                onRewardEarned = {
                                    onRewardEarned()
                                    feedbackMessage = "Pro Trial Pass unlocked!"
                                },
                                onUnavailable = { msg ->
                                    feedbackMessage = msg
                                },
                                onDismissedWithoutReward = {
                                    feedbackMessage = "Watch the full short video ad to unlock 1 Pro Trial Pass."
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CinemaCyan,
                            contentColor = DarkBackground
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_watch_ad_trial_$sourceTag")
                    ) {
                        Icon(
                            imageVector = if (isAdReady) Icons.Filled.PlayCircleOutline else Icons.Filled.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isLoading -> "Loading..."
                                isAdReady -> "Watch Ad"
                                else -> "Load Ad"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (!feedbackMessage.isNullOrBlank()) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    fontSize = 11.sp,
                    color = CinemaCyan
                )
            }
        }
    }
}
