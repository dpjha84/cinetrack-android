package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.OttAuditItem
import com.example.data.model.OttMoneySaverSummary
import com.example.data.model.OttRecommendation
import com.example.data.model.OttServiceCatalogItem
import com.example.data.model.WatchStatus
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class RegionalCurrencySpec(
    val symbol: String,
    val wholeUnitsOnly: Boolean,
    val fallbackMonthlyEstimate: Float
)

object OttMoneySaverCalculator {

    private val eurCurrencyRegions = setOf("FR", "DE", "ES", "IT")

    private val currencyByRegion: Map<String, RegionalCurrencySpec> = mapOf(
        "US" to RegionalCurrencySpec("$", wholeUnitsOnly = false, fallbackMonthlyEstimate = 8.99f),
        "GLOBAL" to RegionalCurrencySpec("$", wholeUnitsOnly = false, fallbackMonthlyEstimate = 8.99f),
        "IN" to RegionalCurrencySpec("₹", wholeUnitsOnly = true, fallbackMonthlyEstimate = 199f),
        "GB" to RegionalCurrencySpec("£", wholeUnitsOnly = false, fallbackMonthlyEstimate = 7.99f),
        "EUR" to RegionalCurrencySpec("€", wholeUnitsOnly = false, fallbackMonthlyEstimate = 8.99f),
        "JP" to RegionalCurrencySpec("¥", wholeUnitsOnly = true, fallbackMonthlyEstimate = 900f),
        "KR" to RegionalCurrencySpec("₩", wholeUnitsOnly = true, fallbackMonthlyEstimate = 9900f),
        "CA" to RegionalCurrencySpec("CA$", wholeUnitsOnly = false, fallbackMonthlyEstimate = 10.99f),
        "AU" to RegionalCurrencySpec("A$", wholeUnitsOnly = false, fallbackMonthlyEstimate = 11.99f),
        "BR" to RegionalCurrencySpec("R$", wholeUnitsOnly = false, fallbackMonthlyEstimate = 29.90f),
        "MX" to RegionalCurrencySpec("MX$", wholeUnitsOnly = true, fallbackMonthlyEstimate = 149f)
    )

    val supportedServices: List<OttServiceCatalogItem> = listOf(
        OttServiceCatalogItem(
            canonicalName = "Netflix",
            shortLabel = "Netflix",
            defaultMonthlyUsd = 15.49f,
            aliases = listOf("netflix", "netflix basic with ads", "netflix standard"),
            regionalMonthlyPrices = mapOf(
                "IN" to 199f,
                "GB" to 10.99f,
                "EUR" to 13.49f,
                "JP" to 890f,
                "KR" to 13500f,
                "CA" to 16.49f,
                "AU" to 16.99f,
                "BR" to 44.90f,
                "MX" to 219f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Prime Video",
            shortLabel = "Prime Video",
            defaultMonthlyUsd = 8.99f,
            aliases = listOf("amazon prime video", "prime video", "prime", "amazon video", "amazon prime"),
            regionalMonthlyPrices = mapOf(
                "IN" to 299f,
                "GB" to 8.99f,
                "EUR" to 8.99f,
                "JP" to 600f,
                "KR" to 6900f,
                "CA" to 9.99f,
                "AU" to 9.99f,
                "BR" to 19.90f,
                "MX" to 99f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Disney+ / Hotstar",
            shortLabel = "Disney+ / Hotstar",
            defaultMonthlyUsd = 11.99f,
            aliases = listOf("disney plus", "disney+", "hotstar", "disney+ hotstar", "jiohotstar"),
            regionalMonthlyPrices = mapOf(
                "IN" to 149f,
                "GB" to 7.99f,
                "EUR" to 8.99f,
                "JP" to 990f,
                "KR" to 9900f,
                "CA" to 11.99f,
                "AU" to 13.99f,
                "BR" to 43.90f,
                "MX" to 179f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Max",
            shortLabel = "Max",
            defaultMonthlyUsd = 16.99f,
            aliases = listOf("max", "hbo max", "hbo"),
            regionalMonthlyPrices = mapOf(
                "IN" to 499f,
                "GB" to 9.99f,
                "EUR" to 9.99f,
                "JP" to 1026f,
                "KR" to 11900f,
                "CA" to 16.99f,
                "AU" to 15.99f,
                "BR" to 39.90f,
                "MX" to 199f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Apple TV+",
            shortLabel = "Apple TV+",
            defaultMonthlyUsd = 9.99f,
            aliases = listOf("apple tv+", "apple tv plus", "apple tv"),
            regionalMonthlyPrices = mapOf(
                "IN" to 99f,
                "GB" to 8.99f,
                "EUR" to 9.99f,
                "JP" to 900f,
                "KR" to 6500f,
                "CA" to 12.99f,
                "AU" to 12.99f,
                "BR" to 21.90f,
                "MX" to 129f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Hulu",
            shortLabel = "Hulu",
            defaultMonthlyUsd = 9.99f,
            aliases = listOf("hulu"),
            regionalMonthlyPrices = mapOf(
                "IN" to 499f,
                "GB" to 7.99f,
                "EUR" to 8.99f,
                "JP" to 1026f,
                "KR" to 9900f,
                "CA" to 10.99f,
                "AU" to 11.99f,
                "BR" to 34.90f,
                "MX" to 149f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "JioCinema",
            shortLabel = "JioCinema",
            defaultMonthlyUsd = 1.99f,
            aliases = listOf("jiocinema", "jio cinema"),
            regionalMonthlyPrices = mapOf(
                "IN" to 29f,
                "GB" to 1.99f,
                "EUR" to 1.99f,
                "JP" to 300f,
                "KR" to 2900f,
                "CA" to 2.99f,
                "AU" to 2.99f,
                "BR" to 9.90f,
                "MX" to 39f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "SonyLIV",
            shortLabel = "SonyLIV",
            defaultMonthlyUsd = 3.99f,
            aliases = listOf("sonyliv", "sony liv"),
            regionalMonthlyPrices = mapOf(
                "IN" to 399f,
                "GB" to 3.99f,
                "EUR" to 3.99f,
                "JP" to 600f,
                "KR" to 4900f,
                "CA" to 4.99f,
                "AU" to 5.99f,
                "BR" to 14.90f,
                "MX" to 69f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "ZEE5",
            shortLabel = "ZEE5",
            defaultMonthlyUsd = 2.99f,
            aliases = listOf("zee5", "zee 5"),
            regionalMonthlyPrices = mapOf(
                "IN" to 149f,
                "GB" to 2.99f,
                "EUR" to 2.99f,
                "JP" to 450f,
                "KR" to 3900f,
                "CA" to 3.99f,
                "AU" to 4.99f,
                "BR" to 12.90f,
                "MX" to 59f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Crunchyroll",
            shortLabel = "Crunchyroll",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("crunchyroll"),
            regionalMonthlyPrices = mapOf(
                "IN" to 79f,
                "GB" to 4.99f,
                "EUR" to 6.99f,
                "JP" to 550f,
                "KR" to 7900f,
                "CA" to 9.99f,
                "AU" to 10.99f,
                "BR" to 14.90f,
                "MX" to 119f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Paramount+",
            shortLabel = "Paramount+",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("paramount+", "paramount plus"),
            regionalMonthlyPrices = mapOf(
                "IN" to 399f,
                "GB" to 6.99f,
                "EUR" to 7.99f,
                "JP" to 770f,
                "KR" to 7900f,
                "CA" to 10.99f,
                "AU" to 9.99f,
                "BR" to 19.90f,
                "MX" to 109f
            )
        ),
        OttServiceCatalogItem(
            canonicalName = "Peacock",
            shortLabel = "Peacock",
            defaultMonthlyUsd = 7.99f,
            aliases = listOf("peacock", "peacock premium"),
            regionalMonthlyPrices = mapOf(
                "IN" to 399f,
                "GB" to 6.99f,
                "EUR" to 7.99f,
                "JP" to 790f,
                "KR" to 7900f,
                "CA" to 9.99f,
                "AU" to 10.99f,
                "BR" to 19.90f,
                "MX" to 119f
            )
        )
    )

    private val indiaPriorityOrder = listOf(
        "Prime Video",
        "Disney+ / Hotstar",
        "Netflix",
        "SonyLIV",
        "ZEE5",
        "JioCinema",
        "Apple TV+",
        "Crunchyroll"
    )

    private fun normalizePricingRegionKey(regionCode: String): String {
        val upper = regionCode.trim().uppercase(Locale.US)
        return when {
            upper in eurCurrencyRegions -> "EUR"
            currencyByRegion.containsKey(upper) -> upper
            else -> "US"
        }
    }

    fun currencySpecForRegion(regionCode: String): RegionalCurrencySpec {
        val key = normalizePricingRegionKey(regionCode)
        return currencyByRegion[key] ?: currencyByRegion.getValue("US")
    }

    fun servicesForRegion(regionCode: String): List<OttServiceCatalogItem> {
        val upper = regionCode.trim().uppercase(Locale.US)
        if (upper != "IN") return supportedServices
        val prioritized = indiaPriorityOrder.mapNotNull { name ->
            supportedServices.firstOrNull { it.canonicalName == name }
        }
        val remaining = supportedServices.filterNot { it.canonicalName in indiaPriorityOrder }
        return prioritized + remaining
    }

    fun displayLabelForRegion(service: OttServiceCatalogItem, regionCode: String): String {
        return displayProviderNameForRegion(service.canonicalName, regionCode)
    }

    fun displayProviderNameForRegion(canonicalName: String, regionCode: String): String {
        val upper = regionCode.trim().uppercase(Locale.US)
        return if (upper == "IN" && canonicalName.equals("Disney+ / Hotstar", ignoreCase = true)) {
            "JioHotstar"
        } else {
            canonicalName
        }
    }

    fun monthlyPriceForRegion(service: OttServiceCatalogItem, regionCode: String): Float {
        val key = normalizePricingRegionKey(regionCode)
        return service.regionalMonthlyPrices[key] ?: service.defaultMonthlyUsd
    }

    fun formatAmount(amount: Float, regionCode: String, forceWholeUnits: Boolean = false): String {
        val spec = currencySpecForRegion(regionCode)
        val useWhole = spec.wholeUnitsOnly || forceWholeUnits
        return if (useWhole) {
            val rounded = amount.roundToInt()
            val formattedNumber = NumberFormat.getIntegerInstance(Locale.US).format(rounded)
            "${spec.symbol}$formattedNumber"
        } else {
            "${spec.symbol}${String.format(Locale.US, "%.2f", amount)}"
        }
    }

    fun formatChipPrice(service: OttServiceCatalogItem, regionCode: String): String {
        val price = monthlyPriceForRegion(service, regionCode)
        return formatAmount(price, regionCode, forceWholeUnits = true)
    }

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
        val normalizedRegion = regionCode.trim().uppercase(Locale.US).ifBlank { "US" }
        val currencySpec = currencySpecForRegion(normalizedRegion)
        val activePool = trackedMedia.filter { item ->
            val st = item.getWatchStatusEnum()
            st == WatchStatus.PLAN_TO_WATCH ||
                st == WatchStatus.WATCHING ||
                (st == WatchStatus.NONE && (item.isFavorite || !item.customWatchlist.isNullOrBlank()))
        }

        if (subscribedCanonicalNames.isEmpty()) {
            val zeroLabel = formatAmount(0f, normalizedRegion)
            return OttMoneySaverSummary(
                subscribedCount = 0,
                totalAuditedWatchlistCount = activePool.size,
                regionCode = normalizedRegion,
                currencySymbol = currencySpec.symbol,
                estimatedMonthlySpendLabel = zeroLabel,
                potentialMonthlySavingsLabel = zeroLabel,
                potentialMonthlySavingsShortLabel = formatAmount(0f, normalizedRegion, forceWholeUnits = true)
            )
        }

        val auditItems = subscribedCanonicalNames.map { subName ->
            val catalogEntry = supportedServices.find {
                it.canonicalName.equals(subName, ignoreCase = true)
            }
            val cost = if (catalogEntry != null) {
                monthlyPriceForRegion(catalogEntry, normalizedRegion)
            } else {
                currencySpec.fallbackMonthlyEstimate
            }

            val matchingItems = activePool.filter { item ->
                item.getProvidersForRegion(normalizedRegion)
                    .map { canonicalizeProvider(it) }
                    .any { it.equals(subName, ignoreCase = true) }
            }

            val recommendation = when {
                matchingItems.isEmpty() -> OttRecommendation.PAUSE_AND_SAVE
                matchingItems.size <= 2 -> OttRecommendation.CONSIDER_ROTATING
                else -> OttRecommendation.KEEP_ACTIVE
            }

            val canonicalProviderName = catalogEntry?.canonicalName ?: subName
            OttAuditItem(
                providerName = canonicalProviderName,
                monthlyCostEstimateUsd = cost,
                matchingTitlesCount = matchingItems.size,
                matchingTitlesPreview = matchingItems.take(3).map { it.title },
                recommendation = recommendation,
                displayName = displayProviderNameForRegion(canonicalProviderName, normalizedRegion),
                monthlyCostLabel = formatAmount(cost, normalizedRegion)
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
            matchesAnySubscribedApp(item, normalizedRegion, subscribedCanonicalNames)
        }

        return OttMoneySaverSummary(
            subscribedCount = subscribedCanonicalNames.size,
            estimatedMonthlySpendUsd = totalSpend,
            potentialMonthlySavingsUsd = potentialSavings,
            coveredWatchlistCount = coveredCount,
            totalAuditedWatchlistCount = activePool.size,
            items = auditItems,
            regionCode = normalizedRegion,
            currencySymbol = currencySpec.symbol,
            estimatedMonthlySpendLabel = formatAmount(totalSpend, normalizedRegion),
            potentialMonthlySavingsLabel = formatAmount(potentialSavings, normalizedRegion),
            potentialMonthlySavingsShortLabel = formatAmount(potentialSavings, normalizedRegion, forceWholeUnits = true)
        )
    }
}
