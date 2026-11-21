package com.example.data.local

import android.content.Context

object OnboardingPreferences {
    private const val PREFS_NAME = "watchpilot_onboarding_prefs"
    private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    private const val KEY_SELECTED_GENRES = "onboarding_selected_genres"
    private const val KEY_ANALYTICS_ENABLED = "analytics_collection_enabled"

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
}
