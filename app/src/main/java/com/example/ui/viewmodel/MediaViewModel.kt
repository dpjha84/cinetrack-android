package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ai.WatchPilotAiService
import com.example.data.billing.MonetizationManager
import com.example.data.local.AppDatabase
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.local.OnboardingPreferences
import com.example.data.local.PopularPersonEntity
import com.example.data.local.SampleMediaData
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.CastMemberItem
import com.example.data.model.ConciergePick
import com.example.data.model.ConciergeUiState
import com.example.data.model.DecadeStat
import com.example.data.model.GenreStat
import com.example.data.model.HomeCatalogSection
import com.example.data.model.MatchCandidateCard
import com.example.data.model.MatchRoomUiState
import com.example.data.model.MediaType
import com.example.data.model.MonetizationState
import com.example.data.model.MultiSearchCategory
import com.example.data.model.OttMoneySaverSummary
import com.example.data.model.RegionalDefaults
import com.example.data.model.ScannedMediaCandidate
import com.example.data.model.SnapScanUiState
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
import com.example.data.remote.firebase.MatchRoomRepository
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.data.repository.MediaRepository
import com.example.util.CsvLibraryPorter
import com.example.util.OttMoneySaverCalculator
import com.example.util.WrappedCardExporter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

data class HomeSectionPaginationState(
    val visibleLimit: Int = 24,
    val lastTmdbPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

data class HomeSectionGridUiState(
    val section: HomeCatalogSection? = null,
    val items: List<MediaEntity> = emptyList(),
    val people: List<TmdbSearchResultItem.Person> = emptyList(),
    val totalAvailableCount: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

internal fun <T> mergeAppendOnlyById(
    existingOrderedIds: MutableList<String>,
    candidates: List<T>,
    idSelector: (T) -> String
): List<T> {
    val candidateById = LinkedHashMap<String, T>(candidates.size)
    for (item in candidates) {
        val id = idSelector(item)
        if (id.isNotBlank() && !candidateById.containsKey(id)) {
            candidateById[id] = item
        }
    }
    existingOrderedIds.retainAll(candidateById.keys)
    val seen = existingOrderedIds.toHashSet()
    for (item in candidates) {
        val id = idSelector(item)
        if (id.isNotBlank() && seen.add(id)) {
            existingOrderedIds.add(id)
        }
    }
    return existingOrderedIds.mapNotNull { candidateById[it] }
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
            firestoreSyncManager,
            db.popularPersonDao()
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

    // WatchPilot v2.0 Monetization, AI & Live Match Room Services
    val monetizationManager = MonetizationManager(
        context = application,
        scope = viewModelScope,
        onSyncEntitlementToCloud = { updatedState ->
            val uid = currentUser.value?.uid
            if (!uid.isNullOrBlank()) {
                firestoreSyncManager.uploadMonetizationEntitlement(uid, updatedState)
            }
        }
    )
    val monetizationState: StateFlow<MonetizationState> = monetizationManager.state

    private val aiService = WatchPilotAiService()
    private val matchRoomRepo = MatchRoomRepository(
        firestore = firestoreSyncManager.firestore,
        auth = Firebase.auth
    )
    private var matchRoomObserverJob: kotlinx.coroutines.Job? = null

    private val _snapScanState = MutableStateFlow<SnapScanUiState>(SnapScanUiState.Idle)
    val snapScanState: StateFlow<SnapScanUiState> = _snapScanState.asStateFlow()

    private val _conciergeState = MutableStateFlow<ConciergeUiState>(ConciergeUiState.Idle)
    val conciergeState: StateFlow<ConciergeUiState> = _conciergeState.asStateFlow()

    private val _matchRoomState = MutableStateFlow<MatchRoomUiState>(MatchRoomUiState.Idle)
    val matchRoomState: StateFlow<MatchRoomUiState> = _matchRoomState.asStateFlow()
    val skippedMatchCandidateIds = MutableStateFlow<Set<String>>(emptySet())

    val isImportingCsv = MutableStateFlow(false)
    val csvImportMessage = MutableStateFlow<String?>(null)

    // Global Sheet Visibility Controls
    val showPaywallSheet = MutableStateFlow(false)
    val paywallTriggerReason = MutableStateFlow<String?>(null)
    val showSnapScannerSheet = MutableStateFlow(false)
    val showAiConciergeSheet = MutableStateFlow(false)
    val showSwipeMatchSheet = MutableStateFlow(false)
    val showOttMoneySaverSheet = MutableStateFlow(false)
    val showLegalComplianceDialog = MutableStateFlow(false)

    // New-User Onboarding & Home Personalization ("Top Picks For You")
    private val _hasCompletedOnboarding = MutableStateFlow(
        OnboardingPreferences.hasCompletedOnboarding(application)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()
    val onboardingStep = MutableStateFlow(0)
    val onboardingAuthLoading = MutableStateFlow(false)
    val onboardingAuthError = MutableStateFlow<String?>(null)
    val onboardingSyncNotice = MutableStateFlow<String?>(null)

    private val _onboardingSelectedGenres = MutableStateFlow(
        OnboardingPreferences.getSelectedGenres(application)
    )
    val onboardingSelectedGenres: StateFlow<Set<String>> = _onboardingSelectedGenres.asStateFlow()

    val onboardingPopularPeople = MutableStateFlow<List<TmdbSearchResultItem.Person>>(emptyList())
    val onboardingPersonQuery = MutableStateFlow("")
    val onboardingPersonSearchResults = MutableStateFlow<List<TmdbSearchResultItem.Person>>(emptyList())
    val isLoadingOnboardingPeople = MutableStateFlow(false)

    val onboardingPopularTitles = MutableStateFlow<List<MediaEntity>>(emptyList())
    val onboardingTitleQuery = MutableStateFlow("")
    val onboardingTitleSearchResults = MutableStateFlow<List<MediaEntity>>(emptyList())
    val isLoadingOnboardingTitles = MutableStateFlow(false)

    val pickedForYouItems = MutableStateFlow<List<MediaEntity>>(emptyList())
    val isLoadingPickedForYou = MutableStateFlow(false)
    private var pickedForYouJob: kotlinx.coroutines.Job? = null

    val allMedia: StateFlow<List<MediaEntity>> = repository.allMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val trackedMedia: StateFlow<List<MediaEntity>> = repository.trackedMedia.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
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
        SharingStarted.Eagerly,
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

    private fun filterHomeSectionList(
        items: List<MediaEntity>,
        section: HomeCatalogSection?,
        filters: SearchFilters,
        prefs: UserPreferencesEntity,
        regionCode: String
    ): List<MediaEntity> {
        val query = filters.query.trim()
        val typeFilter = filters.type
        val genreFilter = filters.genre

        return items.filter { item ->
            val matchesSection = when (section) {
                null -> true
                HomeCatalogSection.POPULAR -> item.hasFeedTag(HomeCatalogSection.POPULAR.feedTag) || item.feedTags.isBlank()
                HomeCatalogSection.NOW_PLAYING -> item.hasFeedTag(HomeCatalogSection.NOW_PLAYING.feedTag)
                HomeCatalogSection.UPCOMING -> item.hasFeedTag(HomeCatalogSection.UPCOMING.feedTag)
                HomeCatalogSection.POPULAR_PEOPLE -> false
            }

            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.cast.contains(query, ignoreCase = true) ||
                item.directorOrCreator.contains(query, ignoreCase = true) ||
                item.genres.contains(query, ignoreCase = true) ||
                item.overview.contains(query, ignoreCase = true)

            val matchesType = typeFilter == null || item.getMediaTypeEnum() == typeFilter

            val matchesGenre = genreFilter == null ||
                item.getGenreList().any { it.equals(genreFilter, ignoreCase = true) }

            val matchesLanguage = if (query.isBlank() && prefs.filterCatalogByLanguage && !prefs.languageCode.equals("all", ignoreCase = true)) {
                item.originalLanguage.equals(prefs.languageCode, ignoreCase = true)
            } else {
                true
            }

            val allowPre2023LiveSeries = section != HomeCatalogSection.UPCOMING &&
                item.getMediaTypeEnum() == MediaType.TV_SHOW &&
                item.id.startsWith("tmdb_tv_")

            val allowTbaUpcoming = section == HomeCatalogSection.UPCOMING && item.releaseYear == 0

            val isExcludedLegacy = query.isBlank() && (
                (!allowPre2023LiveSeries && !allowTbaUpcoming && item.releaseYear < 2023) ||
                    item.title.equals("inception", ignoreCase = true) ||
                    item.title.equals("fight club", ignoreCase = true) ||
                    item.title.equals("parasite", ignoreCase = true) ||
                    item.title.equals("the dark knight", ignoreCase = true) ||
                    item.title.equals("pulp fiction", ignoreCase = true) ||
                    item.title.equals("the shawshank redemption", ignoreCase = true) ||
                    item.title.contains("panchayat", ignoreCase = true) ||
                    item.title.equals("rrr", ignoreCase = true)
                )

            matchesSection && matchesQuery && matchesType && matchesGenre && matchesLanguage && !isExcludedLegacy
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
                if (it.originCountry.equals(regionCode, ignoreCase = true) ||
                    (!prefs.languageCode.equals("all", ignoreCase = true) && it.originalLanguage.equals(prefs.languageCode, ignoreCase = true))
                ) 1 else 0
            }.thenByDescending { it.releaseYear }
                .thenByDescending { it.communityRating }
        )
    }

    // Filtered Discover list respecting search, type, genre, and regional language preferences
    val filteredDiscover: StateFlow<List<MediaEntity>> = combine(
        allMedia,
        searchFilters,
        userPreferences,
        effectiveRegionCode
    ) { items, filters, prefs, regionCode ->
        filterHomeSectionList(items, HomeCatalogSection.POPULAR, filters, prefs, regionCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularHomeItems: StateFlow<List<MediaEntity>> = filteredDiscover

    val nowPlayingHomeItems: StateFlow<List<MediaEntity>> = combine(
        allMedia,
        searchFilters,
        userPreferences,
        effectiveRegionCode
    ) { items, filters, prefs, regionCode ->
        filterHomeSectionList(items, HomeCatalogSection.NOW_PLAYING, filters, prefs, regionCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingHomeItems: StateFlow<List<MediaEntity>> = combine(
        allMedia,
        searchFilters,
        userPreferences,
        effectiveRegionCode
    ) { items, filters, prefs, regionCode ->
        filterHomeSectionList(items, HomeCatalogSection.UPCOMING, filters, prefs, regionCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularPeopleHomeItems: StateFlow<List<TmdbSearchResultItem.Person>> = combine(
        repository.popularPeople,
        allMedia,
        searchFilters
    ) { entities, mediaList, filters ->
        val query = filters.query.trim()
        val typeFilter = filters.type
        val mediaById = mediaList.associateBy { it.id }
        val sourceEntities = if (entities.isEmpty()) {
            SampleMediaData.getSamplePeople().map { PopularPersonEntity.fromPerson(it, pageLoaded = 1) }
        } else {
            entities
        }
        sourceEntities
            .filter { entity ->
                val matchesType = entity.supportsMediaType(typeFilter)
                val matchesQuery = query.isBlank() ||
                    entity.name.contains(query, ignoreCase = true) ||
                    entity.department.contains(query, ignoreCase = true) ||
                    entity.knownForTitles.contains(query, ignoreCase = true)
                matchesType && matchesQuery
            }
            .map { entity ->
                val knownFor = entity.knownForIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .mapNotNull { id ->
                        mediaById[id] ?: SampleMediaData.getSamplePeople()
                            .flatMap { it.knownFor }
                            .find { it.id == id }
                    }
                TmdbSearchResultItem.Person(
                    id = entity.personId,
                    name = entity.name,
                    profileUrl = entity.profileUrl.ifBlank { null },
                    department = entity.department.ifBlank { "Acting" },
                    knownFor = knownFor,
                    popularity = entity.popularity
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isHomeCatalogRefreshing = MutableStateFlow(false)
    val activeHomeCatalogSection = MutableStateFlow<HomeCatalogSection?>(null)
    val isHomeSectionFilterLocked = MutableStateFlow(false)
    val isHomeSectionGenreLocked = MutableStateFlow(false)
    private val sectionPageStateMap = MutableStateFlow<Map<String, HomeSectionPaginationState>>(emptyMap())
    private val sectionOrderedIdsCache = java.util.concurrent.ConcurrentHashMap<String, MutableList<String>>()
    private var sectionLoadMoreJob: Job? = null

    private fun sectionStateKey(
        section: HomeCatalogSection,
        typeFilter: MediaType?,
        genreFilter: String? = selectedGenreFilter.value
    ): String = "${section.name}_${typeFilter?.name ?: "ALL"}_${genreFilter ?: "ALL"}"

    private fun sectionOrderKey(
        section: HomeCatalogSection,
        filters: SearchFilters,
        prefs: UserPreferencesEntity,
        regionCode: String
    ): String {
        val lang = if (prefs.filterCatalogByLanguage) prefs.languageCode else "all"
        return "${section.name}_${filters.type?.name ?: "ALL"}_${filters.genre ?: "ALL"}_${regionCode}_${lang}_${filters.query.trim().lowercase()}"
    }

    val activeSectionGridUiState: StateFlow<HomeSectionGridUiState> = combine(
        activeHomeCatalogSection,
        searchFilters,
        userPreferences,
        effectiveRegionCode,
        popularHomeItems,
        nowPlayingHomeItems,
        upcomingHomeItems,
        popularPeopleHomeItems,
        sectionPageStateMap
    ) { args ->
        val section = args[0] as HomeCatalogSection?
        val filters = args[1] as SearchFilters
        val prefs = args[2] as UserPreferencesEntity
        val regionCode = args[3] as String
        @Suppress("UNCHECKED_CAST")
        val popular = args[4] as List<MediaEntity>
        @Suppress("UNCHECKED_CAST")
        val nowPlaying = args[5] as List<MediaEntity>
        @Suppress("UNCHECKED_CAST")
        val upcoming = args[6] as List<MediaEntity>
        @Suppress("UNCHECKED_CAST")
        val peopleList = args[7] as List<TmdbSearchResultItem.Person>
        @Suppress("UNCHECKED_CAST")
        val pageMap = args[8] as Map<String, HomeSectionPaginationState>

        if (section == null) {
            HomeSectionGridUiState()
        } else {
            val stateKey = sectionStateKey(section, filters.type, filters.genre)
            val paging = pageMap[stateKey] ?: HomeSectionPaginationState()
            val orderKey = sectionOrderKey(section, filters, prefs, regionCode)
            if (section == HomeCatalogSection.POPULAR_PEOPLE) {
                val orderedPeople = synchronized(sectionOrderedIdsCache) {
                    val orderedIds = sectionOrderedIdsCache.getOrPut(orderKey) { mutableListOf() }
                    mergeAppendOnlyById(orderedIds, peopleList) { it.id.toString() }
                }
                HomeSectionGridUiState(
                    section = section,
                    people = orderedPeople.take(paging.visibleLimit),
                    totalAvailableCount = orderedPeople.size,
                    isLoadingMore = paging.isLoadingMore,
                    hasMore = paging.hasMore
                )
            } else {
                val fullList = when (section) {
                    HomeCatalogSection.POPULAR -> popular
                    HomeCatalogSection.NOW_PLAYING -> nowPlaying
                    HomeCatalogSection.UPCOMING -> upcoming
                    HomeCatalogSection.POPULAR_PEOPLE -> emptyList()
                }
                val orderedItems = synchronized(sectionOrderedIdsCache) {
                    val orderedIds = sectionOrderedIdsCache.getOrPut(orderKey) { mutableListOf() }
                    mergeAppendOnlyById(orderedIds, fullList) { it.id }
                }
                HomeSectionGridUiState(
                    section = section,
                    items = orderedItems.take(paging.visibleLimit),
                    totalAvailableCount = orderedItems.size,
                    isLoadingMore = paging.isLoadingMore,
                    hasMore = paging.hasMore
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSectionGridUiState())

    // OTT Subscription Money-Saver Radar Summary
    val ottMoneySaverSummary: StateFlow<OttMoneySaverSummary> = combine(
        trackedMedia,
        effectiveRegionCode,
        monetizationState
    ) { items, regionCode, monState ->
        OttMoneySaverCalculator.calculateAudit(
            trackedMedia = items,
            regionCode = regionCode,
            subscribedCanonicalNames = monState.subscribedProviders
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OttMoneySaverSummary())

    private data class LibraryFilterInputs(
        val tab: LibraryTab,
        val sort: SortOption,
        val collection: String?
    )

    private val libraryFilterInputs = combine(
        selectedLibraryTab,
        librarySortOption,
        selectedCollectionFilter
    ) { tab, sort, collection ->
        LibraryFilterInputs(tab, sort, collection)
    }

    // Filtered Library list (including Pro "On My Apps" OTT filter)
    val filteredLibrary: StateFlow<List<MediaEntity>> = combine(
        trackedMedia,
        libraryFilterInputs,
        monetizationState,
        effectiveRegionCode
    ) { items, inputs, monState, regionCode ->
        val filtered = items.filter { item ->
            val matchesTab = when (inputs.tab) {
                LibraryTab.WATCHING -> item.getWatchStatusEnum() == WatchStatus.WATCHING
                LibraryTab.PLAN_TO_WATCH -> item.getWatchStatusEnum() == WatchStatus.PLAN_TO_WATCH
                LibraryTab.COMPLETED -> item.getWatchStatusEnum() == WatchStatus.COMPLETED
                LibraryTab.FAVORITES -> item.isFavorite
                LibraryTab.ALL_TRACKED -> true
            }

            val matchesCollection = inputs.collection == null || item.customWatchlist == inputs.collection

            val matchesMyApps = if (monState.streamOnMyAppsOnly && monState.subscribedProviders.isNotEmpty()) {
                OttMoneySaverCalculator.matchesAnySubscribedApp(
                    media = item,
                    regionCode = regionCode,
                    subscribedCanonicalNames = monState.subscribedProviders
                )
            } else {
                true
            }

            matchesTab && matchesCollection && matchesMyApps
        }

        when (inputs.sort) {
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
        var watchlistCount = 0
        var totalEpisodes = 0
        var ratingSum = 0f
        var ratedCount = 0
        val genreCountMap = mutableMapOf<String, Int>()
        val decadeCountMap = mutableMapOf<String, Int>()
        val langCountMap = mutableMapOf<String, Int>()

        items.forEach { item ->
            val isMovie = item.getMediaTypeEnum() == MediaType.MOVIE
            val status = item.getWatchStatusEnum()

            if (status == WatchStatus.PLAN_TO_WATCH) {
                watchlistCount++
            }

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
                val decadeLabel = when {
                    item.releaseYear >= 2020 -> "2020s"
                    item.releaseYear >= 2010 -> "2010s"
                    item.releaseYear >= 2000 -> "2000s"
                    item.releaseYear >= 1990 -> "1990s"
                    else -> "Classics"
                }
                decadeCountMap[decadeLabel] = (decadeCountMap[decadeLabel] ?: 0) + 1
                val lang = item.originalLanguage.uppercase().ifBlank { "EN" }
                langCountMap[lang] = (langCountMap[lang] ?: 0) + 1
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

        val decadeBreakdown = decadeCountMap.entries
            .sortedByDescending { it.value }
            .map {
                val pct = if (totalLoggedMedia > 0) (it.value.toFloat() / totalLoggedMedia) else 0f
                DecadeStat(it.key, it.value, pct)
            }

        val totalTrackedNonNone = completedMovies + completedShows + activeShows + watchlistCount
        val completionRatePercent = if (totalTrackedNonNone > 0) {
            (((completedMovies + completedShows).toFloat() / totalTrackedNonNone.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val topLangCode = langCountMap.maxByOrNull { it.value }?.key ?: "EN"
        val topOriginLanguage = when (topLangCode) {
            "EN" -> "English"
            "HI" -> "Hindi"
            "JA" -> "Japanese"
            "KO" -> "Korean"
            "ES" -> "Spanish"
            "FR" -> "French"
            "TA" -> "Tamil"
            "TE" -> "Telugu"
            else -> topLangCode
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
            watchlistCount = watchlistCount,
            totalEpisodesLogged = totalEpisodes,
            averageUserRating = avgRating,
            ratedItemsCount = ratedCount,
            topGenres = topGenres,
            decadeBreakdown = decadeBreakdown,
            completionRatePercent = completionRatePercent,
            topOriginLanguage = topOriginLanguage,
            cinephileRank = rank,
            cinephileBadge = badge
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewingStats())

    init {
        FirebaseAuthHelper.initialize(viewModelScope)

        viewModelScope.launch {
            repository.seedIfEmpty()
            // Only auto-complete for legacy pre-onboarding local upgrades when signed out
            if (!_hasCompletedOnboarding.value &&
                onboardingStep.value == 0 &&
                Firebase.auth.currentUser == null &&
                repository.hasAnyUserTrackedData()
            ) {
                completeOnboarding()
            }
            currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .filter { !it.isNullOrBlank() }
                .collect {
                    syncWithCloud(force = false)
                }
        }

        if (!_hasCompletedOnboarding.value) {
            loadOnboardingCatalog()
        }

        // Live onboarding people search debounce
        viewModelScope.launch {
            onboardingPersonQuery
                .debounce(350)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { query ->
                    searchOnboardingPeople(query)
                }
        }

        // Live onboarding title search debounce
        viewModelScope.launch {
            onboardingTitleQuery
                .debounce(350)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { query ->
                    searchOnboardingTitles(query)
                }
        }

        // Keep "Top Picks For You" on Home synchronized with taste genres, tracked titles & favorite people
        viewModelScope.launch {
            combine(
                hasCompletedOnboarding,
                trackedMedia,
                favoritePeople,
                onboardingSelectedGenres,
                effectiveRegionCode
            ) { completed, tracked, people, genres, region ->
                val tasteFingerprint = buildString {
                    append(completed)
                    append('|')
                    append(region)
                    append('|')
                    append(genres.sorted().joinToString(","))
                    append('|')
                    append(people.joinToString(",") { it.id })
                    append('|')
                    append(tracked.take(15).joinToString(",") { "${it.id}:${it.status}:${it.isFavorite}:${it.userRating}" })
                }
                completed to tasteFingerprint
            }
                .distinctUntilChanged()
                .debounce(350)
                .collect { (completed, _) ->
                    if (completed) {
                        refreshPickedForYou()
                    }
                }
        }

        // Listen to region changes to refresh regional popular, now playing, upcoming, trending and trailers
        viewModelScope.launch {
            effectiveRegionCode.collect {
                synchronized(sectionOrderedIdsCache) {
                    sectionOrderedIdsCache.clear()
                }
                sectionPageStateMap.value = emptyMap()
                fetchPopularMedia()
                fetchTrendingNow()
                fetchTrailers()
            }
        }

        // Prefetch page 1 per media type when switching top filter chips so Movies/TV tabs are rich for all 3 lists
        viewModelScope.launch {
            selectedTypeFilter.collect { typeFilter ->
                val key = getActiveTmdbApiKey()
                if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                    val lang = userPreferences.value.languageCode
                    val reg = effectiveRegionCode.value
                    listOf(
                        HomeCatalogSection.POPULAR,
                        HomeCatalogSection.NOW_PLAYING,
                        HomeCatalogSection.UPCOMING
                    ).forEach { section ->
                        val currentList = when (section) {
                            HomeCatalogSection.POPULAR -> popularHomeItems.value
                            HomeCatalogSection.NOW_PLAYING -> nowPlayingHomeItems.value
                            HomeCatalogSection.UPCOMING -> upcomingHomeItems.value
                            HomeCatalogSection.POPULAR_PEOPLE -> emptyList()
                        }
                        if (currentList.size < 12) {
                            launch {
                                repository.fetchHomeCatalogPage(section, key, typeFilter, lang, reg, page = 1)
                            }
                        }
                    }
                    if (popularPeopleHomeItems.value.size < 12) {
                        launch {
                            repository.fetchPopularPeoplePage(key, lang, page = 1)
                            if (popularPeopleHomeItems.value.size < 12) {
                                repository.fetchPopularPeoplePage(key, lang, page = 2)
                            }
                        }
                    }
                }
            }
        }

        // Reset section pagination availability whenever genre filter changes so clearing a narrow genre re-enables infinite scroll
        viewModelScope.launch {
            selectedGenreFilter.collect {
                if (sectionPageStateMap.value.isNotEmpty()) {
                    sectionPageStateMap.value = sectionPageStateMap.value.mapValues { (_, state) ->
                        state.copy(visibleLimit = 24, hasMore = true)
                    }
                }
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
            isHomeCatalogRefreshing.value = true
            try {
                val key = getActiveTmdbApiKey()
                val lang = userPreferences.value.languageCode
                val reg = effectiveRegionCode.value
                coroutineScope {
                    if (key.isNotBlank() && key != "YOUR_TMDB_API_KEY") {
                        launch {
                            repository.fetchPopularMediaFromTmdb(
                                apiKey = key,
                                language = lang,
                                region = reg
                            )
                        }
                    }
                    launch {
                        repository.fetchPopularPeoplePage(
                            apiKey = key,
                            language = lang,
                            page = 1
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isHomeCatalogRefreshing.value = false
            }
        }
    }

    fun openHomeCatalogSection(section: HomeCatalogSection) {
        sectionLoadMoreJob?.cancel()
        val typeFilter = selectedTypeFilter.value
        val genreFilter = selectedGenreFilter.value
        isHomeSectionFilterLocked.value = (typeFilter != null)
        isHomeSectionGenreLocked.value = (genreFilter != null)
        activeHomeCatalogSection.value = section
        val stateKey = sectionStateKey(section, typeFilter, genreFilter)
        val existing = sectionPageStateMap.value[stateKey]
        if (existing == null) {
            sectionPageStateMap.value = sectionPageStateMap.value + (stateKey to HomeSectionPaginationState())
        } else {
            sectionPageStateMap.value = sectionPageStateMap.value + (
                stateKey to existing.copy(
                    visibleLimit = maxOf(24, existing.visibleLimit),
                    isLoadingMore = false,
                    hasMore = true
                )
            )
        }
        val currentCount = when (section) {
            HomeCatalogSection.POPULAR -> popularHomeItems.value.size
            HomeCatalogSection.NOW_PLAYING -> nowPlayingHomeItems.value.size
            HomeCatalogSection.UPCOMING -> upcomingHomeItems.value.size
            HomeCatalogSection.POPULAR_PEOPLE -> popularPeopleHomeItems.value.size
        }
        if (currentCount < 18) {
            loadMoreForHomeSection(section)
        }
    }

    fun closeHomeCatalogSection() {
        sectionLoadMoreJob?.cancel()
        sectionLoadMoreJob = null
        activeHomeCatalogSection.value = null
        isHomeSectionFilterLocked.value = false
        isHomeSectionGenreLocked.value = false
        synchronized(sectionOrderedIdsCache) {
            sectionOrderedIdsCache.clear()
        }
        if (sectionPageStateMap.value.isNotEmpty()) {
            sectionPageStateMap.value = sectionPageStateMap.value.mapValues { (_, state) ->
                state.copy(visibleLimit = 24, isLoadingMore = false)
            }
        }
    }

    fun loadMoreForHomeSection(section: HomeCatalogSection) {
        val typeFilter = selectedTypeFilter.value
        val genreFilter = selectedGenreFilter.value
        val stateKey = sectionStateKey(section, typeFilter, genreFilter)
        val current = sectionPageStateMap.value[stateKey] ?: HomeSectionPaginationState()
        if (current.isLoadingMore || !current.hasMore) return

        val initialPoolSize = when (section) {
            HomeCatalogSection.POPULAR -> popularHomeItems.value.size
            HomeCatalogSection.NOW_PLAYING -> nowPlayingHomeItems.value.size
            HomeCatalogSection.UPCOMING -> upcomingHomeItems.value.size
            HomeCatalogSection.POPULAR_PEOPLE -> popularPeopleHomeItems.value.size
        }

        // Fast-path: expand already-cached DB/in-memory items immediately without flashing a spinner
        if (initialPoolSize > current.visibleLimit) {
            val expandedLimit = minOf(current.visibleLimit + 24, initialPoolSize)
            sectionPageStateMap.value = sectionPageStateMap.value + (
                stateKey to current.copy(
                    visibleLimit = expandedLimit,
                    isLoadingMore = false,
                    hasMore = expandedLimit < initialPoolSize || current.lastTmdbPage < 15
                )
            )
            // Quietly prefetch next TMDB page in background only if buffer is getting low
            if (initialPoolSize - expandedLimit <= 12 && current.lastTmdbPage < 15) {
                viewModelScope.launch {
                    val tmdbKey = getActiveTmdbApiKey()
                    val lang = userPreferences.value.languageCode
                    val region = effectiveRegionCode.value
                    withTimeoutOrNull(10_000L) {
                        if (section == HomeCatalogSection.POPULAR_PEOPLE) {
                            repository.fetchPopularPeoplePage(tmdbKey, lang, page = current.lastTmdbPage + 1)
                        } else {
                            repository.fetchHomeCatalogPage(section, tmdbKey, typeFilter, lang, region, page = current.lastTmdbPage + 1)
                        }
                    }
                    val latest = sectionPageStateMap.value[stateKey]
                    if (latest != null) {
                        sectionPageStateMap.value = sectionPageStateMap.value + (
                            stateKey to latest.copy(lastTmdbPage = current.lastTmdbPage + 1)
                        )
                    }
                }
            }
            return
        }

        sectionPageStateMap.value = sectionPageStateMap.value + (stateKey to current.copy(isLoadingMore = true))

        sectionLoadMoreJob = viewModelScope.launch {
            var updatedState: HomeSectionPaginationState? = null
            try {
                val tmdbKey = getActiveTmdbApiKey()
                val lang = userPreferences.value.languageCode
                val region = effectiveRegionCode.value
                val newVisibleLimit = current.visibleLimit + 24
                var nextPage = current.lastTmdbPage
                var newlyFetchedCount = 0

                if (section == HomeCatalogSection.POPULAR_PEOPLE) {
                    nextPage = current.lastTmdbPage + 1
                    val fetchedPeople = withTimeoutOrNull(10_000L) {
                        repository.fetchPopularPeoplePage(
                            apiKey = tmdbKey,
                            language = lang,
                            page = nextPage
                        )
                    } ?: emptyList()
                    newlyFetchedCount = fetchedPeople.size
                    if (fetchedPeople.isNotEmpty() && (typeFilter != null && fetchedPeople.count { p ->
                            p.knownFor.any { it.getMediaTypeEnum() == typeFilter }
                        } < 6) && nextPage < 15
                    ) {
                        nextPage += 1
                        newlyFetchedCount += withTimeoutOrNull(8_000L) {
                            repository.fetchPopularPeoplePage(
                                apiKey = tmdbKey,
                                language = lang,
                                page = nextPage
                            )
                        }?.size ?: 0
                    }
                    if (newlyFetchedCount == 0) {
                        val dbMore = repository.getPagedPopularPeopleFromDb(
                            mediaType = typeFilter,
                            limit = 24,
                            offset = initialPoolSize
                        )
                        newlyFetchedCount = dbMore.size
                    }
                } else {
                    val beforeIds = when (section) {
                        HomeCatalogSection.POPULAR -> popularHomeItems.value.mapTo(HashSet()) { it.id }
                        HomeCatalogSection.NOW_PLAYING -> nowPlayingHomeItems.value.mapTo(HashSet()) { it.id }
                        HomeCatalogSection.UPCOMING -> upcomingHomeItems.value.mapTo(HashSet()) { it.id }
                        HomeCatalogSection.POPULAR_PEOPLE -> emptySet()
                    }
                    nextPage = current.lastTmdbPage + 1
                    val fetched = withTimeoutOrNull(10_000L) {
                        repository.fetchHomeCatalogPage(
                            section = section,
                            apiKey = tmdbKey,
                            mediaType = typeFilter,
                            language = lang,
                            region = region,
                            page = nextPage
                        )
                    } ?: emptyList()
                    newlyFetchedCount = fetched.size
                    var addedUnseenForFilter = fetched.count { item ->
                        item.id !in beforeIds &&
                            (typeFilter == null || item.getMediaTypeEnum() == typeFilter) &&
                            (genreFilter == null || item.getGenreList().any { g -> g.equals(genreFilter, ignoreCase = true) })
                    }

                    // If page yielded results but none matched current filter or all were duplicates, pull 1 more page
                    if (fetched.isNotEmpty() && addedUnseenForFilter == 0 && nextPage < 15) {
                        nextPage += 1
                        val extra = withTimeoutOrNull(8_000L) {
                            repository.fetchHomeCatalogPage(
                                section = section,
                                apiKey = tmdbKey,
                                mediaType = typeFilter,
                                language = lang,
                                region = region,
                                page = nextPage
                            )
                        } ?: emptyList()
                        newlyFetchedCount += extra.size
                        addedUnseenForFilter += extra.count { item ->
                            item.id !in beforeIds &&
                                (typeFilter == null || item.getMediaTypeEnum() == typeFilter) &&
                                (genreFilter == null || item.getGenreList().any { g -> g.equals(genreFilter, ignoreCase = true) })
                        }
                    }

                    if (newlyFetchedCount == 0) {
                        val dbMore = repository.getPagedHomeCatalogFromDb(
                            section = section,
                            mediaType = typeFilter,
                            genreFilter = genreFilter,
                            languageFilter = if (userPreferences.value.filterCatalogByLanguage) lang else null,
                            limit = 24,
                            offset = initialPoolSize
                        )
                        if (dbMore.isNotEmpty() && section == HomeCatalogSection.POPULAR) {
                            repository.upsertWithFeedTag(dbMore, section.feedTag)
                            newlyFetchedCount = dbMore.size
                        }
                    }
                }

                // Allow Room Flow one quick cycle to emit newly inserted rows before hiding the spinner
                if (newlyFetchedCount > 0) {
                    delay(80L)
                }

                val updatedPoolSize = when (section) {
                    HomeCatalogSection.POPULAR -> popularHomeItems.value.size
                    HomeCatalogSection.NOW_PLAYING -> nowPlayingHomeItems.value.size
                    HomeCatalogSection.UPCOMING -> upcomingHomeItems.value.size
                    HomeCatalogSection.POPULAR_PEOPLE -> popularPeopleHomeItems.value.size
                }

                val effectiveVisibleLimit = if (updatedPoolSize > 0) {
                    maxOf(current.visibleLimit, minOf(newVisibleLimit, maxOf(24, updatedPoolSize)))
                } else {
                    newVisibleLimit
                }
                val reachedPageCap = nextPage >= 15
                val canStillLoadMore = !reachedPageCap && (
                    updatedPoolSize > effectiveVisibleLimit ||
                        (newlyFetchedCount > 0 && updatedPoolSize > initialPoolSize)
                    )
                updatedState = HomeSectionPaginationState(
                    visibleLimit = effectiveVisibleLimit,
                    lastTmdbPage = nextPage,
                    isLoadingMore = false,
                    hasMore = canStillLoadMore
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val finalState = updatedState ?: (sectionPageStateMap.value[stateKey] ?: current).copy(isLoadingMore = false)
                sectionPageStateMap.value = sectionPageStateMap.value + (stateKey to finalState.copy(isLoadingMore = false))
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
        return ""
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
                val cloudEntitlement = repository.syncWithCloud(uid)
                if (cloudEntitlement != null) {
                    monetizationManager.mergeCloudEntitlement(
                        cloudIsPro = cloudEntitlement.isPro,
                        cloudPlanType = cloudEntitlement.planType,
                        cloudProvidersCsv = cloudEntitlement.subscribedProviders,
                        cloudStreamOnMyAppsOnly = cloudEntitlement.streamOnMyAppsOnly
                    )
                }
                firestoreSyncManager.uploadMonetizationEntitlement(uid, monetizationState.value)
                syncStatusMessage.value = "Synced with cloud successfully"
                if (!_hasCompletedOnboarding.value && repository.hasAnyUserTrackedData()) {
                    onboardingSyncNotice.value = "Cloud synced — review or add picks"
                }
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

    // =========================================================================
    // New-User Onboarding & Home Personalization Actions
    // =========================================================================

    fun loadOnboardingCatalog() {
        viewModelScope.launch {
            val key = getActiveTmdbApiKey()
            val lang = userPreferences.value.languageCode
            val reg = effectiveRegionCode.value
            val peopleJob = async {
                isLoadingOnboardingPeople.value = true
                try {
                    onboardingPopularPeople.value = repository.fetchPopularPeople(key, lang)
                } finally {
                    isLoadingOnboardingPeople.value = false
                }
            }
            val titlesJob = async {
                isLoadingOnboardingTitles.value = true
                try {
                    onboardingPopularTitles.value = repository.fetchOnboardingPopularTitles(key, lang, reg)
                } finally {
                    isLoadingOnboardingTitles.value = false
                }
            }
            peopleJob.await()
            titlesJob.await()
        }
    }

    fun signInWithGoogleDuringOnboarding(activity: Activity) {
        if (onboardingAuthLoading.value) return
        onboardingAuthLoading.value = true
        onboardingAuthError.value = null
        onboardingSyncNotice.value = null
        if (onboardingPopularPeople.value.isEmpty() || onboardingPopularTitles.value.isEmpty()) {
            loadOnboardingCatalog()
        }
        FirebaseAuthHelper.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                viewModelScope.launch {
                    try {
                        signOutJob?.join()
                        val cloudEntitlement = repository.syncWithCloud(user.uid)
                        if (cloudEntitlement != null) {
                            monetizationManager.mergeCloudEntitlement(
                                cloudIsPro = cloudEntitlement.isPro,
                                cloudPlanType = cloudEntitlement.planType,
                                cloudProvidersCsv = cloudEntitlement.subscribedProviders,
                                cloudStreamOnMyAppsOnly = cloudEntitlement.streamOnMyAppsOnly
                            )
                        }
                        firestoreSyncManager.uploadMonetizationEntitlement(user.uid, monetizationState.value)
                        val hasExistingData = repository.hasAnyUserTrackedData()
                        onboardingSyncNotice.value = if (hasExistingData) {
                            "Cloud restored — review or add picks"
                        } else {
                            "Sync active — pick your favorites"
                        }
                        onboardingStep.value = 1
                    } catch (e: Exception) {
                        onboardingAuthError.value = e.localizedMessage ?: "Sync delayed — continuing setup"
                        onboardingStep.value = 1
                    } finally {
                        onboardingAuthLoading.value = false
                    }
                }
            },
            onError = { err ->
                onboardingAuthLoading.value = false
                onboardingAuthError.value = err
            },
            onCancelled = {
                onboardingAuthLoading.value = false
            }
        )
    }

    fun continueAsGuestDuringOnboarding() {
        continueAsGuest()
        onboardingAuthError.value = null
        onboardingSyncNotice.value = null
        if (onboardingPopularPeople.value.isEmpty() || onboardingPopularTitles.value.isEmpty()) {
            loadOnboardingCatalog()
        }
        onboardingStep.value = 1
    }

    fun advanceFromOnboardingWelcome() {
        onboardingAuthError.value = null
        if (onboardingPopularPeople.value.isEmpty() || onboardingPopularTitles.value.isEmpty()) {
            loadOnboardingCatalog()
        }
        onboardingStep.value = 1
    }

    fun setOnboardingStep(step: Int) {
        onboardingStep.value = step.coerceIn(0, 2)
    }

    fun dismissOnboardingSyncNotice() {
        onboardingSyncNotice.value = null
    }

    fun toggleOnboardingGenre(genre: String) {
        val current = _onboardingSelectedGenres.value.toMutableSet()
        if (!current.add(genre)) {
            current.remove(genre)
        }
        _onboardingSelectedGenres.value = current
        OnboardingPreferences.setSelectedGenres(getApplication(), current)
    }

    fun updateOnboardingPersonQuery(query: String) {
        onboardingPersonQuery.value = query
        if (query.isBlank()) {
            onboardingPersonSearchResults.value = emptyList()
        }
    }

    private fun searchOnboardingPeople(query: String) {
        if (query.isBlank()) {
            onboardingPersonSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            isLoadingOnboardingPeople.value = true
            try {
                onboardingPersonSearchResults.value = repository.searchPeopleTmdb(
                    query = query,
                    apiKey = getActiveTmdbApiKey(),
                    language = userPreferences.value.languageCode
                )
            } finally {
                isLoadingOnboardingPeople.value = false
            }
        }
    }

    fun toggleOnboardingPerson(person: TmdbSearchResultItem.Person) {
        toggleFavoritePerson(person, null)
    }

    fun updateOnboardingTitleQuery(query: String) {
        onboardingTitleQuery.value = query
        if (query.isBlank()) {
            onboardingTitleSearchResults.value = emptyList()
        }
    }

    private fun searchOnboardingTitles(query: String) {
        if (query.isBlank()) {
            onboardingTitleSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            isLoadingOnboardingTitles.value = true
            try {
                onboardingTitleSearchResults.value = repository.searchTmdb(
                    query = query,
                    apiKey = getActiveTmdbApiKey(),
                    language = userPreferences.value.languageCode,
                    region = null
                )
            } finally {
                isLoadingOnboardingTitles.value = false
            }
        }
    }

    fun toggleOnboardingTitleStatus(media: MediaEntity, targetStatus: WatchStatus) {
        viewModelScope.launch {
            repository.ensureMediaCached(media)
            val currentStatus = repository.getMediaByIdDirect(media.id)?.getWatchStatusEnum() ?: WatchStatus.NONE
            val nextStatus = if (currentStatus == targetStatus) WatchStatus.NONE else targetStatus
            repository.updateStatus(media.id, nextStatus, getActiveTmdbApiKey())
        }
    }

    fun toggleOnboardingTitleFavorite(media: MediaEntity) {
        viewModelScope.launch {
            repository.ensureMediaCached(media)
            val currentFav = repository.getMediaByIdDirect(media.id)?.isFavorite ?: false
            repository.toggleFavorite(media.id, currentFav)
        }
    }

    fun reopenTastePersonalization() {
        onboardingAuthError.value = null
        onboardingSyncNotice.value = null
        onboardingStep.value = if (currentUser.value != null) 1 else 0
        OnboardingPreferences.setCompletedOnboarding(getApplication(), false)
        _hasCompletedOnboarding.value = false
        if (onboardingPopularPeople.value.isEmpty() || onboardingPopularTitles.value.isEmpty()) {
            loadOnboardingCatalog()
        }
    }

    fun completeOnboarding() {
        OnboardingPreferences.setCompletedOnboarding(getApplication(), true)
        _hasCompletedOnboarding.value = true
        val currentTracked = trackedMedia.value.ifEmpty {
            allMedia.value.filter {
                it.getWatchStatusEnum() != WatchStatus.NONE || it.isFavorite || it.userRating > 0f
            }
        }
        val currentPeople = favoritePeople.value
        val currentGenres = _onboardingSelectedGenres.value
        val region = effectiveRegionCode.value
        if (currentTracked.isNotEmpty() || currentPeople.isNotEmpty() || currentGenres.isNotEmpty()) {
            viewModelScope.launch {
                val instant = repository.computeInstantRecommendations(
                    favoritePeople = currentPeople,
                    trackedMedia = currentTracked,
                    region = region,
                    preferredGenres = currentGenres
                )
                if (instant.isNotEmpty()) {
                    pickedForYouItems.value = instant
                }
                refreshPickedForYou()
            }
        } else {
            pickedForYouItems.value = emptyList()
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun refreshPickedForYou() {
        val currentTracked = trackedMedia.value
        val currentPeople = favoritePeople.value
        val currentGenres = _onboardingSelectedGenres.value
        if (currentTracked.isEmpty() && currentPeople.isEmpty() && currentGenres.isEmpty()) {
            pickedForYouItems.value = emptyList()
            return
        }
        pickedForYouJob?.cancel()
        pickedForYouJob = viewModelScope.launch {
            isLoadingPickedForYou.value = true
            try {
                if (pickedForYouItems.value.isEmpty()) {
                    val instant = repository.computeInstantRecommendations(
                        favoritePeople = currentPeople,
                        trackedMedia = currentTracked,
                        region = effectiveRegionCode.value,
                        preferredGenres = currentGenres
                    )
                    if (instant.isNotEmpty()) {
                        pickedForYouItems.value = instant
                    }
                }
                val items = repository.fetchPersonalizedRecommendations(
                    favoritePeople = currentPeople,
                    trackedMedia = currentTracked,
                    apiKey = getActiveTmdbApiKey(),
                    region = effectiveRegionCode.value,
                    language = userPreferences.value.languageCode,
                    preferredGenres = currentGenres
                )
                pickedForYouItems.value = items
            } finally {
                isLoadingPickedForYou.value = false
            }
        }
    }

    fun signOut(context: Context) {
        syncJob?.cancel()
        matchRoomObserverJob?.cancel()
        _matchRoomState.value = MatchRoomUiState.Idle
        activeSyncUid = null
        isSyncingCloud.value = false
        selectedCollectionFilter.value = null
        selectedMediaId.value = null
        monetizationManager.resetOnSignOut()
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

    private fun findKnownMedia(id: String): MediaEntity? {
        return allMedia.value.find { it.id == id }
            ?: popularHomeItems.value.find { it.id == id }
            ?: nowPlayingHomeItems.value.find { it.id == id }
            ?: upcomingHomeItems.value.find { it.id == id }
            ?: trendingItems.value.find { it.id == id }
            ?: currentTopRatedList.value.find { it.id == id }
            ?: pickedForYouItems.value.find { it.id == id }
            ?: onboardingPopularTitles.value.find { it.id == id }
            ?: onboardingTitleSearchResults.value.find { it.id == id }
    }

    private suspend fun ensureMediaInDb(id: String) {
        if (repository.getMediaByIdDirect(id) == null) {
            findKnownMedia(id)?.let { repository.ensureMediaCached(it) }
        }
    }

    private suspend fun refreshActiveTvSeasonsIfSelected(id: String) {
        if (selectedMediaId.value != id) return
        val media = repository.getMediaByIdDirect(id) ?: findKnownMedia(id) ?: return
        if (media.getMediaTypeEnum() != MediaType.TV_SHOW) return
        val apiKey = getActiveTmdbApiKey()
        val seasons = repository.getSeasonsForTvShow(media, apiKey)
        if (selectedMediaId.value == id) {
            currentTvSeasons.value = seasons
            selectedSeasonForDetail.value?.let { openSeason ->
                selectedSeasonForDetail.value = seasons.find { it.seasonNumber == openSeason.seasonNumber }
                currentSeasonEpisodes.value = repository.getEpisodesForSeason(media, openSeason.seasonNumber, apiKey)
            }
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
            val media = findKnownMedia(id)
            val region = effectiveRegionCode.value
            val initialNames = media?.getCastList().orEmpty()
            if (media != null) {
                viewModelScope.launch {
                    repository.ensureMediaCached(media)
                }
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
                    if (media != null) {
                        repository.ensureMediaCached(media)
                    }
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
                        (repository.getMediaByIdDirect(id) ?: findKnownMedia(id))?.let { updated ->
                            if (selectedMediaId.value == id && updated.getMediaTypeEnum() == MediaType.TV_SHOW) {
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
            ensureMediaInDb(id)
            repository.updateStatus(id, status, getActiveTmdbApiKey())
            refreshActiveTvSeasonsIfSelected(id)
        }
    }

    fun incrementEpisode(id: String) {
        viewModelScope.launch {
            ensureMediaInDb(id)
            repository.incrementEpisode(id, getActiveTmdbApiKey())
            refreshActiveTvSeasonsIfSelected(id)
        }
    }

    fun setWatchedEpisodes(id: String, episodes: Int) {
        viewModelScope.launch {
            ensureMediaInDb(id)
            repository.updateWatchedEpisodes(id, episodes, getActiveTmdbApiKey())
            refreshActiveTvSeasonsIfSelected(id)
        }
    }

    fun updateRatingAndReview(id: String, rating: Float, review: String) {
        viewModelScope.launch {
            ensureMediaInDb(id)
            repository.updateRatingAndReview(id, rating, review)
        }
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch {
            ensureMediaInDb(id)
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
        val clean = name.trim()
        if (clean.isBlank()) return
        val existingList = customCollections.value
        val starterDefaults = setOf("Weekend Binge", "Mind-Bending Sci-Fi", "Must-Watch Classics", "Date Night", "Award Winners")
        val userCreatedCount = existingList.count { it !in starterDefaults }
        if (!BuildConfig.IS_V1_EDITION &&
            !monetizationState.value.isPro &&
            !existingList.any { it.equals(clean, ignoreCase = true) } &&
            userCreatedCount >= MonetizationState.FREE_CUSTOM_COLLECTIONS_LIMIT
        ) {
            openPaywall("Free tier includes ${MonetizationState.FREE_CUSTOM_COLLECTIONS_LIMIT} custom collections. Upgrade to WatchPilot Pro for unlimited collections & CSV/JSON backup!")
            return
        }
        viewModelScope.launch {
            repository.createCustomCollection(clean, description)
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

    // =========================================================================
    // WatchPilot v2.0: Paywall, Billing, OTT Money-Saver, AI & Live Match Rooms
    // =========================================================================

    fun openPaywall(reason: String? = null) {
        if (BuildConfig.IS_V1_EDITION) return
        paywallTriggerReason.value = reason
        showPaywallSheet.value = true
    }

    fun dismissPaywall() {
        showPaywallSheet.value = false
        paywallTriggerReason.value = null
        monetizationManager.clearBillingMessage()
    }

    fun openSnapScanner() {
        if (BuildConfig.IS_V1_EDITION) return
        showSnapScannerSheet.value = true
    }

    fun closeSnapScanner() {
        showSnapScannerSheet.value = false
    }

    fun openAiConcierge() {
        if (BuildConfig.IS_V1_EDITION) return
        showAiConciergeSheet.value = true
    }

    fun closeAiConcierge() {
        showAiConciergeSheet.value = false
    }

    fun openSwipeMatch() {
        if (BuildConfig.IS_V1_EDITION) return
        showSwipeMatchSheet.value = true
    }

    fun closeSwipeMatch() {
        showSwipeMatchSheet.value = false
    }

    fun reportMatchRoomAuthError(message: String) {
        _matchRoomState.value = MatchRoomUiState.Error(message)
    }

    fun openOttMoneySaver() {
        if (BuildConfig.IS_V1_EDITION) return
        showOttMoneySaverSheet.value = true
        refreshOttAuditProviders()
    }

    fun closeOttMoneySaver() {
        showOttMoneySaverSheet.value = false
    }

    fun openLegalCompliance() {
        showLegalComplianceDialog.value = true
    }

    fun closeLegalCompliance() {
        showLegalComplianceDialog.value = false
    }

    fun purchasePro(activity: Activity, productId: String) {
        monetizationManager.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases() {
        monetizationManager.restorePurchases()
    }

    fun activateSandboxPro() {
        monetizationManager.activateSandboxPro()
    }

    fun deactivateSandboxPro() {
        monetizationManager.deactivateSandboxPro()
    }

    fun toggleSubscribedProvider(providerName: String) {
        monetizationManager.toggleSubscribedProvider(providerName)
        refreshOttAuditProviders()
    }

    fun setStreamOnMyAppsOnly(enabled: Boolean) {
        if (enabled && !monetizationState.value.isPro) {
            openPaywall("Filter your entire Library to 'On My Apps' with WatchPilot Pro.")
            return
        }
        if (enabled && monetizationState.value.subscribedProviders.isEmpty()) {
            openOttMoneySaver()
            return
        }
        monetizationManager.setStreamOnMyAppsOnly(enabled)
        if (enabled) {
            refreshOttAuditProviders()
        }
    }

    fun refreshOttAuditProviders() {
        viewModelScope.launch {
            val key = getActiveTmdbApiKey()
            val region = effectiveRegionCode.value
            repository.refreshWatchlistProvidersForAudit(region, key)
        }
    }

    fun scanScreenshotUri(context: Context, uri: Uri) {
        if (!monetizationState.value.canUseSnapScan) {
            showSnapScannerSheet.value = false
            openPaywall("You've used all ${MonetizationState.FREE_SNAP_SCANS_LIMIT} free AI Screenshot Scans! Unlock unlimited Reels/TikTok/Top-10 vision imports with WatchPilot Pro.")
            return
        }

        viewModelScope.launch {
            _snapScanState.value = SnapScanUiState.Scanning
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            val w = info.size.width
                            val h = info.size.height
                            val longest = maxOf(w, h)
                            if (longest > 1280) {
                                val ratio = 1280f / longest.toFloat()
                                decoder.setTargetSize(
                                    (w * ratio).toInt().coerceAtLeast(1),
                                    (h * ratio).toInt().coerceAtLeast(1)
                                )
                            }
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, bounds)
                        }
                        var sampleSize = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1280) {
                            sampleSize *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, decodeOpts)
                        }
                    }
                } ?: run {
                    _snapScanState.value = SnapScanUiState.Error("Could not read the selected screenshot.")
                    return@launch
                }

                val aiResult = try {
                    aiService.extractMediaFromScreenshot(bitmap)
                } finally {
                    bitmap.recycle()
                }
                aiResult.fold(
                    onSuccess = { rawItems ->
                        if (rawItems.isEmpty()) {
                            _snapScanState.value = SnapScanUiState.Error(
                                "No movie or TV series titles were detected in this image. Try a clearer Reel caption, streaming row, or Top-10 list screenshot."
                            )
                            return@fold
                        }
                        val apiKey = getActiveTmdbApiKey()
                        val region = effectiveRegionCode.value
                        val candidates = coroutineScope {
                            rawItems.map { raw ->
                                async {
                                    val resolved = repository.resolveTitleToMediaEntity(
                                        rawTitle = raw.title,
                                        releaseYear = raw.releaseYear,
                                        preferredType = raw.mediaType,
                                        apiKey = apiKey,
                                        regionCode = region
                                    )
                                    ScannedMediaCandidate(
                                        rawTitle = raw.title,
                                        releaseYear = raw.releaseYear ?: resolved?.releaseYear,
                                        mediaType = resolved?.getMediaTypeEnum() ?: raw.mediaType,
                                        note = raw.note,
                                        matchedEntity = resolved
                                    )
                                }
                            }.awaitAll()
                        }
                        monetizationManager.recordSnapScanUsed()
                        _snapScanState.value = SnapScanUiState.Success(candidates)
                    },
                    onFailure = { err ->
                        _snapScanState.value = SnapScanUiState.Error(
                            err.message ?: "Failed to analyze screenshot."
                        )
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snapScanState.value = SnapScanUiState.Error(
                    e.localizedMessage ?: "Screenshot analysis failed."
                )
            }
        }
    }

    fun addScannedCandidateToWatchlist(candidate: ScannedMediaCandidate) {
        viewModelScope.launch {
            val entity = candidate.matchedEntity ?: MediaEntity(
                id = "scan_" + UUID.randomUUID().toString().take(8),
                title = candidate.rawTitle,
                mediaType = candidate.mediaType.name,
                releaseYear = candidate.releaseYear ?: 2025,
                genres = "AI Scanned",
                posterUrl = "",
                backdropUrl = "",
                overview = candidate.note,
                directorOrCreator = "",
                cast = "",
                communityRating = 0f,
                runtimeMinutes = if (candidate.mediaType == MediaType.MOVIE) 120 else 45,
                totalSeasons = if (candidate.mediaType == MediaType.TV_SHOW) 1 else 0,
                totalEpisodes = if (candidate.mediaType == MediaType.MOVIE) 1 else 10,
                isCustom = true
            )
            repository.ensureMediaCached(entity)
            repository.updateStatus(entity.id, WatchStatus.PLAN_TO_WATCH, getActiveTmdbApiKey())
        }
    }

    fun addAllScannedToWatchlist() {
        val current = _snapScanState.value as? SnapScanUiState.Success ?: return
        viewModelScope.launch {
            current.items.forEach { cand ->
                val entity = cand.matchedEntity
                if (entity != null) {
                    repository.ensureMediaCached(entity)
                    if (entity.getWatchStatusEnum() == WatchStatus.NONE) {
                        repository.updateStatus(entity.id, WatchStatus.PLAN_TO_WATCH, getActiveTmdbApiKey())
                    }
                } else {
                    addScannedCandidateToWatchlist(cand)
                }
            }
        }
    }

    fun resetSnapScan() {
        _snapScanState.value = SnapScanUiState.Idle
    }

    fun askAiConcierge(mood: String, timeBudget: String, audience: String) {
        if (!monetizationState.value.canUseConcierge) {
            showAiConciergeSheet.value = false
            openPaywall("You've used all ${MonetizationState.FREE_CONCIERGE_LIMIT} free AI Concierge sessions! Upgrade to WatchPilot Pro for unlimited 'Tonight's Pick' recommendations.")
            return
        }

        viewModelScope.launch {
            _conciergeState.value = ConciergeUiState.Loading
            val tracked = trackedMedia.value
            val favAndRated = tracked
                .filter { it.isFavorite || it.userRating >= 4.0f }
                .sortedByDescending { it.userRating }
                .map { "${it.title} (${it.releaseYear})" }
            val unwatched = tracked
                .filter { it.getWatchStatusEnum() == WatchStatus.PLAN_TO_WATCH }
                .map { "${it.title} (${it.releaseYear})" }
            val region = effectiveRegionCode.value
            val subs = monetizationState.value.subscribedProviders

            val result = aiService.generateConciergePicks(
                mood = mood,
                timeBudget = timeBudget,
                audience = audience,
                regionCode = region,
                subscribedProviders = subs,
                favoriteAndTopRatedTitles = favAndRated,
                unwatchedWatchlistTitles = unwatched
            )

            result.fold(
                onSuccess = { rawPicks ->
                    val apiKey = getActiveTmdbApiKey()
                    val enriched = coroutineScope {
                        rawPicks.map { pick ->
                            async {
                                val entity = repository.resolveTitleToMediaEntity(
                                    rawTitle = pick.title,
                                    releaseYear = pick.releaseYear,
                                    preferredType = pick.mediaType,
                                    apiKey = apiKey,
                                    regionCode = region
                                )
                                ConciergePick(
                                    title = entity?.title ?: pick.title,
                                    releaseYear = entity?.releaseYear ?: pick.releaseYear,
                                    mediaType = entity?.getMediaTypeEnum() ?: pick.mediaType,
                                    whyItFits = pick.whyItFits,
                                    matchScore = pick.matchScore,
                                    matchedEntity = entity
                                )
                            }
                        }.awaitAll()
                    }
                    monetizationManager.recordConciergeUsed()
                    _conciergeState.value = ConciergeUiState.Success(
                        mood = mood,
                        timeBudget = timeBudget,
                        picks = enriched
                    )
                },
                onFailure = { err ->
                    _conciergeState.value = ConciergeUiState.Error(
                        err.message ?: "Failed to generate Tonight's Picks."
                    )
                }
            )
        }
    }

    fun resetConcierge() {
        _conciergeState.value = ConciergeUiState.Idle
    }

    fun createMatchRoom(selectedProviders: List<String>) {
        if (currentUser.value == null) {
            _matchRoomState.value = MatchRoomUiState.Error("Sign in with Google first to host a live Swipe-to-Match room.")
            return
        }
        if (!monetizationState.value.canHostMatchRoom) {
            showSwipeMatchSheet.value = false
            openPaywall("Free tier includes ${MonetizationState.FREE_MATCH_ROOMS_LIMIT} hosted Swipe-to-Match room (joining a friend's code is always free!). Upgrade to WatchPilot Pro to host unlimited rooms.")
            return
        }

        viewModelScope.launch {
            _matchRoomState.value = MatchRoomUiState.Loading
            skippedMatchCandidateIds.value = emptySet()
            val region = effectiveRegionCode.value
            val canonicalSelected = selectedProviders.map { OttMoneySaverCalculator.canonicalizeProvider(it) }.filter { it.isNotBlank() }.toSet()

            val pool = (watchlistMedia.value + trendingItems.value + topRatedMovies.value + allMedia.value)
                .distinctBy { it.id }
                .filter { it.posterUrl.isNotBlank() }

            val filteredPool = if (canonicalSelected.isNotEmpty()) {
                val matching = pool.filter { OttMoneySaverCalculator.matchesAnySubscribedApp(it, region, canonicalSelected) }
                if (matching.size >= 6) matching else (matching + pool).distinctBy { it.id }
            } else {
                pool
            }

            val candidates = filteredPool.take(15).map { item ->
                val provs = item.getProvidersForRegion(region).take(2).joinToString(" • ").ifBlank { "Popular in $region" }
                MatchCandidateCard(
                    id = item.id,
                    title = item.title,
                    releaseYear = item.releaseYear,
                    mediaType = item.mediaType,
                    genres = item.getGenreList().take(2).joinToString(" • "),
                    rating = item.communityRating,
                    posterUrl = item.posterUrl,
                    providersLabel = provs
                )
            }

            if (candidates.isEmpty()) {
                _matchRoomState.value = MatchRoomUiState.Error("Catalog is still loading — wait a moment and try creating the room again.")
                return@launch
            }

            val hostName = currentUser.value?.displayName?.takeIf { it.isNotBlank() } ?: "WatchPilot Host"
            val res = matchRoomRepo.createRoom(
                hostName = hostName,
                regionCode = region,
                selectedProviders = canonicalSelected.toList(),
                candidates = candidates
            )
            res.fold(
                onSuccess = { room ->
                    monetizationManager.recordMatchRoomHosted()
                    _matchRoomState.value = MatchRoomUiState.Active(room)
                    startObservingMatchRoom(room.roomCode)
                },
                onFailure = { err ->
                    _matchRoomState.value = MatchRoomUiState.Error(
                        err.localizedMessage ?: "Could not create room."
                    )
                }
            )
        }
    }

    fun joinMatchRoom(rawCode: String) {
        if (currentUser.value == null) {
            _matchRoomState.value = MatchRoomUiState.Error("Sign in with Google first to join a live Swipe-to-Match room.")
            return
        }
        viewModelScope.launch {
            _matchRoomState.value = MatchRoomUiState.Loading
            skippedMatchCandidateIds.value = emptySet()
            val res = matchRoomRepo.joinRoom(rawCode)
            res.fold(
                onSuccess = { room ->
                    _matchRoomState.value = MatchRoomUiState.Active(room)
                    startObservingMatchRoom(room.roomCode)
                },
                onFailure = { err ->
                    _matchRoomState.value = MatchRoomUiState.Error(
                        err.localizedMessage ?: "Failed to join room."
                    )
                }
            )
        }
    }

    private fun startObservingMatchRoom(roomCode: String) {
        matchRoomObserverJob?.cancel()
        matchRoomObserverJob = viewModelScope.launch {
            matchRoomRepo.observeRoom(roomCode)
                .catch { e ->
                    if (e is CancellationException && e.cause == null) throw e
                    _matchRoomState.value = MatchRoomUiState.Error(
                        e.cause?.localizedMessage ?: e.localizedMessage ?: "Lost connection to room."
                    )
                }
                .collect { room ->
                    if (room != null) {
                        _matchRoomState.value = MatchRoomUiState.Active(room)
                    }
                }
        }
    }

    fun swipeMatchCard(candidate: MatchCandidateCard, liked: Boolean) {
        val active = (_matchRoomState.value as? MatchRoomUiState.Active)?.room ?: return
        if (!liked) {
            skippedMatchCandidateIds.value = skippedMatchCandidateIds.value + candidate.id
            return
        }
        viewModelScope.launch {
            matchRoomRepo.recordRightSwipe(active.roomCode, candidate).fold(
                onSuccess = { updatedRoom ->
                    _matchRoomState.value = MatchRoomUiState.Active(updatedRoom)
                },
                onFailure = { err ->
                    _matchRoomState.value = MatchRoomUiState.Error(
                        err.localizedMessage ?: "Could not save your swipe. Check your connection and try again."
                    )
                }
            )
        }
    }

    fun leaveOrCloseMatchRoom() {
        val active = (_matchRoomState.value as? MatchRoomUiState.Active)?.room
        matchRoomObserverJob?.cancel()
        skippedMatchCandidateIds.value = emptySet()
        _matchRoomState.value = MatchRoomUiState.Idle
        if (active != null && active.hostUid == currentUser.value?.uid && active.status == "OPEN") {
            viewModelScope.launch {
                matchRoomRepo.closeRoom(active.roomCode)
            }
        }
    }

    fun openMatchedMediaFromRoom(mediaId: String, title: String, year: Int, mediaTypeStr: String) {
        showSwipeMatchSheet.value = false
        viewModelScope.launch {
            val existing = findKnownMedia(mediaId) ?: repository.getMediaByIdDirect(mediaId)
            if (existing != null) {
                repository.ensureMediaCached(existing)
                selectMedia(existing.id)
            } else {
                val resolved = repository.resolveTitleToMediaEntity(
                    rawTitle = title,
                    releaseYear = year,
                    preferredType = if (mediaTypeStr.equals("TV_SHOW", ignoreCase = true)) MediaType.TV_SHOW else MediaType.MOVIE,
                    apiKey = getActiveTmdbApiKey(),
                    regionCode = effectiveRegionCode.value
                )
                if (resolved != null) {
                    selectMedia(resolved.id)
                }
            }
        }
    }

    fun importLibraryFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            isImportingCsv.value = true
            csvImportMessage.value = null
            try {
                val csvText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }
                if (csvText.isNullOrBlank()) {
                    csvImportMessage.value = "Selected file is empty or unreadable."
                    return@launch
                }
                val res = repository.importCsvLibrary(
                    csvContent = csvText,
                    apiKey = getActiveTmdbApiKey(),
                    regionCode = effectiveRegionCode.value
                )
                csvImportMessage.value = res.message
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                csvImportMessage.value = "CSV import error: ${e.localizedMessage ?: "Invalid CSV format"}"
            } finally {
                isImportingCsv.value = false
            }
        }
    }

    fun clearCsvImportMessage() {
        csvImportMessage.value = null
    }

    fun exportLibrary(
        context: Context,
        asJson: Boolean,
        onShareIntentReady: (Intent) -> Unit
    ) {
        if (!BuildConfig.IS_V1_EDITION && !monetizationState.value.isPro) {
            openPaywall("Portable CSV & JSON backup export is included with WatchPilot Pro (importing from Moviebase/IMDb/Letterboxd/Trakt is always 100% free!).")
            return
        }
        viewModelScope.launch {
            val tracked = trackedMedia.value
            if (tracked.isEmpty()) {
                csvImportMessage.value = "Track at least 1 movie or TV show before exporting a backup."
                return@launch
            }
            val intentRes = withContext(Dispatchers.IO) {
                CsvLibraryPorter.shareExportFile(context, tracked, asJson)
            }
            intentRes.fold(
                onSuccess = { intent -> onShareIntentReady(intent) },
                onFailure = { err -> csvImportMessage.value = "Export failed: ${err.localizedMessage}" }
            )
        }
    }

    fun shareWrappedCard(
        context: Context,
        onShareIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val userName = currentUser.value?.displayName?.takeIf { it.isNotBlank() } ?: "Cinephile"
            val currentStats = stats.value
            val res = withContext(Dispatchers.IO) {
                WrappedCardExporter.createShareableWrappedIntent(context, userName, currentStats)
            }
            res.fold(
                onSuccess = { intent -> onShareIntentReady(intent) },
                onFailure = { err -> syncStatusMessage.value = "Could not create Wrapped card: ${err.localizedMessage}" }
            )
        }
    }

    override fun onCleared() {
        matchRoomObserverJob?.cancel()
        monetizationManager.destroy()
        super.onCleared()
    }
}
