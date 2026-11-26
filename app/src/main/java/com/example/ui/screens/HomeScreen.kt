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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.local.MediaEntity
import com.example.data.model.HomeCatalogSection
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.ui.components.CompactMediaPosterCard
import com.example.ui.components.CompactPersonCircleCard
import com.example.ui.components.ContinueWatchingCard
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.RegionalDefaults
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.WatchStatusPill
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.PersonalizationEngine
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.FeatureFlags

@Composable
fun HomeScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    onPersonClick: (TmdbSearchResultItem.Person) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onOpenSection: (HomeCatalogSection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val continueWatchingList by viewModel.continueWatching.collectAsStateWithLifecycle()
    val discoverItems by viewModel.filteredDiscover.collectAsStateWithLifecycle()
    val popularHomeItems by viewModel.popularHomeItems.collectAsStateWithLifecycle()
    val popularPeopleHomeItems by viewModel.popularPeopleHomeItems.collectAsStateWithLifecycle()
    val nowPlayingHomeItems by viewModel.nowPlayingHomeItems.collectAsStateWithLifecycle()
    val upcomingHomeItems by viewModel.upcomingHomeItems.collectAsStateWithLifecycle()
    val isHomeCatalogRefreshing by viewModel.isHomeCatalogRefreshing.collectAsStateWithLifecycle()
    val trendingList by viewModel.trendingItems.collectAsStateWithLifecycle()
    val pickedForYouItems by viewModel.pickedForYouItems.collectAsStateWithLifecycle()
    val isLoadingPickedForYou by viewModel.isLoadingPickedForYou.collectAsStateWithLifecycle()
    val onboardingSelectedGenres by viewModel.onboardingSelectedGenres.collectAsStateWithLifecycle()
    val trackedMediaList by viewModel.trackedMedia.collectAsStateWithLifecycle()
    val favoritePeopleList by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()

    val adaptive = LocalAdaptiveLayoutInfo.current
    val gridColumns = adaptive.posterGridColumns

    val regionOption = remember(effectiveRegion) {
        RegionalDefaults.supportedRegions.find { it.code.equals(effectiveRegion, ignoreCase = true) }
            ?: RegionalDefaults.detectDeviceRegion()
    }

    val topSpotlightItems = remember(trendingList, discoverItems) {
        if (trendingList.isNotEmpty()) trendingList.take(10) else discoverItems.take(10)
    }
    val themeColors = WatchPilotColors.current
    val trackedById = remember(trackedMediaList) { trackedMediaList.associateBy { it.id } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(
                start = if (adaptive.isTabletOrWide) 8.dp else 0.dp,
                end = if (adaptive.isTabletOrWide) 8.dp else 0.dp,
                bottom = 24.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_grid")
        ) {
            // WatchPilot Brand Header & Signature Quick Actions
            item(span = { GridItemSpan(gridColumns) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = "WatchPilot",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Track • Decide • Stream",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (monetizationState.isPro) themeColors.goldSolidBg else themeColors.goldChipContainer,
                            border = BorderStroke(1.dp, themeColors.goldChipBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.openPaywall() }
                                .testTag("home_pro_badge_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkspacePremium,
                                    contentDescription = "WatchPilot Pro",
                                    tint = if (monetizationState.isPro) themeColors.onGoldSolid else themeColors.goldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (monetizationState.isPro) "PRO ACTIVE" else "GO PRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (monetizationState.isPro) themeColors.onGoldSolid else themeColors.goldAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Signature WatchPilot Action Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (FeatureFlags.ENABLE_AI_FEATURES) {
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.AutoAwesome,
                                title = "Tonight's Pick",
                                subtitle = "AI Concierge",
                                accent = themeColors.goldAccent,
                                borderColor = themeColors.goldChipBorder,
                                onClick = { viewModel.openAiConcierge() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_ai_concierge"
                            )
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.Favorite,
                                title = "Swipe Match",
                                subtitle = "Live Room",
                                accent = themeColors.greenAccent,
                                borderColor = themeColors.greenChipBorder,
                                onClick = { viewModel.openSwipeMatch() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_swipe_match"
                            )
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.PhotoCamera,
                                title = "Snap Scan",
                                subtitle = "Screenshot AI",
                                accent = themeColors.cyanAccent,
                                borderColor = themeColors.cyanChipBorder,
                                onClick = { viewModel.openSnapScanner() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_snap_scan"
                            )
                        } else {
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.Favorite,
                                title = "Swipe Match",
                                subtitle = "Live Room",
                                accent = themeColors.greenAccent,
                                borderColor = themeColors.greenChipBorder,
                                onClick = { viewModel.openSwipeMatch() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_swipe_match"
                            )
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.Savings,
                                title = "OTT Saver",
                                subtitle = "Audit Spend",
                                accent = themeColors.goldAccent,
                                borderColor = themeColors.goldChipBorder,
                                onClick = { viewModel.openOttMoneySaver() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_ott_saver"
                            )
                        }
                    }
                }
            }

            // Personalized "Top Picks For You" Carousel (populated from onboarding & library taste)
            if (pickedForYouItems.isNotEmpty() || isLoadingPickedForYou) {
                item(span = { GridItemSpan(gridColumns) }) {
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
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = themeColors.cyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Top Picks For You",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                onClick = { viewModel.reopenTastePersonalization() },
                                shape = RoundedCornerShape(12.dp),
                                color = themeColors.cyanChipContainer,
                                border = BorderStroke(1.dp, themeColors.cyanChipBorder),
                                modifier = Modifier.testTag("home_tune_top_picks_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Tune Top Picks",
                                        tint = themeColors.cyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "TUNE",
                                        color = themeColors.cyanAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (pickedForYouItems.isEmpty() && isLoadingPickedForYou) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_picked_for_you_loading")
                            ) {
                                items(4) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .width(136.dp)
                                            .height(266.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                color = themeColors.cyanAccent,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_picked_for_you_carousel")
                            ) {
                                items(
                                    items = pickedForYouItems.distinctBy { it.id },
                                    key = { item -> "picked_for_you_${item.id}" }
                                ) { item ->
                                    val reasonLabel = remember(
                                        item.id,
                                        trackedMediaList,
                                        favoritePeopleList,
                                        onboardingSelectedGenres
                                    ) {
                                        PersonalizationEngine.buildRecommendationReason(
                                            candidate = item,
                                            trackedMedia = trackedMediaList,
                                            favoritePeople = favoritePeopleList,
                                            preferredGenres = onboardingSelectedGenres
                                        )
                                    }
                                    val resolvedMedia = trackedById[item.id] ?: item
                                    PickedForYouCard(
                                        media = resolvedMedia,
                                        reasonLabel = reasonLabel,
                                        onClick = { viewModel.openMediaFromSearch(resolvedMedia) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Horizontal Scrolling Spotlight Carousel (Trending & Featured)
            if (topSpotlightItems.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
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
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = themeColors.goldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${regionOption.flag} Trending in ${regionOption.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = themeColors.goldChipContainer,
                                border = BorderStroke(1.dp, themeColors.goldChipBorder)
                            ) {
                                Text(
                                    text = "TOP PICKS",
                                    color = themeColors.goldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
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
                                val resolvedMedia = trackedById[item.id] ?: item
                                SpotlightBannerCard(
                                    rank = index + 1,
                                    media = resolvedMedia,
                                    onClick = { onMediaClick(resolvedMedia.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Category & Genre Filter Section (Consolidated & Streamlined)
            item(span = { GridItemSpan(gridColumns) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val chipColors = WatchPilotColors.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.selectedTypeFilter.value = null },
                            label = { Text("All Media", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipColors.neutralChipContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = chipColors.goldSolidBg,
                                selectedLabelColor = chipColors.onGoldSolid
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedType == null,
                                borderColor = chipColors.neutralChipBorder,
                                selectedBorderColor = chipColors.goldChipBorder
                            )
                        )
                        FilterChip(
                            selected = selectedType == MediaType.MOVIE,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.MOVIE },
                            label = { Text("Movies", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipColors.neutralChipContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = chipColors.goldSolidBg,
                                selectedLabelColor = chipColors.onGoldSolid,
                                selectedLeadingIconColor = chipColors.onGoldSolid
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedType == MediaType.MOVIE,
                                borderColor = chipColors.neutralChipBorder,
                                selectedBorderColor = chipColors.goldChipBorder
                            )
                        )
                        FilterChip(
                            selected = selectedType == MediaType.TV_SHOW,
                            onClick = { viewModel.selectedTypeFilter.value = MediaType.TV_SHOW },
                            label = { Text("TV Shows", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipColors.neutralChipContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = chipColors.cyanSolidBg,
                                selectedLabelColor = chipColors.onCyanSolid,
                                selectedLeadingIconColor = chipColors.onCyanSolid
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedType == MediaType.TV_SHOW,
                                borderColor = chipColors.neutralChipBorder,
                                selectedBorderColor = chipColors.cyanChipBorder
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
                            label = { Text("All Genres", fontSize = 12.sp, fontWeight = if (selectedGenre == null) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipColors.neutralChipContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = chipColors.goldSolidBg,
                                selectedLabelColor = chipColors.onGoldSolid
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedGenre == null,
                                borderColor = chipColors.neutralChipBorder,
                                selectedBorderColor = chipColors.goldChipBorder
                            )
                        )

                        availableGenres.forEach { genre ->
                            val isGenreSelected = selectedGenre == genre
                            FilterChip(
                                selected = isGenreSelected,
                                onClick = {
                                    viewModel.selectedGenreFilter.value =
                                        if (isGenreSelected) null else genre
                                },
                                label = {
                                    Text(
                                        genre,
                                        fontSize = 12.sp,
                                        fontWeight = if (isGenreSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = chipColors.neutralChipContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = chipColors.goldChipContainer,
                                    selectedLabelColor = chipColors.goldAccent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isGenreSelected,
                                    borderColor = chipColors.neutralChipBorder,
                                    selectedBorderColor = chipColors.goldChipBorder
                                )
                            )
                        }
                    }
                }
            }

            // Continue Watching Section (if active)
            if (continueWatchingList.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "${continueWatchingList.size} active",
                                style = MaterialTheme.typography.labelMedium,
                                color = themeColors.cyanAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (adaptive.isTabletOrWide) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                continueWatchingList.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        pair.forEach { item ->
                                            ContinueWatchingCard(
                                                media = item,
                                                onClick = { onMediaClick(item.id) },
                                                onIncrementEpisode = { viewModel.incrementEpisode(item.id) },
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
            }

            // Calm Inline Sponsored Banner (Collapses to 0dp when WatchPilot Pro is active)
            if (!monetizationState.isPro) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SponsoredBannerCard(
                            isPro = monetizationState.isPro,
                            onRemoveAdsClick = {
                                viewModel.openPaywall("Go 100% Ad-Free across WatchPilot with WatchPilot Pro.")
                            },
                            modifier = Modifier
                                .widthIn(max = 728.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            tagSuffix = "home"
                        )
                    }
                }
            }

            // 3 Compact Horizontal Catalog Sections: Popular, Now Playing, Upcoming
            val catalogSections = listOf(
                Triple(
                    HomeCatalogSection.POPULAR,
                    popularHomeItems,
                    Pair(Icons.Filled.LocalFireDepartment, themeColors.goldAccent)
                ),
                Triple(
                    HomeCatalogSection.NOW_PLAYING,
                    nowPlayingHomeItems,
                    Pair(Icons.Filled.Theaters, themeColors.cyanAccent)
                ),
                Triple(
                    HomeCatalogSection.UPCOMING,
                    upcomingHomeItems,
                    Pair(Icons.Filled.Upcoming, themeColors.greenAccent)
                )
            )

            catalogSections.forEach { (section, sectionItems, badgeVisuals) ->
                val (sectionIcon, accentColor) = badgeVisuals
                item(
                    key = "home_section_${section.name}",
                    span = { GridItemSpan(gridColumns) }
                ) {
                    HomeHorizontalCatalogSection(
                        section = section,
                        regionName = regionOption.name,
                        selectedType = selectedType,
                        icon = sectionIcon,
                        accentColor = accentColor,
                        items = sectionItems.take(20),
                        trackedById = trackedById,
                        totalCount = sectionItems.size,
                        isRefreshing = isHomeCatalogRefreshing,
                        isCompactCardTablet = adaptive.isTabletOrWide,
                        onOpenSection = { onOpenSection(section) },
                        onMediaClick = onMediaClick
                    )
                }
            }

            // 4th Compact Horizontal Section: Popular People (Rounded cards + right arrow + infinite grid)
            item(
                key = "home_section_${HomeCatalogSection.POPULAR_PEOPLE.name}",
                span = { GridItemSpan(gridColumns) }
            ) {
                HomeHorizontalPeopleSection(
                    regionName = regionOption.name,
                    selectedType = selectedType,
                    people = popularPeopleHomeItems.take(20),
                    totalCount = popularPeopleHomeItems.size,
                    isRefreshing = isHomeCatalogRefreshing,
                    isCompactCardTablet = adaptive.isTabletOrWide,
                    onOpenSection = { onOpenSection(HomeCatalogSection.POPULAR_PEOPLE) },
                    onPersonClick = onPersonClick
                )
            }
        }
    }
}

@Composable
private fun HomeHorizontalPeopleSection(
    regionName: String,
    selectedType: MediaType?,
    people: List<TmdbSearchResultItem.Person>,
    totalCount: Int,
    isRefreshing: Boolean,
    isCompactCardTablet: Boolean,
    onOpenSection: () -> Unit,
    onPersonClick: (TmdbSearchResultItem.Person) -> Unit
) {
    val cardWidth = if (isCompactCardTablet) 124.dp else 104.dp
    val accentColor = WatchPilotColors.current.goldAccent
    val sectionTitle = HomeCatalogSection.POPULAR_PEOPLE.formattedTitle(regionName, selectedType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSection)
                .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onOpenSection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_section_arrow_popular_people")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View all $sectionTitle",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (totalCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "$totalCount+",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (people.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "No matching people",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenSection) {
                                Text("Explore", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_section_row_popular_people")
            ) {
                items(
                    items = people.distinctBy { it.id },
                    key = { person -> "popular_person_${person.id}" }
                ) { person ->
                    CompactPersonCircleCard(
                        person = person,
                        onClick = { onPersonClick(person) },
                        modifier = Modifier.width(cardWidth),
                        testTagPrefix = "home_popular_people_card"
                    )
                }

                if (people.size >= 4) {
                    item(key = "popular_people_see_all") {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            ),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(92.dp)
                                .height(if (isCompactCardTablet) 168.dp else 148.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onOpenSection)
                                .testTag("home_section_see_all_popular_people")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.16f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "See all $sectionTitle",
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "View All",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    maxLines = 1
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
private fun HomeHorizontalCatalogSection(
    section: HomeCatalogSection,
    regionName: String,
    selectedType: MediaType?,
    icon: ImageVector,
    accentColor: Color,
    items: List<MediaEntity>,
    trackedById: Map<String, MediaEntity>,
    totalCount: Int,
    isRefreshing: Boolean,
    isCompactCardTablet: Boolean,
    onOpenSection: () -> Unit,
    onMediaClick: (String) -> Unit
) {
    val cardWidth = if (isCompactCardTablet) 128.dp else 108.dp
    val sectionTitle = section.formattedTitle(regionName, selectedType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSection)
                .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onOpenSection,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_section_arrow_${section.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View all $sectionTitle",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (totalCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "$totalCount+",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "No matching titles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenSection) {
                                Text("Explore", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_section_row_${section.name.lowercase()}")
            ) {
                items(
                    items = items.distinctBy { it.id },
                    key = { media -> "${section.name}_${media.id}" }
                ) { media ->
                    val resolvedMedia = trackedById[media.id] ?: media
                    CompactMediaPosterCard(
                        media = resolvedMedia,
                        onClick = { onMediaClick(resolvedMedia.id) },
                        modifier = Modifier.width(cardWidth),
                        testTagPrefix = "home_${section.name.lowercase()}_card"
                    )
                }

                if (items.size >= 4) {
                    item(key = "${section.name}_see_all") {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            ),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(92.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onOpenSection)
                                .testTag("home_section_see_all_${section.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.16f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "See all ${section.displayPrefix}",
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "View All",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    maxLines = 1
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
fun SpotlightBannerCard(
    rank: Int,
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .width(if (adaptive.isTabletOrWide) 340.dp else 290.dp)
            .height(if (adaptive.isTabletOrWide) 204.dp else 175.dp)
            .clickable(onClick = onClick)
            .testTag("spotlight_card_${media.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.banner_cinema),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val bgUrl = media.backdropUrl.ifBlank { media.posterUrl }
            if (bgUrl.isNotBlank()) {
                val context = LocalContext.current
                val imageRequest = remember(bgUrl, context) {
                    ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
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
                            text = if (media.userRating > 0f) media.getFormattedUserRating() else String.format("%.1f", media.communityRating),
                            color = CinemaGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Info + At-a-Glance Watch Status Pill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (media.getWatchStatusEnum() != WatchStatus.NONE) {
                    WatchStatusPill(status = media.getWatchStatusEnum())
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${media.releaseYear} • ${media.genres.ifBlank { "Drama" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeQuickFeatureChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WatchPilotColors.current.neutralChipContainer),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                maxLines = 1,
                color = accent
            )
        }
    }
}

@Composable
private fun PickedForYouCard(
    media: MediaEntity,
    reasonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val context = LocalContext.current
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    val isStarReason = reasonLabel.startsWith("★")
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .width(if (adaptive.isTabletOrWide) 156.dp else 136.dp)
            .clickable(onClick = onClick)
            .testTag("picked_card_${media.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(30.dp)
                    )
                }
                val poster = media.posterUrl.ifBlank { media.backdropUrl }
                if (poster.isNotBlank()) {
                    val req = remember(poster, context) {
                        ImageRequest.Builder(context)
                            .data(poster)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.banner_cinema),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isMovie) "Film" else "TV",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovie) CinemaGold else CinemaCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${media.releaseYear} • ${media.genres.substringBefore(",").trim()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val cardColors = WatchPilotColors.current
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isStarReason) cardColors.goldChipContainer else cardColors.cyanChipContainer,
                    border = BorderStroke(1.dp, if (isStarReason) cardColors.goldChipBorder else cardColors.cyanChipBorder)
                ) {
                    Text(
                        text = reasonLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStarReason) cardColors.goldAccent else cardColors.cyanAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

