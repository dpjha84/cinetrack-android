package com.example.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.MediaEntity
import com.example.data.model.HomeCatalogSection
import com.example.data.model.MediaType
import com.example.ui.components.GoogleSignInIcon
import com.example.ui.theme.WatchPilotTheme
import com.example.ui.util.AdaptiveLayoutProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSectionGridScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleItem(id: String, title: String, type: MediaType): MediaEntity {
        return MediaEntity(
            id = id,
            title = title,
            mediaType = type.name,
            releaseYear = 2025,
            genres = "Action, Sci-Fi",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.4f,
            runtimeMinutes = 120,
            feedTags = HomeCatalogSection.NOW_PLAYING.feedTag
        )
    }

    @Test
    fun sectionGridRendersItemsHandlesFiltersAndBack() {
        val sampleItems = (1..6).map { idx ->
            sampleItem(
                id = "item_$idx",
                title = "Cinema Title $idx",
                type = if (idx % 2 == 0) MediaType.TV_SHOW else MediaType.MOVIE
            )
        }
        var selectedType: MediaType? = null
        var backClicked = false
        var loadMoreClicked = false
        var clickedMediaId: String? = null

        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    HomeSectionGridScreen(
                        section = HomeCatalogSection.NOW_PLAYING,
                        regionName = "India",
                        items = sampleItems,
                        totalAvailableCount = 6,
                        selectedType = selectedType,
                        selectedGenre = null,
                        availableGenres = listOf("Action", "Sci-Fi"),
                        isLoadingMore = false,
                        hasMore = true,
                        onSelectType = { selectedType = it },
                        onSelectGenre = {},
                        onLoadMore = { loadMoreClicked = true },
                        onMediaClick = { clickedMediaId = it },
                        onBack = { backClicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Now Playing in India").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_now_playing_card_item_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_now_playing_card_item_3").assertIsDisplayed()

        composeTestRule.onNodeWithTag("section_filter_movies").performClick()
        assertEquals(MediaType.MOVIE, selectedType)

        composeTestRule.onNodeWithTag("grid_now_playing_card_item_2").performClick()
        assertEquals("item_2", clickedMediaId)

        composeTestRule.onNodeWithTag("section_grid_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun popularPeopleGridRendersRoundedCardsHandlesClickLoadMoreAndBack() {
        val samplePeople = (1..6).map { idx ->
            com.example.data.model.TmdbSearchResultItem.Person(
                id = idx.toLong(),
                name = "Actor Star $idx",
                profileUrl = null,
                department = if (idx % 2 == 0) "Directing" else "Acting",
                knownFor = emptyList(),
                popularity = 90.0 - idx
            )
        }
        var clickedPerson: com.example.data.model.TmdbSearchResultItem.Person? = null
        var backClicked = false
        var selectedType: MediaType? = null
        var loadMoreCount = 0

        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    HomeSectionGridScreen(
                        section = HomeCatalogSection.POPULAR_PEOPLE,
                        regionName = "India",
                        items = emptyList(),
                        totalAvailableCount = 6,
                        selectedType = selectedType,
                        selectedGenre = null,
                        availableGenres = listOf("Action", "Drama"),
                        isLoadingMore = false,
                        hasMore = true,
                        onSelectType = { selectedType = it },
                        onSelectGenre = {},
                        onLoadMore = { loadMoreCount++ },
                        onMediaClick = {},
                        onBack = { backClicked = true },
                        people = samplePeople,
                        onPersonClick = { clickedPerson = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Popular People").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_popular_people_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grid_popular_people_card_3").assertIsDisplayed()

        composeTestRule.onNodeWithTag("grid_popular_people_card_2").performClick()
        assertEquals(2L, clickedPerson?.id)
        assertEquals("Actor Star 2", clickedPerson?.name)

        composeTestRule.onNodeWithTag("section_filter_tv").performClick()
        assertEquals(MediaType.TV_SHOW, selectedType)

        composeTestRule.onNodeWithTag("section_grid_list_popular_people")
            .performScrollToNode(hasTestTag("section_grid_load_more"))
        composeTestRule.onNodeWithTag("section_grid_load_more").performClick()
        assertTrue(loadMoreCount >= 1)

        composeTestRule.onNodeWithTag("section_grid_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun googleSignInIconRendersOnLoginButton() {
        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    Button(onClick = {}) {
                        GoogleSignInIcon(
                            modifier = Modifier.testTag("icon_google_signin")
                        )
                        Text("Sign in with Google")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("icon_google_signin", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun preFilteredMovieWithSpecificGenreHidesBothTypeAndGenreFilters() {
        val movieItem = sampleItem("m1", "Only Movie", MediaType.MOVIE)
        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    HomeSectionGridScreen(
                        section = HomeCatalogSection.NOW_PLAYING,
                        regionName = "United States",
                        items = listOf(movieItem),
                        totalAvailableCount = 1,
                        selectedType = MediaType.MOVIE,
                        selectedGenre = "Action",
                        availableGenres = listOf("Action", "Drama"),
                        isLoadingMore = false,
                        hasMore = false,
                        onSelectType = {},
                        onSelectGenre = {},
                        onLoadMore = {},
                        onMediaClick = {},
                        onBack = {},
                        isFilterLocked = true,
                        isGenreFilterLocked = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Now Playing Movies in United States").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_filter_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("section_filter_movies").assertDoesNotExist()
        composeTestRule.onNodeWithTag("section_filter_tv").assertDoesNotExist()
        composeTestRule.onNodeWithTag("section_genre_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("grid_now_playing_card_m1").assertIsDisplayed()
    }

    @Test
    fun preFilteredShowsWithAllGenresShowsGenreFiltersAndHidesMediaTypeFilters() {
        val showItem = sampleItem("tv1", "Only Show", MediaType.TV_SHOW)
        var pickedGenre: String? = null
        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    HomeSectionGridScreen(
                        section = HomeCatalogSection.NOW_PLAYING,
                        regionName = "United States",
                        items = listOf(showItem),
                        totalAvailableCount = 1,
                        selectedType = MediaType.TV_SHOW,
                        selectedGenre = pickedGenre,
                        availableGenres = listOf("Drama", "Sci-Fi"),
                        isLoadingMore = false,
                        hasMore = false,
                        onSelectType = {},
                        onSelectGenre = { pickedGenre = it },
                        onLoadMore = {},
                        onMediaClick = {},
                        onBack = {},
                        isFilterLocked = true,
                        isGenreFilterLocked = false
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Now Playing Shows in United States").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_filter_all").assertDoesNotExist()
        composeTestRule.onNodeWithTag("section_filter_tv").assertDoesNotExist()
        composeTestRule.onNodeWithTag("section_genre_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_genre_drama").performClick()
        assertEquals("Drama", pickedGenre)
        composeTestRule.onNodeWithTag("grid_now_playing_card_tv1").assertIsDisplayed()
    }

    @Test
    fun paginationFooterNeverShowsLoadingMoreAndLoadMoreTogetherAndStopsLooping() {
        val sampleItems = (1..12).map { idx ->
            sampleItem(id = "np_$idx", title = "Now Playing Movie $idx", type = MediaType.MOVIE)
        }
        var loadMoreTriggerCount = 0
        var isLoadingState = false

        composeTestRule.setContent {
            WatchPilotTheme {
                AdaptiveLayoutProvider {
                    HomeSectionGridScreen(
                        section = HomeCatalogSection.NOW_PLAYING,
                        regionName = "United States",
                        items = sampleItems,
                        totalAvailableCount = 12,
                        selectedType = MediaType.MOVIE,
                        selectedGenre = null,
                        availableGenres = listOf("Action"),
                        isLoadingMore = isLoadingState,
                        hasMore = true,
                        onSelectType = {},
                        onSelectGenre = {},
                        onLoadMore = { loadMoreTriggerCount++ },
                        onMediaClick = {},
                        onBack = {},
                        isFilterLocked = true,
                        isGenreFilterLocked = false
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("section_grid_list_now_playing")
            .performScrollToNode(hasTestTag("section_grid_load_more"))
        composeTestRule.waitForIdle()
        // When idle and hasMore = true, only Load More shows and Loading more is absent
        composeTestRule.onNodeWithTag("section_grid_load_more").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_grid_loading_more").assertDoesNotExist()
        // Scroll near bottom auto-triggered at most once for count=12 without infinite looping
        assertTrue(
            "Expected auto-load to trigger at most once for stagnant count=12, got $loadMoreTriggerCount",
            loadMoreTriggerCount <= 1
        )
    }
}
