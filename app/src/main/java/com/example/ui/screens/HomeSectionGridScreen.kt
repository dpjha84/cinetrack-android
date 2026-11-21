package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MediaEntity
import com.example.data.model.HomeCatalogSection
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.ui.components.CompactMediaPosterCard
import com.example.ui.components.CompactPersonCircleCard
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.util.LocalAdaptiveLayoutInfo
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun HomeSectionGridScreen(
    section: HomeCatalogSection,
    regionName: String,
    items: List<MediaEntity>,
    totalAvailableCount: Int,
    selectedType: MediaType?,
    selectedGenre: String?,
    availableGenres: List<String>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onSelectType: (MediaType?) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onMediaClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    people: List<TmdbSearchResultItem.Person> = emptyList(),
    onPersonClick: (TmdbSearchResultItem.Person) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val isPeopleSection = section == HomeCatalogSection.POPULAR_PEOPLE
    val adaptive = LocalAdaptiveLayoutInfo.current
    // Strictly 3 items per row on phones; adaptive 4-5 on tablets
    val columns = when {
        adaptive.isExpanded -> 5
        adaptive.isTabletOrWide -> 4
        else -> 3
    }

    val gridState = rememberLazyGridState()
    val displayedCount = if (isPeopleSection) people.size else items.size

    // Infinite scrolling trigger: load/populate new data from DB & TMDB before bottom is reached
    LaunchedEffect(gridState, displayedCount, isLoadingMore, hasMore, selectedType, selectedGenre, section) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = layoutInfo.totalItemsCount
            lastVisible to totalItems
        }
            .map { (lastVisible, totalItems) ->
                totalItems > 0 && lastVisible >= (totalItems - (columns * 2)).coerceAtLeast(0)
            }
            .distinctUntilChanged()
            .collect { nearBottom ->
                if (nearBottom && !isLoadingMore && hasMore) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_section_grid_screen_${section.name.lowercase()}")
    ) {
        // Top Header with Back Action
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("section_grid_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPeopleSection) "Popular People" else "${section.displayPrefix} in $regionName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val filterLabel = when (selectedType) {
                            MediaType.MOVIE -> "Movies"
                            MediaType.TV_SHOW -> "TV Shows"
                            null -> "All Media"
                        }
                        val countUnit = if (isPeopleSection) "people" else "titles"
                        val genreSuffix = if (!isPeopleSection && selectedGenre != null) " • $selectedGenre" else ""
                        Text(
                            text = "$displayedCount $countUnit • $filterLabel$genreSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Media Type Filter Chips (All Media, Movies, TV Shows)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onSelectType(null) },
                        label = { Text("All Media", maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CinemaGold,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("section_filter_all")
                    )
                    FilterChip(
                        selected = selectedType == MediaType.MOVIE,
                        onClick = { onSelectType(MediaType.MOVIE) },
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
                        ),
                        modifier = Modifier.testTag("section_filter_movies")
                    )
                    FilterChip(
                        selected = selectedType == MediaType.TV_SHOW,
                        onClick = { onSelectType(MediaType.TV_SHOW) },
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
                        ),
                        modifier = Modifier.testTag("section_filter_tv")
                    )
                }

                if (!isPeopleSection) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Genre Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedGenre == null,
                            onClick = { onSelectGenre(null) },
                            label = { Text("All Genres", fontSize = 12.sp) }
                        )

                        availableGenres.forEach { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = {
                                    onSelectGenre(if (selectedGenre == genre) null else genre)
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
        }

        val isEmpty = if (isPeopleSection) people.isEmpty() else items.isEmpty()
        if (isEmpty && !isLoadingMore) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isPeopleSection) "No matching people" else "No matching titles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try another filter or reset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedType != null || selectedGenre != null) {
                            OutlinedButton(
                                onClick = {
                                    onSelectType(null)
                                    onSelectGenre(null)
                                }
                            ) {
                                Text("Reset Filters")
                            }
                        }
                        if (hasMore) {
                            TextButton(onClick = onLoadMore) {
                                Text("Load More", color = CinemaGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("section_grid_list_${section.name.lowercase()}")
            ) {
                if (isPeopleSection) {
                    items(
                        items = people,
                        key = { person -> "grid_popular_people_${person.id}" }
                    ) { person ->
                        CompactPersonCircleCard(
                            person = person,
                            onClick = { onPersonClick(person) },
                            modifier = Modifier.fillMaxWidth(),
                            testTagPrefix = "grid_popular_people_card"
                        )
                    }
                } else {
                    items(
                        items = items,
                        key = { media -> "grid_${section.name}_${media.id}" }
                    ) { media ->
                        CompactMediaPosterCard(
                            media = media,
                            onClick = { onMediaClick(media.id) },
                            modifier = Modifier.fillMaxWidth(),
                            testTagPrefix = "grid_${section.name.lowercase()}_card"
                        )
                    }
                }

                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoadingMore -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = CinemaGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Loading more...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            hasMore -> {
                                TextButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.testTag("section_grid_load_more")
                                ) {
                                    Text(
                                        text = "Load More",
                                        color = CinemaGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            displayedCount > 0 -> {
                                Text(
                                    text = "All ${maxOf(displayedCount, totalAvailableCount)} loaded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
