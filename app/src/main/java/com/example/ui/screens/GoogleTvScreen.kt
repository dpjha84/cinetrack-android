package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchAccessShortcut
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.RegionalDefaults
import com.example.data.model.WatchStatus
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.components.RatingReviewDialog
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.viewmodel.MediaViewModel

enum class GoogleTvTab(val title: String) {
    FOR_YOU("For You"),
    MOVIES("Movies"),
    SHOWS("Shows"),
    WATCHLIST("Watchlist"),
    SEARCH("Search")
}

@Composable
fun GoogleTvScreen(
    viewModel: MediaViewModel,
    onMediaClick: (String) -> Unit,
    onSwitchToClassic: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GoogleTvTab.FOR_YOU) }
    var showLocalizationDialog by remember { mutableStateOf(false) }
    var itemToRate by remember { mutableStateOf<MediaEntity?>(null) }

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val watchlistItems by viewModel.watchlistMedia.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val trendingList by viewModel.trendingItems.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()

    // Billboard / Featured Item
    val featuredItem = remember(trendingList, allMedia) {
        trendingList.firstOrNull { it.backdropUrl.isNotBlank() }
            ?: allMedia.firstOrNull { it.backdropUrl.isNotBlank() }
            ?: allMedia.firstOrNull()
    }

    // Google TV Deep Dark Theme Background
    val googleTvDarkBg = Color(0xFF090A0E)
    val googleTvSurface = Color(0xFF141721)
    val googleTvBlue = Color(0xFF4C8DF6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(googleTvDarkBg)
            .testTag("google_tv_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Google TV Header Bar
            Surface(
                color = googleTvDarkBg.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Top Branding & Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google TV Logo Mark
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = googleTvBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = "Google TV",
                                        tint = googleTvBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Google TV",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = googleTvBlue
                                    ) {
                                        Text(
                                            text = "v2.0",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Movies, Shows & Watchlist",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Right controls: Switch to Classic UI & Settings
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Version / Classic Mode Switch
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = googleTvSurface,
                                modifier = Modifier
                                    .clickable { onSwitchToClassic() }
                                    .testTag("btn_switch_to_classic")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwitchAccessShortcut,
                                        contentDescription = "Switch to Classic CineTrack",
                                        tint = CinemaGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Classic UI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinemaGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showLocalizationDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(googleTvSurface, CircleShape)
                                    .testTag("btn_google_tv_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Region Settings",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google TV Pill Tabs (For You, Movies, Shows, Watchlist, Search)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoogleTvTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color.White else googleTvSurface,
                                modifier = Modifier
                                    .clickable {
                                        if (tab == GoogleTvTab.SEARCH) {
                                            onNavigateToSearch()
                                        } else {
                                            selectedTab = tab
                                        }
                                    }
                                    .testTag("googletv_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    if (tab == GoogleTvTab.WATCHLIST && watchlistItems.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) googleTvBlue else CinemaGold,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else if (tab == GoogleTvTab.SEARCH) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Text(
                                        text = if (tab == GoogleTvTab.WATCHLIST) "Watchlist (${watchlistItems.size})" else tab.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Google TV Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    GoogleTvTab.FOR_YOU -> {
                        GoogleTvForYouView(
                            featuredItem = featuredItem,
                            watchlistItems = watchlistItems,
                            trendingList = trendingList,
                            continueWatching = continueWatching,
                            allMedia = allMedia,
                            onMediaClick = onMediaClick,
                            onToggleWatchlist = { id -> viewModel.toggleWatchlistQuick(id) },
                            onRateMedia = { item -> itemToRate = item },
                            onOpenWatchlistTab = { selectedTab = GoogleTvTab.WATCHLIST }
                        )
                    }

                    GoogleTvTab.MOVIES -> {
                        GoogleTvCategoryView(
                            title = "Movies on Google TV",
                            mediaType = MediaType.MOVIE,
                            items = allMedia.filter { it.getMediaTypeEnum() == MediaType.MOVIE },
                            watchlistItems = watchlistItems,
                            onMediaClick = onMediaClick,
                            onToggleWatchlist = { id -> viewModel.toggleWatchlistQuick(id) },
                            onRateMedia = { item -> itemToRate = item }
                        )
                    }

                    GoogleTvTab.SHOWS -> {
                        GoogleTvCategoryView(
                            title = "TV Shows & Series on Google TV",
                            mediaType = MediaType.TV_SHOW,
                            items = allMedia.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW },
                            watchlistItems = watchlistItems,
                            onMediaClick = onMediaClick,
                            onToggleWatchlist = { id -> viewModel.toggleWatchlistQuick(id) },
                            onRateMedia = { item -> itemToRate = item }
                        )
                    }

                    GoogleTvTab.WATCHLIST -> {
                        GoogleTvWatchlistView(
                            watchlistItems = watchlistItems,
                            onMediaClick = onMediaClick,
                            onRemoveFromWatchlist = { id -> viewModel.toggleWatchlistQuick(id) },
                            onRateMedia = { item -> itemToRate = item },
                            onExploreClick = { selectedTab = GoogleTvTab.FOR_YOU }
                        )
                    }

                    GoogleTvTab.SEARCH -> {
                        // Handled by onNavigateToSearch
                    }
                }
            }
        }
    }

    // Rate & Review Dialog (1–5 Stars)
    itemToRate?.let { media ->
        RatingReviewDialog(
            mediaTitle = media.title,
            currentRating = media.getUserRatingInStars(),
            currentReview = media.userReview,
            onDismiss = { itemToRate = null },
            onSave = { rating, review ->
                viewModel.updateRatingAndReview(media.id, rating, review)
                itemToRate = null
            }
        )
    }

    // Localization Settings Dialog
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

/**
 * Google TV "For You" Feed:
 * 1. Cinematic Hero Billboard
 * 2. "Your Watchlist" Rail (Room DB)
 * 3. "Trending Movies & Shows" Rail (TMDB /trending/all/day)
 * 4. "Continue Watching" Rail (with episode progress)
 * 5. "Top Picks for You" Rail
 */
@Composable
fun GoogleTvForYouView(
    featuredItem: MediaEntity?,
    watchlistItems: List<MediaEntity>,
    trendingList: List<MediaEntity>,
    continueWatching: List<MediaEntity>,
    allMedia: List<MediaEntity>,
    onMediaClick: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onRateMedia: (MediaEntity) -> Unit,
    onOpenWatchlistTab: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("googletv_for_you_feed")
    ) {
        // 1. Cinematic Hero Billboard
        if (featuredItem != null) {
            item {
                GoogleTvBillboard(
                    media = featuredItem,
                    inWatchlist = watchlistItems.any { it.id == featuredItem.id },
                    onDetailsClick = { onMediaClick(featuredItem.id) },
                    onToggleWatchlist = { onToggleWatchlist(featuredItem.id) },
                    onRateClick = { onRateMedia(featuredItem) }
                )
            }
        }

        // 2. "Your Watchlist" Rail (Stored locally in Room DB)
        item {
            GoogleTvSectionHeader(
                title = "Your Watchlist",
                badgeText = "${watchlistItems.size} Saved",
                onSeeAllClick = onOpenWatchlistTab
            )

            if (watchlistItems.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.testTag("rail_watchlist")
                ) {
                    items(watchlistItems, key = { "wl_${it.id}" }) { item ->
                        GoogleTvMediaCard(
                            media = item,
                            inWatchlist = true,
                            onClick = { onMediaClick(item.id) },
                            onToggleWatchlist = { onToggleWatchlist(item.id) },
                            onRateClick = { onRateMedia(item) }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141721),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenWatchlistTab() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Your Watchlist is ready",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap '+ Watchlist' on any title to save it locally in Room DB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Continue Watching Rail (if active)
        if (continueWatching.isNotEmpty()) {
            item {
                GoogleTvSectionHeader(
                    title = "Continue Watching",
                    badgeText = "${continueWatching.size} in progress"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.testTag("rail_continue_watching")
                ) {
                    items(continueWatching, key = { "cw_${it.id}" }) { item ->
                        GoogleTvContinueWatchingCard(
                            media = item,
                            onClick = { onMediaClick(item.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 4. "Trending Now on Google TV" (TMDB /trending/all/day)
        item {
            GoogleTvSectionHeader(
                title = "🔥 Trending Now on Google TV",
                badgeText = "TMDB /trending/all/day"
            )

            val displayTrending = if (trendingList.isNotEmpty()) trendingList else allMedia.take(10)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.testTag("rail_trending")
            ) {
                itemsIndexed(displayTrending, key = { index, item -> "trend_${item.id}_$index" }) { index, item ->
                    GoogleTvMediaCard(
                        media = item,
                        rank = index + 1,
                        inWatchlist = watchlistItems.any { it.id == item.id },
                        onClick = { onMediaClick(item.id) },
                        onToggleWatchlist = { onToggleWatchlist(item.id) },
                        onRateClick = { onRateMedia(item) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 5. "Top Picks for You"
        item {
            GoogleTvSectionHeader(
                title = "⭐ Top Picks for You",
                badgeText = "Critically Acclaimed"
            )

            val topPicks = allMedia.sortedByDescending { it.communityRating }.take(10)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.testTag("rail_top_picks")
            ) {
                items(topPicks, key = { "top_${it.id}" }) { item ->
                    GoogleTvMediaCard(
                        media = item,
                        inWatchlist = watchlistItems.any { it.id == item.id },
                        onClick = { onMediaClick(item.id) },
                        onToggleWatchlist = { onToggleWatchlist(item.id) },
                        onRateClick = { onRateMedia(item) }
                    )
                }
            }
        }
    }
}

/**
 * Google TV Cinematic Billboard
 */
@Composable
fun GoogleTvBillboard(
    media: MediaEntity,
    inWatchlist: Boolean,
    onDetailsClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onRateClick: () -> Unit
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable { onDetailsClick() }
            .testTag("google_tv_billboard")
    ) {
        // High-res Backdrop
        AsyncImage(
            model = media.backdropUrl.ifBlank { media.posterUrl },
            contentDescription = media.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Google TV Ambient Scrim Gradient (top and bottom dark fades)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88090A0E),
                            Color.Transparent,
                            Color(0xCC090A0E),
                            Color(0xFF090A0E)
                        )
                    )
                )
        )

        // Billboard Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Badges row: 4K, HDR, Year, Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isMovie) "MOVIE" else "SERIES",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4C8DF6).copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "4K UHD • HDR",
                        color = Color(0xFF8AB4F8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CinemaGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", media.communityRating),
                        color = CinemaGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (media.userRating > 0f) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CinemaGold
                    ) {
                        Text(
                            text = "You: ${media.getFormattedUserRating()}",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Overview
            Text(
                text = media.overview.ifBlank { "Available to stream now with full cast details and personal watchlist tracking." },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Google TV Style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button (White filled)
                Button(
                    onClick = onDetailsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_billboard_details")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Watchlist Toggle Button (Outlined pill)
                Button(
                    onClick = onToggleWatchlist,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inWatchlist) CinemaGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                        contentColor = if (inWatchlist) CinemaGreen else Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_billboard_watchlist")
                ) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (inWatchlist) "In Watchlist" else "+ Watchlist",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // 1-5 Star Rating Button
                Button(
                    onClick = onRateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = CinemaGold
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_billboard_rate")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (media.userRating > 0f) "${media.getUserRatingInStars()}★" else "Rate (1-5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Google TV Media Poster Card (for rails)
 */
@Composable
fun GoogleTvMediaCard(
    media: MediaEntity,
    inWatchlist: Boolean,
    rank: Int? = null,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE

    Column(
        modifier = modifier
            .width(135.dp)
            .clickable { onClick() }
            .testTag("gtv_card_${media.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141721))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = media.posterUrl,
                contentDescription = media.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            // Rank Badge or Media Type
            if (rank != null) {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = if (rank <= 3) CinemaGold else Color(0xFF141721).copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "#$rank",
                        color = if (rank <= 3) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = (if (isMovie) Color(0xFFE5A93C) else Color(0xFF4C8DF6)).copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopStart)
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

            // Quick Watchlist icon in top-right
            IconButton(
                onClick = onToggleWatchlist,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Watchlist",
                    tint = if (inWatchlist) CinemaGold else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // User rating or Community rating in bottom
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = CinemaGold,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (media.userRating > 0f) "${media.getUserRatingInStars()}★" else String.format("%.1f", media.communityRating),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = media.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${media.releaseYear}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            if (media.userRating > 0f) {
                Text(
                    text = "Rated",
                    fontSize = 10.sp,
                    color = CinemaGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Google TV Continue Watching Card
 */
@Composable
fun GoogleTvContinueWatchingCard(
    media: MediaEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("gtv_continue_${media.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = media.backdropUrl.ifBlank { media.posterUrl },
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Episode progress bar at bottom of image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(media.progressFraction)
                            .height(4.dp)
                            .background(CinemaGold)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Episode ${media.watchedEpisodes} of ${media.totalEpisodes}",
                    fontSize = 11.sp,
                    color = Color(0xFF8AB4F8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Dedicated Google TV Watchlist Screen
 */
@Composable
fun GoogleTvWatchlistView(
    watchlistItems: List<MediaEntity>,
    onMediaClick: (String) -> Unit,
    onRemoveFromWatchlist: (String) -> Unit,
    onRateMedia: (MediaEntity) -> Unit,
    onExploreClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<MediaType?>(null) } // null = all

    val filteredList = remember(watchlistItems, selectedFilter) {
        if (selectedFilter == null) watchlistItems
        else watchlistItems.filter { it.getMediaTypeEnum() == selectedFilter }
    }

    val movieCount = remember(watchlistItems) { watchlistItems.count { it.getMediaTypeEnum() == MediaType.MOVIE } }
    val tvCount = remember(watchlistItems) { watchlistItems.count { it.getMediaTypeEnum() == MediaType.TV_SHOW } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("googletv_watchlist_view")
    ) {
        // Section Subheader & Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Watchlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Stored locally in Room database • ${watchlistItems.size} Titles",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${watchlistItems.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = selectedFilter == MediaType.MOVIE,
                onClick = { selectedFilter = MediaType.MOVIE },
                label = { Text("Movies ($movieCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CinemaGold,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = selectedFilter == MediaType.TV_SHOW,
                onClick = { selectedFilter = MediaType.TV_SHOW },
                label = { Text("TV Shows ($tvCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4C8DF6),
                    selectedLabelColor = Color.Black
                )
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No titles in Watchlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse titles on Google TV and tap '+ Watchlist' to save them here locally in your Room database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Explore For You Feed", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    GoogleTvMediaCard(
                        media = item,
                        inWatchlist = true,
                        onClick = { onMediaClick(item.id) },
                        onToggleWatchlist = { onRemoveFromWatchlist(item.id) },
                        onRateClick = { onRateMedia(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Google TV Category view (Movies / Shows)
 */
@Composable
fun GoogleTvCategoryView(
    title: String,
    mediaType: MediaType,
    items: List<MediaEntity>,
    watchlistItems: List<MediaEntity>,
    onMediaClick: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onRateMedia: (MediaEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${items.size} Titles Available • Localized for your region",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        items(items, key = { it.id }) { item ->
            GoogleTvMediaCard(
                media = item,
                inWatchlist = watchlistItems.any { it.id == item.id },
                onClick = { onMediaClick(item.id) },
                onToggleWatchlist = { onToggleWatchlist(item.id) },
                onRateClick = { onRateMedia(item) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GoogleTvSectionHeader(
    title: String,
    badgeText: String? = null,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (onSeeAllClick != null) {
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("See All", color = CinemaGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = CinemaGold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
