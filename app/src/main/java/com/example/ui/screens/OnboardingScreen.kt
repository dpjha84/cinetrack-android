package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.local.SampleMediaData
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.DarkBackground
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

private val ONBOARDING_GENRES = listOf(
    "Sci-Fi",
    "Action",
    "Thriller",
    "Drama",
    "Comedy",
    "Crime",
    "Mystery",
    "Romance",
    "Fantasy",
    "Horror",
    "Animation",
    "Documentary"
)

@Composable
fun OnboardingScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val step by viewModel.onboardingStep.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authLoading by viewModel.onboardingAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.onboardingAuthError.collectAsStateWithLifecycle()
    val syncNotice by viewModel.onboardingSyncNotice.collectAsStateWithLifecycle()

    val selectedGenres by viewModel.onboardingSelectedGenres.collectAsStateWithLifecycle()
    val favoritePeople by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val popularPeople by viewModel.onboardingPopularPeople.collectAsStateWithLifecycle()
    val personQuery by viewModel.onboardingPersonQuery.collectAsStateWithLifecycle()
    val personSearchResults by viewModel.onboardingPersonSearchResults.collectAsStateWithLifecycle()
    val isLoadingPeople by viewModel.isLoadingOnboardingPeople.collectAsStateWithLifecycle()

    val popularTitles by viewModel.onboardingPopularTitles.collectAsStateWithLifecycle()
    val titleQuery by viewModel.onboardingTitleQuery.collectAsStateWithLifecycle()
    val titleSearchResults by viewModel.onboardingTitleSearchResults.collectAsStateWithLifecycle()
    val isLoadingTitles by viewModel.isLoadingOnboardingTitles.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val trackedMedia by viewModel.trackedMedia.collectAsStateWithLifecycle()

    val welcomeShowcaseTitles = remember(popularTitles, allMedia) {
        val pool = (popularTitles + allMedia + SampleMediaData.getInitialCatalog())
            .distinctBy { it.id }
            .filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }
        val movies = pool.filter { it.getMediaTypeEnum() == MediaType.MOVIE }
        val shows = pool.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        buildList {
            val maxPairs = maxOf(movies.size, shows.size).coerceAtMost(5)
            for (i in 0 until maxPairs) {
                if (i < movies.size) add(movies[i])
                if (i < shows.size) add(shows[i])
            }
        }.take(8).ifEmpty { SampleMediaData.getInitialCatalog().take(8) }
    }

    var ambientActiveIndex by remember(welcomeShowcaseTitles.size) { mutableIntStateOf(0) }
    LaunchedEffect(welcomeShowcaseTitles.size) {
        if (welcomeShowcaseTitles.size > 1) {
            while (true) {
                delay(3200L)
                ambientActiveIndex = (ambientActiveIndex + 1) % welcomeShowcaseTitles.size
            }
        }
    }
    val safeAmbientIndex = if (welcomeShowcaseTitles.isNotEmpty()) ambientActiveIndex % welcomeShowcaseTitles.size else 0
    val activeAmbientMedia = welcomeShowcaseTitles.getOrNull(safeAmbientIndex)
    val backdropAlpha by animateFloatAsState(
        targetValue = if (step == 0) 0.36f else 0.30f,
        animationSpec = tween(durationMillis = 520),
        label = "onboardingBackdropAlpha"
    )

    val hasAnyPersonalizationSignal = remember(selectedGenres, favoritePeople, trackedMedia, allMedia) {
        selectedGenres.isNotEmpty() ||
            favoritePeople.isNotEmpty() ||
            trackedMedia.isNotEmpty() ||
            allMedia.any { it.isFavorite || it.getWatchStatusEnum() != WatchStatus.NONE }
    }

    if (step > 0) {
        BackHandler {
            viewModel.setOnboardingStep(step - 1)
        }
    }

    val adaptive = LocalAdaptiveLayoutInfo.current
    val containerMaxWidth = when {
        adaptive.isExpanded -> 1040.dp
        adaptive.isTabletOrWide -> 760.dp
        else -> 640.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1119),
                        DarkBackground,
                        Color(0xFF101521)
                    )
                )
            )
            .imePadding()
            .testTag("onboarding_root")
    ) {
        OnboardingAmbientBackdrop(
            activeMedia = activeAmbientMedia,
            imageAlpha = backdropAlpha,
            isInteractiveStep = step > 0,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            OnboardingProgressBar(
                currentStep = step,
                totalSteps = 3,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = containerMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut(tween(180)))
                },
                label = "onboardingStepTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { activeStep ->
                when (activeStep) {
                    0 -> OnboardingWelcomeStep(
                        currentUser = currentUser,
                        isLoading = authLoading,
                        errorMessage = authError,
                        showcaseTitles = welcomeShowcaseTitles,
                        sharedActiveIndex = safeAmbientIndex,
                        onSelectShowcaseIndex = { ambientActiveIndex = it },
                        showAmbientBackdrop = false,
                        onGoogleSignIn = {
                            if (activity != null) {
                                viewModel.signInWithGoogleDuringOnboarding(activity)
                            }
                        },
                        onContinueSignedIn = { viewModel.advanceFromOnboardingWelcome() },
                        onContinueAsGuest = { viewModel.continueAsGuestDuringOnboarding() }
                    )

                    1 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = containerMaxWidth)
                                .fillMaxSize()
                        ) {
                            OnboardingFavoritePeopleStep(
                                syncNotice = syncNotice,
                                onDismissSyncNotice = { viewModel.dismissOnboardingSyncNotice() },
                                selectedGenres = selectedGenres,
                                onToggleGenre = { genre -> viewModel.toggleOnboardingGenre(genre) },
                                query = personQuery,
                                onQueryChange = { viewModel.updateOnboardingPersonQuery(it) },
                                people = if (personQuery.isNotBlank()) personSearchResults else popularPeople,
                                favoritePeople = favoritePeople,
                                isLoading = isLoadingPeople,
                                onTogglePerson = { person -> viewModel.toggleOnboardingPerson(person) },
                                onRemoveFavoritePerson = { fav -> viewModel.removeFavoritePerson(fav) },
                                onBack = { viewModel.setOnboardingStep(0) },
                                onSkip = { viewModel.setOnboardingStep(2) },
                                onNext = { viewModel.setOnboardingStep(2) }
                            )
                        }
                    }

                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = containerMaxWidth)
                                .fillMaxSize()
                        ) {
                            OnboardingWatchedTitlesStep(
                                syncNotice = syncNotice,
                                onDismissSyncNotice = { viewModel.dismissOnboardingSyncNotice() },
                                query = titleQuery,
                                onQueryChange = { viewModel.updateOnboardingTitleQuery(it) },
                                titles = if (titleQuery.isNotBlank()) titleSearchResults else popularTitles,
                                allMedia = allMedia,
                                hasAnyTasteSignal = hasAnyPersonalizationSignal,
                                isLoading = isLoadingTitles,
                                onToggleFavorite = { media -> viewModel.toggleOnboardingTitleFavorite(media) },
                                onToggleStatus = { media, status ->
                                    viewModel.toggleOnboardingTitleStatus(media, status)
                                },
                                onBack = { viewModel.setOnboardingStep(1) },
                                onSkip = { viewModel.skipOnboarding() },
                                onFinish = { viewModel.completeOnboarding() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEP ${currentStep + 1} OF $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = CinemaGold
            )
            Text(
                text = when (currentStep) {
                    0 -> "Welcome"
                    1 -> "Genres & Stars"
                    else -> "Favorites"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (index in 0 until totalSteps) {
                val isCompleted = index < currentStep
                val isActive = index == currentStep
                val barColor by animateColorAsState(
                    targetValue = when {
                        isActive -> CinemaGold
                        isCompleted -> CinemaCyan
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(260),
                    label = "stepBarColor_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
internal fun OnboardingAmbientBackdrop(
    activeMedia: MediaEntity?,
    imageAlpha: Float,
    isInteractiveStep: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingBackdropMotion")
    val ambientScale by infiniteTransition.animateFloat(
        initialValue = 1.03f,
        targetValue = 1.11f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingAmbientScale"
    )
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(if (isInteractiveStep) 22.dp else 26.dp)
    } else {
        Modifier
    }

    Box(modifier = modifier.testTag("onboarding_ambient_backdrop")) {
        AnimatedContent(
            targetState = activeMedia,
            transitionSpec = {
                fadeIn(tween(850)) togetherWith fadeOut(tween(850))
            },
            label = "onboardingAmbientCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { media ->
            val ambientSource = media?.backdropUrl?.ifBlank { media.posterUrl }.orEmpty()
            if (ambientSource.isNotBlank()) {
                AsyncImage(
                    model = ambientSource,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(ambientScale)
                        .then(blurModifier)
                        .alpha(imageAlpha)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_cinema),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(ambientScale)
                        .then(blurModifier)
                        .alpha(imageAlpha * 0.92f)
                )
            }
        }

        // Scrim ensures comfortable readability while preserving vibrant cinema glow across all 3 onboarding steps
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isInteractiveStep) {
                            listOf(
                                Color(0x840A1119),
                                Color(0x9A0B0D13),
                                DarkBackground.copy(alpha = 0.86f)
                            )
                        } else {
                            listOf(
                                Color(0x880A1119),
                                Color(0x9E0B0D13),
                                DarkBackground.copy(alpha = 0.95f)
                            )
                        }
                    )
                )
        )

        if (isInteractiveStep) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CinemaGold.copy(alpha = 0.08f),
                                CinemaCyan.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingWelcomeStep(
    currentUser: FirebaseUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onContinueSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    showcaseTitles: List<MediaEntity> = emptyList(),
    sharedActiveIndex: Int? = null,
    onSelectShowcaseIndex: ((Int) -> Unit)? = null,
    showAmbientBackdrop: Boolean = true
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val resolvedShowcase = remember(showcaseTitles) {
        if (showcaseTitles.isNotEmpty()) {
            showcaseTitles
        } else {
            SampleMediaData.getInitialCatalog().take(8)
        }
    }

    var localActiveIndex by remember(resolvedShowcase.size) { mutableIntStateOf(0) }
    LaunchedEffect(resolvedShowcase.size, sharedActiveIndex == null) {
        if (sharedActiveIndex == null && resolvedShowcase.size > 1) {
            while (true) {
                delay(3200L)
                localActiveIndex = (localActiveIndex + 1) % resolvedShowcase.size
            }
        }
    }

    val rawIndex = sharedActiveIndex ?: localActiveIndex
    val safeIndex = if (resolvedShowcase.isNotEmpty()) rawIndex % resolvedShowcase.size else 0
    val activeMedia = resolvedShowcase.getOrNull(safeIndex)
    val selectIndex: (Int) -> Unit = onSelectShowcaseIndex ?: { localActiveIndex = it }

    val infiniteTransition = rememberInfiniteTransition(label = "welcomeAmbient")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeGlowPulse"
    )
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcomeAmbientDrift"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (showAmbientBackdrop) {
            OnboardingAmbientBackdrop(
                activeMedia = activeMedia,
                imageAlpha = 0.34f,
                isInteractiveStep = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (adaptive.useTwoPaneLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(0.86f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 420.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeHeadlineAndPills(isTablet = true)
                        Spacer(modifier = Modifier.height(20.dp))
                        WelcomeAuthActionsSection(
                            currentUser = currentUser,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onGoogleSignIn = onGoogleSignIn,
                            onContinueSignedIn = onContinueSignedIn,
                            onContinueAsGuest = onContinueAsGuest
                        )
                    }
                }

                WelcomeAmbientCinemaShowcase(
                    items = resolvedShowcase,
                    activeIndex = safeIndex,
                    onSelectIndex = selectIndex,
                    glowPulse = glowPulse,
                    driftOffset = driftOffset,
                    isTablet = true,
                    fillStage = true,
                    modifier = Modifier
                        .weight(1.14f)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                val fillStage = maxHeight >= 520.dp
                if (fillStage) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Welcome to WatchPilot + Top App Features
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp, bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WelcomeHeadlineAndPills(isTablet = adaptive.isTabletOrWide)
                        }

                        // 2. Giant borderless rotating title cards filling the hero space
                        WelcomeAmbientCinemaShowcase(
                            items = resolvedShowcase,
                            activeIndex = safeIndex,
                            onSelectIndex = selectIndex,
                            glowPulse = glowPulse,
                            driftOffset = driftOffset,
                            isTablet = adaptive.isTabletOrWide,
                            fillStage = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        // 3. Login buttons directly below title cards
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 8.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WelcomeAuthActionsSection(
                                currentUser = currentUser,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onGoogleSignIn = onGoogleSignIn,
                                onContinueSignedIn = onContinueSignedIn,
                                onContinueAsGuest = onContinueAsGuest
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WelcomeHeadlineAndPills(isTablet = adaptive.isTabletOrWide)
                        }

                        WelcomeAmbientCinemaShowcase(
                            items = resolvedShowcase,
                            activeIndex = safeIndex,
                            onSelectIndex = selectIndex,
                            glowPulse = glowPulse,
                            driftOffset = driftOffset,
                            isTablet = adaptive.isTabletOrWide,
                            fillStage = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WelcomeAuthActionsSection(
                                currentUser = currentUser,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onGoogleSignIn = onGoogleSignIn,
                                onContinueSignedIn = onContinueSignedIn,
                                onContinueAsGuest = onContinueAsGuest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeAmbientCinemaShowcase(
    items: List<MediaEntity>,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    glowPulse: Float,
    driftOffset: Float,
    isTablet: Boolean,
    fillStage: Boolean,
    modifier: Modifier = Modifier
) {
    val activeItem = items.getOrNull(activeIndex)
    val prevIndex = if (items.size > 1) (activeIndex - 1 + items.size) % items.size else null
    val nextIndex = if (items.size > 2) (activeIndex + 1) % items.size else null
    val sideBlur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(2.5.dp) else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("welcome_ambient_showcase")
    ) {
        // Subtle breathing radial cinema spotlight behind the rotating title posters
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(340.dp)
                .scale(glowPulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CinemaGold.copy(alpha = 0.20f),
                            CinemaCyan.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fillStage) Modifier.fillMaxHeight()
                    else Modifier.height(if (isTablet) 290.dp else 250.dp)
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val desiredCenterHeight = (maxHeight - 4.dp).coerceIn(210.dp, 440.dp)
            val maxAllowedCenterWidth = (maxWidth * 0.62f).coerceAtLeast(170.dp)
            val centerCardWidth = (desiredCenterHeight * (2f / 3f)).coerceAtMost(maxAllowedCenterWidth)
            val sideCardWidth = centerCardWidth * 0.78f
            val sideHorizontalSpread = maxWidth * 0.30f

            if (prevIndex != null) {
                val leftItem = items[prevIndex]
                WelcomeShowcasePosterCard(
                    media = leftItem,
                    isFocused = false,
                    onClick = { onSelectIndex(prevIndex) },
                    modifier = Modifier
                        .width(sideCardWidth)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            translationX = -sideHorizontalSpread.toPx() + driftOffset
                            rotationZ = -8f
                            scaleX = 0.94f
                            scaleY = 0.94f
                        }
                        .then(sideBlur)
                        .alpha(0.62f)
                )
            }

            if (nextIndex != null) {
                val rightItem = items[nextIndex]
                WelcomeShowcasePosterCard(
                    media = rightItem,
                    isFocused = false,
                    onClick = { onSelectIndex(nextIndex) },
                    modifier = Modifier
                        .width(sideCardWidth)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            translationX = sideHorizontalSpread.toPx() - driftOffset
                            rotationZ = 8f
                            scaleX = 0.94f
                            scaleY = 0.94f
                        }
                        .then(sideBlur)
                        .alpha(0.62f)
                )
            }

            if (activeItem != null) {
                AnimatedContent(
                    targetState = activeItem,
                    transitionSpec = {
                        fadeIn(tween(450)) togetherWith fadeOut(tween(350))
                    },
                    label = "welcomeCenterPoster"
                ) { centerMedia ->
                    WelcomeShowcasePosterCard(
                        media = centerMedia,
                        isFocused = true,
                        onClick = {
                            if (items.size > 1) {
                                onSelectIndex((activeIndex + 1) % items.size)
                            }
                        },
                        modifier = Modifier
                            .width(centerCardWidth)
                            .aspectRatio(2f / 3f)
                            .testTag("welcome_center_poster")
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeShowcasePosterCard(
    media: MediaEntity,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isFocused) BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)) else null,
        shadowElevation = if (isFocused) 20.dp else 4.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(34.dp)
            )
            val posterSource = media.posterUrl.ifBlank { media.backdropUrl }
            if (posterSource.isNotBlank()) {
                AsyncImage(
                    model = posterSource,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeHeadlineAndPills(
    isTablet: Boolean
) {
    Text(
        text = "Welcome to WatchPilot",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Your cinema guide. Personalized and synced.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    val featuresModifier = Modifier.testTag("welcome_features_row")
    if (isTablet) {
        FlowRow(
            modifier = featuresModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WelcomeFeaturePill(
                icon = Icons.Filled.AutoAwesome,
                label = "Top Picks",
                accent = CinemaGold
            )
            WelcomeFeaturePill(
                icon = Icons.Filled.LiveTv,
                label = "Streaming",
                accent = CinemaCyan
            )
            WelcomeFeaturePill(
                icon = Icons.Filled.CloudSync,
                label = "Cloud Sync",
                accent = CinemaGreen
            )
        }
    } else {
        Row(
            modifier = featuresModifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            WelcomeFeaturePill(
                icon = Icons.Filled.AutoAwesome,
                label = "Top Picks",
                accent = CinemaGold
            )
            WelcomeFeaturePill(
                icon = Icons.Filled.LiveTv,
                label = "Streaming",
                accent = CinemaCyan
            )
            WelcomeFeaturePill(
                icon = Icons.Filled.CloudSync,
                label = "Cloud Sync",
                accent = CinemaGreen
            )
        }
    }
}

@Composable
private fun WelcomeAuthActionsSection(
    currentUser: FirebaseUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onContinueSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (currentUser != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CinemaGreen.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CinemaGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Signed in",
                        tint = CinemaGreen
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Sync Active",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGreen
                        )
                        Text(
                            text = currentUser.email ?: currentUser.displayName ?: "Signed in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Button(
                onClick = onContinueSignedIn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = DarkBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_btn_continue_signed_in")
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        } else {
            Button(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = DarkBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_btn_google_signin")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Signing in...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    com.example.ui.components.GoogleSignInIcon(
                        modifier = Modifier.testTag("icon_google_onboarding_signin")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onContinueAsGuest,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_btn_guest")
            ) {
                Text(
                    text = "Continue as Guest",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "2 quick steps next",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeFeaturePill(
    icon: ImageVector,
    label: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun OnboardingBenefitRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.16f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingSyncBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = !message.isNullOrBlank()) {
        if (!message.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CinemaGreen.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CinemaGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = CinemaGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss sync notice",
                            tint = CinemaGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingFavoritePeopleStep(
    syncNotice: String?,
    onDismissSyncNotice: () -> Unit,
    selectedGenres: Set<String>,
    onToggleGenre: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    people: List<TmdbSearchResultItem.Person>,
    favoritePeople: List<FavoritePersonEntity>,
    isLoading: Boolean,
    onTogglePerson: (TmdbSearchResultItem.Person) -> Unit,
    onRemoveFavoritePerson: (FavoritePersonEntity) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val totalSelected = selectedGenres.size + favoritePeople.size

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OnboardingSyncBanner(message = syncNotice, onDismiss = onDismissSyncNotice)

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
                    Text(
                        text = "Genres & stars",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Shape your Top Picks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (totalSelected > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CinemaGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "$totalSelected picked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Favorite Genres / Vibes Quick Strip (FlowRow on tablet, LazyRow on phone)
            if (adaptive.isTabletOrWide) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ONBOARDING_GENRES.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleGenre(genre) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = genre,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CinemaCyan,
                                selectedLeadingIconColor = CinemaCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                selectedBorderColor = CinemaCyan.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("onboarding_genre_$genre")
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ONBOARDING_GENRES, key = { it }) { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleGenre(genre) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = genre,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CinemaCyan,
                                selectedLeadingIconColor = CinemaCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                selectedBorderColor = CinemaCyan.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("onboarding_genre_$genre")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "Search people",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    focusedContainerColor = Color(0xAA131A26),
                    unfocusedContainerColor = Color(0x99131A26)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_people_search")
            )

            if (favoritePeople.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(favoritePeople, key = { it.id }) { fav ->
                        FilterChip(
                            selected = true,
                            onClick = { onRemoveFavoritePerson(fav) },
                            label = {
                                Text(
                                    text = fav.name,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${fav.name}",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold.copy(alpha = 0.22f),
                                selectedLabelColor = CinemaGold,
                                selectedTrailingIconColor = CinemaGold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading && people.isEmpty()) {
                CircularProgressIndicator(
                    color = CinemaGold,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (people.isEmpty()) {
                Text(
                    text = "No people found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (adaptive.isTabletOrWide) 128.dp else 104.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (adaptive.isTabletOrWide) 14.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (adaptive.isTabletOrWide) 14.dp else 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = people,
                        key = { "${it.id}_${it.name}" }
                    ) { person ->
                        val isSelected = favoritePeople.any { fav ->
                            (person.id > 0L && fav.personId == person.id) ||
                                fav.name.equals(person.name, ignoreCase = true)
                        }
                        OnboardingPersonCard(
                            person = person,
                            isSelected = isSelected,
                            onClick = { onTogglePerson(person) }
                        )
                    }
                }
            }
        }

        OnboardingBottomActionBar(
            backTag = "onboarding_btn_back_people",
            skipTag = "onboarding_btn_skip_people",
            nextTag = "onboarding_btn_next_people",
            nextLabel = if (totalSelected > 0) "Next ($totalSelected)" else "Next",
            onBack = onBack,
            onSkip = onSkip,
            onNext = onNext
        )
    }
}

@Composable
private fun OnboardingPersonCard(
    person: TmdbSearchResultItem.Person,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "personScale_${person.id}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) CinemaGold.copy(alpha = 0.16f)
                else Color(0x88131A26)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CinemaGold else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("onboarding_person_${person.id}")
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) CinemaGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier.size(72.dp)
            ) {
                if (!person.profileUrl.isNullOrBlank()) {
                    val req = remember(person.profileUrl, context) {
                        ImageRequest.Builder(context)
                            .data(person.profileUrl)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = person.name,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = CinemaGold,
                    border = BorderStroke(1.5.dp, DarkBackground),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = DarkBackground,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = person.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CinemaGold else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = person.department.ifBlank { "Acting" },
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun OnboardingWatchedTitlesStep(
    syncNotice: String?,
    onDismissSyncNotice: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    titles: List<MediaEntity>,
    allMedia: List<MediaEntity>,
    hasAnyTasteSignal: Boolean,
    isLoading: Boolean,
    onToggleFavorite: (MediaEntity) -> Unit,
    onToggleStatus: (MediaEntity, WatchStatus) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val adaptive = LocalAdaptiveLayoutInfo.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedType by remember { mutableStateOf<MediaType?>(null) }

    val statusById = remember(allMedia) {
        allMedia.associate { it.id to it.getWatchStatusEnum() }
    }
    val favoriteIds = remember(allMedia) {
        allMedia.filter { it.isFavorite }.map { it.id }.toHashSet()
    }
    val favoriteCount = remember(allMedia) { allMedia.count { it.isFavorite } }
    val watchedCount = remember(allMedia) {
        allMedia.count { it.getWatchStatusEnum() == WatchStatus.COMPLETED }
    }
    val watchlistCount = remember(allMedia) {
        allMedia.count { it.getWatchStatusEnum() == WatchStatus.PLAN_TO_WATCH }
    }

    val displayedTitles = remember(titles, selectedType) {
        if (selectedType == null) titles
        else titles.filter { it.getMediaTypeEnum() == selectedType }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OnboardingSyncBanner(message = syncNotice, onDismiss = onDismissSyncNotice)

            Text(
                text = "Titles you love",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Heart favorites, mark watched, or save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "Search titles",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    focusedContainerColor = Color(0xAA131A26),
                    unfocusedContainerColor = Color(0x99131A26)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_titles_search")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All", maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = selectedType == MediaType.MOVIE,
                    onClick = {
                        selectedType = if (selectedType == MediaType.MOVIE) null else MediaType.MOVIE
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text("Movies", maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = selectedType == MediaType.TV_SHOW,
                    onClick = {
                        selectedType = if (selectedType == MediaType.TV_SHOW) null else MediaType.TV_SHOW
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text("Series", maxLines = 1, softWrap = false) }
                )

                if (favoriteCount > 0 || watchedCount > 0 || watchlistCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CinemaGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CinemaGreen.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = buildList {
                                if (favoriteCount > 0) add("$favoriteCount ♥")
                                if (watchedCount > 0) add("$watchedCount watched")
                                if (watchlistCount > 0) add("$watchlistCount saved")
                            }.joinToString(" • "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGreen,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading && displayedTitles.isEmpty()) {
                CircularProgressIndicator(
                    color = CinemaGold,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (displayedTitles.isEmpty()) {
                Text(
                    text = "No titles found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (adaptive.isTabletOrWide) 174.dp else 152.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (adaptive.isTabletOrWide) 14.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (adaptive.isTabletOrWide) 14.dp else 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedTitles,
                        key = { it.id }
                    ) { media ->
                        val status = statusById[media.id] ?: WatchStatus.NONE
                        val isFav = favoriteIds.contains(media.id)
                        OnboardingTitlePickerCard(
                            media = media,
                            currentStatus = status,
                            isFavorite = isFav,
                            onToggleFavorite = { onToggleFavorite(media) },
                            onMarkWatched = { onToggleStatus(media, WatchStatus.COMPLETED) },
                            onMarkWatchlist = { onToggleStatus(media, WatchStatus.PLAN_TO_WATCH) }
                        )
                    }
                }
            }
        }

        OnboardingBottomActionBar(
            backTag = "onboarding_btn_back_titles",
            skipTag = "onboarding_btn_skip_titles",
            nextTag = "onboarding_btn_finish",
            nextLabel = if (hasAnyTasteSignal) "Finish" else "Explore",
            onBack = onBack,
            onSkip = onSkip,
            onNext = onFinish
        )
    }
}

@Composable
private fun OnboardingTitlePickerCard(
    media: MediaEntity,
    currentStatus: WatchStatus,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onMarkWatched: () -> Unit,
    onMarkWatchlist: () -> Unit
) {
    val context = LocalContext.current
    val isWatched = currentStatus == WatchStatus.COMPLETED
    val isWatchlist = currentStatus == WatchStatus.PLAN_TO_WATCH
    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "heartScale_${media.id}"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x99131A26)),
        border = when {
            isFavorite -> BorderStroke(1.5.dp, CinemaRed)
            isWatched -> BorderStroke(1.5.dp, CinemaGreen)
            isWatchlist -> BorderStroke(1.5.dp, CinemaCyan)
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_title_card_${media.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onToggleFavorite)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                        modifier = Modifier.size(32.dp)
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
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Top-Start Type + Rating pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = if (media.getMediaTypeEnum() == MediaType.MOVIE) "Film" else "TV",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (media.getMediaTypeEnum() == MediaType.MOVIE) CinemaGold else CinemaCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(10.dp)
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

                // Top-End 48dp Favorite Heart Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("onboarding_title_fav_${media.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFavorite) CinemaRed.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            if (isFavorite) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .scale(heartScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) {
                                    "Remove ${media.title} from favorites"
                                } else {
                                    "Favorite ${media.title}"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${media.releaseYear} • ${media.genres.substringBefore(",").trim()}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Watched & Watchlist 48dp accessible toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onMarkWatched,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWatched) CinemaGreen else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_title_watched_${media.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Mark ${media.title} as watched",
                            tint = if (isWatched) DarkBackground else CinemaGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watched",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWatched) DarkBackground else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    onClick = onMarkWatchlist,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWatchlist) CinemaCyan else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_title_watchlist_${media.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                            contentDescription = "Add ${media.title} to watchlist",
                            tint = if (isWatchlist) DarkBackground else CinemaCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWatchlist) DarkBackground else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomActionBar(
    backTag: String,
    skipTag: String,
    nextTag: String,
    nextLabel: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = Color(0xB80A1119),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(backTag)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(skipTag)
                ) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = DarkBackground
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(nextTag)
            ) {
                Text(
                    text = nextLabel,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        }
    }
}
