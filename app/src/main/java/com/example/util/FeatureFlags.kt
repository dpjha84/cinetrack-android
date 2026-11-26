package com.example.util

/**
 * Release-scoped feature toggles for WatchPilot.
 *
 * v5.1.0 ("Safe Features First"): Ships 100% deterministic, zero-AI features
 * (OTT Money-Saver Radar, On My Apps filter, Streaming Alerts, Live Swipe-to-Match,
 * Unlimited Collections, Cinema DNA, CSV/JSON Export, Ad-Free Pro).
 *
 * Gemini AI features (Snap Scanner, Tonight's AI Concierge, AI Companion Recap & Fit)
 * are preserved in the codebase and disabled until v5.2.0.
 */
object FeatureFlags {
    const val ENABLE_AI_FEATURES: Boolean = false
}
