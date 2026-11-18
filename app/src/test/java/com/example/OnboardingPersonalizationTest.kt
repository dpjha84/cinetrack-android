package com.example

import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.example.util.PersonalizationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPersonalizationTest {

    private fun buildMedia(
        id: String,
        title: String,
        genres: String = "Drama",
        director: String = "",
        cast: String = "",
        status: WatchStatus = WatchStatus.NONE,
        isFavorite: Boolean = false,
        userRating: Float = 0f,
        communityRating: Float = 7.5f,
        originCountry: String = "US",
        posterUrl: String = "https://image.tmdb.org/t/p/w500/poster.jpg"
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = MediaType.MOVIE.name,
        releaseYear = 2024,
        genres = genres,
        posterUrl = posterUrl,
        backdropUrl = "",
        overview = "Overview",
        directorOrCreator = director,
        cast = cast,
        communityRating = communityRating,
        runtimeMinutes = 120,
        status = status.name,
        isFavorite = isFavorite,
        userRating = userRating,
        originCountry = originCountry
    )

    @Test
    fun extractTopGenres_weightsCompletedAndFavoritesHighest() {
        val tracked = listOf(
            buildMedia("m1", "Dune 2", genres = "Sci-Fi, Adventure", status = WatchStatus.COMPLETED, isFavorite = true),
            buildMedia("m2", "Arrival", genres = "Sci-Fi, Drama", status = WatchStatus.COMPLETED),
            buildMedia("m3", "RomCom", genres = "Comedy, Romance", status = WatchStatus.PLAN_TO_WATCH)
        )

        val top = PersonalizationEngine.extractTopGenres(tracked, limit = 2)
        assertEquals("Sci-Fi", top.first())
        assertTrue(top.contains("Adventure") || top.contains("Drama"))

        val withExplicitPreferred = PersonalizationEngine.extractTopGenres(
            trackedMedia = listOf(
                buildMedia("m4", "Hearted Only", genres = "Thriller, Mystery", status = WatchStatus.NONE, isFavorite = true)
            ),
            preferredGenres = setOf("Crime", "Mystery"),
            limit = 3
        )
        assertEquals("Mystery", withExplicitPreferred.first())
        assertTrue(withExplicitPreferred.contains("Crime"))
        assertTrue(withExplicitPreferred.contains("Thriller"))
    }

    @Test
    fun rankAndDeduplicateRecommendations_prioritizesFavoriteActorsAndExcludesWatched() {
        val watchedDune = buildMedia(
            id = "tmdb_m_693134",
            title = "Dune: Part Two",
            genres = "Sci-Fi, Adventure",
            director = "Denis Villeneuve",
            cast = "Timothée Chalamet, Zendaya",
            status = WatchStatus.COMPLETED
        )
        val favoritedOnly = buildMedia(
            id = "tmdb_m_27205",
            title = "Inception",
            genres = "Sci-Fi, Action",
            director = "Christopher Nolan",
            status = WatchStatus.NONE,
            isFavorite = true
        )
        val favPeople = listOf(
            FavoritePersonEntity(id = "tmdb_p_505710", personId = 505710L, name = "Zendaya"),
            FavoritePersonEntity(id = "tmdb_p_525", personId = 525L, name = "Christopher Nolan")
        )

        val candidates = listOf(
            // Duplicate of already watched item — must be excluded
            buildMedia(id = "local_dune", title = "Dune: Part Two", genres = "Sci-Fi", cast = "Zendaya"),
            // Duplicate of favorited-only item — must be excluded
            buildMedia(id = "local_inception", title = "Inception", genres = "Sci-Fi", director = "Christopher Nolan"),
            // Unrelated comedy
            buildMedia(id = "tmdb_m_200", title = "Generic Comedy", genres = "Comedy", cast = "Someone Else", communityRating = 8.0f),
            // Zendaya match — should rank top
            buildMedia(id = "tmdb_m_101", title = "Challengers", genres = "Drama, Romance", cast = "Zendaya", communityRating = 7.9f),
            // Nolan + Sci-Fi match — should rank top
            buildMedia(id = "tmdb_m_102", title = "Tenet", genres = "Sci-Fi, Action", director = "Christopher Nolan", communityRating = 7.4f)
        )

        val ranked = PersonalizationEngine.rankAndDeduplicateRecommendations(
            candidates = candidates,
            trackedMedia = listOf(watchedDune, favoritedOnly),
            favoritePeople = favPeople,
            preferredGenres = setOf("Sci-Fi"),
            regionCode = "US",
            maxItems = 10
        )

        assertFalse(ranked.any { it.title == "Dune: Part Two" })
        assertFalse(ranked.any { it.title == "Inception" })
        assertEquals(3, ranked.size)
        val topTwoIds = ranked.take(2).map { it.id }.toSet()
        assertTrue(topTwoIds.contains("tmdb_m_101"))
        assertTrue(topTwoIds.contains("tmdb_m_102"))
    }

    @Test
    fun rankAndDeduplicateRecommendations_worksWithPreferredGenresOnly() {
        val candidates = listOf(
            buildMedia(id = "tmdb_m_10", title = "Blade Runner 2049", genres = "Sci-Fi, Drama", communityRating = 8.0f),
            buildMedia(id = "tmdb_m_11", title = "Slapstick Comedy", genres = "Comedy", communityRating = 8.2f)
        )
        val ranked = PersonalizationEngine.rankAndDeduplicateRecommendations(
            candidates = candidates,
            trackedMedia = emptyList(),
            favoritePeople = emptyList(),
            preferredGenres = setOf("Sci-Fi"),
            regionCode = "US"
        )
        assertEquals("tmdb_m_10", ranked.first().id)
    }

    @Test
    fun buildRecommendationReason_explainsStarOrGenreMatch() {
        val favPeople = listOf(
            FavoritePersonEntity(id = "tmdb_p_505710", personId = 505710L, name = "Zendaya")
        )
        val starCandidate = buildMedia(
            id = "tmdb_m_101",
            title = "Challengers",
            genres = "Drama",
            cast = "Zendaya, Mike Faist"
        )
        val genreCandidate = buildMedia(
            id = "tmdb_m_102",
            title = "Arrival",
            genres = "Sci-Fi, Drama",
            cast = "Amy Adams"
        )

        assertEquals(
            "★ Zendaya",
            PersonalizationEngine.buildRecommendationReason(
                candidate = starCandidate,
                trackedMedia = emptyList(),
                favoritePeople = favPeople,
                preferredGenres = setOf("Sci-Fi")
            )
        )
        assertEquals(
            "Sci-Fi Pick",
            PersonalizationEngine.buildRecommendationReason(
                candidate = genreCandidate,
                trackedMedia = emptyList(),
                favoritePeople = favPeople,
                preferredGenres = setOf("Sci-Fi")
            )
        )
    }

    @Test
    fun rankAndDeduplicateRecommendations_returnsEmptyWithoutSignals() {
        val candidates = listOf(
            buildMedia(id = "tmdb_m_1", title = "Some Movie", genres = "Action")
        )
        val ranked = PersonalizationEngine.rankAndDeduplicateRecommendations(
            candidates = candidates,
            trackedMedia = emptyList(),
            favoritePeople = emptyList(),
            preferredGenres = emptySet(),
            regionCode = "US"
        )
        assertTrue(ranked.isEmpty())
    }
}
