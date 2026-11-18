package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.MonetizationState
import com.example.data.model.OttMoneySaverSummary
import com.example.data.model.OttRecommendation
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.util.OttMoneySaverCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OttMoneySaverSheet(
    monetizationState: MonetizationState,
    summary: OttMoneySaverSummary,
    effectiveRegionCode: String,
    onDismiss: () -> Unit,
    onToggleProvider: (String) -> Unit,
    onToggleStreamOnMyAppsOnly: (Boolean) -> Unit,
    onUpgradePro: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = Modifier.testTag("ott_money_saver_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OTT Money-Saver Radar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Audit your streaming subscriptions against your $effectiveRegionCode Watchlist",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close OTT Money-Saver")
                }
            }

            Text(
                text = "Select the OTT apps you currently pay for:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OttMoneySaverCalculator.supportedServices.forEach { svc ->
                    val selected = svc.canonicalName in monetizationState.subscribedProviders
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleProvider(svc.canonicalName) },
                        label = {
                            Text(
                                text = "${svc.shortLabel} ($${String.format(Locale.US, "%.0f", svc.defaultMonthlyUsd)})",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = DarkBackground
                        )
                    )
                }
            }

            // "On My Apps" Filter Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filter Library to 'On My Apps'",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (monetizationState.isPro) {
                                "Only show Watchlist & Library items included on your selected apps."
                            } else {
                                "WatchPilot Pro • Hide rentals & unowned OTT services in Library."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = monetizationState.streamOnMyAppsOnly,
                        onCheckedChange = onToggleStreamOnMyAppsOnly,
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = CinemaGold)
                    )
                }
            }

            if (summary.subscribedCount == 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Tap the streaming apps you subscribe to above — WatchPilot calculates how many of your ${summary.totalAuditedWatchlistCount} active Watchlist/Watching titles stream on each service and highlights unused subscriptions you can pause!",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // Spend & Savings Hero Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("Est. Monthly Spend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", summary.estimatedMonthlySpendUsd)}/mo",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${summary.coveredWatchlistCount}/${summary.totalAuditedWatchlistCount} watchlist titles covered",
                                fontSize = 11.sp,
                                color = CinemaCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Potential Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", summary.potentialMonthlySavingsUsd)}/mo",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (summary.potentialMonthlySavingsUsd > 0f) CinemaGreen else CinemaGold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Pause 0-match apps",
                                fontSize = 11.sp,
                                color = CinemaGreen,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (!monetizationState.isPro) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CinemaGold.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, CinemaGold),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Savings, contentDescription = null, tint = CinemaGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unlock Full Per-Service Breakdown",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "We detected $${String.format(Locale.US, "%.2f", summary.potentialMonthlySavingsUsd)}/mo in potential savings across your ${summary.subscribedCount} apps. Upgrade to WatchPilot Pro ($0.99/mo annual) to see exact 'Pause & Save' recommendations!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onUpgradePro,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CinemaGold,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unlock OTT Money-Saver • Pro", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.items.forEach { audit ->
                            val badgeColor = when (audit.recommendation) {
                                OttRecommendation.PAUSE_AND_SAVE -> CinemaRed
                                OttRecommendation.CONSIDER_ROTATING -> CinemaGold
                                OttRecommendation.KEEP_ACTIVE -> CinemaGreen
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${audit.providerName} (~$${String.format(Locale.US, "%.2f", audit.monthlyCostEstimateUsd)}/mo)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = audit.recommendation.badgeText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (audit.matchingTitlesCount == 0) {
                                            "0 titles from your active Watchlist/Watching list stream here in $effectiveRegionCode."
                                        } else {
                                            "${audit.matchingTitlesCount} match(es): ${audit.matchingTitlesPreview.joinToString(", ")}"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalComplianceDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Privacy, Terms & Data Attribution", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TMDB & JustWatch Attribution",
                    fontWeight = FontWeight.Bold,
                    color = CinemaGold,
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.tmdb_attribution),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Privacy & Data Ownership",
                    fontWeight = FontWeight.Bold,
                    color = CinemaGold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (BuildConfig.IS_V1_EDITION) {
                        "Your watchlist, episode progress, ratings, and custom collections are stored locally on your device and optionally synced to your private Google Cloud account when signed in with Google. You can permanently delete your account and cloud data anytime from Profile."
                    } else {
                        "Your watchlist, episode progress, ratings, and custom collections are stored locally on your device and optionally synced to your private Google Cloud account when signed in with Google. You can export your data or permanently delete your account and cloud data anytime from Profile."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (BuildConfig.IS_V1_EDITION) "Advertising" else "Subscriptions & Advertising",
                    fontWeight = FontWeight.Bold,
                    color = CinemaGold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (BuildConfig.IS_V1_EDITION) {
                        "WatchPilot displays clearly labeled inline banner ads and periodic (time-capped) trailer interstitial ads via Google AdMob so all core movie & series tracking features remain 100% free."
                    } else {
                        "WatchPilot Free displays clearly labeled inline sponsored banners via Google AdMob. WatchPilot Pro subscriptions are billed and managed through Google Play and can be canceled anytime in Google Play Subscriptions."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CinemaGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}
