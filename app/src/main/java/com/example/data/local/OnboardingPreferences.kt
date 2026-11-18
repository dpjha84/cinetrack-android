package com.example.data.local

import android.content.Context

object OnboardingPreferences {
    private const val PREFS_NAME = "watchpilot_onboarding_prefs"
    private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

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
}
