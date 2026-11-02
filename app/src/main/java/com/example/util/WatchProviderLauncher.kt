package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WatchProviderLauncher {

    /**
     * Attempts to deep link directly into the streaming provider's native Android app to watch or search
     * the specified content. If the app is not installed or deep-linking fails, cascades to official web
     * search, then JustWatch link, and finally Google streaming search with zero crashes.
     */
    fun launchProvider(
        context: Context,
        providerName: String,
        title: String,
        releaseYear: Int = 0,
        isMovie: Boolean = true,
        webLink: String? = null
    ) {
        val cleanName = providerName.trim()
        val queryText = if (releaseYear > 1900) "$title $releaseYear" else title
        val encodedTitle = URLEncoder.encode(queryText, "UTF-8")
        val simpleEncodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedProvider = URLEncoder.encode(cleanName, "UTF-8")
        val lowerName = cleanName.lowercase()

        val intentCandidates = mutableListOf<IntentCandidate>()

        when {
            lowerName.contains("netflix") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "nflx://www.netflix.com/search?q=$simpleEncodedTitle",
                        packageName = "com.netflix.mediaclient",
                        fallbackWeb = "https://www.netflix.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("prime") || lowerName.contains("amazon") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "primevideo://search?phrase=$simpleEncodedTitle",
                        packageName = "com.amazon.avod.thirdpartyclient",
                        fallbackWeb = "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("hotstar") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "hotstar://search?q=$simpleEncodedTitle",
                        packageName = "in.startv.hotstar",
                        fallbackWeb = "https://www.hotstar.com/in/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("disney") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "disneyplus://search?q=$simpleEncodedTitle",
                        packageName = "com.disney.disneyplus",
                        fallbackWeb = "https://www.disneyplus.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("apple tv") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "https://tv.apple.com/search?term=$simpleEncodedTitle",
                        packageName = "com.apple.atve.androidtv.appletv",
                        fallbackWeb = "https://tv.apple.com/search?term=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("max") || lowerName.contains("hbo") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "https://play.max.com/search?q=$simpleEncodedTitle",
                        packageName = "com.wbd.stream",
                        fallbackWeb = "https://play.max.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("hulu") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "hulu://search?query=$simpleEncodedTitle",
                        packageName = "com.hulu.plus",
                        fallbackWeb = "https://www.hulu.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("youtube") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "vnd.youtube://results?search_query=$encodedTitle+full",
                        packageName = "com.google.android.youtube",
                        fallbackWeb = "https://www.youtube.com/results?search_query=$encodedTitle"
                    )
                )
            }
            lowerName.contains("google play") || lowerName.contains("google tv") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "https://play.google.com/store/search?q=$encodedTitle&c=movies",
                        packageName = "com.google.android.videos",
                        fallbackWeb = "https://play.google.com/store/search?q=$encodedTitle&c=movies"
                    )
                )
            }
            lowerName.contains("peacock") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "https://www.peacocktv.com/search?q=$simpleEncodedTitle",
                        packageName = "com.peacocktv.peacockandroid",
                        fallbackWeb = "https://www.peacocktv.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("paramount") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "https://www.paramountplus.com/search/?q=$simpleEncodedTitle",
                        packageName = "com.cbs.app",
                        fallbackWeb = "https://www.paramountplus.com/search/?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("jio") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "jiocinema://search?query=$simpleEncodedTitle",
                        packageName = "com.jio.media.ondemand",
                        fallbackWeb = "https://www.jiocinema.com/search/$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("zee5") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "zee5://search?q=$simpleEncodedTitle",
                        packageName = "com.graymatrix.did",
                        fallbackWeb = "https://www.zee5.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("sonyliv") || lowerName.contains("sony liv") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "sonyliv://search?q=$simpleEncodedTitle",
                        packageName = "com.sonyliv",
                        fallbackWeb = "https://www.sonyliv.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("crunchyroll") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "crunchyroll://search?q=$simpleEncodedTitle",
                        packageName = "com.crunchyroll.crunchyroid",
                        fallbackWeb = "https://www.crunchyroll.com/search?q=$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("tubi") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "tubitv://search/$simpleEncodedTitle",
                        packageName = "com.tubitv",
                        fallbackWeb = "https://tubitv.com/search/$simpleEncodedTitle"
                    )
                )
            }
            lowerName.contains("pluto") -> {
                intentCandidates.add(
                    IntentCandidate(
                        uri = "plutotv://search?q=$simpleEncodedTitle",
                        packageName = "tv.pluto.android",
                        fallbackWeb = "https://pluto.tv/search/details?q=$simpleEncodedTitle"
                    )
                )
            }
            else -> {
                val fallback = webLink ?: "https://www.google.com/search?q=watch+$encodedTitle+on+$encodedProvider"
                intentCandidates.add(
                    IntentCandidate(
                        uri = fallback,
                        packageName = null,
                        fallbackWeb = fallback
                    )
                )
            }
        }

        var launched = false
        val pm = context.packageManager

        // Cascade 1: Dedicated app package intent
        for (candidate in intentCandidates) {
            if (candidate.packageName != null) {
                try {
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(candidate.uri)).apply {
                        setPackage(candidate.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (appIntent.resolveActivity(pm) != null) {
                        context.startActivity(appIntent)
                        launched = true
                        break
                    }
                } catch (_: Exception) {
                    // Try next
                }
            }
        }

        // Cascade 2: Direct URI scheme launch (system chooser or installed app handler)
        if (!launched) {
            for (candidate in intentCandidates) {
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(candidate.uri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (viewIntent.resolveActivity(pm) != null) {
                        context.startActivity(viewIntent)
                        launched = true
                        break
                    }
                } catch (_: Exception) {
                    // Try web fallback
                }
            }
        }

        // Cascade 3: Fallback web URL in browser
        if (!launched) {
            for (candidate in intentCandidates) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(candidate.fallbackWeb)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    launched = true
                    break
                } catch (_: Exception) {
                    // Next
                }
            }
        }

        // Cascade 4: JustWatch web link if provided
        if (!launched && !webLink.isNullOrBlank()) {
            try {
                val justWatchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(justWatchIntent)
                launched = true
            } catch (_: Exception) {
                // Final
            }
        }

        // Cascade 5: Google streaming search fallback (guaranteed web)
        if (!launched) {
            try {
                val googleUrl = "https://www.google.com/search?q=watch+$encodedTitle+on+$encodedProvider"
                val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(searchIntent)
                launched = true
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open $cleanName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class IntentCandidate(
        val uri: String,
        val packageName: String?,
        val fallbackWeb: String
    )
}
