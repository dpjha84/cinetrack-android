package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.RegionalDefaults
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.TrendingFilterCategory

@Composable
fun TrendingScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingItems by viewModel.filteredTrendingItems.collectAsStateWithLifecycle()
    val rawTrendingList by viewModel.trendingItems.collectAsStateWithLifecycle()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.trendingCategoryFilter.collectAsStateWithLifecycle()
    val timeWindow by viewModel.trendingTimeWindow.collectAsStateWithLifecycle()
    val trendingError by viewModel.trendingError.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val isTmdbConfigured by viewModel.isTmdbConfigured.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()

    var showLocalizationDialog by remember { mutableStateOf(false) }

    val movieCount = remember(rawTrendingList) {
        rawTrendingList.count { it.getMediaTypeEnum() == MediaType.MOVIE }
    }
    val tvCount = remember(rawTrendingList) {
        rawTrendingList.count { it.getMediaTypeEnum() == MediaType.TV_SHOW }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("trending_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CinemaGold.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Trending",
                                        tint = CinemaGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Trending Now",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (timeWindow == "day") "TMDB /trending/all/day • Today" else "TMDB /trending/all/week • This Week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.fetchTrendingNow(timeWindow) },
                                modifier = Modifier.testTag("btn_refresh_trending")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Trending",
                                    tint = CinemaGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Regional & API Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocalizationDialog = true }
                            .testTag("btn_trending_region_settings")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = CinemaGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Region: ${RegionalDefaults.getRegionLabel(effectiveRegion)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${RegionalDefaults.getLanguageLabel(userPreferences.languageCode)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isTmdbConfigured) CinemaGreen.copy(alpha = 0.2f) else CinemaGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isTmdbConfigured) "⚡ TMDB /trending/all/day" else "TMDB Config",
                                    color = if (isTmdbConfigured) CinemaGreen else CinemaGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time Window Selector (Today / This Week)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = timeWindow == "day",
                            onClick = { viewModel.setTrendingTimeWindow("day") },
                            label = { Text("Today (all/day)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_trending_day")
                        )
                        FilterChip(
                            selected = timeWindow == "week",
                            onClick = { viewModel.setTrendingTimeWindow("week") },
                            label = { Text("This Week (all/week)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_trending_week")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Filter Chips (All, Movies, TV Shows)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = categoryFilter == TrendingFilterCategory.ALL,
                            onClick = { viewModel.setTrendingCategory(TrendingFilterCategory.ALL) },
                            label = { Text("All (${rawTrendingList.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold.copy(alpha = 0.25f),
                                selectedLabelColor = CinemaGold
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_cat_all")
                        )
                        FilterChip(
                            selected = categoryFilter == TrendingFilterCategory.MOVIES,
                            onClick = { viewModel.setTrendingCategory(TrendingFilterCategory.MOVIES) },
                            label = { Text("Movies ($movieCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_cat_movies")
                        )
                        FilterChip(
                            selected = categoryFilter == TrendingFilterCategory.TV_SHOWS,
                            onClick = { viewModel.setTrendingCategory(TrendingFilterCategory.TV_SHOWS) },
                            label = { Text("TV Shows ($tvCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaCyan,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_cat_tv")
                        )
                    }
                }
            }

            // Scrollable List Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isTrendingLoading && trendingItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CinemaGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching popular titles from TMDB...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Endpoint: /trending/all/$timeWindow",
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaGold
                        )
                    }
                } else if (trendingItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No trending titles found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trendingError ?: "Check your network connection or TMDB API key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchTrendingNow(timeWindow) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaGold)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Fetch")
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("trending_scrollable_list")
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥 Most Popular ${categoryFilter.label}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${trendingItems.size} Titles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        itemsIndexed(
                            items = trendingItems,
                            key = { index, item -> "${item.id}_$index" }
                        ) { index, item ->
                            val localTracked = allMedia.find { it.id == item.id }
                            val inWatchlist = localTracked != null && localTracked.getWatchStatusEnum() != WatchStatus.NONE

                            TrendingMediaCard(
                                rank = index + 1,
                                media = item,
                                inWatchlist = inWatchlist,
                                onClick = { onMediaClick(item.id) },
                                onToggleWatchlist = {
                                    viewModel.toggleWatchlistQuick(item.id)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }

                // Loading overlay banner if refreshing
                if (isTrendingLoading && trendingItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CinemaGold,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Refreshing TMDB trending...",
                                style = MaterialTheme.typography.labelMedium,
                                color = CinemaGold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLocalizationDialog) {
        LocalizationPreferencesDialog(
            currentPreferences = userPreferences,
            effectiveRegionCode = effectiveRegion,
            activeApiKey = viewModel.getActiveTmdbApiKey(),
            onDismiss = { showLocalizationDialog = false },
            onSave = { regionCode, autoDetect, langCode, filterOnly, tmdbKey ->
                viewModel.updateRegion(regionCode, autoDetect)
                viewModel.updateLanguage(langCode)
                viewModel.toggleFilterByLanguage(filterOnly)
                viewModel.updateCustomTmdbApiKey(tmdbKey)
                showLocalizationDialog = false
            }
        )
    }
}

@Composable
fun TrendingMediaCard(
    rank: Int,
    media: MediaEntity,
    inWatchlist: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE

    // Rank styling: #1 Gold, #2 Silver, #3 Bronze, others subtle
    val (rankBgColor, rankTextColor) = when (rank) {
        1 -> Pair(CinemaGold, Color.Black)
        2 -> Pair(Color(0xFFC0C0C0), Color.Black)
        3 -> Pair(Color(0xFFCD7F32), Color.White)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trending_card_${media.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Poster with Rank Badge Overlay
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Rank Badge in top-left corner
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = rankBgColor,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = rankTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Media Type Pill (Movie vs TV)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isMovie) CinemaGold.copy(alpha = 0.2f) else CinemaCyan.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (isMovie) CinemaGold else CinemaCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMovie) "MOVIE" else "TV SERIES",
                                color = if (isMovie) CinemaGold else CinemaCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Release Year
                    if (media.releaseYear > 0) {
                        Text(
                            text = media.releaseYear.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating & Genres Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = CinemaGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", media.communityRating),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CinemaGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${media.genres}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Overview Snippet
                if (media.overview.isNotBlank()) {
                    Text(
                        text = media.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Watchlist Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inWatchlist) {
                        Button(
                            onClick = onToggleWatchlist,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGreen.copy(alpha = 0.25f),
                                contentColor = CinemaGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_watchlist_${media.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("In Watchlist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleWatchlist,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CinemaGold
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_watchlist_${media.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Watchlist", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
