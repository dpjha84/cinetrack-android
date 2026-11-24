package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import kotlin.random.Random

/**
 * Pure Kotlin policy for rotating Home "Trending" spotlight titles on a
 * deterministic 6-hour cadence while preserving tier quality, movie/series balance,
 * and prioritizing unwatched picks.
 */
object TrendingRotationPolicy {

    const val SIX_HOURS_MS: Long = 6L * 60L * 60L * 1000L

    fun computeSixHourBucket(nowMillis: Long = System.currentTimeMillis()): Long {
        if (nowMillis <= 0L) return 0L
        return nowMillis / SIX_HOURS_MS
    }

    fun computeSeed(
        bucket: Long,
        region: String?,
        timeWindow: String
    ): Long {
        var hash = bucket * 6364136223846793005L + 1442695040888963407L
        val normalizedRegion = region?.trim()?.uppercase().orEmpty()
        for (ch in normalizedRegion) {
            hash = hash * 31L + ch.code
        }
        val normalizedWindow = timeWindow.trim().lowercase()
        for (ch in normalizedWindow) {
            hash = hash * 31L + ch.code
        }
        return hash
    }

    fun rotateTrendingForWindow(
        regionalHits: List<MediaEntity>,
        globalHits: List<MediaEntity>,
        watchedIds: Set<String> = emptySet(),
        region: String? = null,
        timeWindow: String = "day",
        nowMillis: Long = System.currentTimeMillis()
    ): List<MediaEntity> {
        val dedupedRegional = regionalHits.distinctBy { it.id }
        val regionalIdSet = dedupedRegional.mapTo(HashSet()) { it.id }
        val dedupedGlobalOnly = globalHits
            .distinctBy { it.id }
            .filter { it.id !in regionalIdSet }

        if (dedupedRegional.isEmpty() && dedupedGlobalOnly.isEmpty()) {
            return emptyList()
        }

        // Build Tier 1 (spotlight lead pool): top regional releases + top global trending hits
        val tier1RegionalCount = if (dedupedGlobalOnly.isEmpty()) 14 else 8
        val tier1GlobalCount = if (dedupedRegional.isEmpty()) 14 else 6

        val tier1Regional = dedupedRegional.take(tier1RegionalCount)
        val tier1Global = dedupedGlobalOnly.take(tier1GlobalCount)
        val tier1Pool = (tier1Regional + tier1Global).distinctBy { it.id }
        val tier1Ids = tier1Pool.mapTo(HashSet()) { it.id }

        val remainingPool = (dedupedRegional.drop(tier1RegionalCount) + dedupedGlobalOnly.drop(tier1GlobalCount))
            .filter { it.id !in tier1Ids }
            .distinctBy { it.id }

        val tier2Pool = remainingPool.take(14)
        val tier3Pool = remainingPool.drop(14)

        val bucket = computeSixHourBucket(nowMillis)
        val baseSeed = computeSeed(bucket = bucket, region = region, timeWindow = timeWindow)

        val (tier1Unwatched, tier1Watched) = partitionAndShuffleTier(
            items = tier1Pool,
            watchedIds = watchedIds,
            seed = baseSeed + 17L,
            interleaveTypes = true
        )
        val (tier2Unwatched, tier2Watched) = partitionAndShuffleTier(
            items = tier2Pool,
            watchedIds = watchedIds,
            seed = baseSeed + 53L,
            interleaveTypes = true
        )
        val (tier3Unwatched, tier3Watched) = partitionAndShuffleTier(
            items = tier3Pool,
            watchedIds = watchedIds,
            seed = baseSeed + 97L,
            interleaveTypes = false
        )

        return (tier1Unwatched + tier2Unwatched + tier3Unwatched + tier1Watched + tier2Watched + tier3Watched)
            .distinctBy { it.id }
    }

    private fun partitionAndShuffleTier(
        items: List<MediaEntity>,
        watchedIds: Set<String>,
        seed: Long,
        interleaveTypes: Boolean
    ): Pair<List<MediaEntity>, List<MediaEntity>> {
        if (items.isEmpty()) return emptyList<MediaEntity>() to emptyList()

        val unwatched = items.filter { it.id !in watchedIds }
        val watched = items.filter { it.id in watchedIds }

        val shuffledUnwatched = shuffleTier(unwatched, Random(seed), interleaveTypes)
        val shuffledWatched = shuffleTier(watched, Random(seed xor 0x5A5A5A5AL), interleaveTypes = false)
        return shuffledUnwatched to shuffledWatched
    }

    private fun shuffleTier(
        items: List<MediaEntity>,
        rng: Random,
        interleaveTypes: Boolean
    ): List<MediaEntity> {
        if (items.size <= 1) return items
        if (!interleaveTypes) {
            return items.shuffled(rng)
        }
        val movies = items.filter { it.getMediaTypeEnum() == MediaType.MOVIE }.shuffled(rng)
        val tvShows = items.filter { it.getMediaTypeEnum() != MediaType.MOVIE }.shuffled(rng)
        if (movies.isEmpty() || tvShows.isEmpty()) {
            return (movies + tvShows).shuffled(rng)
        }
        val startWithTv = rng.nextBoolean()
        val maxLen = maxOf(movies.size, tvShows.size)
        return buildList(items.size) {
            for (i in 0 until maxLen) {
                if (startWithTv) {
                    if (i < tvShows.size) add(tvShows[i])
                    if (i < movies.size) add(movies[i])
                } else {
                    if (i < movies.size) add(movies[i])
                    if (i < tvShows.size) add(tvShows[i])
                }
            }
        }
    }
}
