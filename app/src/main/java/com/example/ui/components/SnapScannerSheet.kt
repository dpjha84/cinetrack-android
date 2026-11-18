package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.MonetizationState
import com.example.data.model.ScannedMediaCandidate
import com.example.data.model.SnapScanUiState
import com.example.data.model.WatchStatus
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapScannerSheet(
    scanState: SnapScanUiState,
    monetizationState: MonetizationState,
    trackedMedia: List<MediaEntity>,
    effectiveRegionCode: String,
    onDismiss: () -> Unit,
    onPickScreenshotUri: (android.net.Uri) -> Unit,
    onAddCandidate: (ScannedMediaCandidate) -> Unit,
    onAddAllToWatchlist: () -> Unit,
    onOpenMedia: (MediaEntity) -> Unit,
    onUpgradePro: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var addedAllClicked by remember(scanState) { mutableStateOf(false) }
    val trackedIds = remember(trackedMedia) {
        trackedMedia.filter { it.getWatchStatusEnum() != WatchStatus.NONE }.map { it.id }.toSet()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickScreenshotUri(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = Modifier.testTag("snap_scanner_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        text = "Snap-to-Watchlist Vision",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Pick any Reel, TikTok, streaming row, or Top-10 list screenshot",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Vision Scanner")
                }
            }

            // Quota Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeText = if (monetizationState.isPro) {
                    "PRO • UNLIMITED VISION SCANS"
                } else {
                    "${monetizationState.remainingFreeSnapScans} OF ${MonetizationState.FREE_SNAP_SCANS_LIMIT} FREE SCANS LEFT"
                }
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (monetizationState.isPro) CinemaGold.copy(alpha = 0.2f) else CinemaCyan.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (monetizationState.isPro) CinemaGold else CinemaCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!monetizationState.isPro) {
                    Text(
                        text = "Go Unlimited →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaGold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onUpgradePro)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (monetizationState.canUseSnapScan) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        onUpgradePro()
                    }
                },
                enabled = scanState !is SnapScanUiState.Scanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pick_screenshot_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = DarkBackground
                )
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (scanState is SnapScanUiState.Idle) "Choose Screenshot" else "Scan Another Screenshot",
                    fontWeight = FontWeight.Bold
                )
            }

            when (scanState) {
                is SnapScanUiState.Idle -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "How it works: Zero storage permissions required. Select a screenshot from Instagram Reels, YouTube Shorts, Reddit, or Netflix Top 10 — WatchPilot AI extracts every title and matches live TMDB posters & regional streaming apps.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is SnapScanUiState.Scanning -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = CinemaGold)
                        Text(
                            text = "Analyzing screenshot with WatchPilot Vision AI & matching TMDB catalog...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SnapScanUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CinemaRed.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, CinemaRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = scanState.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is SnapScanUiState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected ${scanState.items.size} Title(s)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                addedAllClicked = true
                                onAddAllToWatchlist()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CinemaGreen),
                            modifier = Modifier.testTag("add_all_scanned_button")
                        ) {
                            Icon(
                                imageVector = if (addedAllClicked) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = CinemaGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (addedAllClicked) "Added All!" else "Add All to Watchlist",
                                color = CinemaGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = scanState.items,
                            key = { idx, cand -> "${cand.matchedEntity?.id ?: cand.rawTitle}_$idx" }
                        ) { _, cand ->
                            val entity = cand.matchedEntity
                            val isAlreadyTracked = entity != null && entity.id in trackedIds
                            val providers = entity?.getProvidersForRegion(effectiveRegionCode).orEmpty().take(2)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = entity != null) {
                                        if (entity != null) onOpenMedia(entity)
                                    },
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = entity?.posterUrl?.ifBlank { null },
                                        contentDescription = cand.rawTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 76.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkBackground)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entity?.title ?: cand.rawTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(cand.releaseYear ?: entity?.releaseYear ?: "")
                                                append(" • ")
                                                append(cand.mediaType.label)
                                                if (entity != null && entity.communityRating > 0f) {
                                                    append(" • ★ ${String.format(java.util.Locale.US, "%.1f", entity.communityRating)}")
                                                }
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (providers.isNotEmpty()) {
                                            Text(
                                                text = "On: ${providers.joinToString(", ")}",
                                                fontSize = 11.sp,
                                                color = CinemaCyan,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else if (cand.note.isNotBlank()) {
                                            Text(
                                                text = cand.note,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onAddCandidate(cand) }
                                    ) {
                                        Icon(
                                            imageVector = if (isAlreadyTracked || addedAllClicked) Icons.Filled.Check else Icons.Filled.Add,
                                            contentDescription = "Add ${cand.rawTitle} to Watchlist",
                                            tint = if (isAlreadyTracked || addedAllClicked) CinemaGreen else CinemaGold
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
