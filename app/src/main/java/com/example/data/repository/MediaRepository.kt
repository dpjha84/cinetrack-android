package com.example.data.repository

import java.text.SimpleDateFormat
import java.util.Locale
import com.example.data.local.CustomCollectionDao
import com.example.data.local.CustomCollectionEntity
import com.example.data.local.FavoritePersonDao
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaEntity
import com.example.data.local.PreferencesDao
import com.example.data.local.SampleMediaData
import com.example.data.local.SampleTrailerData
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.CastMemberItem
import com.example.data.model.MediaType
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.TrailerItem
import com.example.data.model.WatchProviderItem
import com.example.data.model.WatchProviderType
import com.example.data.model.WatchProvidersInfo
import com.example.data.model.WatchStatus
import com.example.data.remote.firebase.FirestoreSyncManager
import com.example.data.remote.tmdb.TmdbClient
import com.example.data.remote.tmdb.TmdbMediaDto
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.data.remote.tmdb.TmdbVideoDto
import com.example.data.local.SampleTvEpisodeData
import com.example.data.local.TvProgressDao
import com.example.data.local.TvProgressEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.TvWatchStatus
import com.example.util.JustWatchScraper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Calendar

class MediaRepository(
    private val mediaDao: MediaDao,
    private val preferencesDao: PreferencesDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val collectionDao: CustomCollectionDao,
    private val tvProgressDao: TvProgressDao,
    private val favoritePersonDao: FavoritePersonDao,
    private val firestoreSyncManager: FirestoreSyncManager? = null
) {

    private val syncMutex = Mutex()

    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()
    val trackedMedia: Flow<List<MediaEntity>> = mediaDao.getTrackedMedia()
    val continueWatching: Flow<List<MediaEntity>> = mediaDao.getContinueWatching()
    val favorites: Flow<List<MediaEntity>> = mediaDao.getFavorites()
    val favoritePeople: Flow<List<FavoritePersonEntity>> = favoritePersonDao.getAllFavoritePeople()
    val watchlistMedia: Flow<List<MediaEntity>> = mediaDao.getWatchlistMedia()
    val userPreferences: Flow<UserPreferencesEntity?> = preferencesDao.getPreferences()
    val searchHistory: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()
    val savedCollections: Flow<List<CustomCollectionEntity>> = collectionDao.getAllCollections()

    private fun currentAuthenticatedUserId(): String? {
        if (firestoreSyncManager == null) return null
        return try {
            Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun isMediaTracked(item: MediaEntity): Boolean {
        return item.status != "NONE" ||
            item.isFavorite ||
            item.userRating > 0f ||
            !item.customWatchlist.isNullOrBlank() ||
            item.watchedEpisodes > 0
    }

    suspend fun mergeCloudMedia(cloudMedia: List<MediaEntity>) {
        cloudMedia.forEach { cloudItem ->
            val localItem = mediaDao.getMediaByIdDirect(cloudItem.id)
            when {
                localItem == null -> mediaDao.insertOrUpdate(cloudItem)
                !isMediaTracked(localItem) -> mediaDao.insertOrUpdate(cloudItem)
                else -> {
                    val cloudTs = cloudItem.lastWatchedDate ?: 0L
                    val localTs = localItem.lastWatchedDate ?: 0L
                    if (cloudTs > localTs) {
                        mediaDao.insertOrUpdate(
                            cloudItem.copy(
                                posterUrl = cloudItem.posterUrl.ifBlank { localItem.posterUrl },
                                backdropUrl = cloudItem.backdropUrl.ifBlank { localItem.backdropUrl }
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun mergeCloudTvProgress(cloudProgress: List<TvProgressEntity>) {
        cloudProgress.forEach { cloudProg ->
            val localProg = tvProgressDao.getSpecificProgress(
                cloudProg.mediaId,
                cloudProg.seasonNumber,
                cloudProg.episodeNumber
            )
            if (localProg == null || cloudProg.updatedAt > localProg.updatedAt) {
                tvProgressDao.insertOrUpdate(cloudProg)
            }
        }
    }

    suspend fun syncWithCloud(userId: String) = withContext(Dispatchers.IO) {
        val sync = firestoreSyncManager ?: return@withContext
        if (userId.isBlank()) return@withContext

        syncMutex.withLock {
            // Prevent cross-account contamination if previous local data belonged to a different user
            val previousUid = sync.getLastSyncedUserId()
            if (previousUid != null && previousUid != userId) {
                clearLocalUserDataLocked()
            }
            sync.setLastSyncedUserId(userId)

            // 1. Pull cloud data and smart-merge into Room local database
            val cloudData = sync.pullCloudData(userId)
            if (cloudData != null) {
                if (cloudData.media.isNotEmpty()) {
                    mergeCloudMedia(cloudData.media)
                }
                if (cloudData.tvProgress.isNotEmpty()) {
                    mergeCloudTvProgress(cloudData.tvProgress)
                }
                if (cloudData.collections.isNotEmpty()) {
                    collectionDao.insertAll(cloudData.collections)
                }
                if (cloudData.preferences != null) {
                    val existingPrefs = preferencesDao.getPreferencesDirect()
                    preferencesDao.savePreferences(
                        cloudData.preferences.copy(
                            customTmdbApiKey = existingPrefs?.customTmdbApiKey.orEmpty()
                        )
                    )
                }
                if (cloudData.searchHistory.isNotEmpty()) {
                    searchHistoryDao.insertAll(cloudData.searchHistory)
                }
                if (cloudData.favoritePeople.isNotEmpty()) {
                    favoritePersonDao.insertAll(cloudData.favoritePeople)
                }
            }

            // 2. Push local tracked content and newer progress to cloud
            val localTracked = try {
                mediaDao.getAllMediaList().filter { isMediaTracked(it) }
            } catch (_: Exception) {
                emptyList()
            }
            val cloudMediaMap = cloudData?.media?.associateBy { it.id }.orEmpty()
            localTracked.forEach { localItem ->
                val cloudItem = cloudMediaMap[localItem.id]
                val localTs = localItem.lastWatchedDate ?: 0L
                val cloudTs = cloudItem?.lastWatchedDate ?: -1L
                if (cloudItem == null || localTs > cloudTs) {
                    sync.uploadTrackedMedia(userId, localItem)
                }
            }

            val localTvProgress = try {
                tvProgressDao.getAllProgressList()
            } catch (_: Exception) {
                emptyList()
            }
            val cloudProgressMap = cloudData?.tvProgress?.associateBy {
                "${it.mediaId}_${it.seasonNumber}_${it.episodeNumber}"
            }.orEmpty()
            val progressToPush = localTvProgress.filter { localProg ->
                val key = "${localProg.mediaId}_${localProg.seasonNumber}_${localProg.episodeNumber}"
                val cloudProg = cloudProgressMap[key]
                cloudProg == null || localProg.updatedAt > cloudProg.updatedAt
            }
            sync.uploadTvProgressList(userId, progressToPush)

            val localCollections = collectionDao.getAllCollections().firstOrNull() ?: emptyList()
            localCollections.forEach { sync.uploadCollection(userId, it) }

            val localFavoritePeople = try {
                favoritePersonDao.getAllFavoritePeopleList()
            } catch (_: Exception) {
                emptyList()
            }
            localFavoritePeople.forEach { sync.uploadFavoritePerson(userId, it) }

            val localPrefs = preferencesDao.getPreferencesDirect()
            if (localPrefs != null) {
                sync.uploadPreferences(userId, localPrefs)
            }
        }
    }

    suspend fun createCustomCollection(name: String, description: String = "") = withContext(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.isNotBlank()) {
            val entity = CustomCollectionEntity(name = clean, description = description)
            collectionDao.insert(entity)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadCollection(uid, entity)
            }
        }
    }

    suspend fun deleteCustomCollection(name: String) = withContext(Dispatchers.IO) {
        val clean = name.trim()
        collectionDao.deleteByName(clean)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.deleteCollection(uid, clean)
        }
    }

    fun getMediaById(id: String): Flow<MediaEntity?> = mediaDao.getMediaById(id)

    suspend fun recordSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            searchHistoryDao.insertOrUpdate(
                SearchHistoryEntity(
                    query = trimmed,
                    timestamp = System.currentTimeMillis()
                )
            )
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadSearchHistory(uid, trimmed)
            }
        }
    }

    suspend fun removeSearchQuery(query: String) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteQuery(query.trim())
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAll()
    }

    private suspend fun clearLocalUserDataLocked() {
        mediaDao.resetAllTrackingData()
        mediaDao.deleteCustomMedia()
        tvProgressDao.clearAll()
        searchHistoryDao.clearAll()
        collectionDao.clearAll()
        favoritePersonDao.clearAll()
    }

    suspend fun clearAllUserData(): Unit = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            clearLocalUserDataLocked()
            firestoreSyncManager?.setLastSyncedUserId(null)
        }
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val initialCatalog = SampleMediaData.getInitialCatalog()
        if (mediaDao.countTotal() == 0) {
            mediaDao.insertAll(initialCatalog)
        } else {
            // Synchronize verified TMDB images into existing Room database cache
            initialCatalog.forEach { sampleItem ->
                mediaDao.updateMediaImages(sampleItem.id, sampleItem.posterUrl, sampleItem.backdropUrl)
            }
        }
        if (preferencesDao.getPreferencesDirect() == null) {
            preferencesDao.savePreferences(
                UserPreferencesEntity(
                    id = 1,
                    regionCode = "AUTO",
                    languageCode = "all",
                    autoDetect = true,
                    filterCatalogByLanguage = false,
                    showStreamingProviders = true,
                    customTmdbApiKey = ""
                )
            )
        }
        if (searchHistoryDao.count() == 0) {
            val starterSearches = listOf(
                "Christopher Nolan",
                "Dune",
                "Stranger Things",
                "Zendaya",
                "Oppenheimer",
                "Pedro Pascal"
            ).mapIndexed { idx, q ->
                SearchHistoryEntity(query = q, timestamp = System.currentTimeMillis() - (idx * 60000L))
            }
            searchHistoryDao.insertAll(starterSearches)
        }
        if (collectionDao.count() == 0) {
            val starterCollections = listOf(
                "Must Watch",
                "Weekend Binge",
                "Masterpieces",
                "Comfort Movies",
                "Oscar Winners"
            ).map { CustomCollectionEntity(name = it) }
            collectionDao.insertAll(starterCollections)
        }
    }

    suspend fun updatePreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        preferencesDao.savePreferences(prefs)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadPreferences(uid, prefs)
        }
    }

    suspend fun fetchPopularMediaFromTmdb(apiKey: String, language: String?, region: String?) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        try {
            val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true)) language else null
            val safeRegion = if (!region.isNullOrBlank() && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

            val entities = mutableListOf<MediaEntity>()

            if (safeRegion == "IN") {
                // Fetch actual Indian popular & theatrical movies (e.g. Ramayana: Rise of a Legend, Singham Again, Bhool Bhulaiyaa 3, Pushpa 2, Kalki 2898 AD, Stree 2)
                try {
                    val inDiscover = TmdbClient.api.discoverMovies(
                        apiKey = apiKey,
                        language = safeLanguage,
                        region = "IN",
                        watchRegion = "IN",
                        withOriginCountry = "IN",
                        sortBy = "popularity.desc"
                    ).results.map { TmdbClient.toMediaEntity(it).copy(originCountry = "IN") }
                    entities.addAll(inDiscover)
                } catch (e: Exception) { e.printStackTrace() }

                try {
                    val inNowPlaying = TmdbClient.api.getNowPlayingMovies(
                        apiKey = apiKey,
                        region = "IN",
                        language = safeLanguage
                    ).results.map { TmdbClient.toMediaEntity(it) }
                    entities.addAll(inNowPlaying)
                } catch (e: Exception) { e.printStackTrace() }

                try {
                    val inUpcoming = TmdbClient.api.getUpcomingMovies(
                        apiKey = apiKey,
                        region = "IN",
                        language = safeLanguage
                    ).results.map { TmdbClient.toMediaEntity(it) }
                    entities.addAll(inUpcoming)
                } catch (e: Exception) { e.printStackTrace() }

                try {
                    val inTv = TmdbClient.api.discoverTv(
                        apiKey = apiKey,
                        language = safeLanguage,
                        region = "IN",
                        withOriginCountry = "IN",
                        sortBy = "popularity.desc"
                    ).results.map { TmdbClient.toMediaEntity(it).copy(originCountry = "IN") }
                    entities.addAll(inTv)
                } catch (e: Exception) { e.printStackTrace() }
            }

            // Fetch live popular movies & series (e.g. Gladiator II, Wicked, Deadpool & Wolverine, Dune 2, The Penguin, Shōgun)
            try {
                val popMovies = TmdbClient.api.getPopularMovies(
                    apiKey = apiKey,
                    region = safeRegion,
                    language = safeLanguage
                ).results.map { TmdbClient.toMediaEntity(it) }
                entities.addAll(popMovies)
            } catch (e: Exception) { e.printStackTrace() }

            try {
                val popTv = TmdbClient.api.getPopularTv(
                    apiKey = apiKey,
                    language = safeLanguage
                ).results.map { TmdbClient.toMediaEntity(it) }
                entities.addAll(popTv)
            } catch (e: Exception) { e.printStackTrace() }

            val filteredEntities = entities.filter { item ->
                val titleLower = item.title.lowercase().trim()
                !titleLower.contains("panchayat") &&
                titleLower != "rrr" &&
                titleLower != "inception" &&
                titleLower != "fight club" &&
                titleLower != "parasite" &&
                titleLower != "the dark knight" &&
                titleLower != "pulp fiction" &&
                titleLower != "the shawshank redemption" &&
                item.releaseYear >= 2023
            }.distinctBy { it.id }

            if (filteredEntities.isNotEmpty()) {
                mediaDao.insertAll(filteredEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchTrendingFromTmdb(apiKey: String, language: String?, region: String?) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        try {
            val response = TmdbClient.api.getTrendingAllDay(
                apiKey = apiKey,
                language = if (language != null && !language.equals("all", ignoreCase = true)) language else null
            )
            val entities = response.results.map { TmdbClient.toMediaEntity(it) }.filter { item ->
                val titleLower = item.title.lowercase().trim()
                !titleLower.contains("panchayat") &&
                titleLower != "rrr" &&
                titleLower != "inception" &&
                titleLower != "fight club" &&
                titleLower != "parasite" &&
                item.releaseYear >= 2023
            }
            if (entities.isNotEmpty()) {
                mediaDao.insertAll(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTrendingNow(
        apiKey: String,
        language: String?,
        region: String = "US",
        timeWindow: String = "day"
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true)) language else null
                val safeRegion = if (!region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

                // For India and localized regions, query popular/trending releases in that region
                val regionalEntities = if (safeRegion != null && safeRegion == "IN") {
                    try {
                        val discMovies = TmdbClient.api.discoverMovies(
                            apiKey = apiKey,
                            language = safeLanguage,
                            region = "IN",
                            watchRegion = "IN",
                            withOriginCountry = "IN",
                            withLanguage = if (safeLanguage != null && safeLanguage != "all") safeLanguage else "hi",
                            sortBy = "popularity.desc"
                        ).results.map { TmdbClient.toMediaEntity(it) }

                        val nowPlayingIn = TmdbClient.api.getNowPlayingMovies(
                            apiKey = apiKey,
                            region = "IN",
                            language = safeLanguage
                        ).results.map { TmdbClient.toMediaEntity(it) }

                        (discMovies + nowPlayingIn).distinctBy { it.id }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else if (safeRegion != null) {
                    try {
                        TmdbClient.api.getNowPlayingMovies(
                            apiKey = apiKey,
                            region = safeRegion,
                            language = safeLanguage
                        ).results.map { TmdbClient.toMediaEntity(it) }
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val trendingResponse = if (timeWindow.equals("week", ignoreCase = true)) {
                    TmdbClient.api.getTrendingAllWeek(apiKey = apiKey, language = safeLanguage)
                } else {
                    TmdbClient.api.getTrendingAllDay(apiKey = apiKey, language = safeLanguage)
                }

                val globalTrending = trendingResponse.results
                    .filter { it.mediaType != "person" }
                    .map { TmdbClient.toMediaEntity(it) }

                val combined = (regionalEntities + globalTrending)
                    .filter { 
                        val titleLower = it.title.lowercase().trim()
                        !titleLower.contains("panchayat") &&
                        titleLower != "rrr" &&
                        titleLower != "inception" &&
                        titleLower != "fight club" &&
                        titleLower != "parasite" &&
                        it.releaseYear >= 2023
                    }
                    .distinctBy { it.id }

                if (combined.isNotEmpty()) {
                    mediaDao.insertAll(combined)
                    return@withContext combined
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback to initial catalog sorted for this region (excluding legacy titles)
        val catalog = SampleMediaData.getInitialCatalog()
        return@withContext catalog
            .filter { 
                val titleLower = it.title.lowercase().trim()
                !titleLower.contains("panchayat") &&
                titleLower != "rrr" &&
                titleLower != "inception" &&
                titleLower != "fight club" &&
                titleLower != "parasite" &&
                it.releaseYear >= 2023
            }
            .sortedWith(
                compareByDescending<MediaEntity> {
                    if (it.originCountry.equals(region, ignoreCase = true)) 1 else 0
                }.thenByDescending { it.releaseYear }
                .thenByDescending { it.communityRating }
            )
    }

    suspend fun searchTmdb(query: String, apiKey: String, language: String?, region: String?): List<MediaEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (region != null && region.length == 2 && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.searchMulti(
                    apiKey = apiKey,
                    query = trimmed,
                    language = safeLanguage,
                    region = safeRegion
                )
                val entities = response.results.filter { it.mediaType != "person" }.map { TmdbClient.toMediaEntity(it) }
                if (entities.isNotEmpty()) {
                    mediaDao.insertAll(entities)
                    return@withContext entities
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback to local Room database + sample data
        searchLocalMediaOnly(trimmed)
    }

    suspend fun getTopRatedMovies(apiKey: String, language: String?, region: String?): List<MediaEntity> = withContext(Dispatchers.IO) {
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (region != null && region.length == 2 && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.getTopRatedMovies(
                    apiKey = apiKey,
                    language = safeLanguage,
                    region = safeRegion
                )
                val entities = response.results.map { TmdbClient.toMediaEntity(it) }
                if (entities.isNotEmpty()) {
                    mediaDao.insertAll(entities)
                    return@withContext entities
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        getCuratedTopRatedMovies()
    }

    suspend fun getTopRatedTv(apiKey: String, language: String?): List<MediaEntity> = withContext(Dispatchers.IO) {
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.getTopRatedTv(
                    apiKey = apiKey,
                    language = safeLanguage
                )
                val entities = response.results.map { TmdbClient.toMediaEntity(it) }
                if (entities.isNotEmpty()) {
                    mediaDao.insertAll(entities)
                    return@withContext entities
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        getCuratedTopRatedTv()
    }

    fun getCuratedTopRatedMovies(): List<MediaEntity> {
        return listOf(
            MediaEntity(
                id = "imdb_top_shawshank",
                title = "The Shawshank Redemption",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1994,
                genres = "Drama, Crime",
                posterUrl = "https://image.tmdb.org/t/p/w500/9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/kXfqcdQKsToO0OUXHcrrNCHDBzO.jpg",
                overview = "Imprisoned in the 1940s for the double murder of his wife and her lover, upstanding banker Andy Dufresne begins a new life at the Shawshank prison.",
                directorOrCreator = "Frank Darabont",
                cast = "Tim Robbins, Morgan Freeman, Bob Gunton, William Sadler",
                communityRating = 9.3f,
                runtimeMinutes = 142,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_godfather",
                title = "The Godfather",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1972,
                genres = "Crime, Drama",
                posterUrl = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
                overview = "Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family.",
                directorOrCreator = "Francis Ford Coppola",
                cast = "Marlon Brando, Al Pacino, James Caan, Robert Duvall",
                communityRating = 9.2f,
                runtimeMinutes = 175,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_dark_knight",
                title = "The Dark Knight",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2008,
                genres = "Action, Crime, Drama",
                posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/nMKdUUepR0i5zn0y1T4CsSB5chy.jpg",
                overview = "Batman raises the stakes in his war on crime with the help of Jim Gordon and Harvey Dent to dismantle Gotham's syndicate.",
                directorOrCreator = "Christopher Nolan",
                cast = "Christian Bale, Heath Ledger, Aaron Eckhart, Michael Caine",
                communityRating = 9.0f,
                runtimeMinutes = 152,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_schindler",
                title = "Schindler's List",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1993,
                genres = "Biography, Drama, History",
                posterUrl = "https://image.tmdb.org/t/p/w500/sF1U4EUQS8YHUYjNl3pMGNIQyr0.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/zb6fM1CX41D9rF9hdgclu0peUmy.jpg",
                overview = "The true story of how industrialist Oskar Schindler saved over a thousand Jewish refugees from the Holocaust.",
                directorOrCreator = "Steven Spielberg",
                cast = "Liam Neeson, Ben Kingsley, Ralph Fiennes",
                communityRating = 9.0f,
                runtimeMinutes = 195,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_12_angry_men",
                title = "12 Angry Men",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1957,
                genres = "Drama",
                posterUrl = "https://image.tmdb.org/t/p/w500/ow3wq89wM8qd5X7hWKxiRfsFf9C.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/qqHMGWiVFX5Cl4UvQIPgKaNgUR3.jpg",
                overview = "The defense and the prosecution have rested and the jury is filing into the jury room to decide whether an eighteen-year-old is guilty of murdering his father.",
                directorOrCreator = "Sidney Lumet",
                cast = "Henry Fonda, Lee J. Cobb, Martin Balsam",
                communityRating = 9.0f,
                runtimeMinutes = 96,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_lotr_return_king",
                title = "The Lord of the Rings: The Return of the King",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2003,
                genres = "Adventure, Fantasy, Action",
                posterUrl = "https://image.tmdb.org/t/p/w500/rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/2u7zbn8EudG6kLlBzUYqP8RyFU4.jpg",
                overview = "As armies mass for a final battle that will decide the fate of the world, Frodo and Sam approach Mount Doom to destroy the One Ring.",
                directorOrCreator = "Peter Jackson",
                cast = "Elijah Wood, Ian McKellen, Viggo Mortensen, Orlando Bloom",
                communityRating = 9.0f,
                runtimeMinutes = 201,
                originCountry = "NZ"
            ),
            MediaEntity(
                id = "imdb_top_pulp_fiction",
                title = "Pulp Fiction",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1994,
                genres = "Crime, Thriller",
                posterUrl = "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg",
                overview = "A burger-loving hit man, his philosophical partner, a gangster's moll and a washed-up boxer converge in tales of redemption.",
                directorOrCreator = "Quentin Tarantino",
                cast = "John Travolta, Samuel L. Jackson, Uma Thurman, Bruce Willis",
                communityRating = 8.9f,
                runtimeMinutes = 154,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_inception",
                title = "Inception",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2010,
                genres = "Sci-Fi, Action, Thriller",
                posterUrl = "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/s3TBrRGB1iav7gFOCNx3H31MoES.jpg",
                overview = "Cobb steals information from targets by entering their dreams. He must perform the impossible inception to regain his life.",
                directorOrCreator = "Christopher Nolan",
                cast = "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy",
                communityRating = 8.8f,
                runtimeMinutes = 148,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_fight_club",
                title = "Fight Club",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1999,
                genres = "Drama, Thriller",
                posterUrl = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
                overview = "An insomniac office worker looking for a way to change his life crosses paths with a devil-may-care soap maker and they form an underground club.",
                directorOrCreator = "David Fincher",
                cast = "Edward Norton, Brad Pitt, Helena Bonham Carter",
                communityRating = 8.8f,
                runtimeMinutes = 139,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_interstellar",
                title = "Interstellar",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2014,
                genres = "Sci-Fi, Drama, Adventure",
                posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
                overview = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                directorOrCreator = "Christopher Nolan",
                cast = "Matthew McConaughey, Anne Hathaway, Jessica Chastain, Michael Caine",
                communityRating = 8.7f,
                runtimeMinutes = 169,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_top_parasite",
                title = "Parasite",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2019,
                genres = "Drama, Thriller, Comedy",
                posterUrl = "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/TU9NIjwzjoKPwQHoHshkFcQUCG.jpg",
                overview = "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
                directorOrCreator = "Bong Joon Ho",
                cast = "Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong, Choi Woo-shik",
                communityRating = 8.5f,
                runtimeMinutes = 132,
                originCountry = "KR"
            ),
            MediaEntity(
                id = "imdb_top_spirited_away",
                title = "Spirited Away",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2001,
                genres = "Animation, Fantasy, Adventure",
                posterUrl = "https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/mSDmv9F6fomS7Wp51V95wIq9G0P.jpg",
                overview = "A young girl wanders into a world ruled by gods, witches and spirits, where humans are changed into beasts.",
                directorOrCreator = "Hayao Miyazaki",
                cast = "Rumi Hiiragi, Miyu Irino, Mari Natsuki",
                communityRating = 8.6f,
                runtimeMinutes = 125,
                originCountry = "JP"
            )
        )
    }

    fun getCuratedTopRatedTv(): List<MediaEntity> {
        return listOf(
            MediaEntity(
                id = "imdb_tv_breaking_bad",
                title = "Breaking Bad",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2008,
                genres = "Crime, Drama, Thriller",
                posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                overview = "A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine with a former student.",
                directorOrCreator = "Vince Gilligan",
                cast = "Bryan Cranston, Aaron Paul, Anna Gunn, Dean Norris",
                communityRating = 9.5f,
                runtimeMinutes = 49,
                totalSeasons = 5,
                totalEpisodes = 62,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_band_of_brothers",
                title = "Band of Brothers",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2001,
                genres = "War, Drama, History",
                posterUrl = "https://image.tmdb.org/t/p/w500/wz5A9Z4y1cQZlR7M7yq0yJ6gZ6b.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
                overview = "Drawn from interviews with survivors of Easy Company, as well as soldiers' journals and letters, this series chronicles the true experiences of these young men.",
                directorOrCreator = "Tom Hanks, Steven Spielberg",
                cast = "Damian Lewis, Ron Livingston, Scott Grimes, Donnie Wahlberg",
                communityRating = 9.4f,
                runtimeMinutes = 60,
                totalSeasons = 1,
                totalEpisodes = 10,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_chernobyl",
                title = "Chernobyl",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2019,
                genres = "Drama, History",
                posterUrl = "https://image.tmdb.org/t/p/w500/hlLXt2tOPT6RRnjiUmoxyG1LTFi.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/uL6AdB5qB12O3j2TzP51z73YvW9.jpg",
                overview = "The true story of one of the worst human-made catastrophes in history: the catastrophic explosion at the Chernobyl Nuclear Power Plant in 1986.",
                directorOrCreator = "Craig Mazin",
                cast = "Jared Harris, Stellan Skarsgård, Emily Watson, Paul Ritter",
                communityRating = 9.3f,
                runtimeMinutes = 65,
                totalSeasons = 1,
                totalEpisodes = 5,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_the_wire",
                title = "The Wire",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2002,
                genres = "Crime, Drama, Thriller",
                posterUrl = "https://image.tmdb.org/t/p/w500/4lbclFySvugI51fwsyxBTOm4DqK.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/og6O22w7M2v1iA3rZ9U0j8YqJ2.jpg",
                overview = "The Baltimore drug scene, as seen through the eyes of drug dealers and law enforcement.",
                directorOrCreator = "David Simon",
                cast = "Dominic West, John Doman, Idris Elba, Frankie Faison",
                communityRating = 9.3f,
                runtimeMinutes = 58,
                totalSeasons = 5,
                totalEpisodes = 60,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_the_sopranos",
                title = "The Sopranos",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 1999,
                genres = "Drama, Crime",
                posterUrl = "https://image.tmdb.org/t/p/w500/6nT506E1G64q6w43dK8nN24t1J4.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/7RyHsO4yDXtBv1z9fZ8x8bLwZ4.jpg",
                overview = "New Jersey mob boss Tony Soprano deals with personal and professional issues in his home and business life that affect his mental state.",
                directorOrCreator = "David Chase",
                cast = "James Gandolfini, Lorraine Bracco, Edie Falco, Michael Imperioli",
                communityRating = 9.2f,
                runtimeMinutes = 55,
                totalSeasons = 6,
                totalEpisodes = 86,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_avatar_airbender",
                title = "Avatar: The Last Airbender",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2005,
                genres = "Animation, Action & Adventure, Sci-Fi & Fantasy",
                posterUrl = "https://image.tmdb.org/t/p/w500/cHFZA8Tlv03n09jhHQJCnr799zK.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/b9UCf9AcA9uhG0t9x9H1dYf4d3.jpg",
                overview = "In a war-torn world of elemental magic, a young boy reawakens to undertake a dangerous mystic quest to fulfill his destiny as the Avatar.",
                directorOrCreator = "Michael Dante DiMartino, Bryan Konietzko",
                cast = "Zach Tyler Eisen, Mae Whitman, Jack De Sena, Dante Basco",
                communityRating = 9.3f,
                runtimeMinutes = 24,
                totalSeasons = 3,
                totalEpisodes = 61,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_game_of_thrones",
                title = "Game of Thrones",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2011,
                genres = "Sci-Fi & Fantasy, Drama, Action & Adventure",
                posterUrl = "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
                overview = "Seven noble families fight for control of the mythical land of Westeros. Friction between the houses leads to full-scale war.",
                directorOrCreator = "David Benioff, D.B. Weiss",
                cast = "Emilia Clarke, Peter Dinklage, Kit Harington, Lena Headey",
                communityRating = 9.2f,
                runtimeMinutes = 60,
                totalSeasons = 8,
                totalEpisodes = 73,
                originCountry = "US"
            ),
            MediaEntity(
                id = "imdb_tv_arcane",
                title = "Arcane",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2021,
                genres = "Animation, Sci-Fi & Fantasy, Action & Adventure, Drama",
                posterUrl = "https://image.tmdb.org/t/p/w500/fq23b8v4vB8e6T2yR8c3p2vM8Z.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/hPea3QiUeeNoqUvsD9lqj1YQ04q.jpg",
                overview = "Amid the stark discord of twin cities Piltover and Zaun, two sisters fight on rival sides of a war between magic technologies and incompatible convictions.",
                directorOrCreator = "Christian Linke, Alex Yee",
                cast = "Hailee Steinfeld, Ella Purnell, Kevin Alejandro, Katie Leung",
                communityRating = 9.0f,
                runtimeMinutes = 40,
                totalSeasons = 2,
                totalEpisodes = 18,
                originCountry = "US"
            )
        )
    }

    suspend fun searchMultiTmdb(
        query: String,
        apiKey: String,
        language: String?,
        region: String?
    ): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (region != null && region.length == 2 && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.searchMulti(
                    apiKey = apiKey,
                    query = trimmed,
                    language = safeLanguage,
                    region = safeRegion
                )

                val results = mutableListOf<TmdbSearchResultItem>()
                val entitiesToCache = mutableListOf<MediaEntity>()

                response.results.forEach { dto ->
                    if (dto.mediaType == "person") {
                        val profile = dto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" }
                        val knownForEntities = dto.knownFor?.map { TmdbClient.knownForToEntity(it) } ?: emptyList()
                        entitiesToCache.addAll(knownForEntities)
                        results.add(
                            TmdbSearchResultItem.Person(
                                id = dto.id,
                                name = dto.name ?: "Unknown Person",
                                profileUrl = profile,
                                department = dto.knownForDepartment ?: "Acting",
                                knownFor = knownForEntities,
                                popularity = dto.popularity ?: 0.0
                            )
                        )
                    } else {
                        val isMovie = dto.mediaType == "movie" || dto.title != null
                        val entity = TmdbClient.toMediaEntity(dto)
                        entitiesToCache.add(entity)
                        results.add(
                            TmdbSearchResultItem.Media(
                                entity = entity,
                                isMovie = isMovie
                            )
                        )
                    }
                }

                if (entitiesToCache.isNotEmpty()) {
                    mediaDao.insertAll(entitiesToCache)
                }
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext searchLocalAndSample(trimmed)
    }

    suspend fun discoverByGenreTmdb(
        genre: String,
        apiKey: String,
        language: String?,
        region: String?
    ): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val normalized = genre.trim()
        if (normalized.isBlank()) return@withContext emptyList()

        val (movieGenreId, tvGenreId) = when (normalized.lowercase()) {
            "action" -> "28" to "10759"
            "adventure" -> "12" to "10759"
            "animation" -> "16" to "16"
            "comedy" -> "35" to "35"
            "crime" -> "80" to "80"
            "documentary" -> "99" to "99"
            "drama" -> "18" to "18"
            "family" -> "10751" to "10751"
            "fantasy" -> "14" to "10765"
            "history" -> "36" to null
            "horror" -> "27" to null
            "music" -> "10402" to null
            "mystery" -> "9648" to "9648"
            "romance" -> "10749" to null
            "sci-fi", "science fiction" -> "878" to "10765"
            "thriller" -> "53" to "80|9648"
            "war" -> "10752" to "10768"
            "western" -> "37" to "37"
            else -> null to null
        }

        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (region != null && region.length == 2 && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY" && (movieGenreId != null || tvGenreId != null)) {
            try {
                val (movieResults, tvResults) = coroutineScope {
                    val moviesDeferred = async {
                        if (movieGenreId != null) {
                            try {
                                TmdbClient.api.discoverMovies(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    region = safeRegion,
                                    withGenres = movieGenreId,
                                    sortBy = "popularity.desc"
                                ).results.map { dto ->
                                    TmdbClient.toMediaEntity(dto.copy(mediaType = "movie"))
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                    }
                    val tvDeferred = async {
                        if (tvGenreId != null) {
                            try {
                                TmdbClient.api.discoverTv(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    region = safeRegion,
                                    withGenres = tvGenreId,
                                    sortBy = "popularity.desc"
                                ).results.map { dto ->
                                    TmdbClient.toMediaEntity(dto.copy(mediaType = "tv"))
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                    }
                    moviesDeferred.await() to tvDeferred.await()
                }

                // Interleave Movies and TV Series so both appear cleanly in the "All" tab
                val combined = mutableListOf<TmdbSearchResultItem>()
                val entitiesToCache = mutableListOf<MediaEntity>()
                val maxSize = maxOf(movieResults.size, tvResults.size)
                for (i in 0 until maxSize) {
                    if (i < movieResults.size) {
                        val m = movieResults[i]
                        entitiesToCache.add(m)
                        combined.add(TmdbSearchResultItem.Media(entity = m, isMovie = true))
                    }
                    if (i < tvResults.size) {
                        val tv = tvResults[i]
                        entitiesToCache.add(tv)
                        combined.add(TmdbSearchResultItem.Media(entity = tv, isMovie = false))
                    }
                }

                if (entitiesToCache.isNotEmpty()) {
                    mediaDao.insertAll(entitiesToCache)
                }
                if (combined.isNotEmpty()) {
                    return@withContext combined.distinctBy {
                        (it as TmdbSearchResultItem.Media).entity.id
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext discoverLocalByGenre(normalized)
    }

    suspend fun discoverLocalByGenre(genre: String): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val allLocal = try {
            (mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog()).distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }
        allLocal.filter { item ->
            item.getGenreList().any { g ->
                g.equals(genre, ignoreCase = true) ||
                    g.contains(genre, ignoreCase = true) ||
                    (genre.equals("Sci-Fi", ignoreCase = true) && g.contains("Science Fiction", ignoreCase = true))
            }
        }.sortedByDescending { it.communityRating }
            .map { entity ->
                TmdbSearchResultItem.Media(
                    entity = entity,
                    isMovie = entity.getMediaTypeEnum() == MediaType.MOVIE
                )
            }
    }

    private suspend fun searchLocalMediaOnly(query: String): List<MediaEntity> = withContext(Dispatchers.IO) {
        val allLocal = try {
            (mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog()).distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }
        allLocal.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                    item.cast.contains(query, ignoreCase = true) ||
                    item.directorOrCreator.contains(query, ignoreCase = true) ||
                    item.genres.contains(query, ignoreCase = true) ||
                    item.overview.contains(query, ignoreCase = true)
        }
    }

    suspend fun searchLocalAndSample(query: String): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TmdbSearchResultItem>()

        val allLocal = try {
            (mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog()).distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }

        val matchingMedia = allLocal.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                    item.cast.contains(query, ignoreCase = true) ||
                    item.directorOrCreator.contains(query, ignoreCase = true) ||
                    item.genres.contains(query, ignoreCase = true) ||
                    item.overview.contains(query, ignoreCase = true)
        }.map { entity ->
            TmdbSearchResultItem.Media(
                entity = entity,
                isMovie = entity.getMediaTypeEnum() == MediaType.MOVIE
            )
        }
        results.addAll(matchingMedia)

        val matchingPeople = SampleMediaData.getSamplePeople().filter { person ->
            person.name.contains(query, ignoreCase = true) ||
                    person.knownFor.any { it.title.contains(query, ignoreCase = true) }
        }
        results.addAll(matchingPeople)

        results
    }

    suspend fun searchPersonByName(
        name: String,
        apiKey: String,
        language: String? = null
    ): TmdbSearchResultItem.Person? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext null

        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) {
            language
        } else {
            null
        }

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.searchPerson(
                    apiKey = apiKey,
                    query = trimmed,
                    language = safeLanguage
                )
                val bestDto = response.results.firstOrNull {
                    (it.name ?: "").equals(trimmed, ignoreCase = true)
                } ?: response.results.firstOrNull()

                if (bestDto != null) {
                    val profile = bestDto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    val knownForEntities = bestDto.knownFor?.map { TmdbClient.knownForToEntity(it) } ?: emptyList()
                    if (knownForEntities.isNotEmpty()) {
                        mediaDao.insertAll(knownForEntities)
                    }
                    return@withContext TmdbSearchResultItem.Person(
                        id = bestDto.id,
                        name = bestDto.name ?: trimmed,
                        profileUrl = profile,
                        department = bestDto.knownForDepartment ?: "Acting",
                        knownFor = knownForEntities,
                        popularity = bestDto.popularity ?: 0.0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        SampleMediaData.getSamplePeople().find {
            it.name.equals(trimmed, ignoreCase = true)
        } ?: SampleMediaData.getSamplePeople().find {
            it.name.contains(trimmed, ignoreCase = true)
        }
    }

    suspend fun fetchPersonDetails(personId: Long, apiKey: String): TmdbPersonDetailDto? = withContext(Dispatchers.IO) {
        if (personId <= 0L || apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext null
        try {
            TmdbClient.api.getPersonDetails(personId, apiKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resolveCastMembers(
        names: List<String>,
        apiKey: String,
        language: String? = null
    ): List<CastMemberItem> = withContext(Dispatchers.IO) {
        val samplePeople = SampleMediaData.getSamplePeople()
        val limited = names.map { it.trim() }.filter { it.isNotBlank() }.take(8)
        if (limited.isEmpty()) return@withContext emptyList()

        coroutineScope {
            limited.map { actorName ->
                async {
                    val sampleMatch = samplePeople.find { it.name.equals(actorName, ignoreCase = true) }
                    if (sampleMatch != null && !sampleMatch.profileUrl.isNullOrBlank()) {
                        CastMemberItem(
                            id = sampleMatch.id,
                            name = sampleMatch.name,
                            profileUrl = sampleMatch.profileUrl
                        )
                    } else if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
                        val resolved = searchPersonByName(actorName, apiKey, language)
                        CastMemberItem(
                            id = resolved?.id ?: 0L,
                            name = actorName,
                            profileUrl = resolved?.profileUrl ?: sampleMatch?.profileUrl
                        )
                    } else {
                        CastMemberItem(
                            id = sampleMatch?.id ?: 0L,
                            name = actorName,
                            profileUrl = sampleMatch?.profileUrl
                        )
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun fetchDetailsFromTmdb(id: String, apiKey: String): List<CastMemberItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext emptyList()
        try {
            val existing = mediaDao.getMediaByIdDirect(id)
            if (id.startsWith("tmdb_m_")) {
                val tmdbId = id.removePrefix("tmdb_m_").toLongOrNull() ?: return@withContext emptyList()
                val detail = TmdbClient.api.getMovieDetails(tmdbId, apiKey)
                val updated = TmdbClient.movieDetailToEntity(detail, existing)
                mediaDao.insertOrUpdate(updated)
                return@withContext detail.credits?.cast?.take(12)?.map { dto ->
                    CastMemberItem(
                        id = dto.id ?: 0L,
                        name = dto.name,
                        character = dto.character?.takeIf { it.isNotBlank() },
                        profileUrl = dto.profilePath?.let { "${TmdbClient.IMAGE_BASE_URL}$it" }
                    )
                }.orEmpty()
            } else if (id.startsWith("tmdb_tv_")) {
                val tmdbId = id.removePrefix("tmdb_tv_").toLongOrNull() ?: return@withContext emptyList()
                val detail = TmdbClient.api.getTvDetails(tmdbId, apiKey)
                val updated = TmdbClient.tvDetailToEntity(detail, existing)
                mediaDao.insertOrUpdate(updated)
                return@withContext detail.credits?.cast?.take(12)?.map { dto ->
                    CastMemberItem(
                        id = dto.id ?: 0L,
                        name = dto.name,
                        character = dto.character?.takeIf { it.isNotBlank() },
                        profileUrl = dto.profilePath?.let { "${TmdbClient.IMAGE_BASE_URL}$it" }
                    )
                }.orEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun fetchLiveWatchProviders(
        mediaId: String,
        regionCode: String,
        apiKey: String
    ): WatchProvidersInfo? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext null
        try {
            val response = if (mediaId.startsWith("tmdb_m_")) {
                val movieId = mediaId.removePrefix("tmdb_m_").toLongOrNull() ?: return@withContext null
                TmdbClient.api.getMovieWatchProviders(movieId, apiKey)
            } else if (mediaId.startsWith("tmdb_tv_")) {
                val tvId = mediaId.removePrefix("tmdb_tv_").toLongOrNull() ?: return@withContext null
                TmdbClient.api.getTvWatchProviders(tvId, apiKey)
            } else {
                return@withContext null
            }

            val regionData = response.results?.get(regionCode.uppercase())
                ?: response.results?.get("US")
                ?: response.results?.values?.firstOrNull()
                ?: return@withContext null

            // Scrape JustWatch direct outbound content URLs
            val directLinks = if (!regionData.link.isNullOrBlank()) {
                JustWatchScraper.scrapeDirectContentUrls(regionData.link)
            } else {
                emptyMap()
            }

            fun findDirectUrl(providerName: String): String? {
                val clean = providerName.trim().lowercase()
                return directLinks[clean]
                    ?: directLinks.entries.firstOrNull { (k, _) -> clean.contains(k) || k.contains(clean) }?.value
            }

            val regionLabel = RegionalDefaults.getRegionLabel(regionCode)
            val streamItems = (regionData.flatrate.orEmpty()).mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 1),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.STREAM,
                    webLink = regionData.link,
                    directContentUrl = findDirectUrl(p.providerName)
                )
            }
            val freeItems = (regionData.free.orEmpty() + regionData.ads.orEmpty()).mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 100),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.FREE,
                    webLink = regionData.link,
                    directContentUrl = findDirectUrl(p.providerName)
                )
            }
            val rentItems = regionData.rent.orEmpty().mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 200),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.RENT,
                    webLink = regionData.link,
                    directContentUrl = findDirectUrl(p.providerName)
                )
            }
            val buyItems = regionData.buy.orEmpty().mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 300),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.BUY,
                    webLink = regionData.link,
                    directContentUrl = findDirectUrl(p.providerName)
                )
            }

            val info = WatchProvidersInfo(
                regionCode = regionCode,
                regionLabel = regionLabel,
                stream = streamItems,
                free = freeItems,
                rent = rentItems,
                buy = buyItems,
                justWatchLink = regionData.link
            )

            // Update local media streaming providers string if present
            val existing = mediaDao.getMediaByIdDirect(mediaId)
            if (existing != null && streamItems.isNotEmpty()) {
                val newProviderNames = streamItems.joinToString(",") { it.name }
                val updatedProviders = if (existing.streamingProviders.isBlank()) {
                    "$regionCode:$newProviderNames"
                } else {
                    "${existing.streamingProviders}|$regionCode:$newProviderNames"
                }
                mediaDao.insertOrUpdate(existing.copy(streamingProviders = updatedProviders))
            }

            info
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateStatus(id: String, status: WatchStatus) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val newEpisodes = when (status) {
            WatchStatus.COMPLETED -> existing.totalEpisodes
            WatchStatus.PLAN_TO_WATCH -> 0
            else -> existing.watchedEpisodes
        }
        val updated = existing.copy(
            status = status.name,
            watchedEpisodes = newEpisodes,
            lastWatchedDate = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
        }
    }

    suspend fun incrementEpisode(id: String) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        if (existing.getMediaTypeEnum() == MediaType.TV_SHOW) {
            val nextEpisode = (existing.watchedEpisodes + 1).coerceAtMost(existing.totalEpisodes)
            val newStatus = if (nextEpisode >= existing.totalEpisodes && existing.totalEpisodes > 0) {
                WatchStatus.COMPLETED.name
            } else {
                WatchStatus.WATCHING.name
            }
            val updated = existing.copy(
                watchedEpisodes = nextEpisode,
                status = newStatus,
                lastWatchedDate = System.currentTimeMillis()
            )
            mediaDao.insertOrUpdate(updated)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, updated)
            }
        }
    }

    suspend fun updateWatchedEpisodes(id: String, episodes: Int) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val safeEpisodes = episodes.coerceIn(0, existing.totalEpisodes)
        val newStatus = if (safeEpisodes >= existing.totalEpisodes && existing.totalEpisodes > 0) {
            WatchStatus.COMPLETED.name
        } else if (safeEpisodes > 0) {
            WatchStatus.WATCHING.name
        } else {
            existing.status
        }
        val updated = existing.copy(
            watchedEpisodes = safeEpisodes,
            status = newStatus,
            lastWatchedDate = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
        }
    }

    suspend fun updateRatingAndReview(id: String, rating: Float, review: String) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val updated = existing.copy(
            userRating = rating,
            userReview = review,
            lastWatchedDate = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
        }
    }

    suspend fun toggleFavorite(id: String, current: Boolean) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val updated = existing.copy(
            isFavorite = !current,
            lastWatchedDate = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
        }
    }

    suspend fun toggleFavoritePerson(person: FavoritePersonEntity) = withContext(Dispatchers.IO) {
        val allExisting = favoritePersonDao.getAllFavoritePeopleList()
        val matching = allExisting.filter {
            it.id == person.id ||
                (person.personId > 0L && it.personId == person.personId) ||
                it.name.equals(person.name, ignoreCase = true)
        }
        val uid = currentAuthenticatedUserId()
        if (matching.isNotEmpty()) {
            matching.forEach { match ->
                favoritePersonDao.deleteById(match.id)
                if (!uid.isNullOrBlank()) {
                    firestoreSyncManager?.deleteFavoritePerson(uid, match.id)
                }
            }
        } else {
            favoritePersonDao.insert(person)
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadFavoritePerson(uid, person)
            }
        }
    }

    suspend fun updateCustomWatchlist(id: String, watchlist: String?) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val updated = existing.copy(
            customWatchlist = watchlist,
            lastWatchedDate = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
        }
    }

    suspend fun ensureMediaCached(item: MediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertAll(listOf(item))
    }

    suspend fun toggleWatchlistForEntity(item: MediaEntity) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(item.id)
        if (existing == null) {
            val added = item.copy(
                status = WatchStatus.PLAN_TO_WATCH.name,
                lastWatchedDate = System.currentTimeMillis()
            )
            mediaDao.insertOrUpdate(added)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, added)
            }
        } else {
            val currentStatus = existing.getWatchStatusEnum()
            val nextStatus = if (currentStatus == WatchStatus.NONE) {
                WatchStatus.PLAN_TO_WATCH
            } else {
                WatchStatus.NONE
            }
            updateStatus(existing.id, nextStatus)
        }
    }

    suspend fun saveMedia(item: MediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertOrUpdate(item)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, item)
        }
    }

    suspend fun deleteMedia(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteById(id)
        tvProgressDao.deleteAllForMedia(id)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.deleteTrackedMedia(uid, id)
        }
    }

    fun getTvProgress(mediaId: String): Flow<List<TvProgressEntity>> = tvProgressDao.getProgressForMedia(mediaId)

    suspend fun getSeasonsForTvShow(media: MediaEntity, apiKey: String): List<SeasonItem> = withContext(Dispatchers.IO) {
        val baseSeasons = if (media.id.startsWith("tmdb_tv_") && apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val tmdbId = media.id.removePrefix("tmdb_tv_").toLongOrNull()
                if (tmdbId != null) {
                    val tvDetails = TmdbClient.api.getTvDetails(tmdbId, apiKey)
                    tvDetails.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.map { s ->
                        val sNum = s.seasonNumber ?: 1
                        val poster = s.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: media.posterUrl
                        SeasonItem(
                            seriesId = media.id,
                            seriesTitle = media.title,
                            seasonNumber = sNum,
                            name = s.name ?: "Season $sNum",
                            overview = s.overview.orEmpty().ifBlank { media.overview },
                            posterUrl = poster,
                            backdropUrl = media.backdropUrl,
                            episodeCount = s.episodeCount ?: 10,
                            releaseYear = (s.airDate ?: media.releaseYear.toString()).take(4).toIntOrNull() ?: media.releaseYear,
                            airDate = s.airDate ?: "${media.releaseYear}-01-01",
                            rating = (s.voteAverage?.toFloat() ?: media.communityRating)
                        )
                    } ?: SampleTvEpisodeData.getCuratedSeasons(media)
                } else SampleTvEpisodeData.getCuratedSeasons(media)
            } catch (e: Exception) {
                SampleTvEpisodeData.getCuratedSeasons(media)
            }
        } else {
            SampleTvEpisodeData.getCuratedSeasons(media)
        }

        // Merge saved progress from Room
        val progressList = tvProgressDao.getProgressForMediaDirect(media.id)
        val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }

        baseSeasons.map { season ->
            val seasonStatusStr = progressMap["${season.seasonNumber}_0"]
            val seasonStatus = when (seasonStatusStr) {
                "WATCHED" -> TvWatchStatus.WATCHED
                "WATCHING" -> TvWatchStatus.WATCHING
                "PLAN_TO_WATCH" -> TvWatchStatus.PLAN_TO_WATCH
                else -> TvWatchStatus.UNWATCHED
            }

            // Count how many episodes in this season are marked WATCHED
            val watchedEpsCount = (1..season.episodeCount).count { epNum ->
                progressMap["${season.seasonNumber}_$epNum"] == "WATCHED"
            }

            val effectiveStatus = if (watchedEpsCount >= season.episodeCount && season.episodeCount > 0) {
                TvWatchStatus.WATCHED
            } else if (watchedEpsCount > 0 && seasonStatus == TvWatchStatus.UNWATCHED) {
                TvWatchStatus.WATCHING
            } else {
                seasonStatus
            }

            season.copy(
                watchStatus = effectiveStatus,
                watchedEpisodesCount = if (effectiveStatus == TvWatchStatus.WATCHED && watchedEpsCount == 0) season.episodeCount else watchedEpsCount
            )
        }
    }

    suspend fun getEpisodesForSeason(media: MediaEntity, seasonNumber: Int, apiKey: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val baseEpisodes = if (media.id.startsWith("tmdb_tv_") && apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val tmdbId = media.id.removePrefix("tmdb_tv_").toLongOrNull()
                if (tmdbId != null) {
                    val seasonDetail = TmdbClient.api.getSeasonDetails(tmdbId, seasonNumber, apiKey)
                    if (seasonDetail.episodes.isNotEmpty()) {
                        seasonDetail.episodes.map { ep ->
                            val still = ep.stillPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: media.backdropUrl
                            EpisodeItem(
                                id = "${media.id}_s${seasonNumber}_e${ep.episodeNumber}",
                                seriesId = media.id,
                                seriesTitle = media.title,
                                seasonNumber = seasonNumber,
                                episodeNumber = ep.episodeNumber,
                                title = ep.name ?: "Episode ${ep.episodeNumber}",
                                overview = ep.overview.orEmpty(),
                                stillUrl = still,
                                airDate = ep.airDate ?: "",
                                runtimeMinutes = ep.runtime ?: 45,
                                rating = ep.voteAverage?.toFloat() ?: media.communityRating,
                                director = media.directorOrCreator,
                                writer = media.directorOrCreator
                            )
                        }
                    } else SampleTvEpisodeData.getCuratedEpisodes(media, seasonNumber)
                } else SampleTvEpisodeData.getCuratedEpisodes(media, seasonNumber)
            } catch (e: Exception) {
                SampleTvEpisodeData.getCuratedEpisodes(media, seasonNumber)
            }
        } else {
            SampleTvEpisodeData.getCuratedEpisodes(media, seasonNumber)
        }

        // Merge saved progress
        val progressList = tvProgressDao.getProgressForMediaDirect(media.id)
        val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
        val seasonStatus = progressMap["${seasonNumber}_0"]

        baseEpisodes.map { episode ->
            val epStatusStr = progressMap["${seasonNumber}_${episode.episodeNumber}"]
            val status = when {
                epStatusStr == "WATCHED" -> TvWatchStatus.WATCHED
                epStatusStr == "WATCHING" -> TvWatchStatus.WATCHING
                epStatusStr == "PLAN_TO_WATCH" -> TvWatchStatus.PLAN_TO_WATCH
                epStatusStr == "UNWATCHED" -> TvWatchStatus.UNWATCHED
                seasonStatus == "WATCHED" -> TvWatchStatus.WATCHED
                else -> TvWatchStatus.UNWATCHED
            }
            episode.copy(watchStatus = status)
        }
    }

    suspend fun setSeasonWatchStatus(media: MediaEntity, seasonNumber: Int, status: TvWatchStatus) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val seasonProgress = TvProgressEntity(
            mediaId = media.id,
            seasonNumber = seasonNumber,
            episodeNumber = 0,
            status = status.name,
            updatedAt = now
        )
        tvProgressDao.insertOrUpdate(seasonProgress)

        // If WATCHED, mark all episodes in this season as WATCHED
        val episodes = getEpisodesForSeason(media, seasonNumber, "")
        val epEntities = when (status) {
            TvWatchStatus.WATCHED -> episodes.map {
                TvProgressEntity(media.id, seasonNumber, it.episodeNumber, "WATCHED", now)
            }
            TvWatchStatus.UNWATCHED -> episodes.map {
                TvProgressEntity(media.id, seasonNumber, it.episodeNumber, "UNWATCHED", now)
            }
            else -> emptyList()
        }
        if (epEntities.isNotEmpty()) {
            tvProgressDao.insertAll(epEntities)
        }

        // Recalculate total watched episodes across the entire series
        val allProgress = tvProgressDao.getProgressForMediaDirect(media.id)
        val watchedCount = allProgress.count { it.episodeNumber > 0 && it.status == "WATCHED" }
        mediaDao.updateWatchedEpisodes(media.id, watchedCount, now)

        val newMediaStatus = if (watchedCount >= media.totalEpisodes && media.totalEpisodes > 0) {
            WatchStatus.COMPLETED
        } else if (watchedCount > 0 || status == TvWatchStatus.WATCHING) {
            WatchStatus.WATCHING
        } else if (status == TvWatchStatus.PLAN_TO_WATCH) {
            WatchStatus.PLAN_TO_WATCH
        } else {
            media.getWatchStatusEnum()
        }
        mediaDao.updateStatus(media.id, newMediaStatus.name, now)

        // Upload to Firestore if logged in
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            mediaDao.getMediaByIdDirect(media.id)?.let {
                firestoreSyncManager?.uploadTrackedMedia(uid, it)
            }
            firestoreSyncManager?.uploadTvProgressList(uid, listOf(seasonProgress) + epEntities)
        }
    }

    suspend fun setEpisodeWatchStatus(media: MediaEntity, seasonNumber: Int, episodeNumber: Int, status: TvWatchStatus) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val epProgress = TvProgressEntity(
            mediaId = media.id,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            status = status.name,
            updatedAt = now
        )
        tvProgressDao.insertOrUpdate(epProgress)

        // Check if all episodes in this season are watched
        val episodes = getEpisodesForSeason(media, seasonNumber, "")
        val allProgress = tvProgressDao.getProgressForMediaDirect(media.id)
        val progressMap = allProgress.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }

        val allInSeasonWatched = episodes.isNotEmpty() && episodes.all { ep ->
            if (ep.episodeNumber == episodeNumber) status == TvWatchStatus.WATCHED
            else progressMap["${seasonNumber}_${ep.episodeNumber}"] == "WATCHED"
        }

        val updatedSeasonProgress = if (allInSeasonWatched) {
            TvProgressEntity(media.id, seasonNumber, 0, "WATCHED", now).also {
                tvProgressDao.insertOrUpdate(it)
            }
        } else if (status == TvWatchStatus.WATCHING) {
            TvProgressEntity(media.id, seasonNumber, 0, "WATCHING", now).also {
                tvProgressDao.insertOrUpdate(it)
            }
        } else {
            null
        }

        // Recalculate total watched episodes
        val updatedProgress = tvProgressDao.getProgressForMediaDirect(media.id)
        val watchedCount = updatedProgress.count { it.episodeNumber > 0 && it.status == "WATCHED" }
        mediaDao.updateWatchedEpisodes(media.id, watchedCount, now)

        val newMediaStatus = if (watchedCount >= media.totalEpisodes && media.totalEpisodes > 0) {
            WatchStatus.COMPLETED
        } else if (watchedCount > 0 || status == TvWatchStatus.WATCHING) {
            WatchStatus.WATCHING
        } else {
            media.getWatchStatusEnum()
        }
        mediaDao.updateStatus(media.id, newMediaStatus.name, now)

        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            mediaDao.getMediaByIdDirect(media.id)?.let {
                firestoreSyncManager?.uploadTrackedMedia(uid, it)
            }
            val progressBatch = listOfNotNull(epProgress, updatedSeasonProgress)
            firestoreSyncManager?.uploadTvProgressList(uid, progressBatch)
        }
    }

    private fun normalizeTrailerTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    suspend fun fetchTrailers(apiKey: String, region: String = "US", language: String = "en"): List<TrailerItem> = withContext(Dispatchers.IO) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayCal.timeInMillis
        val sixtyDaysAgoMillis = todayStartMillis - (60L * 24 * 60 * 60 * 1000)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(todayCal.time)
        val sixtyDaysAgoStr = dateFormat.format(java.util.Date(sixtyDaysAgoMillis))

        val initialFallbackList = SampleTrailerData.sampleTrailers
            .filter {
                it.isWithinRelevanceWindow() &&
                !it.title.lowercase().contains("panchayat") &&
                it.title.lowercase().trim() != "rrr"
            }
            .distinctBy { normalizeTrailerTitle(it.title) to it.mediaType }

        val comparator = compareByDescending<TrailerItem> {
            var score = 0
            if (it.originCountry.equals(region, ignoreCase = true)) score += 4
            if (!language.equals("all", ignoreCase = true) && it.language.equals(language, ignoreCase = true)) score += 2
            if (it.videoType.equals("Trailer", ignoreCase = true)) score += 2
            score
        }.thenByDescending { it.releaseDate }
            .thenByDescending { it.communityRating }

        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") {
            return@withContext initialFallbackList.sortedWith(comparator)
        }

        try {
            val safeLang = if (language.equals("all", ignoreCase = true)) null else language
            val safeRegion = if (region.equals("AUTO", ignoreCase = true) || region.equals("GLOBAL", ignoreCase = true)) {
                null
            } else {
                region.uppercase()
            }

            data class CandidateSource(
                val dto: TmdbMediaDto,
                val isMovie: Boolean,
                val fallbackUpcoming: Boolean
            )

            data class TrailerCandidatePools(
                val nowPlayingRegion: List<TmdbMediaDto>,
                val upcomingRegion: List<TmdbMediaDto>,
                val discoverUpcomingMovies: List<TmdbMediaDto>,
                val nowPlayingGlobal: List<TmdbMediaDto>,
                val upcomingGlobal: List<TmdbMediaDto>,
                val onTheAirTv: List<TmdbMediaDto>,
                val airingTodayTv: List<TmdbMediaDto>,
                val discoverUpcomingTv: List<TmdbMediaDto>,
                val regionalMovies: List<TmdbMediaDto>,
                val regionalTv: List<TmdbMediaDto>
            )

            // Fetch rich candidate lists across Now Playing, Upcoming Movies, On-The-Air & Upcoming TV Series concurrently
            val pools = coroutineScope {
                val npRegDef = async {
                    val p1 = runCatching { TmdbClient.api.getNowPlayingMovies(apiKey, region = safeRegion, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                    val p2 = runCatching { TmdbClient.api.getNowPlayingMovies(apiKey, region = safeRegion, language = safeLang, page = 2).results }.getOrDefault(emptyList())
                    p1 + p2
                }
                val upRegDef = async {
                    val p1 = runCatching { TmdbClient.api.getUpcomingMovies(apiKey, region = safeRegion, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                    val p2 = runCatching { TmdbClient.api.getUpcomingMovies(apiKey, region = safeRegion, language = safeLang, page = 2).results }.getOrDefault(emptyList())
                    p1 + p2
                }
                val discUpMovDef = async {
                    runCatching {
                        TmdbClient.api.discoverMovies(
                            apiKey = apiKey,
                            language = safeLang,
                            region = safeRegion,
                            primaryReleaseDateGte = todayStr,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results
                    }.getOrDefault(emptyList())
                }
                val npGlobDef = async {
                    if (safeRegion != null) {
                        runCatching { TmdbClient.api.getNowPlayingMovies(apiKey, region = null, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                    } else emptyList()
                }
                val upGlobDef = async {
                    if (safeRegion != null) {
                        runCatching { TmdbClient.api.getUpcomingMovies(apiKey, region = null, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                    } else emptyList()
                }
                val onAirDef = async {
                    val p1 = runCatching { TmdbClient.api.getOnTheAirTv(apiKey, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                    val p2 = runCatching { TmdbClient.api.getOnTheAirTv(apiKey, language = safeLang, page = 2).results }.getOrDefault(emptyList())
                    p1 + p2
                }
                val airingDef = async {
                    runCatching { TmdbClient.api.getAiringTodayTv(apiKey, language = safeLang, page = 1).results }.getOrDefault(emptyList())
                }
                val discUpTvDef = async {
                    runCatching {
                        TmdbClient.api.discoverTv(
                            apiKey = apiKey,
                            language = safeLang,
                            firstAirDateGte = todayStr,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results
                    }.getOrDefault(emptyList())
                }
                val regMovDef = async {
                    if (safeRegion != null && safeRegion != "US") {
                        runCatching {
                            TmdbClient.api.discoverMovies(
                                apiKey = apiKey,
                                language = safeLang,
                                withOriginCountry = safeRegion,
                                withLanguage = safeLang,
                                primaryReleaseDateGte = sixtyDaysAgoStr,
                                sortBy = "popularity.desc",
                                page = 1
                            ).results
                        }.getOrDefault(emptyList())
                    } else emptyList()
                }
                val regTvDef = async {
                    if (safeRegion != null && safeRegion != "US") {
                        runCatching {
                            TmdbClient.api.discoverTv(
                                apiKey = apiKey,
                                language = safeLang,
                                withOriginCountry = safeRegion,
                                withLanguage = safeLang,
                                firstAirDateGte = sixtyDaysAgoStr,
                                sortBy = "popularity.desc",
                                page = 1
                            ).results
                        }.getOrDefault(emptyList())
                    } else emptyList()
                }

                TrailerCandidatePools(
                    nowPlayingRegion = npRegDef.await(),
                    upcomingRegion = upRegDef.await(),
                    discoverUpcomingMovies = discUpMovDef.await(),
                    nowPlayingGlobal = npGlobDef.await(),
                    upcomingGlobal = upGlobDef.await(),
                    onTheAirTv = onAirDef.await(),
                    airingTodayTv = airingDef.await(),
                    discoverUpcomingTv = discUpTvDef.await(),
                    regionalMovies = regMovDef.await(),
                    regionalTv = regTvDef.await()
                )
            }

            // Interleave candidates evenly so Upcoming Movies, Now Playing Movies, Upcoming Series, Streaming Series, and Regional Picks are all well represented
            val orderedSources = buildList {
                pools.regionalMovies.take(12).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = false)) }
                pools.regionalTv.take(10).forEach { add(CandidateSource(it, isMovie = false, fallbackUpcoming = false)) }
                pools.upcomingRegion.take(16).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = true)) }
                pools.nowPlayingRegion.take(16).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = false)) }
                pools.discoverUpcomingTv.take(12).forEach { add(CandidateSource(it, isMovie = false, fallbackUpcoming = true)) }
                pools.onTheAirTv.take(14).forEach { add(CandidateSource(it, isMovie = false, fallbackUpcoming = false)) }
                pools.discoverUpcomingMovies.take(12).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = true)) }
                pools.airingTodayTv.take(10).forEach { add(CandidateSource(it, isMovie = false, fallbackUpcoming = false)) }
                pools.upcomingGlobal.take(10).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = true)) }
                pools.nowPlayingGlobal.take(10).forEach { add(CandidateSource(it, isMovie = true, fallbackUpcoming = false)) }
            }

            // Strict deduplication by TMDB (type + id) AND normalized title before fetching videos
            val seenIds = HashSet<String>()
            val seenTitles = HashSet<String>()
            val uniqueCandidates = orderedSources.filter { src ->
                val rawTitle = (src.dto.title ?: src.dto.name ?: "").trim()
                val titleLower = rawTitle.lowercase()
                if (rawTitle.isBlank() || titleLower.contains("panchayat") || titleLower == "rrr") {
                    return@filter false
                }
                val idKey = "${if (src.isMovie) "m" else "tv"}_${src.dto.id}"
                val titleKey = "${if (src.isMovie) "m" else "tv"}_${normalizeTrailerTitle(rawTitle)}"
                seenIds.add(idKey) && seenTitles.add(titleKey)
            }.take(65)

            // Resolve playable YouTube trailers in parallel with bounded concurrency
            val semaphore = Semaphore(10)
            val liveTrailers = coroutineScope {
                uniqueCandidates.map { candidate ->
                    async {
                        semaphore.withPermit {
                            try {
                                val media = candidate.dto
                                val isMovie = candidate.isMovie
                                val dateStr = media.releaseDate ?: media.firstAirDate ?: ""
                                var isWithinWindow = true
                                var isFuture = candidate.fallbackUpcoming

                                if (dateStr.isNotBlank()) {
                                    val parsedDate = runCatching { dateFormat.parse(dateStr) }.getOrNull()
                                    if (parsedDate != null) {
                                        if (parsedDate.time < sixtyDaysAgoMillis) {
                                            isWithinWindow = false
                                        }
                                        // Date deterministically defines Now Playing vs Upcoming (mutually exclusive)
                                        isFuture = parsedDate.time > todayStartMillis
                                    }
                                }

                                if (!isWithinWindow) return@withPermit null

                                val includeLangs = listOfNotNull(
                                    safeLang?.lowercase(),
                                    media.originalLanguage?.lowercase(),
                                    "en",
                                    "null"
                                ).distinct().joinToString(",")

                                val videos = if (isMovie) {
                                    TmdbClient.api.getMovieVideos(
                                        movieId = media.id,
                                        apiKey = apiKey,
                                        language = safeLang,
                                        includeVideoLanguage = includeLangs
                                    )
                                } else {
                                    TmdbClient.api.getTvVideos(
                                        seriesId = media.id,
                                        apiKey = apiKey,
                                        language = safeLang,
                                        includeVideoLanguage = includeLangs
                                    )
                                }

                                val youtubeVideos = videos.results.filter {
                                    it.site.equals("YouTube", ignoreCase = true) &&
                                    it.key.isNotBlank() &&
                                    (it.type.equals("Trailer", ignoreCase = true) || it.type.equals("Teaser", ignoreCase = true))
                                }

                                if (youtubeVideos.isEmpty()) return@withPermit null

                                // Strictly prioritize Official Trailers -> Trailers -> Official Teasers -> Teasers
                                // So if a title has BOTH Trailer and Teaser, ONLY Trailer is selected!
                                val prioritizedVideos = youtubeVideos.sortedWith(
                                    compareBy<TmdbVideoDto>(
                                        { if (it.type.equals("Trailer", ignoreCase = true)) 0 else 1 },
                                        { if (it.official != false) 0 else 1 },
                                        {
                                            val vLang = it.iso6391?.lowercase()
                                            when {
                                                !safeLang.isNullOrBlank() && vLang == safeLang.lowercase() -> 0
                                                !media.originalLanguage.isNullOrBlank() && vLang == media.originalLanguage.lowercase() -> 1
                                                vLang == "en" -> 2
                                                else -> 3
                                            }
                                        }
                                    ).thenByDescending { it.size ?: 0 }
                                     .thenByDescending { it.publishedAt.orEmpty() }
                                )

                                val playableVideo = prioritizedVideos.take(5).firstOrNull { video ->
                                    TmdbClient.isYouTubeVideoPlayable(video.key)
                                } ?: return@withPermit null

                                val title = media.title ?: media.name ?: "Unknown"
                                val year = dateStr.take(4).toIntOrNull() ?: todayCal.get(Calendar.YEAR)
                                val poster = media.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
                                val backdrop = media.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: poster
                                val mediaOriginCountry = media.originCountry?.firstOrNull()
                                    ?: safeRegion
                                    ?: if (media.originalLanguage == "en") "US" else "GLOBAL"

                                TrailerItem(
                                    id = "live_${if (isMovie) "m" else "tv"}_${media.id}",
                                    mediaId = if (isMovie) "tmdb_m_${media.id}" else "tmdb_tv_${media.id}",
                                    title = title,
                                    videoKey = playableVideo.key,
                                    videoTitle = playableVideo.name,
                                    videoType = if (playableVideo.type.equals("Teaser", ignoreCase = true)) "Teaser" else "Trailer",
                                    mediaType = if (isMovie) MediaType.MOVIE else MediaType.TV_SHOW,
                                    releaseYear = year,
                                    genres = TmdbClient.mapGenres(media.genreIds),
                                    posterUrl = poster,
                                    backdropUrl = backdrop,
                                    communityRating = (media.voteAverage ?: 7.5).toFloat(),
                                    overview = media.overview.orEmpty(),
                                    originCountry = mediaOriginCountry,
                                    language = media.originalLanguage ?: (safeLang ?: "en"),
                                    releaseDate = dateStr,
                                    releaseStatus = if (isFuture) "UPCOMING" else "NOW_PLAYING"
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (liveTrailers.isNotEmpty()) {
                // Guarantee strictly one entry per title across the entire feed (never duplicate Now Playing & Upcoming)
                val finalTrailers = liveTrailers
                    .distinctBy { it.mediaId }
                    .distinctBy { normalizeTrailerTitle(it.title) to it.mediaType }
                    .sortedWith(comparator)
                return@withContext finalTrailers
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext initialFallbackList.sortedWith(comparator)
    }
}
