package com.example.data.billing

import com.example.data.model.MonetizationState
import com.example.data.model.ProFeatureQuotaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure Kotlin quota & anti-abuse policy for WatchPilot 5.1.0:
 * - Free monthly quotas automatically reset on calendar month rollover ("YYYY-MM").
 * - Hybrid "Watch an Ad to Try Pro Once" is strictly capped at 1 ad/day & 3 ads/month,
 *   with max 2 banked passes to prevent abuse.
 */
object MonetizationQuotaPolicy {

    fun currentDayKey(nowMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(nowMs))
    }

    fun currentMonthKey(nowMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Date(nowMs))
    }

    fun normalizeForDate(
        state: MonetizationState,
        dayKey: String = currentDayKey(),
        monthKey: String = currentMonthKey()
    ): MonetizationState {
        var next = state
        if (next.quotaYearMonth != monthKey) {
            next = next.copy(
                snapScansUsed = 0,
                aiConciergeUsed = 0,
                matchRoomsHosted = 0,
                aiRecapsUsed = 0,
                quotaYearMonth = monthKey
            )
        }
        if (next.rewardedLastDayKey != dayKey) {
            next = next.copy(
                rewardedAdsWatchedToday = 0,
                rewardedLastDayKey = dayKey
            )
        }
        if (next.rewardedMonthKey != monthKey) {
            next = next.copy(
                rewardedAdsWatchedThisMonth = 0,
                rewardedMonthKey = monthKey
            )
        }
        return next
    }

    fun recordRewardedAdEarned(
        state: MonetizationState,
        dayKey: String = currentDayKey(),
        monthKey: String = currentMonthKey()
    ): MonetizationState {
        val normalized = normalizeForDate(state, dayKey, monthKey)
        if (!normalized.canWatchRewardedTrialAd) return normalized
        return normalized.copy(
            rewardedTrialCredits = (normalized.rewardedTrialCredits + 1)
                .coerceAtMost(MonetizationState.MAX_BANKED_TRIAL_CREDITS),
            rewardedAdsWatchedToday = normalized.rewardedAdsWatchedToday + 1,
            rewardedLastDayKey = dayKey,
            rewardedAdsWatchedThisMonth = normalized.rewardedAdsWatchedThisMonth + 1,
            rewardedMonthKey = monthKey
        )
    }

    fun consumeFeatureUse(
        state: MonetizationState,
        feature: ProFeatureQuotaType,
        dayKey: String = currentDayKey(),
        monthKey: String = currentMonthKey()
    ): MonetizationState {
        val normalized = normalizeForDate(state, dayKey, monthKey)
        if (normalized.isPro) return normalized

        return when (feature) {
            ProFeatureQuotaType.SNAP_SCAN -> {
                if (normalized.remainingFreeSnapScans > 0) {
                    normalized.copy(snapScansUsed = normalized.snapScansUsed + 1)
                } else if (normalized.rewardedTrialCredits > 0) {
                    normalized.copy(rewardedTrialCredits = normalized.rewardedTrialCredits - 1)
                } else normalized
            }

            ProFeatureQuotaType.AI_CONCIERGE -> {
                if (normalized.remainingFreeConciergePicks > 0) {
                    normalized.copy(aiConciergeUsed = normalized.aiConciergeUsed + 1)
                } else if (normalized.rewardedTrialCredits > 0) {
                    normalized.copy(rewardedTrialCredits = normalized.rewardedTrialCredits - 1)
                } else normalized
            }

            ProFeatureQuotaType.MATCH_ROOM_HOST -> {
                if (normalized.remainingFreeHostedRooms > 0) {
                    normalized.copy(matchRoomsHosted = normalized.matchRoomsHosted + 1)
                } else if (normalized.rewardedTrialCredits > 0) {
                    normalized.copy(rewardedTrialCredits = normalized.rewardedTrialCredits - 1)
                } else normalized
            }

            ProFeatureQuotaType.AI_RECAP -> {
                if (normalized.remainingFreeAiRecaps > 0) {
                    normalized.copy(aiRecapsUsed = normalized.aiRecapsUsed + 1)
                } else if (normalized.rewardedTrialCredits > 0) {
                    normalized.copy(rewardedTrialCredits = normalized.rewardedTrialCredits - 1)
                } else normalized
            }
        }
    }
}
