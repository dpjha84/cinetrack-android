package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.local.MediaEntity
import com.example.data.model.MultiSearchCategory
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.TopRatedTab
import com.example.util.FeatureFlags

@Composable
fun SearchScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.multiSearchQuery.collectAsStateWithLifecycle()
    val selectedSearchGenre by viewModel.selectedSearchGenre.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedSearchCategory.collectAsStateWithLifecycle()
    val searchResults by viewModel.filteredMultiSearchResults.collectAsStateWithLifecycle()
    val searchCounts by viewModel.searchCounts.collectAsStateWithLifecycle()
    val isSearching by viewModel.isMultiSearching.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentSearchHistory.collectAsStateWithLifecycle()
    val searchHistoryEntries by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val isTmdbConfigured by viewModel.isTmdbConfigured.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val allTrackedMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val selectedTopRatedTab by viewModel.selectedTopRatedTab.collectAsStateWithLifecycle()
    val topRatedList by viewModel.currentTopRatedList.collectAsStateWithLifecycle()
    val isTopRatedLoading by viewModel.isTopRatedLoading.collectAsStateWithLifecycle()

    val historyCountMap = remember(searchHistoryEntries) {
        searchHistoryEntries.associate { it.query to it.resultCount }
    }
    val adaptive = LocalAdaptiveLayoutInfo.current
    var showLocalizationDialog by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchColors = WatchPilotColors.current

    BackHandler(enabled = selectedSearchGenre != null || searchQuery.isNotBlank()) {
        viewModel.clearSearchGenre()
        viewModel.setMultiSearchQuery("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search Bar & Category Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (adaptive.isTabletOrWide) 1040.dp else Dp.Unspecified)
                .align(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
        ) {
            // Search textfield
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setMultiSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .testTag("multi_search_input"),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "Search movies, TV & people",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = searchColors.goldAccent
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = searchColors.goldAccent,
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty() || selectedSearchGenre != null) {
                        IconButton(
                            onClick = {
                                viewModel.clearSearchGenre()
                                viewModel.setMultiSearchQuery("")
                            },
                            modifier = Modifier.testTag("btn_clear_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showLocalizationDialog = true },
                            modifier = Modifier.testTag("btn_search_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Catalog & Region Settings",
                                tint = if (isTmdbConfigured) searchColors.greenAccent else searchColors.goldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.submitMultiSearch(searchQuery)
                        focusManager.clearFocus()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = searchColors.goldAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (FeatureFlags.ENABLE_AI_FEATURES) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.openSnapScanner() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Scan Screenshot",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Scan Screenshot",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = searchColors.cyanChipContainer,
                            labelColor = searchColors.cyanAccent,
                            iconColor = searchColors.cyanAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = searchColors.cyanChipBorder
                        ),
                        modifier = Modifier.testTag("search_btn_snap_scan")
                    )
                }

                MultiSearchCategory.entries.forEach { category ->
                    val selected = selectedCategory == category
                    val count = when (category) {
                        MultiSearchCategory.ALL -> searchCounts.all
                        MultiSearchCategory.MOVIES -> searchCounts.movies
                        MultiSearchCategory.TV_SHOWS -> searchCounts.tvShows
                        MultiSearchCategory.PEOPLE -> searchCounts.people
                    }

                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setMultiSearchCategory(category) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (category) {
                                    MultiSearchCategory.ALL -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(14.dp))
                                    MultiSearchCategory.MOVIES -> Icon(Icons.Default.Movie, null, modifier = Modifier.size(14.dp))
                                    MultiSearchCategory.TV_SHOWS -> Icon(Icons.Default.Tv, null, modifier = Modifier.size(14.dp))
                                    MultiSearchCategory.PEOPLE -> Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(category.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                if (searchQuery.isNotBlank() || selectedSearchGenre != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($count)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) searchColors.onGoldSolid else searchColors.goldAccent
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = searchColors.neutralChipContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = searchColors.goldSolidBg,
                            selectedLabelColor = searchColors.onGoldSolid,
                            selectedLeadingIconColor = searchColors.onGoldSolid
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = searchColors.neutralChipBorder,
                            selectedBorderColor = searchColors.goldChipBorder
                        ),
                        modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                    )
                }
            }

            AnimatedVisibility(visible = selectedSearchGenre != null) {
                selectedSearchGenre?.let { activeGenre ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = searchColors.cyanChipContainer,
                        border = BorderStroke(1.dp, searchColors.cyanChipBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = searchColors.cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Genre: $activeGenre",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = searchColors.cyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, searchColors.neutralChipBorder),
                                modifier = Modifier
                                    .clickable { viewModel.clearSearchGenre() }
                                    .testTag("btn_clear_genre")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear genre filter",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = isOffline) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = searchColors.goldChipContainer,
                    border = BorderStroke(1.dp, searchColors.goldChipBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("banner_offline_search")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = stringResource(R.string.offline_search_banner),
                            tint = searchColors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.offline_search_banner),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = searchColors.goldAccent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                searchQuery.isBlank() && selectedSearchGenre == null -> {
                    // Empty Query Screen with Trending/Suggested Searches & IMDb Top 10
                    EmptyQueryView(
                        isSearchFocused = isSearchFocused,
                        isOffline = isOffline,
                        recentHistory = recentHistory,
                        historyCountMap = historyCountMap,
                        allTrackedMedia = allTrackedMedia,
                        onSuggestionClick = {
                            viewModel.submitMultiSearch(it)
                            focusManager.clearFocus()
                        },
                        onGenreClick = { viewModel.selectSearchGenre(it) },
                        onDeleteRecentClick = { viewModel.deleteRecentSearch(it) },
                        onClearAllHistory = { viewModel.clearAllSearchHistory() },
                        onOpenSettings = { showLocalizationDialog = true },
                        isTmdbLive = isTmdbConfigured,
                        regionCode = effectiveRegion,
                        selectedTopRatedTab = selectedTopRatedTab,
                        topRatedList = topRatedList,
                        isTopRatedLoading = isTopRatedLoading,
                        onSelectTopRatedTab = { viewModel.setTopRatedTab(it) },
                        onMediaClick = { entity ->
                            viewModel.openMediaFromSearch(entity)
                            onMediaClick(entity.id)
                        },
                        onToggleWatchlist = { entity ->
                            viewModel.toggleWatchlistQuick(entity)
                        }
                    )
                }

                isSearching && searchResults.isEmpty() -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = searchColors.goldAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "Loading $selectedSearchGenre..."
                                } else {
                                    "Searching..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                searchResults.isEmpty() -> {
                    // No Results State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = searchColors.goldAccent.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "No $selectedSearchGenre titles"
                                } else {
                                    "No results"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "Try another filter or genre."
                                } else {
                                    "Try another title, actor, or keyword."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearSearchGenre()
                                    viewModel.setMultiSearchQuery("")
                                },
                                modifier = Modifier.testTag("btn_reset_search")
                            ) {
                                Text(if (selectedSearchGenre != null) "Clear Genre" else "Clear Search")
                            }
                        }
                    }
                }

                else -> {
                    if (adaptive.isTabletOrWide) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(adaptive.cardGridColumns),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_list")
                        ) {
                            items(searchResults, key = { item ->
                                when (item) {
                                    is TmdbSearchResultItem.Media -> "media_${item.entity.id}"
                                    is TmdbSearchResultItem.Person -> "person_${item.id}"
                                }
                            }) { item ->
                                when (item) {
                                    is TmdbSearchResultItem.Media -> {
                                        val currentEntity = allTrackedMedia.find { it.id == item.entity.id } ?: item.entity
                                        val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE

                                        SearchResultMediaCard(
                                            media = currentEntity,
                                            isMovie = item.isMovie,
                                            isInWatchlist = isInWatchlist,
                                            onCardClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.openMediaFromSearch(currentEntity)
                                                onMediaClick(currentEntity.id)
                                            },
                                            onToggleWatchlist = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.toggleWatchlistQuick(currentEntity)
                                            }
                                        )
                                    }

                                    is TmdbSearchResultItem.Person -> {
                                        SearchResultPersonCard(
                                            person = item,
                                            onPersonClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.selectPersonForDetail(item)
                                            },
                                            onMediaClick = { media ->
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.openMediaFromSearch(media)
                                                onMediaClick(media.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Results List
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_list")
                        ) {
                            items(searchResults, key = { item ->
                                when (item) {
                                    is TmdbSearchResultItem.Media -> "media_${item.entity.id}"
                                    is TmdbSearchResultItem.Person -> "person_${item.id}"
                                }
                            }) { item ->
                                when (item) {
                                    is TmdbSearchResultItem.Media -> {
                                        val currentEntity = allTrackedMedia.find { it.id == item.entity.id } ?: item.entity
                                        val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE

                                        SearchResultMediaCard(
                                            media = currentEntity,
                                            isMovie = item.isMovie,
                                            isInWatchlist = isInWatchlist,
                                            onCardClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.openMediaFromSearch(currentEntity)
                                                onMediaClick(currentEntity.id)
                                            },
                                            onToggleWatchlist = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.toggleWatchlistQuick(currentEntity)
                                            }
                                        )
                                    }

                                    is TmdbSearchResultItem.Person -> {
                                        SearchResultPersonCard(
                                            person = item,
                                            onPersonClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.selectPersonForDetail(item)
                                            },
                                            onMediaClick = { media ->
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.recordCompletedSearch(searchQuery)
                                                }
                                                viewModel.openMediaFromSearch(media)
                                                onMediaClick(media.id)
                                            }
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

    // Localization & TMDB settings dialog
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
fun SearchResultMediaCard(
    media: MediaEntity,
    isMovie: Boolean,
    isInWatchlist: Boolean,
    onCardClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("search_card_${media.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                )
                val posterSource = media.posterUrl.ifBlank { media.backdropUrl }
                if (posterSource.isNotBlank()) {
                    AsyncImage(
                        model = posterSource,
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                val resultColors = WatchPilotColors.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isMovie) resultColors.goldChipContainer else resultColors.cyanChipContainer,
                        border = BorderStroke(1.dp, if (isMovie) resultColors.goldChipBorder else resultColors.cyanChipBorder)
                    ) {
                        Text(
                            text = if (isMovie) "MOVIE" else "TV SERIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovie) resultColors.goldAccent else resultColors.cyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${media.releaseYear}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = resultColors.goldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = resultColors.goldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (media.genres.isNotBlank()) {
                    Text(
                        text = media.genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (media.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = media.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Watchlist button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isInWatchlist) resultColors.greenChipContainer else resultColors.goldChipContainer,
                        border = BorderStroke(1.dp, if (isInWatchlist) resultColors.greenChipBorder else resultColors.goldChipBorder),
                        modifier = Modifier
                            .clickable(onClick = onToggleWatchlist)
                            .testTag("btn_toggle_watchlist_${media.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isInWatchlist) resultColors.greenAccent else resultColors.goldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInWatchlist) "In Watchlist" else "+ Watchlist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInWatchlist) resultColors.greenAccent else resultColors.goldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultPersonCard(
    person: TmdbSearchResultItem.Person,
    onPersonClick: () -> Unit,
    onMediaClick: (MediaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val personColors = WatchPilotColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPersonClick)
            .testTag("person_card_${person.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(personColors.goldChipContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!person.profileUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = person.profileUrl,
                            contentDescription = person.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = personColors.goldAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = personColors.goldChipContainer,
                            border = BorderStroke(1.dp, personColors.goldChipBorder)
                        ) {
                            Text(
                                text = person.department.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = personColors.goldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (person.popularity > 0) {
                            Text(
                                text = "• Popularity ${String.format("%.1f", person.popularity)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Bio & filmography",
                        style = MaterialTheme.typography.bodySmall,
                        color = personColors.cyanAccent,
                        fontSize = 11.sp
                    )
                }
            }

            // Known For List
            if (person.knownFor.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Known for:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(person.knownFor, key = { it.id }) { media ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, personColors.neutralChipBorder),
                            modifier = Modifier
                                .clickable { onMediaClick(media) }
                                .testTag("known_for_${media.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (media.posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = media.posterUrl,
                                        contentDescription = media.title,
                                        modifier = Modifier
                                            .size(20.dp, 30.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column {
                                    Text(
                                        text = media.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${media.releaseYear} • ★ ${String.format("%.1f", media.communityRating)}",
                                        fontSize = 10.sp,
                                        color = personColors.goldAccent
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyQueryView(
    isSearchFocused: Boolean,
    isOffline: Boolean = false,
    recentHistory: List<String>,
    historyCountMap: Map<String, Int> = emptyMap(),
    allTrackedMedia: List<MediaEntity> = emptyList(),
    onSuggestionClick: (String) -> Unit,
    onGenreClick: (String) -> Unit = {},
    onDeleteRecentClick: (String) -> Unit = {},
    onClearAllHistory: () -> Unit = {},
    onOpenSettings: () -> Unit,
    isTmdbLive: Boolean,
    regionCode: String,
    selectedTopRatedTab: TopRatedTab = TopRatedTab.MOVIES,
    topRatedList: List<MediaEntity> = emptyList(),
    isTopRatedLoading: Boolean = false,
    onSelectTopRatedTab: (TopRatedTab) -> Unit = {},
    onMediaClick: (MediaEntity) -> Unit = {},
    onToggleWatchlist: (MediaEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val emptyColors = WatchPilotColors.current
    val trendingSuggestions = listOf(
        "Christopher Nolan",
        "Dune: Part Two",
        "Zendaya",
        "Stranger Things",
        "Oppenheimer",
        "Pedro Pascal",
        "Denis Villeneuve",
        "The Last of Us",
        "Florence Pugh",
        "Cillian Murphy",
        "Breaking Bad",
        "Interstellar"
    )

    val popularGenres = listOf(
        "Action", "Sci-Fi", "Thriller", "Comedy", "Drama", "Horror", "Animation", "Crime"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (adaptive.isTabletOrWide) 1080.dp else Dp.Unspecified)
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
        // Recent Search History (shown when focused OR offline so cached queries are immediately accessible)
        if ((isSearchFocused || isOffline) && recentHistory.isNotEmpty()) {
            val historyColors = WatchPilotColors.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = historyColors.cyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = onClearAllHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("btn_clear_all_recent_searches")
                ) {
                    Text(
                        text = "Clear all",
                        color = historyColors.goldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            recentHistory.forEach { item ->
                val cachedCount = historyCountMap[item] ?: 0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSuggestionClick(item) }
                        .testTag("recent_search_item_$item")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (cachedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = historyColors.cyanChipContainer,
                                    border = BorderStroke(1.dp, historyColors.cyanChipBorder),
                                    modifier = Modifier.testTag("recent_search_cached_count_$item")
                                ) {
                                    Text(
                                        text = stringResource(R.string.offline_search_cached_count, cachedCount),
                                        fontSize = 10.sp,
                                        color = historyColors.cyanAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = historyColors.goldChipContainer,
                                border = BorderStroke(1.dp, historyColors.goldChipBorder),
                                modifier = Modifier.clickable { onSuggestionClick(item) }
                            ) {
                                Text(
                                    text = "Re-run",
                                    fontSize = 11.sp,
                                    color = historyColors.goldAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteRecentClick(item) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_delete_recent_$item")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove recent query",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Suggested Searches Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = emptyColors.goldAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Trending Searches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (adaptive.isTabletOrWide) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trendingSuggestions.forEach { tag ->
                    SuggestionChip(
                        text = tag,
                        onClick = { onSuggestionClick(tag) }
                    )
                }
            }
        } else {
            // Single scrollable row of trending suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trendingSuggestions.forEach { tag ->
                    SuggestionChip(
                        text = tag,
                        onClick = { onSuggestionClick(tag) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Browse by Genre Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = emptyColors.cyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Genres",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (adaptive.isTabletOrWide) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularGenres.forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = emptyColors.neutralChipContainer,
                        border = BorderStroke(1.dp, emptyColors.neutralChipBorder),
                        modifier = Modifier
                            .clickable { onGenreClick(genre) }
                            .testTag("genre_chip_$genre")
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularGenres.forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = emptyColors.neutralChipContainer,
                        border = BorderStroke(1.dp, emptyColors.neutralChipBorder),
                        modifier = Modifier
                            .clickable { onGenreClick(genre) }
                            .testTag("genre_chip_$genre")
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // IMDb Top 10 Charts (Movies & TV Shows)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = emptyColors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IMDb Top 10",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = emptyColors.goldChipContainer,
                border = BorderStroke(1.dp, emptyColors.goldChipBorder)
            ) {
                Text(
                    text = "IMDb Chart",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = emptyColors.goldAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Switcher between Movies and TV Series
        val topRatedColors = WatchPilotColors.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTopRatedTab == TopRatedTab.MOVIES,
                onClick = { onSelectTopRatedTab(TopRatedTab.MOVIES) },
                label = { Text("Movies", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = topRatedColors.neutralChipContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = topRatedColors.goldSolidBg,
                    selectedLabelColor = topRatedColors.onGoldSolid,
                    selectedLeadingIconColor = topRatedColors.onGoldSolid
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedTopRatedTab == TopRatedTab.MOVIES,
                    borderColor = topRatedColors.neutralChipBorder,
                    selectedBorderColor = topRatedColors.goldChipBorder
                ),
                modifier = Modifier.testTag("tab_top_rated_movies")
            )

            FilterChip(
                selected = selectedTopRatedTab == TopRatedTab.TV_SERIES,
                onClick = { onSelectTopRatedTab(TopRatedTab.TV_SERIES) },
                label = { Text("TV Shows", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = topRatedColors.neutralChipContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = topRatedColors.cyanSolidBg,
                    selectedLabelColor = topRatedColors.onCyanSolid,
                    selectedLeadingIconColor = topRatedColors.onCyanSolid
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedTopRatedTab == TopRatedTab.TV_SERIES,
                    borderColor = topRatedColors.neutralChipBorder,
                    selectedBorderColor = topRatedColors.cyanChipBorder
                ),
                modifier = Modifier.testTag("tab_top_rated_tv")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isTopRatedLoading && topRatedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = topRatedColors.goldAccent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (topRatedList.isEmpty()) {
            Text(
                text = "No top rated titles found.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            val rankedTopTen = topRatedList.take(10)
            if (adaptive.isTabletOrWide) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rankedTopTen.mapIndexed { idx, item -> idx to item }.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { (index, item) ->
                                val currentEntity = allTrackedMedia.find { it.id == item.id } ?: item
                                val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE
                                TopRatedEntryCard(
                                    rankIndex = index,
                                    entity = currentEntity,
                                    isInWatchlist = isInWatchlist,
                                    onMediaClick = { onMediaClick(currentEntity) },
                                    onToggleWatchlist = { onToggleWatchlist(currentEntity) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rankedTopTen.forEachIndexed { index, item ->
                        val currentEntity = allTrackedMedia.find { it.id == item.id } ?: item
                        val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE
                        TopRatedEntryCard(
                            rankIndex = index,
                            entity = currentEntity,
                            isInWatchlist = isInWatchlist,
                            onMediaClick = { onMediaClick(currentEntity) },
                            onToggleWatchlist = { onToggleWatchlist(currentEntity) }
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TopRatedEntryCard(
    rankIndex: Int,
    entity: MediaEntity,
    isInWatchlist: Boolean,
    onMediaClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onMediaClick)
            .testTag("top_rated_item_${entity.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val topRatedCardColors = WatchPilotColors.current
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (rankIndex < 3) topRatedCardColors.goldSolidBg else topRatedCardColors.neutralChipContainer,
                border = BorderStroke(1.dp, if (rankIndex < 3) topRatedCardColors.goldChipBorder else topRatedCardColors.neutralChipBorder),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${rankIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rankIndex < 3) topRatedCardColors.onGoldSolid else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            AsyncImage(
                model = entity.posterUrl.ifBlank { entity.backdropUrl },
                contentDescription = entity.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(58.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = topRatedCardColors.goldChipContainer,
                        border = BorderStroke(1.dp, topRatedCardColors.goldChipBorder),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = topRatedCardColors.goldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "IMDb %.1f", entity.communityRating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = topRatedCardColors.goldAccent
                            )
                        }
                    }

                    Text(
                        text = "${entity.releaseYear}" + if (entity.runtimeMinutes > 0) " • ${entity.runtimeMinutes}m" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (entity.genres.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entity.genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = topRatedCardColors.cyanAccent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (entity.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entity.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isInWatchlist) topRatedCardColors.greenChipContainer else topRatedCardColors.goldChipContainer,
                        border = BorderStroke(1.dp, if (isInWatchlist) topRatedCardColors.greenChipBorder else topRatedCardColors.goldChipBorder),
                        modifier = Modifier
                            .clickable(onClick = onToggleWatchlist)
                            .testTag("btn_top_rated_watchlist_${entity.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                contentDescription = if (isInWatchlist) "In Watchlist" else "Add to Watchlist",
                                tint = if (isInWatchlist) topRatedCardColors.greenAccent else topRatedCardColors.goldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInWatchlist) "In Watchlist" else "+ Watchlist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInWatchlist) topRatedCardColors.greenAccent else topRatedCardColors.goldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColors = WatchPilotColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = chipColors.neutralChipContainer,
        border = BorderStroke(1.dp, chipColors.neutralChipBorder),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("suggestion_chip_$text")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = chipColors.goldAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
