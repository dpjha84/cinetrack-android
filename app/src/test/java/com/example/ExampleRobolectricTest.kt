package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.ViewingStats
import com.example.data.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CineTrack", appName)
    }

    @Test
    fun `tv show progress fraction calculation`() {
        val show = MediaEntity(
            id = "test_tv",
            title = "Test Series",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2024,
            genres = "Drama",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.5f,
            runtimeMinutes = 45,
            totalSeasons = 2,
            totalEpisodes = 20,
            watchedEpisodes = 10
        )
        assertEquals(0.5f, show.progressFraction, 0.01f)
    }

    @Test
    fun `viewing stats formatting`() {
        val stats = ViewingStats(
            totalTimeMinutes = 1500, // 25 hours -> 1d 1h
            completedMoviesCount = 5,
            totalEpisodesLogged = 15
        )
        assertEquals(1L, stats.totalDays)
        assertEquals(1L, stats.remainingHours)
        assertTrue(stats.formattedDuration.contains("1d 1h"))
    }

    @Test
    fun `regional streaming provider resolution`() {
        val media = MediaEntity(
            id = "test_regional",
            title = "International Hit",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2024,
            genres = "Action",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.0f,
            runtimeMinutes = 120,
            originalLanguage = "ja",
            originCountry = "JP",
            streamingProviders = "US:Max,Hulu|JP:U-NEXT,Netflix|GLOBAL:Prime Video"
        )
        val usProviders = media.getProvidersForRegion("US")
        val jpProviders = media.getProvidersForRegion("JP")
        val defaultProviders = media.getProvidersForRegion("FR")

        assertTrue(usProviders.contains("Max"))
        assertTrue(jpProviders.contains("U-NEXT"))
        assertTrue(defaultProviders.contains("Prime Video"))
    }
}
