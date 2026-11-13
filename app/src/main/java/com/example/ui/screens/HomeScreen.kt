package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.MediaPosterCard
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.RegionalDefaults
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.viewmodel.MediaViewModel

@Composable
fun HomeScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val continueWatchingList by viewModel.continueWatching.collectAsStateWithLifecycle()
    val discoverItems by viewModel.filteredDiscover.collectAsStateWithLifecycle()
    val trendingList by viewModel.trendingItems.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()

    val regionOption = remember(effectiveRegion) {
        RegionalDefaults.supportedRegions.find { it.code.equals(effectiveRegion, ignoreCase = true) }
            ?: RegionalDefaults.detectDeviceRegion()
    }

    val topSpotlightItems = remember(trendingList, discoverItems) {
        if (trendingList.isNotEmpty()) trendingList.take(10) else discoverItems.take(10)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_grid")
        ) {
            // Top Horizontal Scrolling Spotlight Carousel (Trending & Featured)
            if (topSpotlightItems.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = CinemaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${regionOption.flag} Trending in ${regionOption.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CinemaGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "TOP PICKS",
                                    color = CinemaGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_top_spotlight_carousel")
                        ) {
                            itemsIndexed(
                                items = topSpotlightItems,
                                key = { index, item -> "top_spotlight_${item.id}_$index" }
                            ) { index, item ->
                                SpotlightBannerCard(
                                    rank = index + 1,
                                    media = item,
                                    onClick = { onMediaClick(item.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Category & Genre Filter Section (Consolidated & Streamlined)
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Media Type Chips (All, Movies, TV)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.selectedTypeFilter.value = null },
                            label = { Text("All Media", maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        FilterChip(
                            selected = selectedType == MediaType.MOVIE,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.MOVIE },
                            label = { Text("Movies", maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        FilterChip(
                            selected = selectedType == MediaType.TV_SHOW,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.TV_SHOW },
                            label = { Text("TV Shows", maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaCyan,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Genre Scrollable Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedGenre == null,
                            onClick = { viewModel.selectedGenreFilter.value = null },
                            label = { Text("All Genres", fontSize = 12.sp) }
                        )

                        availableGenres.forEach { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = {
                                    viewModel.selectedGenreFilter.value =
                                        if (selectedGenre == genre) null else genre
                                },
                                label = { Text(genre, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CinemaGold.copy(alpha = 0.2f),
                                    selectedLabelColor = CinemaGold
                                )
                            )
                        }
                    }
                }
            }

            // Continue Watching Section (if active)
            if (continueWatchingList.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${continueWatchingList.size} Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = CinemaCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            continueWatchingList.forEach { item ->
                                ContinueWatchingCard(
                                    media = item,
                                    onClick = { onMediaClick(item.id) },
                                    onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Catalog Grid Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular in ${regionOption.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${discoverItems.size} titles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty State
            if (discoverItems.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matches found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try adjusting your search query or genre filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(discoverItems, key = { it.id }) { media ->
                    MediaPosterCard(
                        media = media,
                        onClick = { onMediaClick(media.id) },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpotlightBannerCard(
    rank: Int,
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .width(290.dp)
            .height(175.dp)
            .clickable(onClick = onClick)
            .testTag("spotlight_card_${media.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bgUrl = media.backdropUrl.ifBlank { media.posterUrl }
            Image(
                painter = painterResource(id = R.drawable.banner_cinema),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (bgUrl.isNotBlank()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.banner_cinema)
                        .error(R.drawable.banner_cinema)
                        .build(),
                    contentDescription = media.title,
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
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#$rank",
                            color = CinemaGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${if (isMovie) "MOVIE" else "SERIES"}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            color = CinemaGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${media.releaseYear} • ${media.genres.ifBlank { "Drama" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
