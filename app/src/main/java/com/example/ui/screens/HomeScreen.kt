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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.FloatingActionButton
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
    onOpenAddDialog: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToTrending: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val continueWatchingList by viewModel.continueWatching.collectAsStateWithLifecycle()
    val discoverItems by viewModel.filteredDiscover.collectAsStateWithLifecycle()
    val trendingList by viewModel.trendingItems.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val isSearchingTmdb by viewModel.isSearchingTmdb.collectAsStateWithLifecycle()
    val isTmdbConfigured by viewModel.isTmdbConfigured.collectAsStateWithLifecycle()

    var showLocalizationDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_grid")
        ) {
            // Hero Spotlight Banner
            item(span = { GridItemSpan(2) }) {
                val featured = discoverItems.firstOrNull { it.backdropUrl.isNotBlank() } ?: discoverItems.firstOrNull()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            featured?.let { onMediaClick(it.id) }
                        }
                        .testTag("hero_spotlight_banner")
                ) {
                    if (featured != null && featured.backdropUrl.isNotBlank()) {
                        AsyncImage(
                            model = featured.backdropUrl,
                            contentDescription = featured.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.banner_cinema),
                            contentDescription = "Cinema Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Scrim gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CinemaGold.copy(alpha = 0.25f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = CinemaGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FEATURED SPOTLIGHT • ${featured?.releaseYear ?: 2024}",
                                    color = CinemaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = featured?.title ?: "Track Every Story",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${featured?.genres ?: "Cinema & TV"} • ★ ${String.format("%.1f", featured?.communityRating ?: 8.8f)} • Tap to view",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Search Bar & Filter Section
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Google TV Mode Switcher Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141721),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { viewModel.setGoogleTvMode(true) }
                            .testTag("btn_enable_googletv_mode")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4C8DF6).copy(alpha = 0.25f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = Color(0xFF8AB4F8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Google TV Mode (v2.0)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Experience the cinematic Android TV look & feel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF4C8DF6)
                            ) {
                                Text(
                                    text = "Switch",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Regional Localization Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocalizationDialog = true }
                            .testTag("btn_open_localization")
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isTmdbConfigured) CinemaGreen.copy(alpha = 0.2f) else CinemaGold.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = if (isTmdbConfigured) "⚡ TMDB LIVE" else "TMDB Setup",
                                        color = if (isTmdbConfigured) CinemaGreen else CinemaGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CinemaCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = { Text(if (isTmdbConfigured) "Search millions of titles on TMDB..." else "Search title, actor, director...") },
                        leadingIcon = {
                            IconButton(
                                onClick = onNavigateToSearch,
                                modifier = Modifier.testTag("btn_home_open_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Open Full Search Screen",
                                    tint = CinemaGold
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSearchingTmdb) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CinemaGold,
                                    strokeWidth = 2.dp
                                )
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaGold,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Media Type Chips (All, Movies, TV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.selectedTypeFilter.value = null },
                            label = { Text("All Media") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == MediaType.MOVIE,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.MOVIE },
                            label = { Text("Movies") },
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
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == MediaType.TV_SHOW,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.TV_SHOW },
                            label = { Text("TV Shows") },
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
                            ),
                            modifier = Modifier.weight(1f)
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

            // Trending Today Horizontal Spotlight (/trending/all/day)
            if (trendingList.isNotEmpty() && searchQuery.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🔥 Trending Today",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CinemaGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "TMDB LIVE",
                                        color = CinemaGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            TextButton(
                                onClick = onNavigateToTrending,
                                modifier = Modifier.testTag("btn_see_all_trending")
                            ) {
                                Text(
                                    text = "See All",
                                    color = CinemaGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "See All Trending",
                                    tint = CinemaGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("home_trending_carousel")
                        ) {
                            itemsIndexed(
                                items = trendingList.take(10),
                                key = { index, item -> "home_trending_${item.id}_$index" }
                            ) { index, item ->
                                TrendingMiniPosterCard(
                                    rank = index + 1,
                                    media = item,
                                    onClick = { onMediaClick(item.id) }
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
                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Explore Titles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${discoverItems.size} items",
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

        // Floating Action Button to add custom title
        FloatingActionButton(
            onClick = onOpenAddDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("fab_add_custom_media"),
            containerColor = CinemaGold,
            contentColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Title")
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
fun TrendingMiniPosterCard(
    rank: Int,
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    val (rankBgColor, rankTextColor) = when (rank) {
        1 -> Pair(CinemaGold, Color.Black)
        2 -> Pair(Color(0xFFC0C0C0), Color.Black)
        3 -> Pair(Color(0xFFCD7F32), Color.White)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .width(130.dp)
            .clickable { onClick() }
            .testTag("trending_mini_card_${media.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Rank Badge in top-left
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = rankBgColor,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = rankTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Media type chip in bottom-left
                Surface(
                    shape = RoundedCornerShape(topEnd = 6.dp),
                    color = (if (isMovie) CinemaGold else CinemaCyan).copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = if (isMovie) "MOVIE" else "TV",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold
                        )
                    }

                    if (media.releaseYear > 0) {
                        Text(
                            text = media.releaseYear.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
