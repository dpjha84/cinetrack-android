package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.billing.MonetizationManager
import com.example.data.model.MonetizationState
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.WatchPilotColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPilotProPaywallSheet(
    monetizationState: MonetizationState,
    triggerReason: String?,
    onDismiss: () -> Unit,
    onPurchaseSku: (Activity, String) -> Unit,
    onRestorePurchases: () -> Unit,
    onActivateSandboxPro: () -> Unit,
    onDeactivateSandboxPro: () -> Unit,
    onWatchRewardedTrialAd: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSku by remember { mutableStateOf(MonetizationManager.SKU_PRO_ANNUAL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("watchpilot_pro_paywall_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(CinemaGold, CinemaCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "WatchPilot Pro",
                            tint = DarkBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WatchPilot Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val paywallColors = WatchPilotColors.current
                        Text(
                            text = "Track Smarter • Decide Faster • Save on OTT",
                            fontSize = 12.sp,
                            color = paywallColors.goldAccent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Paywall",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!triggerReason.isNullOrBlank()) {
                val reasonColors = WatchPilotColors.current
                Card(
                    colors = CardDefaults.cardColors(containerColor = reasonColors.goldChipContainer),
                    border = BorderStroke(1.dp, reasonColors.goldChipBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = triggerReason,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Value Pillars
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProBenefitRow(
                        icon = Icons.Filled.Savings,
                        title = "OTT Money-Saver Radar & Streaming Alerts",
                        subtitle = "Spot idle paid apps, filter Library to 'On My Apps', and get New Season & streaming arrival alerts."
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.PhotoCamera,
                        title = "Unlimited AI Screenshot Vision Scanner",
                        subtitle = "Turn Reels, TikToks & Top-10 list screenshots into instant TMDB watchlist items."
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.Favorite,
                        title = "Unlimited Live Couple & Group Swipe-to-Match",
                        subtitle = "Host unlimited 6-digit rooms to agree on tonight's movie in under 60 seconds."
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Unlimited AI Concierge & Spoiler-Free Recaps",
                        subtitle = "Get Tonight's Pick plus personalized Watch-Fit & spoiler-free 'Previously On...' recaps."
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.Insights,
                        title = "Cinema DNA Insights + CSV/JSON Backup",
                        subtitle = "Unlock Completion Rate, Era DNA breakdown, unlimited collections & portable backups."
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.Block,
                        title = "100% Ad-Free Experience",
                        subtitle = "Zero sponsored banners or trailer interstitials across WatchPilot."
                    )
                }
            }

            // Plan Selector
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanOptionCard(
                    title = "Annual Pro",
                    price = monetizationState.annualPriceLabel,
                    badge = "BEST VALUE • SAVE 50%",
                    subtitle = "Less than $1/mo — pays for itself by pausing 1 unused OTT app.",
                    selected = selectedSku == MonetizationManager.SKU_PRO_ANNUAL,
                    onClick = { selectedSku = MonetizationManager.SKU_PRO_ANNUAL },
                    testTag = "plan_annual_card"
                )
                PlanOptionCard(
                    title = "Monthly Pro",
                    price = monetizationState.monthlyPriceLabel,
                    badge = null,
                    subtitle = "Flexible month-to-month access, cancel anytime in Google Play.",
                    selected = selectedSku == MonetizationManager.SKU_PRO_MONTHLY,
                    onClick = { selectedSku = MonetizationManager.SKU_PRO_MONTHLY },
                    testTag = "plan_monthly_card"
                )
                PlanOptionCard(
                    title = "Lifetime Founders Unlock",
                    price = monetizationState.lifetimePriceLabel,
                    badge = "PAY ONCE • OWN FOREVER",
                    subtitle = "One-time purchase, zero recurring subscription fees.",
                    selected = selectedSku == MonetizationManager.SKU_PRO_LIFETIME,
                    onClick = { selectedSku = MonetizationManager.SKU_PRO_LIFETIME },
                    testTag = "plan_lifetime_card"
                )
            }

            if (!monetizationState.billingStatusMessage.isNullOrBlank()) {
                Text(
                    text = monetizationState.billingStatusMessage,
                    fontSize = 12.sp,
                    color = CinemaCyan,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = {
                    val act = context as? Activity
                    if (act != null) {
                        onPurchaseSku(act, selectedSku)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_play_billing_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = DarkBackground
                )
            ) {
                Text(
                    text = if (monetizationState.isPro) "Active: ${monetizationState.planType.displayLabel}" else "Continue with Google Play",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            if (!monetizationState.isPro) {
                RewardedTrialPassBanner(
                    monetizationState = monetizationState,
                    sourceTag = "paywall",
                    onRewardEarned = {
                        onWatchRewardedTrialAd()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (BuildConfig.DEBUG) Arrangement.spacedBy(8.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier
                        .then(if (BuildConfig.DEBUG) Modifier.weight(1f) else Modifier)
                        .testTag("restore_purchases_button")
                ) {
                    Text(
                        text = "Restore Purchases",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (BuildConfig.DEBUG) {
                    if (monetizationState.isPro) {
                        OutlinedButton(
                            onClick = onDeactivateSandboxPro,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("switch_to_free_sandbox_button")
                        ) {
                            Text(
                                text = "Switch to Free (Sandbox)",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onActivateSandboxPro()
                                onDismiss()
                            },
                            border = BorderStroke(1.dp, CinemaCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("try_pro_sandbox_button")
                        ) {
                            Text(
                                text = "Pro Preview (Sandbox)",
                                color = CinemaCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Text(
                text = "Subscriptions auto-renew via Google Play unless canceled at least 24 hours before renewal. Watchlist tracking, CSV import & Google Cloud Sync remain 100% free forever.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProBenefitRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CinemaGold,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    badge: String?,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val planColors = WatchPilotColors.current
    val borderColor = if (selected) planColors.goldChipBorder else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val bgColor = if (selected) planColors.goldChipContainer else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (selected) planColors.goldAccent else Color.Gray.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = price,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (selected) planColors.goldAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (badge != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(planColors.greenChipContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = planColors.greenAccent
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
