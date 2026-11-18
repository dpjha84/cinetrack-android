package com.example

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.OttRecommendation
import com.example.data.model.WatchStatus
import com.example.util.CsvLibraryPorter
import com.example.util.OttMoneySaverCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPilotV2UnitTest {

    private fun sampleMedia(
        id: String,
        title: String,
        status: WatchStatus = WatchStatus.PLAN_TO_WATCH,
        providers: String = "US:Netflix,Max|IN:JioCinema",
        rating: Float = 0f,
        isFavorite: Boolean = false
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = MediaType.MOVIE.name,
        releaseYear = 2024,
        genres = "Sci-Fi, Thriller",
        posterUrl = "https://example.com/p.jpg",
        backdropUrl = "https://example.com/b.jpg",
        overview = "Sample overview",
        directorOrCreator = "Director",
        cast = "Actor",
        communityRating = 8.1f,
        runtimeMinutes = 120,
        totalSeasons = 0,
        totalEpisodes = 1,
        status = status.name,
        userRating = rating,
        isFavorite = isFavorite,
        streamingProviders = providers
    )

    @Test
    fun csvPorter_parsesMoviebaseImdbLetterboxdAndRoundTripsWatchPilot() {
        val moviebaseCsv = """
            tmdb_id,title,year,media_type,status,rating
            693134,"Dune: Part Two",2024,movie,completed,9.0
            94997,"House of the Dragon",2022,tv,watching,8.0
        """.trimIndent()

        val mbBatch = CsvLibraryPorter.parseUniversalCsv(moviebaseCsv)
        assertEquals("Moviebase", mbBatch.formatName)
        assertEquals(2, mbBatch.rows.size)
        assertEquals("Dune: Part Two", mbBatch.rows[0].title)
        assertEquals(WatchStatus.COMPLETED, mbBatch.rows[0].status)
        assertEquals(4.5f, mbBatch.rows[0].userRatingOutOf5, 0.01f)
        assertEquals(MediaType.TV_SHOW, mbBatch.rows[1].mediaType)
        assertEquals(WatchStatus.WATCHING, mbBatch.rows[1].status)

        val imdbCsv = """
            Const,Your Rating,Date Rated,Title,Title Type,Year
            tt15239678,8,2024-03-05,Dune: Part Two,Movie,2024
        """.trimIndent()
        val imdbBatch = CsvLibraryPorter.parseUniversalCsv(imdbCsv)
        assertEquals("IMDb", imdbBatch.formatName)
        assertEquals(1, imdbBatch.rows.size)
        assertEquals(WatchStatus.COMPLETED, imdbBatch.rows[0].status)
        assertEquals(4.0f, imdbBatch.rows[0].userRatingOutOf5, 0.01f)

        val letterboxdCsv = """
            Date,Name,Year,Letterboxd URI,Rating,Watched Date
            2024-04-01,Oppenheimer,2023,https://boxd.it/xyz,4.5,2024-03-29
        """.trimIndent()
        val lbBatch = CsvLibraryPorter.parseUniversalCsv(letterboxdCsv)
        assertEquals("Letterboxd", lbBatch.formatName)
        assertEquals(1, lbBatch.rows.size)
        assertEquals(WatchStatus.COMPLETED, lbBatch.rows[0].status)
        assertEquals(4.5f, lbBatch.rows[0].userRatingOutOf5, 0.01f)

        val roundTripSource = listOf(
            sampleMedia("tmdb_m_101", "Dune: Part Two", WatchStatus.COMPLETED, rating = 5.0f, isFavorite = true),
            sampleMedia("tmdb_m_102", "Blade Runner 2099", WatchStatus.PLAN_TO_WATCH, rating = 0f, isFavorite = false)
        )
        val exportedCsv = CsvLibraryPorter.serializeToCsv(roundTripSource)
        val wpBatch = CsvLibraryPorter.parseUniversalCsv(exportedCsv)
        assertEquals("WatchPilot", wpBatch.formatName)
        assertEquals(2, wpBatch.rows.size)
        assertTrue(wpBatch.rows[0].isFavorite)
        assertEquals(WatchStatus.PLAN_TO_WATCH, wpBatch.rows[1].status)
    }

    @Test
    fun ottMoneySaver_detectsPauseAndSaveCandidatesAndMatchesApps() {
        val tracked = listOf(
            sampleMedia("m1", "Title 1", WatchStatus.PLAN_TO_WATCH, "US:Netflix,Max"),
            sampleMedia("m2", "Title 2", WatchStatus.WATCHING, "US:Netflix"),
            sampleMedia("m3", "Title 3", WatchStatus.PLAN_TO_WATCH, "US:Netflix"),
            sampleMedia("m4", "Title 4", WatchStatus.PLAN_TO_WATCH, "US:Prime Video")
        )

        val summary = OttMoneySaverCalculator.calculateAudit(
            trackedMedia = tracked,
            regionCode = "US",
            subscribedCanonicalNames = setOf("Netflix", "Prime Video", "Apple TV+")
        )

        assertEquals(3, summary.subscribedCount)
        assertEquals(4, summary.coveredWatchlistCount)

        val appleAudit = summary.items.first { it.providerName == "Apple TV+" }
        assertEquals(0, appleAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.PAUSE_AND_SAVE, appleAudit.recommendation)

        val primeAudit = summary.items.first { it.providerName == "Prime Video" }
        assertEquals(1, primeAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.CONSIDER_ROTATING, primeAudit.recommendation)

        val netflixAudit = summary.items.first { it.providerName == "Netflix" }
        assertEquals(3, netflixAudit.matchingTitlesCount)
        assertEquals(OttRecommendation.KEEP_ACTIVE, netflixAudit.recommendation)

        assertTrue(summary.potentialMonthlySavingsUsd >= 9.99f)
        assertTrue(OttMoneySaverCalculator.matchesAnySubscribedApp(tracked[0], "US", setOf("Netflix")))
        assertFalse(OttMoneySaverCalculator.matchesAnySubscribedApp(tracked[3], "US", setOf("Apple TV+")))
    }
}
