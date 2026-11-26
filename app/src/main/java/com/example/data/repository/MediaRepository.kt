package com.example.data.repository

import java.text.SimpleDateFormat
import java.util.Locale
import com.example.data.local.CachedPersonDao
import com.example.data.local.CachedPersonEntity
import com.example.data.local.CachedSearchDao
import com.example.data.local.CachedSearchResultEntity
import com.example.data.local.CachedTvEpisodeEntity
import com.example.data.local.CustomCollectionDao
import com.example.data.local.CustomCollectionEntity
import com.example.data.local.FavoritePersonDao
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaEntity
import com.example.data.local.PopularPersonDao
import com.example.data.local.PopularPersonEntity
import com.example.data.local.PreferencesDao
import com.example.data.local.SampleMediaData
import com.example.data.local.SampleTrailerData
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.TvEpisodeCacheDao
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.CastMemberItem
import com.example.data.model.HomeCatalogSection
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
import com.example.data.model.CsvImportResult
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.TvWatchStatus
import com.example.util.CsvLibraryPorter
import com.example.util.OfflineCachePolicy
import com.example.util.OnboardingRegionalPolicy
import com.example.util.PersonalizationEngine
import com.example.util.TrendingRotationPolicy
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class MediaRepository(
    private val mediaDao: MediaDao,
    private val preferencesDao: PreferencesDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val collectionDao: CustomCollectionDao,
    private val tvProgressDao: TvProgressDao,
    private val favoritePersonDao: FavoritePersonDao,
    private val firestoreSyncManager: FirestoreSyncManager? = null,
    private val popularPersonDao: PopularPersonDao? = null,
    private val tvEpisodeCacheDao: TvEpisodeCacheDao? = null,
    private val cachedPersonDao: CachedPersonDao? = null,
    private val cachedSearchDao: CachedSearchDao? = null
) {

    private val syncMutex = Mutex()
    private val baseSeasonsCache = ConcurrentHashMap<String, List<SeasonItem>>()

    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()
    val trackedMedia: Flow<List<MediaEntity>> = mediaDao.getTrackedMedia()
    val continueWatching: Flow<List<MediaEntity>> = mediaDao.getContinueWatching()
    val favorites: Flow<List<MediaEntity>> = mediaDao.getFavorites()
    val favoritePeople: Flow<List<FavoritePersonEntity>> = favoritePersonDao.getAllFavoritePeople()
    val popularPeople: Flow<List<PopularPersonEntity>> = popularPersonDao?.getAllPopularPeople() ?: flowOf(emptyList())
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

    suspend fun syncWithCloud(userId: String): FirestoreSyncManager.CloudEntitlement? = withContext(Dispatchers.IO) {
        val sync = firestoreSyncManager ?: return@withContext null
        if (userId.isBlank()) return@withContext null

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

            cloudData?.entitlement
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

    suspend fun recordSearchQuery(query: String, resultCount: Int = 0) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            val existing = searchHistoryDao.getByQuery(trimmed)
            val resolvedCount = if (resultCount > 0) resultCount else (existing?.resultCount ?: 0)
            if (existing != null && !existing.query.equals(trimmed, ignoreCase = false)) {
                searchHistoryDao.deleteQuery(existing.query)
            }
            searchHistoryDao.insertOrUpdate(
                SearchHistoryEntity(
                    query = trimmed,
                    timestamp = System.currentTimeMillis(),
                    resultCount = resolvedCount
                )
            )
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadSearchHistory(uid, trimmed, resolvedCount)
            }
        }
    }

    suspend fun updateRecordedSearchResultCount(query: String, resultCount: Int) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2 || resultCount <= 0) return@withContext
        val existing = searchHistoryDao.getByQuery(trimmed) ?: return@withContext
        if (existing.resultCount != resultCount) {
            searchHistoryDao.updateResultCount(trimmed, resultCount)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadSearchHistory(uid, existing.query, resultCount)
            }
        }
    }

    suspend fun removeSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext
        searchHistoryDao.deleteQuery(trimmed)
        cachedSearchDao?.deleteForQuery(OfflineCachePolicy.normalizeSearchQuery(trimmed))
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.deleteSearchHistory(uid, trimmed)
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAll()
        cachedSearchDao?.clearAll()
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.clearSearchHistory(uid)
        }
    }

    private suspend fun clearLocalUserDataLocked() {
        mediaDao.resetAllTrackingData()
        mediaDao.deleteCustomMedia()
        tvProgressDao.clearAll()
        searchHistoryDao.clearAll()
        cachedSearchDao?.clearAll()
        collectionDao.clearAll()
        favoritePersonDao.clearAll()
        baseSeasonsCache.clear()
    }

    suspend fun clearAllUserData(): Unit = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            clearLocalUserDataLocked()
            firestoreSyncManager?.setLastSyncedUserId(null)
        }
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val sampleUpcomingIds = setOf("m_ramayana", "m_moana_two", "tv_house_of_dragon", "tv_squid_game")
        val sampleNowPlayingIds = setOf(
            "m_gladiator_two", "m_wicked", "m_moana_two", "m_singham_again",
            "m_bb3", "m_pushpa_2", "m_wild_robot", "tv_the_penguin",
            "tv_shogun", "tv_fallout", "tv_the_bear", "tv_queen_of_tears", "tv_severance"
        )
        val initialCatalog = SampleMediaData.getInitialCatalog().map { item ->
            val tags = buildList {
                add(HomeCatalogSection.POPULAR.feedTag)
                if (item.id in sampleNowPlayingIds) add(HomeCatalogSection.NOW_PLAYING.feedTag)
                if (item.id in sampleUpcomingIds) add(HomeCatalogSection.UPCOMING.feedTag)
            }.joinToString(",")
            item.copy(feedTags = mergeFeedTagString(item.feedTags, tags))
        }
        if (mediaDao.countTotal() == 0) {
            mediaDao.insertAll(initialCatalog)
        } else {
            // Synchronize verified TMDB images and baseline catalog feed tags into existing Room cache
            val existingMap = mediaDao.getByIds(initialCatalog.map { it.id }).associateBy { it.id }
            initialCatalog.forEach { sampleItem ->
                mediaDao.updateMediaImages(sampleItem.id, sampleItem.posterUrl, sampleItem.backdropUrl)
                val existing = existingMap[sampleItem.id]
                if (existing != null && existing.feedTags.isBlank()) {
                    mediaDao.updateFeedTags(sampleItem.id, sampleItem.feedTags)
                }
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

    private fun mergeFeedTagString(existingTags: String, newTags: String): String {
        val parts = (existingTags.split(",") + newTags.split(","))
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
        return parts.joinToString(",")
    }

    private fun isExcludedLegacyCatalogTitle(item: MediaEntity, enforceRecentYear: Boolean = true): Boolean {
        val titleLower = item.title.lowercase().trim()
        val legacyTitle = titleLower.contains("panchayat") ||
            titleLower == "rrr" ||
            titleLower == "inception" ||
            titleLower == "fight club" ||
            titleLower == "parasite" ||
            titleLower == "the dark knight" ||
            titleLower == "pulp fiction" ||
            titleLower == "the shawshank redemption"
        return legacyTitle || (enforceRecentYear && item.releaseYear < 2023 && !item.isCustom)
    }

    suspend fun upsertWithFeedTag(items: List<MediaEntity>, feedTag: String) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val cleanTag = feedTag.trim().uppercase()
        val uniqueItems = items.distinctBy { it.id }
        val existingMap = uniqueItems.map { it.id }
            .chunked(400)
            .flatMap { chunk -> mediaDao.getByIds(chunk) }
            .associateBy { it.id }

        val toInsert = mutableListOf<MediaEntity>()
        val toUpdateTags = mutableListOf<Pair<String, String>>()

        uniqueItems.forEach { item ->
            val existing = existingMap[item.id]
            if (existing == null) {
                toInsert.add(item.copy(feedTags = mergeFeedTagString(item.feedTags, cleanTag)))
            } else if (!existing.hasFeedTag(cleanTag)) {
                toUpdateTags.add(existing.id to mergeFeedTagString(existing.feedTags, cleanTag))
            }
        }

        if (toInsert.isNotEmpty()) {
            mediaDao.insertAll(toInsert)
        }
        toUpdateTags.forEach { (id, merged) ->
            mediaDao.updateFeedTags(id, merged)
        }
    }

    suspend fun getPagedHomeCatalogFromDb(
        section: HomeCatalogSection,
        mediaType: MediaType?,
        genreFilter: String?,
        languageFilter: String?,
        limit: Int,
        offset: Int
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        val typeStr = mediaType?.name
        val safeGenre = genreFilter?.takeIf { it.isNotBlank() }
        val safeLang = languageFilter?.takeIf { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
        val feedItems = mediaDao.getPagedMediaByFeed(
            feedTag = section.feedTag,
            mediaType = typeStr,
            genreFilter = safeGenre,
            languageFilter = safeLang,
            limit = limit,
            offset = offset
        )
        if (feedItems.isNotEmpty() || section != HomeCatalogSection.POPULAR) {
            feedItems
        } else {
            mediaDao.getPagedAllCatalogMedia(
                mediaType = typeStr,
                genreFilter = safeGenre,
                languageFilter = safeLang,
                limit = limit,
                offset = offset
            )
        }
    }

    suspend fun fetchHomeCatalogPage(
        section: HomeCatalogSection,
        apiKey: String,
        mediaType: MediaType?,
        language: String?,
        region: String?,
        page: Int
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        if (section == HomeCatalogSection.POPULAR_PEOPLE) return@withContext emptyList()
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext emptyList()
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (!region.isNullOrBlank() && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) {
            region.uppercase()
        } else null

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFmt.format(Calendar.getInstance().time)
        val tomorrowStr = dateFmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)

        val wantMovies = mediaType == null || mediaType == MediaType.MOVIE
        val wantTv = mediaType == null || mediaType == MediaType.TV_SHOW

        try {
            val (movieEntities, tvEntities) = coroutineScope {
                val moviesDeferred = async {
                    if (!wantMovies) return@async emptyList()
                    when (section) {
                        HomeCatalogSection.POPULAR -> {
                            val list = mutableListOf<MediaEntity>()
                            if (safeRegion == "IN" && page <= 3) {
                                runCatching {
                                    TmdbClient.api.discoverMovies(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = "IN",
                                        watchRegion = "IN",
                                        withOriginCountry = "IN",
                                        sortBy = "popularity.desc",
                                        page = page
                                    ).results.map { TmdbClient.toMediaEntity(it).copy(originCountry = "IN") }
                                }.getOrNull()?.let { list.addAll(it) }
                            }
                            runCatching {
                                TmdbClient.api.getPopularMovies(
                                    apiKey = apiKey,
                                    region = safeRegion,
                                    language = safeLanguage,
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = true) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.NOW_PLAYING -> {
                            val list = mutableListOf<MediaEntity>()
                            runCatching {
                                TmdbClient.api.getNowPlayingMovies(
                                    apiKey = apiKey,
                                    region = safeRegion,
                                    language = safeLanguage,
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            if (safeRegion != null && list.size < 10) {
                                runCatching {
                                    TmdbClient.api.getNowPlayingMovies(
                                        apiKey = apiKey,
                                        region = null,
                                        language = safeLanguage,
                                        page = page
                                    ).results.map { TmdbClient.toMediaEntity(it) }
                                }.getOrNull()?.let { list.addAll(it) }
                            }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = false) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.UPCOMING -> {
                            val list = mutableListOf<MediaEntity>()
                            runCatching {
                                TmdbClient.api.getUpcomingMovies(
                                    apiKey = apiKey,
                                    region = safeRegion,
                                    language = safeLanguage,
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            if (list.size < 12 || page > 1) {
                                runCatching {
                                    TmdbClient.api.discoverMovies(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = safeRegion,
                                        primaryReleaseDateGte = tomorrowStr,
                                        sortBy = "popularity.desc",
                                        page = page
                                    ).results.map { TmdbClient.toMediaEntity(it) }
                                }.getOrNull()?.let { list.addAll(it) }
                            }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = true) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.POPULAR_PEOPLE -> emptyList()
                    }
                }

                val tvDeferred = async {
                    if (!wantTv) return@async emptyList()
                    when (section) {
                        HomeCatalogSection.POPULAR -> {
                            val list = mutableListOf<MediaEntity>()
                            if (safeRegion == "IN" && page <= 3) {
                                runCatching {
                                    TmdbClient.api.discoverTv(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = "IN",
                                        withOriginCountry = "IN",
                                        sortBy = "popularity.desc",
                                        page = page
                                    ).results.map { TmdbClient.toMediaEntity(it).copy(originCountry = "IN") }
                                }.getOrNull()?.let { list.addAll(it) }
                            }
                            runCatching {
                                TmdbClient.api.getPopularTv(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = false) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.NOW_PLAYING -> {
                            val list = mutableListOf<MediaEntity>()
                            runCatching {
                                TmdbClient.api.getOnTheAirTv(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            if (page == 1) {
                                runCatching {
                                    TmdbClient.api.getAiringTodayTv(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        page = 1
                                    ).results.map { TmdbClient.toMediaEntity(it) }
                                }.getOrNull()?.let { list.addAll(it) }
                            }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = false) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.UPCOMING -> {
                            val list = mutableListOf<MediaEntity>()
                            runCatching {
                                TmdbClient.api.discoverTv(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    firstAirDateGte = todayStr,
                                    sortBy = "popularity.desc",
                                    page = page
                                ).results.map { TmdbClient.toMediaEntity(it) }
                            }.getOrNull()?.let { list.addAll(it) }
                            list.filter { !isExcludedLegacyCatalogTitle(it, enforceRecentYear = false) }
                                .distinctBy { it.id }
                        }

                        HomeCatalogSection.POPULAR_PEOPLE -> emptyList()
                    }
                }

                moviesDeferred.await() to tvDeferred.await()
            }

            // Interleave movies and TV evenly when All Media is selected
            val combined = buildList {
                val maxLen = maxOf(movieEntities.size, tvEntities.size)
                for (i in 0 until maxLen) {
                    if (i < movieEntities.size) add(movieEntities[i])
                    if (i < tvEntities.size) add(tvEntities[i])
                }
            }.distinctBy { it.id }
                .map { it.copy(feedTags = mergeFeedTagString(it.feedTags, section.feedTag)) }

            if (combined.isNotEmpty()) {
                upsertWithFeedTag(combined, section.feedTag)
            }
            combined
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchPopularMediaFromTmdb(apiKey: String, language: String?, region: String?) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        try {
            coroutineScope {
                async { fetchHomeCatalogPage(HomeCatalogSection.POPULAR, apiKey, null, language, region, page = 1) }
                async { fetchHomeCatalogPage(HomeCatalogSection.NOW_PLAYING, apiKey, null, language, region, page = 1) }
                async { fetchHomeCatalogPage(HomeCatalogSection.UPCOMING, apiKey, null, language, region, page = 1) }
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
        val watchedIds = runCatching {
            mediaDao.getAllMediaList()
                .asSequence()
                .filter { it.status == WatchStatus.COMPLETED.name }
                .map { it.id }
                .toHashSet()
        }.getOrDefault(emptySet())

        val isEligibleTrending: (MediaEntity) -> Boolean = {
            val titleLower = it.title.lowercase().trim()
            !titleLower.contains("panchayat") &&
                titleLower != "rrr" &&
                titleLower != "inception" &&
                titleLower != "fight club" &&
                titleLower != "parasite" &&
                it.releaseYear >= 2023
        }

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

                val filteredRegional = regionalEntities.filter(isEligibleTrending).distinctBy { it.id }
                val filteredGlobal = globalTrending.filter(isEligibleTrending).distinctBy { it.id }

                val rotated = TrendingRotationPolicy.rotateTrendingForWindow(
                    regionalHits = filteredRegional,
                    globalHits = filteredGlobal,
                    watchedIds = watchedIds,
                    region = region,
                    timeWindow = timeWindow
                )

                if (rotated.isNotEmpty()) {
                    mediaDao.insertAll(rotated)
                    return@withContext rotated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback to initial catalog sorted for this region (excluding legacy titles)
        val sortedFallback = SampleMediaData.getInitialCatalog()
            .filter(isEligibleTrending)
            .sortedWith(
                compareByDescending<MediaEntity> {
                    if (it.originCountry.equals(region, ignoreCase = true)) 1 else 0
                }.thenByDescending { it.releaseYear }
                    .thenByDescending { it.communityRating }
            )
        return@withContext TrendingRotationPolicy.rotateTrendingForWindow(
            regionalHits = sortedFallback.filter { it.originCountry.equals(region, ignoreCase = true) },
            globalHits = sortedFallback.filterNot { it.originCountry.equals(region, ignoreCase = true) },
            watchedIds = watchedIds,
            region = region,
            timeWindow = timeWindow
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
        val seeds = getCuratedTopRatedMovies()
        val hasLiveKey = apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY"
        val existingMap = runCatching {
            mediaDao.getByIds(seeds.map { it.id }).associateBy { it.id }
        }.getOrDefault(emptyMap())

        val hydrated = coroutineScope {
            seeds.map { seed ->
                async {
                    val existing = existingMap[seed.id]
                    val needsLiveHydration = hasLiveKey && (
                        existing == null ||
                            existing.posterUrl.isBlank() ||
                            existing.cast.isBlank() ||
                            existing.streamingProviders.isBlank()
                        )
                    val resolved = if (needsLiveHydration) {
                        val tmdbId = seed.id.removePrefix("tmdb_m_").toLongOrNull()
                        val detail = if (tmdbId != null) {
                            runCatching { TmdbClient.api.getMovieDetails(tmdbId, apiKey) }.getOrNull()
                        } else {
                            null
                        }
                        if (detail != null) {
                            TmdbClient.movieDetailToEntity(detail, existing ?: seed).copy(
                                communityRating = seed.communityRating,
                                feedTags = mergeFeedTagString((existing ?: seed).feedTags, "IMDB_TOP_10_MOVIE")
                            ).also { fresh ->
                                runCatching { mediaDao.insertOrUpdate(fresh) }
                            }
                        } else {
                            (existing ?: seed).copy(communityRating = seed.communityRating)
                        }
                    } else {
                        (existing ?: seed).copy(
                            posterUrl = existing?.posterUrl?.takeIf { it.isNotBlank() } ?: seed.posterUrl,
                            backdropUrl = existing?.backdropUrl?.takeIf { it.isNotBlank() } ?: seed.backdropUrl,
                            communityRating = seed.communityRating
                        )
                    }
                    resolved
                }
            }.awaitAll()
        }

        upsertWithFeedTag(hydrated, "IMDB_TOP_10_MOVIE")
        hydrated
    }

    suspend fun getTopRatedTv(apiKey: String, language: String?): List<MediaEntity> = withContext(Dispatchers.IO) {
        val seeds = getCuratedTopRatedTv()
        val hasLiveKey = apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY"
        val existingMap = runCatching {
            mediaDao.getByIds(seeds.map { it.id }).associateBy { it.id }
        }.getOrDefault(emptyMap())

        val hydrated = coroutineScope {
            seeds.map { seed ->
                async {
                    val existing = existingMap[seed.id]
                    val needsLiveHydration = hasLiveKey && (
                        existing == null ||
                            existing.posterUrl.isBlank() ||
                            existing.cast.isBlank() ||
                            existing.streamingProviders.isBlank()
                        )
                    val resolved = if (needsLiveHydration) {
                        val tmdbId = seed.id.removePrefix("tmdb_tv_").toLongOrNull()
                        val detail = if (tmdbId != null) {
                            runCatching { TmdbClient.api.getTvDetails(tmdbId, apiKey) }.getOrNull()
                        } else {
                            null
                        }
                        if (detail != null) {
                            TmdbClient.tvDetailToEntity(detail, existing ?: seed).copy(
                                communityRating = seed.communityRating,
                                feedTags = mergeFeedTagString((existing ?: seed).feedTags, "IMDB_TOP_10_TV")
                            ).also { fresh ->
                                runCatching { mediaDao.insertOrUpdate(fresh) }
                            }
                        } else {
                            (existing ?: seed).copy(communityRating = seed.communityRating)
                        }
                    } else {
                        (existing ?: seed).copy(
                            posterUrl = existing?.posterUrl?.takeIf { it.isNotBlank() } ?: seed.posterUrl,
                            backdropUrl = existing?.backdropUrl?.takeIf { it.isNotBlank() } ?: seed.backdropUrl,
                            communityRating = seed.communityRating
                        )
                    }
                    resolved
                }
            }.awaitAll()
        }

        upsertWithFeedTag(hydrated, "IMDB_TOP_10_TV")
        hydrated
    }

    fun getCuratedTopRatedMovies(): List<MediaEntity> = imdbTop10Movies()

    fun getCuratedTopRatedTv(): List<MediaEntity> = imdbTop10TvShows()

    companion object {
        fun imdbTop10Movies(): List<MediaEntity> {
            return listOf(
            MediaEntity(
                id = "tmdb_m_278",
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
                id = "tmdb_m_238",
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
                id = "tmdb_m_155",
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
                id = "tmdb_m_240",
                title = "The Godfather Part II",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1974,
                genres = "Drama, Crime",
                posterUrl = "https://image.tmdb.org/t/p/w500/hek3koDUyRQk7FIhPXsa6mT2Zc3.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/kGzFbGhp99zva6oZODW5atUtnqi.jpg",
                overview = "In the continuing saga of the Corleone crime family, a young Vito Corleone grows up in Sicily and 1910s New York, while Michael attempts to expand the family syndicate.",
                directorOrCreator = "Francis Ford Coppola",
                cast = "Al Pacino, Robert De Niro, Robert Duvall, Diane Keaton",
                communityRating = 9.0f,
                runtimeMinutes = 202,
                originCountry = "US"
            ),
            MediaEntity(
                id = "tmdb_m_389",
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
                id = "tmdb_m_122",
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
                id = "tmdb_m_424",
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
                id = "tmdb_m_680",
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
                id = "tmdb_m_120",
                title = "The Lord of the Rings: The Fellowship of the Ring",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 2001,
                genres = "Adventure, Fantasy, Action",
                posterUrl = "https://image.tmdb.org/t/p/w500/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/x2RS3uTcsJJ9IfjNPcgDmukoEcQ.jpg",
                overview = "Young hobbit Frodo Baggins, after inheriting a mysterious ring from his uncle Bilbo, must leave his home in order to keep it from falling into the hands of its evil creator.",
                directorOrCreator = "Peter Jackson",
                cast = "Elijah Wood, Ian McKellen, Viggo Mortensen, Sean Astin",
                communityRating = 8.9f,
                runtimeMinutes = 178,
                originCountry = "NZ"
            ),
            MediaEntity(
                id = "tmdb_m_429",
                title = "The Good, the Bad and the Ugly",
                mediaType = MediaType.MOVIE.name,
                releaseYear = 1966,
                genres = "Western",
                posterUrl = "https://image.tmdb.org/t/p/w500/bX2xnavhMYjWDoZp1VM6VnU1xwe.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/Adrip2Jqzw56KeuV2nAxucKMNXA.jpg",
                overview = "While the Civil War rages on between the Union and the Confederacy, three men—a quiet loner, a ruthless hitman, and a Mexican bandit—comb the American Southwest in search of buried gold.",
                directorOrCreator = "Sergio Leone",
                cast = "Clint Eastwood, Eli Wallach, Lee Van Cleef",
                communityRating = 8.8f,
                runtimeMinutes = 161,
                originCountry = "IT"
            )
        )
        }

        fun imdbTop10TvShows(): List<MediaEntity> {
            return listOf(
            MediaEntity(
                id = "tmdb_tv_1396",
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
                id = "tmdb_tv_68595",
                title = "Planet Earth II",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2016,
                genres = "Documentary",
                posterUrl = "https://image.tmdb.org/t/p/w500/wu444tM9YBvrzn9u51eL1gT5g3n.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg",
                overview = "David Attenborough returns with a new wildlife documentary showcasing life across islands, mountains, jungles, deserts, grasslands, and cities.",
                directorOrCreator = "David Attenborough",
                cast = "David Attenborough",
                communityRating = 9.5f,
                runtimeMinutes = 60,
                totalSeasons = 1,
                totalEpisodes = 6,
                originCountry = "GB"
            ),
            MediaEntity(
                id = "tmdb_tv_1044",
                title = "Planet Earth",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2006,
                genres = "Documentary",
                posterUrl = "https://image.tmdb.org/t/p/w500/mf7xQ3hL6qR7oC3i5O3fN2L3oR0.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/a7M1mE1vW8fD3J4qK6oZ0vG6W8.jpg",
                overview = "Emmy-winning landmark nature documentary series exploring the rarest habitats and wildlife across every continent on Earth.",
                directorOrCreator = "Alastair Fothergill",
                cast = "David Attenborough",
                communityRating = 9.4f,
                runtimeMinutes = 50,
                totalSeasons = 1,
                totalEpisodes = 11,
                originCountry = "GB"
            ),
            MediaEntity(
                id = "tmdb_tv_4613",
                title = "Band of Brothers",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2001,
                genres = "War, Drama, History",
                posterUrl = "https://image.tmdb.org/t/p/w500/zReOJYste13Qq3T3B5Oy2e31K7A.jpg",
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
                id = "tmdb_tv_87108",
                title = "Chernobyl",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2019,
                genres = "Drama, History",
                posterUrl = "https://image.tmdb.org/t/p/w500/hlLXt2tOPT6RRnjiUmoxyG1LTFi.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/900tHlUYUkp7Ol04XFSoAaEIXcT.jpg",
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
                id = "tmdb_tv_1434",
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
                id = "tmdb_tv_246",
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
                id = "tmdb_tv_1398",
                title = "The Sopranos",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 1999,
                genres = "Drama, Crime",
                posterUrl = "https://image.tmdb.org/t/p/w500/rTc7ZXdroqjkKivFPvCPX0Ru7uw.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/lNpkvX2s8LGB0mjGODMT4o6Up7j.jpg",
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
                id = "tmdb_tv_58474",
                title = "Cosmos: A Spacetime Odyssey",
                mediaType = MediaType.TV_SHOW.name,
                releaseYear = 2014,
                genres = "Documentary, Science",
                posterUrl = "https://image.tmdb.org/t/p/w500/o6i2S8J2h9i3wD4mH3n2V0D2G3U.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/a7M1mE1vW8fD3J4qK6oZ0vG6W8.jpg",
                overview = "Astrophysicist Neil deGrasse Tyson hosts a thrilling journey across space and time to uncover the laws of nature and our coordinates in the cosmos.",
                directorOrCreator = "Ann Druyan, Seth MacFarlane",
                cast = "Neil deGrasse Tyson",
                communityRating = 9.2f,
                runtimeMinutes = 43,
                totalSeasons = 1,
                totalEpisodes = 13,
                originCountry = "US"
            ),
            MediaEntity(
                id = "tmdb_tv_1399",
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
            )
        )
        }
    }

    private suspend fun cacheSearchResultsInRoom(
        cacheKey: String,
        results: List<TmdbSearchResultItem>,
        entitiesToCache: List<MediaEntity>
    ) {
        if (entitiesToCache.isNotEmpty()) {
            mediaDao.insertAll(entitiesToCache.distinctBy { it.id })
        }
        val people = results.filterIsInstance<TmdbSearchResultItem.Person>().filter { it.id > 0L }
        if (people.isNotEmpty()) {
            cachedPersonDao?.insertAll(people.map { CachedPersonEntity.fromPerson(it) })
        }
        val dao = cachedSearchDao ?: return
        val normalized = OfflineCachePolicy.normalizeSearchQuery(cacheKey)
        if (normalized.isBlank()) return
        val now = System.currentTimeMillis()
        dao.deleteForQuery(normalized)
        val entries = results.mapIndexedNotNull { idx, item ->
            when (item) {
                is TmdbSearchResultItem.Media -> CachedSearchResultEntity(
                    normalizedQuery = normalized,
                    resultKey = "m_${item.entity.id}",
                    resultType = "MEDIA",
                    mediaId = item.entity.id,
                    position = idx,
                    updatedAt = now
                )
                is TmdbSearchResultItem.Person -> CachedSearchResultEntity(
                    normalizedQuery = normalized,
                    resultKey = "p_${item.id}",
                    resultType = "PERSON",
                    personId = item.id,
                    position = idx,
                    updatedAt = now
                )
            }
        }
        if (entries.isNotEmpty()) {
            dao.insertAll(entries)
        }
    }

    private suspend fun loadCachedSearchResultsFromRoom(cacheKey: String): List<TmdbSearchResultItem> {
        val dao = cachedSearchDao ?: return emptyList()
        val normalized = OfflineCachePolicy.normalizeSearchQuery(cacheKey)
        if (normalized.isBlank()) return emptyList()
        val rows = dao.getResultsForQuery(normalized)
        if (rows.isEmpty()) return emptyList()

        val mediaIds = rows.filter { it.resultType == "MEDIA" && it.mediaId.isNotBlank() }.map { it.mediaId }.distinct()
        val mediaMap = if (mediaIds.isNotEmpty()) {
            mediaIds.chunked(400).flatMap { mediaDao.getByIds(it) }.associateBy { it.id }
        } else emptyMap()

        val personIds = rows.filter { it.resultType == "PERSON" && it.personId > 0L }.map { it.personId }.distinct()
        val personEntities = if (personIds.isNotEmpty()) {
            cachedPersonDao?.getByIds(personIds).orEmpty()
        } else emptyList()
        val knownForIds = personEntities.flatMap { p ->
            p.knownForIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct()
        val allMediaForPeople = if (knownForIds.isNotEmpty()) {
            mediaMap + knownForIds.chunked(400).flatMap { mediaDao.getByIds(it) }.associateBy { it.id }
        } else mediaMap
        val personMap = personEntities.associate { it.personId to it.toPersonItem(allMediaForPeople) }

        return rows.mapNotNull { row ->
            when (row.resultType) {
                "MEDIA" -> mediaMap[row.mediaId]?.let { entity ->
                    TmdbSearchResultItem.Media(
                        entity = entity,
                        isMovie = entity.getMediaTypeEnum() == MediaType.MOVIE
                    )
                }
                "PERSON" -> personMap[row.personId]
                else -> null
            }
        }
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

                if (results.isNotEmpty()) {
                    cacheSearchResultsInRoom(trimmed, results, entitiesToCache)
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

                val deduped = combined.distinctBy {
                    (it as TmdbSearchResultItem.Media).entity.id
                }
                if (deduped.isNotEmpty()) {
                    cacheSearchResultsInRoom("genre:${normalized.lowercase()}", deduped, entitiesToCache)
                    return@withContext deduped
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext discoverLocalByGenre(normalized)
    }

    suspend fun discoverLocalByGenre(genre: String): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val cachedGenre = runCatching {
            loadCachedSearchResultsFromRoom("genre:${genre.trim().lowercase()}")
        }.getOrDefault(emptyList())
        val allLocal = try {
            (mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog()).distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }
        val localMatches = allLocal.filter { item ->
            item.getGenreList().any { g ->
                g.equals(genre, ignoreCase = true) ||
                    g.contains(genre, ignoreCase = true) ||
                    (genre.equals("Sci-Fi", ignoreCase = true) && g.contains("Science Fiction", ignoreCase = true))
            }
        }.sortedWith(
            compareByDescending<MediaEntity> { if (it.isInWatchlist()) 1 else 0 }
                .thenByDescending { it.communityRating }
        ).map { entity ->
            TmdbSearchResultItem.Media(
                entity = entity,
                isMovie = entity.getMediaTypeEnum() == MediaType.MOVIE
            )
        }
        (cachedGenre + localMatches).distinctBy {
            when (it) {
                is TmdbSearchResultItem.Media -> "m_${it.entity.id}"
                is TmdbSearchResultItem.Person -> "p_${it.id}"
            }
        }
    }

    private suspend fun searchLocalMediaOnly(query: String): List<MediaEntity> = withContext(Dispatchers.IO) {
        val sqlMatches = runCatching { mediaDao.searchMediaOffline(query) }.getOrDefault(emptyList())
        val allLocal = try {
            (sqlMatches + mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog()).distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }
        OfflineCachePolicy.rankOfflineMediaMatches(allLocal, query)
    }

    suspend fun searchLocalAndSample(query: String): List<TmdbSearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TmdbSearchResultItem>()
        val seenKeys = HashSet<String>()

        // 1. Exact cached multi-search replay from Room (for re-running Search History offline)
        val cachedReplay = runCatching { loadCachedSearchResultsFromRoom(query) }.getOrDefault(emptyList())
        cachedReplay.forEach { item ->
            val key = when (item) {
                is TmdbSearchResultItem.Media -> "m_${item.entity.id}"
                is TmdbSearchResultItem.Person -> "p_${item.id}"
            }
            if (seenKeys.add(key)) {
                results.add(item)
            }
        }

        // 2. Local Room media_items (prioritizing saved watchlist items) + baseline catalog
        val sqlMatches = runCatching { mediaDao.searchMediaOffline(query) }.getOrDefault(emptyList())
        val fallbackPool = try {
            (sqlMatches + mediaDao.getWatchlistMediaDirect() + mediaDao.getAllMediaList() + SampleMediaData.getInitialCatalog())
                .distinctBy { it.id }
        } catch (_: Exception) {
            SampleMediaData.getInitialCatalog()
        }
        val rankedMedia = OfflineCachePolicy.rankOfflineMediaMatches(fallbackPool, query)
        rankedMedia.forEach { entity ->
            if (seenKeys.add("m_${entity.id}")) {
                results.add(
                    TmdbSearchResultItem.Media(
                        entity = entity,
                        isMovie = entity.getMediaTypeEnum() == MediaType.MOVIE
                    )
                )
            }
        }

        // 3. Cached people + favorite people + popular people + sample people
        val cachedPeopleRows = runCatching { cachedPersonDao?.searchByQuery(query).orEmpty() }.getOrDefault(emptyList())
        val cachedKnownForIds = cachedPeopleRows.flatMap { row ->
            row.knownForIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct()
        val cachedKnownForMap = if (cachedKnownForIds.isNotEmpty()) {
            runCatching {
                cachedKnownForIds.chunked(400).flatMap { mediaDao.getByIds(it) }.associateBy { it.id }
            }.getOrDefault(emptyMap())
        } else emptyMap()
        val cachedPeople = cachedPeopleRows.map { it.toPersonItem(cachedKnownForMap) }

        val favPeople = runCatching { favoritePersonDao.getAllFavoritePeopleList() }.getOrDefault(emptyList())
            .map { fav ->
                TmdbSearchResultItem.Person(
                    id = fav.personId,
                    name = fav.name,
                    profileUrl = fav.profileUrl.ifBlank { null },
                    department = fav.department.ifBlank { "Acting" },
                    knownFor = emptyList(),
                    popularity = 50.0
                )
            }

        val matchingPeople = OfflineCachePolicy.rankOfflinePeopleMatches(
            people = cachedPeople + favPeople + SampleMediaData.getSamplePeople(),
            query = query
        )
        matchingPeople.forEach { person ->
            val key = if (person.id > 0L) "p_${person.id}" else "p_${person.name.lowercase()}"
            if (seenKeys.add(key)) {
                results.add(person)
            }
        }

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
            if (id.startsWith("tmdb_m_")) {
                val tmdbId = id.removePrefix("tmdb_m_").toLongOrNull() ?: return@withContext emptyList()
                val detail = TmdbClient.api.getMovieDetails(tmdbId, apiKey)
                val latestExisting = mediaDao.getMediaByIdDirect(id)
                val updated = TmdbClient.movieDetailToEntity(detail, latestExisting)
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
                val latestExisting = mediaDao.getMediaByIdDirect(id)
                val updated = TmdbClient.tvDetailToEntity(detail, latestExisting)
                mediaDao.insertOrUpdate(updated)
                detail.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.takeIf { it.isNotEmpty() }?.let { tmdbSeasons ->
                    baseSeasonsCache["${id}_true"] = tmdbSeasons.map { s ->
                        val sNum = s.seasonNumber ?: 1
                        val poster = s.posterPath?.let { "${TmdbClient.IMAGE_BASE_URL}$it" } ?: updated.posterUrl
                        SeasonItem(
                            seriesId = id,
                            seriesTitle = updated.title,
                            seasonNumber = sNum,
                            name = s.name ?: "Season $sNum",
                            overview = s.overview.orEmpty().ifBlank { updated.overview },
                            posterUrl = poster,
                            backdropUrl = updated.backdropUrl,
                            episodeCount = (s.episodeCount ?: 10).coerceAtLeast(1),
                            releaseYear = (s.airDate ?: updated.releaseYear.toString()).take(4).toIntOrNull() ?: updated.releaseYear,
                            airDate = s.airDate ?: "${updated.releaseYear}-01-01",
                            rating = (s.voteAverage?.toFloat() ?: updated.communityRating)
                        )
                    }
                }
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

            val regionLabel = RegionalDefaults.getRegionLabel(regionCode)
            val streamItems = (regionData.flatrate.orEmpty()).mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 1),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.STREAM,
                    webLink = regionData.link,
                    directContentUrl = null
                )
            }
            val freeItems = (regionData.free.orEmpty() + regionData.ads.orEmpty()).mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 100),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.FREE,
                    webLink = regionData.link,
                    directContentUrl = null
                )
            }
            val rentItems = regionData.rent.orEmpty().mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 200),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.RENT,
                    webLink = regionData.link,
                    directContentUrl = null
                )
            }
            val buyItems = regionData.buy.orEmpty().mapIndexed { idx, p ->
                WatchProviderItem(
                    id = p.providerId ?: (idx + 300),
                    name = p.providerName,
                    logoUrl = p.logoPath?.let { "https://image.tmdb.org/t/p/w154$it" },
                    type = WatchProviderType.BUY,
                    webLink = regionData.link,
                    directContentUrl = null
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

            // Replace/upsert regional streaming providers tag cleanly without duplicating blocks
            val existing = mediaDao.getMediaByIdDirect(mediaId)
            val includedItems = (streamItems + freeItems).distinctBy { it.name }
            if (existing != null && includedItems.isNotEmpty()) {
                val upperReg = regionCode.uppercase()
                val newProviderNames = includedItems.joinToString(",") { it.name }
                val otherBlocks = existing.streamingProviders
                    .split("|")
                    .filter { it.isNotBlank() && !it.startsWith("$upperReg:", ignoreCase = true) }
                val updatedProviders = (otherBlocks + "$upperReg:$newProviderNames")
                    .joinToString("|")
                    .take(1900)
                mediaDao.updateStreamingProviders(mediaId, updatedProviders)
            }

            info
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun refreshWatchlistProvidersForAudit(
        regionCode: String,
        apiKey: String
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        val upperReg = regionCode.uppercase()
        val tracked = runCatching { mediaDao.getAllMediaList() }.getOrDefault(emptyList())
            .filter { item ->
                val st = item.getWatchStatusEnum()
                (st == WatchStatus.PLAN_TO_WATCH || st == WatchStatus.WATCHING || item.isFavorite) &&
                    (item.id.startsWith("tmdb_m_") || item.id.startsWith("tmdb_tv_")) &&
                    item.getProvidersForRegion(upperReg).isEmpty()
            }
            .take(24)

        if (tracked.isEmpty()) return@withContext
        val sem = Semaphore(6)
        coroutineScope {
            tracked.map { item ->
                async {
                    sem.withPermit {
                        fetchLiveWatchProviders(item.id, upperReg, apiKey)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun resolveTitleToMediaEntity(
        rawTitle: String,
        releaseYear: Int?,
        preferredType: MediaType,
        apiKey: String,
        regionCode: String
    ): MediaEntity? = withContext(Dispatchers.IO) {
        val clean = rawTitle.trim()
        if (clean.isBlank()) return@withContext null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val results = TmdbClient.api.searchMulti(
                    apiKey = apiKey,
                    query = clean,
                    language = null,
                    region = null
                ).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }

                val bestDto = results.maxByOrNull { dto ->
                    val dtoTitle = (dto.title ?: dto.name).orEmpty().trim()
                    val dtoYear = (dto.releaseDate ?: dto.firstAirDate).orEmpty().take(4).toIntOrNull()
                    val dtoIsMovie = dto.mediaType == "movie" || dto.title != null
                    var score = 0
                    if (dtoTitle.equals(clean, ignoreCase = true)) score += 50
                    else if (dtoTitle.contains(clean, ignoreCase = true) || clean.contains(dtoTitle, ignoreCase = true)) score += 20
                    if (releaseYear != null && dtoYear != null && kotlin.math.abs(dtoYear - releaseYear) <= 1) score += 25
                    if ((preferredType == MediaType.MOVIE && dtoIsMovie) || (preferredType == MediaType.TV_SHOW && !dtoIsMovie)) score += 15
                    score + ((dto.popularity ?: 0.0).coerceAtMost(20.0).toInt())
                }

                if (bestDto != null) {
                    val baseEntity = TmdbClient.toMediaEntity(bestDto)
                    val existing = mediaDao.getMediaByIdDirect(baseEntity.id)
                    val toSave = existing ?: baseEntity
                    if (existing == null) {
                        mediaDao.insertAll(listOf(baseEntity))
                    }
                    if (toSave.getProvidersForRegion(regionCode).isEmpty()) {
                        fetchLiveWatchProviders(toSave.id, regionCode, apiKey)
                    }
                    return@withContext mediaDao.getMediaByIdDirect(baseEntity.id) ?: toSave
                }
            } catch (_: Exception) {
            }
        }

        searchLocalMediaOnly(clean).firstOrNull()
    }

    suspend fun importCsvLibrary(
        csvContent: String,
        apiKey: String,
        regionCode: String
    ): CsvImportResult = withContext(Dispatchers.IO) {
        val parsed = CsvLibraryPorter.parseUniversalCsv(csvContent)
        if (parsed.rows.isEmpty()) {
            return@withContext CsvImportResult(
                importedCount = 0,
                enrichedCount = 0,
                detectedFormat = parsed.formatName,
                message = "No valid movie or TV rows found in ${parsed.formatName} file."
            )
        }

        val uid = currentAuthenticatedUserId()
        var imported = 0
        var enriched = 0
        val sem = Semaphore(6)
        val rowsToProcess = parsed.rows.take(150)

        val resolvedItems = coroutineScope {
            rowsToProcess.map { row ->
                async {
                    sem.withPermit {
                        var baseEntity: MediaEntity? = null
                        if (row.tmdbId != null && apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
                            baseEntity = runCatching {
                                if (row.mediaType == MediaType.TV_SHOW) {
                                    val tv = TmdbClient.api.getTvDetails(row.tmdbId, apiKey)
                                    TmdbClient.tvDetailToEntity(tv, mediaDao.getMediaByIdDirect("tmdb_tv_${row.tmdbId}"))
                                } else {
                                    val mov = TmdbClient.api.getMovieDetails(row.tmdbId, apiKey)
                                    TmdbClient.movieDetailToEntity(mov, mediaDao.getMediaByIdDirect("tmdb_m_${row.tmdbId}"))
                                }
                            }.getOrNull()
                        }

                        if (baseEntity == null) {
                            baseEntity = resolveTitleToMediaEntity(
                                rawTitle = row.title,
                                releaseYear = row.releaseYear,
                                preferredType = row.mediaType,
                                apiKey = apiKey,
                                regionCode = regionCode
                            )
                        }

                        val now = System.currentTimeMillis()
                        if (baseEntity != null) {
                            baseEntity.copy(
                                status = row.status.name,
                                userRating = if (row.userRatingOutOf5 > 0f) row.userRatingOutOf5 else baseEntity.userRating,
                                isFavorite = row.isFavorite || baseEntity.isFavorite,
                                customWatchlist = row.collectionName ?: baseEntity.customWatchlist,
                                watchedEpisodes = if (row.status == WatchStatus.COMPLETED && baseEntity.getMediaTypeEnum() == MediaType.MOVIE) 1 else baseEntity.watchedEpisodes,
                                lastWatchedDate = now
                            ) to true
                        } else {
                            val fallbackId = "import_" + row.title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24) + "_${row.releaseYear ?: 2024}"
                            MediaEntity(
                                id = fallbackId,
                                title = row.title,
                                mediaType = row.mediaType.name,
                                releaseYear = row.releaseYear ?: 2024,
                                genres = "Imported",
                                posterUrl = "",
                                backdropUrl = "",
                                overview = "Imported from ${parsed.formatName}",
                                directorOrCreator = "",
                                cast = "",
                                communityRating = 0f,
                                runtimeMinutes = if (row.mediaType == MediaType.MOVIE) 120 else 45,
                                totalSeasons = if (row.mediaType == MediaType.TV_SHOW) 1 else 0,
                                totalEpisodes = if (row.mediaType == MediaType.MOVIE) 1 else 10,
                                status = row.status.name,
                                userRating = row.userRatingOutOf5,
                                isFavorite = row.isFavorite,
                                customWatchlist = row.collectionName,
                                isCustom = true,
                                lastWatchedDate = now
                            ) to false
                        }
                    }
                }
            }.awaitAll()
        }

        val collectionsAdded = HashSet<String>()
        for ((entity, wasEnriched) in resolvedItems) {
            mediaDao.insertOrUpdate(entity)
            imported++
            if (wasEnriched) enriched++
            entity.customWatchlist?.takeIf { it.isNotBlank() && collectionsAdded.add(it) }?.let { collName ->
                val coll = CustomCollectionEntity(name = collName)
                collectionDao.insert(coll)
                if (!uid.isNullOrBlank()) {
                    firestoreSyncManager?.uploadCollection(uid, coll)
                }
            }
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, entity)
            }
        }

        CsvImportResult(
            importedCount = imported,
            enrichedCount = enriched,
            detectedFormat = parsed.formatName,
            message = "Imported $imported titles from ${parsed.formatName} ($enriched matched with live TMDB posters & metadata)."
        )
    }

    suspend fun getMediaByIdDirect(id: String): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaByIdDirect(id)
    }

    suspend fun updateStatus(id: String, status: WatchStatus, apiKey: String = "") = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val now = System.currentTimeMillis()
        val isTv = existing.getMediaTypeEnum() == MediaType.TV_SHOW
        val progressToUpload = mutableListOf<TvProgressEntity>()

        val newEpisodes = if (isTv) {
            when (status) {
                WatchStatus.COMPLETED -> {
                    val baseSeasons = fetchBaseSeasons(existing, apiKey).sortedBy { it.seasonNumber }
                    val entities = mutableListOf<TvProgressEntity>()
                    baseSeasons.forEach { s ->
                        entities.add(TvProgressEntity(id, s.seasonNumber, 0, "WATCHED", now))
                        for (ep in 1..s.episodeCount) {
                            entities.add(TvProgressEntity(id, s.seasonNumber, ep, "WATCHED", now))
                        }
                    }
                    if (entities.isNotEmpty()) {
                        tvProgressDao.insertAll(entities)
                        progressToUpload.addAll(entities)
                    }
                    val catalogTotal = baseSeasons.sumOf { it.episodeCount }
                    if (catalogTotal > 0) catalogTotal else existing.totalEpisodes
                }
                WatchStatus.PLAN_TO_WATCH -> {
                    val baseSeasons = fetchBaseSeasons(existing, apiKey).sortedBy { it.seasonNumber }
                    val entities = mutableListOf<TvProgressEntity>()
                    baseSeasons.forEach { s ->
                        entities.add(TvProgressEntity(id, s.seasonNumber, 0, "PLAN_TO_WATCH", now))
                        for (ep in 1..s.episodeCount) {
                            entities.add(TvProgressEntity(id, s.seasonNumber, ep, "UNWATCHED", now))
                        }
                    }
                    if (entities.isNotEmpty()) {
                        tvProgressDao.insertAll(entities)
                        progressToUpload.addAll(entities)
                    }
                    0
                }
                else -> existing.watchedEpisodes
            }
        } else {
            when (status) {
                WatchStatus.COMPLETED -> existing.totalEpisodes
                WatchStatus.PLAN_TO_WATCH -> 0
                else -> existing.watchedEpisodes
            }
        }

        val updated = existing.copy(
            status = status.name,
            watchedEpisodes = newEpisodes,
            totalEpisodes = maxOf(existing.totalEpisodes, newEpisodes),
            lastWatchedDate = now
        )
        mediaDao.insertOrUpdate(updated)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.uploadTrackedMedia(uid, updated)
            if (progressToUpload.isNotEmpty()) {
                firestoreSyncManager?.uploadTvProgressList(uid, progressToUpload)
            }
        }
    }

    suspend fun incrementEpisode(id: String, apiKey: String = "") = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        if (existing.getMediaTypeEnum() == MediaType.TV_SHOW) {
            val now = System.currentTimeMillis()
            val baseSeasons = fetchBaseSeasons(existing, apiKey).sortedBy { it.seasonNumber }
            val progressList = tvProgressDao.getProgressForMediaDirect(id)
            val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
            val watchedSet = resolveEffectiveWatchedEpisodesSet(existing, baseSeasons, progressList).toMutableSet()

            // Find the first unwatched (season, episode) in sequential order
            var nextTarget: Pair<Int, Int>? = null
            for (season in baseSeasons) {
                for (ep in 1..season.episodeCount) {
                    val key = season.seasonNumber to ep
                    if (key !in watchedSet) {
                        nextTarget = key
                        break
                    }
                }
                if (nextTarget != null) break
            }

            if (nextTarget != null) {
                watchedSet.add(nextTarget)
            }

            val entitiesToSave = buildSyncedTvProgressEntities(id, baseSeasons, watchedSet, progressMap, now)
            if (entitiesToSave.isNotEmpty()) {
                tvProgressDao.insertAll(entitiesToSave)
            }

            val catalogTotal = baseSeasons.sumOf { it.episodeCount }
            val effectiveTotal = if (catalogTotal > 0) catalogTotal else existing.totalEpisodes
            val nextEpisodeCount = if (catalogTotal > 0) {
                watchedSet.size
            } else {
                (existing.watchedEpisodes + 1).coerceAtMost(existing.totalEpisodes)
            }

            val newStatus = if (nextEpisodeCount >= effectiveTotal && effectiveTotal > 0) {
                WatchStatus.COMPLETED.name
            } else {
                WatchStatus.WATCHING.name
            }
            val updated = existing.copy(
                watchedEpisodes = nextEpisodeCount,
                totalEpisodes = effectiveTotal,
                totalSeasons = maxOf(existing.totalSeasons, baseSeasons.size),
                status = newStatus,
                lastWatchedDate = now
            )
            mediaDao.insertOrUpdate(updated)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, updated)
                if (entitiesToSave.isNotEmpty()) {
                    firestoreSyncManager?.uploadTvProgressList(uid, entitiesToSave)
                }
            }
        }
    }

    suspend fun updateWatchedEpisodes(id: String, episodes: Int, apiKey: String = "") = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(id) ?: return@withContext
        val now = System.currentTimeMillis()
        if (existing.getMediaTypeEnum() == MediaType.TV_SHOW) {
            val baseSeasons = fetchBaseSeasons(existing, apiKey).sortedBy { it.seasonNumber }
            val catalogTotal = baseSeasons.sumOf { it.episodeCount }
            val effectiveTotal = if (catalogTotal > 0) catalogTotal else existing.totalEpisodes
            val safeEpisodes = episodes.coerceIn(0, effectiveTotal)

            val progressList = tvProgressDao.getProgressForMediaDirect(id)
            val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
            val currentOrderedWatched = resolveEffectiveWatchedEpisodesSet(existing, baseSeasons, progressList).toMutableList()

            if (safeEpisodes < currentOrderedWatched.size) {
                // Undo / step backward from the latest watched episodes
                val toRemove = currentOrderedWatched.size - safeEpisodes
                repeat(toRemove) {
                    if (currentOrderedWatched.isNotEmpty()) {
                        currentOrderedWatched.removeAt(currentOrderedWatched.lastIndex)
                    }
                }
            } else if (safeEpisodes > currentOrderedWatched.size) {
                var toAdd = safeEpisodes - currentOrderedWatched.size
                val existingSet = currentOrderedWatched.toHashSet()
                for (season in baseSeasons) {
                    for (ep in 1..season.episodeCount) {
                        if (toAdd <= 0) break
                        val key = season.seasonNumber to ep
                        if (key !in existingSet) {
                            currentOrderedWatched.add(key)
                            existingSet.add(key)
                            toAdd--
                        }
                    }
                    if (toAdd <= 0) break
                }
            }

            val updatedWatchedSet = currentOrderedWatched.toSet()
            val entitiesToSave = buildSyncedTvProgressEntities(id, baseSeasons, updatedWatchedSet, progressMap, now)
            if (entitiesToSave.isNotEmpty()) {
                tvProgressDao.insertAll(entitiesToSave)
            }

            val newStatus = when {
                safeEpisodes >= effectiveTotal && effectiveTotal > 0 -> WatchStatus.COMPLETED.name
                safeEpisodes > 0 -> WatchStatus.WATCHING.name
                existing.status == WatchStatus.WATCHING.name || existing.status == WatchStatus.COMPLETED.name -> WatchStatus.NONE.name
                else -> existing.status
            }
            val updated = existing.copy(
                watchedEpisodes = safeEpisodes,
                totalEpisodes = effectiveTotal,
                totalSeasons = maxOf(existing.totalSeasons, baseSeasons.size),
                status = newStatus,
                lastWatchedDate = now
            )
            mediaDao.insertOrUpdate(updated)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, updated)
                if (entitiesToSave.isNotEmpty()) {
                    firestoreSyncManager?.uploadTvProgressList(uid, entitiesToSave)
                }
            }
        } else {
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
                lastWatchedDate = now
            )
            mediaDao.insertOrUpdate(updated)
            val uid = currentAuthenticatedUserId()
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager?.uploadTrackedMedia(uid, updated)
            }
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
        val existing = mediaDao.getMediaByIdDirect(item.id)
        if (existing == null) {
            mediaDao.insertAll(listOf(item))
        } else {
            val merged = OfflineCachePolicy.mergeCatalogMetadataPreservingUserTracking(existing, item)
            if (merged != existing) {
                mediaDao.update(merged)
            }
        }
    }

    suspend fun hydrateWatchlistItemForOffline(
        mediaId: String,
        apiKey: String,
        regionCode: String
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        if (!mediaId.startsWith("tmdb_m_") && !mediaId.startsWith("tmdb_tv_")) return@withContext
        runCatching {
            fetchDetailsFromTmdb(mediaId, apiKey)
            fetchLiveWatchProviders(mediaId, regionCode, apiKey)
            val refreshed = mediaDao.getMediaByIdDirect(mediaId)
            if (refreshed != null && refreshed.getMediaTypeEnum() == MediaType.TV_SHOW) {
                getSeasonsForTvShow(refreshed, apiKey)
                getEpisodesForSeason(refreshed, 1, apiKey)
            }
        }
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
            val enriched = OfflineCachePolicy.mergeCatalogMetadataPreservingUserTracking(existing, item)
            if (enriched != existing) {
                mediaDao.update(enriched)
            }
            val currentStatus = enriched.getWatchStatusEnum()
            val nextStatus = if (currentStatus == WatchStatus.NONE) {
                WatchStatus.PLAN_TO_WATCH
            } else {
                WatchStatus.NONE
            }
            updateStatus(enriched.id, nextStatus)
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
        tvEpisodeCacheDao?.deleteForMedia(id)
        val uid = currentAuthenticatedUserId()
        if (!uid.isNullOrBlank()) {
            firestoreSyncManager?.deleteTrackedMedia(uid, id)
        }
    }

    fun getTvProgress(mediaId: String): Flow<List<TvProgressEntity>> = tvProgressDao.getProgressForMedia(mediaId)

    private suspend fun fetchBaseSeasons(media: MediaEntity, apiKey: String): List<SeasonItem> {
        val hasLiveKey = media.id.startsWith("tmdb_tv_") && apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY"
        val liveKeyId = "${media.id}_true"
        val cacheKey = "${media.id}_$hasLiveKey"

        baseSeasonsCache[liveKeyId]?.let { return it }
        if (!hasLiveKey) {
            baseSeasonsCache[cacheKey]?.let { return it }
        }

        val fetchedFromTmdb = if (hasLiveKey) {
            try {
                val tmdbId = media.id.removePrefix("tmdb_tv_").toLongOrNull()
                if (tmdbId != null) {
                    val tvDetails = TmdbClient.api.getTvDetails(tmdbId, apiKey)
                    tvDetails.seasons?.filter { (it.seasonNumber ?: 0) > 0 }?.takeIf { it.isNotEmpty() }?.map { s ->
                        val sNum = s.seasonNumber ?: 1
                        val poster = s.posterPath?.let { "${TmdbClient.IMAGE_BASE_URL}$it" } ?: media.posterUrl
                        SeasonItem(
                            seriesId = media.id,
                            seriesTitle = media.title,
                            seasonNumber = sNum,
                            name = s.name ?: "Season $sNum",
                            overview = s.overview.orEmpty().ifBlank { media.overview },
                            posterUrl = poster,
                            backdropUrl = media.backdropUrl,
                            episodeCount = (s.episodeCount ?: 10).coerceAtLeast(1),
                            releaseYear = (s.airDate ?: media.releaseYear.toString()).take(4).toIntOrNull() ?: media.releaseYear,
                            airDate = s.airDate ?: "${media.releaseYear}-01-01",
                            rating = (s.voteAverage?.toFloat() ?: media.communityRating)
                        )
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        if (!fetchedFromTmdb.isNullOrEmpty()) {
            tvEpisodeCacheDao?.insertAll(fetchedFromTmdb.map { CachedTvEpisodeEntity.fromSeason(it) })
            baseSeasonsCache[cacheKey] = fetchedFromTmdb
            return fetchedFromTmdb
        }

        val roomCachedSeasons = runCatching {
            tvEpisodeCacheDao?.getCachedSeasons(media.id).orEmpty()
        }.getOrDefault(emptyList())
        if (roomCachedSeasons.isNotEmpty()) {
            val restored = roomCachedSeasons.map {
                it.toSeasonItem(seriesTitle = media.title, fallbackBackdropUrl = media.backdropUrl)
            }
            baseSeasonsCache[cacheKey] = restored
            return restored
        }

        val fallback = SampleTvEpisodeData.getCuratedSeasons(media)
        baseSeasonsCache[cacheKey] = fallback
        return fallback
    }

    private fun resolveEffectiveWatchedEpisodesSet(
        media: MediaEntity,
        baseSeasons: List<SeasonItem>,
        progressList: List<TvProgressEntity>
    ): LinkedHashSet<Pair<Int, Int>> {
        val orderedSeasons = baseSeasons.sortedBy { it.seasonNumber }
        val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
        val watchedSet = LinkedHashSet<Pair<Int, Int>>()

        for (season in orderedSeasons) {
            val seasonWholeWatched = progressMap["${season.seasonNumber}_0"] == "WATCHED"
            for (ep in 1..season.episodeCount) {
                val epStatus = progressMap["${season.seasonNumber}_$ep"]
                if (epStatus == "WATCHED" || (epStatus == null && seasonWholeWatched)) {
                    watchedSet.add(season.seasonNumber to ep)
                }
            }
        }

        if (media.watchedEpisodes > watchedSet.size) {
            var remaining = media.watchedEpisodes - watchedSet.size
            for (season in orderedSeasons) {
                for (ep in 1..season.episodeCount) {
                    if (remaining <= 0) break
                    val key = season.seasonNumber to ep
                    if (key !in watchedSet && progressMap["${season.seasonNumber}_$ep"] != "UNWATCHED") {
                        watchedSet.add(key)
                        remaining--
                    }
                }
                if (remaining <= 0) break
            }
            if (remaining > 0) {
                for (season in orderedSeasons) {
                    for (ep in 1..season.episodeCount) {
                        if (remaining <= 0) break
                        val key = season.seasonNumber to ep
                        if (key !in watchedSet) {
                            watchedSet.add(key)
                            remaining--
                        }
                    }
                    if (remaining <= 0) break
                }
            }
        }

        val sortedWatched = LinkedHashSet<Pair<Int, Int>>()
        for (season in orderedSeasons) {
            for (ep in 1..season.episodeCount) {
                val key = season.seasonNumber to ep
                if (key in watchedSet) {
                    sortedWatched.add(key)
                }
            }
        }
        return sortedWatched
    }

    private fun buildSyncedTvProgressEntities(
        mediaId: String,
        baseSeasons: List<SeasonItem>,
        watchedSet: Set<Pair<Int, Int>>,
        progressMap: Map<String, String>,
        now: Long
    ): List<TvProgressEntity> {
        val entities = mutableListOf<TvProgressEntity>()
        for (season in baseSeasons) {
            val sNum = season.seasonNumber
            var seasonWatchedCount = 0
            for (ep in 1..season.episodeCount) {
                val isWatched = (sNum to ep) in watchedSet
                if (isWatched) seasonWatchedCount++
                val existingStatus = progressMap["${sNum}_$ep"]
                if (isWatched && existingStatus != "WATCHED") {
                    entities.add(TvProgressEntity(mediaId, sNum, ep, "WATCHED", now))
                } else if (!isWatched && (existingStatus == "WATCHED" || progressMap["${sNum}_0"] == "WATCHED")) {
                    entities.add(TvProgressEntity(mediaId, sNum, ep, "UNWATCHED", now))
                }
            }

            val existingSeasonStatus = progressMap["${sNum}_0"]
            val desiredSeasonStatus = when {
                seasonWatchedCount >= season.episodeCount && season.episodeCount > 0 -> "WATCHED"
                seasonWatchedCount > 0 -> "WATCHING"
                existingSeasonStatus == "WATCHED" || existingSeasonStatus == "WATCHING" -> "UNWATCHED"
                else -> null
            }
            if (desiredSeasonStatus != null && existingSeasonStatus != desiredSeasonStatus) {
                entities.add(TvProgressEntity(mediaId, sNum, 0, desiredSeasonStatus, now))
            }
        }
        return entities
    }

    suspend fun getSeasonsForTvShow(media: MediaEntity, apiKey: String): List<SeasonItem> = withContext(Dispatchers.IO) {
        val latestMedia = mediaDao.getMediaByIdDirect(media.id) ?: media
        val baseSeasons = fetchBaseSeasons(latestMedia, apiKey)

        // Merge saved progress from Room + reconciled watchedEpisodes
        val progressList = tvProgressDao.getProgressForMediaDirect(latestMedia.id)
        val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
        val watchedSet = resolveEffectiveWatchedEpisodesSet(latestMedia, baseSeasons, progressList)

        baseSeasons.map { season ->
            val seasonStatusStr = progressMap["${season.seasonNumber}_0"]
            val seasonStatus = when (seasonStatusStr) {
                "WATCHED" -> TvWatchStatus.WATCHED
                "WATCHING" -> TvWatchStatus.WATCHING
                "PLAN_TO_WATCH" -> TvWatchStatus.PLAN_TO_WATCH
                else -> TvWatchStatus.UNWATCHED
            }

            val watchedEpsCount = (1..season.episodeCount).count { epNum ->
                (season.seasonNumber to epNum) in watchedSet
            }

            val effectiveStatus = when {
                watchedEpsCount >= season.episodeCount && season.episodeCount > 0 -> TvWatchStatus.WATCHED
                watchedEpsCount > 0 && seasonStatus == TvWatchStatus.UNWATCHED -> TvWatchStatus.WATCHING
                watchedEpsCount == 0 && seasonStatus == TvWatchStatus.WATCHED -> TvWatchStatus.UNWATCHED
                else -> seasonStatus
            }

            season.copy(
                watchStatus = effectiveStatus,
                watchedEpisodesCount = watchedEpsCount
            )
        }
    }

    suspend fun getEpisodesForSeason(media: MediaEntity, seasonNumber: Int, apiKey: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val latestMedia = mediaDao.getMediaByIdDirect(media.id) ?: media
        val fetchedFromTmdb = if (latestMedia.id.startsWith("tmdb_tv_") && apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val tmdbId = latestMedia.id.removePrefix("tmdb_tv_").toLongOrNull()
                if (tmdbId != null) {
                    val seasonDetail = TmdbClient.api.getSeasonDetails(tmdbId, seasonNumber, apiKey)
                    if (seasonDetail.episodes.isNotEmpty()) {
                        seasonDetail.episodes.map { ep ->
                            val still = ep.stillPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: latestMedia.backdropUrl
                            EpisodeItem(
                                id = "${latestMedia.id}_s${seasonNumber}_e${ep.episodeNumber}",
                                seriesId = latestMedia.id,
                                seriesTitle = latestMedia.title,
                                seasonNumber = seasonNumber,
                                episodeNumber = ep.episodeNumber,
                                title = ep.name ?: "Episode ${ep.episodeNumber}",
                                overview = ep.overview.orEmpty(),
                                stillUrl = still,
                                airDate = ep.airDate ?: "",
                                runtimeMinutes = ep.runtime ?: 45,
                                rating = ep.voteAverage?.toFloat() ?: latestMedia.communityRating,
                                director = latestMedia.directorOrCreator,
                                writer = latestMedia.directorOrCreator
                            )
                        }
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val baseEpisodes = if (!fetchedFromTmdb.isNullOrEmpty()) {
            tvEpisodeCacheDao?.insertAll(fetchedFromTmdb.map { CachedTvEpisodeEntity.fromEpisode(it) })
            fetchedFromTmdb
        } else {
            val cachedRoomEpisodes = runCatching {
                tvEpisodeCacheDao?.getCachedEpisodes(latestMedia.id, seasonNumber).orEmpty()
            }.getOrDefault(emptyList())
            if (cachedRoomEpisodes.isNotEmpty()) {
                cachedRoomEpisodes.map {
                    it.toEpisodeItem(
                        seriesTitle = latestMedia.title,
                        director = latestMedia.directorOrCreator,
                        writer = latestMedia.directorOrCreator
                    )
                }
            } else {
                SampleTvEpisodeData.getCuratedEpisodes(latestMedia, seasonNumber)
            }
        }

        // Merge saved progress + reconciled watchedEpisodes
        val progressList = tvProgressDao.getProgressForMediaDirect(latestMedia.id)
        val progressMap = progressList.associate { "${it.seasonNumber}_${it.episodeNumber}" to it.status }
        val baseSeasons = fetchBaseSeasons(latestMedia, apiKey)
        val watchedSet = resolveEffectiveWatchedEpisodesSet(latestMedia, baseSeasons, progressList)

        baseEpisodes.map { episode ->
            val epStatusStr = progressMap["${seasonNumber}_${episode.episodeNumber}"]
            val status = when {
                (seasonNumber to episode.episodeNumber) in watchedSet -> TvWatchStatus.WATCHED
                epStatusStr == "WATCHING" -> TvWatchStatus.WATCHING
                epStatusStr == "PLAN_TO_WATCH" -> TvWatchStatus.PLAN_TO_WATCH
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

    suspend fun hasAnyUserTrackedData(): Boolean = withContext(Dispatchers.IO) {
        val tracked = runCatching { mediaDao.getAllMediaList().any { isMediaTracked(it) } }.getOrDefault(false)
        if (tracked) return@withContext true
        val favPeople = runCatching { favoritePersonDao.getAllFavoritePeopleList().isNotEmpty() }.getOrDefault(false)
        favPeople
    }

    suspend fun seedPopularPeopleIfEmpty() = withContext(Dispatchers.IO) {
        val dao = popularPersonDao ?: return@withContext
        if (dao.countPopularPeople() > 0) return@withContext
        val samplePeople = SampleMediaData.getSamplePeople()
        val entitiesToCache = samplePeople.flatMap { it.knownFor }.distinctBy { it.id }
        if (entitiesToCache.isNotEmpty()) {
            mediaDao.insertAll(entitiesToCache)
        }
        dao.insertAll(samplePeople.map { PopularPersonEntity.fromPerson(it, pageLoaded = 1) })
    }

    suspend fun cachePopularPeopleInDb(
        people: List<TmdbSearchResultItem.Person>,
        pageLoaded: Int
    ) = withContext(Dispatchers.IO) {
        val entitiesToCache = people.flatMap { it.knownFor }.distinctBy { it.id }
        if (entitiesToCache.isNotEmpty()) {
            mediaDao.insertAll(entitiesToCache)
        }
        popularPersonDao?.insertAll(
            people.map { PopularPersonEntity.fromPerson(it, pageLoaded = pageLoaded) }
        )
    }

    suspend fun getPagedPopularPeopleFromDb(
        mediaType: MediaType?,
        limit: Int,
        offset: Int
    ): List<PopularPersonEntity> = withContext(Dispatchers.IO) {
        popularPersonDao?.getPagedPopularPeople(
            mediaType = mediaType?.name,
            limit = limit,
            offset = offset
        ) ?: emptyList()
    }

    suspend fun fetchPopularPeoplePage(
        apiKey: String,
        language: String?,
        page: Int
    ): List<TmdbSearchResultItem.Person> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") {
            if (page == 1) {
                seedPopularPeopleIfEmpty()
                return@withContext SampleMediaData.getSamplePeople()
            }
            return@withContext emptyList()
        }
        try {
            val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
            val response = TmdbClient.api.getPopularPeople(apiKey = apiKey, language = safeLanguage, page = page)
            val entitiesToCache = mutableListOf<MediaEntity>()
            val livePeople = response.results.mapNotNull { dto ->
                val personName = dto.name?.trim().orEmpty()
                if (personName.isBlank()) return@mapNotNull null
                val knownForEntities = dto.knownFor?.map { TmdbClient.knownForToEntity(it) }.orEmpty()
                entitiesToCache.addAll(knownForEntities)
                TmdbSearchResultItem.Person(
                    id = dto.id,
                    name = personName,
                    profileUrl = dto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    department = dto.knownForDepartment ?: "Acting",
                    knownFor = knownForEntities,
                    popularity = dto.popularity ?: 0.0
                )
            }
            if (entitiesToCache.isNotEmpty()) {
                mediaDao.insertAll(entitiesToCache.distinctBy { it.id })
            }
            if (livePeople.isNotEmpty()) {
                popularPersonDao?.insertAll(
                    livePeople.map { PopularPersonEntity.fromPerson(it, pageLoaded = page) }
                )
                return@withContext livePeople
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (page == 1) {
            seedPopularPeopleIfEmpty()
            return@withContext SampleMediaData.getSamplePeople()
        }
        emptyList()
    }

    private suspend fun fetchRegionalStarsForOnboarding(
        apiKey: String,
        safeLanguage: String?,
        safeRegion: String
    ): List<TmdbSearchResultItem.Person> = coroutineScope {
        val (regionalMovies, regionalTv) = run {
            val mDef = async {
                withTimeoutOrNull(5000L) {
                    runCatching {
                        TmdbClient.api.discoverMovies(
                            apiKey = apiKey,
                            language = safeLanguage,
                            region = safeRegion,
                            watchRegion = safeRegion,
                            withOriginCountry = safeRegion,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results.map { TmdbClient.toMediaEntity(it.copy(mediaType = "movie")).copy(originCountry = safeRegion) }
                    }.getOrDefault(emptyList())
                } ?: emptyList()
            }
            val tDef = async {
                withTimeoutOrNull(5000L) {
                    runCatching {
                        TmdbClient.api.discoverTv(
                            apiKey = apiKey,
                            language = safeLanguage,
                            region = safeRegion,
                            withOriginCountry = safeRegion,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results.map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")).copy(originCountry = safeRegion) }
                    }.getOrDefault(emptyList())
                } ?: emptyList()
            }
            mDef.await() to tDef.await()
        }

        val topMovies = regionalMovies.filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }.take(4)
        val topTv = regionalTv.filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }.take(3)
        if (topMovies.isEmpty() && topTv.isEmpty()) return@coroutineScope emptyList()

        val allSourceMedia = (topMovies + topTv).distinctBy { it.id }
        if (allSourceMedia.isNotEmpty()) {
            runCatching { mediaDao.insertAll(allSourceMedia) }
        }

        data class AccumulatedStar(
            val id: Long,
            val name: String,
            val profileUrl: String,
            val department: String,
            val popularity: Double,
            var score: Double = 0.0,
            val knownFor: MutableList<MediaEntity> = mutableListOf()
        )

        val movieCastCalls = topMovies.map { media ->
            async {
                val numericId = media.id.removePrefix("tmdb_m_").toLongOrNull() ?: return@async null
                val detail = withTimeoutOrNull(4500L) {
                    runCatching { TmdbClient.api.getMovieDetails(numericId, apiKey) }.getOrNull()
                } ?: return@async null
                media to (detail.credits?.cast.orEmpty().take(5))
            }
        }
        val tvCastCalls = topTv.map { media ->
            async {
                val numericId = media.id.removePrefix("tmdb_tv_").toLongOrNull() ?: return@async null
                val detail = withTimeoutOrNull(4500L) {
                    runCatching { TmdbClient.api.getTvDetails(numericId, apiKey) }.getOrNull()
                } ?: return@async null
                media to (detail.credits?.cast.orEmpty().take(5))
            }
        }

        val starMap = LinkedHashMap<Long, AccumulatedStar>()
        val castResults = (movieCastCalls.awaitAll() + tvCastCalls.awaitAll()).filterNotNull()
        for ((sourceMedia, castList) in castResults) {
            castList.forEachIndexed { index, castDto ->
                val personId = castDto.id ?: return@forEachIndexed
                if (personId <= 0L) return@forEachIndexed
                val profilePath = castDto.profilePath?.takeIf { it.isNotBlank() } ?: return@forEachIndexed
                val cleanName = castDto.name.trim()
                if (cleanName.isBlank()) return@forEachIndexed
                val billingWeight = (5 - index).coerceAtLeast(1) * 15.0
                val pop = castDto.popularity ?: 0.0

                val entry = starMap.getOrPut(personId) {
                    AccumulatedStar(
                        id = personId,
                        name = cleanName,
                        profileUrl = "${TmdbClient.IMAGE_BASE_URL}$profilePath",
                        department = castDto.knownForDepartment?.takeIf { it.isNotBlank() } ?: "Acting",
                        popularity = pop
                    )
                }
                entry.score += billingWeight + pop
                if (entry.knownFor.none { it.id == sourceMedia.id }) {
                    entry.knownFor.add(sourceMedia)
                }
            }
        }

        starMap.values
            .sortedByDescending { it.score }
            .take(18)
            .map { acc ->
                TmdbSearchResultItem.Person(
                    id = acc.id,
                    name = acc.name,
                    profileUrl = acc.profileUrl,
                    department = acc.department,
                    knownFor = acc.knownFor.toList(),
                    popularity = acc.popularity
                )
            }
    }

    private suspend fun getCachedPeopleFallback(): List<TmdbSearchResultItem.Person> {
        val allMediaMap = runCatching { mediaDao.getAllMediaList().associateBy { it.id } }.getOrDefault(emptyMap())
        val cachedEntities = runCatching { cachedPersonDao?.searchByQuery("") ?: emptyList() }.getOrDefault(emptyList())
        val popularEntities = runCatching { popularPersonDao?.getPagedPopularPeople(null, limit = 30, offset = 0) ?: emptyList() }.getOrDefault(emptyList())
        val fromCached = cachedEntities.map { it.toPersonItem(allMediaMap) }
        val fromPopular = popularEntities.map { it.toSearchResultPerson(allMediaMap) }
        return (fromCached + fromPopular).distinctBy { it.id }
    }

    suspend fun fetchPopularPeople(
        apiKey: String,
        language: String?,
        region: String? = null
    ): List<TmdbSearchResultItem.Person> = withContext(Dispatchers.IO) {
        val samplePeople = SampleMediaData.getSamplePeople()
        val safeRegion = OnboardingRegionalPolicy.normalizeRegionCode(region)
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") {
            seedPopularPeopleIfEmpty()
            return@withContext OnboardingRegionalPolicy.rankFallbackPeopleForRegion(
                samplePeople = samplePeople,
                cachedPeople = getCachedPeopleFallback(),
                regionCode = safeRegion,
                limit = 24
            )
        }
        try {
            val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
            val (regionalStars, livePeople) = coroutineScope {
                val regionalDeferred = async {
                    if (safeRegion != null) {
                        runCatching { fetchRegionalStarsForOnboarding(apiKey, safeLanguage, safeRegion) }
                            .getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                val globalDeferred = async {
                    runCatching {
                        val response = TmdbClient.api.getPopularPeople(apiKey = apiKey, language = safeLanguage, page = 1)
                        val entitiesToCache = mutableListOf<MediaEntity>()
                        val parsed = response.results.mapNotNull { dto ->
                            val personName = dto.name?.trim().orEmpty()
                            if (personName.isBlank()) return@mapNotNull null
                            val knownForEntities = dto.knownFor?.map { TmdbClient.knownForToEntity(it) }.orEmpty()
                            entitiesToCache.addAll(knownForEntities)
                            TmdbSearchResultItem.Person(
                                id = dto.id,
                                name = personName,
                                profileUrl = dto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" },
                                department = dto.knownForDepartment ?: "Acting",
                                knownFor = knownForEntities,
                                popularity = dto.popularity ?: 0.0
                            )
                        }
                        if (entitiesToCache.isNotEmpty()) {
                            mediaDao.insertAll(entitiesToCache.distinctBy { it.id })
                        }
                        parsed
                    }.getOrDefault(emptyList())
                }
                regionalDeferred.await() to globalDeferred.await()
            }

            if (livePeople.isNotEmpty()) {
                popularPersonDao?.insertAll(
                    livePeople.map { PopularPersonEntity.fromPerson(it, pageLoaded = 1) }
                )
            }
            val merged = OnboardingRegionalPolicy.prioritizeOnboardingPeopleForRegion(
                regionalStars = regionalStars,
                globalPopularPeople = livePeople,
                samplePeople = samplePeople,
                regionCode = safeRegion,
                limit = 24
            )
            if (merged.isNotEmpty()) {
                cachedPersonDao?.insertAll(merged.map { CachedPersonEntity.fromPerson(it) })
                return@withContext merged
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        seedPopularPeopleIfEmpty()
        OnboardingRegionalPolicy.rankFallbackPeopleForRegion(
            samplePeople = samplePeople,
            cachedPeople = getCachedPeopleFallback(),
            regionCode = safeRegion,
            limit = 24
        )
    }

    suspend fun searchPeopleTmdb(
        query: String,
        apiKey: String,
        language: String?,
        region: String? = null
    ): List<TmdbSearchResultItem.Person> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext fetchPopularPeople(apiKey, language, region)
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = TmdbClient.api.searchPerson(
                    apiKey = apiKey,
                    query = trimmed,
                    language = safeLanguage
                )
                val entitiesToCache = mutableListOf<MediaEntity>()
                val liveResults = response.results.mapNotNull { dto ->
                    val personName = dto.name?.trim().orEmpty()
                    if (personName.isBlank()) return@mapNotNull null
                    val knownForEntities = dto.knownFor?.map { TmdbClient.knownForToEntity(it) }.orEmpty()
                    entitiesToCache.addAll(knownForEntities)
                    TmdbSearchResultItem.Person(
                        id = dto.id,
                        name = personName,
                        profileUrl = dto.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" },
                        department = dto.knownForDepartment ?: "Acting",
                        knownFor = knownForEntities,
                        popularity = dto.popularity ?: 0.0
                    )
                }
                if (entitiesToCache.isNotEmpty()) {
                    mediaDao.insertAll(entitiesToCache)
                }
                if (liveResults.isNotEmpty()) {
                    cachedPersonDao?.insertAll(liveResults.take(24).map { CachedPersonEntity.fromPerson(it) })
                    return@withContext liveResults.take(24)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val offlinePool = (getCachedPeopleFallback() + SampleMediaData.getSamplePeople())
            .distinctBy { it.id.takeIf { id -> id > 0L } ?: it.name.lowercase() }
        return@withContext OfflineCachePolicy.rankOfflinePeopleMatches(offlinePool, trimmed, limit = 24)
    }

    suspend fun fetchOnboardingPopularTitles(
        apiKey: String,
        language: String?,
        region: String?
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        val safeRegion = OnboardingRegionalPolicy.normalizeRegionCode(region)
        val fallbackPool = (
            runCatching { mediaDao.getAllMediaList() }.getOrDefault(emptyList()) +
                SampleMediaData.getInitialCatalog() +
                getCuratedTopRatedMovies() +
                getCuratedTopRatedTv()
            ).distinctBy { it.id }

        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") {
            return@withContext OnboardingRegionalPolicy.rankFallbackTitlesForRegion(
                pool = fallbackPool,
                regionCode = safeRegion,
                limit = 30
            )
        }

        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null

        try {
            val results = coroutineScope {
                val regionalMoviesDef = async {
                    if (safeRegion == null) return@async emptyList()
                    runCatching {
                        TmdbClient.api.discoverMovies(
                            apiKey = apiKey,
                            language = safeLanguage,
                            region = safeRegion,
                            watchRegion = safeRegion,
                            withOriginCountry = safeRegion,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results.map {
                            TmdbClient.toMediaEntity(it.copy(mediaType = "movie")).copy(originCountry = safeRegion)
                        }
                    }.getOrDefault(emptyList())
                }
                val regionalTvDef = async {
                    if (safeRegion == null) return@async emptyList()
                    runCatching {
                        TmdbClient.api.discoverTv(
                            apiKey = apiKey,
                            language = safeLanguage,
                            region = safeRegion,
                            withOriginCountry = safeRegion,
                            sortBy = "popularity.desc",
                            page = 1
                        ).results.map {
                            TmdbClient.toMediaEntity(it.copy(mediaType = "tv")).copy(originCountry = safeRegion)
                        }
                    }.getOrDefault(emptyList())
                }
                val mDef = async {
                    runCatching {
                        TmdbClient.api.getPopularMovies(apiKey, region = safeRegion, language = safeLanguage)
                            .results.map { TmdbClient.toMediaEntity(it.copy(mediaType = "movie")) }
                    }.getOrDefault(emptyList())
                }
                val tDef = async {
                    runCatching {
                        val regionStreamingTv = if (safeRegion != null) {
                            runCatching {
                                TmdbClient.api.discoverTv(
                                    apiKey = apiKey,
                                    language = safeLanguage,
                                    region = safeRegion,
                                    sortBy = "popularity.desc",
                                    page = 1
                                ).results.map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")) }
                            }.getOrDefault(emptyList())
                        } else emptyList()
                        val globalPopularTv = TmdbClient.api.getPopularTv(apiKey, language = safeLanguage)
                            .results.map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")) }
                        (regionStreamingTv + globalPopularTv).distinctBy { it.id }
                    }.getOrDefault(emptyList())
                }
                val tmDef = async {
                    runCatching {
                        TmdbClient.api.getTopRatedMovies(apiKey, language = safeLanguage, region = safeRegion)
                            .results.take(10).map { TmdbClient.toMediaEntity(it.copy(mediaType = "movie")) }
                    }.getOrDefault(emptyList())
                }
                val ttDef = async {
                    runCatching {
                        TmdbClient.api.getTopRatedTv(apiKey, language = safeLanguage)
                            .results.take(10).map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")) }
                    }.getOrDefault(emptyList())
                }
                listOf(
                    regionalMoviesDef.await(),
                    regionalTvDef.await(),
                    mDef.await(),
                    tDef.await(),
                    tmDef.await(),
                    ttDef.await()
                )
            }

            val combined = OnboardingRegionalPolicy.prioritizeOnboardingTitlesForRegion(
                regionalOriginMovies = results[0],
                regionalOriginTv = results[1],
                popularInRegionMovies = results[2],
                popularInRegionTv = results[3],
                topRatedMovies = results[4],
                topRatedTv = results[5],
                regionCode = safeRegion,
                limit = 36
            )

            if (combined.isNotEmpty()) {
                mediaDao.insertAll(combined)
                return@withContext combined
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        OnboardingRegionalPolicy.rankFallbackTitlesForRegion(
            pool = fallbackPool,
            regionCode = safeRegion,
            limit = 30
        )
    }

    suspend fun computeInstantRecommendations(
        favoritePeople: List<FavoritePersonEntity>,
        trackedMedia: List<MediaEntity>,
        region: String?,
        preferredGenres: Set<String> = emptySet()
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        if (favoritePeople.isEmpty() && trackedMedia.isEmpty() && preferredGenres.isEmpty()) {
            return@withContext emptyList()
        }
        val resolvedRegion = region?.takeIf { !it.equals("AUTO", ignoreCase = true) && !it.equals("GLOBAL", ignoreCase = true) }?.uppercase() ?: "US"
        val candidatePool = mutableListOf<MediaEntity>()
        runCatching { candidatePool.addAll(mediaDao.getAllMediaList()) }
        candidatePool.addAll(SampleMediaData.getInitialCatalog())
        candidatePool.addAll(getCuratedTopRatedMovies())
        candidatePool.addAll(getCuratedTopRatedTv())

        PersonalizationEngine.rankAndDeduplicateRecommendations(
            candidates = candidatePool,
            trackedMedia = trackedMedia,
            favoritePeople = favoritePeople,
            regionCode = resolvedRegion,
            preferredGenres = preferredGenres,
            maxItems = 18
        )
    }

    suspend fun fetchPersonalizedRecommendations(
        favoritePeople: List<FavoritePersonEntity>,
        trackedMedia: List<MediaEntity>,
        apiKey: String,
        region: String?,
        language: String?,
        preferredGenres: Set<String> = emptySet()
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        if (favoritePeople.isEmpty() && trackedMedia.isEmpty() && preferredGenres.isEmpty()) {
            return@withContext emptyList()
        }

        val resolvedRegion = region?.takeIf { !it.equals("AUTO", ignoreCase = true) && !it.equals("GLOBAL", ignoreCase = true) }?.uppercase() ?: "US"
        val safeLanguage = if (language != null && !language.equals("all", ignoreCase = true) && language.isNotBlank()) language else null
        val safeRegion = if (!region.isNullOrBlank() && !region.equals("AUTO", ignoreCase = true) && !region.equals("GLOBAL", ignoreCase = true)) region.uppercase() else null

        val candidatePool = mutableListOf<MediaEntity>()

        // Always include local + curated catalog as baseline candidate pool
        runCatching {
            candidatePool.addAll(mediaDao.getAllMediaList())
        }
        candidatePool.addAll(SampleMediaData.getInitialCatalog())
        candidatePool.addAll(getCuratedTopRatedMovies())
        candidatePool.addAll(getCuratedTopRatedTv())

        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val topGenres = PersonalizationEngine.extractTopGenres(
                    trackedMedia = trackedMedia,
                    preferredGenres = preferredGenres,
                    limit = 3
                )
                val seedTitles = trackedMedia
                    .filter { it.id.startsWith("tmdb_m_") || it.id.startsWith("tmdb_tv_") }
                    .sortedWith(
                        compareByDescending<MediaEntity> { it.isFavorite }
                            .thenByDescending { it.getWatchStatusEnum() == WatchStatus.COMPLETED }
                            .thenByDescending { it.userRating }
                            .thenByDescending { it.lastWatchedDate ?: 0L }
                    )
                    .take(4)
                val seedPeople = favoritePeople.filter { it.personId > 0L }.take(3)

                val fetchedFromTmdb = coroutineScope {
                    val recJobs = seedTitles.map { media ->
                        async {
                            runCatching {
                                if (media.id.startsWith("tmdb_m_")) {
                                    val movieId = media.id.removePrefix("tmdb_m_").toLongOrNull() ?: return@runCatching emptyList()
                                    TmdbClient.api.getMovieRecommendations(movieId, apiKey, safeLanguage)
                                        .results.take(8).map { TmdbClient.toMediaEntity(it.copy(mediaType = "movie")) }
                                } else {
                                    val tvId = media.id.removePrefix("tmdb_tv_").toLongOrNull() ?: return@runCatching emptyList()
                                    TmdbClient.api.getTvRecommendations(tvId, apiKey, safeLanguage)
                                        .results.take(8).map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")) }
                                }
                            }.getOrDefault(emptyList())
                        }
                    }

                    val personJobs = seedPeople.map { person ->
                        async {
                            runCatching {
                                val detail = TmdbClient.api.getPersonDetails(person.personId, apiKey)
                                val combinedCredits = (detail.combinedCredits?.cast.orEmpty() + detail.combinedCredits?.crew.orEmpty())
                                    .filter { !it.posterPath.isNullOrBlank() }
                                    .sortedByDescending { it.voteAverage ?: 0.0 }
                                    .take(8)
                                val creditEntities = combinedCredits.map { dto ->
                                    TmdbClient.knownForToEntity(dto).let { entity ->
                                        if (entity.cast.isBlank()) entity.copy(cast = person.name) else entity
                                    }
                                }
                                val discoveredByPerson = runCatching {
                                    TmdbClient.api.discoverMovies(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = safeRegion,
                                        withPeople = person.personId.toString(),
                                        sortBy = "popularity.desc"
                                    ).results.take(6).map {
                                        TmdbClient.toMediaEntity(it.copy(mediaType = "movie")).let { entity ->
                                            if (entity.cast.isBlank()) entity.copy(cast = person.name) else entity
                                        }
                                    }
                                }.getOrDefault(emptyList())
                                (creditEntities + discoveredByPerson).distinctBy { it.id }
                            }.getOrDefault(emptyList())
                        }
                    }

                    val genreJobs = topGenres.map { genreName ->
                        async {
                            val (movieGenreId, tvGenreId) = PersonalizationEngine.mapGenreNameToTmdbIds(genreName)
                            val movieMatches = if (movieGenreId != null) {
                                runCatching {
                                    TmdbClient.api.discoverMovies(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = safeRegion,
                                        withGenres = movieGenreId,
                                        sortBy = "popularity.desc"
                                    ).results.take(8).map { TmdbClient.toMediaEntity(it.copy(mediaType = "movie")) }
                                }.getOrDefault(emptyList())
                            } else emptyList()

                            val tvMatches = if (tvGenreId != null) {
                                runCatching {
                                    TmdbClient.api.discoverTv(
                                        apiKey = apiKey,
                                        language = safeLanguage,
                                        region = safeRegion,
                                        withGenres = tvGenreId,
                                        sortBy = "popularity.desc"
                                    ).results.take(6).map { TmdbClient.toMediaEntity(it.copy(mediaType = "tv")) }
                                }.getOrDefault(emptyList())
                            } else emptyList()

                            movieMatches + tvMatches
                        }
                    }

                    (recJobs + personJobs + genreJobs).awaitAll().flatten()
                }

                if (fetchedFromTmdb.isNotEmpty()) {
                    mediaDao.insertAll(fetchedFromTmdb.distinctBy { it.id })
                    candidatePool.addAll(0, fetchedFromTmdb)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        PersonalizationEngine.rankAndDeduplicateRecommendations(
            candidates = candidatePool,
            trackedMedia = trackedMedia,
            favoritePeople = favoritePeople,
            regionCode = resolvedRegion,
            preferredGenres = preferredGenres,
            maxItems = 18
        )
    }
}
