package com.example.data.model

import com.example.data.local.MediaEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TrailerItem(
    val id: String,
    val mediaId: String,
    val title: String,
    val videoKey: String, // YouTube Video Key
    val videoTitle: String,
    val videoType: String, // "Trailer", "Teaser"
    val mediaType: MediaType,
    val releaseYear: Int,
    val genres: String,
    val posterUrl: String,
    val backdropUrl: String,
    val communityRating: Float,
    val overview: String,
    val originCountry: String = "US",
    val language: String = "en",
    val releaseDate: String = "", // e.g. "2026-11-22"
    val releaseStatus: String = "NOW_PLAYING" // "NOW_PLAYING", "UPCOMING"
) {
    val youtubeWatchUrl: String get() = "https://www.youtube.com/watch?v=$videoKey"
    val youtubeThumbnailUrl: String get() = "https://img.youtube.com/vi/$videoKey/hqdefault.jpg"

    val isUpcoming: Boolean get() {
        if (releaseDate.isNotBlank()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsed = format.parse(releaseDate)
                if (parsed != null) {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    return parsed.time > todayStart
                }
            } catch (_: Exception) {}
        }
        return releaseStatus.equals("UPCOMING", ignoreCase = true)
    }

    val isNowPlaying: Boolean get() = !isUpcoming

    /**
     * Keeps active theatrical / airing releases (within past 60 days) and all upcoming titles.
     */
    fun isWithinRelevanceWindow(referenceTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val sixtyDaysAgo = referenceTimeMillis - (60L * 24 * 60 * 60 * 1000)
        if (releaseDate.isNotBlank()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsed = format.parse(releaseDate)
                if (parsed != null) {
                    return parsed.time >= sixtyDaysAgo
                }
            } catch (_: Exception) {}
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return releaseYear >= currentYear
    }

    val statusBadgeLabel: String get() = when {
        isUpcoming -> if (releaseDate.isNotBlank()) "UPCOMING • ${formatDateMonthYear(releaseDate)}" else "UPCOMING • $releaseYear"
        mediaType == MediaType.MOVIE -> "NOW PLAYING"
        else -> "NOW STREAMING"
    }

    fun toMediaEntity(): MediaEntity {
        val isMovie = mediaType == MediaType.MOVIE
        return MediaEntity(
            id = mediaId,
            title = title,
            mediaType = mediaType.name,
            releaseYear = releaseYear,
            genres = genres,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            overview = overview,
            directorOrCreator = "Official Studio",
            cast = "",
            communityRating = communityRating,
            runtimeMinutes = if (isMovie) 120 else 50,
            totalSeasons = if (isMovie) 0 else 1,
            totalEpisodes = if (isMovie) 1 else 10,
            originalLanguage = language,
            originCountry = originCountry
        )
    }

    private fun formatDateMonthYear(dateStr: String): String {
        return try {
            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFormat = SimpleDateFormat("MMM yyyy", Locale.US)
            val d = inFormat.parse(dateStr)
            if (d != null) outFormat.format(d) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }
}
