package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import com.example.R
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchProviderItem
import com.example.data.model.WatchProviderType
import com.example.data.model.WatchProvidersInfo
import com.example.data.model.WatchStatus
import com.example.ui.components.RatingReviewDialog
import com.example.ui.components.WatchStatusPill
import com.example.ui.components.WatchlistSelectDialog
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.WatchPilotColors
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.CastMemberItem
import com.example.data.model.MonetizationState
import com.example.data.model.RegionalDefaults
import com.example.data.model.SeasonItem
import com.example.data.model.TitleAiBriefUiState
import com.example.data.model.TvWatchStatus
import com.example.ui.components.RewardedTrialPassBanner
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.WatchProviderLauncher

@Composable
fun MediaDetailScreen(
    media: MediaEntity,
    viewModel: MediaViewModel,
    availableCollections: List<String>,
    effectiveRegionCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activeWatchProviders by viewModel.activeWatchProviders.collectAsStateWithLifecycle()
    val isLoadingWatchProviders by viewModel.isLoadingWatchProviders.collectAsStateWithLifecycle()
    val currentMediaCast by viewModel.currentMediaCast.collectAsStateWithLifecycle()
    val seasons by viewModel.currentTvSeasons.collectAsStateWithLifecycle()
    val isLoadingSeasons by viewModel.isLoadingSeasons.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val showStreamingProviders = userPrefs.showStreamingProviders
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()
    val titleAiBriefs by viewModel.titleAiBriefs.collectAsStateWithLifecycle()
    val aiBriefState = titleAiBriefs[media.id] ?: TitleAiBriefUiState.Idle

    var showRatingDialog by remember { mutableStateOf(false) }
    var showWatchlistDialog by remember { mutableStateOf(false) }

    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    val maxSeasons = maxOf(1, media.totalSeasons, seasons.count { it.seasonNumber > 0 })
    val totalEpisodes = maxOf(1, media.totalEpisodes)

    val nextUpTarget = remember(media.id, media.watchedEpisodes, totalEpisodes, maxSeasons, seasons) {
        val regularSeasons = seasons.filter { it.seasonNumber > 0 }.ifEmpty { seasons }.sortedBy { it.seasonNumber }
        if (regularSeasons.isNotEmpty()) {
            val seasonsWatchedSum = regularSeasons.sumOf { it.watchedEpisodesCount }
            var pendingDelta = (media.watchedEpisodes - seasonsWatchedSum).coerceAtLeast(0)
            var resolved: Pair<Int, Int>? = null
            for (seasonItem in regularSeasons) {
                if (seasonItem.episodeCount <= 0) continue
                val effectiveSeasonWatched = (seasonItem.watchedEpisodesCount + pendingDelta)
                    .coerceAtMost(seasonItem.episodeCount)
                pendingDelta = (pendingDelta - (effectiveSeasonWatched - seasonItem.watchedEpisodesCount))
                    .coerceAtLeast(0)
                if (effectiveSeasonWatched < seasonItem.episodeCount) {
                    resolved = seasonItem.seasonNumber to (effectiveSeasonWatched + 1)
                    break
                }
            }
            resolved ?: (1 to 1)
        } else {
            val epsPerSeason = maxOf(1, (totalEpisodes + maxSeasons - 1) / maxSeasons)
            if (media.watchedEpisodes < totalEpisodes) {
                val s = ((media.watchedEpisodes / epsPerSeason) + 1).coerceIn(1, maxSeasons)
                val e = ((media.watchedEpisodes % epsPerSeason) + 1).coerceAtLeast(1)
                s to e
            } else {
                1 to 1
            }
        }
    }
    val nextUpSeason = nextUpTarget.first
    val nextUpEpisode = nextUpTarget.second

    // Interactive Season and Episode selection for TV series direct playback
    var hasCustomPlaybackTarget by remember(media.id) { mutableStateOf(false) }
    var selectedSeason by remember(media.id) { mutableIntStateOf(nextUpSeason) }
    var selectedEpisode by remember(media.id) { mutableIntStateOf(nextUpEpisode) }

    LaunchedEffect(media.id, nextUpSeason, nextUpEpisode) {
        if (!hasCustomPlaybackTarget) {
            selectedSeason = nextUpSeason
            selectedEpisode = nextUpEpisode
        }
    }

    val maxEpisodesInSelectedSeason = remember(seasons, selectedSeason) {
        seasons.find { it.seasonNumber == selectedSeason }?.episodeCount?.takeIf { it > 0 }
    }

    val adaptive = LocalAdaptiveLayoutInfo.current
    val tabletMaxWidth = if (adaptive.isExpanded) 1140.dp else 760.dp

    val providersInfo = activeWatchProviders ?: remember(media, effectiveRegionCode) {
        media.getWatchProvidersInfo(effectiveRegionCode)
    }
    val streamAndFree = (providersInfo.stream + providersInfo.free).distinctBy { it.name }
    val displayCast = remember(currentMediaCast, media.cast) {
        if (currentMediaCast.isNotEmpty()) {
            currentMediaCast
        } else {
            media.getCastList().map { CastMemberItem(name = it) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("media_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Backdrop Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (adaptive.isTabletOrWide) 340.dp else 280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner_cinema),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                val backdropSource = media.backdropUrl.ifBlank { media.posterUrl }
                if (backdropSource.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropSource)
                            .crossfade(true)
                            .build(),
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                }

                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Back and Favorite Buttons in Status Bar Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (adaptive.isTabletOrWide) Modifier.widthIn(max = tabletMaxWidth).align(Alignment.TopCenter) else Modifier)
                        .statusBarsPadding()
                        .padding(horizontal = if (adaptive.isTabletOrWide) 24.dp else 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite(media.id, media.isFavorite) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("detail_fav_button")
                    ) {
                        Icon(
                            imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (media.isFavorite) CinemaRed else Color.White
                        )
                    }
                }

                // Poster & Title Row Floating over bottom of backdrop
                Row(
                    modifier = Modifier
                        .align(if (adaptive.isTabletOrWide) Alignment.BottomCenter else Alignment.BottomStart)
                        .then(if (adaptive.isTabletOrWide) Modifier.fillMaxWidth().widthIn(max = tabletMaxWidth) else Modifier)
                        .padding(horizontal = if (adaptive.isTabletOrWide) 24.dp else 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (adaptive.isTabletOrWide) 134.dp else 105.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        val posterSource = media.posterUrl.ifBlank { media.backdropUrl }
                        if (posterSource.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(posterSource)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = media.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        val heroColors = WatchPilotColors.current
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMovie) heroColors.goldSolidBg else heroColors.cyanSolidBg
                        ) {
                            Text(
                                text = if (isMovie) "MOVIE" else "TV SERIES",
                                color = if (isMovie) heroColors.onGoldSolid else heroColors.onCyanSolid,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = media.title,
                            style = if (adaptive.isTabletOrWide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${media.releaseYear} • ${if (media.getMediaTypeEnum() == MediaType.MOVIE) "${media.runtimeMinutes} min" else "${media.totalSeasons} Seasons, ${media.totalEpisodes} Eps"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (adaptive.useTwoPaneLayout) {
                // Tablet Expanded Two-Pane Details Body
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = tabletMaxWidth)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Pane: Genres, Rating & Review, Collection, Synopsis, Creator, Cast
                    Column(modifier = Modifier.weight(1.06f)) {
                        MediaGenresAndOriginStrip(media = media, isTablet = true)
                        Spacer(modifier = Modifier.height(16.dp))
                        MediaRatingAndReviewCard(
                            media = media,
                            onEditRating = { showRatingDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MediaCustomCollectionCard(
                            media = media,
                            onChangeClick = { showWatchlistDialog = true }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        MediaAiCompanionCard(
                            isMovie = isMovie,
                            monetizationState = monetizationState,
                            briefState = aiBriefState,
                            onGenerate = { viewModel.generateTitleAiBrief(media) },
                            onOpenPaywall = {
                                viewModel.openPaywall(
                                    "Unlock unlimited Spoiler-Free AI Recaps & Personal Watch-Fit briefs with WatchPilot Pro."
                                )
                            },
                            onRewardEarned = { viewModel.grantRewardedTrialPass("media_detail_recap") }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        MediaSynopsisAndCastSection(
                            media = media,
                            displayCast = displayCast,
                            onCastClick = { viewModel.openCastMember(it) }
                        )
                    }

                    // Right Pane: Where to Watch, Watch Status, Episode Progress, Seasons
                    Column(modifier = Modifier.weight(0.94f)) {
                        if (showStreamingProviders) {
                            MediaWhereToWatchCard(
                                media = media,
                                isMovie = isMovie,
                                effectiveRegionCode = effectiveRegionCode,
                                providersInfo = providersInfo,
                                streamAndFree = streamAndFree,
                                isLoadingWatchProviders = isLoadingWatchProviders,
                                maxSeasons = maxSeasons,
                                selectedSeason = selectedSeason,
                                selectedEpisode = selectedEpisode,
                                maxEpisodesInSelectedSeason = maxEpisodesInSelectedSeason,
                                onSelectSeason = { newSeason ->
                                    hasCustomPlaybackTarget = true
                                    selectedSeason = newSeason
                                    selectedEpisode = 1
                                },
                                onSelectEpisode = { newEpisode ->
                                    hasCustomPlaybackTarget = true
                                    selectedEpisode = newEpisode
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        MediaWatchStatusSelectorSection(
                            media = media,
                            isTablet = true,
                            onUpdateStatus = { st -> viewModel.updateStatus(media.id, st) }
                        )

                        if (media.getMediaTypeEnum() == MediaType.TV_SHOW && media.totalEpisodes > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            MediaEpisodeProgressCard(
                                media = media,
                                maxSeasons = maxSeasons,
                                nextUpSeason = nextUpSeason,
                                nextUpEpisode = nextUpEpisode,
                                showStreamingProviders = showStreamingProviders,
                                streamAndFree = streamAndFree,
                                providersInfo = providersInfo,
                                selectedSeason = selectedSeason,
                                selectedEpisode = selectedEpisode,
                                onSetWatchedEpisodes = { count -> viewModel.setWatchedEpisodes(media.id, count) },
                                onIncrementEpisode = { viewModel.incrementEpisode(media.id) }
                            )
                        }

                        if (media.getMediaTypeEnum() == MediaType.TV_SHOW) {
                            Spacer(modifier = Modifier.height(20.dp))
                            MediaSeasonsSection(
                                media = media,
                                seasons = seasons,
                                isLoadingSeasons = isLoadingSeasons,
                                onSetSeasonStatus = { sNum, status ->
                                    viewModel.setSeasonWatchStatus(media, sNum, status)
                                },
                                onOpenSeasonDetail = { seasonItem ->
                                    viewModel.openSeasonDetail(media, seasonItem)
                                }
                            )
                        }
                    }
                }
            } else {
                // Phone (Compact) & Tablet Medium Single-Column Details Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (adaptive.isTabletOrWide) Modifier.widthIn(max = tabletMaxWidth) else Modifier)
                        .padding(horizontal = if (adaptive.isTabletOrWide) 24.dp else 20.dp, vertical = 12.dp)
                ) {
                    MediaGenresAndOriginStrip(media = media, isTablet = adaptive.isTabletOrWide)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (showStreamingProviders) {
                        MediaWhereToWatchCard(
                            media = media,
                            isMovie = isMovie,
                            effectiveRegionCode = effectiveRegionCode,
                            providersInfo = providersInfo,
                            streamAndFree = streamAndFree,
                            isLoadingWatchProviders = isLoadingWatchProviders,
                            maxSeasons = maxSeasons,
                            selectedSeason = selectedSeason,
                            selectedEpisode = selectedEpisode,
                            maxEpisodesInSelectedSeason = maxEpisodesInSelectedSeason,
                            onSelectSeason = { newSeason ->
                                hasCustomPlaybackTarget = true
                                selectedSeason = newSeason
                                selectedEpisode = 1
                            },
                            onSelectEpisode = { newEpisode ->
                                hasCustomPlaybackTarget = true
                                selectedEpisode = newEpisode
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    MediaWatchStatusSelectorSection(
                        media = media,
                        isTablet = adaptive.isTabletOrWide,
                        onUpdateStatus = { st -> viewModel.updateStatus(media.id, st) }
                    )

                    if (media.getMediaTypeEnum() == MediaType.TV_SHOW && media.totalEpisodes > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MediaEpisodeProgressCard(
                            media = media,
                            maxSeasons = maxSeasons,
                            nextUpSeason = nextUpSeason,
                            nextUpEpisode = nextUpEpisode,
                            showStreamingProviders = showStreamingProviders,
                            streamAndFree = streamAndFree,
                            providersInfo = providersInfo,
                            selectedSeason = selectedSeason,
                            selectedEpisode = selectedEpisode,
                            onSetWatchedEpisodes = { count -> viewModel.setWatchedEpisodes(media.id, count) },
                            onIncrementEpisode = { viewModel.incrementEpisode(media.id) }
                        )
                    }

                    if (media.getMediaTypeEnum() == MediaType.TV_SHOW) {
                        Spacer(modifier = Modifier.height(20.dp))
                        MediaSeasonsSection(
                            media = media,
                            seasons = seasons,
                            isLoadingSeasons = isLoadingSeasons,
                            onSetSeasonStatus = { sNum, status ->
                                viewModel.setSeasonWatchStatus(media, sNum, status)
                            },
                            onOpenSeasonDetail = { seasonItem ->
                                viewModel.openSeasonDetail(media, seasonItem)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    MediaRatingAndReviewCard(
                        media = media,
                        onEditRating = { showRatingDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MediaCustomCollectionCard(
                        media = media,
                        onChangeClick = { showWatchlistDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MediaAiCompanionCard(
                        isMovie = isMovie,
                        monetizationState = monetizationState,
                        briefState = aiBriefState,
                        onGenerate = { viewModel.generateTitleAiBrief(media) },
                        onOpenPaywall = {
                            viewModel.openPaywall(
                                "Unlock unlimited Spoiler-Free AI Recaps & Personal Watch-Fit briefs with WatchPilot Pro."
                            )
                        },
                        onRewardEarned = { viewModel.grantRewardedTrialPass("media_detail_recap") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MediaSynopsisAndCastSection(
                        media = media,
                        displayCast = displayCast,
                        onCastClick = { viewModel.openCastMember(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showRatingDialog) {
        RatingReviewDialog(
            mediaTitle = media.title,
            currentRating = media.userRating,
            currentReview = media.userReview,
            onDismiss = { showRatingDialog = false },
            onSave = { rating, review ->
                viewModel.updateRatingAndReview(media.id, rating, review)
                showRatingDialog = false
            }
        )
    }

    if (showWatchlistDialog) {
        WatchlistSelectDialog(
            currentWatchlist = media.customWatchlist,
            availableCollections = availableCollections,
            onDismiss = { showWatchlistDialog = false },
            onSelect = { selectedCol ->
                viewModel.setCustomWatchlist(media.id, selectedCol)
                showWatchlistDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaGenresAndOriginStrip(
    media: MediaEntity,
    isTablet: Boolean
) {
    val stripColors = WatchPilotColors.current
    if (isTablet) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = stripColors.cyanChipContainer,
                border = BorderStroke(1.dp, stripColors.cyanChipBorder)
            ) {
                Text(
                    text = "${media.getOriginFlag()} ${media.originCountry} • ${media.getLanguageFlagAndName()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = stripColors.cyanAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            media.getGenreList().forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = stripColors.neutralChipContainer,
                    border = BorderStroke(1.dp, stripColors.neutralChipBorder)
                ) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Origin and Language Badges
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = stripColors.cyanChipContainer,
                border = BorderStroke(1.dp, stripColors.cyanChipBorder)
            ) {
                Text(
                    text = "${media.getOriginFlag()} ${media.originCountry} • ${media.getLanguageFlagAndName()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = stripColors.cyanAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            media.getGenreList().forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = stripColors.neutralChipContainer,
                    border = BorderStroke(1.dp, stripColors.neutralChipBorder)
                ) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaWhereToWatchCard(
    media: MediaEntity,
    isMovie: Boolean,
    effectiveRegionCode: String,
    providersInfo: WatchProvidersInfo,
    streamAndFree: List<WatchProviderItem>,
    isLoadingWatchProviders: Boolean,
    maxSeasons: Int,
    selectedSeason: Int,
    selectedEpisode: Int,
    maxEpisodesInSelectedSeason: Int?,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit
) {
    val context = LocalContext.current
    val whereColors = WatchPilotColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_where_to_watch")
    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = whereColors.goldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Where to Watch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Region badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = whereColors.goldChipContainer,
                                border = BorderStroke(1.dp, whereColors.goldChipBorder)
                            ) {
                                Text(
                                    text = "${RegionalDefaults.supportedRegions.find { it.code.equals(effectiveRegionCode, ignoreCase = true) }?.flag ?: "🌐"} ${RegionalDefaults.getRegionLabel(effectiveRegionCode)}",
                                    color = whereColors.goldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // TV Series Season & Episode Selector for direct playback deep linking
                        if (!isMovie) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, whereColors.cyanChipBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag("tv_episode_direct_selector")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                             Text(
                                                 text = "Target Episode",
                                                 style = MaterialTheme.typography.labelLarge,
                                                 fontWeight = FontWeight.Bold,
                                                 color = MaterialTheme.colorScheme.onSurface
                                             )
                                             Text(
                                                 text = "Opens in selected streaming app",
                                                 style = MaterialTheme.typography.labelSmall,
                                                 fontSize = 11.sp,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                             )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = whereColors.cyanChipContainer,
                                            border = BorderStroke(1.dp, whereColors.cyanChipBorder)
                                        ) {
                                            Text(
                                                text = "S$selectedSeason · E$selectedEpisode",
                                                color = whereColors.cyanAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompactStepperControl(
                                            label = "Season",
                                            limitHint = if (maxSeasons > 1) "of $maxSeasons" else null,
                                            value = selectedSeason,
                                            canDecrement = selectedSeason > 1,
                                            canIncrement = selectedSeason < maxSeasons,
                                            onDecrement = {
                                                if (selectedSeason > 1) {
                                                    onSelectSeason(selectedSeason - 1)
                                                }
                                            },
                                            onIncrement = {
                                                if (selectedSeason < maxSeasons) {
                                                    onSelectSeason(selectedSeason + 1)
                                                }
                                            },
                                            decrementDescription = "Previous season",
                                            incrementDescription = "Next season",
                                            modifier = Modifier.weight(1f)
                                        )

                                        CompactStepperControl(
                                            label = "Episode",
                                            limitHint = maxEpisodesInSelectedSeason?.let { "of $it" },
                                            value = selectedEpisode,
                                            canDecrement = selectedEpisode > 1,
                                            canIncrement = maxEpisodesInSelectedSeason == null || selectedEpisode < maxEpisodesInSelectedSeason,
                                            onDecrement = {
                                                if (selectedEpisode > 1) {
                                                    onSelectEpisode(selectedEpisode - 1)
                                                }
                                            },
                                            onIncrement = {
                                                val maxEp = maxEpisodesInSelectedSeason
                                                if (maxEp == null || selectedEpisode < maxEp) {
                                                    onSelectEpisode(selectedEpisode + 1)
                                                }
                                            },
                                            decrementDescription = "Previous episode",
                                            incrementDescription = "Next episode",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoadingWatchProviders) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = whereColors.goldAccent,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Checking availability...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (streamAndFree.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = whereColors.greenAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMovie) "STREAM NOW" else "WATCH S$selectedSeason:E$selectedEpisode NOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = whereColors.greenAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                streamAndFree.forEach { provider ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = whereColors.neutralChipContainer,
                                        border = BorderStroke(1.dp, whereColors.neutralChipBorder),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .clickable {
                                                WatchProviderLauncher.launchProvider(
                                                    context = context,
                                                    providerName = provider.name,
                                                    title = media.title,
                                                    releaseYear = media.releaseYear,
                                                    isMovie = isMovie,
                                                    seasonNumber = if (!isMovie) selectedSeason else null,
                                                    episodeNumber = if (!isMovie) selectedEpisode else null,
                                                    webLink = provider.webLink ?: providersInfo.justWatchLink,
                                                    directContentUrl = provider.directContentUrl
                                                )
                                            }
                                            .testTag("btn_watch_${provider.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (provider.logoUrl != null) {
                                                AsyncImage(
                                                    model = provider.logoUrl,
                                                    contentDescription = provider.name,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(6.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = whereColors.goldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = provider.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Open in app",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val rentOrBuy = (providersInfo.rent + providersInfo.buy).distinctBy { it.name }
                        if (rentOrBuy.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = whereColors.cyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RENT OR BUY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = whereColors.cyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rentOrBuy.forEach { provider ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = whereColors.neutralChipContainer,
                                        border = BorderStroke(1.dp, whereColors.neutralChipBorder),
                                        modifier = Modifier
                                            .clickable {
                                                WatchProviderLauncher.launchProvider(
                                                    context = context,
                                                    providerName = provider.name,
                                                    title = media.title,
                                                    releaseYear = media.releaseYear,
                                                    isMovie = isMovie,
                                                    seasonNumber = if (!isMovie) selectedSeason else null,
                                                    episodeNumber = if (!isMovie) selectedEpisode else null,
                                                    webLink = provider.webLink ?: providersInfo.justWatchLink,
                                                    directContentUrl = provider.directContentUrl
                                                )
                                            }
                                            .testTag("btn_buy_${provider.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (provider.logoUrl != null) {
                                                AsyncImage(
                                                    model = provider.logoUrl,
                                                    contentDescription = provider.name,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Text(
                                                text = provider.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (streamAndFree.isEmpty() && rentOrBuy.isEmpty() && !isLoadingWatchProviders) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Not on subscription in ${RegionalDefaults.getRegionLabel(effectiveRegionCode)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    WatchProviderLauncher.launchProvider(
                                        context = context,
                                        providerName = "Google TV",
                                        title = media.title,
                                        releaseYear = media.releaseYear,
                                        isMovie = isMovie,
                                        seasonNumber = if (!isMovie) selectedSeason else null,
                                        episodeNumber = if (!isMovie) selectedEpisode else null,
                                        webLink = providersInfo.justWatchLink
                                    )
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = whereColors.neutralChipContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, whereColors.neutralChipBorder),
                                modifier = Modifier.testTag("btn_search_digital_stores")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Digital Stores")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Availability by JustWatch • Data by TMDB",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaWatchStatusSelectorSection(
    media: MediaEntity,
    isTablet: Boolean,
    onUpdateStatus: (WatchStatus) -> Unit
) {
                Text(
                    text = "Watch Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val statuses = remember {
                    listOf(
                        WatchStatus.PLAN_TO_WATCH,
                        WatchStatus.WATCHING,
                        WatchStatus.COMPLETED,
                        WatchStatus.ON_HOLD,
                        WatchStatus.DROPPED
                    )
                }

                val detailChipColors = WatchPilotColors.current
                if (isTablet) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statuses.forEach { st ->
                            val isSelected = media.getWatchStatusEnum() == st
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateStatus(st) },
                                label = {
                                    Text(
                                        st.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = detailChipColors.neutralChipContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = detailChipColors.goldSolidBg,
                                    selectedLabelColor = detailChipColors.onGoldSolid
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = detailChipColors.neutralChipBorder,
                                    selectedBorderColor = detailChipColors.goldChipBorder
                                )
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statuses.forEach { st ->
                            val isSelected = media.getWatchStatusEnum() == st
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateStatus(st) },
                                label = {
                                    Text(
                                        st.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = detailChipColors.neutralChipContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = detailChipColors.goldSolidBg,
                                    selectedLabelColor = detailChipColors.onGoldSolid
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = detailChipColors.neutralChipBorder,
                                    selectedBorderColor = detailChipColors.goldChipBorder
                                )
                            )
                        }
                    }
                }
}

@Composable
private fun MediaEpisodeProgressCard(
    media: MediaEntity,
    maxSeasons: Int,
    nextUpSeason: Int,
    nextUpEpisode: Int,
    showStreamingProviders: Boolean,
    streamAndFree: List<WatchProviderItem>,
    providersInfo: WatchProvidersInfo,
    selectedSeason: Int,
    selectedEpisode: Int,
    onSetWatchedEpisodes: (Int) -> Unit,
    onIncrementEpisode: () -> Unit
) {
    val context = LocalContext.current
    val progressColors = WatchPilotColors.current
    val hasStartedWatching = media.watchedEpisodes > 0
    val isSeriesCompleted = media.watchedEpisodes >= media.totalEpisodes
    val progressPercent = (media.progressFraction * 100).toInt().coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episode Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${media.watchedEpisodes} / ${media.totalEpisodes} watched",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (hasStartedWatching) FontWeight.Bold else FontWeight.Medium,
                                        color = if (hasStartedWatching) {
                                            progressColors.cyanAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (hasStartedWatching) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSeriesCompleted) progressColors.greenChipContainer else progressColors.goldChipContainer,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSeriesCompleted) progressColors.greenChipBorder else progressColors.goldChipBorder
                                            )
                                        ) {
                                            Text(
                                                text = "$progressPercent%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSeriesCompleted) progressColors.greenAccent else progressColors.goldAccent,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { media.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = if (isSeriesCompleted) progressColors.greenSolidBg else progressColors.goldSolidBg,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Full-width Up Next summary row separated cleanly from action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isSeriesCompleted -> "Completed"
                                        hasStartedWatching -> "Up next"
                                        else -> "Not started"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isSeriesCompleted -> progressColors.greenAccent
                                        hasStartedWatching -> progressColors.cyanAccent
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isSeriesCompleted -> progressColors.greenChipContainer
                                        hasStartedWatching -> progressColors.cyanChipContainer
                                        else -> progressColors.neutralChipContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isSeriesCompleted -> progressColors.greenChipBorder
                                            hasStartedWatching -> progressColors.cyanChipBorder
                                            else -> progressColors.neutralChipBorder
                                        }
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            isSeriesCompleted -> "All watched"
                                            maxSeasons > 1 -> "S$nextUpSeason · E$nextUpEpisode"
                                            else -> "Episode $nextUpEpisode"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSeriesCompleted -> progressColors.greenAccent
                                            hasStartedWatching -> progressColors.cyanAccent
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dedicated full-width Progress Actions row so buttons never clamp or wrap
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasStartedWatching) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = progressColors.neutralChipContainer,
                                        border = BorderStroke(1.dp, progressColors.neutralChipBorder),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                onSetWatchedEpisodes(media.watchedEpisodes - 1)
                                            }
                                            .testTag("btn_undo_episode")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Undo last watched episode",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Undo",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSeriesCompleted) {
                                        progressColors.greenChipContainer
                                    } else {
                                        progressColors.cyanChipContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSeriesCompleted) {
                                            progressColors.greenChipBorder
                                        } else {
                                            progressColors.cyanChipBorder
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isSeriesCompleted) {
                                            onIncrementEpisode()
                                        }
                                        .testTag("btn_mark_next_episode")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSeriesCompleted) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Outlined.CheckCircle
                                            },
                                            contentDescription = if (isSeriesCompleted) {
                                                "All episodes watched"
                                            } else {
                                                "Mark episode $nextUpEpisode watched"
                                            },
                                            tint = if (isSeriesCompleted) progressColors.greenAccent else progressColors.cyanAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when {
                                                isSeriesCompleted -> "All Watched"
                                                maxSeasons > 1 -> "Mark S$nextUpSeason·E$nextUpEpisode Watched"
                                                else -> "Mark E$nextUpEpisode Watched"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSeriesCompleted) progressColors.greenAccent else progressColors.cyanAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (showStreamingProviders && streamAndFree.isNotEmpty()) {
                                val topProvider = streamAndFree.first()
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = progressColors.greenChipContainer,
                                    border = BorderStroke(1.dp, progressColors.greenChipBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            WatchProviderLauncher.launchProvider(
                                                context = context,
                                                providerName = topProvider.name,
                                                title = media.title,
                                                releaseYear = media.releaseYear,
                                                isMovie = false,
                                                seasonNumber = selectedSeason,
                                                episodeNumber = selectedEpisode,
                                                webLink = topProvider.webLink ?: providersInfo.justWatchLink,
                                                directContentUrl = topProvider.directContentUrl
                                            )
                                        }
                                        .testTag("btn_watch_episode_progress_direct")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = progressColors.greenAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Watch S$selectedSeason · E$selectedEpisode on ${topProvider.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = progressColors.greenAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = progressColors.greenAccent.copy(alpha = 0.8f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
}

@Composable
private fun MediaSeasonsSection(
    media: MediaEntity,
    seasons: List<SeasonItem>,
    isLoadingSeasons: Boolean,
    onSetSeasonStatus: (Int, TvWatchStatus) -> Unit,
    onOpenSeasonDetail: (SeasonItem) -> Unit
) {
                    val seasonSecColors = WatchPilotColors.current
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = seasonSecColors.cyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Seasons & Episodes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Tap a season for episodes & details",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isLoadingSeasons) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = seasonSecColors.goldAccent,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        seasons.forEach { seasonItem ->
                            SeasonOverviewCard(
                                season = seasonItem,
                                media = media,
                                onStatusChange = { newStatus ->
                                    onSetSeasonStatus(seasonItem.seasonNumber, newStatus)
                                },
                                onClick = {
                                    onOpenSeasonDetail(seasonItem)
                                }
                            )
                        }
                    }
                    }
}

@Composable
private fun MediaRatingAndReviewCard(
    media: MediaEntity,
    onEditRating: () -> Unit
) {
                val ratingColors = WatchPilotColors.current
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Rating & Review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            IconButton(onClick = onEditRating) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Rating", tint = ratingColors.goldAccent)
                            }
                        }

                        if (media.userRating > 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..5) {
                                    val isFilled = media.getUserRatingInStars() >= i.toFloat() - 0.25f
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) ratingColors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${media.getFormattedUserRating()} Stars",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ratingColors.goldAccent
                                )
                            }
                            if (media.userReview.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${media.userReview}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = "Not rated yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onEditRating,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ratingColors.goldChipContainer,
                                    contentColor = ratingColors.goldAccent
                                ),
                                border = BorderStroke(1.dp, ratingColors.goldChipBorder)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rate Title", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
}

@Composable
private fun MediaCustomCollectionCard(
    media: MediaEntity,
    onChangeClick: () -> Unit
) {
                val collectionColors = WatchPilotColors.current
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Collection / List",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = media.customWatchlist ?: "Default Watchlist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = collectionColors.goldAccent,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = onChangeClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = collectionColors.neutralChipContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, collectionColors.neutralChipBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.widthIn(min = 104.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Change",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
}

@Composable
private fun MediaSynopsisAndCastSection(
    media: MediaEntity,
    displayCast: List<CastMemberItem>,
    onCastClick: (CastMemberItem) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {

                // Overview / Synopsis
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = media.overview.ifBlank { "No overview available." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Director / Creator
                Text(
                    text = if (media.getMediaTypeEnum() == MediaType.MOVIE) "Director" else "Created By",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.directorOrCreator.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Starring Cast with Circular Images & Name Below
                if (displayCast.isNotEmpty()) {
                    Text(
                        text = "Starring Cast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        displayCast.forEach { member ->
                            val initials = remember(member.name) {
                                member.name
                                    .split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .ifBlank { "?" }
                            }
                            val castColors = WatchPilotColors.current
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCastClick(member) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .testTag("cast_chip_${member.name}")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.5.dp, castColors.cyanChipBorder),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = castColors.cyanAccent
                                        )
                                        if (!member.profileUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(member.profileUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = member.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!member.character.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = member.character,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
    }
}

@Composable
fun SeasonOverviewCard(
    season: SeasonItem,
    media: MediaEntity,
    onStatusChange: (TvWatchStatus) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColors = WatchPilotColors.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("season_card_${season.seasonNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Season Poster
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                    val posterSource = season.posterUrl.ifBlank { media.posterUrl }
                    if (posterSource.isNotBlank()) {
                        AsyncImage(
                            model = posterSource,
                            contentDescription = season.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = season.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        // Rating badge
                        if (season.rating > 0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = cardColors.goldAccent, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format("%.1f", season.rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardColors.goldAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${season.releaseYear} • ${season.episodeCount} Episodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { season.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = cardColors.goldSolidBg,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${season.watchedEpisodesCount}/${season.episodeCount} eps watched",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = cardColors.cyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onClick() }
                        ) {
                            Text(
                                text = "Episodes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = cardColors.goldAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View episodes",
                                tint = cardColors.goldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Season Watch Status Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TvWatchStatus.WATCHED to "Watched",
                    TvWatchStatus.WATCHING to "Watching",
                    TvWatchStatus.PLAN_TO_WATCH to "Plan",
                    TvWatchStatus.UNWATCHED to "Clear"
                ).forEach { (status, label) ->
                    val isSelected = season.watchStatus == status
                    val selectedBg = when (status) {
                        TvWatchStatus.WATCHED -> cardColors.greenSolidBg
                        TvWatchStatus.WATCHING -> cardColors.cyanSolidBg
                        TvWatchStatus.PLAN_TO_WATCH -> cardColors.goldSolidBg
                        TvWatchStatus.UNWATCHED -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val selectedFg = when (status) {
                        TvWatchStatus.WATCHED -> cardColors.onGreenSolid
                        TvWatchStatus.WATCHING -> cardColors.onCyanSolid
                        TvWatchStatus.PLAN_TO_WATCH -> cardColors.onGoldSolid
                        TvWatchStatus.UNWATCHED -> MaterialTheme.colorScheme.onSurface
                    }
                    val selectedBorder = when (status) {
                        TvWatchStatus.WATCHED -> cardColors.greenChipBorder
                        TvWatchStatus.WATCHING -> cardColors.cyanChipBorder
                        TvWatchStatus.PLAN_TO_WATCH -> cardColors.goldChipBorder
                        TvWatchStatus.UNWATCHED -> cardColors.neutralChipBorder
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) selectedBg else cardColors.neutralChipContainer,
                        border = BorderStroke(1.dp, if (isSelected) selectedBorder else cardColors.neutralChipBorder),
                        modifier = Modifier
                            .clickable { onStatusChange(status) }
                            .testTag("chip_s_${season.seasonNumber}_${status.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    TvWatchStatus.WATCHED -> Icons.Default.Check
                                    TvWatchStatus.WATCHING -> Icons.Default.Visibility
                                    TvWatchStatus.PLAN_TO_WATCH -> Icons.Default.Bookmark
                                    TvWatchStatus.UNWATCHED -> Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = if (isSelected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStepperControl(
    label: String,
    limitHint: String?,
    value: Int,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            if (!limitHint.isNullOrBlank()) {
                Text(
                    text = limitHint,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = canDecrement, onClick = onDecrement)
                        .testTag("stepper_${label.lowercase()}_minus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = decrementDescription,
                        tint = if (canDecrement) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(15.dp)
                    )
                }

                VerticalDivider(
                    thickness = 0.75.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "$value",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    thickness = 0.75.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = canIncrement, onClick = onIncrement)
                        .testTag("stepper_${label.lowercase()}_plus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = incrementDescription,
                        tint = if (canIncrement) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaAiCompanionCard(
    isMovie: Boolean,
    monetizationState: MonetizationState,
    briefState: TitleAiBriefUiState,
    onGenerate: () -> Unit,
    onOpenPaywall: () -> Unit,
    onRewardEarned: () -> Unit
) {
    val aiColors = WatchPilotColors.current
    val pillText = when {
        monetizationState.isPro -> "PRO"
        monetizationState.rewardedTrialCredits > 0 -> "${monetizationState.rewardedTrialCredits} Ad Pass"
        else -> "${monetizationState.remainingFreeAiRecaps}/${MonetizationState.FREE_AI_RECAPS_LIMIT} Free/mo"
    }
    val pillBg = when {
        monetizationState.isPro -> aiColors.goldChipContainer
        monetizationState.canUseAiRecap -> aiColors.cyanChipContainer
        else -> aiColors.redChipContainer
    }
    val pillBorder = when {
        monetizationState.isPro -> aiColors.goldChipBorder
        monetizationState.canUseAiRecap -> aiColors.cyanChipBorder
        else -> aiColors.redChipBorder
    }
    val pillFg = when {
        monetizationState.isPro -> aiColors.goldAccent
        monetizationState.canUseAiRecap -> aiColors.cyanAccent
        else -> aiColors.redAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media_ai_companion_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, aiColors.cyanChipBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = aiColors.cyanChipContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Companion",
                                tint = aiColors.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "AI Companion • Recap & Fit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isMovie) {
                                "Spoiler-free mood hook & personal watch-fit"
                            } else {
                                "Spoiler-free catch-up & personal watch-fit"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillBg,
                    border = BorderStroke(1.dp, pillBorder)
                ) {
                    Text(
                        text = pillText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pillFg,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            when (briefState) {
                TitleAiBriefUiState.Idle -> {
                    if (!monetizationState.canUseAiRecap) {
                        RewardedTrialPassBanner(
                            monetizationState = monetizationState,
                            sourceTag = "media_detail_recap",
                            onRewardEarned = onRewardEarned
                        )
                    }

                    Button(
                        onClick = {
                            if (monetizationState.canUseAiRecap) onGenerate() else onOpenPaywall()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (monetizationState.canUseAiRecap) aiColors.cyanSolidBg else aiColors.goldSolidBg,
                            contentColor = if (monetizationState.canUseAiRecap) aiColors.onCyanSolid else aiColors.onGoldSolid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_generate_ai_brief")
                    ) {
                        Icon(
                            imageVector = if (monetizationState.canUseAiRecap) {
                                Icons.Filled.AutoAwesome
                            } else {
                                Icons.Filled.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (monetizationState.canUseAiRecap) {
                                if (isMovie) "Get Watch-Fit & Mood Hook" else "Get Spoiler-Free Recap & Fit"
                            } else {
                                "Unlock Unlimited AI Recaps with Pro"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TitleAiBriefUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = aiColors.cyanAccent
                        )
                        Text(
                            text = "Generating spoiler-free AI brief...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is TitleAiBriefUiState.Success -> {
                    val brief = briefState.brief
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = aiColors.greenChipContainer,
                            border = BorderStroke(1.dp, aiColors.greenChipBorder)
                        ) {
                            Text(
                                text = "${brief.personalMatchScore}% Match",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = aiColors.greenAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        if (brief.watchPriorityTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = aiColors.goldChipContainer,
                                border = BorderStroke(1.dp, aiColors.goldChipBorder)
                            ) {
                                Text(
                                    text = brief.watchPriorityTag,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = aiColors.goldAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "WHY IT FITS YOU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = aiColors.cyanAccent,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = brief.fitSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isMovie) "SPOILER-FREE HOOK" else "SPOILER-FREE RECAP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = aiColors.goldAccent,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = brief.seasonRecap,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!monetizationState.canUseAiRecap) {
                        RewardedTrialPassBanner(
                            monetizationState = monetizationState,
                            sourceTag = "media_detail_refresh",
                            onRewardEarned = onRewardEarned
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (monetizationState.canUseAiRecap) onGenerate() else onOpenPaywall()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, aiColors.neutralChipBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_refresh_ai_brief")
                    ) {
                        Icon(
                            imageVector = if (monetizationState.canUseAiRecap) {
                                Icons.Filled.Refresh
                            } else {
                                Icons.Filled.WorkspacePremium
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (monetizationState.canUseAiRecap) {
                                "Refresh Brief"
                            } else {
                                "Upgrade to Pro for Unlimited Briefs"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is TitleAiBriefUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = aiColors.redChipContainer,
                        border = BorderStroke(1.dp, aiColors.redChipBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = briefState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (monetizationState.canUseAiRecap) onGenerate() else onOpenPaywall()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = aiColors.neutralChipContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, aiColors.neutralChipBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_retry_ai_brief")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

