package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaType
import com.example.data.model.RegionalDefaults
import com.example.data.model.WatchStatus

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaType: String, // "MOVIE" or "TV_SHOW"
    val releaseYear: Int,
    val genres: String, // comma separated e.g. "Sci-Fi, Adventure"
    val posterUrl: String,
    val backdropUrl: String,
    val overview: String,
    val directorOrCreator: String,
    val cast: String, // comma separated
    val communityRating: Float,
    val runtimeMinutes: Int,
    val totalSeasons: Int = 0,
    val totalEpisodes: Int = 1,
    val networkOrStudio: String = "",
    val status: String = WatchStatus.NONE.name,
    val userRating: Float = 0f,
    val userReview: String = "",
    val isFavorite: Boolean = false,
    val lastWatchedDate: Long? = null,
    val watchedEpisodes: Int = 0,
    val watchedSeasons: Int = 0,
    val customWatchlist: String? = null,
    val isCustom: Boolean = false,
    val originalLanguage: String = "en", // "en", "ja", "ko", "es", "hi", "fr", "de"
    val originCountry: String = "US", // "US", "JP", "KR", "IN", "GB", "ES", "FR", "DE"
    val streamingProviders: String = "", // formatted: "US:Netflix,Prime|GB:BBC iPlayer|..."
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getMediaTypeEnum(): MediaType {
        return try {
            MediaType.valueOf(mediaType)
        } catch (_: Exception) {
            MediaType.MOVIE
        }
    }

    fun getWatchStatusEnum(): WatchStatus {
        return try {
            WatchStatus.valueOf(status)
        } catch (_: Exception) {
            WatchStatus.NONE
        }
    }

    fun getGenreList(): List<String> {
        return if (genres.isBlank()) emptyList() else genres.split(",").map { it.trim() }
    }

    fun getCastList(): List<String> {
        return if (cast.isBlank()) emptyList() else cast.split(",").map { it.trim() }
    }

    val progressFraction: Float
        get() {
            if (getMediaTypeEnum() == MediaType.MOVIE) {
                return if (getWatchStatusEnum() == WatchStatus.COMPLETED) 1f else 0f
            }
            if (totalEpisodes <= 0) return 0f
            return (watchedEpisodes.toFloat() / totalEpisodes).coerceIn(0f, 1f)
        }

    fun getProvidersForRegion(regionCode: String): List<String> {
        if (streamingProviders.isBlank()) return emptyList()
        val blocks = streamingProviders.split("|")
        for (block in blocks) {
            val parts = block.split(":")
            if (parts.size == 2 && (parts[0].equals(regionCode, ignoreCase = true) || parts[0].equals("GLOBAL", ignoreCase = true))) {
                return parts[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
        }
        // Fallback to GLOBAL or first entry
        val globalBlock = blocks.firstOrNull { it.startsWith("GLOBAL:", ignoreCase = true) }
        return globalBlock?.substringAfter(":")?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun getLanguageFlagAndName(): String {
        return RegionalDefaults.getLanguageLabel(originalLanguage)
    }

    fun getOriginFlag(): String {
        val region = RegionalDefaults.supportedRegions.find { it.code.equals(originCountry, ignoreCase = true) }
        return region?.flag ?: "🌐"
    }

    fun getUserRatingInStars(): Float {
        if (userRating <= 0f) return 0f
        return if (userRating > 5.0f) (userRating / 2f).coerceIn(1f, 5f) else userRating.coerceIn(1f, 5f)
    }

    fun getFormattedUserRating(): String {
        val stars = getUserRatingInStars()
        return if (stars > 0f) String.format("%.1f / 5.0", stars) else "Unrated"
    }

    fun isInWatchlist(): Boolean {
        return getWatchStatusEnum() == WatchStatus.PLAN_TO_WATCH ||
                getWatchStatusEnum() != WatchStatus.NONE ||
                customWatchlist != null ||
                isFavorite
    }
}
