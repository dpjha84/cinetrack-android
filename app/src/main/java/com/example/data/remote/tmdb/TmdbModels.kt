package com.example.data.remote.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val page: Int = 1,
    val results: List<TmdbMediaDto> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 1,
    @Json(name = "total_results") val totalResults: Int = 0
)

@JsonClass(generateAdapter = true)
data class TmdbMediaDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "profile_path") val profilePath: String? = null,
    @Json(name = "known_for_department") val knownForDepartment: String? = null,
    @Json(name = "known_for") val knownFor: List<TmdbKnownForDto>? = null,
    val popularity: Double? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "origin_country") val originCountry: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbKnownForDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "origin_country") val originCountry: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbPersonDetailDto(
    val id: Long,
    val name: String,
    val biography: String? = null,
    val birthday: String? = null,
    @Json(name = "place_of_birth") val placeOfBirth: String? = null,
    @Json(name = "profile_path") val profilePath: String? = null,
    @Json(name = "known_for_department") val knownForDepartment: String? = null,
    @Json(name = "combined_credits") val combinedCredits: TmdbPersonCombinedCreditsDto? = null
)

@JsonClass(generateAdapter = true)
data class TmdbPersonCombinedCreditsDto(
    val cast: List<TmdbKnownForDto>? = null,
    val crew: List<TmdbKnownForDto>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetailDto(
    val id: Long,
    val title: String,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    val genres: List<TmdbGenreDto>? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "production_countries") val productionCountries: List<TmdbCountryDto>? = null,
    val credits: TmdbCreditsDto? = null,
    @Json(name = "watch/providers") val watchProviders: TmdbWatchProvidersResponse? = null
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetailDto(
    val id: Long,
    val name: String,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "episode_run_time") val episodeRunTime: List<Int>? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    val genres: List<TmdbGenreDto>? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "origin_country") val originCountry: List<String>? = null,
    val credits: TmdbCreditsDto? = null,
    @Json(name = "watch/providers") val watchProviders: TmdbWatchProvidersResponse? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCreditsDto(
    val cast: List<TmdbCastDto>? = null,
    val crew: List<TmdbCrewDto>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCastDto(
    val name: String,
    val character: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCrewDto(
    val name: String,
    val job: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbGenreDto(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCountryDto(
    @Json(name = "iso_3166_1") val isoCode: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbWatchProvidersResponse(
    val results: Map<String, TmdbCountryProvidersDto>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCountryProvidersDto(
    val link: String? = null,
    val flatrate: List<TmdbProviderDto>? = null,
    val rent: List<TmdbProviderDto>? = null,
    val buy: List<TmdbProviderDto>? = null,
    val free: List<TmdbProviderDto>? = null,
    val ads: List<TmdbProviderDto>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbProviderDto(
    @Json(name = "provider_id") val providerId: Int? = null,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "logo_path") val logoPath: String? = null,
    @Json(name = "display_priority") val displayPriority: Int? = null
)
