package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object JustWatchScraper {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val clickoutRegex = Pattern.compile(
        """href=["'](https://click\.justwatch\.com/a\?[^"']+)["'][^>]*title=["'][^"']*on\s+([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val fallbackClickoutRegex = Pattern.compile(
        """click\.justwatch\.com/a\?[^"'\s&]+&r=([^"'\s&]+)""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Scrapes the JustWatch/watch page to extract direct content URLs for each provider.
     * Returns a map of providerName.lowercase() -> directContentUrl (e.g. "netflix" -> "https://www.netflix.com/title/80057281").
     */
    suspend fun scrapeDirectContentUrls(watchUrl: String): Map<String, String> = withContext(Dispatchers.IO) {
        if (watchUrl.isBlank()) return@withContext emptyMap()
        val directMap = mutableMapOf<String, String>()

        try {
            val request = Request.Builder()
                .url(watchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return@withContext emptyMap()

            val matcher = clickoutRegex.matcher(html)
            while (matcher.find()) {
                val fullUrl = matcher.group(1).orEmpty()
                val providerName = matcher.group(2).orEmpty().trim().lowercase()

                val directUrl = extractDirectUrlFromClickout(fullUrl)
                if (directUrl != null && providerName.isNotBlank() && !directMap.containsKey(providerName)) {
                    directMap[providerName] = directUrl
                }
            }

            // Fallback scan across all clickout links in the page
            val fallbackMatcher = fallbackClickoutRegex.matcher(html)
            while (fallbackMatcher.find()) {
                val encodedR = fallbackMatcher.group(1).orEmpty()
                try {
                    val decoded = URLDecoder.decode(encodedR, "UTF-8")
                    if (decoded.startsWith("http")) {
                        when {
                            decoded.contains("netflix.com") && !directMap.containsKey("netflix") -> directMap["netflix"] = decoded
                            (decoded.contains("amazon.com") || decoded.contains("primevideo.com")) && !directMap.containsKey("amazon prime video") -> {
                                directMap["amazon prime video"] = decoded
                                directMap["prime video"] = decoded
                                directMap["amazon video"] = decoded
                            }
                            decoded.contains("hotstar.com") && !directMap.containsKey("hotstar") -> {
                                directMap["hotstar"] = decoded
                                directMap["disney+ hotstar"] = decoded
                            }
                            decoded.contains("disneyplus.com") && !directMap.containsKey("disney plus") -> {
                                directMap["disney plus"] = decoded
                                directMap["disney+"] = decoded
                            }
                            decoded.contains("play.google.com") && !directMap.containsKey("google play movies") -> {
                                directMap["google play movies"] = decoded
                                directMap["google tv"] = decoded
                            }
                            decoded.contains("youtube.com") && !directMap.containsKey("youtube") -> directMap["youtube"] = decoded
                            (decoded.contains("max.com") || decoded.contains("hbo")) && !directMap.containsKey("max") -> directMap["max"] = decoded
                            decoded.contains("hulu.com") && !directMap.containsKey("hulu") -> directMap["hulu"] = decoded
                            decoded.contains("jiocinema.com") && !directMap.containsKey("jiocinema") -> directMap["jiocinema"] = decoded
                            decoded.contains("zee5.com") && !directMap.containsKey("zee5") -> directMap["zee5"] = decoded
                            decoded.contains("sonyliv.com") && !directMap.containsKey("sonyliv") -> directMap["sonyliv"] = decoded
                            decoded.contains("apple.com") && !directMap.containsKey("apple tv") -> directMap["apple tv"] = decoded
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Network fallback gracefully caught
        }

        directMap
    }

    private fun extractDirectUrlFromClickout(clickoutUrl: String): String? {
        val rParam = clickoutUrl.substringAfter("&r=", "").substringBefore("&").ifEmpty {
            clickoutUrl.substringAfter("?r=", "").substringBefore("&")
        }
        if (rParam.isBlank()) return null
        return try {
            val decoded = URLDecoder.decode(rParam, "UTF-8")
            if (decoded.startsWith("http")) decoded else null
        } catch (_: Exception) {
            null
        }
    }
}
