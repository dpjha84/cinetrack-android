package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.WatchStatusPill
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

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
    val libraryItemsByTab by viewModel.libraryItemsByTab.collectAsStateWithLifecycle()
    val favoritePeople by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()
    val isImportingCsv by viewModel.isImportingCsv.collectAsStateWithLifecycle()
    val csvImportMessage by viewModel.csvImportMessage.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { LibraryTab.entries.size }
    )

    LaunchedEffect(pagerState.settledPage) {
        val settledTab = LibraryTab.entries[pagerState.settledPage]
        if (viewModel.selectedLibraryTab.value != settledTab) {
            viewModel.selectedLibraryTab.value = settledTab
        }
    }

    LaunchedEffect(selectedTab) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(selectedTab.ordinal)
        }
    }

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
            "$total favorites$filterSuffix"
        } else {
            "${libraryItems.size} titles$filterSuffix"
        }
    }

    val headerColors = WatchPilotColors.current

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

            FilledTonalButton(
                onClick = launchCsvImport,
                enabled = !isImportingCsv,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, headerColors.goldChipBorder),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = headerColors.goldChipContainer,
                    contentColor = headerColors.goldAccent
                ),
                modifier = Modifier.testTag("btn_import_csv_library")
            ) {
                if (isImportingCsv) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = headerColors.goldAccent,
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

        if (isImportingCsv || !csvImportMessage.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = headerColors.cyanChipContainer
                ),
                border = BorderStroke(1.dp, headerColors.cyanChipBorder),
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
                            "Importing watchlist..."
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

        if (isOffline) {
            val offlineColors = WatchPilotColors.current
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = offlineColors.goldChipContainer,
                border = BorderStroke(1.dp, offlineColors.goldChipBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("banner_offline_library")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = stringResource(R.string.offline_watchlist_banner),
                        tint = offlineColors.goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.offline_watchlist_banner),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = offlineColors.goldAccent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                val libColors = WatchPilotColors.current
                val color by animateColorAsState(
                    targetValue = if (selected) libColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onClick = {
                        viewModel.selectedLibraryTab.value = tab
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tab.ordinal)
                        }
                    },
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
        val libColors = WatchPilotColors.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = monetizationState.streamOnMyAppsOnly,
                onClick = { viewModel.setStreamOnMyAppsOnly(!monetizationState.streamOnMyAppsOnly) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Subscriptions,
                        contentDescription = null,
                        tint = if (monetizationState.streamOnMyAppsOnly) libColors.cyanAccent else libColors.goldAccent,
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
                    containerColor = libColors.neutralChipContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = libColors.cyanChipContainer,
                    selectedLabelColor = libColors.cyanAccent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = monetizationState.streamOnMyAppsOnly,
                    borderColor = libColors.neutralChipBorder,
                    selectedBorderColor = libColors.cyanChipBorder
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
                        tint = libColors.greenAccent,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = "OTT Money-Saver",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = libColors.greenAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = libColors.greenChipContainer,
                    labelColor = libColors.greenAccent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = libColors.greenChipBorder
                ),
                modifier = Modifier.testTag("chip_ott_money_saver")
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            SortOption.entries.forEach { option ->
                val isSortSelected = sortOption == option
                FilterChip(
                    selected = isSortSelected,
                    onClick = { viewModel.librarySortOption.value = option },
                    label = {
                        Text(
                            option.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = libColors.neutralChipContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = libColors.goldChipContainer,
                        selectedLabelColor = libColors.goldAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSortSelected,
                        borderColor = libColors.neutralChipBorder,
                        selectedBorderColor = libColors.goldChipBorder
                    )
                )
            }

            if (customCollections.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = selectedCollection == null,
                    onClick = { viewModel.selectedCollectionFilter.value = null },
                    label = {
                        Text(
                            "All Lists",
                            fontSize = 11.sp,
                            fontWeight = if (selectedCollection == null) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = libColors.neutralChipContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = libColors.goldChipContainer,
                        selectedLabelColor = libColors.goldAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCollection == null,
                        borderColor = libColors.neutralChipBorder,
                        selectedBorderColor = libColors.goldChipBorder
                    )
                )
                customCollections.forEach { col ->
                    val isColSelected = selectedCollection == col
                    FilterChip(
                        selected = isColSelected,
                        onClick = {
                            viewModel.selectedCollectionFilter.value =
                                if (isColSelected) null else col
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = {
                            Text(
                                col,
                                fontSize = 11.sp,
                                fontWeight = if (isColSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = libColors.neutralChipContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = libColors.goldChipContainer,
                            selectedLabelColor = libColors.goldAccent,
                            selectedLeadingIconColor = libColors.goldAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isColSelected,
                            borderColor = libColors.neutralChipBorder,
                            selectedBorderColor = libColors.goldChipBorder
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Swipeable Library Tabs Content
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("library_tab_pager")
        ) { pageIndex ->
            val pageTab = LibraryTab.entries[pageIndex]
            val pageItems = libraryItemsByTab[pageTab]
                ?: if (pageTab == selectedTab) libraryItems else emptyList()
            val pageFavoritePeople = remember(pageTab, selectedCollection, favoritePeople, sortOption) {
                if (pageTab == LibraryTab.FAVORITES && selectedCollection == null) {
                    when (sortOption) {
                        SortOption.TITLE -> favoritePeople.sortedBy { it.name.lowercase() }
                        else -> favoritePeople.sortedByDescending { it.createdAt }
                    }
                } else {
                    emptyList()
                }
            }
            val isTabEmpty = pageItems.isEmpty() && pageFavoritePeople.isEmpty()
            val tabColors = WatchPilotColors.current

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
                            imageVector = if (pageTab == LibraryTab.FAVORITES) Icons.Default.Favorite else Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (monetizationState.streamOnMyAppsOnly) {
                                "No ${pageTab.label} on your apps"
                            } else {
                                "Nothing in ${pageTab.label}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (monetizationState.streamOnMyAppsOnly) {
                                "Turn off 'On My Apps' or add more streaming apps."
                            } else if (pageTab == LibraryTab.FAVORITES) {
                                "Heart titles or people to save them here."
                            } else {
                                "Explore titles or import a CSV watchlist."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExploreClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tabColors.goldSolidBg,
                                contentColor = tabColors.onGoldSolid
                            ),
                            modifier = Modifier.testTag("btn_explore_from_empty_library")
                        ) {
                            Icon(imageVector = Icons.Default.Explore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Explore Titles", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = launchCsvImport,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = tabColors.goldChipContainer,
                                contentColor = tabColors.goldAccent
                            ),
                            border = BorderStroke(1.dp, tabColors.goldChipBorder),
                            modifier = Modifier.testTag("btn_import_csv_empty_state")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import CSV", fontWeight = FontWeight.Bold)
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
                        if (pageFavoritePeople.isNotEmpty()) {
                            item(
                                key = "fav_people_header",
                                span = { GridItemSpan(gridColumns) }
                            ) {
                                Text(
                                    text = "People (${pageFavoritePeople.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tabColors.goldAccent,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }

                            items(pageFavoritePeople, key = { "person_${it.id}" }) { person ->
                                FavoritePersonLibraryCard(
                                    person = person,
                                    onClick = { viewModel.openFavoritePerson(person) },
                                    onToggleFavorite = { viewModel.removeFavoritePerson(person) }
                                )
                            }

                            if (pageItems.isNotEmpty()) {
                                item(
                                    key = "fav_media_header",
                                    span = { GridItemSpan(gridColumns) }
                                ) {
                                    Text(
                                        text = "Movies & Series (${pageItems.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tabColors.goldAccent,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        items(pageItems, key = { it.id }) { item ->
                            LibraryItemCard(
                                media = item,
                                onClick = { onMediaClick(item.id) },
                                onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                            )
                        }

                        item(
                            key = "library_sponsored_${pageTab.name}",
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
                        if (pageFavoritePeople.isNotEmpty()) {
                            item(key = "fav_people_header") {
                                Text(
                                    text = "People (${pageFavoritePeople.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tabColors.goldAccent,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }

                            items(pageFavoritePeople, key = { "person_${it.id}" }) { person ->
                                FavoritePersonLibraryCard(
                                    person = person,
                                    onClick = { viewModel.openFavoritePerson(person) },
                                    onToggleFavorite = { viewModel.removeFavoritePerson(person) }
                                )
                            }

                            if (pageItems.isNotEmpty()) {
                                item(key = "fav_media_header") {
                                    Text(
                                        text = "Movies & Series (${pageItems.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tabColors.goldAccent,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        items(pageItems, key = { it.id }) { item ->
                            LibraryItemCard(
                                media = item,
                                onClick = { onMediaClick(item.id) },
                                onIncrementEpisode = { viewModel.incrementEpisode(item.id) }
                            )
                        }

                        item(key = "library_sponsored_${pageTab.name}") {
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
}

@Composable
fun FavoritePersonLibraryCard(
    person: FavoritePersonEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColors = WatchPilotColors.current
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
                    .background(cardColors.goldChipContainer),
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
                        tint = cardColors.goldAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cardColors.cyanChipContainer,
                    border = BorderStroke(1.dp, cardColors.cyanChipBorder)
                ) {
                    Text(
                        text = person.department.ifBlank { "Acting" }.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardColors.cyanAccent,
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
                        text = "Profile & filmography",
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
                    tint = cardColors.redAccent
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
    val itemColors = WatchPilotColors.current
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (media.getMediaTypeEnum() == MediaType.MOVIE) Icons.Default.Movie else Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.size(26.dp)
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
                            tint = itemColors.redAccent,
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
                            color = itemColors.goldAccent,
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
                                tint = itemColors.goldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = media.getFormattedUserRating(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = itemColors.goldAccent,
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
                            color = itemColors.cyanAccent,
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
                        color = itemColors.goldSolidBg,
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
                    border = BorderStroke(1.dp, itemColors.goldChipBorder),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = itemColors.goldChipContainer,
                        contentColor = itemColors.goldAccent
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
