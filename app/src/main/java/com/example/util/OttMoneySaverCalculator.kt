package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.OttAuditItem
import com.example.data.model.OttMoneySaverSummary
import com.example.data.model.OttRecommendation
import com.example.data.model.OttServiceCatalogItem
import com.example.data.model.WatchStatus

object OttMoneySaverCalculator {

    val supportedServices: List<OttServiceCatalogItem> = listOf(
        OttServiceCatalogItem(
            canonicalName = "Netflix",
            shortLabel = "Netflix",
            defaultMonthlyUsd = 15.49f,
            aliases = listOf("netflix", "netflix basic with ads", "netflix standard")
        ),
        OttServiceCatalogItem(
            canonicalName = "Prime Video",
            shortLabel = "Prime Video",
            defaultMonthlyUsd = 8.99f,
            aliases = listOf("amazon prime video", "prime video", "prime", "amazon video", "amazon prime")
        ),
        OttServiceCatalogItem(
            canonicalName = "Disney+ / Hotstar",
            shortLabel = "Disney+ / Hotstar",
            defaultMonthlyUsd = 11.99f,
            aliases = listOf("disney plus", "disney+", "hotstar", "disney+ hotstar", "jiohotstar")
        ),
        OttServiceCatalogItem(
            canonicalName = "Max",
            shortLabel = "Max",
            defaultMonthlyUsd = 16.99f,
            aliases = listOf("max", "hbo max", "hbo")
        ),
        OttServiceCatalogItem(
            canonicalName = "Apple TV+",
            shortLabel = "Apple TV+",
            defaultMonthlyUsd = 9.99f,
            aliases = listOf("apple tv+", "apple tv plus", "apple tv")
        ),
        OttServiceCatalogItem(
            canonicalName = "Hulu",
            shortLabel = "Hulu",
            defaultMonthlyUsd = 9.99f,
            aliases = listOf("hulu")
        ),
        OttServiceCatalogItem(
            canonicalName = "JioCinema",
            shortLabel = "JioCinema",
            defaultMonthlyUsd = 1.99f,
            aliases = listOf("jiocinema", "jio cinema")
        ),
        OttServiceCatalogItem(
            canonicalName = "SonyLIV",
            shortLabel = "SonyLIV",
            defaultMonthlyUsd = 3.99f,
            aliases = listOf("sonyliv", "sony liv")
        ),
        OttServiceCatalogItem(
            canonicalName = "ZEE5",
            shortLabel = "ZEE5",
            defaultMonthlyUsd = 2.99f,
            aliases = listOf("zee5", "zee 5")
        ),
        OttServiceCatalogItem(
            canonicalName = "Crunchyroll",
            shortLabel = "Crunchyroll",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("crunchyroll")
        ),
        OttServiceCatalogItem(
            canonicalName = "Paramount+",
            shortLabel = "Paramount+",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("paramount+", "paramount plus")
        ),
        OttServiceCatalogItem(
            canonicalName = "Peacock",
            shortLabel = "Peacock",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("peacock", "peacock premium")
        )
    )

    fun canonicalizeProvider(rawName: String): String {
        val clean = rawName.trim().lowercase()
        if (clean.isBlank()) return ""
        val match = supportedServices.firstOrNull { service ->
            service.canonicalName.equals(clean, ignoreCase = true) ||
                service.aliases.any { alias -> clean == alias || clean.contains(alias) }
        }
        return match?.canonicalName ?: rawName.trim()
    }

    fun matchesAnySubscribedApp(
        media: MediaEntity,
        regionCode: String,
        subscribedCanonicalNames: Set<String>
    ): Boolean {
        if (subscribedCanonicalNames.isEmpty()) return true
        val regionalProviders = media.getProvidersForRegion(regionCode)
            .map { canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
        return regionalProviders.any { it in subscribedCanonicalNames }
    }

    fun calculateAudit(
        trackedMedia: List<MediaEntity>,
        regionCode: String,
        subscribedCanonicalNames: Set<String>
    ): OttMoneySaverSummary {
        val activePool = trackedMedia.filter { item ->
            val st = item.getWatchStatusEnum()
            st == WatchStatus.PLAN_TO_WATCH ||
                st == WatchStatus.WATCHING ||
                (st == WatchStatus.NONE && (item.isFavorite || !item.customWatchlist.isNullOrBlank()))
        }

        if (subscribedCanonicalNames.isEmpty()) {
            return OttMoneySaverSummary(
                subscribedCount = 0,
                totalAuditedWatchlistCount = activePool.size
            )
        }

        val auditItems = subscribedCanonicalNames.map { subName ->
            val catalogEntry = supportedServices.find {
                it.canonicalName.equals(subName, ignoreCase = true)
            }
            val cost = catalogEntry?.defaultMonthlyUsd ?: 8.99f

            val matchingItems = activePool.filter { item ->
                item.getProvidersForRegion(regionCode)
                    .map { canonicalizeProvider(it) }
                    .any { it.equals(subName, ignoreCase = true) }
            }

            val recommendation = when {
                matchingItems.isEmpty() -> OttRecommendation.PAUSE_AND_SAVE
                matchingItems.size <= 2 -> OttRecommendation.CONSIDER_ROTATING
                else -> OttRecommendation.KEEP_ACTIVE
            }

            OttAuditItem(
                providerName = catalogEntry?.canonicalName ?: subName,
                monthlyCostEstimateUsd = cost,
                matchingTitlesCount = matchingItems.size,
                matchingTitlesPreview = matchingItems.take(3).map { it.title },
                recommendation = recommendation
            )
        }.sortedWith(
            compareBy<OttAuditItem> {
                when (it.recommendation) {
                    OttRecommendation.PAUSE_AND_SAVE -> 0
                    OttRecommendation.CONSIDER_ROTATING -> 1
                    OttRecommendation.KEEP_ACTIVE -> 2
                }
            }.thenByDescending { it.monthlyCostEstimateUsd }
        )

        val totalSpend = auditItems.sumOf { it.monthlyCostEstimateUsd.toDouble() }.toFloat()
        val potentialSavings = auditItems
            .filter { it.recommendation == OttRecommendation.PAUSE_AND_SAVE }
            .sumOf { it.monthlyCostEstimateUsd.toDouble() }
            .toFloat()

        val coveredCount = activePool.count { item ->
            matchesAnySubscribedApp(item, regionCode, subscribedCanonicalNames)
        }

        return OttMoneySaverSummary(
            subscribedCount = subscribedCanonicalNames.size,
            estimatedMonthlySpendUsd = totalSpend,
            potentialMonthlySavingsUsd = potentialSavings,
            coveredWatchlistCount = coveredCount,
            totalAuditedWatchlistCount = activePool.size,
            items = auditItems
        )
    }
}
