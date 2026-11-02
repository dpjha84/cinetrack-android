package com.example.data.model

data class ViewingStats(
    val totalTimeMinutes: Long = 0,
    val completedMoviesCount: Int = 0,
    val completedShowsCount: Int = 0,
    val activeShowsCount: Int = 0,
    val totalEpisodesLogged: Int = 0,
    val averageUserRating: Float = 0f,
    val ratedItemsCount: Int = 0,
    val topGenres: List<GenreStat> = emptyList(),
    val cinephileRank: String = "Film Novice",
    val cinephileBadge: String = "🎬"
) {
    val totalDays: Long
        get() = totalTimeMinutes / (24 * 60)

    val remainingHours: Long
        get() = (totalTimeMinutes % (24 * 60)) / 60

    val remainingMinutes: Long
        get() = totalTimeMinutes % 60

    val formattedDuration: String
        get() = if (totalDays > 0) {
            "${totalDays}d ${remainingHours}h"
        } else if (remainingHours > 0) {
            "${remainingHours}h ${remainingMinutes}m"
        } else {
            "${remainingMinutes}m"
        }
}

data class GenreStat(
    val genre: String,
    val count: Int,
    val percentage: Float
)
