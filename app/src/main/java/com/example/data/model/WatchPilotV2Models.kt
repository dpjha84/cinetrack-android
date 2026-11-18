package com.example.data.model

import com.example.data.local.MediaEntity

enum class PlanType(val displayLabel: String) {
    FREE("WatchPilot Free"),
    PRO_MONTHLY("WatchPilot Pro • Monthly"),
    PRO_ANNUAL("WatchPilot Pro • Annual"),
    PRO_LIFETIME("WatchPilot Pro • Lifetime"),
    PRO_SANDBOX("WatchPilot Pro • Sandbox Preview")
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
    val subscribedProviders: Set<String> = emptySet(),
    val streamOnMyAppsOnly: Boolean = false,
    val billingStatusMessage: String? = null
) {
    companion object {
        const val FREE_SNAP_SCANS_LIMIT = 3
        const val FREE_CONCIERGE_LIMIT = 3
        const val FREE_MATCH_ROOMS_LIMIT = 1
        const val FREE_CUSTOM_COLLECTIONS_LIMIT = 2
    }

    val remainingFreeSnapScans: Int
        get() = (FREE_SNAP_SCANS_LIMIT - snapScansUsed).coerceAtLeast(0)

    val remainingFreeConciergePicks: Int
        get() = (FREE_CONCIERGE_LIMIT - aiConciergeUsed).coerceAtLeast(0)

    val remainingFreeHostedRooms: Int
        get() = (FREE_MATCH_ROOMS_LIMIT - matchRoomsHosted).coerceAtLeast(0)

    val canUseSnapScan: Boolean
        get() = isPro || remainingFreeSnapScans > 0

    val canUseConcierge: Boolean
        get() = isPro || remainingFreeConciergePicks > 0

    val canHostMatchRoom: Boolean
        get() = isPro || remainingFreeHostedRooms > 0
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
    val aliases: List<String>
)

data class OttAuditItem(
    val providerName: String,
    val monthlyCostEstimateUsd: Float,
    val matchingTitlesCount: Int,
    val matchingTitlesPreview: List<String>,
    val recommendation: OttRecommendation
)

data class OttMoneySaverSummary(
    val subscribedCount: Int = 0,
    val estimatedMonthlySpendUsd: Float = 0f,
    val potentialMonthlySavingsUsd: Float = 0f,
    val coveredWatchlistCount: Int = 0,
    val totalAuditedWatchlistCount: Int = 0,
    val items: List<OttAuditItem> = emptyList()
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
