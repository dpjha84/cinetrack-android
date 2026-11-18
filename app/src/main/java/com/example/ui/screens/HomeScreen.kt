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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
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
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.WatchStatusPill
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.PersonalizationEngine
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

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
    val pickedForYouItems by viewModel.pickedForYouItems.collectAsStateWithLifecycle()
    val isLoadingPickedForYou by viewModel.isLoadingPickedForYou.collectAsStateWithLifecycle()
    val onboardingSelectedGenres by viewModel.onboardingSelectedGenres.collectAsStateWithLifecycle()
    val trackedMediaList by viewModel.trackedMedia.collectAsStateWithLifecycle()
    val favoritePeopleList by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()

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
            // WatchPilot v2.0 Brand Header & Signature Quick Actions
            item(span = { GridItemSpan(2) }) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WatchPilot",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (!BuildConfig.IS_V1_EDITION) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CinemaCyan.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = BuildConfig.EDITION_NAME,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CinemaCyan,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (!BuildConfig.IS_V1_EDITION) {
                                Text(
                                    text = "Track • Decide Together • Stream Smarter",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!BuildConfig.IS_V1_EDITION) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (monetizationState.isPro) CinemaGold else CinemaGold.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.6f)),
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
                                        tint = if (monetizationState.isPro) DarkBackground else CinemaGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (monetizationState.isPro) "PRO ACTIVE" else "GO PRO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (monetizationState.isPro) DarkBackground else CinemaGold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (!BuildConfig.IS_V1_EDITION) {
                        // 3 Signature WatchPilot v2.0 Action Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.AutoAwesome,
                                title = "Tonight's Pick",
                                subtitle = "AI Concierge",
                                accent = CinemaGold,
                                onClick = { viewModel.openAiConcierge() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_ai_concierge"
                            )
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.Favorite,
                                title = "Swipe Match",
                                subtitle = "Live Room",
                                accent = CinemaGreen,
                                onClick = { viewModel.openSwipeMatch() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_swipe_match"
                            )
                            HomeQuickFeatureChip(
                                icon = Icons.Filled.PhotoCamera,
                                title = "Snap Scan",
                                subtitle = "Screenshot AI",
                                accent = CinemaCyan,
                                onClick = { viewModel.openSnapScanner() },
                                modifier = Modifier.weight(1f),
                                testTag = "home_btn_snap_scan"
                            )
                        }
                    }
                }
            }

            // Personalized "Top Picks For You" Carousel (populated from onboarding & library taste)
            if (pickedForYouItems.isNotEmpty() || isLoadingPickedForYou) {
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
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = CinemaCyan,
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
                                color = CinemaCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, CinemaCyan.copy(alpha = 0.35f)),
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
                                        tint = CinemaCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "TUNE",
                                        color = CinemaCyan,
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
                                            .height(236.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                color = CinemaCyan,
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
                                    items = pickedForYouItems,
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
                                    PickedForYouCard(
                                        media = item,
                                        reasonLabel = reasonLabel,
                                        onClick = { viewModel.openMediaFromSearch(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "${continueWatchingList.size} Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = CinemaCyan,
                                maxLines = 1,
                                softWrap = false
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

            // Calm Inline Sponsored Banner (Collapses to 0dp when WatchPilot Pro is active)
            if (!monetizationState.isPro) {
                item(span = { GridItemSpan(2) }) {
                    SponsoredBannerCard(
                        isPro = monetizationState.isPro,
                        onRemoveAdsClick = {
                            viewModel.openPaywall("Go 100% Ad-Free across WatchPilot with WatchPilot Pro.")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        tagSuffix = "home"
                    )
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "${discoverItems.size} titles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
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
            if (bgUrl.isNotBlank()) {
                val context = LocalContext.current
                val imageRequest = remember(bgUrl, context) {
                    ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.banner_cinema)
                        .error(R.drawable.banner_cinema)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_cinema),
                    contentDescription = null,
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

@Composable
private fun HomeQuickFeatureChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
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
    val context = LocalContext.current
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    val isStarReason = reasonLabel.startsWith("★")
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .width(136.dp)
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
                val poster = media.posterUrl.ifBlank { media.backdropUrl }
                if (poster.isNotBlank()) {
                    val req = remember(poster, context) {
                        ImageRequest.Builder(context)
                            .data(poster)
                            .crossfade(true)
                            .placeholder(R.drawable.banner_cinema)
                            .error(R.drawable.banner_cinema)
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isStarReason) CinemaGold.copy(alpha = 0.16f) else CinemaCyan.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = reasonLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStarReason) CinemaGold else CinemaCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

