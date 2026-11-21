package com.example.util

import android.content.Context
import android.os.Bundle
import com.example.data.local.OnboardingPreferences
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

/**
 * Centralized Firebase Analytics instrumentation for WatchPilot.
 * Uses the configured Firebase project (`movieverse-dd226`) via Google Services
 * without emitting extra Logcat noise in debug builds (verify live events via Firebase DebugView).
 */
object AnalyticsTracker {

    @Volatile
    private var analyticsEnabled: Boolean = true

    /**
     * Optional test-only sink so JVM/Robolectric unit tests can inspect logged events
     * without requiring Google Play Services measurement binding.
     */
    @Volatile
    internal var testEventObserver: ((eventName: String, params: Map<String, String>) -> Unit)? = null

    fun init(context: Context) {
        val enabled = OnboardingPreferences.isAnalyticsEnabled(context)
        analyticsEnabled = enabled
        applyCollectionEnabled(enabled)
    }

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        OnboardingPreferences.setAnalyticsEnabled(context, enabled)
        analyticsEnabled = enabled
        applyCollectionEnabled(enabled)
    }

    private fun applyCollectionEnabled(enabled: Boolean) {
        runCatching {
            Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

    fun setAuthModeUserProperty(isSignedIn: Boolean) {
        if (!analyticsEnabled) return
        val mode = if (isSignedIn) "google" else "guest"
        runCatching {
            Firebase.analytics.setUserProperty("auth_mode", mode)
        }
    }

    fun setRegionUserProperty(regionCode: String) {
        if (!analyticsEnabled) return
        val clean = sanitizeParamValue(regionCode).ifBlank { "US" }
        runCatching {
            Firebase.analytics.setUserProperty("region_code", clean)
        }
    }

    // =========================================================================
    // 1. Screen Views & Navigation
    // =========================================================================

    fun logScreenView(screenName: String, screenClass: String = "WatchPilotCompose") {
        logEvent(
            name = FirebaseAnalytics.Event.SCREEN_VIEW,
            params = mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenClass
            )
        )
    }

    // =========================================================================
    // 2. Onboarding Steps & Sign-In Choices
    // =========================================================================

    fun logOnboardingBegin() {
        logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN)
    }

    fun logOnboardingStep(stepIndex: Int, stepName: String) {
        logEvent(
            name = "onboarding_step",
            params = mapOf(
                "step_index" to stepIndex.toString(),
                "step_name" to stepName
            )
        )
    }

    fun logOnboardingComplete(genresCount: Int, starsCount: Int, authMode: String) {
        logEvent(
            name = FirebaseAnalytics.Event.TUTORIAL_COMPLETE,
            params = mapOf(
                "genres_count" to genresCount.toString(),
                "stars_count" to starsCount.toString(),
                "auth_mode" to authMode
            )
        )
    }

    fun logSignInChoice(method: String, isOnboarding: Boolean) {
        if (method == "google") {
            logEvent(
                name = FirebaseAnalytics.Event.LOGIN,
                params = mapOf(
                    FirebaseAnalytics.Param.METHOD to "google",
                    "entry_point" to if (isOnboarding) "onboarding" else "app"
                )
            )
        } else {
            logEvent(
                name = "auth_guest_continue",
                params = mapOf(
                    "entry_point" to if (isOnboarding) "onboarding" else "app"
                )
            )
        }
    }

    // =========================================================================
    // 3. Title Opens, Watchlist & Watched Saves
    // =========================================================================

    fun logTitleOpened(mediaId: String, title: String, mediaType: MediaType) {
        logEvent(
            name = FirebaseAnalytics.Event.SELECT_ITEM,
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to mediaId,
                FirebaseAnalytics.Param.ITEM_NAME to title,
                FirebaseAnalytics.Param.CONTENT_TYPE to if (mediaType == MediaType.TV_SHOW) "tv_show" else "movie"
            )
        )
    }

    fun logPersonOpened(personId: Long, name: String) {
        logEvent(
            name = FirebaseAnalytics.Event.SELECT_ITEM,
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to "person_$personId",
                FirebaseAnalytics.Param.ITEM_NAME to name,
                FirebaseAnalytics.Param.CONTENT_TYPE to "person"
            )
        )
    }

    fun logWatchStatusChanged(
        mediaId: String,
        mediaType: MediaType,
        status: WatchStatus,
        source: String = "detail"
    ) {
        val eventName = when (status) {
            WatchStatus.PLAN_TO_WATCH -> "watchlist_added"
            WatchStatus.COMPLETED -> "title_watched"
            WatchStatus.NONE -> "watchlist_removed"
            else -> "media_status_updated"
        }
        logEvent(
            name = eventName,
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to mediaId,
                FirebaseAnalytics.Param.CONTENT_TYPE to if (mediaType == MediaType.TV_SHOW) "tv_show" else "movie",
                "watch_status" to status.name.lowercase(),
                "source" to source
            )
        )
    }

    fun logFavoriteToggled(mediaId: String, mediaType: MediaType, isFavorite: Boolean) {
        logEvent(
            name = "favorite_toggled",
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to mediaId,
                FirebaseAnalytics.Param.CONTENT_TYPE to if (mediaType == MediaType.TV_SHOW) "tv_show" else "movie",
                "is_favorite" to isFavorite.toString()
            )
        )
    }

    fun logEpisodeLogged(mediaId: String, seasonNumber: Int, episodeNumber: Int, watched: Boolean) {
        logEvent(
            name = "episode_logged",
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to mediaId,
                "season" to seasonNumber.toString(),
                "episode" to episodeNumber.toString(),
                "watched" to watched.toString()
            )
        )
    }

    // =========================================================================
    // 4. Search Queries, Filters & AI Picks
    // =========================================================================

    fun logSearch(query: String, category: String = "all", genre: String? = null) {
        val cleanQuery = sanitizeParamValue(query)
        val params = mutableMapOf(
            FirebaseAnalytics.Param.SEARCH_TERM to cleanQuery,
            "search_category" to category.lowercase()
        )
        if (!genre.isNullOrBlank()) {
            params["genre"] = genre
        }
        logEvent(FirebaseAnalytics.Event.SEARCH, params)
    }

    fun logCatalogFilterChanged(typeFilter: MediaType?, genreFilter: String?, surface: String) {
        logEvent(
            name = "catalog_filtered",
            params = mapOf(
                "media_type" to when (typeFilter) {
                    MediaType.MOVIE -> "movie"
                    MediaType.TV_SHOW -> "tv_show"
                    null -> "all"
                },
                "genre" to (genreFilter?.takeIf { it.isNotBlank() } ?: "all"),
                "surface" to surface
            )
        )
    }

    fun logAiConciergeResult(mood: String, timeBudget: String, picksCount: Int, success: Boolean) {
        logEvent(
            name = "ai_concierge_generated",
            params = mapOf(
                "mood" to mood,
                "time_budget" to timeBudget,
                "picks_count" to picksCount.toString(),
                "success" to success.toString()
            )
        )
    }

    fun logSnapScanResult(itemsCount: Int, success: Boolean) {
        logEvent(
            name = "snap_scan_completed",
            params = mapOf(
                "items_count" to itemsCount.toString(),
                "success" to success.toString()
            )
        )
    }

    // =========================================================================
    // 5. Swipe-to-Match Rooms & Matches
    // =========================================================================

    fun logMatchRoomCreated(providersCount: Int) {
        logEvent(
            name = "match_room_created",
            params = mapOf("providers_count" to providersCount.toString())
        )
    }

    fun logMatchRoomJoined() {
        logEvent("match_room_joined")
    }

    fun logMatchCardSwiped(
        mediaId: String,
        mediaType: String,
        liked: Boolean,
        isMutualMatch: Boolean
    ) {
        logEvent(
            name = "match_card_swiped",
            params = mapOf(
                FirebaseAnalytics.Param.ITEM_ID to mediaId,
                FirebaseAnalytics.Param.CONTENT_TYPE to mediaType.lowercase(),
                "liked" to liked.toString(),
                "mutual_match" to isMutualMatch.toString()
            )
        )
        if (isMutualMatch) {
            logEvent(
                name = "match_found",
                params = mapOf(
                    FirebaseAnalytics.Param.ITEM_ID to mediaId,
                    FirebaseAnalytics.Param.CONTENT_TYPE to mediaType.lowercase()
                )
            )
        }
    }

    // =========================================================================
    // Internal Dispatcher & Parameter Sanitization
    // =========================================================================

    internal fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        if (!analyticsEnabled) return
        val safeName = sanitizeEventName(name)
        if (safeName.isBlank()) return

        val sanitizedMap = buildMap {
            params.entries.take(25).forEach { (rawKey, rawValue) ->
                val cleanKey = sanitizeParamKey(rawKey)
                val cleanVal = sanitizeParamValue(rawValue)
                if (cleanKey.isNotBlank() && cleanVal.isNotBlank()) {
                    put(cleanKey, cleanVal)
                }
            }
        }

        testEventObserver?.invoke(safeName, sanitizedMap)

        runCatching {
            val bundle = Bundle().apply {
                sanitizedMap.forEach { (k, v) -> putString(k, v) }
            }
            Firebase.analytics.logEvent(safeName, bundle)
        }
    }

    private fun sanitizeEventName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        return cleaned.take(40)
    }

    private fun sanitizeParamKey(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        return cleaned.take(40)
    }

    private fun sanitizeParamValue(raw: String): String {
        return raw.trim().take(100)
    }
}
