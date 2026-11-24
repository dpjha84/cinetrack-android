package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.ConciergePick
import com.example.data.model.ConciergeUiState
import com.example.data.model.MonetizationState
import com.example.data.model.WatchStatus
import com.example.ui.theme.WatchPilotColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConciergeSheet(
    conciergeState: ConciergeUiState,
    monetizationState: MonetizationState,
    trackedMedia: List<MediaEntity>,
    effectiveRegionCode: String,
    onDismiss: () -> Unit,
    onGeneratePicks: (mood: String, timeBudget: String, audience: String) -> Unit,
    onAddPickToWatchlist: (ConciergePick) -> Unit,
    onOpenMedia: (MediaEntity) -> Unit,
    onUpgradePro: () -> Unit,
    onWatchRewardedTrialAd: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val moods = listOf("Mind-Bending", "Feel-Good", "Edge-of-Seat Thriller", "Cozy Comedy", "Epic Sci-Fi", "True Crime / Doc")
    val budgets = listOf("Quick < 45m Ep", "Under 2h Movie", "Weekend Binge")
    val audiences = listOf("Solo", "Date Night", "Friends", "Family")

    var selectedMood by remember { mutableStateOf(moods.first()) }
    var selectedBudget by remember { mutableStateOf(budgets[1]) }
    var selectedAudience by remember { mutableStateOf(audiences.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_concierge_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tonight's AI Concierge",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "3 tailored picks grounded in your ratings, watchlist & streaming apps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close AI Concierge")
                }
            }

            // Remaining quota pill
            val quotaLabel = when {
                monetizationState.isPro -> "PRO • UNLIMITED TONIGHT'S PICKS"
                monetizationState.rewardedTrialCredits > 0 ->
                    "${monetizationState.remainingFreeConciergePicks}/${MonetizationState.FREE_CONCIERGE_LIMIT} FREE/MO • ${monetizationState.rewardedTrialCredits} AD PASS"
                else ->
                    "${monetizationState.remainingFreeConciergePicks} OF ${MonetizationState.FREE_CONCIERGE_LIMIT} FREE THIS MONTH"
            }
            val conciergeColors = WatchPilotColors.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(conciergeColors.goldChipContainer)
                        .border(1.dp, conciergeColors.goldChipBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = quotaLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = conciergeColors.goldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (monetizationState.subscribedProviders.isNotEmpty()) {
                    Text(
                        text = "Using ${monetizationState.subscribedProviders.size} OTT app(s)",
                        fontSize = 11.sp,
                        color = conciergeColors.cyanAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text("1. Tonight's Mood", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moods.forEach { mood ->
                    val isMoodSelected = selectedMood == mood
                    FilterChip(
                        selected = isMoodSelected,
                        onClick = { selectedMood = mood },
                        label = {
                            Text(
                                text = mood,
                                fontSize = 12.sp,
                                fontWeight = if (isMoodSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = conciergeColors.neutralChipContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = conciergeColors.goldSolidBg,
                            selectedLabelColor = conciergeColors.onGoldSolid
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isMoodSelected,
                            borderColor = conciergeColors.neutralChipBorder,
                            selectedBorderColor = conciergeColors.goldChipBorder
                        )
                    )
                }
            }

            Text("2. Time Budget", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                budgets.forEach { budget ->
                    val isBudgetSelected = selectedBudget == budget
                    FilterChip(
                        selected = isBudgetSelected,
                        onClick = { selectedBudget = budget },
                        label = {
                            Text(
                                text = budget,
                                fontSize = 12.sp,
                                fontWeight = if (isBudgetSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = conciergeColors.neutralChipContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = conciergeColors.cyanSolidBg,
                            selectedLabelColor = conciergeColors.onCyanSolid
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isBudgetSelected,
                            borderColor = conciergeColors.neutralChipBorder,
                            selectedBorderColor = conciergeColors.cyanChipBorder
                        )
                    )
                }
            }

            Text("3. Watching With", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                audiences.forEach { aud ->
                    val isAudienceSelected = selectedAudience == aud
                    FilterChip(
                        selected = isAudienceSelected,
                        onClick = { selectedAudience = aud },
                        label = {
                            Text(
                                text = aud,
                                fontSize = 12.sp,
                                fontWeight = if (isAudienceSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = conciergeColors.neutralChipContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = conciergeColors.goldSolidBg,
                            selectedLabelColor = conciergeColors.onGoldSolid
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAudienceSelected,
                            borderColor = conciergeColors.neutralChipBorder,
                            selectedBorderColor = conciergeColors.goldChipBorder
                        )
                    )
                }
            }

            if (!monetizationState.isPro && monetizationState.remainingFreeConciergePicks == 0) {
                RewardedTrialPassBanner(
                    monetizationState = monetizationState,
                    sourceTag = "ai_concierge",
                    onRewardEarned = onWatchRewardedTrialAd
                )
            }

            Button(
                onClick = {
                    if (monetizationState.canUseConcierge) {
                        onGeneratePicks(selectedMood, selectedBudget, selectedAudience)
                    } else {
                        onUpgradePro()
                    }
                },
                enabled = conciergeState !is ConciergeUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_tonights_picks_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = conciergeColors.goldSolidBg,
                    contentColor = conciergeColors.onGoldSolid
                )
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (monetizationState.canUseConcierge) "Pick What We Watch Tonight" else "Unlock Unlimited with Pro",
                    fontWeight = FontWeight.ExtraBold
                )
            }

            when (conciergeState) {
                is ConciergeUiState.Idle -> Unit
                is ConciergeUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = conciergeColors.goldAccent)
                        Text(
                            text = "Curating 3 high-match picks for your $selectedMood vibe...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ConciergeUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = conciergeColors.redChipContainer),
                        border = BorderStroke(1.dp, conciergeColors.redChipBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = conciergeState.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is ConciergeUiState.Success -> {
                    val trackedIds = remember(trackedMedia) {
                        trackedMedia.filter { it.getWatchStatusEnum() != WatchStatus.NONE }.map { it.id }.toSet()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        conciergeState.picks.forEach { pick ->
                            val entity = pick.matchedEntity
                            val isTracked = entity != null && entity.id in trackedIds
                            val regionalProviders = entity?.getProvidersForRegion(effectiveRegionCode).orEmpty().take(2)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = entity != null) {
                                        if (entity != null) onOpenMedia(entity)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, conciergeColors.goldChipBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    AsyncImage(
                                        model = entity?.posterUrl?.ifBlank { null },
                                        contentDescription = pick.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 96.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(conciergeColors.greenChipContainer)
                                                    .border(1.dp, conciergeColors.greenChipBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${pick.matchScore}% MATCH",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = conciergeColors.greenAccent,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                text = "${pick.releaseYear} • ${pick.mediaType.label}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pick.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = pick.whyItFits,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (regionalProviders.isNotEmpty()) {
                                            Text(
                                                text = "Stream on: ${regionalProviders.joinToString(", ")}",
                                                fontSize = 11.sp,
                                                color = conciergeColors.cyanAccent,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onAddPickToWatchlist(pick) }) {
                                        Icon(
                                            imageVector = if (isTracked) Icons.Filled.Check else Icons.Filled.Add,
                                            contentDescription = "Save ${pick.title}",
                                            tint = if (isTracked) conciergeColors.greenAccent else conciergeColors.goldAccent
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
}
