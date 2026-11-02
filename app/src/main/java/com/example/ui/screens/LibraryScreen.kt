package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.WatchStatus
import com.example.ui.components.WatchStatusPill
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaRed
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.SortOption

@Composable
fun LibraryScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val sortOption by viewModel.librarySortOption.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollectionFilter.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val libraryItems by viewModel.filteredLibrary.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Watchlist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${libraryItems.size} titles logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CinemaGold.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIBRARY",
                        color = CinemaGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Library Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 16.dp,
            divider = {},
            containerColor = Color.Transparent
        ) {
            LibraryTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val color by animateColorAsState(
                    targetValue = if (selected) CinemaGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabColor"
                )
                Tab(
                    selected = selected,
                    onClick = { viewModel.selectedLibraryTab.value = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = color
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sort Options & Collections Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            SortOption.entries.forEach { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { viewModel.librarySortOption.value = option },
                    label = { Text(option.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CinemaGold.copy(alpha = 0.2f),
                        selectedLabelColor = CinemaGold
                    )
                )
            }

            if (customCollections.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = selectedCollection == null,
                    onClick = { viewModel.selectedCollectionFilter.value = null },
                    label = { Text("All Lists", fontSize = 11.sp) }
                )
                customCollections.forEach { col ->
                    FilterChip(
                        selected = selectedCollection == col,
                        onClick = {
                            viewModel.selectedCollectionFilter.value =
                                if (selectedCollection == col) null else col
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = { Text(col, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Library Items List
        if (libraryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No titles in ${selectedTab.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Discover movies and TV series to start tracking your watch journey.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explore Titles", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(libraryItems, key = { it.id }) { item ->
                    LibraryItemCard(
                        media = item,
                        onClick = { onMediaClick(item.id) },
                        onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryItemCard(
    media: MediaEntity,
    onClick: () -> Unit,
    onIncrementEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("library_card_${media.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WatchStatusPill(status = media.getWatchStatusEnum())

                    if (media.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = CinemaRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${media.releaseYear} • ${if (media.getMediaTypeEnum() == MediaType.MOVIE) "Movie" else "${media.totalSeasons} Season"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (media.customWatchlist != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${media.customWatchlist}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaGold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rating & Episodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (media.userRating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = media.getFormattedUserRating(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CinemaGold
                            )
                        }
                    } else {
                        Text(
                            text = "⭐ ${media.communityRating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (media.getMediaTypeEnum() == MediaType.TV_SHOW && media.totalEpisodes > 0) {
                        Text(
                            text = "${media.watchedEpisodes} / ${media.totalEpisodes} eps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CinemaCyan
                        )
                    }
                }

                // Progress Bar for TV Shows
                if (media.getMediaTypeEnum() == MediaType.TV_SHOW && media.totalEpisodes > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { media.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CinemaGold,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }

                // Snippet of personal review if present
                if (media.userReview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${media.userReview}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick +1 button for watching TV shows
            if (media.getMediaTypeEnum() == MediaType.TV_SHOW &&
                media.getWatchStatusEnum() == WatchStatus.WATCHING &&
                media.watchedEpisodes < media.totalEpisodes
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onIncrementEpisode,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CinemaGold.copy(alpha = 0.2f),
                        contentColor = CinemaGold
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Episode",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
