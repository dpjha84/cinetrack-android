package com.example.util

import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem

/**
 * Pure Kotlin prioritization and merging helpers so onboarding screens
 * showcase titles and stars/creators tailored to the user's detected region.
 */
object OnboardingRegionalPolicy {

    fun normalizeRegionCode(region: String?): String? {
        val clean = region?.trim()?.uppercase().orEmpty()
        if (clean.isBlank() || clean == "AUTO" || clean == "GLOBAL") return null
        return clean
    }

    fun isTitleRelevantToRegion(media: MediaEntity, regionCode: String?): Boolean {
        val safeRegion = normalizeRegionCode(regionCode) ?: return true
        if (media.originCountry.equals(safeRegion, ignoreCase = true)) return true
        if (media.streamingProviders.contains("$safeRegion:", ignoreCase = true)) return true
        return false
    }

    fun isPersonRelevantToRegion(person: TmdbSearchResultItem.Person, regionCode: String?): Boolean {
        val safeRegion = normalizeRegionCode(regionCode) ?: return true
        return person.knownFor.any { isTitleRelevantToRegion(it, safeRegion) }
    }

    fun interleaveMoviesAndTv(
        movies: List<MediaEntity>,
        tvShows: List<MediaEntity>
    ): List<MediaEntity> = buildList {
        val distinctMovies = movies.distinctBy { it.id }
        val distinctTv = tvShows.distinctBy { it.id }
        val maxLen = maxOf(distinctMovies.size, distinctTv.size)
        for (i in 0 until maxLen) {
            if (i < distinctMovies.size) add(distinctMovies[i])
            if (i < distinctTv.size) add(distinctTv[i])
        }
    }

    fun prioritizeOnboardingTitlesForRegion(
        regionalOriginMovies: List<MediaEntity>,
        regionalOriginTv: List<MediaEntity>,
        popularInRegionMovies: List<MediaEntity>,
        popularInRegionTv: List<MediaEntity>,
        topRatedMovies: List<MediaEntity>,
        topRatedTv: List<MediaEntity>,
        regionCode: String?,
        limit: Int = 36
    ): List<MediaEntity> {
        val safeRegion = normalizeRegionCode(regionCode)
        val hasRenderableArt: (MediaEntity) -> Boolean = {
            it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank()
        }

        if (safeRegion == null) {
            return interleaveMoviesAndTv(
                movies = popularInRegionMovies + topRatedMovies,
                tvShows = popularInRegionTv + topRatedTv
            ).filter(hasRenderableArt)
                .distinctBy { it.id }
                .take(limit)
        }

        val regionalPrimary = interleaveMoviesAndTv(
            movies = regionalOriginMovies,
            tvShows = regionalOriginTv
        ).filter(hasRenderableArt)

        val regionalSupporting = interleaveMoviesAndTv(
            movies = popularInRegionMovies + topRatedMovies,
            tvShows = popularInRegionTv + topRatedTv
        ).filter(hasRenderableArt)

        val primaryIds = regionalPrimary.mapTo(HashSet()) { it.id }
        val supportingOnly = regionalSupporting.filter { it.id !in primaryIds }

        // Blend regional-origin hits first while keeping global/streaming-in-region favorites accessible
        val primaryLeadQuota = ((limit * 2) / 3).coerceAtLeast(18)
        val leadChunk = regionalPrimary.take(primaryLeadQuota)
        val remainingRegional = regionalPrimary.drop(leadChunk.size)

        return (leadChunk + supportingOnly + remainingRegional)
            .distinctBy { it.id }
            .take(limit)
    }

    fun rankFallbackTitlesForRegion(
        pool: List<MediaEntity>,
        regionCode: String?,
        limit: Int = 30
    ): List<MediaEntity> {
        val safeRegion = normalizeRegionCode(regionCode)
        val cleanPool = pool
            .filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }
            .distinctBy { it.id }

        if (safeRegion == null) {
            return interleaveMoviesAndTv(
                movies = cleanPool.filter { it.getMediaTypeEnum() == MediaType.MOVIE },
                tvShows = cleanPool.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
            ).take(limit)
        }

        val originMatches = cleanPool.filter { it.originCountry.equals(safeRegion, ignoreCase = true) }
        val providerMatches = cleanPool.filter {
            !it.originCountry.equals(safeRegion, ignoreCase = true) &&
                it.streamingProviders.contains("$safeRegion:", ignoreCase = true)
        }
        val globalOthers = cleanPool.filter {
            !it.originCountry.equals(safeRegion, ignoreCase = true) &&
                !it.streamingProviders.contains("$safeRegion:", ignoreCase = true)
        }

        val originInterleaved = interleaveMoviesAndTv(
            movies = originMatches.filter { it.getMediaTypeEnum() == MediaType.MOVIE },
            tvShows = originMatches.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        )
        val providerInterleaved = interleaveMoviesAndTv(
            movies = providerMatches.filter { it.getMediaTypeEnum() == MediaType.MOVIE },
            tvShows = providerMatches.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        )
        val globalInterleaved = interleaveMoviesAndTv(
            movies = globalOthers.filter { it.getMediaTypeEnum() == MediaType.MOVIE },
            tvShows = globalOthers.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        )

        return (originInterleaved + providerInterleaved + globalInterleaved)
            .distinctBy { it.id }
            .take(limit)
    }

    fun prioritizeOnboardingPeopleForRegion(
        regionalStars: List<TmdbSearchResultItem.Person>,
        globalPopularPeople: List<TmdbSearchResultItem.Person>,
        samplePeople: List<TmdbSearchResultItem.Person>,
        regionCode: String?,
        limit: Int = 24
    ): List<TmdbSearchResultItem.Person> {
        val safeRegion = normalizeRegionCode(regionCode)
        val ordered = if (safeRegion == null) {
            globalPopularPeople + samplePeople + regionalStars
        } else {
            val regionalSample = samplePeople.filter {
                it.knownFor.any { media -> media.originCountry.equals(safeRegion, ignoreCase = true) }
            }
            val otherSample = samplePeople.filterNot { person ->
                person.knownFor.any { media -> media.originCountry.equals(safeRegion, ignoreCase = true) }
            }
            regionalStars + regionalSample + globalPopularPeople + otherSample
        }

        val seenIds = HashSet<Long>()
        val seenNames = HashSet<String>()
        val result = ArrayList<TmdbSearchResultItem.Person>(limit)

        for (person in ordered) {
            val cleanName = person.name.trim().lowercase()
            if (cleanName.isBlank()) continue
            if (person.id > 0L && !seenIds.add(person.id)) continue
            if (!seenNames.add(cleanName)) continue
            result.add(person)
            if (result.size >= limit) break
        }
        return result
    }

    fun rankFallbackPeopleForRegion(
        samplePeople: List<TmdbSearchResultItem.Person>,
        cachedPeople: List<TmdbSearchResultItem.Person>,
        regionCode: String?,
        limit: Int = 24
    ): List<TmdbSearchResultItem.Person> {
        val safeRegion = normalizeRegionCode(regionCode)
        val combined = cachedPeople + samplePeople
        if (safeRegion == null) {
            return prioritizeOnboardingPeopleForRegion(
                regionalStars = emptyList(),
                globalPopularPeople = cachedPeople,
                samplePeople = samplePeople,
                regionCode = null,
                limit = limit
            )
        }
        val regional = combined.filter { person ->
            person.knownFor.any { it.originCountry.equals(safeRegion, ignoreCase = true) }
        }
        val others = combined.filterNot { person ->
            person.knownFor.any { it.originCountry.equals(safeRegion, ignoreCase = true) }
        }
        return prioritizeOnboardingPeopleForRegion(
            regionalStars = regional,
            globalPopularPeople = others,
            samplePeople = emptyList(),
            regionCode = safeRegion,
            limit = limit
        )
    }
}
