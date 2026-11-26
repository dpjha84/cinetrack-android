package com.example.data.model

import com.example.data.local.MediaEntity

enum class PlanType(val displayLabel: String) {
    FREE("WatchPilot Free"),
    PRO_MONTHLY("WatchPilot Pro • Monthly"),
    PRO_ANNUAL("WatchPilot Pro • Annual"),
    PRO_LIFETIME("WatchPilot Pro • Lifetime"),
    PRO_SANDBOX("WatchPilot Pro • Sandbox Preview")
}

enum class ProFeatureQuotaType {
    SNAP_SCAN,
    AI_CONCIERGE,
    MATCH_ROOM_HOST,
    AI_RECAP
}

data class MonetizationState(
    val isPro: Boolean = false,
    val planType: PlanType = PlanType.FREE,
    val monthlyPriceLabel: String = "$1.99 / mo",
    val annualPriceLabel: String = "$11.99 / yr",
    val lifetimePriceLabel: String = "$24.99 once",
    val isPlayBillingConnected: Boolean = false,
    val snapScansUsed: Int = 0,
    val aiConciergeUsed: Int = 0,
    val matchRoomsHosted: Int = 0,
    val aiRecapsUsed: Int = 0,
    val quotaYearMonth: String = "",
    val rewardedTrialCredits: Int = 0,
    val rewardedAdsWatchedToday: Int = 0,
    val rewardedLastDayKey: String = "",
    val rewardedAdsWatchedThisMonth: Int = 0,
    val rewardedMonthKey: String = "",
    val subscribedProviders: Set<String> = emptySet(),
    val streamOnMyAppsOnly: Boolean = false,
    val proAlertsEnabled: Boolean = false,
    val billingStatusMessage: String? = null
) {
    companion object {
        const val FREE_SNAP_SCANS_LIMIT = 3
        const val FREE_CONCIERGE_LIMIT = 3
        const val FREE_MATCH_ROOMS_LIMIT = 1
        const val FREE_AI_RECAPS_LIMIT = 1
        const val FREE_CUSTOM_COLLECTIONS_LIMIT = 2

        const val MAX_REWARDED_ADS_PER_DAY = 1
        const val MAX_REWARDED_ADS_PER_MONTH = 3
        const val MAX_BANKED_TRIAL_CREDITS = 2
    }

    val remainingFreeSnapScans: Int
        get() = (FREE_SNAP_SCANS_LIMIT - snapScansUsed).coerceAtLeast(0)

    val remainingFreeConciergePicks: Int
        get() = (FREE_CONCIERGE_LIMIT - aiConciergeUsed).coerceAtLeast(0)

    val remainingFreeHostedRooms: Int
        get() = (FREE_MATCH_ROOMS_LIMIT - matchRoomsHosted).coerceAtLeast(0)

    val remainingFreeAiRecaps: Int
        get() = (FREE_AI_RECAPS_LIMIT - aiRecapsUsed).coerceAtLeast(0)

    val remainingRewardedAdsToday: Int
        get() = (MAX_REWARDED_ADS_PER_DAY - rewardedAdsWatchedToday).coerceAtLeast(0)

    val remainingRewardedAdsThisMonth: Int
        get() = (MAX_REWARDED_ADS_PER_MONTH - rewardedAdsWatchedThisMonth).coerceAtLeast(0)

    val canWatchRewardedTrialAd: Boolean
        get() = !isPro &&
            rewardedTrialCredits < MAX_BANKED_TRIAL_CREDITS &&
            remainingRewardedAdsToday > 0 &&
            remainingRewardedAdsThisMonth > 0

    val canUseSnapScan: Boolean
        get() = isPro || remainingFreeSnapScans > 0 || rewardedTrialCredits > 0

    val canUseConcierge: Boolean
        get() = isPro || remainingFreeConciergePicks > 0 || rewardedTrialCredits > 0

    val canHostMatchRoom: Boolean
        get() = isPro || remainingFreeHostedRooms > 0 || rewardedTrialCredits > 0

    val canUseAiRecap: Boolean
        get() = isPro || remainingFreeAiRecaps > 0 || rewardedTrialCredits > 0

    val rewardedCapStatusText: String
        get() = when {
            isPro -> "Pro active — unlimited access without ads"
            rewardedTrialCredits >= MAX_BANKED_TRIAL_CREDITS ->
                "$rewardedTrialCredits Ad Passes ready — use one before unlocking more"
            remainingRewardedAdsThisMonth <= 0 ->
                "Monthly ad trial cap reached ($MAX_REWARDED_ADS_PER_MONTH/$MAX_REWARDED_ADS_PER_MONTH used)"
            remainingRewardedAdsToday <= 0 ->
                "Daily ad trial used ($MAX_REWARDED_ADS_PER_DAY/1 today) • Resets tomorrow"
            else ->
                "$remainingRewardedAdsToday/1 available today • $remainingRewardedAdsThisMonth/$MAX_REWARDED_ADS_PER_MONTH this month"
        }
}

enum class OttRecommendation(val badgeText: String) {
    KEEP_ACTIVE("High Value • Keep"),
    CONSIDER_ROTATING("Low Usage • Rotate"),
    PAUSE_AND_SAVE("0 Matches • Pause & Save")
}

data class OttServiceCatalogItem(
    val canonicalName: String,
    val shortLabel: String,
    val defaultMonthlyUsd: Float,
    val aliases: List<String>,
    val regionalMonthlyPrices: Map<String, Float> = emptyMap()
)

data class OttAuditItem(
    val providerName: String,
    val monthlyCostEstimateUsd: Float,
    val matchingTitlesCount: Int,
    val matchingTitlesPreview: List<String>,
    val recommendation: OttRecommendation,
    val displayName: String = providerName,
    val monthlyCostLabel: String = ""
)

data class OttMoneySaverSummary(
    val subscribedCount: Int = 0,
    val estimatedMonthlySpendUsd: Float = 0f,
    val potentialMonthlySavingsUsd: Float = 0f,
    val coveredWatchlistCount: Int = 0,
    val totalAuditedWatchlistCount: Int = 0,
    val items: List<OttAuditItem> = emptyList(),
    val regionCode: String = "US",
    val currencySymbol: String = "$",
    val estimatedMonthlySpendLabel: String = "$0.00",
    val potentialMonthlySavingsLabel: String = "$0.00",
    val potentialMonthlySavingsShortLabel: String = "$0"
)

data class ScannedMediaCandidate(
    val rawTitle: String,
    val releaseYear: Int?,
    val mediaType: MediaType,
    val note: String,
    val matchedEntity: MediaEntity?
)

sealed interface SnapScanUiState {
    data object Idle : SnapScanUiState
    data object Scanning : SnapScanUiState
    data class Success(val items: List<ScannedMediaCandidate>) : SnapScanUiState
    data class Error(val message: String) : SnapScanUiState
}

data class ConciergePick(
    val title: String,
    val releaseYear: Int,
    val mediaType: MediaType,
    val whyItFits: String,
    val matchScore: Int,
    val matchedEntity: MediaEntity?
)

sealed interface ConciergeUiState {
    data object Idle : ConciergeUiState
    data object Loading : ConciergeUiState
    data class Success(
        val mood: String,
        val timeBudget: String,
        val picks: List<ConciergePick>
    ) : ConciergeUiState
    data class Error(val message: String) : ConciergeUiState
}

data class TitleAiBrief(
    val mediaId: String,
    val personalMatchScore: Int,
    val watchPriorityTag: String,
    val fitSummary: String,
    val seasonRecap: String
)

sealed interface TitleAiBriefUiState {
    data object Idle : TitleAiBriefUiState
    data object Loading : TitleAiBriefUiState
    data class Success(val brief: TitleAiBrief) : TitleAiBriefUiState
    data class Error(val message: String) : TitleAiBriefUiState
}

enum class WatchAlertType(val badgeLabel: String) {
    NOW_ON_MY_APPS("NOW STREAMING"),
    NEW_SEASON("NEW SEASON")
}

data class WatchAlertEvent(
    val id: String,
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val alertType: WatchAlertType,
    val detailMessage: String,
    val detectedAtMs: Long = System.currentTimeMillis()
)

data class MatchCandidateCard(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val mediaType: String,
    val genres: String,
    val rating: Float,
    val posterUrl: String,
    val providersLabel: String
)

data class MatchRoomState(
    val roomCode: String,
    val hostUid: String,
    val hostName: String,
    val regionCode: String,
    val selectedProviders: List<String>,
    val status: String,
    val participantUids: List<String>,
    val candidates: List<MatchCandidateCard>,
    val swipesByUid: Map<String, Set<String>>,
    val matchedMediaId: String?,
    val matchedTitle: String?,
    val matchedPosterUrl: String?,
    val createdAt: Long
)

sealed interface MatchRoomUiState {
    data object Idle : MatchRoomUiState
    data object Loading : MatchRoomUiState
    data class Active(val room: MatchRoomState) : MatchRoomUiState
    data class Error(val message: String) : MatchRoomUiState
}

data class CsvImportResult(
    val importedCount: Int,
    val enrichedCount: Int,
    val detectedFormat: String,
    val message: String
)
