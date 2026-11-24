package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCachePolicyTest {

    private fun sampleMedia(
        id: String,
        title: String,
        status: WatchStatus = WatchStatus.NONE,
        isFavorite: Boolean = false,
        customWatchlist: String? = null,
        rating: Float = 8.0f,
        releaseYear: Int = 2024
    ): MediaEntity = MediaEntity(
        id = id,
        title = title,
        mediaType = MediaType.MOVIE.name,
        releaseYear = releaseYear,
        genres = "Sci-Fi, Drama",
        posterUrl = "https://image.tmdb.org/t/p/w500/$id.jpg",
        backdropUrl = "",
        overview = "Sample overview for $title",
        directorOrCreator = "Director",
        cast = "Actor One",
        communityRating = rating,
        runtimeMinutes = 120,
        status = status.name,
        isFavorite = isFavorite,
        customWatchlist = customWatchlist
    )

    @Test
    fun normalizeSearchQueryTrimsCollapsesWhitespaceAndLowercases() {
        assertEquals("christopher nolan", OfflineCachePolicy.normalizeSearchQuery("  Christopher   Nolan  "))
        assertEquals("dune: part two", OfflineCachePolicy.normalizeSearchQuery("DUNE:   PART   TWO"))
        assertEquals("", OfflineCachePolicy.normalizeSearchQuery("   "))
    }

    @Test
    fun mergeCatalogMetadataPreservesUserTrackingAndUpgradesProviders() {
        val existingTracked = sampleMedia(
            id = "tmdb_m_101",
            title = "Dune: Part Two",
            status = WatchStatus.WATCHING,
            isFavorite = true,
            customWatchlist = "Weekend Sci-Fi",
            rating = 8.5f
        ).copy(
            userRating = 9.5f,
            userReview = "Masterpiece",
            watchedEpisodes = 1,
            lastWatchedDate = 123456L,
            directorOrCreator = "Unknown Director",
            streamingProviders = "GLOBAL:Netflix",
            feedTags = "POPULAR"
        )

        val incomingCatalog = sampleMedia(
            id = "tmdb_m_101",
            title = "Dune: Part Two",
            status = WatchStatus.NONE,
            isFavorite = false,
            customWatchlist = null,
            rating = 8.8f
        ).copy(
            userRating = 0f,
            userReview = "",
            watchedEpisodes = 0,
            lastWatchedDate = null,
            directorOrCreator = "Denis Villeneuve",
            cast = "Timothee Chalamet, Zendaya, Rebecca Ferguson",
            streamingProviders = "US:Max,Prime Video",
            feedTags = "TRENDING"
        )

        val merged = OfflineCachePolicy.mergeCatalogMetadataPreservingUserTracking(
            existing = existingTracked,
            incoming = incomingCatalog,
            extraFeedTag = "TOP_RATED_MOVIE"
        )

        // User tracking state strictly preserved
        assertEquals(WatchStatus.WATCHING.name, merged.status)
        assertTrue(merged.isFavorite)
        assertEquals("Weekend Sci-Fi", merged.customWatchlist)
        assertEquals(9.5f, merged.userRating, 0.01f)
        assertEquals("Masterpiece", merged.userReview)
        assertEquals(1, merged.watchedEpisodes)
        assertEquals(123456L, merged.lastWatchedDate)

        // Fresh catalog metadata & regional providers merged cleanly
        assertEquals("Denis Villeneuve", merged.directorOrCreator)
        assertTrue(merged.cast.contains("Zendaya"))
        assertEquals(8.8f, merged.communityRating, 0.01f)
        assertTrue(merged.streamingProviders.contains("US:Max,Prime Video"))
        assertTrue(merged.hasFeedTag("POPULAR"))
        assertTrue(merged.hasFeedTag("TRENDING"))
        assertTrue(merged.hasFeedTag("TOP_RATED_MOVIE"))
    }

    @Test
    fun rankOfflineMediaMatchesPrioritizesWatchlistAndExactTitle() {
        val untrackedHighRated = sampleMedia(
            id = "m_1",
            title = "Solaris Echo",
            status = WatchStatus.NONE,
            rating = 9.5f
        )
        val savedWatchlist = sampleMedia(
            id = "m_2",
            title = "Solaris Chronicles",
            status = WatchStatus.PLAN_TO_WATCH,
            rating = 7.4f
        )
        val savedCustomList = sampleMedia(
            id = "m_3",
            title = "Solaris",
            status = WatchStatus.NONE,
            customWatchlist = "Space Night",
            rating = 7.8f
        )

        val ranked = OfflineCachePolicy.rankOfflineMediaMatches(
            items = listOf(untrackedHighRated, savedWatchlist, savedCustomList),
            query = "  solaris "
        )

        assertEquals(3, ranked.size)
        // Both saved/watchlist items come before untracked higher-rated item; exact title wins tie
        assertEquals("m_3", ranked[0].id)
        assertEquals("m_2", ranked[1].id)
        assertEquals("m_1", ranked[2].id)
    }

    @Test
    fun rankOfflinePeopleMatchesPrioritizesExactAndPrefixMatches() {
        val related = TmdbSearchResultItem.Person(
            id = 1L,
            name = "John Nolan",
            profileUrl = null,
            department = "Writing",
            knownFor = emptyList(),
            popularity = 99.0
        )
        val exact = TmdbSearchResultItem.Person(
            id = 2L,
            name = "Nolan",
            profileUrl = null,
            department = "Directing",
            knownFor = emptyList(),
            popularity = 10.0
        )
        val prefix = TmdbSearchResultItem.Person(
            id = 3L,
            name = "Nolan North",
            profileUrl = null,
            department = "Acting",
            knownFor = emptyList(),
            popularity = 40.0
        )

        val ranked = OfflineCachePolicy.rankOfflinePeopleMatches(
            people = listOf(related, prefix, exact),
            query = "nolan"
        )

        assertEquals(listOf(2L, 3L, 1L), ranked.map { it.id })
    }
}
