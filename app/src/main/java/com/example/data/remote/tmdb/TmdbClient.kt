package com.example.data.remote.tmdb

import com.example.BuildConfig
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object TmdbClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    val api: TmdbApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TmdbApiService::class.java)

    private val videoCheckClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private val videoPlayabilityCache = ConcurrentHashMap<String, Boolean>()

    fun isYouTubeVideoPlayable(videoKey: String): Boolean {
        val cleanKey = videoKey.trim()
        if (cleanKey.isBlank()) return false
        videoPlayabilityCache[cleanKey]?.let { return it }

        val playable = try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$cleanKey&format=json"
            val req = Request.Builder().url(oembedUrl).get().build()
            videoCheckClient.newCall(req).execute().use { resp ->
                when (resp.code) {
                    in 200..299 -> true
                    400, 404, 410 -> false
                    401, 403 -> {
                        // Embedding may be disabled by studio even though video plays in YouTube/Chrome.
                        // Verify public existence via YouTube thumbnail endpoint.
                        val thumbReq = Request.Builder()
                            .url("https://img.youtube.com/vi/$cleanKey/hqdefault.jpg")
                            .head()
                            .build()
                        videoCheckClient.newCall(thumbReq).execute().use { thumbResp ->
                            thumbResp.isSuccessful
                        }
                    }
                    else -> true
                }
            }
        } catch (_: Exception) {
            // Do not wipe live TMDB entries on transient network timeout
            true
        }

        videoPlayabilityCache[cleanKey] = playable
        return playable
    }

    private val genreMap = mapOf(
        28 to "Action",
        12 to "Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Sci-Fi",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western",
        10759 to "Action & Adventure",
        10762 to "Kids",
        10763 to "News",
        10764 to "Reality",
        10765 to "Sci-Fi & Fantasy",
        10766 to "Soap",
        10767 to "Talk",
        10768 to "War & Politics"
    )

    fun mapGenres(genreIds: List<Int>?): String {
        if (genreIds.isNullOrEmpty()) return "Drama"
        return genreIds.mapNotNull { genreMap[it] }.take(3).joinToString(", ").ifBlank { "Drama" }
    }

    fun toMediaEntity(dto: TmdbMediaDto): MediaEntity {
        val isMovie = dto.mediaType == "movie" || dto.title != null
        val title = dto.title ?: dto.name ?: "Unknown Title"
        val rawDate = dto.releaseDate ?: dto.firstAirDate ?: "2024"
        val year = rawDate.take(4).toIntOrNull() ?: 2024

        val poster = dto.posterPath?.let { "$IMAGE_BASE_URL$it" } ?: ""
        val backdrop = dto.backdropPath?.let { "$BACKDROP_BASE_URL$it" } ?: ""

        val origin = dto.originCountry?.firstOrNull() ?: when (dto.originalLanguage?.lowercase()) {
            "hi", "ta", "te", "ml", "kn", "bn", "mr", "pa", "gu", "or", "as" -> "IN"
            "ja" -> "JP"
            "ko" -> "KR"
            "en" -> "US"
            else -> "GLOBAL"
        }

        return MediaEntity(
            id = if (isMovie) "tmdb_m_${dto.id}" else "tmdb_tv_${dto.id}",
            title = title,
            mediaType = if (isMovie) MediaType.MOVIE.name else MediaType.TV_SHOW.name,
            releaseYear = year,
            genres = mapGenres(dto.genreIds),
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = dto.overview.orEmpty(),
            directorOrCreator = "TMDB Creator",
            cast = "",
            communityRating = (dto.voteAverage ?: 7.5).toFloat(),
            runtimeMinutes = if (isMovie) 120 else 50,
            totalSeasons = if (isMovie) 0 else 1,
            totalEpisodes = if (isMovie) 1 else 10,
            networkOrStudio = "TMDB",
            status = WatchStatus.NONE.name,
            userRating = 0f,
            userReview = "",
            isFavorite = false,
            originalLanguage = dto.originalLanguage ?: "en",
            originCountry = origin,
            streamingProviders = "GLOBAL:Netflix,Prime Video",
            isCustom = false
        )
    }

    fun knownForToEntity(dto: TmdbKnownForDto): MediaEntity {
        val isMovie = dto.mediaType == "movie" || dto.title != null
        val title = dto.title ?: dto.name ?: "Unknown Title"
        val rawDate = dto.releaseDate ?: dto.firstAirDate ?: "2024"
        val year = rawDate.take(4).toIntOrNull() ?: 2024

        val poster = dto.posterPath?.let { "$IMAGE_BASE_URL$it" } ?: ""
        val backdrop = dto.backdropPath?.let { "$BACKDROP_BASE_URL$it" } ?: ""
        val origin = dto.originCountry?.firstOrNull() ?: if (dto.originalLanguage == "en") "US" else "GLOBAL"

        return MediaEntity(
            id = if (isMovie) "tmdb_m_${dto.id}" else "tmdb_tv_${dto.id}",
            title = title,
            mediaType = if (isMovie) MediaType.MOVIE.name else MediaType.TV_SHOW.name,
            releaseYear = year,
            genres = mapGenres(dto.genreIds),
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = dto.overview.orEmpty(),
            directorOrCreator = "TMDB Creator",
            cast = "",
            communityRating = (dto.voteAverage ?: 7.5).toFloat(),
            runtimeMinutes = if (isMovie) 120 else 50,
            totalSeasons = if (isMovie) 0 else 1,
            totalEpisodes = if (isMovie) 1 else 10,
            networkOrStudio = "TMDB",
            status = WatchStatus.NONE.name,
            userRating = 0f,
            userReview = "",
            isFavorite = false,
            originalLanguage = dto.originalLanguage ?: "en",
            originCountry = origin,
            streamingProviders = "GLOBAL:Netflix,Prime Video",
            isCustom = false
        )
    }

    fun movieDetailToEntity(detail: TmdbMovieDetailDto, existing: MediaEntity? = null): MediaEntity {
        val poster = detail.posterPath?.let { "$IMAGE_BASE_URL$it" } ?: existing?.posterUrl.orEmpty()
        val backdrop = detail.backdropPath?.let { "$BACKDROP_BASE_URL$it" } ?: existing?.backdropUrl.orEmpty()

        val director = detail.credits?.crew?.find { it.job.equals("Director", ignoreCase = true) }?.name ?: "Unknown Director"
        val cast = detail.credits?.cast?.take(5)?.joinToString(", ") { it.name }.orEmpty()
        val genres = detail.genres?.take(3)?.joinToString(", ") { it.name } ?: "Drama"
        val year = detail.releaseDate?.take(4)?.toIntOrNull() ?: 2024
        val origin = detail.productionCountries?.firstOrNull()?.isoCode ?: "US"

        val providersString = formatWatchProviders(detail.watchProviders)

        return (existing ?: MediaEntity(
            id = "tmdb_m_${detail.id}",
            title = detail.title,
            mediaType = MediaType.MOVIE.name,
            releaseYear = year,
            genres = genres,
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = detail.overview.orEmpty(),
            directorOrCreator = director,
            cast = cast,
            communityRating = (detail.voteAverage ?: 7.5).toFloat(),
            runtimeMinutes = detail.runtime ?: 120,
            totalSeasons = 0,
            totalEpisodes = 1,
            networkOrStudio = "Universal / Warner Bros.",
            status = WatchStatus.NONE.name,
            originalLanguage = detail.originalLanguage ?: "en",
            originCountry = origin,
            streamingProviders = providersString
        )).copy(
            title = detail.title,
            genres = genres,
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = detail.overview ?: existing?.overview.orEmpty(),
            directorOrCreator = director,
            cast = if (cast.isNotBlank()) cast else existing?.cast.orEmpty(),
            runtimeMinutes = detail.runtime ?: existing?.runtimeMinutes ?: 120,
            streamingProviders = if (providersString.isNotBlank()) providersString else existing?.streamingProviders.orEmpty()
        )
    }

    fun tvDetailToEntity(detail: TmdbTvDetailDto, existing: MediaEntity? = null): MediaEntity {
        val poster = detail.posterPath?.let { "$IMAGE_BASE_URL$it" } ?: existing?.posterUrl.orEmpty()
        val backdrop = detail.backdropPath?.let { "$BACKDROP_BASE_URL$it" } ?: existing?.backdropUrl.orEmpty()

        val creator = detail.credits?.crew?.find { it.job.equals("Executive Producer", ignoreCase = true) || it.job.equals("Creator", ignoreCase = true) }?.name
            ?: "Unknown Creator"
        val cast = detail.credits?.cast?.take(5)?.joinToString(", ") { it.name }.orEmpty()
        val genres = detail.genres?.take(3)?.joinToString(", ") { it.name } ?: "Drama"
        val year = detail.firstAirDate?.take(4)?.toIntOrNull() ?: 2024
        val origin = detail.originCountry?.firstOrNull() ?: "US"

        val providersString = formatWatchProviders(detail.watchProviders)
        val avgRuntime = detail.episodeRunTime?.firstOrNull() ?: 50

        return (existing ?: MediaEntity(
            id = "tmdb_tv_${detail.id}",
            title = detail.name,
            mediaType = MediaType.TV_SHOW.name,
            releaseYear = year,
            genres = genres,
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = detail.overview.orEmpty(),
            directorOrCreator = creator,
            cast = cast,
            communityRating = (detail.voteAverage ?: 7.5).toFloat(),
            runtimeMinutes = avgRuntime,
            totalSeasons = detail.numberOfSeasons ?: 1,
            totalEpisodes = detail.numberOfEpisodes ?: 10,
            networkOrStudio = "TV Network",
            status = WatchStatus.NONE.name,
            originalLanguage = detail.originalLanguage ?: "en",
            originCountry = origin,
            streamingProviders = providersString
        )).copy(
            title = detail.name,
            genres = genres,
            posterUrl = poster,
            backdropUrl = backdrop,
            overview = detail.overview ?: existing?.overview.orEmpty(),
            directorOrCreator = creator,
            cast = if (cast.isNotBlank()) cast else existing?.cast.orEmpty(),
            totalSeasons = detail.numberOfSeasons ?: existing?.totalSeasons ?: 1,
            totalEpisodes = detail.numberOfEpisodes ?: existing?.totalEpisodes ?: 10,
            runtimeMinutes = avgRuntime,
            streamingProviders = if (providersString.isNotBlank()) providersString else existing?.streamingProviders.orEmpty()
        )
    }

    private fun formatWatchProviders(response: TmdbWatchProvidersResponse?): String {
        if (response?.results == null) return ""
        val buffer = mutableListOf<String>()

        response.results.forEach { (countryCode, countryData) ->
            val list = mutableListOf<String>()
            countryData.flatrate?.map { it.providerName }?.let { list.addAll(it) }
            countryData.rent?.take(2)?.map { it.providerName }?.let { list.addAll(it) }
            val distinct = list.distinct().take(4)
            if (distinct.isNotEmpty()) {
                buffer.add("$countryCode:${distinct.joinToString(",")}")
            }
        }
        return buffer.joinToString("|")
    }
}
