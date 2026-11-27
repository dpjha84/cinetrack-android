package com.example.data.local

import android.content.Context
import com.example.util.RateAppEngagementPolicy
import com.example.util.RateAppPromptState

object OnboardingPreferences {
    private const val PREFS_NAME = "watchpilot_onboarding_prefs"
    private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    private const val KEY_SELECTED_GENRES = "onboarding_selected_genres"
    private const val KEY_ANALYTICS_ENABLED = "analytics_collection_enabled"
    private const val KEY_DARK_THEME = "dark_theme_enabled"
    private const val KEY_RATE_WATCHED_COUNT = "rate_prompt_watched_count"
    private const val KEY_RATE_NEXT_THRESHOLD = "rate_prompt_next_threshold"
    private const val KEY_RATE_DISMISS_COUNT = "rate_prompt_dismiss_count"
    private const val KEY_RATE_HAS_RATED = "rate_prompt_has_rated"
    private const val KEY_RATE_NEVER_ASK = "rate_prompt_never_ask"
    private const val KEY_DISMISSED_GUEST_SAVE_BANNER = "dismissed_guest_save_banner"

    fun hasCompletedOnboarding(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
    }

    fun setCompletedOnboarding(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed)
            .apply()
    }

    fun getSelectedGenres(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED_GENRES, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun setSelectedGenres(context: Context, genres: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_GENRES, genres.toSet())
            .apply()
    }

    fun isAnalyticsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ANALYTICS_ENABLED, true)
    }

    fun setAnalyticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ANALYTICS_ENABLED, enabled)
            .apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_THEME, enabled)
            .apply()
    }

    fun getRateAppPromptState(context: Context): RateAppPromptState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return RateAppPromptState(
            watchedCount = prefs.getInt(KEY_RATE_WATCHED_COUNT, 0),
            nextTriggerThreshold = prefs.getInt(
                KEY_RATE_NEXT_THRESHOLD,
                RateAppEngagementPolicy.DEFAULT_TRIGGER_THRESHOLD
            ),
            dismissCount = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0),
            hasRatedApp = prefs.getBoolean(KEY_RATE_HAS_RATED, false),
            neverAskAgain = prefs.getBoolean(KEY_RATE_NEVER_ASK, false)
        )
    }

    fun saveRateAppPromptState(context: Context, state: RateAppPromptState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_RATE_WATCHED_COUNT, state.watchedCount)
            .putInt(KEY_RATE_NEXT_THRESHOLD, state.nextTriggerThreshold)
            .putInt(KEY_RATE_DISMISS_COUNT, state.dismissCount)
            .putBoolean(KEY_RATE_HAS_RATED, state.hasRatedApp)
            .putBoolean(KEY_RATE_NEVER_ASK, state.neverAskAgain)
            .apply()
    }

    fun hasDismissedGuestSaveBanner(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISMISSED_GUEST_SAVE_BANNER, false)
    }

    fun setDismissedGuestSaveBanner(context: Context, dismissed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISMISSED_GUEST_SAVE_BANNER, dismissed)
            .apply()
    }

    fun shouldShowGuestSaveBanner(isSignedIn: Boolean, isDismissed: Boolean): Boolean {
        return !isSignedIn && !isDismissed
    }
}
