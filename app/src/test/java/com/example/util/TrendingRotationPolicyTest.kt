package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendingRotationPolicyTest {

    private fun media(
        id: String,
        title: String,
        type: MediaType = MediaType.MOVIE,
        originCountry: String = "US"
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = type.name,
        releaseYear = 2025,
        genres = "Drama",
        posterUrl = "https://image.tmdb.org/t/p/w500/$id.jpg",
        backdropUrl = "",
        overview = "",
        directorOrCreator = "",
        cast = "",
        communityRating = 8.0f,
        runtimeMinutes = 120,
        originCountry = originCountry
    )

    @Test
    fun sameSixHourWindowProducesDeterministicStableOrder() {
        val regional = (1..10).map { media("reg_$it", "Regional $it", MediaType.MOVIE, "IN") }
        val global = (1..10).map {
            media(
                id = "glob_$it",
                title = "Global $it",
                type = if (it % 2 == 0) MediaType.TV_SHOW else MediaType.MOVIE
            )
        }

        val startOfWindow = 100L * TrendingRotationPolicy.SIX_HOURS_MS + 1_000L
        val laterSameWindow = 100L * TrendingRotationPolicy.SIX_HOURS_MS + (5L * 60L * 60L * 1000L)

        val first = TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = regional,
            globalHits = global,
            region = "IN",
            timeWindow = "day",
            nowMillis = startOfWindow
        )
        val second = TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = regional,
            globalHits = global,
            region = "IN",
            timeWindow = "day",
            nowMillis = laterSameWindow
        )

        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(20, first.size)
    }

    @Test
    fun crossingSixHourBoundaryRotatesSpotlightWhilePreservingTopTierQuality() {
        val regional = (1..12).map { media("reg_$it", "Regional $it", MediaType.MOVIE, "US") }
        val global = (1..16).map {
            media(
                id = "glob_$it",
                title = "Global $it",
                type = if (it % 2 == 0) MediaType.TV_SHOW else MediaType.MOVIE
            )
        }

        val bucketA = 200L * TrendingRotationPolicy.SIX_HOURS_MS + 60_000L
        val ordersAcrossWindows = (0L..3L).map { offset ->
            TrendingRotationPolicy.rotateTrendingForWindow(
                regionalHits = regional,
                globalHits = global,
                region = "US",
                timeWindow = "day",
                nowMillis = bucketA + offset * TrendingRotationPolicy.SIX_HOURS_MS
            ).map { it.id }
        }

        // Across 4 consecutive 6-hour windows, the top 10 spotlight order changes cleanly
        assertTrue(ordersAcrossWindows.map { it.take(10) }.distinct().size >= 3)

        // All Tier 1 pool items (first 8 regional + first 6 global = 14) remain ahead of Tier 2/3 items
        val expectedTier1Ids = (regional.take(8) + global.take(6)).map { it.id }.toSet()
        for (order in ordersAcrossWindows) {
            assertEquals(expectedTier1Ids, order.take(14).toSet())
            assertNotEquals(ordersAcrossWindows[0].take(14), listOf<String>())
        }
    }

    @Test
    fun watchedTitlesArePushedAfterUnwatchedTrendingPicks() {
        val regional = (1..6).map { media("reg_$it", "Regional $it", MediaType.MOVIE, "IN") }
        val global = (1..6).map { media("glob_$it", "Global $it", MediaType.TV_SHOW, "US") }
        val watched = setOf("reg_1", "reg_2", "glob_1")

        val rotated = TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = regional,
            globalHits = global,
            watchedIds = watched,
            region = "IN",
            timeWindow = "day",
            nowMillis = 500L * TrendingRotationPolicy.SIX_HOURS_MS
        )

        val unwatchedCount = rotated.size - watched.size
        assertTrue(rotated.take(unwatchedCount).none { it.id in watched })
        assertTrue(rotated.drop(unwatchedCount).all { it.id in watched })
    }

    @Test
    fun tier1InterleavesMoviesAndTvShowsWhenBothAreAvailable() {
        val regionalMovies = (1..8).map { media("reg_m_$it", "Movie $it", MediaType.MOVIE, "US") }
        val globalTvShows = (1..6).map { media("glob_tv_$it", "Series $it", MediaType.TV_SHOW, "US") }

        val rotated = TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = regionalMovies,
            globalHits = globalTvShows,
            region = "US",
            timeWindow = "day",
            nowMillis = 321L * TrendingRotationPolicy.SIX_HOURS_MS
        )

        val top6Types = rotated.take(6).map { it.getMediaTypeEnum() }.toSet()
        assertTrue(MediaType.MOVIE in top6Types)
        assertTrue(MediaType.TV_SHOW in top6Types)
    }

    @Test
    fun emptyInputsReturnEmptySafely() {
        val empty = TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = emptyList(),
            globalHits = emptyList()
        )
        assertTrue(empty.isEmpty())
    }
}
