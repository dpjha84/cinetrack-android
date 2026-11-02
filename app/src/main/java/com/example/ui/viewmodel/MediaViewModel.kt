package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.GenreStat
import com.example.data.model.MediaType
import com.example.data.model.MultiSearchCategory
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.ViewingStats
import com.example.data.model.WatchProvidersInfo
import com.example.data.model.WatchStatus
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class LibraryTab(val label: String) {
    WATCHING("Watching"),
    PLAN_TO_WATCH("Plan to Watch"),
    COMPLETED("Completed"),
    FAVORITES("Favorites"),
    ALL_TRACKED("All Logged")
}

enum class SortOption(val label: String) {
    RECENT("Recently Active"),
    TITLE("Title (A-Z)"),
    RATING("Highest Rated"),
    YEAR("Release Year")
}

enum class TrendingFilterCategory(val label: String) {
    ALL("All Trending"),
    MOVIES("Movies"),
    TV_SHOWS("TV Shows")
}

@OptIn(FlowPreview::class)
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository = run {
        val db = AppDatabase.getDatabase(application)
        MediaRepository(db.mediaDao(), db.preferencesDao(), db.searchHistoryDao())
    }

    val allMedia: StateFlow<List<MediaEntity>> = repository.allMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val trackedMedia: StateFlow<List<MediaEntity>> = repository.trackedMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val continueWatching: StateFlow<List<MediaEntity>> = repository.continueWatching.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favorites: StateFlow<List<MediaEntity>> = repository.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val watchlistMedia: StateFlow<List<MediaEntity>> = repository.watchlistMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Google TV look & feel mode toggle (defaults to false for classic mobile experience)
    val isGoogleTvMode = MutableStateFlow(false)

    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<MediaType?>(null) // null = all
    val selectedGenreFilter = MutableStateFlow<String?>(null) // null = all genres

    val selectedLibraryTab = MutableStateFlow(LibraryTab.WATCHING)
    val librarySortOption = MutableStateFlow(SortOption.RECENT)
    val selectedCollectionFilter = MutableStateFlow<String?>(null)

    val selectedMediaId = MutableStateFlow<String?>(null)
    val isSearchingTmdb = MutableStateFlow(false)

    // Multi-Search State & Category Filters
    data class SearchCounts(val all: Int = 0, val movies: Int = 0, val tvShows: Int = 0, val people: Int = 0)

    val multiSearchQuery = MutableStateFlow("")
    val selectedSearchCategory = MutableStateFlow(MultiSearchCategory.ALL)
    val multiSearchResults = MutableStateFlow<List<TmdbSearchResultItem>>(emptyList())
    val isMultiSearching = MutableStateFlow(false)
    val selectedPersonForDetail = MutableStateFlow<TmdbSearchResultItem.Person?>(null)
    val personDetailData = MutableStateFlow<TmdbPersonDetailDto?>(null)
    val isLoadingPersonDetail = MutableStateFlow(false)
    val activeWatchProviders = MutableStateFlow<WatchProvidersInfo?>(null)
    val isLoadingWatchProviders = MutableStateFlow(false)
    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository.searchHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentSearchHistory: StateFlow<List<String>> = searchHistory.map { list ->
        list.map { it.query }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMultiSearchResults: StateFlow<List<TmdbSearchResultItem>> = combine(
        multiSearchResults,
        selectedSearchCategory
    ) { results, category ->
        when (category) {
            MultiSearchCategory.ALL -> results
            MultiSearchCategory.MOVIES -> results.filter { it is TmdbSearchResultItem.Media && it.isMovie }
            MultiSearchCategory.TV_SHOWS -> results.filter { it is TmdbSearchResultItem.Media && !it.isMovie }
            MultiSearchCategory.PEOPLE -> results.filterIsInstance<TmdbSearchResultItem.Person>()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchCounts: StateFlow<SearchCounts> = multiSearchResults.map { list ->
        val movies = list.count { it is TmdbSearchResultItem.Media && it.isMovie }
        val tv = list.count { it is TmdbSearchResultItem.Media && !it.isMovie }
        val people = list.count { it is TmdbSearchResultItem.Person }
        SearchCounts(all = list.size, movies = movies, tvShows = tv, people = people)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchCounts())

    // Trending Now (/trending/all/day) State
    val trendingItems = MutableStateFlow<List<MediaEntity>>(emptyList())
    val isTrendingLoading = MutableStateFlow(false)
    val trendingCategoryFilter = MutableStateFlow(TrendingFilterCategory.ALL)
    val trendingTimeWindow = MutableStateFlow("day")
    val trendingError = MutableStateFlow<String?>(null)

    val filteredTrendingItems: StateFlow<List<MediaEntity>> = combine(
        trendingItems,
        trendingCategoryFilter
    ) { items, category ->
        when (category) {
            TrendingFilterCategory.ALL -> items
            TrendingFilterCategory.MOVIES -> items.filter { it.getMediaTypeEnum() == MediaType.MOVIE }
            TrendingFilterCategory.TV_SHOWS -> items.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preferences & Localization
    val userPreferences: StateFlow<UserPreferencesEntity> = repository.userPreferences.map {
        it ?: UserPreferencesEntity(id = 1, regionCode = "AUTO", languageCode = "all", autoDetect = true)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferencesEntity(id = 1, regionCode = "AUTO", languageCode = "all", autoDetect = true)
    )

    // Effective country / region code (resolves AUTO to device default)
    val effectiveRegionCode: StateFlow<String> = userPreferences.map { prefs ->
        if (prefs.autoDetect || prefs.regionCode.equals("AUTO", ignoreCase = true)) {
            RegionalDefaults.detectDeviceRegion().code
        } else {
            prefs.regionCode
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RegionalDefaults.detectDeviceRegion().code
    )

    val isTmdbConfigured: StateFlow<Boolean> = userPreferences.map {
        val key = getActiveTmdbApiKey()
        key.isNotBlank() && key != "YOUR_TMDB_API_KEY"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private data class SearchFilters(
        val query: String,
        val type: MediaType?,
        val genre: String?
    )

    private val searchFilters = combine(
        searchQuery,
        selectedTypeFilter,
        selectedGenreFilter
    ) { query, type, genre ->
        SearchFilters(query, type, genre)
    }

    // Filtered Discover list respecting search, type, genre, and regional language preferences
    val filteredDiscover: StateFlow<List<MediaEntity>> = combine(
        allMedia,
        searchFilters,
        userPreferences,
        effectiveRegionCode
    ) { items, filters, prefs, regionCode ->
        val query = filters.query.trim()
        val typeFilter = filters.type
        val genreFilter = filters.genre

        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.cast.contains(query, ignoreCase = true) ||
                    item.directorOrCreator.contains(query, ignoreCase = true) ||
                    item.genres.contains(query, ignoreCase = true) ||
                    item.overview.contains(query, ignoreCase = true)

            val matchesType = typeFilter == null || item.getMediaTypeEnum() == typeFilter

            val matchesGenre = genreFilter == null ||
                    item.getGenreList().any { it.equals(genreFilter, ignoreCase = true) }

            // When user types a specific search query, don't hide matching items by language
            val matchesLanguage = if (query.isBlank() && prefs.filterCatalogByLanguage && !prefs.languageCode.equals("all", ignoreCase = true)) {
                item.originalLanguage.equals(prefs.languageCode, ignoreCase = true)
            } else {
                true
            }

            matchesQuery && matchesType && matchesGenre && matchesLanguage
        }.sortedWith(
            compareByDescending<MediaEntity> {
                // Boost content matching the user's localized region or language
                if (it.originCountry.equals(regionCode, ignoreCase = true) ||
                    (!prefs.languageCode.equals("all", ignoreCase = true) && it.originalLanguage.equals(prefs.languageCode, ignoreCase = true))
                ) 1 else 0
            }.thenByDescending { it.communityRating }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Library list
    val filteredLibrary: StateFlow<List<MediaEntity>> = combine(
        trackedMedia,
        selectedLibraryTab,
        librarySortOption,
        selectedCollectionFilter
    ) { items, tab, sort, collection ->
        val filtered = items.filter { item ->
            val matchesTab = when (tab) {
                LibraryTab.WATCHING -> item.getWatchStatusEnum() == WatchStatus.WATCHING
                LibraryTab.PLAN_TO_WATCH -> item.getWatchStatusEnum() == WatchStatus.PLAN_TO_WATCH
                LibraryTab.COMPLETED -> item.getWatchStatusEnum() == WatchStatus.COMPLETED
                LibraryTab.FAVORITES -> item.isFavorite
                LibraryTab.ALL_TRACKED -> true
            }

            val matchesCollection = collection == null || item.customWatchlist == collection

            matchesTab && matchesCollection
        }

        when (sort) {
            SortOption.RECENT -> filtered.sortedByDescending { it.lastWatchedDate ?: it.createdAt }
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.RATING -> filtered.sortedByDescending {
                if (it.userRating > 0) it.userRating else it.communityRating
            }
            SortOption.YEAR -> filtered.sortedByDescending { it.releaseYear }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableGenres: StateFlow<List<String>> = allMedia.combine(MutableStateFlow(Unit)) { items, _ ->
        items.flatMap { it.getGenreList() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCollections: StateFlow<List<String>> = trackedMedia.combine(MutableStateFlow(Unit)) { items, _ ->
        items.mapNotNull { it.customWatchlist }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ViewingStats> = trackedMedia.combine(MutableStateFlow(Unit)) { items, _ ->
        var totalMinutes: Long = 0
        var completedMovies = 0
        var completedShows = 0
        var activeShows = 0
        var totalEpisodes = 0
        var ratingSum = 0f
        var ratedCount = 0
        val genreCountMap = mutableMapOf<String, Int>()

        items.forEach { item ->
            val isMovie = item.getMediaTypeEnum() == MediaType.MOVIE
            val status = item.getWatchStatusEnum()

            if (isMovie) {
                if (status == WatchStatus.COMPLETED) {
                    completedMovies++
                    totalMinutes += item.runtimeMinutes
                }
            } else {
                if (status == WatchStatus.COMPLETED) {
                    completedShows++
                } else if (status == WatchStatus.WATCHING) {
                    activeShows++
                }
                totalEpisodes += item.watchedEpisodes
                totalMinutes += (item.watchedEpisodes.toLong() * item.runtimeMinutes.toLong())
            }

            if (item.userRating > 0f) {
                ratingSum += item.userRating
                ratedCount++
            }

            if (status == WatchStatus.COMPLETED || status == WatchStatus.WATCHING) {
                item.getGenreList().forEach { genre ->
                    genreCountMap[genre] = (genreCountMap[genre] ?: 0) + 1
                }
            }
        }

        val totalLoggedMedia = completedMovies + completedShows + activeShows
        val topGenres = genreCountMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map {
                val pct = if (totalLoggedMedia > 0) (it.value.toFloat() / totalLoggedMedia) else 0f
                GenreStat(it.key, it.value, pct)
            }

        val avgRating = if (ratedCount > 0) ratingSum / ratedCount else 0f

        val (rank, badge) = when {
            totalMinutes >= 20000 -> Pair("Cinema Legend", "🏆")
            totalMinutes >= 10000 -> Pair("Master Cinephile", "🌟")
            totalMinutes >= 5000 -> Pair("Binge Specialist", "🍿")
            totalMinutes >= 1500 -> Pair("Movie Buff", "🎞️")
            totalMinutes > 0 -> Pair("Film Explorer", "🎬")
            else -> Pair("Film Novice", "🎟️")
        }

        ViewingStats(
            totalTimeMinutes = totalMinutes,
            completedMoviesCount = completedMovies,
            completedShowsCount = completedShows,
            activeShowsCount = activeShows,
            totalEpisodesLogged = totalEpisodes,
            averageUserRating = avgRating,
            ratedItemsCount = ratedCount,
            topGenres = topGenres,
            cinephileRank = rank,
            cinephileBadge = badge
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewingStats())

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }

        // Live TMDB Search Debounce
        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .map { it.trim() }
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    repository.recordSearchQuery(query)
                    val key = getActiveTmdbApiKey()
                    if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                        isSearchingTmdb.value = true
                        try {
                            repository.searchTmdb(
                                query = query,
                                apiKey = key,
                                language = userPreferences.value.languageCode,
                                region = null
                            )
                        } finally {
                            isSearchingTmdb.value = false
                        }
                    }
                }
        }

        // Live TMDB /search/multi Debounce
        viewModelScope.launch {
            multiSearchQuery
                .debounce(350)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { query ->
                    performMultiSearch(query)
                }
        }

        // Fetch live TMDB trending upon startup
        viewModelScope.launch {
            val key = getActiveTmdbApiKey()
            if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                repository.fetchTrendingFromTmdb(
                    apiKey = key,
                    language = userPreferences.value.languageCode,
                    region = effectiveRegionCode.value
                )
            }
            fetchTrendingNow("day")
        }
    }

    fun setTrendingCategory(category: TrendingFilterCategory) {
        trendingCategoryFilter.value = category
    }

    fun setTrendingTimeWindow(window: String) {
        trendingTimeWindow.value = window
        fetchTrendingNow(window)
    }

    fun fetchTrendingNow(timeWindow: String = trendingTimeWindow.value) {
        viewModelScope.launch {
            isTrendingLoading.value = true
            trendingError.value = null
            try {
                val key = getActiveTmdbApiKey()
                val items = repository.getTrendingNow(
                    apiKey = key,
                    language = userPreferences.value.languageCode,
                    timeWindow = timeWindow
                )
                trendingItems.value = items
                trendingTimeWindow.value = timeWindow
            } catch (e: Exception) {
                trendingError.value = e.message ?: "Failed to fetch trending"
            } finally {
                isTrendingLoading.value = false
            }
        }
    }

    fun performMultiSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            multiSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            isMultiSearching.value = true
            try {
                val key = getActiveTmdbApiKey()
                val results = repository.searchMultiTmdb(
                    query = trimmed,
                    apiKey = key,
                    language = userPreferences.value.languageCode,
                    region = null // Pass null for unrestricted global multi-search
                )
                multiSearchResults.value = results
                if (trimmed.length >= 2) {
                    repository.recordSearchQuery(trimmed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                multiSearchResults.value = repository.searchLocalAndSample(trimmed)
            } finally {
                isMultiSearching.value = false
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.removeSearchQuery(query)
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun setMultiSearchCategory(category: MultiSearchCategory) {
        selectedSearchCategory.value = category
    }

    fun setMultiSearchQuery(query: String) {
        multiSearchQuery.value = query
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            performMultiSearch(trimmed)
        } else {
            multiSearchResults.value = emptyList()
        }
    }

    fun selectPersonForDetail(person: TmdbSearchResultItem.Person?) {
        selectedPersonForDetail.value = person
        personDetailData.value = null
        if (person != null) {
            viewModelScope.launch {
                isLoadingPersonDetail.value = true
                try {
                    val key = getActiveTmdbApiKey()
                    personDetailData.value = repository.fetchPersonDetails(person.id, key)
                } finally {
                    isLoadingPersonDetail.value = false
                }
            }
        }
    }

    fun openMediaFromSearch(entity: MediaEntity) {
        viewModelScope.launch {
            repository.saveMedia(entity)
            selectMedia(entity.id)
        }
    }

    fun toggleWatchlistQuick(mediaId: String) {
        viewModelScope.launch {
            val current = allMedia.value.find { it.id == mediaId }
            if (current != null) {
                val currentStatus = current.getWatchStatusEnum()
                val nextStatus = if (currentStatus == WatchStatus.NONE) {
                    WatchStatus.PLAN_TO_WATCH
                } else {
                    WatchStatus.NONE
                }
                repository.updateStatus(mediaId, nextStatus)
            }
        }
    }

    companion object {
        const val EMBEDDED_TMDB_API_KEY = "967abbed236b6e3d97da4ced26b2199c"
    }

    fun getActiveTmdbApiKey(): String {
        val customKey = userPreferences.value.customTmdbApiKey.trim()
        if (customKey.isNotBlank()) return customKey
        val buildKey = try {
            BuildConfig.TMDB_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "YOUR_TMDB_API_KEY") {
            return buildKey
        }
        return EMBEDDED_TMDB_API_KEY
    }

    fun toggleGoogleTvMode() {
        isGoogleTvMode.value = !isGoogleTvMode.value
    }

    fun setGoogleTvMode(enabled: Boolean) {
        isGoogleTvMode.value = enabled
    }

    fun selectMedia(id: String?) {
        selectedMediaId.value = id
        activeWatchProviders.value = null
        if (id != null) {
            val media = allMedia.value.find { it.id == id }
            val region = effectiveRegionCode.value
            if (media != null) {
                activeWatchProviders.value = media.getWatchProvidersInfo(region)
            }
            if (id.startsWith("tmdb_m_") || id.startsWith("tmdb_tv_")) {
                viewModelScope.launch {
                    val key = getActiveTmdbApiKey()
                    if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                        repository.fetchDetailsFromTmdb(id, key)
                        isLoadingWatchProviders.value = true
                        val liveProviders = repository.fetchLiveWatchProviders(id, region, key)
                        if (liveProviders != null && !liveProviders.isEmpty) {
                            activeWatchProviders.value = liveProviders
                        }
                        isLoadingWatchProviders.value = false
                    }
                }
            }
        }
    }

    fun refreshWatchProviders(mediaId: String) {
        val region = effectiveRegionCode.value
        val media = allMedia.value.find { it.id == mediaId }
        if (media != null) {
            activeWatchProviders.value = media.getWatchProvidersInfo(region)
        }
        if (mediaId.startsWith("tmdb_m_") || mediaId.startsWith("tmdb_tv_")) {
            viewModelScope.launch {
                val key = getActiveTmdbApiKey()
                if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                    isLoadingWatchProviders.value = true
                    val liveProviders = repository.fetchLiveWatchProviders(mediaId, region, key)
                    if (liveProviders != null && !liveProviders.isEmpty) {
                        activeWatchProviders.value = liveProviders
                    }
                    isLoadingWatchProviders.value = false
                }
            }
        }
    }

    fun syncTrendingFromTmdb() {
        viewModelScope.launch {
            val key = getActiveTmdbApiKey()
            if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                repository.fetchTrendingFromTmdb(
                    apiKey = key,
                    language = userPreferences.value.languageCode,
                    region = effectiveRegionCode.value
                )
            }
            fetchTrendingNow(trendingTimeWindow.value)
        }
    }

    fun updateStatus(id: String, status: WatchStatus) {
        viewModelScope.launch {
            repository.updateStatus(id, status)
        }
    }

    fun incrementEpisode(id: String) {
        viewModelScope.launch {
            repository.incrementEpisode(id)
        }
    }

    fun setWatchedEpisodes(id: String, episodes: Int) {
        viewModelScope.launch {
            repository.updateWatchedEpisodes(id, episodes)
        }
    }

    fun updateRatingAndReview(id: String, rating: Float, review: String) {
        viewModelScope.launch {
            repository.updateRatingAndReview(id, rating, review)
        }
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, current)
        }
    }

    fun setCustomWatchlist(id: String, watchlist: String?) {
        viewModelScope.launch {
            repository.updateCustomWatchlist(id, watchlist)
        }
    }

    fun deleteMedia(id: String) {
        viewModelScope.launch {
            repository.deleteMedia(id)
            if (selectedMediaId.value == id) {
                selectedMediaId.value = null
            }
        }
    }

    fun updateRegion(regionCode: String, autoDetect: Boolean) {
        viewModelScope.launch {
            val current = userPreferences.value
            repository.updatePreferences(
                current.copy(regionCode = regionCode, autoDetect = autoDetect)
            )
            syncTrendingFromTmdb()
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            val current = userPreferences.value
            repository.updatePreferences(
                current.copy(languageCode = languageCode)
            )
            syncTrendingFromTmdb()
        }
    }

    fun toggleFilterByLanguage(enabled: Boolean) {
        viewModelScope.launch {
            val current = userPreferences.value
            repository.updatePreferences(
                current.copy(filterCatalogByLanguage = enabled)
            )
        }
    }

    fun updateCustomTmdbApiKey(apiKey: String) {
        viewModelScope.launch {
            val current = userPreferences.value
            repository.updatePreferences(
                current.copy(customTmdbApiKey = apiKey.trim())
            )
            syncTrendingFromTmdb()
        }
    }

    fun addCustomMedia(
        title: String,
        mediaType: MediaType,
        releaseYear: Int,
        genres: String,
        runtimeMinutes: Int,
        totalSeasons: Int,
        totalEpisodes: Int,
        overview: String,
        director: String,
        cast: String,
        status: WatchStatus,
        initialRating: Float,
        posterUrl: String,
        language: String = "en",
        originCountry: String = "US"
    ) {
        viewModelScope.launch {
            val id = "custom_" + UUID.randomUUID().toString().take(8)
            val fallbackPoster = if (posterUrl.isNotBlank()) posterUrl else {
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80"
            }
            val item = MediaEntity(
                id = id,
                title = title,
                mediaType = mediaType.name,
                releaseYear = releaseYear,
                genres = genres,
                posterUrl = fallbackPoster,
                backdropUrl = fallbackPoster,
                overview = overview,
                directorOrCreator = director,
                cast = cast,
                communityRating = 8.0f,
                runtimeMinutes = runtimeMinutes,
                totalSeasons = totalSeasons,
                totalEpisodes = if (mediaType == MediaType.MOVIE) 1 else totalEpisodes,
                networkOrStudio = "Custom Entry",
                status = status.name,
                userRating = initialRating,
                userReview = "",
                isFavorite = false,
                lastWatchedDate = if (status != WatchStatus.NONE) System.currentTimeMillis() else null,
                watchedEpisodes = if (status == WatchStatus.COMPLETED) (if (mediaType == MediaType.MOVIE) 1 else totalEpisodes) else 0,
                isCustom = true,
                originalLanguage = language,
                originCountry = originCountry
            )
            repository.saveMedia(item)
        }
    }
}
