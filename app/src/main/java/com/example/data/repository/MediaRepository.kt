package com.example.data.repository

import com.example.data.local.MediaDao
import com.example.data.local.MediaEntity
import com.example.data.local.PreferencesDao
import com.example.data.local.SampleMediaData
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.MediaType
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchProviderItem
import com.example.data.model.WatchProviderType
import com.example.data.model.WatchProvidersInfo
import com.example.data.model.WatchStatus
import com.example.data.remote.tmdb.TmdbClient
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.util.JustWatchScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MediaRepository(
    private val mediaDao: MediaDao,
    private val preferencesDao: PreferencesDao,
    private val searchHistoryDao: SearchHistoryDao
) {

    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()
    val trackedMedia: Flow<List<MediaEntity>> = mediaDao.getTrackedMedia()
    val continueWatching: Flow<List<MediaEntity>> = mediaDao.getContinueWatching()
    val favorites: Flow<List<MediaEntity>> = mediaDao.getFavorites()
    val watchlistMedia: Flow<List<MediaEntity>> = mediaDao.getWatchlistMedia()
    val userPreferences: Flow<UserPreferencesEntity?> = preferencesDao.getPreferences()
    val searchHistory: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

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
        }
    }

    suspend fun removeSearchQuery(query: String) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteQuery(query.trim())
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAll()
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (mediaDao.countTotal() == 0) {
            mediaDao.insertAll(SampleMediaData.getInitialCatalog())
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
    }

    suspend fun updatePreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        preferencesDao.savePreferences(prefs)
    }

    suspend fun fetchTrendingFromTmdb(apiKey: String, language: String?, region: String?) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        try {
            val response = TmdbClient.api.getTrendingAllDay(
                apiKey = apiKey,
                language = if (language != null && !language.equals("all", ignoreCase = true)) language else null
            )
            val entities = response.results.map { TmdbClient.toMediaEntity(it) }
            mediaDao.insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTrendingNow(
        apiKey: String,
        language: String?,
        timeWindow: String = "day"
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val response = if (timeWindow.equals("week", ignoreCase = true)) {
                    TmdbClient.api.getTrendingAllWeek(
                        apiKey = apiKey,
                        language = if (language != null && !language.equals("all", ignoreCase = true)) language else null
                    )
                } else {
                    TmdbClient.api.getTrendingAllDay(
                        apiKey = apiKey,
                        language = if (language != null && !language.equals("all", ignoreCase = true)) language else null
                    )
                }
                val entities = response.results
                    .filter { it.mediaType != "person" }
                    .map { TmdbClient.toMediaEntity(it) }
                if (entities.isNotEmpty()) {
                    mediaDao.insertAll(entities)
                    return@withContext entities
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback to initial high-rated catalog items
        return@withContext SampleMediaData.getInitialCatalog().sortedByDescending { it.communityRating }
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

    suspend fun fetchPersonDetails(personId: Long, apiKey: String): TmdbPersonDetailDto? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext null
        try {
            TmdbClient.api.getPersonDetails(personId, apiKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchDetailsFromTmdb(id: String, apiKey: String) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY") return@withContext
        try {
            val existing = mediaDao.getMediaByIdDirect(id)
            if (id.startsWith("tmdb_m_")) {
                val tmdbId = id.removePrefix("tmdb_m_").toLongOrNull() ?: return@withContext
                val detail = TmdbClient.api.getMovieDetails(tmdbId, apiKey)
                val updated = TmdbClient.movieDetailToEntity(detail, existing)
                mediaDao.insertOrUpdate(updated)
            } else if (id.startsWith("tmdb_tv_")) {
                val tmdbId = id.removePrefix("tmdb_tv_").toLongOrNull() ?: return@withContext
                val detail = TmdbClient.api.getTvDetails(tmdbId, apiKey)
                val updated = TmdbClient.tvDetailToEntity(detail, existing)
                mediaDao.insertOrUpdate(updated)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    }

    suspend fun updateRatingAndReview(id: String, rating: Float, review: String) = withContext(Dispatchers.IO) {
        mediaDao.updateRatingAndReview(id, rating, review)
    }

    suspend fun toggleFavorite(id: String, current: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateFavorite(id, !current)
    }

    suspend fun updateCustomWatchlist(id: String, watchlist: String?) = withContext(Dispatchers.IO) {
        mediaDao.updateCustomWatchlist(id, watchlist)
    }

    suspend fun saveMedia(item: MediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertOrUpdate(item)
    }

    suspend fun deleteMedia(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteById(id)
    }
}
