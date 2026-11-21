package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OnboardingPreferences
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.google.firebase.analytics.FirebaseAnalytics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalyticsTrackerTest {

    private lateinit var context: Context
    private val recordedEvents = mutableListOf<Pair<String, Map<String, String>>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OnboardingPreferences.setAnalyticsEnabled(context, true)
        AnalyticsTracker.init(context)
        recordedEvents.clear()
        AnalyticsTracker.testEventObserver = { name, params ->
            recordedEvents.add(name to params)
        }
    }

    @After
    fun tearDown() {
        AnalyticsTracker.testEventObserver = null
        OnboardingPreferences.setAnalyticsEnabled(context, true)
        AnalyticsTracker.init(context)
    }

    @Test
    fun logsAllFiveRequestedEventSuitesWithExpectedParameters() {
        AnalyticsTracker.logScreenView("home")
        AnalyticsTracker.logOnboardingStep(1, "onboarding_genres_stars")
        AnalyticsTracker.logSignInChoice(method = "google", isOnboarding = true)
        AnalyticsTracker.logTitleOpened("tmdb_m_123", "Inception", MediaType.MOVIE)
        AnalyticsTracker.logWatchStatusChanged("tmdb_m_123", MediaType.MOVIE, WatchStatus.COMPLETED, "detail")
        AnalyticsTracker.logSearch(query = "Dune Part Two", category = "MOVIES")
        AnalyticsTracker.logCatalogFilterChanged(MediaType.TV_SHOW, "Sci-Fi", "home")
        AnalyticsTracker.logAiConciergeResult(mood = "Mind-bending", timeBudget = "2h", picksCount = 3, success = true)
        AnalyticsTracker.logMatchRoomCreated(providersCount = 2)
        AnalyticsTracker.logMatchCardSwiped("tmdb_m_123", "MOVIE", liked = true, isMutualMatch = true)

        assertEquals(FirebaseAnalytics.Event.SCREEN_VIEW, recordedEvents[0].first)
        assertEquals("home", recordedEvents[0].second[FirebaseAnalytics.Param.SCREEN_NAME])

        assertEquals("onboarding_step", recordedEvents[1].first)
        assertEquals("onboarding_genres_stars", recordedEvents[1].second["step_name"])

        assertEquals(FirebaseAnalytics.Event.LOGIN, recordedEvents[2].first)
        assertEquals("google", recordedEvents[2].second[FirebaseAnalytics.Param.METHOD])

        assertEquals(FirebaseAnalytics.Event.SELECT_ITEM, recordedEvents[3].first)
        assertEquals("movie", recordedEvents[3].second[FirebaseAnalytics.Param.CONTENT_TYPE])

        assertEquals("title_watched", recordedEvents[4].first)
        assertEquals("tmdb_m_123", recordedEvents[4].second[FirebaseAnalytics.Param.ITEM_ID])

        assertEquals(FirebaseAnalytics.Event.SEARCH, recordedEvents[5].first)
        assertEquals("Dune Part Two", recordedEvents[5].second[FirebaseAnalytics.Param.SEARCH_TERM])

        assertEquals("catalog_filtered", recordedEvents[6].first)
        assertEquals("Sci-Fi", recordedEvents[6].second["genre"])

        assertEquals("ai_concierge_generated", recordedEvents[7].first)
        assertEquals("3", recordedEvents[7].second["picks_count"])

        assertEquals("match_room_created", recordedEvents[8].first)
        assertEquals("2", recordedEvents[8].second["providers_count"])

        assertEquals("match_card_swiped", recordedEvents[9].first)
        assertEquals("match_found", recordedEvents[10].first)
    }

    @Test
    fun respectsOptOutToggleAndPersistsPreference() {
        AnalyticsTracker.setCollectionEnabled(context, false)
        assertFalse(OnboardingPreferences.isAnalyticsEnabled(context))

        AnalyticsTracker.logScreenView("profile")
        AnalyticsTracker.logSearch("Matrix", "ALL")
        assertTrue(recordedEvents.isEmpty())

        AnalyticsTracker.setCollectionEnabled(context, true)
        assertTrue(OnboardingPreferences.isAnalyticsEnabled(context))

        AnalyticsTracker.logScreenView("profile")
        assertEquals(1, recordedEvents.size)
        assertEquals("profile", recordedEvents.first().second[FirebaseAnalytics.Param.SCREEN_NAME])
    }

    @Test
    fun sanitizesLongEventNamesAndParameterValues() {
        val longValue = "x".repeat(150)
        AnalyticsTracker.logSearch(query = longValue, category = "ALL")

        assertEquals(1, recordedEvents.size)
        val loggedQuery = recordedEvents.first().second[FirebaseAnalytics.Param.SEARCH_TERM].orEmpty()
        assertEquals(100, loggedQuery.length)
    }
}
