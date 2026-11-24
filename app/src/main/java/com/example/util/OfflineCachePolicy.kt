package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.TmdbSearchResultItem

/**
 * Pure Kotlin helpers for Room offline caching, non-destructive metadata merging,
 * and offline watchlist/search ranking.
 */
object OfflineCachePolicy {

    fun normalizeSearchQuery(query: String): String {
        return query.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    fun mergeFeedTags(existingTags: String, newTags: String): String {
        if (existingTags.isBlank() && newTags.isBlank()) return ""
        return (existingTags.split(",") + newTags.split(","))
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    /**
     * Merges incoming catalog metadata into an existing [MediaEntity] while strictly preserving
     * user tracking state (status, ratings, reviews, favorites, watched progress, custom collections).
     */
    fun mergeCatalogMetadataPreservingUserTracking(
        existing: MediaEntity,
        incoming: MediaEntity,
        extraFeedTag: String? = null
    ): MediaEntity {
        val mergedTags = mergeFeedTags(
            mergeFeedTags(existing.feedTags, incoming.feedTags),
            extraFeedTag.orEmpty()
        )
        return existing.copy(
            title = incoming.title.ifBlank { existing.title },
            releaseYear = if (incoming.releaseYear > 0) incoming.releaseYear else existing.releaseYear,
            genres = incoming.genres.ifBlank { existing.genres },
            posterUrl = incoming.posterUrl.ifBlank { existing.posterUrl },
            backdropUrl = incoming.backdropUrl.ifBlank { existing.backdropUrl },
            overview = incoming.overview.ifBlank { existing.overview },
            directorOrCreator = when {
                incoming.directorOrCreator.isBlank() -> existing.directorOrCreator
                existing.directorOrCreator.isBlank() ||
                    existing.directorOrCreator == "TMDB Creator" ||
                    existing.directorOrCreator == "Unknown Director" ||
                    existing.directorOrCreator == "Unknown Creator" -> incoming.directorOrCreator
                else -> existing.directorOrCreator
            },
            cast = if (incoming.cast.isNotBlank() && incoming.cast.length > existing.cast.length) {
                incoming.cast
            } else {
                existing.cast.ifBlank { incoming.cast }
            },
            communityRating = if (incoming.communityRating > 0f) incoming.communityRating else existing.communityRating,
            runtimeMinutes = if (incoming.runtimeMinutes > 0 && existing.runtimeMinutes <= 0) {
                incoming.runtimeMinutes
            } else if (incoming.runtimeMinutes > 0 && incoming.runtimeMinutes != 120 && incoming.runtimeMinutes != 50) {
                incoming.runtimeMinutes
            } else {
                existing.runtimeMinutes
            },
            totalSeasons = maxOf(existing.totalSeasons, incoming.totalSeasons),
            totalEpisodes = maxOf(existing.totalEpisodes, incoming.totalEpisodes),
            originalLanguage = incoming.originalLanguage.ifBlank { existing.originalLanguage },
            originCountry = if (existing.originCountry.isBlank() || existing.originCountry == "GLOBAL") {
                incoming.originCountry.ifBlank { existing.originCountry }
            } else {
                existing.originCountry
            },
            streamingProviders = mergeProviderBlocks(existing.streamingProviders, incoming.streamingProviders),
            feedTags = mergedTags
        )
    }

    private fun mergeProviderBlocks(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        val blocks = linkedMapOf<String, String>()
        for (raw in existing.split("|") + incoming.split("|")) {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) continue
            val region = trimmed.substringBefore(":", "").uppercase()
            val providers = trimmed.substringAfter(":", "")
            if (region.isNotBlank() && providers.isNotBlank()) {
                // Incoming regional block upgrades GLOBAL placeholder or appends missing region
                if (region == "GLOBAL" && blocks.containsKey("GLOBAL")) continue
                blocks[region] = providers
            }
        }
        return blocks.entries.joinToString("|") { "${it.key}:${it.value}" }.take(1900)
    }

    /**
     * Ranks offline media search results so saved watchlist/tracked items and strong title matches
     * appear at the top when internet is unavailable.
     */
    fun rankOfflineMediaMatches(
        items: List<MediaEntity>,
        query: String,
        limit: Int = 60
    ): List<MediaEntity> {
        val clean = normalizeSearchQuery(query)
        if (clean.isBlank()) return emptyList()

        return items
            .distinctBy { it.id }
            .filter { item ->
                item.title.contains(clean, ignoreCase = true) ||
                    item.cast.contains(clean, ignoreCase = true) ||
                    item.directorOrCreator.contains(clean, ignoreCase = true) ||
                    item.genres.contains(clean, ignoreCase = true) ||
                    item.overview.contains(clean, ignoreCase = true) ||
                    (item.customWatchlist?.contains(clean, ignoreCase = true) == true)
            }
            .sortedWith(
                compareByDescending<MediaEntity> { if (it.isInWatchlist()) 1 else 0 }
                    .thenByDescending { if (it.title.equals(clean, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { if (it.title.startsWith(clean, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { if (it.title.contains(clean, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { it.communityRating }
                    .thenByDescending { it.releaseYear }
            )
            .take(limit)
    }

    fun rankOfflinePeopleMatches(
        people: List<TmdbSearchResultItem.Person>,
        query: String,
        limit: Int = 30
    ): List<TmdbSearchResultItem.Person> {
        val clean = normalizeSearchQuery(query)
        if (clean.isBlank()) return emptyList()

        return people
            .distinctBy { it.id.takeIf { id -> id > 0L } ?: it.name.lowercase() }
            .filter { person ->
                person.name.contains(clean, ignoreCase = true) ||
                    person.department.contains(clean, ignoreCase = true) ||
                    person.knownFor.any { it.title.contains(clean, ignoreCase = true) }
            }
            .sortedWith(
                compareByDescending<TmdbSearchResultItem.Person> { if (it.name.equals(clean, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { if (it.name.startsWith(clean, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { it.popularity }
            )
            .take(limit)
    }
}
