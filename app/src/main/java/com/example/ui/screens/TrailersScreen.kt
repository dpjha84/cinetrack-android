package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.model.MediaType
import com.example.data.model.TrailerItem
import com.example.data.model.WatchStatus
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.TrailerCategory
import com.example.util.TrailerInterstitialAdManager

@Composable
fun TrailersScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val context = LocalContext.current
    val activity = context as? Activity
    val trailers by viewModel.filteredTrailers.collectAsStateWithLifecycle()
    val allTrailers by viewModel.trailers.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.trailerCategoryFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isTrailersLoading.collectAsStateWithLifecycle()
    val allTrackedMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()

    LaunchedEffect(monetizationState.isPro) {
        if (!monetizationState.isPro) {
            TrailerInterstitialAdManager.preload(context, isPro = false)
        }
    }

    val regionOption = remember(effectiveRegion) {
        com.example.data.model.RegionalDefaults.supportedRegions.find { it.code.equals(effectiveRegion, ignoreCase = true) }
            ?: com.example.data.model.RegionalDefaults.detectDeviceRegion()
    }

    val relevantTrailers = remember(allTrailers) {
        allTrailers.filter { it.isWithinRelevanceWindow() }.distinctBy { it.mediaId }
    }
    val nowPlayingCount = remember(relevantTrailers) { relevantTrailers.count { it.isNowPlaying } }
    val upcomingCount = remember(relevantTrailers) { relevantTrailers.count { it.isUpcoming } }
    val movieCount = remember(relevantTrailers) { relevantTrailers.count { it.mediaType == MediaType.MOVIE } }
    val tvCount = remember(relevantTrailers) { relevantTrailers.count { it.mediaType == MediaType.TV_SHOW } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("trailers_screen")
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CinemaGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Trailers",
                                    tint = CinemaGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trailers",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CinemaGold.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${regionOption.flag} ${regionOption.code}",
                                        color = CinemaGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Now Playing & Upcoming • ${regionOption.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.fetchTrailers() },
                        modifier = Modifier.testTag("btn_refresh_trailers")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Trailers",
                            tint = CinemaGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Consolidated Category Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TrailerCategory.entries, key = { it.name }) { category ->
                        val selected = selectedCategory == category
                        val labelWithCount = when (category) {
                            TrailerCategory.ALL -> "All (${relevantTrailers.size})"
                            TrailerCategory.NOW_PLAYING -> "Now Playing ($nowPlayingCount)"
                            TrailerCategory.UPCOMING -> "Upcoming ($upcomingCount)"
                            TrailerCategory.MOVIES -> "Movies ($movieCount)"
                            TrailerCategory.TV_SHOWS -> "Series ($tvCount)"
                        }

                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setTrailerCategory(category) },
                            label = {
                                Text(
                                    text = labelWithCount,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = when (category) {
                                TrailerCategory.NOW_PLAYING -> {
                                    { Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(14.dp)) }
                                }
                                TrailerCategory.UPCOMING -> {
                                    { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp)) }
                                }
                                TrailerCategory.MOVIES -> {
                                    { Icon(Icons.Default.Movie, null, modifier = Modifier.size(14.dp)) }
                                }
                                TrailerCategory.TV_SHOWS -> {
                                    { Icon(Icons.Default.Tv, null, modifier = Modifier.size(14.dp)) }
                                }
                                else -> null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("trailer_filter_${category.name.lowercase()}")
                        )
                    }
                }

                if (isLoading && trailers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        color = CinemaGold,
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }
            }
        }

        // Trailers Content
        when {
            trailers.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CinemaGold)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Loading trailers...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            trailers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = CinemaGold.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No trailers found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.setTrailerCategory(TrailerCategory.ALL) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black)
                        ) {
                            Text("Show All")
                        }
                    }
                }
            }

            else -> {
                if (adaptive.isTabletOrWide) {
                    val gridColumns = adaptive.cardGridColumns
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("trailers_feed_list")
                    ) {
                        items(
                            items = trailers,
                            key = { it.id }
                        ) { trailer ->
                            val isMovie = trailer.mediaType == MediaType.MOVIE
                            val tracked = allTrackedMedia.find {
                                it.id == trailer.mediaId || it.title.equals(trailer.title, ignoreCase = true)
                            }
                            val isInWatchlist = tracked != null && tracked.getWatchStatusEnum() != WatchStatus.NONE

                            TrailerCard(
                                trailer = trailer,
                                isMovie = isMovie,
                                isInWatchlist = isInWatchlist,
                                onPlayClick = {
                                    TrailerInterstitialAdManager.maybeShowBeforeTrailer(
                                        activity = activity,
                                        isPro = monetizationState.isPro
                                    ) {
                                        playYouTubeVideo(context, trailer.videoKey)
                                    }
                                },
                                onToggleWatchlist = {
                                    if (tracked != null) {
                                        viewModel.toggleWatchlistQuick(tracked.id)
                                    } else {
                                        viewModel.toggleWatchlistQuick(trailer.toMediaEntity())
                                        Toast.makeText(context, "Added to Watchlist", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCardClick = {
                                    val targetId = tracked?.id ?: trailer.mediaId
                                    if (tracked == null) {
                                        viewModel.openMediaFromSearch(trailer.toMediaEntity())
                                    }
                                    onMediaClick(targetId)
                                }
                            )
                        }

                        if (!monetizationState.isPro) {
                            item(
                                key = "trailers_sponsored_slot",
                                span = { GridItemSpan(gridColumns) }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SponsoredBannerCard(
                                        isPro = monetizationState.isPro,
                                        onRemoveAdsClick = {
                                            viewModel.openPaywall("Remove all ads across WatchPilot with WatchPilot Pro.")
                                        },
                                        modifier = Modifier.widthIn(max = 728.dp),
                                        tagSuffix = "trailers"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("trailers_feed_list")
                    ) {
                        items(trailers, key = { it.id }) { trailer ->
                            val isMovie = trailer.mediaType == MediaType.MOVIE
                            val tracked = allTrackedMedia.find {
                                it.id == trailer.mediaId || it.title.equals(trailer.title, ignoreCase = true)
                            }
                            val isInWatchlist = tracked != null && tracked.getWatchStatusEnum() != WatchStatus.NONE

                            TrailerCard(
                                trailer = trailer,
                                isMovie = isMovie,
                                isInWatchlist = isInWatchlist,
                                onPlayClick = {
                                    TrailerInterstitialAdManager.maybeShowBeforeTrailer(
                                        activity = activity,
                                        isPro = monetizationState.isPro
                                    ) {
                                        playYouTubeVideo(context, trailer.videoKey)
                                    }
                                },
                                onToggleWatchlist = {
                                    if (tracked != null) {
                                        viewModel.toggleWatchlistQuick(tracked.id)
                                    } else {
                                        viewModel.toggleWatchlistQuick(trailer.toMediaEntity())
                                        Toast.makeText(context, "Added to Watchlist", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCardClick = {
                                    val targetId = tracked?.id ?: trailer.mediaId
                                    if (tracked == null) {
                                        viewModel.openMediaFromSearch(trailer.toMediaEntity())
                                    }
                                    onMediaClick(targetId)
                                }
                            )
                        }

                        if (!monetizationState.isPro) {
                            item(key = "trailers_sponsored_slot") {
                                SponsoredBannerCard(
                                    isPro = monetizationState.isPro,
                                    onRemoveAdsClick = {
                                        viewModel.openPaywall("Remove all ads across WatchPilot with WatchPilot Pro.")
                                    },
                                    tagSuffix = "trailers"
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
fun TrailerCard(
    trailer: TrailerItem,
    isMovie: Boolean,
    isInWatchlist: Boolean,
    onPlayClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trailer_card_${trailer.id}")
    ) {
        Column {
            // 16:9 Video Thumbnail with Single Play Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .clickable { onPlayClick() }
                    .testTag("btn_play_trailer_${trailer.id}")
            ) {
                val imageUrl = trailer.backdropUrl.ifBlank { trailer.youtubeThumbnailUrl }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Play ${trailer.title} ${trailer.videoType}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Top-Left: Release Status (+ subtle Teaser tag only if no Trailer exists yet)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (trailer.isUpcoming) CinemaGold else CinemaGreen
                    ) {
                        Text(
                            text = trailer.statusBadgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (trailer.videoType.equals("Teaser", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = "TEASER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Top-Right: Compact Type + Rating Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = if (isMovie) "MOVIE" else "SERIES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isMovie) CinemaGold else CinemaCyan
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = String.format("%.1f", trailer.communityRating),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Center Play Button
                Surface(
                    shape = CircleShape,
                    color = CinemaRed.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${trailer.videoType}",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom-Left: Title & Year/Genres
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = trailer.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${trailer.releaseYear} • ${trailer.genres}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card Body: Concise Overview + Watchlist & Details Actions
            Column(modifier = Modifier.padding(14.dp)) {
                if (trailer.overview.isNotBlank()) {
                    Text(
                        text = trailer.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isInWatchlist) CinemaGreen else CinemaGold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_watchlist_trailer_${trailer.id}")
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isInWatchlist) "In Watchlist" else "Watchlist",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onCardClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_details_trailer_${trailer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Details",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
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

private fun playYouTubeVideo(context: Context, videoKey: String) {
    if (videoKey.isBlank()) {
        Toast.makeText(context, "Trailer not available", Toast.LENGTH_SHORT).show()
        return
    }
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoKey")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoKey")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(appIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open video player", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open video player", Toast.LENGTH_SHORT).show()
        }
    }
}
