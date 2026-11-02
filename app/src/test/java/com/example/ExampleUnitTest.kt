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
}

