package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale

object WatchProviderLauncher {

    private val hotstarMarkets = setOf("IN", "ID", "MY", "TH", "SG", "PH")

    /**
     * All known OTT/streaming package IDs so the Scenario B browser resolver never
     * accidentally routes a worst-case web fallback back into a streaming app router.
     */
    private val knownStreamingPackages = setOf(
        "com.netflix.mediaclient",
        "com.netflix.ninja",
        "com.amazon.avod.thirdpartyclient",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "in.startv.hotstar",
        "com.hotstar.hotstar",
        "com.apple.atve.androidtv.appletv",
        "com.apple.atve.sony.appletv",
        "com.google.android.youtube",
        "com.google.android.videos",
        "com.wbd.stream",
        "com.hbo.hbonow",
        "com.hbo.max",
        "com.hulu.plus",
        "com.peacocktv.peacockandroid",
        "com.cbs.app",
        "com.crunchyroll.crunchyroid",
        "com.tubitv",
        "tv.pluto.android",
        "com.plexapp.android",
        "com.mubi",
        "com.viki.android",
        "com.discovery.discoveryplus.mobile",
        "com.discoveryplus.mobile.android",
        "com.discovery.discoveryplus.androidtv",
        "ca.bellmedia.cravetv",
        "com.globo.globotv",
        "com.univision.prendetv",
        "com.jio.media.ondemand",
        "com.graymatrix.did",
        "com.sonyliv",
        "aha.ott.video.movies",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro",
        "com.suntv.sunnxt",
        "com.viewlift.hoichoi",
        "com.lionsgateplay.videoapp",
        "bbc.iplayer.android",
        "air.ITVMobilePlayer",
        "com.channel4.ondemand",
        "com.nowtv.uk",
        "com.bskyb.skygo",
        "com.canalplus.canalplus",
        "de.prosiebensat1digital.seventv",
        "com.skyshowtime.skyshowtime.google",
        "es.plus.yomvi",
        "com.movistarplus.androidtv",
        "tv.wuaki",
        "tv.wuaki.apptv",
        "au.com.stan.and",
        "au.com.streamotion.ares",
        "jp.unext.mediaplayer",
        "jp.co.rakuten.video.showtimeplayer",
        "net.cj.cjhv.gs.tving",
        "com.coupang.mobile.play",
        "kr.co.captv.pooqV2"
    )

    /**
     * Global App-First + Browser-Guaranteed Watch Provider Launcher.
     *
     * Strategy across ALL streaming providers and ALL countries:
     * 1. SCENARIO A (App installed on device):
     *    - Try verified directContentUrl locked to the installed app (when available).
     *    - Try verified in-app search App Links ONLY for apps whose native routers reliably
     *      handle clean title search (Netflix, Prime Video, YouTube, Google TV).
     *    - For every other installed OTT app (JioHotstar, Disney+, Zee5, SonyLIV, BBC iPlayer,
     *      ITVX, Stan, Crave, Globoplay, ViX, U-NEXT, TVING, Canal+, Joyn, Max, Hulu, Apple TV+,
     *      etc.), launch the native app cleanly via its launcher intent + search hint toast.
     *      Never sends unverified custom schemes or guessed `/search` paths that trigger in-app
     *      "Something went wrong" error screens.
     * 2. SCENARIO B (App not installed, regional/unknown provider, or app launch failed):
     *    - Routes cleanly to an actual Web Browser (`findWebBrowserPackage`) using sanitized
     *      HTTPS URLs in priority order:
     *      (a) directContentUrl (if HTTPS)
     *      (b) localized official provider web search URL (`regionCode`-aware)
     *      (c) localized TMDB / JustWatch watch link (`webLink`)
     *      (d) universal Google Watch Search fallback
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
        directContentUrl: String? = null,
        regionCode: String = "US"
    ) {
        val cleanName = providerName.trim()
        val cleanTitle = title.trim().ifBlank { cleanName }
        val pm = context.packageManager

        val targetConfig = resolveProviderConfig(
            providerName = cleanName,
            title = cleanTitle,
            releaseYear = releaseYear,
            webLink = webLink,
            directContentUrl = directContentUrl,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            regionCode = regionCode
        )

        // 1. Detect installed streaming app package on device
        var installedPackage: String? = null
        for (pkg in targetConfig.packageNames) {
            try {
                if (pm.getLaunchIntentForPackage(pkg) != null) {
                    installedPackage = pkg
                    break
                }
            } catch (_: Exception) {
                // Continue checking fallback package IDs
            }
        }

        // =========================================================================
        // SCENARIO A: APP IS INSTALLED -> TRY DIRECT TITLE OR IN-APP SEARCH FIRST
        // =========================================================================
        if (installedPackage != null) {
            var appLaunched = false

            // Attempt 1: Verified In-App Title / Episode / Search URIs locked to the installed app
            for (deepLinkUri in targetConfig.verifiedInAppUris) {
                if (tryStartViewIntent(context, pm, deepLinkUri, installedPackage)) {
                    appLaunched = true
                    break
                }
            }

            // Attempt 2: Direct Content URL (if not already tried via verifiedInAppUris)
            if (!appLaunched && !directContentUrl.isNullOrBlank()) {
                appLaunched = tryStartViewIntent(
                    context = context,
                    pm = pm,
                    uriString = directContentUrl,
                    targetPackage = installedPackage
                )
            }

            if (appLaunched) {
                return
            }
        }

        // =========================================================================
        // SCENARIO B: TITLE-TARGETED WEB HANDOFF (when no direct title / in-app search
        // exists, or app is not installed) -> Avoid dumping user on app homepage!
        // =========================================================================
        val browserPackage = findWebBrowserPackage(pm)
        val uriTitle = encodeUriComponent(cleanTitle)
        val formTitle = encodeFormParam(cleanTitle)
        val googleFallback = buildGoogleWatchSearchUrl(
            cleanName = cleanName,
            cleanTitle = cleanTitle,
            releaseYear = releaseYear
        )
        val safeDirectWeb = safeHttpUrl(directContentUrl)
        val safeWebLink = safeHttpUrl(webLink)

        val rawWebCandidates = buildList {
            safeDirectWeb?.let { add(it) }
            targetConfig.webSearchUrls.forEach { url -> safeHttpUrl(url)?.let { add(it) } }
            safeWebLink?.let { add(it) }
            add(googleFallback)
        }.distinct()

        // Always prioritize URLs that target this exact title (direct URL, provider search query,
        // TMDB/JustWatch watch link, or Google "watch <title> on <provider>") ahead of bare homepages.
        val (titleTargetedUrls, bareHomepageUrls) = rawWebCandidates.partition { url ->
            isTitleTargetedWebUrl(
                url = url,
                safeDirectWeb = safeDirectWeb,
                safeWebLink = safeWebLink,
                uriTitle = uriTitle,
                formTitle = formTitle
            )
        }
        val candidateWebUrls = (titleTargetedUrls + bareHomepageUrls).distinct()

        for (webUrl in candidateWebUrls) {
            // Prefer targeting the resolved web browser directly so an installed OTT app cannot
            // intercept an unverified /search URL and throw an in-app "Something went wrong" dialog.
            if (browserPackage != null &&
                tryStartViewIntent(
                    context = context,
                    pm = pm,
                    uriString = webUrl,
                    targetPackage = browserPackage,
                    addBrowsable = true
                )
            ) {
                return
            }
            if (tryStartViewIntent(
                    context = context,
                    pm = pm,
                    uriString = webUrl,
                    targetPackage = null,
                    addBrowsable = true
                )
            ) {
                return
            }
        }

        // Last-resort safety net if no browser is available on device but the native app is installed
        if (installedPackage != null) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(installedPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    val hintTitle = if (cleanTitle.equals(cleanName, ignoreCase = true)) {
                        "Opening $cleanName"
                    } else {
                        "Opening $cleanName — search \"$cleanTitle\""
                    }
                    Toast.makeText(context, hintTitle, Toast.LENGTH_SHORT).show()
                    context.startActivity(launchIntent)
                    return
                }
            } catch (_: Exception) {
                // Fall through to unavailable toast
            }
        }

        val displayTarget = cleanName.ifBlank { "streaming page" }
        Toast.makeText(context, "Unable to open $displayTarget in browser", Toast.LENGTH_SHORT).show()
    }

    internal fun isTitleTargetedWebUrl(
        url: String,
        safeDirectWeb: String?,
        safeWebLink: String?,
        uriTitle: String,
        formTitle: String
    ): Boolean {
        if (url == safeDirectWeb || url == safeWebLink) return true
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("google.com/search?")) return true
        if (uriTitle.isNotBlank() && url.contains(uriTitle, ignoreCase = true)) return true
        if (formTitle.isNotBlank() && url.contains(formTitle, ignoreCase = true)) return true
        return false
    }

    private fun findWebBrowserPackage(pm: PackageManager): String? {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        fun isEligibleBrowser(pkg: String?): Boolean {
            if (pkg.isNullOrBlank()) return false
            if (pkg == "android" || pkg.contains("resolver", ignoreCase = true)) return false
            if (pkg in knownStreamingPackages) return false
            return true
        }

        try {
            val resolvedDefault = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
            val defaultPkg = resolvedDefault?.activityInfo?.packageName
            if (isEligibleBrowser(defaultPkg)) {
                return defaultPkg
            }
        } catch (_: Exception) {
            // Fall back to full query below
        }

        return try {
            val candidates = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            candidates
                .mapNotNull { it.activityInfo?.takeIf { info -> info.exported }?.packageName }
                .firstOrNull { isEligibleBrowser(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryStartViewIntent(
        context: Context,
        pm: PackageManager,
        uriString: String,
        targetPackage: String?,
        addBrowsable: Boolean = false
    ): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                if (addBrowsable) {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                if (!targetPackage.isNullOrBlank()) {
                    setPackage(targetPackage)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!targetPackage.isNullOrBlank() && intent.resolveActivity(pm) == null) {
                return false
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    internal fun safeHttpUrl(url: String?): String? {
        val trimmed = url?.trim() ?: return null
        return if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            trimmed
        } else {
            null
        }
    }

    internal fun buildGoogleWatchSearchUrl(
        cleanName: String,
        cleanTitle: String,
        releaseYear: Int = 0
    ): String {
        val titleWithYear = if (releaseYear > 1900) "$cleanTitle $releaseYear" else cleanTitle
        val encodedWithYear = encodeFormParam(titleWithYear)
        return if (cleanName.isBlank() || cleanName.equals("Streaming Provider", ignoreCase = true)) {
            "https://www.google.com/search?q=watch+$encodedWithYear"
        } else {
            val encodedProvider = encodeFormParam(cleanName)
            "https://www.google.com/search?q=watch+$encodedWithYear+on+$encodedProvider"
        }
    }

    internal fun encodeUriComponent(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")

    internal fun encodeFormParam(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8")

    internal fun resolveProviderConfig(
        providerName: String,
        title: String,
        releaseYear: Int = 0,
        webLink: String? = null,
        directContentUrl: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        regionCode: String = "US"
    ): ProviderConfig {
        val cleanName = providerName.trim()
        val lowerName = cleanName.lowercase(Locale.ROOT)
        val upperRegion = regionCode.trim().uppercase(Locale.ROOT).ifBlank { "US" }
        val cleanTitle = title.trim().ifBlank { cleanName }
        val uriTitle = encodeUriComponent(cleanTitle)
        val formTitle = encodeFormParam(cleanTitle)
        val formTitleWithYear = encodeFormParam(
            if (releaseYear > 1900) "$cleanTitle $releaseYear" else cleanTitle
        )
        val googleFallback = buildGoogleWatchSearchUrl(
            cleanName = cleanName,
            cleanTitle = cleanTitle,
            releaseYear = releaseYear
        )
        val safeDirectWeb = safeHttpUrl(directContentUrl)
        val safeWebLink = safeHttpUrl(webLink)

        return when {
            lowerName.contains("netflix") -> {
                val netflixId = directContentUrl?.let { url ->
                    Regex("""(?:title|watch)/(\d+)""").find(url)?.groupValues?.get(1)
                }

                val netflixInApp = mutableListOf<String>()
                if (netflixId != null) {
                    if (seasonNumber != null && episodeNumber != null) {
                        netflixInApp.add("nflx://www.netflix.com/watch/$netflixId?episode=$episodeNumber&season=$seasonNumber")
                    }
                    netflixInApp.add("nflx://www.netflix.com/watch/$netflixId")
                    netflixInApp.add("nflx://www.netflix.com/title/$netflixId")
                    netflixInApp.add("https://www.netflix.com/title/$netflixId")
                }
                // Netflix Android reliably opens in-app search results via HTTPS App Link with clean title
                netflixInApp.add("https://www.netflix.com/search?q=$uriTitle")

                ProviderConfig(
                    packageNames = listOf("com.netflix.mediaclient", "com.netflix.ninja"),
                    verifiedInAppUris = netflixInApp,
                    webSearchUrls = if (netflixId != null) {
                        listOfNotNull(
                            safeDirectWeb,
                            "https://www.netflix.com/title/$netflixId",
                            "https://www.netflix.com/search?q=$uriTitle"
                        ).distinct()
                    } else {
                        listOfNotNull(
                            safeDirectWeb,
                            "https://www.netflix.com/search?q=$uriTitle"
                        ).distinct()
                    }
                )
            }

            // Catch Amazon Prime Video, Amazon Video, and "* Amazon Channel" add-ons (US/UK/DE/JP/IN/Global)
            lowerName.contains("prime") || lowerName.contains("amazon") -> {
                val primeInApp = mutableListOf<String>()
                if (!directContentUrl.isNullOrBlank()) {
                    primeInApp.add(directContentUrl)
                    val asin = Regex("""(?:asin=|gti=|detail/|dp/)([A-Z0-9]{10})""")
                        .find(directContentUrl)
                        ?.groupValues
                        ?.getOrNull(1)
                    if (!asin.isNullOrBlank()) {
                        primeInApp.add("https://www.primevideo.com/dp/$asin")
                        primeInApp.add("amzn://video/detail/$asin")
                    }
                }
                // Prime Video Android uses HTTPS App Links on app.primevideo.com globally
                primeInApp.add("https://app.primevideo.com/search?phrase=$uriTitle")

                val regionalAmazonWeb = when (upperRegion) {
                    "US" -> "https://www.amazon.com/s?k=$formTitle&i=instant-video"
                    "GB" -> "https://www.amazon.co.uk/s?k=$formTitle&i=instant-video"
                    "DE" -> "https://www.amazon.de/s?k=$formTitle&i=instant-video"
                    "JP" -> "https://www.amazon.co.jp/s?k=$formTitle&i=instant-video"
                    "IN" -> "https://www.amazon.in/s?k=$formTitle&i=instant-video"
                    else -> "https://www.amazon.com/gp/video/search?phrase=$uriTitle"
                }

                ProviderConfig(
                    packageNames = listOf(
                        "com.amazon.avod.thirdpartyclient",
                        "com.amazon.amazonvideo.livingroom"
                    ),
                    verifiedInAppUris = primeInApp.distinct(),
                    webSearchUrls = listOfNotNull(
                        safeDirectWeb,
                        "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$uriTitle",
                        regionalAmazonWeb
                    ).distinct()
                )
            }

            lowerName.contains("hotstar") -> {
                // JioHotstar / Hotstar X (in.startv.hotstar) intercepts hotstar://* and /in/search?q=...
                // and displays "Something went wrong" if given an unverified search route.
                // Launch the native app cleanly unless directContentUrl is verified; localize web path by country.
                val hotstarRegionPath = when {
                    upperRegion in hotstarMarkets -> upperRegion.lowercase(Locale.ROOT)
                    lowerName.contains("jiohotstar") || lowerName.contains("jio hotstar") -> "in"
                    else -> null
                }
                val hotstarSearchWeb = if (hotstarRegionPath != null) {
                    "https://www.hotstar.com/$hotstarRegionPath/explore?search_query=$uriTitle"
                } else {
                    "https://www.hotstar.com/explore?search_query=$uriTitle"
                }
                val hotstarHomeWeb = if (hotstarRegionPath != null) {
                    "https://www.hotstar.com/$hotstarRegionPath"
                } else {
                    "https://www.hotstar.com/"
                }
                ProviderConfig(
                    packageNames = listOf("in.startv.hotstar", "com.hotstar.hotstar"),
                    verifiedInAppUris = listOfNotNull(directContentUrl),
                    webSearchUrls = listOfNotNull(
                        safeDirectWeb,
                        hotstarSearchWeb,
                        hotstarHomeWeb
                    ).distinct()
                )
            }

            lowerName.contains("disney") -> {
                val isHotstarTerritory = upperRegion in setOf("IN", "ID", "MY", "TH")
                val packages = if (isHotstarTerritory) {
                    listOf("in.startv.hotstar", "com.hotstar.hotstar", "com.disney.disneyplus")
                } else {
                    listOf("com.disney.disneyplus", "in.startv.hotstar")
                }
                val webUrls = if (isHotstarTerritory) {
                    listOfNotNull(
                        safeDirectWeb,
                        "https://www.hotstar.com/${upperRegion.lowercase(Locale.ROOT)}/explore?search_query=$uriTitle",
                        "https://www.disneyplus.com/"
                    ).distinct()
                } else {
                    listOfNotNull(
                        safeDirectWeb,
                        "https://www.disneyplus.com/",
                        safeWebLink
                    ).distinct()
                }
                ProviderConfig(
                    packageNames = packages,
                    verifiedInAppUris = listOfNotNull(directContentUrl),
                    webSearchUrls = webUrls
                )
            }

            lowerName.contains("jio") -> ProviderConfig(
                packageNames = listOf("com.jio.media.ondemand", "in.startv.hotstar"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.hotstar.com/in/explore?search_query=$uriTitle",
                    "https://www.jiocinema.com/search/$uriTitle"
                ).distinct()
            )

            lowerName.contains("zee5") -> ProviderConfig(
                packageNames = listOf("com.graymatrix.did"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.zee5.com/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("sonyliv") || lowerName.contains("sony liv") -> ProviderConfig(
                packageNames = listOf("com.sonyliv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.sonyliv.com/search"
                ).distinct()
            )

            lowerName.contains("sun nxt") || lowerName.contains("sunnxt") -> ProviderConfig(
                packageNames = listOf("com.suntv.sunnxt"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.sunnxt.com/"
                ).distinct()
            )

            lowerName.contains("hoichoi") -> ProviderConfig(
                packageNames = listOf("com.viewlift.hoichoi"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.hoichoi.tv/"
                ).distinct()
            )

            lowerName.contains("lionsgate") -> ProviderConfig(
                packageNames = listOf("com.lionsgateplay.videoapp"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://lionsgateplay.com/"
                ).distinct()
            )

            lowerName == "aha" || lowerName.startsWith("aha ") || lowerName.contains("aha video") -> ProviderConfig(
                packageNames = listOf("aha.ott.video.movies"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.aha.video/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("mx player") -> ProviderConfig(
                packageNames = listOf("com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.pro"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.mxplayer.in/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("youtube") -> ProviderConfig(
                packageNames = listOf("com.google.android.youtube"),
                verifiedInAppUris = listOfNotNull(
                    directContentUrl,
                    "vnd.youtube://results?search_query=$formTitleWithYear+movie",
                    "https://www.youtube.com/results?search_query=$formTitleWithYear"
                ),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.youtube.com/results?search_query=$formTitleWithYear"
                ).distinct()
            )

            lowerName.contains("google play") || lowerName.contains("google tv") -> ProviderConfig(
                packageNames = listOf("com.google.android.videos"),
                verifiedInAppUris = listOfNotNull(
                    directContentUrl,
                    "https://play.google.com/store/search?q=$formTitleWithYear&c=movies",
                    "market://search?q=$formTitleWithYear&c=movies"
                ),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://play.google.com/store/search?q=$formTitleWithYear&c=movies"
                ).distinct()
            )

            lowerName.contains("apple tv") -> ProviderConfig(
                packageNames = listOf(
                    "com.apple.atve.androidtv.appletv",
                    "com.apple.atve.sony.appletv"
                ),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://tv.apple.com/search?term=$uriTitle"
                ).distinct()
            )

            lowerName == "max" || lowerName.startsWith("max ") || lowerName.contains("hbo") -> ProviderConfig(
                packageNames = listOf("com.wbd.stream", "com.hbo.hbonow", "com.hbo.max"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://play.max.com/"
                ).distinct()
            )

            lowerName.contains("hulu") -> ProviderConfig(
                packageNames = listOf("com.hulu.plus"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.hulu.com/"
                ).distinct()
            )

            lowerName.contains("peacock") -> ProviderConfig(
                packageNames = listOf("com.peacocktv.peacockandroid"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.peacocktv.com/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("paramount") -> ProviderConfig(
                packageNames = listOf("com.cbs.app"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.paramountplus.com/search/?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("crunchyroll") -> ProviderConfig(
                packageNames = listOf("com.crunchyroll.crunchyroid"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.crunchyroll.com/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("tubi") -> ProviderConfig(
                packageNames = listOf("com.tubitv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://tubitv.com/search/$uriTitle"
                ).distinct()
            )

            lowerName.contains("pluto") -> ProviderConfig(
                packageNames = listOf("tv.pluto.android"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://pluto.tv/search/details?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("plex") -> ProviderConfig(
                packageNames = listOf("com.plexapp.android"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://watch.plex.tv/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("mubi") -> ProviderConfig(
                packageNames = listOf("com.mubi"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://mubi.com/en/search?query=$uriTitle"
                ).distinct()
            )

            lowerName.contains("viki") -> ProviderConfig(
                packageNames = listOf("com.viki.android"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.viki.com/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("discovery") -> ProviderConfig(
                packageNames = listOf(
                    "com.discovery.discoveryplus.mobile",
                    "com.discoveryplus.mobile.android",
                    "com.discovery.discoveryplus.androidtv"
                ),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.discoveryplus.com/"
                ).distinct()
            )

            lowerName.contains("bbc iplayer") || lowerName == "iplayer" -> ProviderConfig(
                packageNames = listOf("bbc.iplayer.android"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.bbc.co.uk/iplayer/search?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("itvx") || lowerName == "itv" || lowerName.contains("itv hub") -> ProviderConfig(
                packageNames = listOf("air.ITVMobilePlayer"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.itv.com/"
                ).distinct()
            )

            lowerName.contains("channel 4") || lowerName.contains("all 4") -> ProviderConfig(
                packageNames = listOf("com.channel4.ondemand"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.channel4.com/"
                ).distinct()
            )

            lowerName == "now" || lowerName.startsWith("now ") || lowerName.contains("now tv") -> ProviderConfig(
                packageNames = listOf("com.nowtv.uk"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.nowtv.com/"
                ).distinct()
            )

            lowerName.contains("skyshowtime") || lowerName.contains("sky showtime") -> ProviderConfig(
                packageNames = listOf("com.skyshowtime.skyshowtime.google"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.skyshowtime.com/"
                ).distinct()
            )

            lowerName.contains("sky go") -> ProviderConfig(
                packageNames = listOf("com.bskyb.skygo"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.sky.com/"
                ).distinct()
            )

            lowerName.contains("canal+") || lowerName.contains("canal plus") -> ProviderConfig(
                packageNames = listOf("com.canalplus.canalplus"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.canalplus.com/"
                ).distinct()
            )

            lowerName.contains("joyn") -> ProviderConfig(
                packageNames = listOf("de.prosiebensat1digital.seventv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.joyn.de/suche?q=$uriTitle"
                ).distinct()
            )

            lowerName.contains("movistar") -> ProviderConfig(
                packageNames = listOf("es.plus.yomvi", "com.movistarplus.androidtv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.movistarplus.es/"
                ).distinct()
            )

            lowerName.contains("rakuten") -> ProviderConfig(
                packageNames = if (upperRegion == "JP") {
                    listOf("jp.co.rakuten.video.showtimeplayer", "tv.wuaki", "tv.wuaki.apptv")
                } else {
                    listOf("tv.wuaki", "tv.wuaki.apptv", "jp.co.rakuten.video.showtimeplayer")
                },
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    if (upperRegion == "JP") {
                        "https://tv.rakuten.co.jp/search/keyword/?q=$uriTitle"
                    } else {
                        "https://www.rakuten.tv/"
                    }
                ).distinct()
            )

            lowerName.contains("crave") -> ProviderConfig(
                packageNames = listOf("ca.bellmedia.cravetv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.crave.ca/en"
                ).distinct()
            )

            lowerName.contains("stan") -> ProviderConfig(
                packageNames = listOf("au.com.stan.and"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.stan.com.au/"
                ).distinct()
            )

            lowerName.contains("binge") -> ProviderConfig(
                packageNames = listOf("au.com.streamotion.ares"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://binge.com.au/"
                ).distinct()
            )

            lowerName.contains("globoplay") -> ProviderConfig(
                packageNames = listOf("com.globo.globotv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://globoplay.globo.com/busca/?q=$uriTitle"
                ).distinct()
            )

            lowerName == "vix" || lowerName.startsWith("vix ") -> ProviderConfig(
                packageNames = listOf("com.univision.prendetv"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://vix.com/"
                ).distinct()
            )

            lowerName.contains("u-next") || lowerName.contains("unext") -> ProviderConfig(
                packageNames = listOf("jp.unext.mediaplayer"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://video.unext.jp/freeword?query=$uriTitle"
                ).distinct()
            )

            lowerName.contains("tving") -> ProviderConfig(
                packageNames = listOf("net.cj.cjhv.gs.tving"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.tving.com/"
                ).distinct()
            )

            lowerName.contains("coupang") -> ProviderConfig(
                packageNames = listOf("com.coupang.mobile.play"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.coupangplay.com/"
                ).distinct()
            )

            lowerName.contains("wavve") -> ProviderConfig(
                packageNames = listOf("kr.co.captv.pooqV2"),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    "https://www.wavve.com/"
                ).distinct()
            )

            // Universal fallback for any other regional or niche provider worldwide:
            // Routes cleanly to directContentUrl (if HTTPS), localized TMDB/JustWatch webLink,
            // or Google Watch Search directly in the user's web browser.
            else -> ProviderConfig(
                packageNames = emptyList(),
                verifiedInAppUris = listOfNotNull(directContentUrl),
                webSearchUrls = listOfNotNull(
                    safeDirectWeb,
                    safeWebLink,
                    googleFallback
                ).distinct()
            )
        }
    }

    internal data class ProviderConfig(
        val packageNames: List<String>,
        val verifiedInAppUris: List<String>,
        val webSearchUrls: List<String>
    )
}
