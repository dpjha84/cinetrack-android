package com.example.util

import java.net.URLDecoder

/**
 * WatchPilot v2.0 Policy-Compliant Link Helper.
 * Never scrapes third-party HTML pages; streaming availability is sourced strictly
 * via the official TMDB watch/providers API (powered by JustWatch) and launched
 * via WatchProviderLauncher intent / deep-link resolution.
 */
object JustWatchScraper {

    fun extractDirectUrlFromClickout(clickoutUrl: String?): String? {
        if (clickoutUrl.isNullOrBlank()) return null
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
