package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.theme.CinemaCyan
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.model.RegionalDefaults
import com.example.data.model.WatchStatus
import com.example.ui.components.AiConciergeSheet
import com.example.ui.components.LegalComplianceDialog
import com.example.ui.components.OttMoneySaverSheet
import com.example.ui.components.SnapScannerSheet
import com.example.ui.components.SwipeToMatchSheet
import com.example.ui.components.WatchPilotProPaywallSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeSectionGridScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PersonDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeasonDetailScreen
import com.example.ui.screens.TrailersScreen
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.WatchPilotTheme
import com.example.ui.util.AdaptiveLayoutProvider
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.TrailerInterstitialAdManager
import com.example.util.configureAppCheck
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class MainNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TRAILERS("Trailers", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_trailers"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    LIBRARY("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_library"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Configure Firebase App Check with launch intent BEFORE ViewModels, Firebase calls, or setContent
        configureAppCheck(this, intent)

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                MobileAds.initialize(this@MainActivity) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            TrailerInterstitialAdManager.preload(this@MainActivity)
                        }
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    var showLaunchSplash by rememberSaveable { mutableStateOf(true) }
                    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) {
                        if (showLaunchSplash) {
                            delay(780)
                            showLaunchSplash = false
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = hasCompletedOnboarding,
                            transitionSpec = { fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(240)) },
                            label = "onboardingGateTransition"
                        ) { completed ->
                            if (completed) {
                                MovieverseApp(viewModel = viewModel)
                            } else {
                                OnboardingScreen(viewModel = viewModel)
                            }
                        }
                        AnimatedVisibility(
                            visible = showLaunchSplash,
                            exit = fadeOut(animationSpec = tween(durationMillis = 420))
                        ) {
                            WatchPilotLaunchSplash()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPilotLaunchSplash() {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val logoScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.86f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.25f,
        animationSpec = tween(durationMillis = 420),
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1119),
                        Color(0xFF0B0D13),
                        Color(0xFF0E131D)
                    )
                )
            )
            .testTag("launch_splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle dual radial spotlight behind the Titanium Aperture
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CinemaGold.copy(alpha = 0.16f),
                            CinemaCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0A1119),
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            CinemaGold.copy(alpha = 0.75f),
                            CinemaCyan.copy(alpha = 0.65f)
                        )
                    )
                ),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .size(132.dp)
                    .scale(logoScale)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.watchpilot_aperture_icon),
                    contentDescription = "WatchPilot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "WatchPilot",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "STREAM SMARTER • DECIDE FASTER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CinemaGold,
                letterSpacing = 1.8.sp
            )
        }
    }
}

@Composable
fun MovieverseApp(viewModel: MediaViewModel) {
    val context = LocalContext.current
    var currentNav by remember { mutableStateOf(MainNavDestination.HOME) }

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val trackedMedia by viewModel.trackedMedia.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val selectedMediaId by viewModel.selectedMediaId.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val selectedSeasonForDetail by viewModel.selectedSeasonForDetail.collectAsStateWithLifecycle()

    val trendingItems by viewModel.trendingItems.collectAsStateWithLifecycle()
    val currentTopRatedList by viewModel.currentTopRatedList.collectAsStateWithLifecycle()
    val pickedForYouItems by viewModel.pickedForYouItems.collectAsStateWithLifecycle()
    val popularHomeItems by viewModel.popularHomeItems.collectAsStateWithLifecycle()
    val nowPlayingHomeItems by viewModel.nowPlayingHomeItems.collectAsStateWithLifecycle()
    val upcomingHomeItems by viewModel.upcomingHomeItems.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedGenreFilter by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val activeSectionGridState by viewModel.activeSectionGridUiState.collectAsStateWithLifecycle()
    val favoritePeople by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPersonForDetail.collectAsStateWithLifecycle()
    val personDetail by viewModel.personDetailData.collectAsStateWithLifecycle()
    val isLoadingPersonDetail by viewModel.isLoadingPersonDetail.collectAsStateWithLifecycle()

    // WatchPilot v2.0 Sheet States
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()
    val ottMoneySaverSummary by viewModel.ottMoneySaverSummary.collectAsStateWithLifecycle()
    val snapScanState by viewModel.snapScanState.collectAsStateWithLifecycle()
    val conciergeState by viewModel.conciergeState.collectAsStateWithLifecycle()
    val matchRoomState by viewModel.matchRoomState.collectAsStateWithLifecycle()
    val skippedMatchIds by viewModel.skippedMatchCandidateIds.collectAsStateWithLifecycle()

    val showPaywall by viewModel.showPaywallSheet.collectAsStateWithLifecycle()
    val paywallReason by viewModel.paywallTriggerReason.collectAsStateWithLifecycle()
    val showSnapScanner by viewModel.showSnapScannerSheet.collectAsStateWithLifecycle()
    val showAiConcierge by viewModel.showAiConciergeSheet.collectAsStateWithLifecycle()
    val showSwipeMatch by viewModel.showSwipeMatchSheet.collectAsStateWithLifecycle()
    val showOttMoneySaver by viewModel.showOttMoneySaverSheet.collectAsStateWithLifecycle()
    val showLegalDialog by viewModel.showLegalComplianceDialog.collectAsStateWithLifecycle()

    val selectedMedia = remember(
        selectedMediaId,
        allMedia,
        popularHomeItems,
        nowPlayingHomeItems,
        upcomingHomeItems,
        trendingItems,
        currentTopRatedList,
        pickedForYouItems
    ) {
        allMedia.find { it.id == selectedMediaId }
            ?: popularHomeItems.find { it.id == selectedMediaId }
            ?: nowPlayingHomeItems.find { it.id == selectedMediaId }
            ?: upcomingHomeItems.find { it.id == selectedMediaId }
            ?: trendingItems.find { it.id == selectedMediaId }
            ?: currentTopRatedList.find { it.id == selectedMediaId }
            ?: pickedForYouItems.find { it.id == selectedMediaId }
    }

    val regionDisplayName = remember(effectiveRegion) {
        RegionalDefaults.supportedRegions.find { it.code.equals(effectiveRegion, ignoreCase = true) }?.name
            ?: RegionalDefaults.detectDeviceRegion().name
    }

    val adaptive = LocalAdaptiveLayoutInfo.current
    val activeHomeSection = activeSectionGridState.section
    val showTopNav = selectedMedia == null &&
        selectedPerson == null &&
        !(currentNav == MainNavDestination.HOME && activeHomeSection != null)
    val useNavigationRail = adaptive.isTabletOrWide

    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail && showTopNav) {
            AdaptiveTabletNavigationRail(
                currentNav = currentNav,
                availableHeight = adaptive.availableHeight,
                onSelectDestination = { destination ->
                    if (destination == MainNavDestination.HOME) {
                        viewModel.closeHomeCatalogSection()
                    }
                    currentNav = destination
                }
            )
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentWindowInsets = if (useNavigationRail && showTopNav) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            } else {
                WindowInsets.safeDrawing
            },
            bottomBar = {
                if (!useNavigationRail && showTopNav) {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = MaterialTheme.colorScheme.surfaceVariant.let { 4.dp }
                    ) {
                        MainNavDestination.entries.forEach { destination ->
                            val selected = currentNav == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentNav = destination },
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = CinemaGold.copy(alpha = 0.25f),
                                    selectedIconColor = CinemaGold,
                                    selectedTextColor = CinemaGold,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = Pair(
                    Triple(selectedPerson, selectedMedia, selectedSeasonForDetail),
                    if (currentNav == MainNavDestination.HOME) activeHomeSection else null
                ),
                contentKey = { (detailTriple, sectionKey) ->
                    val (person, media, season) = detailTriple
                    "${person != null}|${media?.id}|${season?.seasonNumber}|${sectionKey?.name}"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screenTransition",
                modifier = Modifier.padding(innerPadding)
            ) { (detailTriple, activeSection) ->
            val (activePerson, activeMedia, activeSeason) = detailTriple
            if (activePerson != null) {
                val resolvedId = personDetail?.id?.takeIf { it > 0L } ?: activePerson.id
                val resolvedName = personDetail?.name?.takeIf { it.isNotBlank() } ?: activePerson.name
                val isPersonFavorite = remember(resolvedId, resolvedName, favoritePeople) {
                    favoritePeople.any { fav ->
                        (resolvedId > 0L && fav.personId == resolvedId) ||
                            fav.name.equals(resolvedName, ignoreCase = true)
                    }
                }
                PersonDetailScreen(
                    person = activePerson,
                    detailDto = personDetail,
                    isLoading = isLoadingPersonDetail,
                    isFavorite = isPersonFavorite,
                    trackedMedia = allMedia,
                    onBack = { viewModel.selectPersonForDetail(null) },
                    onToggleFavorite = { viewModel.toggleFavoritePerson(activePerson, personDetail) },
                    onMediaClick = { media ->
                        viewModel.selectPersonForDetail(null)
                        viewModel.openMediaFromSearch(media)
                    },
                    onToggleWatchlist = { media ->
                        viewModel.toggleWatchlistQuick(media)
                    }
                )
            } else if (activeMedia != null) {
                if (activeSeason != null) {
                    SeasonDetailScreen(
                        media = activeMedia,
                        season = activeSeason,
                        viewModel = viewModel,
                        effectiveRegionCode = effectiveRegion,
                        onBack = { viewModel.closeSeasonDetail() }
                    )
                } else {
                    MediaDetailScreen(
                        media = activeMedia,
                        viewModel = viewModel,
                        availableCollections = customCollections,
                        effectiveRegionCode = effectiveRegion,
                        onBack = { viewModel.selectMedia(null) }
                    )
                }
            } else if (activeSection != null && currentNav == MainNavDestination.HOME) {
                HomeSectionGridScreen(
                    section = activeSection,
                    regionName = regionDisplayName,
                    items = activeSectionGridState.items,
                    totalAvailableCount = activeSectionGridState.totalAvailableCount,
                    selectedType = selectedTypeFilter,
                    selectedGenre = selectedGenreFilter,
                    availableGenres = availableGenres,
                    isLoadingMore = activeSectionGridState.isLoadingMore,
                    hasMore = activeSectionGridState.hasMore,
                    onSelectType = { type -> viewModel.selectedTypeFilter.value = type },
                    onSelectGenre = { genre -> viewModel.selectedGenreFilter.value = genre },
                    onLoadMore = { viewModel.loadMoreForHomeSection(activeSection) },
                    onMediaClick = { id -> viewModel.selectMedia(id) },
                    onBack = { viewModel.closeHomeCatalogSection() },
                    people = activeSectionGridState.people,
                    onPersonClick = { person -> viewModel.selectPersonForDetail(person) }
                )
            } else {
                if (currentNav != MainNavDestination.HOME) {
                    BackHandler {
                        currentNav = MainNavDestination.HOME
                    }
                }

                when (currentNav) {
                    MainNavDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) },
                        onPersonClick = { person -> viewModel.selectPersonForDetail(person) },
                        onNavigateToSearch = { currentNav = MainNavDestination.SEARCH },
                        onOpenSection = { section -> viewModel.openHomeCatalogSection(section) }
                    )

                    MainNavDestination.TRAILERS -> TrailersScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) }
                    )

                    MainNavDestination.SEARCH -> SearchScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) }
                    )

                    MainNavDestination.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) },
                        onExploreClick = {
                            viewModel.closeHomeCatalogSection()
                            currentNav = MainNavDestination.HOME
                        }
                    )

                    MainNavDestination.PROFILE -> ProfileScreen(
                        viewModel = viewModel
                    )
                }
            }
        }

        // WatchPilot v2.0 Global Modal Bottom Sheets
        if (!BuildConfig.IS_V1_EDITION) {
            if (showPaywall) {
                WatchPilotProPaywallSheet(
                    monetizationState = monetizationState,
                    triggerReason = paywallReason,
                    onDismiss = { viewModel.dismissPaywall() },
                    onPurchaseSku = { act, sku -> viewModel.purchasePro(act, sku) },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    onActivateSandboxPro = { viewModel.activateSandboxPro() },
                    onDeactivateSandboxPro = { viewModel.deactivateSandboxPro() }
                )
            }

            if (showSnapScanner) {
                SnapScannerSheet(
                    scanState = snapScanState,
                    monetizationState = monetizationState,
                    trackedMedia = trackedMedia,
                    effectiveRegionCode = effectiveRegion,
                    onDismiss = { viewModel.closeSnapScanner() },
                    onPickScreenshotUri = { uri -> viewModel.scanScreenshotUri(context, uri) },
                    onAddCandidate = { cand -> viewModel.addScannedCandidateToWatchlist(cand) },
                    onAddAllToWatchlist = { viewModel.addAllScannedToWatchlist() },
                    onOpenMedia = { entity ->
                        viewModel.closeSnapScanner()
                        viewModel.openMediaFromSearch(entity)
                    },
                    onUpgradePro = {
                        viewModel.closeSnapScanner()
                        viewModel.openPaywall("Unlock unlimited Snap-to-Watchlist Vision Scans with WatchPilot Pro.")
                    }
                )
            }

            if (showAiConcierge) {
                AiConciergeSheet(
                    conciergeState = conciergeState,
                    monetizationState = monetizationState,
                    trackedMedia = trackedMedia,
                    effectiveRegionCode = effectiveRegion,
                    onDismiss = { viewModel.closeAiConcierge() },
                    onGeneratePicks = { mood, budget, aud -> viewModel.askAiConcierge(mood, budget, aud) },
                    onAddPickToWatchlist = { pick ->
                        val entity = pick.matchedEntity
                        if (entity != null) {
                            viewModel.updateStatus(entity.id, WatchStatus.PLAN_TO_WATCH)
                        }
                    },
                    onOpenMedia = { entity ->
                        viewModel.closeAiConcierge()
                        viewModel.openMediaFromSearch(entity)
                    },
                    onUpgradePro = {
                        viewModel.closeAiConcierge()
                        viewModel.openPaywall("Unlock unlimited Tonight's AI Concierge picks with WatchPilot Pro.")
                    }
                )
            }

            if (showSwipeMatch) {
                SwipeToMatchSheet(
                    currentUser = currentUser,
                    matchRoomState = matchRoomState,
                    skippedIds = skippedMatchIds,
                    monetizationState = monetizationState,
                    onDismiss = { viewModel.closeSwipeMatch() },
                    onSignInWithGoogle = { act ->
                        viewModel.signInWithGoogle(
                            activity = act,
                            onError = { err -> viewModel.reportMatchRoomAuthError(err) }
                        )
                    },
                    onCreateRoom = { providers -> viewModel.createMatchRoom(providers) },
                    onJoinRoom = { code -> viewModel.joinMatchRoom(code) },
                    onSwipeCandidate = { cand, liked -> viewModel.swipeMatchCard(cand, liked) },
                    onLeaveOrCloseRoom = { viewModel.leaveOrCloseMatchRoom() },
                    onOpenMatchedMedia = { id, title, yr, type ->
                        viewModel.openMatchedMediaFromRoom(id, title, yr, type)
                    }
                )
            }

            if (showOttMoneySaver) {
                OttMoneySaverSheet(
                    monetizationState = monetizationState,
                    summary = ottMoneySaverSummary,
                    effectiveRegionCode = effectiveRegion,
                    onDismiss = { viewModel.closeOttMoneySaver() },
                    onToggleProvider = { name -> viewModel.toggleSubscribedProvider(name) },
                    onToggleStreamOnMyAppsOnly = { enabled -> viewModel.setStreamOnMyAppsOnly(enabled) },
                    onUpgradePro = {
                        viewModel.closeOttMoneySaver()
                        viewModel.openPaywall("Unlock the full OTT Money-Saver Radar & 'On My Apps' Library filter with WatchPilot Pro.")
                    }
                )
            }
        }

        if (showLegalDialog) {
            LegalComplianceDialog(
                onDismiss = { viewModel.closeLegalCompliance() }
            )
        }
    }
    }
}

@Composable
internal fun AdaptiveTabletNavigationRail(
    currentNav: MainNavDestination,
    availableHeight: Dp,
    onSelectDestination: (MainNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isShortLandscapeHeight = availableHeight < 540.dp
    val isUltraShortHeight = availableHeight < 420.dp

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Vertical + WindowInsetsSides.Start
        ),
        header = if (isUltraShortHeight) {
            null
        } else {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        top = if (isShortLandscapeHeight) 2.dp else 8.dp,
                        bottom = if (isShortLandscapeHeight) 4.dp else 12.dp
                    )
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0A1119),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(listOf(CinemaGold, CinemaCyan))
                        ),
                        modifier = Modifier.size(if (isShortLandscapeHeight) 32.dp else 40.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.watchpilot_aperture_icon),
                            contentDescription = "WatchPilot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .testTag("tablet_nav_rail")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isShortLandscapeHeight) {
                Arrangement.spacedBy(0.dp, Alignment.Top)
            } else {
                Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            }
        ) {
            MainNavDestination.entries.forEach { destination ->
                val selected = currentNav == destination
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectDestination(destination) },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.title,
                            modifier = if (isShortLandscapeHeight) Modifier.size(20.dp) else Modifier
                        )
                    },
                    label = {
                        Text(
                            text = destination.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isShortLandscapeHeight) 10.sp else 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = CinemaGold.copy(alpha = 0.25f),
                        selectedIconColor = CinemaGold,
                        selectedTextColor = CinemaGold,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag(destination.testTag)
                )
            }
        }
    }
}
