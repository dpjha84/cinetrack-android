package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var mediaDao: MediaDao
    private lateinit var preferencesDao: PreferencesDao
    private lateinit var searchHistoryDao: SearchHistoryDao
    private lateinit var customCollectionDao: CustomCollectionDao
    private lateinit var repository: MediaRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaDao = db.mediaDao()
        preferencesDao = db.preferencesDao()
        searchHistoryDao = db.searchHistoryDao()
        customCollectionDao = db.customCollectionDao()
        val tvProgressDao = db.tvProgressDao()
        val favoritePersonDao = db.favoritePersonDao()
        val popularPersonDao = db.popularPersonDao()
        repository = MediaRepository(
            mediaDao,
            preferencesDao,
            searchHistoryDao,
            customCollectionDao,
            tvProgressDao,
            favoritePersonDao,
            popularPersonDao = popularPersonDao
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetMediaEntity() = runTest {
        val media = MediaEntity(
            id = "test_movie_1",
            title = "Inception",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2010,
            genres = "Action, Sci-Fi",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            overview = "A thief who steals corporate secrets through dream-sharing technology.",
            directorOrCreator = "Christopher Nolan",
            cast = "Leonardo DiCaprio, Joseph Gordon-Levitt",
            communityRating = 8.8f,
            runtimeMinutes = 148,
            status = WatchStatus.COMPLETED.name,
            userRating = 9.5f,
            userReview = "Brilliant mind-bending cinema.",
            isFavorite = true
        )

        mediaDao.insertOrUpdate(media)

        val retrieved = mediaDao.getMediaByIdDirect("test_movie_1")
        assertNotNull(retrieved)
        assertEquals("Inception", retrieved?.title)
        assertEquals(WatchStatus.COMPLETED.name, retrieved?.status)
        assertTrue(retrieved?.isFavorite == true)

        val allItems = mediaDao.getAllMedia().first()
        assertEquals(1, allItems.size)
    }

    @Test
    fun updateMediaStatusAndEpisodes() = runTest {
        val series = MediaEntity(
            id = "test_tv_1",
            title = "Breaking Bad",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2008,
            genres = "Crime, Drama",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "Vince Gilligan",
            cast = "Bryan Cranston, Aaron Paul",
            communityRating = 9.5f,
            runtimeMinutes = 47,
            totalSeasons = 5,
            totalEpisodes = 62,
            status = WatchStatus.PLAN_TO_WATCH.name,
            watchedEpisodes = 0
        )

        mediaDao.insertOrUpdate(series)

        // Update to WATCHING with episodes
        val now = System.currentTimeMillis()
        mediaDao.updateStatus("test_tv_1", WatchStatus.WATCHING.name, now)
        mediaDao.updateWatchedEpisodes("test_tv_1", 12, now)

        val updated = mediaDao.getMediaByIdDirect("test_tv_1")
        assertNotNull(updated)
        assertEquals(WatchStatus.WATCHING.name, updated?.status)
        assertEquals(12, updated?.watchedEpisodes)
        assertEquals(now, updated?.lastWatchedDate)

        val continueWatching = mediaDao.getContinueWatching().first()
        assertEquals(1, continueWatching.size)
        assertEquals("Breaking Bad", continueWatching.first().title)
    }

    @Test
    fun deleteMediaItem() = runTest {
        val item = MediaEntity(
            id = "to_delete",
            title = "Disposable Movie",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2020,
            genres = "Comedy",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 5.0f,
            runtimeMinutes = 90
        )
        mediaDao.insertOrUpdate(item)
        assertEquals(1, mediaDao.countTotal())

        mediaDao.deleteById("to_delete")
        assertEquals(0, mediaDao.countTotal())
        assertNull(mediaDao.getMediaByIdDirect("to_delete"))
    }

    @Test
    fun preferencesDaoSaveAndRetrieve() = runTest {
        val prefs = UserPreferencesEntity(
            id = 1,
            regionCode = "GB",
            languageCode = "en",
            autoDetect = false,
            filterCatalogByLanguage = true,
            showStreamingProviders = true,
            customTmdbApiKey = "custom_test_key"
        )

        preferencesDao.savePreferences(prefs)

        val direct = preferencesDao.getPreferencesDirect()
        assertNotNull(direct)
        assertEquals("GB", direct?.regionCode)
        assertFalse(direct?.autoDetect ?: true)
        assertEquals("custom_test_key", direct?.customTmdbApiKey)

        val flowVal = preferencesDao.getPreferences().first()
        assertEquals("GB", flowVal?.regionCode)
    }

    @Test
    fun searchHistoryDaoOperations() = runTest {
        searchHistoryDao.insertOrUpdate(SearchHistoryEntity(query = "Interstellar", timestamp = 1000L))
        searchHistoryDao.insertOrUpdate(SearchHistoryEntity(query = "Oppenheimer", timestamp = 2000L))
        searchHistoryDao.insertOrUpdate(SearchHistoryEntity(query = "Dune", timestamp = 3000L))

        assertEquals(3, searchHistoryDao.count())

        val recent = searchHistoryDao.getRecentSearches(limit = 2).first()
        assertEquals(2, recent.size)
        assertEquals("Dune", recent[0].query)
        assertEquals("Oppenheimer", recent[1].query)

        searchHistoryDao.deleteQuery("Oppenheimer")
        assertEquals(2, searchHistoryDao.count())

        searchHistoryDao.clearAll()
        assertEquals(0, searchHistoryDao.count())
    }

    @Test
    fun customCollectionDaoOperations() = runTest {
        customCollectionDao.insert(CustomCollectionEntity(name = "Sci-Fi Classics", description = "Best of the best"))
        customCollectionDao.insert(CustomCollectionEntity(name = "Weekend Marathon"))

        assertEquals(2, customCollectionDao.count())

        val collections = customCollectionDao.getAllCollections().first()
        assertEquals(2, collections.size)
        assertTrue(collections.any { it.name == "Sci-Fi Classics" })

        customCollectionDao.deleteByName("Weekend Marathon")
        assertEquals(1, customCollectionDao.count())
    }

    @Test
    fun repositorySeedIfEmptyPopulatesData() = runTest {
        assertEquals(0, mediaDao.countTotal())
        assertEquals(0, searchHistoryDao.count())
        assertEquals(0, customCollectionDao.count())
        assertNull(preferencesDao.getPreferencesDirect())

        repository.seedIfEmpty()

        assertTrue(mediaDao.countTotal() > 0)
        assertTrue(searchHistoryDao.count() > 0)
        assertTrue(customCollectionDao.count() > 0)
        assertNotNull(preferencesDao.getPreferencesDirect())

        // Re-seeding should not duplicate or overwrite
        val initialCount = mediaDao.countTotal()
        repository.seedIfEmpty()
        assertEquals(initialCount, mediaDao.countTotal())
    }

    @Test
    fun tvProgressAndSeasonEpisodeTracking() = runTest {
        val tvShow = MediaEntity(
            id = "tv_test_series",
            title = "Mystery Town",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2023,
            genres = "Mystery, Drama",
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            overview = "A mysterious town with dark secrets.",
            directorOrCreator = "Jane Doe",
            cast = "Actor A, Actor B",
            communityRating = 8.5f,
            runtimeMinutes = 45,
            totalSeasons = 2,
            totalEpisodes = 16
        )
        mediaDao.insertOrUpdate(tvShow)

        val seasons = repository.getSeasonsForTvShow(tvShow, "")
        assertEquals(2, seasons.size)

        val season1Episodes = repository.getEpisodesForSeason(tvShow, 1, "")
        assertTrue(season1Episodes.isNotEmpty())

        // Mark season 1 as WATCHED
        repository.setSeasonWatchStatus(tvShow, 1, com.example.data.model.TvWatchStatus.WATCHED)

        val updatedSeasons = repository.getSeasonsForTvShow(tvShow, "")
        val s1 = updatedSeasons.find { it.seasonNumber == 1 }
        assertNotNull(s1)
        assertEquals(com.example.data.model.TvWatchStatus.WATCHED, s1?.watchStatus)

        // Mark specific episode in season 2 as WATCHED
        repository.setEpisodeWatchStatus(tvShow, 2, 1, com.example.data.model.TvWatchStatus.WATCHED)
        val updatedS2Episodes = repository.getEpisodesForSeason(tvShow, 2, "")
        val ep1 = updatedS2Episodes.find { it.episodeNumber == 1 }
        assertEquals(com.example.data.model.TvWatchStatus.WATCHED, ep1?.watchStatus)
    }

    @Test
    fun multiDeviceMergeOverwritesUntrackedLocalAndPreservesNewerLocal() = runTest {
        val localUntracked = MediaEntity(
            id = "tmdb_m_999",
            title = "Device B Cached Movie",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2025,
            genres = "Action",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.0f,
            runtimeMinutes = 120,
            status = WatchStatus.NONE.name,
            isFavorite = false
        )
        mediaDao.insertOrUpdate(localUntracked)

        val cloudTracked = localUntracked.copy(
            status = WatchStatus.COMPLETED.name,
            isFavorite = true,
            userRating = 4.5f,
            userReview = "Synced from Device A",
            lastWatchedDate = 2000L
        )
        repository.mergeCloudMedia(listOf(cloudTracked))

        val merged = mediaDao.getMediaByIdDirect("tmdb_m_999")
        assertNotNull(merged)
        assertEquals(WatchStatus.COMPLETED.name, merged?.status)
        assertTrue(merged?.isFavorite == true)
        assertEquals("Synced from Device A", merged?.userReview)

        // Newer local tracking should not be overwritten by older cloud copy
        val newerLocal = cloudTracked.copy(userReview = "Updated on Device B", lastWatchedDate = 5000L)
        mediaDao.insertOrUpdate(newerLocal)
        repository.mergeCloudMedia(listOf(cloudTracked.copy(userReview = "Older Cloud", lastWatchedDate = 2000L)))
        assertEquals("Updated on Device B", mediaDao.getMediaByIdDirect("tmdb_m_999")?.userReview)
    }

    @Test
    fun favoritePersonAndTvProgressMergePersistCorrectly() = runTest {
        val actor = FavoritePersonEntity(
            id = "tmdb_p_6193",
            personId = 6193L,
            name = "Leonardo DiCaprio",
            department = "Acting"
        )
        repository.toggleFavoritePerson(actor)
        assertEquals(1, db.favoritePersonDao().getAllFavoritePeopleList().size)

        // Toggling again removes it
        repository.toggleFavoritePerson(actor)
        assertEquals(0, db.favoritePersonDao().getAllFavoritePeopleList().size)

        // TV progress merge picks newer timestamp
        val older = TvProgressEntity("tmdb_tv_100", 1, 2, "WATCHING", updatedAt = 1000L)
        val newer = TvProgressEntity("tmdb_tv_100", 1, 2, "WATCHED", updatedAt = 3000L)
        db.tvProgressDao().insertOrUpdate(older)
        repository.mergeCloudTvProgress(listOf(newer))
        assertEquals("WATCHED", db.tvProgressDao().getSpecificProgress("tmdb_tv_100", 1, 2)?.status)
    }

    @Test
    fun clearAllUserDataWipesTrackedLibraryAndCustomEntries() = runTest {
        val trackedCatalogItem = MediaEntity(
            id = "tmdb_m_550",
            title = "Fight Club",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 1999,
            genres = "Drama",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "David Fincher",
            cast = "Brad Pitt",
            communityRating = 8.8f,
            runtimeMinutes = 139,
            status = WatchStatus.COMPLETED.name,
            userRating = 9.0f,
            userReview = "Classic",
            isFavorite = true,
            customWatchlist = "Must Watch",
            lastWatchedDate = 9000L
        )
        val customUserItem = trackedCatalogItem.copy(
            id = "custom_123",
            title = "My Private Indie Film",
            isCustom = true
        )
        mediaDao.insertOrUpdate(trackedCatalogItem)
        mediaDao.insertOrUpdate(customUserItem)
        db.favoritePersonDao().insert(
            FavoritePersonEntity(id = "tmdb_p_287", personId = 287L, name = "Brad Pitt")
        )
        db.tvProgressDao().insertOrUpdate(TvProgressEntity("tmdb_tv_1", 1, 1, "WATCHED", 5000L))
        customCollectionDao.insert(CustomCollectionEntity(name = "Must Watch"))
        searchHistoryDao.insertOrUpdate(SearchHistoryEntity("Fight Club", 5000L))

        repository.clearAllUserData()

        assertTrue(mediaDao.getTrackedMedia().first().isEmpty())
        assertTrue(mediaDao.getFavorites().first().isEmpty())
        assertTrue(mediaDao.getWatchlistMedia().first().isEmpty())
        assertNull(mediaDao.getMediaByIdDirect("custom_123"))
        val catalogAfterClear = mediaDao.getMediaByIdDirect("tmdb_m_550")
        assertNotNull(catalogAfterClear)
        assertEquals(WatchStatus.NONE.name, catalogAfterClear?.status)
        assertFalse(catalogAfterClear?.isFavorite ?: true)
        assertNull(catalogAfterClear?.customWatchlist)
        assertEquals(0, db.favoritePersonDao().getAllFavoritePeopleList().size)
        assertEquals(0, db.tvProgressDao().getAllProgressList().size)
        assertEquals(0, customCollectionDao.count())
        assertEquals(0, searchHistoryDao.count())
    }

    @Test
    fun incrementAndUndoEpisodeAdvancesUpNextAndSeasons() = runTest {
        val tvShow = MediaEntity(
            id = "tv_stranger_things",
            title = "Stranger Things",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2016,
            genres = "Sci-Fi, Drama",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "The Duffer Brothers",
            cast = "Winona Ryder",
            communityRating = 8.7f,
            runtimeMinutes = 50,
            totalSeasons = 4,
            totalEpisodes = 34,
            watchedEpisodes = 0
        )
        mediaDao.insertOrUpdate(tvShow)

        // Initial unstarted state: Season 1 has 0 watched (Up next = S1 E1)
        val initialSeasons = repository.getSeasonsForTvShow(tvShow, "")
        assertEquals(0, initialSeasons.first().watchedEpisodesCount)

        // Click Mark S1·E1 Watched -> Season 1 watchedEpisodesCount becomes 1 (Up next = S1 E2)
        repository.incrementEpisode("tv_stranger_things", "")
        val afterE1Media = mediaDao.getMediaByIdDirect("tv_stranger_things")!!
        assertEquals(1, afterE1Media.watchedEpisodes)
        assertEquals(WatchStatus.WATCHING.name, afterE1Media.status)

        val seasonsAfterE1 = repository.getSeasonsForTvShow(afterE1Media, "")
        assertEquals(1, seasonsAfterE1.first { it.seasonNumber == 1 }.watchedEpisodesCount)
        val s1EpisodesAfterE1 = repository.getEpisodesForSeason(afterE1Media, 1, "")
        assertEquals(com.example.data.model.TvWatchStatus.WATCHED, s1EpisodesAfterE1.first { it.episodeNumber == 1 }.watchStatus)
        assertEquals(com.example.data.model.TvWatchStatus.UNWATCHED, s1EpisodesAfterE1.first { it.episodeNumber == 2 }.watchStatus)

        // Click Mark S1·E2 Watched -> Season 1 watchedEpisodesCount becomes 2 (Up next = S1 E3)
        repository.incrementEpisode("tv_stranger_things", "")
        val afterE2Media = mediaDao.getMediaByIdDirect("tv_stranger_things")!!
        assertEquals(2, afterE2Media.watchedEpisodes)
        val seasonsAfterE2 = repository.getSeasonsForTvShow(afterE2Media, "")
        assertEquals(2, seasonsAfterE2.first { it.seasonNumber == 1 }.watchedEpisodesCount)

        // Undo from 2 -> 1 reverts S1 E2 back to UNWATCHED (Up next returns to S1 E2)
        repository.updateWatchedEpisodes("tv_stranger_things", 1, "")
        val afterUndo1 = mediaDao.getMediaByIdDirect("tv_stranger_things")!!
        assertEquals(1, afterUndo1.watchedEpisodes)
        val seasonsAfterUndo1 = repository.getSeasonsForTvShow(afterUndo1, "")
        assertEquals(1, seasonsAfterUndo1.first { it.seasonNumber == 1 }.watchedEpisodesCount)

        // Undo from 1 -> 0 resets back to Not Started (0 watched, status NONE)
        repository.updateWatchedEpisodes("tv_stranger_things", 0, "")
        val afterUndo0 = mediaDao.getMediaByIdDirect("tv_stranger_things")!!
        assertEquals(0, afterUndo0.watchedEpisodes)
        assertEquals(WatchStatus.NONE.name, afterUndo0.status)
        val seasonsAfterUndo0 = repository.getSeasonsForTvShow(afterUndo0, "")
        assertEquals(0, seasonsAfterUndo0.first { it.seasonNumber == 1 }.watchedEpisodesCount)
    }

    @Test
    fun homeSectionFeedTagUpsertAndPagedDbFiltering() = runTest {
        val trackedMovie = MediaEntity(
            id = "tmdb_m_101",
            title = "Tracked Blockbuster",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2025,
            genres = "Action, Sci-Fi",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.9f,
            runtimeMinutes = 130,
            status = WatchStatus.WATCHING.name,
            userRating = 9.0f,
            isFavorite = true,
            feedTags = com.example.data.model.HomeCatalogSection.POPULAR.feedTag
        )
        mediaDao.insertOrUpdate(trackedMovie)

        // Now upsert the same movie as NOW_PLAYING with untracked default state -> user tracking MUST survive and tag MUST merge
        val incomingNowPlaying = trackedMovie.copy(
            status = WatchStatus.NONE.name,
            userRating = 0f,
            isFavorite = false,
            feedTags = ""
        )
        val freshUpcomingTv = MediaEntity(
            id = "tmdb_tv_202",
            title = "Upcoming Galaxy Show",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2026,
            genres = "Sci-Fi",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.4f,
            runtimeMinutes = 48,
            feedTags = ""
        )

        repository.upsertWithFeedTag(
            items = listOf(incomingNowPlaying, freshUpcomingTv),
            feedTag = com.example.data.model.HomeCatalogSection.NOW_PLAYING.feedTag
        )
        repository.upsertWithFeedTag(
            items = listOf(freshUpcomingTv),
            feedTag = com.example.data.model.HomeCatalogSection.UPCOMING.feedTag
        )

        val mergedMovie = mediaDao.getMediaByIdDirect("tmdb_m_101")!!
        assertEquals(WatchStatus.WATCHING.name, mergedMovie.status)
        assertTrue(mergedMovie.isFavorite)
        assertTrue(mergedMovie.hasFeedTag(com.example.data.model.HomeCatalogSection.POPULAR.feedTag))
        assertTrue(mergedMovie.hasFeedTag(com.example.data.model.HomeCatalogSection.NOW_PLAYING.feedTag))

        // Verify section + mediaType paged queries
        val nowPlayingAll = repository.getPagedHomeCatalogFromDb(
            section = com.example.data.model.HomeCatalogSection.NOW_PLAYING,
            mediaType = null,
            genreFilter = null,
            languageFilter = null,
            limit = 10,
            offset = 0
        )
        assertEquals(2, nowPlayingAll.size)

        val nowPlayingTvOnly = repository.getPagedHomeCatalogFromDb(
            section = com.example.data.model.HomeCatalogSection.NOW_PLAYING,
            mediaType = MediaType.TV_SHOW,
            genreFilter = null,
            languageFilter = null,
            limit = 10,
            offset = 0
        )
        assertEquals(1, nowPlayingTvOnly.size)
        assertEquals("tmdb_tv_202", nowPlayingTvOnly.first().id)

        val upcomingMoviesOnly = repository.getPagedHomeCatalogFromDb(
            section = com.example.data.model.HomeCatalogSection.UPCOMING,
            mediaType = MediaType.MOVIE,
            genreFilter = null,
            languageFilter = null,
            limit = 10,
            offset = 0
        )
        assertTrue(upcomingMoviesOnly.isEmpty())

        // Verify offset paging
        val page2Single = repository.getPagedHomeCatalogFromDb(
            section = com.example.data.model.HomeCatalogSection.NOW_PLAYING,
            mediaType = null,
            genreFilter = null,
            languageFilter = null,
            limit = 1,
            offset = 1
        )
        assertEquals(1, page2Single.size)
    }

    @Test
    fun popularPeopleRoomCachingAndPaging() = runTest {
        val movieEntity = MediaEntity(
            id = "tmdb_m_901",
            title = "Space Horizon",
            mediaType = MediaType.MOVIE.name,
            releaseYear = 2025,
            genres = "Sci-Fi",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.3f,
            runtimeMinutes = 130
        )
        val tvEntity = MediaEntity(
            id = "tmdb_tv_902",
            title = "City Detectives",
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = 2025,
            genres = "Crime",
            posterUrl = "",
            backdropUrl = "",
            overview = "",
            directorOrCreator = "",
            cast = "",
            communityRating = 8.6f,
            runtimeMinutes = 45
        )
        val starOne = com.example.data.model.TmdbSearchResultItem.Person(
            id = 1001L,
            name = "Aria Vance",
            profileUrl = null,
            department = "Acting",
            knownFor = listOf(movieEntity),
            popularity = 95.0
        )
        val starTwo = com.example.data.model.TmdbSearchResultItem.Person(
            id = 1002L,
            name = "Marcus Hale",
            profileUrl = null,
            department = "Directing",
            knownFor = listOf(tvEntity),
            popularity = 82.0
        )

        repository.cachePopularPeopleInDb(listOf(starOne, starTwo), pageLoaded = 1)

        val allPeople = repository.popularPeople.first()
        assertEquals(2, allPeople.size)
        assertEquals("Aria Vance", allPeople.first().name)

        val moviePeople = repository.getPagedPopularPeopleFromDb(
            mediaType = MediaType.MOVIE,
            limit = 10,
            offset = 0
        )
        assertEquals(1, moviePeople.size)
        assertEquals(1001L, moviePeople.first().personId)

        val tvPeople = repository.getPagedPopularPeopleFromDb(
            mediaType = MediaType.TV_SHOW,
            limit = 10,
            offset = 0
        )
        assertEquals(1, tvPeople.size)
        assertEquals(1002L, tvPeople.first().personId)

        val pageOffset = repository.getPagedPopularPeopleFromDb(
            mediaType = null,
            limit = 1,
            offset = 1
        )
        assertEquals(1, pageOffset.size)
        assertEquals(1002L, pageOffset.first().personId)
    }
}
