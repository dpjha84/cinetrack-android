package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import com.example.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.TvWatchStatus
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.WatchProviderLauncher

@Composable
fun SeasonDetailScreen(
    media: MediaEntity,
    season: SeasonItem,
    viewModel: MediaViewModel,
    effectiveRegionCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val episodes by viewModel.currentSeasonEpisodes.collectAsStateWithLifecycle()
    val isLoadingEpisodes by viewModel.isLoadingEpisodes.collectAsStateWithLifecycle()
    val selectedEpisodeDetail by viewModel.selectedEpisodeDetail.collectAsStateWithLifecycle()
    val activeWatchProviders by viewModel.activeWatchProviders.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val showStreamingProviders = userPrefs.showStreamingProviders

    val providersInfo = activeWatchProviders ?: remember(media, effectiveRegionCode) {
        media.getWatchProvidersInfo(effectiveRegionCode)
    }
    val streamAndFree = if (showStreamingProviders) {
        (providersInfo.stream + providersInfo.free).distinctBy { it.name }
    } else {
        emptyList()
    }

    val adaptive = LocalAdaptiveLayoutInfo.current
    val gridColumns = adaptive.cardGridColumns

    if (adaptive.isTabletOrWide) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = modifier
                .fillMaxSize()
                .testTag("season_detail_screen"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(gridColumns) }) {
                SeasonHeroHeaderBox(
                    media = media,
                    season = season,
                    isTablet = true,
                    onBack = onBack
                )
            }

            item(span = { GridItemSpan(gridColumns) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SeasonWatchStatusCard(
                        media = media,
                        season = season,
                        streamAndFree = streamAndFree,
                        providersInfo = providersInfo,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1040.dp)
                    )
                }
            }

            if (season.overview.isNotBlank()) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1040.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Season Synopsis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = season.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(gridColumns) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1040.dp)
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes (${episodes.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        if (isLoadingEpisodes) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = WatchPilotColors.current.goldAccent,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            gridItems(episodes, key = { it.id }) { episode ->
                EpisodeCard(
                    episode = episode,
                    media = media,
                    streamProviders = streamAndFree,
                    showStreamOverlay = showStreamingProviders,
                    onStatusChange = { newStatus ->
                        viewModel.setEpisodeWatchStatus(
                            media = media,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            status = newStatus
                        )
                    },
                    onClick = { viewModel.openEpisodeDetail(episode) },
                    onPlayClick = { provider ->
                        WatchProviderLauncher.launchProvider(
                            context = context,
                            providerName = provider?.name ?: "Streaming Provider",
                            title = media.title,
                            releaseYear = media.releaseYear,
                            isMovie = false,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            webLink = provider?.webLink ?: providersInfo.justWatchLink,
                            directContentUrl = provider?.directContentUrl,
                            regionCode = providersInfo.regionCode.ifBlank { effectiveRegionCode }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("season_detail_screen"),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // Hero Header Box
            item {
                SeasonHeroHeaderBox(
                    media = media,
                    season = season,
                    isTablet = false,
                    onBack = onBack
                )
            }

            // Season Interactive Watch Status Card
            item {
                SeasonWatchStatusCard(
                    media = media,
                    season = season,
                    streamAndFree = streamAndFree,
                    providersInfo = providersInfo,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Season Overview
            if (season.overview.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Season Synopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = season.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Episodes Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes (${episodes.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    if (isLoadingEpisodes) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = WatchPilotColors.current.goldAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Episodes List
            items(episodes, key = { it.id }) { episode ->
                EpisodeCard(
                    episode = episode,
                    media = media,
                    streamProviders = streamAndFree,
                    showStreamOverlay = showStreamingProviders,
                    onStatusChange = { newStatus ->
                        viewModel.setEpisodeWatchStatus(
                            media = media,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            status = newStatus
                        )
                    },
                    onClick = {
                        viewModel.openEpisodeDetail(episode)
                    },
                    onPlayClick = { provider ->
                        WatchProviderLauncher.launchProvider(
                            context = context,
                            providerName = provider?.name ?: "Streaming Provider",
                            title = media.title,
                            releaseYear = media.releaseYear,
                            isMovie = false,
                            seasonNumber = episode.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                            webLink = provider?.webLink ?: providersInfo.justWatchLink,
                            directContentUrl = provider?.directContentUrl,
                            regionCode = providersInfo.regionCode.ifBlank { effectiveRegionCode }
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Episode Detail Dialog / BottomSheet
    selectedEpisodeDetail?.let { episode ->
        EpisodeDetailDialog(
            episode = episode,
            media = media,
            streamProviders = streamAndFree,
            onDismiss = { viewModel.closeEpisodeDetail() },
            onStatusChange = { newStatus ->
                viewModel.setEpisodeWatchStatus(
                    media = media,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    status = newStatus
                )
            },
            onPlayClick = { provider ->
                WatchProviderLauncher.launchProvider(
                    context = context,
                    providerName = provider?.name ?: "Streaming Provider",
                    title = media.title,
                    releaseYear = media.releaseYear,
                    isMovie = false,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    webLink = provider?.webLink ?: providersInfo.justWatchLink,
                    directContentUrl = provider?.directContentUrl,
                    regionCode = providersInfo.regionCode.ifBlank { effectiveRegionCode }
                )
            }
        )
    }
}

@Composable
private fun SeasonHeroHeaderBox(
    media: MediaEntity,
    season: SeasonItem,
    isTablet: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 320.dp else 280.dp)
            .clip(if (isTablet) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(0.dp))
    ) {
                Image(
                    painter = painterResource(id = R.drawable.banner_cinema),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Backdrop / Poster
                val headerImageUrl = season.backdropUrl.ifBlank { season.posterUrl.ifBlank { media.backdropUrl } }
                if (headerImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(headerImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = season.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Dark gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Top Bar with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isTablet) Modifier.widthIn(max = 1040.dp).align(Alignment.TopCenter) else Modifier)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .testTag("season_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Series",
                            tint = Color.White
                        )
                    }

                    // Season Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SEASON ${season.seasonNumber}",
                                color = CinemaGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Season Title & Series Floating Content
                Row(
                    modifier = Modifier
                        .align(if (isTablet) Alignment.BottomCenter else Alignment.BottomStart)
                        .fillMaxWidth()
                        .then(if (isTablet) Modifier.widthIn(max = 1040.dp) else Modifier)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Season Poster
                    Box(
                        modifier = Modifier
                            .width(if (isTablet) 114.dp else 90.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(32.dp)
                        )
                        val posterSource = season.posterUrl.ifBlank { media.posterUrl }
                        if (posterSource.isNotBlank()) {
                            AsyncImage(
                                model = posterSource,
                                contentDescription = season.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    val headerColors = WatchPilotColors.current
                    Column {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerColors.cyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = season.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${season.releaseYear} • ${season.episodeCount} Episodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (season.rating > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = headerColors.goldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", season.rating),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = headerColors.goldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
}

@Composable
private fun SeasonWatchStatusCard(
    media: MediaEntity,
    season: SeasonItem,
    streamAndFree: List<com.example.data.model.WatchProviderItem>,
    providersInfo: com.example.data.model.WatchProvidersInfo,
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = modifier
                    .testTag("season_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                text = "Season Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (season.watchStatus) {
                                    TvWatchStatus.WATCHED -> "Completed"
                                    TvWatchStatus.WATCHING -> "Watching"
                                    TvWatchStatus.PLAN_TO_WATCH -> "Plan to watch"
                                    TvWatchStatus.UNWATCHED -> "Unwatched"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Badge
                        val seasonColors = WatchPilotColors.current
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (season.watchStatus) {
                                TvWatchStatus.WATCHED -> seasonColors.greenChipContainer
                                TvWatchStatus.WATCHING -> seasonColors.cyanChipContainer
                                TvWatchStatus.PLAN_TO_WATCH -> seasonColors.goldChipContainer
                                TvWatchStatus.UNWATCHED -> seasonColors.neutralChipContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                when (season.watchStatus) {
                                    TvWatchStatus.WATCHED -> seasonColors.greenChipBorder
                                    TvWatchStatus.WATCHING -> seasonColors.cyanChipBorder
                                    TvWatchStatus.PLAN_TO_WATCH -> seasonColors.goldChipBorder
                                    TvWatchStatus.UNWATCHED -> seasonColors.neutralChipBorder
                                }
                            )
                        ) {
                            Text(
                                text = season.watchStatus.chipLabel.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = when (season.watchStatus) {
                                    TvWatchStatus.WATCHED -> seasonColors.greenAccent
                                    TvWatchStatus.WATCHING -> seasonColors.cyanAccent
                                    TvWatchStatus.PLAN_TO_WATCH -> seasonColors.goldAccent
                                    TvWatchStatus.UNWATCHED -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Segmented Status Selectors
                    val chipColors = WatchPilotColors.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TvWatchStatus.WATCHED to Icons.Default.Check,
                            TvWatchStatus.WATCHING to Icons.Default.Visibility,
                            TvWatchStatus.PLAN_TO_WATCH to Icons.Default.Bookmark,
                            TvWatchStatus.UNWATCHED to Icons.Default.Close
                        ).forEach { (status, icon) ->
                            val isSelected = season.watchStatus == status
                            val selectedFg = when (status) {
                                TvWatchStatus.WATCHED -> chipColors.onGreenSolid
                                TvWatchStatus.WATCHING -> chipColors.onCyanSolid
                                TvWatchStatus.PLAN_TO_WATCH -> chipColors.onGoldSolid
                                TvWatchStatus.UNWATCHED -> MaterialTheme.colorScheme.onSurface
                            }
                            val selectedBorder = when (status) {
                                TvWatchStatus.WATCHED -> chipColors.greenChipBorder
                                TvWatchStatus.WATCHING -> chipColors.cyanChipBorder
                                TvWatchStatus.PLAN_TO_WATCH -> chipColors.goldChipBorder
                                TvWatchStatus.UNWATCHED -> chipColors.neutralChipBorder
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSeasonWatchStatus(media, season.seasonNumber, status) },
                                label = {
                                    Text(
                                        text = status.chipLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = chipColors.neutralChipContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = when (status) {
                                        TvWatchStatus.WATCHED -> chipColors.greenSolidBg
                                        TvWatchStatus.WATCHING -> chipColors.cyanSolidBg
                                        TvWatchStatus.PLAN_TO_WATCH -> chipColors.goldSolidBg
                                        TvWatchStatus.UNWATCHED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    selectedLabelColor = selectedFg,
                                    selectedLeadingIconColor = selectedFg
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = chipColors.neutralChipBorder,
                                    selectedBorderColor = selectedBorder
                                ),
                                modifier = Modifier.testTag("chip_season_status_${status.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Watched progress bar for this season
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Season Progress",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "${season.watchedEpisodesCount} of ${season.episodeCount} episodes (${(season.progressFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = chipColors.cyanAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { season.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = chipColors.goldSolidBg,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mark All Watched Button
                        OutlinedButton(
                            onClick = {
                                viewModel.setSeasonWatchStatus(media, season.seasonNumber, TvWatchStatus.WATCHED)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, chipColors.neutralChipBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_mark_all_season_watched")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark All Watched",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play S{x}:E1 Direct Streaming Button
                        if (streamAndFree.isNotEmpty()) {
                            val provider = streamAndFree.first()
                            Button(
                                onClick = {
                                    WatchProviderLauncher.launchProvider(
                                        context = context,
                                        providerName = provider.name,
                                        title = media.title,
                                        releaseYear = media.releaseYear,
                                        isMovie = false,
                                        seasonNumber = season.seasonNumber,
                                        episodeNumber = 1,
                                        webLink = provider.webLink ?: providersInfo.justWatchLink,
                                        directContentUrl = provider.directContentUrl,
                                        regionCode = providersInfo.regionCode
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WatchPilotColors.current.goldSolidBg,
                                    contentColor = WatchPilotColors.current.onGoldSolid
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("btn_play_s1_direct")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play S${season.seasonNumber}:E1",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
}

@Composable
fun EpisodeCard(
    episode: EpisodeItem,
    media: MediaEntity,
    streamProviders: List<com.example.data.model.WatchProviderItem>,
    showStreamOverlay: Boolean = true,
    onStatusChange: (TvWatchStatus) -> Unit,
    onClick: () -> Unit,
    onPlayClick: (com.example.data.model.WatchProviderItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (episode.watchStatus == TvWatchStatus.WATCHED) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("episode_card_${episode.seasonNumber}_${episode.episodeNumber}")
    ) {
        Column {
            // Still Image Thumbnail with Duration and Play Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = episode.stillUrl.ifBlank { media.backdropUrl },
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Episode Number Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "EPISODE ${episode.episodeNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = CinemaGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Runtime Badge
                if (episode.runtimeMinutes > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${episode.runtimeMinutes} min",
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Consolidated Play / Stream Button Overlay
                if (showStreamOverlay) {
                    Surface(
                        shape = CircleShape,
                        color = CinemaGold.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(46.dp)
                            .clickable { onPlayClick(streamProviders.firstOrNull()) }
                            .testTag("btn_stream_ep_${episode.episodeNumber}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play or Stream Episode",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Title and Air Date at bottom of thumbnail
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (episode.airDate.isNotBlank()) {
                        Text(
                            text = episode.airDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Episode Body
            Column(modifier = Modifier.padding(12.dp)) {
                if (episode.overview.isNotBlank()) {
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Interactive Watch Status Buttons
                val epCardColors = WatchPilotColors.current
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        TvWatchStatus.WATCHED to "Watched",
                        TvWatchStatus.WATCHING to "Watching",
                        TvWatchStatus.PLAN_TO_WATCH to "Plan"
                    ).forEach { (status, label) ->
                        val isSelected = episode.watchStatus == status
                        val selectedBg = when (status) {
                            TvWatchStatus.WATCHED -> epCardColors.greenSolidBg
                            TvWatchStatus.WATCHING -> epCardColors.cyanSolidBg
                            TvWatchStatus.PLAN_TO_WATCH -> epCardColors.goldSolidBg
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val selectedFg = when (status) {
                            TvWatchStatus.WATCHED -> epCardColors.onGreenSolid
                            TvWatchStatus.WATCHING -> epCardColors.onCyanSolid
                            TvWatchStatus.PLAN_TO_WATCH -> epCardColors.onGoldSolid
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val selectedBorder = when (status) {
                            TvWatchStatus.WATCHED -> epCardColors.greenChipBorder
                            TvWatchStatus.WATCHING -> epCardColors.cyanChipBorder
                            TvWatchStatus.PLAN_TO_WATCH -> epCardColors.goldChipBorder
                            else -> epCardColors.neutralChipBorder
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) selectedBg else epCardColors.neutralChipContainer,
                            border = BorderStroke(1.dp, if (isSelected) selectedBorder else epCardColors.neutralChipBorder),
                            modifier = Modifier
                                .clickable {
                                    val next = if (isSelected) TvWatchStatus.UNWATCHED else status
                                    onStatusChange(next)
                                }
                                .testTag("btn_ep_${episode.episodeNumber}_${status.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (status) {
                                        TvWatchStatus.WATCHED -> Icons.Default.Check
                                        TvWatchStatus.WATCHING -> Icons.Default.Visibility
                                        TvWatchStatus.PLAN_TO_WATCH -> Icons.Default.Bookmark
                                        else -> Icons.Default.Close
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeDetailDialog(
    episode: EpisodeItem,
    media: MediaEntity,
    streamProviders: List<com.example.data.model.WatchProviderItem>,
    onDismiss: () -> Unit,
    onStatusChange: (TvWatchStatus) -> Unit,
    onPlayClick: (com.example.data.model.WatchProviderItem?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Still Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = episode.stillUrl.ifBlank { media.backdropUrl },
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "S${episode.seasonNumber}:E${episode.episodeNumber}",
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${episode.runtimeMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = episode.airDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (episode.rating > 0f) {
                        val ratingColors = WatchPilotColors.current
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = ratingColors.goldAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", episode.rating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ratingColors.goldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Episode Synopsis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.overview.ifBlank { "No detailed synopsis available for this episode." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                if (episode.director.isNotBlank() || episode.writer.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (episode.director.isNotBlank()) {
                        Text(
                            text = "Director: ${episode.director}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (episode.writer.isNotBlank()) {
                        Text(
                            text = "Writer: ${episode.writer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Watch Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val epChipColors = WatchPilotColors.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TvWatchStatus.WATCHED to "Watched",
                        TvWatchStatus.WATCHING to "Watching",
                        TvWatchStatus.PLAN_TO_WATCH to "Plan"
                    ).forEach { (status, label) ->
                        val isSelected = episode.watchStatus == status
                        val selectedFg = when (status) {
                            TvWatchStatus.WATCHED -> epChipColors.onGreenSolid
                            TvWatchStatus.WATCHING -> epChipColors.onCyanSolid
                            TvWatchStatus.PLAN_TO_WATCH -> epChipColors.onGoldSolid
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val selectedBorder = when (status) {
                            TvWatchStatus.WATCHED -> epChipColors.greenChipBorder
                            TvWatchStatus.WATCHING -> epChipColors.cyanChipBorder
                            TvWatchStatus.PLAN_TO_WATCH -> epChipColors.goldChipBorder
                            else -> epChipColors.neutralChipBorder
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) TvWatchStatus.UNWATCHED else status
                                onStatusChange(next)
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = epChipColors.neutralChipContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = when (status) {
                                    TvWatchStatus.WATCHED -> epChipColors.greenSolidBg
                                    TvWatchStatus.WATCHING -> epChipColors.cyanSolidBg
                                    TvWatchStatus.PLAN_TO_WATCH -> epChipColors.goldSolidBg
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                selectedLabelColor = selectedFg
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = epChipColors.neutralChipBorder,
                                selectedBorderColor = selectedBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (streamProviders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Stream This Episode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        streamProviders.forEach { provider ->
                            FilledTonalButton(
                                onClick = { onPlayClick(provider) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = epChipColors.neutralChipContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, epChipColors.neutralChipBorder)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val doneColors = WatchPilotColors.current
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = doneColors.goldSolidBg, contentColor = doneColors.onGoldSolid)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
