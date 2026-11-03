package com.example.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WatchProviderLauncher {

    /**
     * App-First Watch Provider Launcher with JustWatch Direct Deeplink Scraping & Episode Support.
     *
     * If the streaming app (Netflix, Prime Video, Hotstar, JioCinema, Disney+, YouTube, etc.)
     * is installed on the user's Android phone:
     * 1. Uses the direct scraped JustWatch content URL (e.g. netflix.com/title/80057281) to bypass
     *    the provider's home page and land straight on that movie or series playback screen.
     * 2. If season and episode are specified (e.g. S2:E4), directly targets that specific episode.
     * 3. If direct link is unavailable, uses package-locked search or launches the installed app directly.
     * 4. Only if the app is NOT installed does it fall back to the web browser.
     */
    fun launchProvider(
        context: Context,
        providerName: String,
        title: String,
        releaseYear: Int = 0,
        isMovie: Boolean = true,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        webLink: String? = null,
        directContentUrl: String? = null
    ) {
        val cleanName = providerName.trim()
        val hasEpisode = seasonNumber != null && episodeNumber != null
        val episodeSuffix = if (hasEpisode) " Season $seasonNumber Episode $episodeNumber" else ""
        val queryText = "$title$episodeSuffix" + if (releaseYear > 1900 && !hasEpisode) " $releaseYear" else ""

        val encodedTitle = URLEncoder.encode(queryText, "UTF-8")
        val simpleEncodedTitle = URLEncoder.encode("$title$episodeSuffix", "UTF-8")
        val encodedProvider = URLEncoder.encode(cleanName, "UTF-8")
        val lowerName = cleanName.lowercase()
        val pm = context.packageManager

        // 1. Resolve targeted package candidates and deep link URIs for this provider
        val targetConfig = resolveProviderConfig(
            lowerName = lowerName,
            cleanName = cleanName,
            title = title,
            simpleEncodedTitle = simpleEncodedTitle,
            encodedTitle = encodedTitle,
            encodedProvider = encodedProvider,
            webLink = webLink,
            directContentUrl = directContentUrl,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )

        // 2. CHECK IF THE APP IS INSTALLED ON THE DEVICE FIRST!
        var installedPackage: String? = null
        for (pkg in targetConfig.packageNames) {
            try {
                if (pm.getLaunchIntentForPackage(pkg) != null) {
                    installedPackage = pkg
                    break
                }
            } catch (_: Exception) {
                // Ignore and continue checking
            }
        }

        // =========================================================================
        // SCENARIO A: APP IS INSTALLED -> STRICTLY OPEN THE APP (NEVER OPEN BROWSER)
        // =========================================================================
        if (installedPackage != null) {
            var appLaunched = false

            // Attempt 1: If specific episode is requested, prioritize targeted episode deep link URIs first
            // (e.g. nflx://www.netflix.com/watch/$id?episode=$ep&season=$season)
            if (hasEpisode) {
                for (schemeUri in targetConfig.appSchemeUris) {
                    try {
                        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(schemeUri)).apply {
                            setPackage(installedPackage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (appIntent.resolveActivity(pm) != null) {
                            context.startActivity(appIntent)
                            appLaunched = true
                            break
                        }
                    } catch (_: Exception) {
                        // Try next scheme
                    }
                }
            }

            // Attempt 2: Scraped Direct Content URL locked directly to the installed app
            if (!appLaunched && !directContentUrl.isNullOrBlank()) {
                try {
                    val directIntent = Intent(Intent.ACTION_VIEW, Uri.parse(directContentUrl)).apply {
                        setPackage(installedPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (directIntent.resolveActivity(pm) != null) {
                        context.startActivity(directIntent)
                        appLaunched = true
                    }
                } catch (_: Exception) {
                    // Try next
                }
            }

            // Attempt 3: Targeted App Scheme Deep Links (e.g. nflx://www.netflix.com/title/...)
            if (!appLaunched) {
                for (schemeUri in targetConfig.appSchemeUris) {
                    try {
                        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(schemeUri)).apply {
                            setPackage(installedPackage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (appIntent.resolveActivity(pm) != null) {
                            context.startActivity(appIntent)
                            appLaunched = true
                            break
                        }
                    } catch (_: Exception) {
                        // Try next scheme
                    }
                }
            }

            // Attempt 3: HTTPS Content Search URL locked strictly to the installed app package
            if (!appLaunched) {
                for (webUrl in targetConfig.webSearchUrls) {
                    try {
                        val appWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                            setPackage(installedPackage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (appWebIntent.resolveActivity(pm) != null) {
                            context.startActivity(appWebIntent)
                            appLaunched = true
                            break
                        }
                    } catch (_: Exception) {
                        // Try next
                    }
                }
            }

            // Attempt 4: Native Android Search Intent locked to the installed app package
            if (!appLaunched) {
                try {
                    val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage(installedPackage)
                        putExtra(SearchManager.QUERY, queryText)
                        putExtra("query", queryText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (searchIntent.resolveActivity(pm) != null) {
                        context.startActivity(searchIntent)
                        appLaunched = true
                    }
                } catch (_: Exception) {
                    // Try next
                }
            }

            // Attempt 5: Direct Launch Intent of the installed app
            // Guaranteed fallback when app is installed: launches app directly without opening browser!
            if (!appLaunched) {
                try {
                    val launchIntent = pm.getLaunchIntentForPackage(installedPackage)?.apply {
                        putExtra(SearchManager.QUERY, queryText)
                        putExtra("query", queryText)
                        if (seasonNumber != null) putExtra("season", seasonNumber)
                        if (episodeNumber != null) putExtra("episode", episodeNumber)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        appLaunched = true
                    }
                } catch (_: Exception) {
                    // Fallthrough
                }
            }

            if (appLaunched) {
                return
            }
        }

        // =========================================================================
        // SCENARIO B: APP NOT INSTALLED -> FALL BACK TO DIRECT LINK / WEB / GOOGLE
        // =========================================================================
        var webLaunched = false

        // Attempt 1: Direct scraped JustWatch content URL in browser
        if (!directContentUrl.isNullOrBlank()) {
            try {
                val directIntent = Intent(Intent.ACTION_VIEW, Uri.parse(directContentUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(directIntent)
                webLaunched = true
            } catch (_: Exception) {
                // Next
            }
        }

        // Attempt 2: Provider's official web search page
        if (!webLaunched) {
            for (webUrl in targetConfig.webSearchUrls) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    webLaunched = true
                    break
                } catch (_: Exception) {
                    // Try next
                }
            }
        }

        // Attempt 3: JustWatch web link if provided
        if (!webLaunched && !webLink.isNullOrBlank()) {
            try {
                val justWatchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(justWatchIntent)
                webLaunched = true
            } catch (_: Exception) {
                // Next
            }
        }

        // Attempt 4: Google streaming search fallback
        if (!webLaunched) {
            try {
                val googleUrl = "https://www.google.com/search?q=watch+$encodedTitle+on+$encodedProvider"
                val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(searchIntent)
                webLaunched = true
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open $cleanName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveProviderConfig(
        lowerName: String,
        cleanName: String,
        title: String,
        simpleEncodedTitle: String,
        encodedTitle: String,
        encodedProvider: String,
        webLink: String?,
        directContentUrl: String?,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): ProviderConfig {
        return when {
            lowerName.contains("netflix") -> {
                // Extract Netflix ID if present in direct content URL (e.g. netflix.com/title/80057281)
                val netflixId = directContentUrl?.let { url ->
                    Regex("""(?:title|watch)/(\d+)""").find(url)?.groupValues?.get(1)
                }

                val netflixSchemes = mutableListOf<String>()
                if (netflixId != null) {
                    if (seasonNumber != null && episodeNumber != null) {
                        netflixSchemes.add("nflx://www.netflix.com/watch/$netflixId?episode=$episodeNumber&season=$seasonNumber")
                    }
                    netflixSchemes.add("nflx://www.netflix.com/watch/$netflixId")
                    netflixSchemes.add("nflx://www.netflix.com/title/$netflixId")
                    netflixSchemes.add("https://www.netflix.com/title/$netflixId")
                }
                netflixSchemes.add("nflx://www.netflix.com/search?q=$simpleEncodedTitle")
                netflixSchemes.add("https://www.netflix.com/search?q=$simpleEncodedTitle")

                ProviderConfig(
                    packageNames = listOf("com.netflix.mediaclient"),
                    appSchemeUris = netflixSchemes,
                    webSearchUrls = if (netflixId != null) {
                        listOf("https://www.netflix.com/title/$netflixId", "https://www.netflix.com/search?q=$simpleEncodedTitle")
                    } else {
                        listOf("https://www.netflix.com/search?q=$simpleEncodedTitle")
                    }
                )
            }

            lowerName.contains("prime") || lowerName.contains("amazon") -> {
                val primeSchemes = mutableListOf<String>()
                if (!directContentUrl.isNullOrBlank()) {
                    primeSchemes.add(directContentUrl)
                }
                primeSchemes.add("primevideo://search?phrase=$simpleEncodedTitle")
                primeSchemes.add("https://app.primevideo.com/search?phrase=$simpleEncodedTitle")
                primeSchemes.add("https://www.amazon.com/gp/video/search?phrase=$simpleEncodedTitle")

                ProviderConfig(
                    packageNames = listOf(
                        "com.amazon.avod.thirdpartyclient",
                        "com.amazon.amazonvideo.livingroom"
                    ),
                    appSchemeUris = primeSchemes,
                    webSearchUrls = listOfNotNull(
                        directContentUrl,
                        "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$simpleEncodedTitle",
                        "https://www.amazon.com/gp/video/search?phrase=$simpleEncodedTitle"
                    )
                )
            }

            lowerName.contains("hotstar") -> {
                val hotstarSchemes = mutableListOf<String>()
                if (!directContentUrl.isNullOrBlank()) {
                    hotstarSchemes.add(directContentUrl)
                }
                hotstarSchemes.add("hotstar://search?q=$simpleEncodedTitle")
                hotstarSchemes.add("https://www.hotstar.com/in/search?q=$simpleEncodedTitle")
                hotstarSchemes.add("https://www.hotstar.com/search?q=$simpleEncodedTitle")

                ProviderConfig(
                    packageNames = listOf("in.startv.hotstar", "com.hotstar.hotstar"),
                    appSchemeUris = hotstarSchemes,
                    webSearchUrls = listOfNotNull(
                        directContentUrl,
                        "https://www.hotstar.com/in/search?q=$simpleEncodedTitle",
                        "https://www.hotstar.com/search?q=$simpleEncodedTitle"
                    )
                )
            }

            lowerName.contains("disney") -> {
                val disneySchemes = mutableListOf<String>()
                if (!directContentUrl.isNullOrBlank()) {
                    disneySchemes.add(directContentUrl)
                }
                disneySchemes.add("disneyplus://search?q=$simpleEncodedTitle")
                disneySchemes.add("https://www.disneyplus.com/search?q=$simpleEncodedTitle")
                disneySchemes.add("hotstar://search?q=$simpleEncodedTitle")

                ProviderConfig(
                    packageNames = listOf("com.disney.disneyplus", "in.startv.hotstar"),
                    appSchemeUris = disneySchemes,
                    webSearchUrls = listOfNotNull(
                        directContentUrl,
                        "https://www.disneyplus.com/search?q=$simpleEncodedTitle",
                        "https://www.hotstar.com/in/search?q=$simpleEncodedTitle"
                    )
                )
            }

            lowerName.contains("jio") -> ProviderConfig(
                packageNames = listOf("com.jio.media.ondemand"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "jiocinema://search?query=$simpleEncodedTitle",
                    "https://www.jiocinema.com/search/$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.jiocinema.com/search/$simpleEncodedTitle"
                )
            )

            lowerName.contains("zee5") -> ProviderConfig(
                packageNames = listOf("com.graymatrix.did"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "zee5://search?q=$simpleEncodedTitle",
                    "https://www.zee5.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.zee5.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("sonyliv") || lowerName.contains("sony liv") -> ProviderConfig(
                packageNames = listOf("com.sonyliv"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "sonyliv://search?q=$simpleEncodedTitle",
                    "https://www.sonyliv.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.sonyliv.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("youtube") -> ProviderConfig(
                packageNames = listOf("com.google.android.youtube"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "vnd.youtube://results?search_query=$encodedTitle+movie",
                    "https://www.youtube.com/results?search_query=$encodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.youtube.com/results?search_query=$encodedTitle"
                )
            )

            lowerName.contains("google play") || lowerName.contains("google tv") -> ProviderConfig(
                packageNames = listOf("com.google.android.videos"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://play.google.com/store/search?q=$encodedTitle&c=movies",
                    "market://search?q=$encodedTitle&c=movies"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://play.google.com/store/search?q=$encodedTitle&c=movies"
                )
            )

            lowerName.contains("apple tv") -> ProviderConfig(
                packageNames = listOf(
                    "com.apple.atve.androidtv.appletv",
                    "com.apple.atve.sony.appletv"
                ),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://tv.apple.com/search?term=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://tv.apple.com/search?term=$simpleEncodedTitle"
                )
            )

            lowerName.contains("max") || lowerName.contains("hbo") -> ProviderConfig(
                packageNames = listOf("com.wbd.stream", "com.hbo.hbonow", "com.hbo.max"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://play.max.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://play.max.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("hulu") -> ProviderConfig(
                packageNames = listOf("com.hulu.plus"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "hulu://search?query=$simpleEncodedTitle",
                    "https://www.hulu.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.hulu.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("peacock") -> ProviderConfig(
                packageNames = listOf("com.peacocktv.peacockandroid"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://www.peacocktv.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.peacocktv.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("paramount") -> ProviderConfig(
                packageNames = listOf("com.cbs.app"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://www.paramountplus.com/search/?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.paramountplus.com/search/?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("crunchyroll") -> ProviderConfig(
                packageNames = listOf("com.crunchyroll.crunchyroid"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "crunchyroll://search?q=$simpleEncodedTitle",
                    "https://www.crunchyroll.com/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.crunchyroll.com/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("tubi") -> ProviderConfig(
                packageNames = listOf("com.tubitv"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "tubitv://search/$simpleEncodedTitle",
                    "https://tubitv.com/search/$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://tubitv.com/search/$simpleEncodedTitle"
                )
            )

            lowerName.contains("pluto") -> ProviderConfig(
                packageNames = listOf("tv.pluto.android"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "plutotv://search?q=$simpleEncodedTitle",
                    "https://pluto.tv/search/details?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://pluto.tv/search/details?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("aha") -> ProviderConfig(
                packageNames = listOf("aha.ott.video.movies"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://www.aha.video/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.aha.video/search?q=$simpleEncodedTitle"
                )
            )

            lowerName.contains("mx player") -> ProviderConfig(
                packageNames = listOf("com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.pro"),
                appSchemeUris = listOfNotNull(
                    directContentUrl,
                    "https://www.mxplayer.in/search?q=$simpleEncodedTitle"
                ),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    "https://www.mxplayer.in/search?q=$simpleEncodedTitle"
                )
            )

            else -> ProviderConfig(
                packageNames = emptyList(),
                appSchemeUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    directContentUrl,
                    webLink,
                    "https://www.google.com/search?q=watch+$encodedTitle+on+$encodedProvider"
                )
            )
        }
    }

    private data class ProviderConfig(
        val packageNames: List<String>,
        val appSchemeUris: List<String>,
        val webSearchUrls: List<String>
    )
}
