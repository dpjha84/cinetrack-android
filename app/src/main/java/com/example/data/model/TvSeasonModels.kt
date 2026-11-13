package com.example.data.model

enum class TvWatchStatus(val label: String, val chipLabel: String) {
    UNWATCHED("Not Watched", "Unwatched"),
    PLAN_TO_WATCH("Plan to Watch", "Plan to Watch"),
    WATCHING("Watching", "Watching"),
    WATCHED("Watched", "Watched")
}

data class SeasonItem(
    val seriesId: String,
    val seriesTitle: String,
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val episodeCount: Int,
    val releaseYear: Int,
    val airDate: String,
    val rating: Float,
    val watchStatus: TvWatchStatus = TvWatchStatus.UNWATCHED,
    val watchedEpisodesCount: Int = 0
) {
    val progressFraction: Float
        get() = if (episodeCount > 0) (watchedEpisodesCount.toFloat() / episodeCount).coerceIn(0f, 1f) else 0f

    val isAllWatched: Boolean
        get() = episodeCount > 0 && watchedEpisodesCount >= episodeCount
}

data class EpisodeItem(
    val id: String, // e.g. "${seriesId}_s${seasonNumber}_e${episodeNumber}"
    val seriesId: String,
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val stillUrl: String,
    val airDate: String,
    val runtimeMinutes: Int,
    val rating: Float,
    val director: String = "",
    val writer: String = "",
    val guestCast: String = "",
    val watchStatus: TvWatchStatus = TvWatchStatus.UNWATCHED
)
