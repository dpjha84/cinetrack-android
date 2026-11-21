package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.example.ui.theme.WatchPilotTheme
import com.example.ui.util.AdaptiveLayoutProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingConciseUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun peopleSearchPlaceholderRemainsCompactAndSingleLineOnPhoneWithFontScaling() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.2f)
            ) {
                WatchPilotTheme {
                    AdaptiveLayoutProvider {
                        Box(modifier = Modifier.width(340.dp)) {
                            OnboardingFavoritePeopleStep(
                                syncNotice = "Cloud synced — review or add picks",
                                onDismissSyncNotice = {},
                                selectedGenres = setOf("Crime", "Mystery"),
                                onToggleGenre = {},
                                query = "",
                                onQueryChange = {},
                                people = emptyList(),
                                favoritePeople = emptyList(),
                                isLoading = false,
                                onTogglePerson = {},
                                onRemoveFavoritePerson = {},
                                onBack = {},
                                onSkip = {},
                                onNext = {}
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Genres & stars").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shape your Top Picks.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search people").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_people_search").assertIsDisplayed()

        val searchBounds = composeTestRule
            .onNodeWithTag("onboarding_people_search")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Expected single-line compact search field (<= 72.dp), was ${searchBounds.height}",
            searchBounds.height <= 72.dp
        )
    }

    @Test
    fun titlesStepSearchAndCopyAreConciseAndCompact() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.2f)
            ) {
                WatchPilotTheme {
                    AdaptiveLayoutProvider {
                        Box(modifier = Modifier.width(340.dp)) {
                            OnboardingWatchedTitlesStep(
                                syncNotice = null,
                                onDismissSyncNotice = {},
                                query = "",
                                onQueryChange = {},
                                titles = emptyList(),
                                allMedia = emptyList(),
                                hasAnyTasteSignal = false,
                                isLoading = false,
                                onToggleFavorite = {},
                                onToggleStatus = { _, _ -> },
                                onBack = {},
                                onSkip = {},
                                onFinish = {}
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Titles you love").assertIsDisplayed()
        composeTestRule.onNodeWithText("Heart favorites, mark watched, or save.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search titles").assertIsDisplayed()

        val searchBounds = composeTestRule
            .onNodeWithTag("onboarding_titles_search")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Expected single-line compact title search field (<= 72.dp), was ${searchBounds.height}",
            searchBounds.height <= 72.dp
        )
    }

    @Test
    fun onboardingProgressBarUsesConciseStepLabels() {
        composeTestRule.setContent {
            WatchPilotTheme {
                OnboardingProgressBar(
                    currentStep = 1,
                    totalSteps = 3
                )
            }
        }

        composeTestRule.onNodeWithText("STEP 2 OF 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Genres & Stars").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h840dp-xhdpi")
    fun welcomeAmbientShowcaseRendersMoviesAndSeriesAboveGoogleSignIn() {
        val showcase = com.example.data.local.SampleMediaData.getInitialCatalog().take(4)
        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    OnboardingWelcomeStep(
                        currentUser = null,
                        isLoading = false,
                        errorMessage = null,
                        onGoogleSignIn = {},
                        onContinueSignedIn = {},
                        onContinueAsGuest = {},
                        showcaseTitles = showcase
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val welcomeNode = composeTestRule.onNodeWithText("Welcome to WatchPilot").assertIsDisplayed()
        val featuresNode = composeTestRule.onNodeWithTag("welcome_features_row").assertIsDisplayed()
        val showcaseNode = composeTestRule.onNodeWithTag("welcome_ambient_showcase").assertIsDisplayed()
        val centerPosterNode = composeTestRule.onNodeWithTag("welcome_center_poster").assertIsDisplayed()
        val googleBtnNode = composeTestRule.onNodeWithTag("onboarding_btn_google_signin").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_showcase_title")
            .assertIsDisplayed()
            .assertTextEquals(showcase[0].title)
        composeTestRule.onNodeWithTag("icon_google_onboarding_signin", useUnmergedTree = true).assertIsDisplayed()

        // Confirm low-level header and poster badges were removed for pure immersion
        composeTestRule.onNodeWithText("FILM").assertDoesNotExist()
        composeTestRule.onNodeWithText("SERIES").assertDoesNotExist()
        composeTestRule.onNodeWithText("WATCHPILOT").assertDoesNotExist()
        composeTestRule.onNodeWithText("TOP MOVIES").assertDoesNotExist()
        composeTestRule.onNodeWithText("TOP SERIES").assertDoesNotExist()

        // Assert strict visual hierarchy: Welcome -> Features -> Title Cards -> Sign-In
        val welcomeBounds = welcomeNode.getUnclippedBoundsInRoot()
        val featuresBounds = featuresNode.getUnclippedBoundsInRoot()
        val showcaseBounds = showcaseNode.getUnclippedBoundsInRoot()
        val posterBounds = centerPosterNode.getUnclippedBoundsInRoot()
        val googleBounds = googleBtnNode.getUnclippedBoundsInRoot()

        assertTrue(
            "Expected Welcome headline above feature pills",
            welcomeBounds.bottom <= featuresBounds.top + 4.dp
        )
        assertTrue(
            "Expected feature pills above title cards",
            featuresBounds.bottom <= showcaseBounds.top + 8.dp
        )
        assertTrue(
            "Expected title cards above Continue with Google button",
            showcaseBounds.bottom <= googleBounds.top + 4.dp
        )
        assertTrue(
            "Expected full-width showcase (>= 410.dp), was ${showcaseBounds.width}",
            showcaseBounds.width >= 410.dp
        )
        assertTrue(
            "Expected large center poster (width >= 180.dp), was ${posterBounds.width}",
            posterBounds.width >= 180.dp
        )
        val gapAboveButton = googleBounds.top - showcaseBounds.bottom
        assertTrue(
            "Expected no empty space above Continue with Google (<= 24.dp), was $gapAboveButton",
            gapAboveButton <= 24.dp
        )

        // Advance past 3.2s auto-rotation interval and confirm spotlight advances
        composeTestRule.mainClock.advanceTimeBy(3400L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_showcase_title")
            .assertIsDisplayed()
            .assertTextEquals(showcase[1].title)
    }

    @Test
    fun ambientBackdropRemainsVisibleAcrossInteractiveOnboardingSteps() {
        val sampleMedia = com.example.data.local.SampleMediaData.getInitialCatalog().first()
        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    Box {
                        OnboardingAmbientBackdrop(
                            activeMedia = sampleMedia,
                            imageAlpha = 0.30f,
                            isInteractiveStep = true
                        )
                        OnboardingFavoritePeopleStep(
                            syncNotice = null,
                            onDismissSyncNotice = {},
                            selectedGenres = emptySet(),
                            onToggleGenre = {},
                            query = "",
                            onQueryChange = {},
                            people = emptyList(),
                            favoritePeople = emptyList(),
                            isLoading = false,
                            onTogglePerson = {},
                            onRemoveFavoritePerson = {},
                            onBack = {},
                            onSkip = {},
                            onNext = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_ambient_backdrop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Genres & stars").assertIsDisplayed()
    }
}
