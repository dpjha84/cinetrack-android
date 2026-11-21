package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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
}
