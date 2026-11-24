package com.example.util

data class RateAppPromptState(
    val watchedCount: Int = 0,
    val nextTriggerThreshold: Int = RateAppEngagementPolicy.DEFAULT_TRIGGER_THRESHOLD,
    val dismissCount: Int = 0,
    val hasRatedApp: Boolean = false,
    val neverAskAgain: Boolean = false
)

/**
 * Pure Kotlin engagement milestone policy for the 'Rate this App' prompt.
 * Triggers once the user marks 5 movies or episodes as watched, with respectful
 * snooze spacing if dismissed.
 */
object RateAppEngagementPolicy {
    const val DEFAULT_TRIGGER_THRESHOLD = 5
    const val SNOOZE_WATCHES_STEP = 10
    const val MAX_SNOOZES_BEFORE_STOP = 2

    fun recordWatched(
        state: RateAppPromptState,
        delta: Int = 1,
        alreadyPromptedThisSession: Boolean = false
    ): Pair<RateAppPromptState, Boolean> {
        val safeDelta = delta.coerceAtLeast(0)
        if (safeDelta == 0) return state to false

        val nextCount = (state.watchedCount + safeDelta).coerceAtLeast(0)
        val updated = state.copy(watchedCount = nextCount)

        val shouldTrigger = !alreadyPromptedThisSession &&
            !updated.hasRatedApp &&
            !updated.neverAskAgain &&
            nextCount >= updated.nextTriggerThreshold

        return updated to shouldTrigger
    }

    fun onRateNow(state: RateAppPromptState): RateAppPromptState {
        return state.copy(hasRatedApp = true)
    }

    fun onMaybeLater(state: RateAppPromptState): RateAppPromptState {
        val nextDismissCount = state.dismissCount + 1
        return if (nextDismissCount >= MAX_SNOOZES_BEFORE_STOP) {
            state.copy(
                dismissCount = nextDismissCount,
                neverAskAgain = true
            )
        } else {
            state.copy(
                dismissCount = nextDismissCount,
                nextTriggerThreshold = maxOf(
                    state.nextTriggerThreshold,
                    state.watchedCount + SNOOZE_WATCHES_STEP
                )
            )
        }
    }

    fun onNeverAskAgain(state: RateAppPromptState): RateAppPromptState {
        return state.copy(neverAskAgain = true)
    }
}
