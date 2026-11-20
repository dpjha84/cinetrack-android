package com.example

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchProviderType
import com.example.data.model.WatchProvidersInfo
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMediaEntityWatchProvidersParsing() {
    val entity = MediaEntity(
        id = "test_1",
        title = "Inception",
        mediaType = MediaType.MOVIE.name,
        releaseYear = 2010,
        genres = "Sci-Fi, Action",
        posterUrl = "https://example.com/poster.jpg",
        backdropUrl = "https://example.com/backdrop.jpg",
        overview = "Dream heist",
        directorOrCreator = "Christopher Nolan",
        cast = "Leonardo DiCaprio",
        communityRating = 8.8f,
        runtimeMinutes = 148,
        totalSeasons = 0,
        totalEpisodes = 1,
        networkOrStudio = "Warner Bros",
        status = "PLAN_TO_WATCH",
        originalLanguage = "en",
        originCountry = "US",
        streamingProviders = "US:Netflix,Max|IN:JioCinema,Prime Video|GLOBAL:Netflix"
    )

    val usInfo = entity.getWatchProvidersInfo("US")
    assertEquals(2, usInfo.stream.size)
    assertEquals("Netflix", usInfo.stream[0].name)
    assertEquals("Max", usInfo.stream[1].name)
    assertEquals(WatchProviderType.STREAM, usInfo.stream[0].type)

    val inInfo = entity.getWatchProvidersInfo("IN")
    assertEquals(2, inInfo.stream.size)
    assertEquals("JioCinema", inInfo.stream[0].name)
    assertEquals("Prime Video", inInfo.stream[1].name)
  }

  @Test
  fun testWatchProvidersInfoEmptyDetection() {
    val emptyInfo = WatchProvidersInfo(regionCode = "US", regionLabel = "United States")
    assertTrue(emptyInfo.isEmpty)

    val nonEmpty = WatchProvidersInfo(
        regionCode = "US",
        regionLabel = "United States",
        stream = listOf(
            com.example.data.model.WatchProviderItem(
                id = 8,
                name = "Netflix",
                logoUrl = null,
                type = WatchProviderType.STREAM
            )
        )
    )
    assertFalse(nonEmpty.isEmpty)
    assertEquals(1, nonEmpty.allProviders.size)
  }

  @Test
  fun testTrailerInterstitialThreeMinuteCooldown() {
    val startMs = 1_700_000_000_000L
    // First-ever trailer playback is eligible once loaded
    assertTrue(com.example.util.TrailerInterstitialAdManager.shouldShowInterstitialNow(startMs, 0L))
    // 2 minutes 59 seconds later is still inside the 3-minute cooldown
    assertFalse(
        com.example.util.TrailerInterstitialAdManager.shouldShowInterstitialNow(
            nowEpochMs = startMs + 179_999L,
            lastShownEpochMs = startMs
        )
    )
    // Exactly 3 minutes (180,000 ms) later becomes eligible again
    assertTrue(
        com.example.util.TrailerInterstitialAdManager.shouldShowInterstitialNow(
            nowEpochMs = startMs + 180_000L,
            lastShownEpochMs = startMs
        )
    )
  }

  @Test
  fun testMoshiCodegenWithoutKotlinReflect() {
    val moshi = com.squareup.moshi.Moshi.Builder().build()
    val adapter = moshi.adapter(com.example.data.remote.tmdb.TmdbSearchResponse::class.java)
    val json = """
      {
        "page": 1,
        "results": [
          {
            "id": 693134,
            "media_type": "movie",
            "title": "Dune: Part Two",
            "release_date": "2024-02-27",
            "genre_ids": [878, 12],
            "vote_average": 8.2
          }
        ]
      }
    """.trimIndent()
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals(1, parsed!!.results.size)
    assertEquals("Dune: Part Two", parsed.results.first().title)
    assertEquals(693134L, parsed.results.first().id)
  }
}

