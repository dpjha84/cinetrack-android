package com.example.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/all/day")
    suspend fun getTrendingAllDay(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/all/week")
    suspend fun getTrendingAllWeek(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("tv/on_the_air")
    suspend fun getOnTheAirTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("tv/airing_today")
    suspend fun getAiringTodayTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String? = null,
        @Query("region") region: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/person")
    suspend fun searchPerson(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("region") region: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("region") region: String? = null,
        @Query("watch_region") watchRegion: String? = null,
        @Query("with_origin_country") withOriginCountry: String? = null,
        @Query("with_original_language") withLanguage: String? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("watch_region") region: String? = null,
        @Query("with_origin_country") withOriginCountry: String? = null,
        @Query("with_original_language") withLanguage: String? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("first_air_date.gte") firstAirDateGte: String? = null,
        @Query("first_air_date.lte") firstAirDateLte: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,watch/providers"
    ): TmdbMovieDetailDto

    @GET("tv/{series_id}")
    suspend fun getTvDetails(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,watch/providers"
    ): TmdbTvDetailDto

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Long,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "combined_credits"
    ): TmdbPersonDetailDto

    @GET("movie/{movie_id}/watch/providers")
    suspend fun getMovieWatchProviders(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String
    ): TmdbWatchProvidersResponse

    @GET("tv/{series_id}/watch/providers")
    suspend fun getTvWatchProviders(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String
    ): TmdbWatchProvidersResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("include_video_language") includeVideoLanguage: String? = null
    ): TmdbVideosResponse

    @GET("tv/{series_id}/videos")
    suspend fun getTvVideos(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null,
        @Query("include_video_language") includeVideoLanguage: String? = null
    ): TmdbVideosResponse

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("series_id") seriesId: Long,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null
    ): TmdbSeasonDetailDto
}
