package com.example.util

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ResolvedStreamingDeepLinks(
    val urlsByProviderKey: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
internal data class WikidataSparqlResponse(
    val results: WikidataResultsDto? = null
)

@JsonClass(generateAdapter = true)
internal data class WikidataResultsDto(
    val bindings: List<Map<String, WikidataBindingValueDto>>? = null
)

@JsonClass(generateAdapter = true)
internal data class WikidataBindingValueDto(
    val type: String? = null,
    val value: String? = null
)

@JsonClass(generateAdapter = true)
internal data class ItunesSearchResponseDto(
    val results: List<ItunesItemDto>? = null
)

@JsonClass(generateAdapter = true)
internal data class ItunesItemDto(
    val trackName: String? = null,
    val collectionName: String? = null,
    val trackViewUrl: String? = null,
    val collectionViewUrl: String? = null,
    val releaseDate: String? = null
)

/**
 * Resolves exact provider title deep links (e.g. Hotstar show URL, Netflix title ID,
 * Prime Video ASIN detail URL, Disney+ movie/series URL, Max/Hulu/Apple TV/Peacock/
 * Crunchyroll/BBC iPlayer URLs) using open structured JSON APIs (Wikidata SPARQL &
 * Apple iTunes Search API) with zero HTML scraping.
 */
object StreamingDeepLinkResolver {

    private const val WIKIDATA_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql"
    private const val ITUNES_SEARCH_ENDPOINT = "https://itunes.apple.com/search"
    private const val USER_AGENT = "WatchPilot/5.1.0 (Android; Movie & TV Tracker)"

    private val hotstarMarkets = setOf("IN", "ID", "MY", "TH", "SG", "PH")

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder().build()
    private val wikidataAdapter = moshi.adapter(WikidataSparqlResponse::class.java)
    private val itunesAdapter = moshi.adapter(ItunesSearchResponseDto::class.java)

    private val cache = ConcurrentHashMap<String, ResolvedStreamingDeepLinks>()
    private val imdbIdCache = ConcurrentHashMap<String, String>()

    fun registerImdbId(mediaId: String, imdbId: String?) {
        val clean = imdbId?.trim()?.takeIf { Regex("^tt\\d+$").matches(it) } ?: return
        imdbIdCache[mediaId.trim()] = clean
    }

    suspend fun resolveForTitle(
        mediaId: String,
        title: String,
        isMovie: Boolean,
        releaseYear: Int = 0,
        regionCode: String = "US",
        providerNames: List<String> = emptyList(),
        imdbId: String? = null
    ): ResolvedStreamingDeepLinks = withContext(Dispatchers.IO) {
        val upperRegion = regionCode.trim().uppercase(Locale.ROOT).ifBlank { "US" }
        val cacheKey = "${mediaId.trim()}:$upperRegion"
        cache[cacheKey]?.let { return@withContext it }

        val effectiveImdbId = imdbId?.trim()?.takeIf { Regex("^tt\\d+$").matches(it) }
            ?: imdbIdCache[mediaId.trim()]

        val tmdbNumericId = when {
            mediaId.startsWith("tmdb_m_") -> mediaId.removePrefix("tmdb_m_").toLongOrNull()
            mediaId.startsWith("tmdb_tv_") -> mediaId.removePrefix("tmdb_tv_").toLongOrNull()
            else -> null
        }

        val needsAppleLookup = providerNames.any {
            val lower = it.lowercase(Locale.ROOT)
            lower.contains("apple") || lower.contains("itunes")
        }

        val resolved = coroutineScope {
            val wikidataDeferred = async {
                if (tmdbNumericId != null || effectiveImdbId != null) {
                    withTimeoutOrNull(4000L) {
                        queryWikidataDeepLinks(
                            tmdbId = tmdbNumericId,
                            imdbId = effectiveImdbId,
                            title = title,
                            isMovie = isMovie,
                            regionCode = upperRegion
                        )
                    }.orEmpty()
                } else {
                    emptyMap()
                }
            }

            val itunesDeferred = async {
                if (needsAppleLookup && title.isNotBlank()) {
                    withTimeoutOrNull(2200L) {
                        queryAppleItunesUrl(
                            title = title,
                            isMovie = isMovie,
                            releaseYear = releaseYear,
                            regionCode = upperRegion
                        )
                    }
                } else {
                    null
                }
            }

            val merged = LinkedHashMap<String, String>(wikidataDeferred.await())
            val itunesUrl = itunesDeferred.await()
            if (!itunesUrl.isNullOrBlank() && !merged.containsKey("apple")) {
                merged["apple"] = itunesUrl
            }

            ResolvedStreamingDeepLinks(urlsByProviderKey = merged)
        }

        if (resolved.urlsByProviderKey.isNotEmpty()) {
            cache[cacheKey] = resolved
        }
        resolved
    }

    fun matchDirectUrl(
        resolved: ResolvedStreamingDeepLinks,
        providerName: String,
        regionCode: String = "US"
    ): String? {
        if (resolved.urlsByProviderKey.isEmpty()) return null
        val lower = providerName.trim().lowercase(Locale.ROOT)
        val upperRegion = regionCode.trim().uppercase(Locale.ROOT).ifBlank { "US" }

        return when {
            lower.contains("netflix") -> resolved.urlsByProviderKey["netflix"]
            lower.contains("hotstar") -> resolved.urlsByProviderKey["hotstar"]
            lower.contains("disney") -> {
                if (upperRegion in setOf("IN", "ID", "MY", "TH")) {
                    resolved.urlsByProviderKey["hotstar"] ?: resolved.urlsByProviderKey["disney"]
                } else {
                    resolved.urlsByProviderKey["disney"] ?: resolved.urlsByProviderKey["hotstar"]
                }
            }
            lower.contains("jio") -> resolved.urlsByProviderKey["hotstar"]
            lower.contains("amazon channel") -> null
            lower.contains("prime") || lower.contains("amazon") -> resolved.urlsByProviderKey["prime"]
            lower.contains("hulu") -> resolved.urlsByProviderKey["hulu"]
            lower == "max" || lower.startsWith("max ") || lower.contains("hbo") -> resolved.urlsByProviderKey["max"]
            lower.contains("apple") || lower.contains("itunes") -> resolved.urlsByProviderKey["apple"]
            lower.contains("peacock") -> resolved.urlsByProviderKey["peacock"]
            lower.contains("crunchyroll") -> resolved.urlsByProviderKey["crunchyroll"]
            lower.contains("bbc iplayer") || lower == "iplayer" -> resolved.urlsByProviderKey["bbc"]
            else -> null
        }
    }

    internal fun buildWikidataSparqlQuery(
        tmdbId: Long?,
        isMovie: Boolean,
        imdbId: String? = null
    ): String {
        val tmdbProperty = if (isMovie) "wdt:P4947" else "wdt:P4983"
        val cleanImdb = imdbId?.trim()?.takeIf { Regex("^tt\\d+$").matches(it) }
        val matchClause = when {
            tmdbId != null && cleanImdb != null ->
                """{ ?item $tmdbProperty "$tmdbId" . } UNION { ?item wdt:P345 "$cleanImdb" . }"""
            tmdbId != null ->
                """?item $tmdbProperty "$tmdbId" ."""
            cleanImdb != null ->
                """?item wdt:P345 "$cleanImdb" ."""
            else -> ""
        }
        return """
            SELECT ?netflix ?hotstar ?prime ?disneyMovie ?disneySeries ?huluMovie ?huluSeries ?max ?appleMovie ?peacock ?crunchyroll ?bbc WHERE {
              $matchClause
              OPTIONAL { ?item wdt:P1874 ?netflix . }
              OPTIONAL { ?item wdt:P11049 ?hotstar . }
              OPTIONAL { ?item wdt:P8055 ?prime . }
              OPTIONAL { ?item wdt:P7595 ?disneyMovie . }
              OPTIONAL { ?item wdt:P7596 ?disneySeries . }
              OPTIONAL { ?item wdt:P6466 ?huluMovie . }
              OPTIONAL { ?item wdt:P6467 ?huluSeries . }
              OPTIONAL { ?item wdt:P8298 ?max . }
              OPTIONAL { ?item wdt:P9586 ?appleMovie . }
              OPTIONAL { ?item wdt:P11815 ?peacock . }
              OPTIONAL { ?item wdt:P11330 ?crunchyroll . }
              OPTIONAL { ?item wdt:P827 ?bbc . }
            }
            LIMIT 1
        """.trimIndent()
    }

    internal fun parseWikidataBindings(
        jsonBody: String,
        title: String,
        isMovie: Boolean,
        regionCode: String
    ): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val parsed = runCatching { wikidataAdapter.fromJson(jsonBody) }.getOrNull() ?: return result
        val row = parsed.results?.bindings?.firstOrNull() ?: return result

        fun valOf(key: String): String? =
            row[key]?.value?.trim()?.takeIf { it.isNotBlank() }

        valOf("netflix")?.let { raw ->
            normalizeNetflixUrl(raw)?.let { result["netflix"] = it }
        }
        valOf("hotstar")?.let { raw ->
            normalizeHotstarUrl(raw, title = title, isMovie = isMovie, regionCode = regionCode)
                ?.let { result["hotstar"] = it }
        }
        valOf("prime")?.let { raw ->
            normalizePrimeVideoUrl(raw)?.let { result["prime"] = it }
        }
        val disneyRaw = if (isMovie) valOf("disneyMovie") ?: valOf("disneySeries")
        else valOf("disneySeries") ?: valOf("disneyMovie")
        disneyRaw?.let { raw ->
            normalizeDisneyUrl(raw, isMovie = isMovie)?.let { result["disney"] = it }
        }

        val huluRaw = if (isMovie) valOf("huluMovie") ?: valOf("huluSeries")
        else valOf("huluSeries") ?: valOf("huluMovie")
        huluRaw?.let { raw ->
            normalizeHuluUrl(raw, isMovie = isMovie)?.let { result["hulu"] = it }
        }

        valOf("max")?.let { raw ->
            normalizeMaxUrl(raw)?.let { result["max"] = it }
        }
        valOf("appleMovie")?.let { raw ->
            normalizeAppleTvUrl(raw, isMovie = true)?.let { result["apple"] = it }
        }
        valOf("peacock")?.let { raw ->
            normalizePeacockUrl(raw)?.let { result["peacock"] = it }
        }
        valOf("crunchyroll")?.let { raw ->
            normalizeCrunchyrollUrl(raw)?.let { result["crunchyroll"] = it }
        }
        valOf("bbc")?.let { raw ->
            normalizeBbcUrl(raw)?.let { result["bbc"] = it }
        }
        return result
    }

    internal fun parseItunesSearchResponse(
        jsonBody: String,
        title: String,
        isMovie: Boolean,
        releaseYear: Int
    ): String? {
        val parsed = runCatching { itunesAdapter.fromJson(jsonBody) }.getOrNull() ?: return null
        val results = parsed.results.orEmpty()
        if (results.isEmpty()) return null

        val normalizedQuery = normalizeTitleForCompare(title)
        var bestScore = -1
        var bestUrl: String? = null

        for (item in results) {
            val candidateTitle = (if (isMovie) item.trackName else item.collectionName)
                .orEmpty()
                .ifBlank { item.trackName.orEmpty() }
                .trim()
            val candidateUrl = (if (isMovie) item.trackViewUrl else item.collectionViewUrl)
                .orEmpty()
                .ifBlank { item.trackViewUrl.orEmpty() }
                .trim()
            if (!candidateUrl.startsWith("https://")) continue

            val normCandidate = normalizeTitleForCompare(candidateTitle)
            if (normCandidate.isBlank()) continue

            var score = 0
            if (normCandidate == normalizedQuery) score += 100
            else if (normCandidate.contains(normalizedQuery) || normalizedQuery.contains(normCandidate)) score += 45
            else continue

            val itemYear = item.releaseDate?.take(4)?.toIntOrNull()
            if (releaseYear > 1900 && itemYear != null) {
                val diff = kotlin.math.abs(itemYear - releaseYear)
                score += when {
                    diff == 0 -> 30
                    diff <= 1 -> 15
                    else -> -20
                }
            }
            if (candidateUrl.contains("tv.apple.com", ignoreCase = true)) {
                score += 10
            }

            if (score > bestScore) {
                bestScore = score
                bestUrl = candidateUrl
            }
        }
        return if (bestScore >= 40) bestUrl else null
    }

    internal fun normalizeNetflixUrl(raw: String): String? {
        val clean = raw.trim()
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        val digits = clean.removePrefix("/")
        return "https://www.netflix.com/title/$digits"
    }

    internal fun normalizeHotstarUrl(
        raw: String,
        title: String,
        isMovie: Boolean,
        regionCode: String
    ): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean

        val upperRegion = regionCode.trim().uppercase(Locale.ROOT).ifBlank { "IN" }
        val marketPrefix = if (upperRegion in hotstarMarkets) {
            upperRegion.lowercase(Locale.ROOT)
        } else {
            "in"
        }

        return when {
            Regex("^[a-z]{2}/.+").matches(clean) -> "https://www.hotstar.com/$clean"
            clean.contains("/") -> "https://www.hotstar.com/$marketPrefix/$clean"
            else -> {
                val category = if (isMovie) "movies" else "shows"
                val slug = slugifyTitle(title)
                if (slug.isNotBlank()) {
                    "https://www.hotstar.com/$marketPrefix/$category/$slug/$clean"
                } else {
                    "https://www.hotstar.com/$marketPrefix/$category/$clean"
                }
            }
        }
    }

    internal fun normalizePrimeVideoUrl(raw: String): String? {
        val clean = raw.trim()
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return "https://app.primevideo.com/detail?asin=${clean.removePrefix("/")}"
    }

    internal fun normalizeDisneyUrl(raw: String, isMovie: Boolean): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        val segment = if (isMovie) "movies/wd" else "series/wp"
        return if (clean.startsWith("movies/") || clean.startsWith("series/")) {
            "https://www.disneyplus.com/$clean"
        } else {
            "https://www.disneyplus.com/$segment/$clean"
        }
    }

    internal fun normalizeHuluUrl(raw: String, isMovie: Boolean): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        val segment = if (isMovie) "movie" else "series"
        return if (clean.startsWith("movie/") || clean.startsWith("series/")) {
            "https://www.hulu.com/$clean"
        } else {
            "https://www.hulu.com/$segment/$clean"
        }
    }

    internal fun normalizeMaxUrl(raw: String): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return "https://play.max.com/$clean"
    }

    internal fun normalizeAppleTvUrl(raw: String, isMovie: Boolean): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        val segment = if (isMovie) "movie" else "show"
        return if (clean.startsWith("movie/") || clean.startsWith("show/")) {
            "https://tv.apple.com/$clean"
        } else {
            "https://tv.apple.com/$segment/$clean"
        }
    }

    internal fun normalizePeacockUrl(raw: String): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return "https://www.peacocktv.com/$clean"
    }

    internal fun normalizeCrunchyrollUrl(raw: String): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return if (clean.startsWith("series/") || clean.startsWith("watch/")) {
            "https://www.crunchyroll.com/$clean"
        } else {
            "https://www.crunchyroll.com/series/$clean"
        }
    }

    internal fun normalizeBbcUrl(raw: String): String? {
        val clean = raw.trim().trimStart('/')
        if (clean.isBlank()) return null
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return "https://www.bbc.co.uk/programmes/$clean"
    }

    internal fun slugifyTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }

    private fun normalizeTitleForCompare(title: String): String =
        title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

    private fun queryWikidataDeepLinks(
        tmdbId: Long?,
        imdbId: String?,
        title: String,
        isMovie: Boolean,
        regionCode: String
    ): Map<String, String> {
        if (tmdbId == null && imdbId.isNullOrBlank()) return emptyMap()
        return try {
            val sparql = buildWikidataSparqlQuery(
                tmdbId = tmdbId,
                isMovie = isMovie,
                imdbId = imdbId
            )
            val encodedQuery = URLEncoder.encode(sparql, "UTF-8")
            val req = Request.Builder()
                .url("$WIKIDATA_SPARQL_ENDPOINT?format=json&query=$encodedQuery")
                .header("Accept", "application/sparql-results+json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyMap()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return emptyMap()
                parseWikidataBindings(
                    jsonBody = body,
                    title = title,
                    isMovie = isMovie,
                    regionCode = regionCode
                )
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun queryAppleItunesUrl(
        title: String,
        isMovie: Boolean,
        releaseYear: Int,
        regionCode: String
    ): String? {
        return try {
            val encodedTerm = URLEncoder.encode(title.trim(), "UTF-8")
            val mediaParam = if (isMovie) "movie" else "tvShow"
            val countryParam = regionCode.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
            val url = "$ITUNES_SEARCH_ENDPOINT?term=$encodedTerm&media=$mediaParam&country=$countryParam&limit=5"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return null
                parseItunesSearchResponse(
                    jsonBody = body,
                    title = title,
                    isMovie = isMovie,
                    releaseYear = releaseYear
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
