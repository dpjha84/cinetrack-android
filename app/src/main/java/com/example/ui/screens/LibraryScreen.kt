package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.BuildConfig
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.WatchStatusPill
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.util.LocalAdaptiveLayoutInfo
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
    val adaptive = LocalAdaptiveLayoutInfo.current
    val context = LocalContext.current
    val selectedTab by viewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val sortOption by viewModel.librarySortOption.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollectionFilter.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val libraryItems by viewModel.filteredLibrary.collectAsStateWithLifecycle()
    val favoritePeople by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()
    val isImportingCsv by viewModel.isImportingCsv.collectAsStateWithLifecycle()
    val csvImportMessage by viewModel.csvImportMessage.collectAsStateWithLifecycle()

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLibraryFromUri(context, uri)
        }
    }

    val launchCsvImport = {
        csvPickerLauncher.launch(
            arrayOf(
                "text/*",
                "text/comma-separated-values",
                "application/csv",
                "application/octet-stream"
            )
        )
    }

    val visibleFavoritePeople = remember(selectedTab, selectedCollection, favoritePeople, sortOption) {
        if (selectedTab == LibraryTab.FAVORITES && selectedCollection == null) {
            when (sortOption) {
                SortOption.TITLE -> favoritePeople.sortedBy { it.name.lowercase() }
                else -> favoritePeople.sortedByDescending { it.createdAt }
            }
        } else {
            emptyList()
        }
    }

    val headerSubtitle = remember(selectedTab, libraryItems.size, visibleFavoritePeople.size, monetizationState.streamOnMyAppsOnly) {
        val filterSuffix = if (monetizationState.streamOnMyAppsOnly) " • On My Apps" else ""
        if (selectedTab == LibraryTab.FAVORITES && visibleFavoritePeople.isNotEmpty()) {
            val total = libraryItems.size + visibleFavoritePeople.size
            "$total favorites (${libraryItems.size} titles • ${visibleFavoritePeople.size} people)$filterSuffix"
        } else {
            "${libraryItems.size} titles logged$filterSuffix"
        }
    }

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "My Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = headerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!BuildConfig.IS_V1_EDITION) {
                FilledTonalButton(
                    onClick = launchCsvImport,
                    enabled = !isImportingCsv,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CinemaGold.copy(alpha = 0.16f),
                        contentColor = CinemaGold
                    ),
                    modifier = Modifier.testTag("btn_import_csv_library")
                ) {
                    if (isImportingCsv) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = CinemaGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isImportingCsv) "Importing..." else "Import CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (!BuildConfig.IS_V1_EDITION && (isImportingCsv || !csvImportMessage.isNullOrBlank())) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CinemaCyan.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("banner_csv_import_status")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isImportingCsv) {
                            "Importing & enriching your watchlist via TMDB..."
                        } else {
                            csvImportMessage.orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isImportingCsv) {
                        IconButton(
                            onClick = { viewModel.clearCsvImportMessage() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss import notice",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                val tabIcon = when (tab) {
                    LibraryTab.WATCHING -> Icons.Default.Visibility
                    LibraryTab.PLAN_TO_WATCH -> Icons.Default.Bookmark
                    LibraryTab.COMPLETED -> Icons.Default.CheckCircle
                    LibraryTab.FAVORITES -> Icons.Default.Favorite
                    LibraryTab.ALL_TRACKED -> Icons.Default.VideoLibrary
                }
                Tab(
                    selected = selected,
                    onClick = { viewModel.selectedLibraryTab.value = tab },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = color,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // OTT Smart Filters + Sort Options + Collections Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!BuildConfig.IS_V1_EDITION) {
                FilterChip(
                    selected = monetizationState.streamOnMyAppsOnly,
                    onClick = { viewModel.setStreamOnMyAppsOnly(!monetizationState.streamOnMyAppsOnly) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Subscriptions,
                            contentDescription = null,
                            tint = if (monetizationState.streamOnMyAppsOnly) CinemaCyan else CinemaGold,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (monetizationState.isPro) "On My Apps" else "On My Apps • PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CinemaCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CinemaCyan
                    ),
                    modifier = Modifier.testTag("chip_on_my_apps")
                )

                FilterChip(
                    selected = false,
                    onClick = { viewModel.openOttMoneySaver() },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = CinemaGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "OTT Money-Saver",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("chip_ott_money_saver")
                )
            }

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
                    label = { Text(option.label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
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
                    label = { Text("All Lists", fontSize = 11.sp, maxLines = 1, softWrap = false) }
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
                        label = { Text(col, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Library Items List
        val isTabEmpty = libraryItems.isEmpty() && visibleFavoritePeople.isEmpty()
        if (isTabEmpty) {
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
                        imageVector = if (selectedTab == LibraryTab.FAVORITES) Icons.Default.Favorite else Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (monetizationState.streamOnMyAppsOnly) {
                            "No ${selectedTab.label} titles on your selected apps"
                        } else {
                            "No items in ${selectedTab.label}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (monetizationState.streamOnMyAppsOnly) {
                            "Try turning off 'On My Apps' or add more streaming apps in OTT Money-Saver."
                        } else if (selectedTab == LibraryTab.FAVORITES) {
                            "Favorite movies, TV series, or people to find them quickly here."
                        } else if (BuildConfig.IS_V1_EDITION) {
                            "Discover trending movies & TV shows and track what you watch."
                        } else {
                            "Discover titles or import your existing Moviebase, IMDb, Letterboxd, or Trakt CSV for free."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("btn_explore_from_empty_library")
                    ) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explore Titles", fontWeight = FontWeight.Bold)
                    }
                    if (!BuildConfig.IS_V1_EDITION) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = launchCsvImport,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_import_csv_empty_state")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Moviebase / IMDb CSV (Free)")
                        }
                    }
                }
            }
        } else {
            if (adaptive.isTabletOrWide) {
                val gridColumns = adaptive.cardGridColumns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 96.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (visibleFavoritePeople.isNotEmpty()) {
                        item(
                            key = "fav_people_header",
                            span = { GridItemSpan(gridColumns) }
                        ) {
                            Text(
                                text = "People (${visibleFavoritePeople.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CinemaGold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(visibleFavoritePeople, key = { "person_${it.id}" }) { person ->
                            FavoritePersonLibraryCard(
                                person = person,
                                onClick = { viewModel.openFavoritePerson(person) },
                                onToggleFavorite = { viewModel.removeFavoritePerson(person) }
                            )
                        }

                        if (libraryItems.isNotEmpty()) {
                            item(
                                key = "fav_media_header",
                                span = { GridItemSpan(gridColumns) }
                            ) {
                                Text(
                                    text = "Movies & Series (${libraryItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    items(libraryItems, key = { it.id }) { item ->
                        LibraryItemCard(
                            media = item,
                            onClick = { onMediaClick(item.id) },
                            onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                        )
                    }

                    item(
                        key = "library_sponsored_slot",
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
                                tagSuffix = "library",
                                modifier = Modifier
                                    .widthIn(max = 728.dp)
                                    .padding(top = 6.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (visibleFavoritePeople.isNotEmpty()) {
                        item(key = "fav_people_header") {
                            Text(
                                text = "People (${visibleFavoritePeople.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CinemaGold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(visibleFavoritePeople, key = { "person_${it.id}" }) { person ->
                            FavoritePersonLibraryCard(
                                person = person,
                                onClick = { viewModel.openFavoritePerson(person) },
                                onToggleFavorite = { viewModel.removeFavoritePerson(person) }
                            )
                        }

                        if (libraryItems.isNotEmpty()) {
                            item(key = "fav_media_header") {
                                Text(
                                    text = "Movies & Series (${libraryItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    items(libraryItems, key = { it.id }) { item ->
                        LibraryItemCard(
                            media = item,
                            onClick = { onMediaClick(item.id) },
                            onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                        )
                    }

                    item(key = "library_sponsored_slot") {
                        SponsoredBannerCard(
                            isPro = monetizationState.isPro,
                            onRemoveAdsClick = {
                                viewModel.openPaywall("Remove all ads across WatchPilot with WatchPilot Pro.")
                            },
                            tagSuffix = "library",
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritePersonLibraryCard(
    person: FavoritePersonEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("library_person_card_${person.id}"),
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CinemaGold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (person.profileUrl.isNotBlank()) {
                    AsyncImage(
                        model = person.profileUrl,
                        contentDescription = person.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = person.name,
                        tint = CinemaGold,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CinemaCyan.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = person.department.ifBlank { "Acting" }.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val metaLine = buildString {
                    if (person.birthday.isNotBlank()) {
                        append("Born ${person.birthday}")
                    }
                    if (person.placeOfBirth.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(person.placeOfBirth)
                    }
                }
                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Tap to view profile & filmography",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("library_person_unfav_${person.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove ${person.name} from Favorites",
                    tint = CinemaRed
                )
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
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val seasonLabel = if (media.totalSeasons == 1) "1 Season" else "${media.totalSeasons} Seasons"
                    Text(
                        text = "${media.releaseYear} • ${if (media.getMediaTypeEnum() == MediaType.MOVIE) "Movie" else seasonLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (media.customWatchlist != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${media.customWatchlist}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
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
                                color = CinemaGold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = "⭐ ${media.communityRating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )
                    }

                    if (media.getMediaTypeEnum() == MediaType.TV_SHOW && media.totalEpisodes > 0) {
                        Text(
                            text = "${media.watchedEpisodes} / ${media.totalEpisodes} eps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CinemaCyan,
                            maxLines = 1,
                            softWrap = false
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
                    Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
