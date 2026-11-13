package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.MultiSearchCategory
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.TopRatedTab

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
    val isTmdbConfigured by viewModel.isTmdbConfigured.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val allTrackedMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val selectedTopRatedTab by viewModel.selectedTopRatedTab.collectAsStateWithLifecycle()
    val topRatedList by viewModel.currentTopRatedList.collectAsStateWithLifecycle()
    val isTopRatedLoading by viewModel.isTopRatedLoading.collectAsStateWithLifecycle()

    var showLocalizationDialog by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
                placeholder = {
                    Text("Search Movieverse...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CinemaGold
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CinemaGold,
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
                                tint = if (isTmdbConfigured) CinemaGreen else CinemaGold,
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
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                Text(category.label)
                                if (searchQuery.isNotBlank() || selectedSearchGenre != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($count)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.surface else CinemaGold
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = MaterialTheme.colorScheme.surface,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                    )
                }
            }

            AnimatedVisibility(visible = selectedSearchGenre != null) {
                selectedSearchGenre?.let { activeGenre ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CinemaCyan.copy(alpha = 0.14f),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = CinemaCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Genre: $activeGenre",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaCyan
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
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
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                searchQuery.isBlank() && selectedSearchGenre == null -> {
                    // Empty Query Screen with Trending/Suggested Searches & TMDB Top 10
                    EmptyQueryView(
                        isSearchFocused = isSearchFocused,
                        recentHistory = recentHistory,
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
                                color = CinemaGold,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "Discovering $selectedSearchGenre titles..."
                                } else {
                                    "Searching live catalog..."
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
                                tint = CinemaGold.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "No $selectedSearchGenre titles found"
                                } else {
                                    "No results found for \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedSearchGenre != null) {
                                    "Try switching the Movies/TV filter or choose another genre."
                                } else {
                                    "Try searching by exact actor or director name, title in English or original language, or check your API key."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearSearchGenre()
                                    viewModel.setMultiSearchQuery("")
                                },
                                modifier = Modifier.testTag("btn_reset_search")
                            ) {
                                Text(if (selectedSearchGenre != null) "Clear Genre Filter" else "Clear Search")
                            }
                        }
                    }
                }

                else -> {
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
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = media.posterUrl.ifBlank { media.backdropUrl },
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (media.posterUrl.isNotBlank()) ContentScale.Crop else ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isMovie) CinemaGold.copy(alpha = 0.25f) else CinemaCyan.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = if (isMovie) "MOVIE" else "TV SERIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovie) CinemaGold else CinemaCyan,
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
                            tint = CinemaGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold
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
                        color = if (isInWatchlist) CinemaGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
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
                                tint = if (isInWatchlist) CinemaGreen else CinemaGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInWatchlist) "In Watchlist" else "+ Watchlist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInWatchlist) CinemaGreen else CinemaGold
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
                        .background(CinemaGold.copy(alpha = 0.2f)),
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
                            tint = CinemaGold,
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
                            color = CinemaGold.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = person.department.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CinemaGold,
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
                        text = "Tap to view bio & filmography",
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaCyan,
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
                    items(person.knownFor) { media ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
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
                                        color = CinemaGold
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
fun EmptyQueryView(
    isSearchFocused: Boolean,
    recentHistory: List<String>,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Recent Search History (ONLY shown when user clicks / focuses on the search text box)
        if (isSearchFocused && recentHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CinemaCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                TextButton(
                    onClick = onClearAllHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("btn_clear_all_recent_searches")
                ) {
                    Text(
                        text = "Clear all",
                        color = CinemaGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            recentHistory.forEach { item ->
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CinemaGold.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onSuggestionClick(item) }
                            ) {
                                Text(
                                    text = "Re-run",
                                    fontSize = 11.sp,
                                    color = CinemaGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
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
                tint = CinemaGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Popular & Trending Searches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        // Browse by Genre Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = CinemaCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Explore by Popular Genres",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            popularGenres.forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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

        // Top 10 Rated on TMDB
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
                    tint = CinemaGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TMDB Top 10 Rated",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Switcher between Movies and TV Series
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTopRatedTab == TopRatedTab.MOVIES,
                onClick = { onSelectTopRatedTab(TopRatedTab.MOVIES) },
                label = { Text("Top 10 Movies", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CinemaGold,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
                ),
                modifier = Modifier.testTag("tab_top_rated_movies")
            )

            FilterChip(
                selected = selectedTopRatedTab == TopRatedTab.TV_SERIES,
                onClick = { onSelectTopRatedTab(TopRatedTab.TV_SERIES) },
                label = { Text("Top 10 TV Series", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CinemaCyan,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
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
                    color = CinemaGold,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topRatedList.take(10).forEachIndexed { index, item ->
                    val currentEntity = allTrackedMedia.find { it.id == item.id } ?: item
                    val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMediaClick(currentEntity) }
                            .testTag("top_rated_item_${currentEntity.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank badge (#1..#10)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (index < 3) CinemaGold else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 2:3 aspect ratio poster thumbnail
                            AsyncImage(
                                model = currentEntity.posterUrl.ifBlank { currentEntity.backdropUrl },
                                contentDescription = currentEntity.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(58.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details & + Watchlist action
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentEntity.title,
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
                                        color = CinemaGold.copy(alpha = 0.18f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = CinemaGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format("%.1f", currentEntity.communityRating),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CinemaGold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${currentEntity.releaseYear}" + if (currentEntity.runtimeMinutes > 0) " • ${currentEntity.runtimeMinutes}m" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                if (currentEntity.genres.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentEntity.genres,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaCyan.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (currentEntity.overview.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentEntity.overview,
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
                                        color = if (isInWatchlist) CinemaGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable { onToggleWatchlist(currentEntity) }
                                            .testTag("btn_top_rated_watchlist_${currentEntity.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isInWatchlist) "In Watchlist" else "Add to Watchlist",
                                                tint = if (isInWatchlist) CinemaGreen else CinemaGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isInWatchlist) "In Watchlist" else "+ Watchlist",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isInWatchlist) CinemaGreen else CinemaGold
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
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
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
                tint = CinemaGold,
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
