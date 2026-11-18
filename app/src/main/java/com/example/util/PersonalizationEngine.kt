package com.example.util

import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.model.WatchStatus

object PersonalizationEngine {

    private fun normalizeTitleKey(item: MediaEntity): String {
        val clean = item.title.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${clean}_${item.mediaType}"
    }

    fun extractTopGenres(
        trackedMedia: List<MediaEntity>,
        preferredGenres: Set<String> = emptySet(),
        limit: Int = 4
    ): List<String> {
        if (trackedMedia.isEmpty() && preferredGenres.isEmpty()) return emptyList()
        val counts = mutableMapOf<String, Int>()

        preferredGenres.forEach { genre ->
            val clean = genre.trim()
            if (clean.isNotBlank()) {
                counts[clean] = (counts[clean] ?: 0) + 4
            }
        }

        trackedMedia.forEach { item ->
            val weight = when (item.getWatchStatusEnum()) {
                WatchStatus.COMPLETED -> 3
                WatchStatus.WATCHING -> 2
                WatchStatus.PLAN_TO_WATCH -> 1
                else -> 0
            } + (if (item.isFavorite) 3 else 0) + (if (item.userRating >= 4.0f) 2 else 0)

            if (weight > 0) {
                item.getGenreList().forEach { genre ->
                    val normalized = genre.trim()
                    if (normalized.isNotBlank()) {
                        counts[normalized] = (counts[normalized] ?: 0) + weight
                    }
                }
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    fun mapGenreNameToTmdbIds(genreName: String): Pair<String?, String?> {
        return when (genreName.trim().lowercase()) {
            "action" -> "28" to "10759"
            "adventure" -> "12" to "10759"
            "animation", "anime" -> "16" to "16"
            "comedy" -> "35" to "35"
            "crime" -> "80" to "80"
            "documentary" -> "99" to "99"
            "drama" -> "18" to "18"
            "family" -> "10751" to "10751"
            "fantasy" -> "14" to "10765"
            "history" -> "36" to null
            "horror" -> "27" to null
            "music" -> "10402" to null
            "mystery" -> "9648" to "9648"
            "romance" -> "10749" to null
            "sci-fi", "science fiction", "sci-fi & fantasy" -> "878" to "10765"
            "thriller" -> "53" to "80"
            "war", "war & politics" -> "10752" to "10768"
            "western" -> "37" to "37"
            "action & adventure" -> "28,12" to "10759"
            else -> null to null
        }
    }

    fun rankAndDeduplicateRecommendations(
        candidates: List<MediaEntity>,
        trackedMedia: List<MediaEntity>,
        favoritePeople: List<FavoritePersonEntity>,
        regionCode: String,
        preferredGenres: Set<String> = emptySet(),
        maxItems: Int = 18
    ): List<MediaEntity> {
        if (candidates.isEmpty()) return emptyList()

        val hasSignals = trackedMedia.isNotEmpty() || favoritePeople.isNotEmpty() || preferredGenres.isNotEmpty()
        if (!hasSignals) return emptyList()

        val trackedIds = trackedMedia.map { it.id }.toHashSet()
        val trackedKeys = trackedMedia.map { normalizeTitleKey(it) }.toHashSet()
        val topGenres = extractTopGenres(trackedMedia, preferredGenres = preferredGenres, limit = 5)
        val topGenreSet = topGenres.map { it.lowercase() }.toSet()
        val favoriteNames = favoritePeople
            .map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }

        val deduped = candidates
            .filter { candidate ->
                candidate.title.isNotBlank() &&
                    candidate.id !in trackedIds &&
                    normalizeTitleKey(candidate) !in trackedKeys &&
                    candidate.status == WatchStatus.NONE.name &&
                    !candidate.isFavorite
            }
            .groupBy { normalizeTitleKey(it) }
            .values
            .map { group ->
                // Prefer TMDB live entries with posters
                group.maxBy { item ->
                    var q = 0
                    if (item.id.startsWith("tmdb_")) q += 4
                    if (item.posterUrl.isNotBlank()) q += 3
                    if (item.cast.isNotBlank()) q += 1
                    q
                }
            }

        return deduped
            .filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }
            .sortedByDescending { item ->
                var score = 0f

                // Strong signal: matches one of the user's favorite actors or directors
                val castLower = item.cast.lowercase()
                val directorLower = item.directorOrCreator.lowercase()
                val personMatchCount = favoriteNames.count { personName ->
                    castLower.contains(personName) || directorLower.contains(personName)
                }
                score += personMatchCount * 48f

                // Genre affinity with preferred onboarding genres & watched/liked titles
                val genreMatches = item.getGenreList().count { genre ->
                    val gLower = genre.trim().lowercase()
                    gLower in topGenreSet || topGenreSet.any { tg -> gLower.contains(tg) || tg.contains(gLower) }
                }
                score += genreMatches * 18f

                // Region boost
                if (item.originCountry.equals(regionCode, ignoreCase = true)) {
                    score += 8f
                }

                // Community quality & recency
                score += item.communityRating.coerceIn(0f, 10f) * 2.2f
                if (item.releaseYear >= 2022) score += 5f
                if (item.id.startsWith("tmdb_")) score += 4f
                score
            }
            .take(maxItems)
    }

    fun buildRecommendationReason(
        candidate: MediaEntity,
        trackedMedia: List<MediaEntity>,
        favoritePeople: List<FavoritePersonEntity>,
        preferredGenres: Set<String> = emptySet()
    ): String {
        val castLower = candidate.cast.lowercase()
        val directorLower = candidate.directorOrCreator.lowercase()
        val matchedPerson = favoritePeople.firstOrNull { person ->
            val pLower = person.name.trim().lowercase()
            pLower.isNotBlank() && (castLower.contains(pLower) || directorLower.contains(pLower))
        }
        if (matchedPerson != null) {
            return "★ ${matchedPerson.name}"
        }

        val topGenres = extractTopGenres(trackedMedia, preferredGenres = preferredGenres, limit = 5)
        val candidateGenres = candidate.getGenreList()
        val matchedGenre = candidateGenres.firstOrNull { cg ->
            val cgLower = cg.trim().lowercase()
            topGenres.any { tg ->
                val tgLower = tg.trim().lowercase()
                cgLower == tgLower || cgLower.contains(tgLower) || tgLower.contains(cgLower)
            }
        }
        if (!matchedGenre.isNullOrBlank()) {
            return "$matchedGenre Pick"
        }

        if (trackedMedia.any { it.isFavorite || it.getWatchStatusEnum() == WatchStatus.COMPLETED }) {
            return "Taste Match"
        }
        return "Top Rated"
    }
}
