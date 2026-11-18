package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.viewmodel.MediaViewModel
import com.google.firebase.auth.FirebaseUser

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

    if (step > 0) {
        BackHandler {
            viewModel.setOnboardingStep(step - 1)
        }
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
            .statusBarsPadding()
            .testTag("onboarding_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // Segmented 3-Step Indicator
            OnboardingProgressBar(
                currentStep = step,
                totalSteps = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut())
                },
                label = "onboardingStepTransition",
                modifier = Modifier.weight(1f)
            ) { activeStep ->
                when (activeStep) {
                    0 -> OnboardingWelcomeStep(
                        currentUser = currentUser,
                        isLoading = authLoading,
                        errorMessage = authError,
                        onGoogleSignIn = {
                            if (activity != null) {
                                viewModel.signInWithGoogleDuringOnboarding(activity)
                            }
                        },
                        onContinueSignedIn = { viewModel.advanceFromOnboardingWelcome() },
                        onContinueAsGuest = { viewModel.continueAsGuestDuringOnboarding() }
                    )

                    1 -> OnboardingFavoritePeopleStep(
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

                    else -> OnboardingWatchedTitlesStep(
                        query = titleQuery,
                        onQueryChange = { viewModel.updateOnboardingTitleQuery(it) },
                        titles = if (titleQuery.isNotBlank()) titleSearchResults else popularTitles,
                        allMedia = allMedia,
                        isLoading = isLoadingTitles,
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

@Composable
private fun OnboardingProgressBar(
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
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CinemaGold
            )
            Text(
                text = when (currentStep) {
                    0 -> "Welcome & Sync"
                    1 -> "Actors & Directors"
                    else -> "Watched & Watchlist"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (index in 0 until totalSteps) {
                val isCompletedOrActive = index <= currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isCompletedOrActive) CinemaGold
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(
    currentUser: FirebaseUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onContinueSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Titanium Aperture Emblem with soft CinemaGold/CinemaCyan spotlight
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CinemaGold.copy(alpha = 0.20f),
                                    CinemaCyan.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0A1119),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(CinemaGold, CinemaCyan))
                    ),
                    shadowElevation = 14.dp,
                    modifier = Modifier.size(104.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to WatchPilot",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Track movies & series, check where to stream, and get instant picks tuned to your taste.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Cloud Sync Benefit Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ),
                border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CinemaGold.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "RECOMMENDED • SYNC ACROSS DEVICES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = CinemaGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    OnboardingBenefitRow(
                        icon = Icons.Filled.CloudSync,
                        accent = CinemaGold,
                        title = "Automatic Cloud Backup",
                        subtitle = "Your watchlist, watched episodes, and Cinema DNA stay backed up safely."
                    )
                    OnboardingBenefitRow(
                        icon = Icons.Filled.Devices,
                        accent = CinemaCyan,
                        title = "Seamless Multi-Device Access",
                        subtitle = "Sign in on any phone or tablet and restore your full library instantly."
                    )
                    OnboardingBenefitRow(
                        icon = Icons.Filled.AutoAwesome,
                        accent = CinemaGreen,
                        title = "Smarter Home Recommendations",
                        subtitle = "Your favorite stars and watched picks follow your account everywhere."
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 8.dp),
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
                            contentDescription = null,
                            tint = CinemaGreen
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Cloud Sync Active",
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
                        .height(54.dp)
                        .testTag("onboarding_btn_continue_signed_in")
                ) {
                    Text(
                        text = "Personalize Your Feed",
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
                        .height(54.dp)
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
                            text = "Signing in & syncing...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
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
                        .height(50.dp)
                        .testTag("onboarding_btn_guest")
                ) {
                    Text(
                        text = "Continue as Guest",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Guest data stays on this device only. You can sign in anytime in Profile.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
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
private fun OnboardingFavoritePeopleStep(
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
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Favorite actors & directors",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pick stars & creators you love to tune your recommendations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (favoritePeople.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CinemaGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${favoritePeople.size} picked",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search actors or directors (e.g. Nolan, Zendaya)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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

        Spacer(modifier = Modifier.height(8.dp))

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
                    text = "No matching actors or directors found. Try another name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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
            nextLabel = if (favoritePeople.isNotEmpty()) "Next (${favoritePeople.size})" else "Next",
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) CinemaGold.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CinemaGold else Color.Transparent,
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
                    width = if (isSelected) 2.dp else 1.dp,
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
private fun OnboardingWatchedTitlesStep(
    query: String,
    onQueryChange: (String) -> Unit,
    titles: List<MediaEntity>,
    allMedia: List<MediaEntity>,
    isLoading: Boolean,
    onToggleStatus: (MediaEntity, WatchStatus) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    var selectedType by remember { mutableStateOf<MediaType?>(null) }

    val statusById = remember(allMedia) {
        allMedia.associate { it.id to it.getWatchStatusEnum() }
    }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Movies & series you know",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Mark titles you've watched or add them to your watchlist to personalize Home & Cinema DNA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search movies or series...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                    label = { Text("All Mix") }
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
                    label = { Text("Movies") }
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
                    label = { Text("Series") }
                )

                if (watchedCount > 0 || watchlistCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CinemaGreen.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "$watchedCount watched • $watchlistCount saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                    text = "No matching movies or series found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedTitles,
                        key = { it.id }
                    ) { media ->
                        val status = statusById[media.id] ?: WatchStatus.NONE
                        OnboardingTitlePickerCard(
                            media = media,
                            currentStatus = status,
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
            nextLabel = "Finish & Personalize",
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
    onMarkWatched: () -> Unit,
    onMarkWatchlist: () -> Unit
) {
    val context = LocalContext.current
    val isWatched = currentStatus == WatchStatus.COMPLETED
    val isWatchlist = currentStatus == WatchStatus.PLAN_TO_WATCH

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = when {
            isWatched -> BorderStroke(1.5.dp, CinemaGreen)
            isWatchlist -> BorderStroke(1.5.dp, CinemaCyan)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
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
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                        color = Color.White.copy(alpha = 0.78f),
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
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
