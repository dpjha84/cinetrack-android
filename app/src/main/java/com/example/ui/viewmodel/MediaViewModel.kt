package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.CastMemberItem
import com.example.data.model.GenreStat
import com.example.data.model.MediaType
import com.example.data.model.MultiSearchCategory
import com.example.data.model.RegionalDefaults
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.TrailerItem
import com.example.data.model.ViewingStats
import com.example.data.model.WatchProvidersInfo
import com.example.data.model.WatchStatus
import com.example.data.model.SeasonItem
import com.example.data.model.EpisodeItem
import com.example.data.model.TvWatchStatus
import com.example.data.remote.firebase.FirebaseAuthHelper
import com.example.data.remote.firebase.FirestoreSyncManager
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.data.repository.MediaRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    PLAN_TO_WATCH("Watchlist"),
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

enum class TrailerCategory(val label: String) {
    ALL("All"),
    NOW_PLAYING("Now Playing"),
    UPCOMING("Upcoming"),
    MOVIES("Movies"),
    TV_SHOWS("Series")
}

enum class TopRatedTab(val label: String) {
    MOVIES("Top Rated Movies"),
    TV_SERIES("Top Rated Series")
}

@OptIn(FlowPreview::class)
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    val firestoreSyncManager = FirestoreSyncManager(application)

    private val repository: MediaRepository = run {
        val db = AppDatabase.getDatabase(application)
        MediaRepository(
            db.mediaDao(),
            db.preferencesDao(),
            db.searchHistoryDao(),
            db.customCollectionDao(),
            db.tvProgressDao(),
            db.favoritePersonDao(),
            firestoreSyncManager
        )
    }

    // Authentication & Cloud Sync
    val currentUser: StateFlow<FirebaseUser?> = FirebaseAuthHelper.currentUser
    val isGuest: StateFlow<Boolean> = FirebaseAuthHelper.isGuest
    val isSyncingCloud = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow<String?>(null)
    private var syncJob: kotlinx.coroutines.Job? = null
    private var activeSyncUid: String? = null
    private var signOutJob: kotlinx.coroutines.Job? = null

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

    val favoritePeople: StateFlow<List<FavoritePersonEntity>> = repository.favoritePeople.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val watchlistMedia: StateFlow<List<MediaEntity>> = repository.watchlistMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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
    val selectedSearchGenre = MutableStateFlow<String?>(null)
    val selectedSearchCategory = MutableStateFlow(MultiSearchCategory.ALL)
    val multiSearchResults = MutableStateFlow<List<TmdbSearchResultItem>>(emptyList())
    val isMultiSearching = MutableStateFlow(false)
    val selectedPersonForDetail = MutableStateFlow<TmdbSearchResultItem.Person?>(null)
    val personDetailData = MutableStateFlow<TmdbPersonDetailDto?>(null)
    val isLoadingPersonDetail = MutableStateFlow(false)
    private var personDetailJob: kotlinx.coroutines.Job? = null
    val activeWatchProviders = MutableStateFlow<WatchProvidersInfo?>(null)
    val isLoadingWatchProviders = MutableStateFlow(false)
    val currentMediaCast = MutableStateFlow<List<CastMemberItem>>(emptyList())
    private var castLoadJob: kotlinx.coroutines.Job? = null

    // TV Seasons & Episodes State
    val selectedSeasonForDetail = MutableStateFlow<SeasonItem?>(null)
    val selectedEpisodeDetail = MutableStateFlow<EpisodeItem?>(null)
    val currentTvSeasons = MutableStateFlow<List<SeasonItem>>(emptyList())
    val currentSeasonEpisodes = MutableStateFlow<List<EpisodeItem>>(emptyList())
    val isLoadingSeasons = MutableStateFlow(false)
    val isLoadingEpisodes = MutableStateFlow(false)
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

    // Trailers Feed State
    val trailerCategoryFilter = MutableStateFlow(TrailerCategory.ALL)
    private val _trailers = MutableStateFlow<List<TrailerItem>>(emptyList())
    val trailers: StateFlow<List<TrailerItem>> = _trailers.asStateFlow()
    val isTrailersLoading = MutableStateFlow(false)
    private var trailersJob: kotlinx.coroutines.Job? = null
    private var trailersRequestSeq: Long = 0L

    // Top 10 Highest-Rated Titles on TMDB
    val selectedTopRatedTab = MutableStateFlow(TopRatedTab.MOVIES)
    val topRatedMovies = MutableStateFlow<List<MediaEntity>>(emptyList())
    val topRatedTv = MutableStateFlow<List<MediaEntity>>(emptyList())
    val isTopRatedLoading = MutableStateFlow(false)

    val currentTopRatedList: StateFlow<List<MediaEntity>> = combine(
        selectedTopRatedTab,
        topRatedMovies,
        topRatedTv
    ) { tab, movies, tv ->
        when (tab) {
            TopRatedTab.MOVIES -> movies
            TopRatedTab.TV_SERIES -> tv
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

    val filteredTrailers: StateFlow<List<TrailerItem>> = combine(
        _trailers,
        trailerCategoryFilter,
        effectiveRegionCode,
        userPreferences
    ) { list, category, regionCode, prefs ->
        val relevantList = list
            .filter { it.isWithinRelevanceWindow() }
            .distinctBy { it.mediaId }

        val filtered = when (category) {
            TrailerCategory.ALL -> relevantList
            TrailerCategory.NOW_PLAYING -> relevantList.filter { it.isNowPlaying }
            TrailerCategory.UPCOMING -> relevantList.filter { it.isUpcoming }
            TrailerCategory.MOVIES -> relevantList.filter { it.mediaType == MediaType.MOVIE }
            TrailerCategory.TV_SHOWS -> relevantList.filter { it.mediaType == MediaType.TV_SHOW }
        }

        filtered.sortedWith(
            compareByDescending<TrailerItem> {
                var score = 0
                if (it.originCountry.equals(regionCode, ignoreCase = true)) score += 4
                if (!prefs.languageCode.equals("all", ignoreCase = true) && it.language.equals(prefs.languageCode, ignoreCase = true)) score += 2
                if (it.videoType.equals("Trailer", ignoreCase = true)) score += 2
                score
            }.thenByDescending { it.releaseDate }
                .thenByDescending { it.communityRating }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

            val isExcludedLegacy = query.isBlank() && (
                item.releaseYear < 2023 ||
                item.title.equals("inception", ignoreCase = true) ||
                item.title.equals("fight club", ignoreCase = true) ||
                item.title.equals("parasite", ignoreCase = true) ||
                item.title.equals("the dark knight", ignoreCase = true) ||
                item.title.equals("pulp fiction", ignoreCase = true) ||
                item.title.equals("the shawshank redemption", ignoreCase = true) ||
                item.title.contains("panchayat", ignoreCase = true) ||
                item.title.equals("rrr", ignoreCase = true)
            )

            matchesQuery && matchesType && matchesGenre && matchesLanguage && !isExcludedLegacy
        }.groupBy { item ->
            val normTitle = item.title.lowercase().replace(Regex("[^a-z0-9]"), "")
            "${normTitle}_${item.mediaType}"
        }.values.map { group ->
            group.maxBy { item ->
                val isTracked = item.status != WatchStatus.NONE.name ||
                    item.isFavorite ||
                    item.userRating > 0f ||
                    !item.customWatchlist.isNullOrBlank()
                val isTmdbLive = item.id.startsWith("tmdb_m_") || item.id.startsWith("tmdb_tv_")
                when {
                    isTracked && isTmdbLive -> 5
                    isTracked -> 4
                    isTmdbLive -> 3
                    item.isCustom -> 2
                    else -> 1
                }
            }
        }.sortedWith(
            compareByDescending<MediaEntity> {
                // Boost content matching the user's localized region or language
                if (it.originCountry.equals(regionCode, ignoreCase = true) ||
                    (!prefs.languageCode.equals("all", ignoreCase = true) && it.originalLanguage.equals(prefs.languageCode, ignoreCase = true))
                ) 1 else 0
            }.thenByDescending { it.releaseYear } // Prioritize current 2024-2026 releases!
            .thenByDescending { it.communityRating }
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

    val customCollections: StateFlow<List<String>> = combine(
        trackedMedia,
        repository.savedCollections
    ) { items, saved ->
        val fromItems = items.mapNotNull { it.customWatchlist }.filter { it.isNotBlank() }
        val fromSaved = saved.map { it.name }.filter { it.isNotBlank() }
        (fromSaved + fromItems).distinct().sorted()
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
        FirebaseAuthHelper.initialize(viewModelScope)

        viewModelScope.launch {
            repository.seedIfEmpty()
            currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .filter { !it.isNullOrBlank() }
                .collect {
                    syncWithCloud(force = false)
                }
        }

        // Listen to region changes to refresh regional popular, trending and trailers
        viewModelScope.launch {
            effectiveRegionCode.collect {
                fetchPopularMedia()
                fetchTrendingNow()
                fetchTrailers()
            }
        }

        // Live TMDB Search Debounce
        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .map { it.trim() }
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
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

        // Fetch Top 10 rated lists upon startup
        viewModelScope.launch {
            fetchTopRated()
        }
    }

    fun setTopRatedTab(tab: TopRatedTab) {
        selectedTopRatedTab.value = tab
    }

    fun fetchTopRated() {
        viewModelScope.launch {
            isTopRatedLoading.value = true
            try {
                val key = getActiveTmdbApiKey()
                val lang = userPreferences.value.languageCode
                val reg = effectiveRegionCode.value
                val movies = repository.getTopRatedMovies(key, lang, reg)
                val tv = repository.getTopRatedTv(key, lang)
                topRatedMovies.value = movies
                topRatedTv.value = tv
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isTopRatedLoading.value = false
            }
        }
    }

    fun fetchPopularMedia() {
        viewModelScope.launch {
            try {
                val key = getActiveTmdbApiKey()
                if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                    repository.fetchPopularMediaFromTmdb(
                        apiKey = key,
                        language = userPreferences.value.languageCode,
                        region = effectiveRegionCode.value
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTrailerCategory(category: TrailerCategory) {
        trailerCategoryFilter.value = category
    }

    fun fetchTrailers() {
        trailersJob?.cancel()
        val reqId = ++trailersRequestSeq
        trailersJob = viewModelScope.launch {
            isTrailersLoading.value = true
            try {
                val key = getActiveTmdbApiKey()
                val list = repository.fetchTrailers(
                    apiKey = key,
                    region = effectiveRegionCode.value,
                    language = userPreferences.value.languageCode
                )
                if (reqId == trailersRequestSeq && list.isNotEmpty()) {
                    _trailers.value = list
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (reqId == trailersRequestSeq) {
                    isTrailersLoading.value = false
                }
            }
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
                    region = effectiveRegionCode.value,
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
            if (selectedSearchGenre.value == null) {
                multiSearchResults.value = emptyList()
            }
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
            } catch (e: Exception) {
                e.printStackTrace()
                multiSearchResults.value = repository.searchLocalAndSample(trimmed)
            } finally {
                isMultiSearching.value = false
            }
        }
    }

    fun recordCompletedSearch(query: String = multiSearchQuery.value) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            viewModelScope.launch {
                repository.recordSearchQuery(trimmed)
            }
        }
    }

    fun submitMultiSearch(query: String = multiSearchQuery.value) {
        val trimmed = query.trim()
        multiSearchQuery.value = trimmed
        if (trimmed.isNotBlank()) {
            selectedSearchGenre.value = null
            performMultiSearch(trimmed)
            recordCompletedSearch(trimmed)
        } else if (selectedSearchGenre.value == null) {
            multiSearchResults.value = emptyList()
        }
    }

    fun selectSearchGenre(genre: String) {
        val trimmed = genre.trim()
        if (trimmed.isBlank()) return
        selectedSearchGenre.value = trimmed
        multiSearchQuery.value = ""
        selectedSearchCategory.value = MultiSearchCategory.ALL
        viewModelScope.launch {
            isMultiSearching.value = true
            try {
                val key = getActiveTmdbApiKey()
                val results = repository.discoverByGenreTmdb(
                    genre = trimmed,
                    apiKey = key,
                    language = userPreferences.value.languageCode,
                    region = effectiveRegionCode.value
                )
                multiSearchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
                multiSearchResults.value = repository.discoverLocalByGenre(trimmed)
            } finally {
                isMultiSearching.value = false
            }
        }
    }

    fun clearSearchGenre() {
        selectedSearchGenre.value = null
        if (multiSearchQuery.value.isBlank()) {
            multiSearchResults.value = emptyList()
        } else {
            performMultiSearch(multiSearchQuery.value)
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
            selectedSearchGenre.value = null
        } else if (selectedSearchGenre.value == null) {
            multiSearchResults.value = emptyList()
        }
    }

    fun selectPersonForDetail(person: TmdbSearchResultItem.Person?) {
        personDetailJob?.cancel()
        selectedPersonForDetail.value = person
        personDetailData.value = null
        if (person != null && person.id > 0L) {
            personDetailJob = viewModelScope.launch {
                isLoadingPersonDetail.value = true
                try {
                    val key = getActiveTmdbApiKey()
                    personDetailData.value = repository.fetchPersonDetails(person.id, key)
                } finally {
                    isLoadingPersonDetail.value = false
                }
            }
        } else {
            isLoadingPersonDetail.value = false
        }
    }

    fun openPersonByName(actorName: String) {
        val trimmed = actorName.trim()
        if (trimmed.isBlank()) return
        personDetailJob?.cancel()
        selectedPersonForDetail.value = TmdbSearchResultItem.Person(
            id = 0L,
            name = trimmed,
            profileUrl = null,
            department = "Acting",
            knownFor = emptyList(),
            popularity = 0.0
        )
        personDetailData.value = null
        personDetailJob = viewModelScope.launch {
            isLoadingPersonDetail.value = true
            try {
                val key = getActiveTmdbApiKey()
                val matched = repository.searchPersonByName(
                    name = trimmed,
                    apiKey = key,
                    language = userPreferences.value.languageCode
                )
                if (matched != null) {
                    selectedPersonForDetail.value = matched
                    if (matched.id > 0L) {
                        personDetailData.value = repository.fetchPersonDetails(matched.id, key)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingPersonDetail.value = false
            }
        }
    }

    fun openCastMember(member: CastMemberItem) {
        if (member.id > 0L) {
            selectPersonForDetail(
                TmdbSearchResultItem.Person(
                    id = member.id,
                    name = member.name,
                    profileUrl = member.profileUrl,
                    department = "Acting",
                    knownFor = emptyList()
                )
            )
        } else {
            openPersonByName(member.name)
        }
    }

    fun openMediaFromSearch(entity: MediaEntity) {
        viewModelScope.launch {
            repository.ensureMediaCached(entity)
            selectMedia(entity.id)
        }
    }

    fun toggleWatchlistQuick(entity: MediaEntity) {
        viewModelScope.launch {
            repository.toggleWatchlistForEntity(entity)
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

    fun syncWithCloud(force: Boolean = true) {
        val uid = currentUser.value?.uid ?: return
        if (syncJob?.isActive == true && activeSyncUid == uid) return

        syncJob?.cancel()
        activeSyncUid = uid
        syncJob = viewModelScope.launch {
            isSyncingCloud.value = true
            try {
                // Ensure any pending sign-out cleanup completes before pulling/pushing for the new user
                signOutJob?.join()
                repository.syncWithCloud(uid)
                syncStatusMessage.value = "Synced with cloud successfully"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                syncStatusMessage.value = "Cloud sync error: ${e.message}"
            } finally {
                if (activeSyncUid == uid) {
                    isSyncingCloud.value = false
                }
            }
        }
    }

    fun continueAsGuest() {
        FirebaseAuthHelper.continueAsGuest()
    }

    fun signInWithGoogle(
        activity: Activity,
        onSuccess: (FirebaseUser) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        FirebaseAuthHelper.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                syncWithCloud(force = false)
                onSuccess(user)
            },
            onError = onError
        )
    }

    fun signOut(context: Context) {
        syncJob?.cancel()
        activeSyncUid = null
        isSyncingCloud.value = false
        selectedCollectionFilter.value = null
        selectedMediaId.value = null
        FirebaseAuthHelper.signOut(context, viewModelScope) {
            syncStatusMessage.value = "Signed out — device library cleared"
        }
        signOutJob = viewModelScope.launch {
            repository.clearAllUserData()
        }
    }

    fun deleteAccountAndCloudData(context: Context, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUser.value?.uid
        viewModelScope.launch {
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager.deleteUserCloudData(uid)
            }
            repository.clearAllUserData()
            FirebaseAuthHelper.deleteAccount(context, viewModelScope) { success, error ->
                syncStatusMessage.value = if (success) "Account and cloud data deleted" else "Account deletion notice: $error"
                onComplete(success, error)
            }
        }
    }

    fun resetLocalLibrary(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllUserData()
            syncStatusMessage.value = "Local data cleared"
            onComplete()
        }
    }

    fun selectMedia(id: String?) {
        selectedMediaId.value = id
        activeWatchProviders.value = null
        currentMediaCast.value = emptyList()
        castLoadJob?.cancel()
        selectedSeasonForDetail.value = null
        selectedEpisodeDetail.value = null
        currentTvSeasons.value = emptyList()
        currentSeasonEpisodes.value = emptyList()
        if (id != null) {
            val media = allMedia.value.find { it.id == id }
                ?: trendingItems.value.find { it.id == id }
                ?: currentTopRatedList.value.find { it.id == id }
            val region = effectiveRegionCode.value
            val initialNames = media?.getCastList().orEmpty()
            if (media != null) {
                if (initialNames.isNotEmpty()) {
                    currentMediaCast.value = initialNames.map { CastMemberItem(name = it) }
                }
                activeWatchProviders.value = media.getWatchProvidersInfo(region)
                if (media.getMediaTypeEnum() == MediaType.TV_SHOW) {
                    loadSeasonsForMedia(media)
                }
            }
            val key = getActiveTmdbApiKey()
            val lang = userPreferences.value.languageCode
            if (id.startsWith("tmdb_m_") || id.startsWith("tmdb_tv_")) {
                castLoadJob = viewModelScope.launch {
                    if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                        val creditsCast = repository.fetchDetailsFromTmdb(id, key)
                        if (selectedMediaId.value == id && creditsCast.isNotEmpty()) {
                            currentMediaCast.value = creditsCast
                        } else if (selectedMediaId.value == id && initialNames.isNotEmpty()) {
                            val resolved = repository.resolveCastMembers(initialNames, key, lang)
                            if (selectedMediaId.value == id && resolved.isNotEmpty()) {
                                currentMediaCast.value = resolved
                            }
                        }
                        isLoadingWatchProviders.value = true
                        val liveProviders = repository.fetchLiveWatchProviders(id, region, key)
                        if (liveProviders != null && !liveProviders.isEmpty) {
                            activeWatchProviders.value = liveProviders
                        }
                        isLoadingWatchProviders.value = false
                        allMedia.value.find { it.id == id }?.let { updated ->
                            if (updated.getMediaTypeEnum() == MediaType.TV_SHOW) {
                                loadSeasonsForMedia(updated)
                            }
                        }
                    }
                }
            } else if (initialNames.isNotEmpty()) {
                castLoadJob = viewModelScope.launch {
                    val resolved = repository.resolveCastMembers(initialNames, key, lang)
                    if (selectedMediaId.value == id && resolved.isNotEmpty()) {
                        currentMediaCast.value = resolved
                    }
                }
            }
        }
    }

    fun loadSeasonsForMedia(media: MediaEntity) {
        if (media.getMediaTypeEnum() != MediaType.TV_SHOW) return
        viewModelScope.launch {
            isLoadingSeasons.value = true
            try {
                val apiKey = getActiveTmdbApiKey()
                val seasons = repository.getSeasonsForTvShow(media, apiKey)
                currentTvSeasons.value = seasons
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingSeasons.value = false
            }
        }
    }

    fun openSeasonDetail(media: MediaEntity, season: SeasonItem) {
        selectedSeasonForDetail.value = season
        loadEpisodesForSeason(media, season.seasonNumber)
    }

    fun closeSeasonDetail() {
        selectedSeasonForDetail.value = null
        currentSeasonEpisodes.value = emptyList()
    }

    fun loadEpisodesForSeason(media: MediaEntity, seasonNumber: Int) {
        viewModelScope.launch {
            isLoadingEpisodes.value = true
            try {
                val apiKey = getActiveTmdbApiKey()
                val episodes = repository.getEpisodesForSeason(media, seasonNumber, apiKey)
                currentSeasonEpisodes.value = episodes
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingEpisodes.value = false
            }
        }
    }

    fun openEpisodeDetail(episode: EpisodeItem) {
        selectedEpisodeDetail.value = episode
    }

    fun closeEpisodeDetail() {
        selectedEpisodeDetail.value = null
    }

    fun setSeasonWatchStatus(media: MediaEntity, seasonNumber: Int, status: TvWatchStatus) {
        viewModelScope.launch {
            repository.setSeasonWatchStatus(media, seasonNumber, status)
            val apiKey = getActiveTmdbApiKey()
            val seasons = repository.getSeasonsForTvShow(media, apiKey)
            currentTvSeasons.value = seasons
            selectedSeasonForDetail.value?.let { current ->
                if (current.seasonNumber == seasonNumber) {
                    selectedSeasonForDetail.value = seasons.find { it.seasonNumber == seasonNumber }
                    loadEpisodesForSeason(media, seasonNumber)
                }
            }
        }
    }

    fun setEpisodeWatchStatus(media: MediaEntity, seasonNumber: Int, episodeNumber: Int, status: TvWatchStatus) {
        viewModelScope.launch {
            repository.setEpisodeWatchStatus(media, seasonNumber, episodeNumber, status)
            val apiKey = getActiveTmdbApiKey()
            val episodes = repository.getEpisodesForSeason(media, seasonNumber, apiKey)
            currentSeasonEpisodes.value = episodes
            selectedEpisodeDetail.value?.let { curr ->
                if (curr.seasonNumber == seasonNumber && curr.episodeNumber == episodeNumber) {
                    selectedEpisodeDetail.value = episodes.find { it.episodeNumber == episodeNumber }
                }
            }
            val seasons = repository.getSeasonsForTvShow(media, apiKey)
            currentTvSeasons.value = seasons
            selectedSeasonForDetail.value?.let { current ->
                if (current.seasonNumber == seasonNumber) {
                    selectedSeasonForDetail.value = seasons.find { it.seasonNumber == seasonNumber }
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
            fetchTrailers()
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

    fun toggleFavoritePerson(
        person: TmdbSearchResultItem.Person,
        detailDto: TmdbPersonDetailDto? = null
    ) {
        val resolvedName = detailDto?.name?.takeIf { it.isNotBlank() } ?: person.name
        val resolvedId = if (detailDto != null && detailDto.id > 0L) detailDto.id else person.id
        val profileUrl = detailDto?.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" }
            ?: person.profileUrl.orEmpty()
        val department = (detailDto?.knownForDepartment ?: person.department).ifBlank { "Acting" }
        val storageId = FavoritePersonEntity.buildStorageId(resolvedId, resolvedName)

        viewModelScope.launch {
            repository.toggleFavoritePerson(
                FavoritePersonEntity(
                    id = storageId,
                    personId = resolvedId,
                    name = resolvedName,
                    profileUrl = profileUrl,
                    department = department,
                    birthday = detailDto?.birthday.orEmpty(),
                    placeOfBirth = detailDto?.placeOfBirth.orEmpty()
                )
            )
        }
    }

    fun removeFavoritePerson(fav: FavoritePersonEntity) {
        viewModelScope.launch {
            repository.toggleFavoritePerson(fav)
        }
    }

    fun openFavoritePerson(fav: FavoritePersonEntity) {
        if (fav.personId > 0L) {
            selectPersonForDetail(
                TmdbSearchResultItem.Person(
                    id = fav.personId,
                    name = fav.name,
                    profileUrl = fav.profileUrl.ifBlank { null },
                    department = fav.department.ifBlank { "Acting" },
                    knownFor = emptyList(),
                    popularity = 0.0
                )
            )
        } else {
            openPersonByName(fav.name)
        }
    }

    fun setCustomWatchlist(id: String, watchlist: String?) {
        viewModelScope.launch {
            if (!watchlist.isNullOrBlank()) {
                repository.createCustomCollection(watchlist)
            }
            repository.updateCustomWatchlist(id, watchlist)
        }
    }

    fun createCustomCollection(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createCustomCollection(name, description)
        }
    }

    fun deleteCustomCollection(name: String) {
        viewModelScope.launch {
            repository.deleteCustomCollection(name)
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

    fun toggleStreamingProviders(enabled: Boolean) {
        viewModelScope.launch {
            val current = userPreferences.value
            repository.updatePreferences(
                current.copy(showStreamingProviders = enabled)
            )
        }
    }

    fun updatePreferences(prefs: UserPreferencesEntity) {
        viewModelScope.launch {
            repository.updatePreferences(prefs)
            syncTrendingFromTmdb()
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
