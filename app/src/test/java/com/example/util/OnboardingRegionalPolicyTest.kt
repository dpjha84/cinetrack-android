package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRegionalPolicyTest {

    private fun media(
        id: String,
        title: String,
        type: MediaType = MediaType.MOVIE,
        originCountry: String = "US",
        providers: String = "GLOBAL:Netflix",
        posterUrl: String = "https://image.tmdb.org/t/p/w500/$id.jpg"
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = type.name,
        releaseYear = 2024,
        genres = "Drama",
        posterUrl = posterUrl,
        backdropUrl = "",
        overview = "",
        directorOrCreator = "",
        cast = "",
        communityRating = 8.4f,
        runtimeMinutes = 120,
        originCountry = originCountry,
        streamingProviders = providers
    )

    private fun person(
        id: Long,
        name: String,
        knownFor: List<MediaEntity> = emptyList(),
        popularity: Double = 50.0
    ): TmdbSearchResultItem.Person = TmdbSearchResultItem.Person(
        id = id,
        name = name,
        profileUrl = "https://image.tmdb.org/t/p/w500/$id.jpg",
        department = "Acting",
        knownFor = knownFor,
        popularity = popularity
    )

    @Test
    fun normalizeRegionCodeHandlesAutoGlobalAndLowercase() {
        assertNull(OnboardingRegionalPolicy.normalizeRegionCode(null))
        assertNull(OnboardingRegionalPolicy.normalizeRegionCode(""))
        assertNull(OnboardingRegionalPolicy.normalizeRegionCode("AUTO"))
        assertNull(OnboardingRegionalPolicy.normalizeRegionCode("global"))
        assertEquals("IN", OnboardingRegionalPolicy.normalizeRegionCode(" in "))
        assertEquals("JP", OnboardingRegionalPolicy.normalizeRegionCode("jp"))
    }

    @Test
    fun prioritizeOnboardingTitlesForRegionLeadsWithRegionalOriginAndDeduplicates() {
        val inMovie = media("tmdb_m_1", "Kalki 2898 AD", MediaType.MOVIE, originCountry = "IN")
        val inTv = media("tmdb_tv_2", "Mirzapur", MediaType.TV_SHOW, originCountry = "IN")
        val usPopular = media("tmdb_m_3", "Dune: Part Two", MediaType.MOVIE, originCountry = "US")
        val globalTv = media("tmdb_tv_4", "The Bear", MediaType.TV_SHOW, originCountry = "US")
        val noArt = media("tmdb_m_5", "No Art", posterUrl = "")

        val combined = OnboardingRegionalPolicy.prioritizeOnboardingTitlesForRegion(
            regionalOriginMovies = listOf(inMovie, noArt),
            regionalOriginTv = listOf(inTv),
            popularInRegionMovies = listOf(usPopular, inMovie),
            popularInRegionTv = listOf(globalTv),
            topRatedMovies = emptyList(),
            topRatedTv = emptyList(),
            regionCode = "IN",
            limit = 10
        )

        assertEquals(listOf("tmdb_m_1", "tmdb_tv_2", "tmdb_m_3", "tmdb_tv_4"), combined.map { it.id })
    }

    @Test
    fun rankFallbackTitlesForRegionPlacesOriginThenProviderMatchesFirst() {
        val usOnly = media("m_us", "US Film", originCountry = "US", providers = "US:Max")
        val krStreamIn = media("tv_kr", "K-Drama", MediaType.TV_SHOW, originCountry = "KR", providers = "IN:Netflix|GLOBAL:Netflix")
        val inMovie = media("m_in", "Indian Hit", originCountry = "IN", providers = "IN:Prime Video")
        val inTv = media("tv_in", "Indian Series", MediaType.TV_SHOW, originCountry = "IN", providers = "IN:Netflix")

        val ranked = OnboardingRegionalPolicy.rankFallbackTitlesForRegion(
            pool = listOf(usOnly, krStreamIn, inMovie, inTv),
            regionCode = "IN",
            limit = 10
        )

        assertEquals("m_in", ranked[0].id)
        assertEquals("tv_in", ranked[1].id)
        assertTrue(ranked.indexOfFirst { it.id == "tv_kr" } < ranked.indexOfFirst { it.id == "m_us" })
    }

    @Test
    fun prioritizeOnboardingPeopleForRegionPlacesRegionalStarsBeforeGlobal() {
        val inMovie = media("m_in", "Stree 2", originCountry = "IN")
        val usMovie = media("m_us", "Oppenheimer", originCountry = "US")

        val regionalStar = person(101L, "Rajkummar Rao", knownFor = listOf(inMovie), popularity = 40.0)
        val regionalSampleDirector = person(102L, "Nitesh Tiwari", knownFor = listOf(inMovie), popularity = 30.0)
        val globalStar = person(201L, "Cillian Murphy", knownFor = listOf(usMovie), popularity = 90.0)
        val duplicateGlobal = person(101L, "Rajkummar Rao", knownFor = listOf(inMovie), popularity = 40.0)

        val merged = OnboardingRegionalPolicy.prioritizeOnboardingPeopleForRegion(
            regionalStars = listOf(regionalStar),
            globalPopularPeople = listOf(globalStar, duplicateGlobal),
            samplePeople = listOf(regionalSampleDirector),
            regionCode = "IN",
            limit = 10
        )

        assertEquals(listOf(101L, 102L, 201L), merged.map { it.id })
    }
}
