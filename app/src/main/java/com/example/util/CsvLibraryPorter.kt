package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedCsvRow(
    val title: String,
    val releaseYear: Int?,
    val mediaType: MediaType,
    val status: WatchStatus,
    val userRatingOutOf5: Float,
    val tmdbId: Long?,
    val isFavorite: Boolean,
    val collectionName: String?
)

object CsvLibraryPorter {

    data class ParseBatch(
        val formatName: String,
        val rows: List<ParsedCsvRow>
    )

    /**
     * Parses CSV files exported from Moviebase, IMDb, Letterboxd, Trakt, or WatchPilot.
     */
    fun parseUniversalCsv(csvContent: String): ParseBatch {
        val normalized = csvContent.removePrefix("\uFEFF").trim()
        if (normalized.isBlank()) return ParseBatch("Empty CSV", emptyList())

        val lines = splitCsvLines(normalized)
        if (lines.size < 2) return ParseBatch("Unknown CSV", emptyList())

        val headers = parseCsvLine(lines.first()).map { it.trim().lowercase() }

        val formatName = when {
            headers.contains("watchpilot_id") || headers.contains("customwatchlist") -> "WatchPilot"
            headers.contains("const") && headers.contains("title type") -> "IMDb"
            headers.contains("letterboxd uri") || (headers.contains("name") && headers.contains("watched date")) -> "Letterboxd"
            headers.contains("trakt_id") || headers.contains("listed_at") -> "Trakt"
            headers.contains("tmdb_id") || headers.contains("tmdbid") || headers.contains("show_title") -> "Moviebase"
            else -> "Standard CSV"
        }

        fun colIndex(vararg candidates: String): Int {
            for (cand in candidates) {
                val idx = headers.indexOfFirst { it == cand || it.replace("_", " ") == cand }
                if (idx >= 0) return idx
            }
            return -1
        }

        val titleIdx = colIndex("title", "name", "original title", "show title", "movie title")
        if (titleIdx < 0) return ParseBatch(formatName, emptyList())

        val yearIdx = colIndex("year", "release year", "releaseyear", "start year")
        val ratingIdx = colIndex("your rating", "rating", "userrating", "user rating", "personal rating")
        val typeIdx = colIndex("title type", "type", "mediatype", "media type")
        val statusIdx = colIndex("status", "watch status", "state", "list")
        val watchedDateIdx = colIndex("watched date", "date rated", "completed at", "watched_at")
        val tmdbIdx = colIndex("tmdb id", "tmdbid", "tmdb")
        val favIdx = colIndex("isfavorite", "is favorite", "favorite", "liked")
        val collIdx = colIndex("customwatchlist", "custom watchlist", "collection", "list name", "tags")

        val parsedRows = mutableListOf<ParsedCsvRow>()

        for (lineIdx in 1 until lines.size) {
            val line = lines[lineIdx]
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            val rawTitle = cols.getOrNull(titleIdx)?.trim().orEmpty()
            if (rawTitle.isBlank()) continue

            val year = cols.getOrNull(yearIdx)?.trim()?.take(4)?.toIntOrNull()
            val rawRating = cols.getOrNull(ratingIdx)?.trim()?.toFloatOrNull() ?: 0f
            // Normalize 10-point ratings (IMDb / Moviebase / Trakt) to 5-star scale if > 5
            val rating5 = when {
                rawRating <= 0f -> 0f
                rawRating > 5f -> (rawRating / 2f).coerceIn(0.5f, 5f)
                else -> rawRating.coerceIn(0.5f, 5f)
            }

            val rawType = cols.getOrNull(typeIdx)?.trim()?.lowercase().orEmpty()
            val mediaType = if (
                rawType.contains("tv") ||
                rawType.contains("series") ||
                rawType.contains("show") ||
                rawType.contains("mini") ||
                rawType.contains("episode")
            ) {
                MediaType.TV_SHOW
            } else {
                MediaType.MOVIE
            }

            val rawStatus = cols.getOrNull(statusIdx)?.trim()?.uppercase().orEmpty()
            val hasWatchedDate = !cols.getOrNull(watchedDateIdx)?.trim().isNullOrBlank()
            val status = when {
                rawStatus in listOf("WATCHING", "IN_PROGRESS", "CURRENT") -> WatchStatus.WATCHING
                rawStatus in listOf("COMPLETED", "WATCHED", "SEEN", "HISTORY") -> WatchStatus.COMPLETED
                rawStatus in listOf("ON_HOLD", "PAUSED") -> WatchStatus.ON_HOLD
                rawStatus in listOf("DROPPED", "ABANDONED") -> WatchStatus.DROPPED
                rawStatus in listOf("PLAN_TO_WATCH", "WATCHLIST", "TO_WATCH") -> WatchStatus.PLAN_TO_WATCH
                formatName == "Letterboxd" && (hasWatchedDate || rating5 > 0f) -> WatchStatus.COMPLETED
                rating5 > 0f && formatName == "IMDb" -> WatchStatus.COMPLETED
                else -> WatchStatus.PLAN_TO_WATCH
            }

            val tmdbId = cols.getOrNull(tmdbIdx)?.trim()?.toLongOrNull()
            val favStr = cols.getOrNull(favIdx)?.trim()?.lowercase().orEmpty()
            val isFavorite = favStr in listOf("true", "1", "yes", "y")
            val collection = cols.getOrNull(collIdx)?.trim()?.takeIf { it.isNotBlank() }

            parsedRows.add(
                ParsedCsvRow(
                    title = rawTitle,
                    releaseYear = year,
                    mediaType = mediaType,
                    status = status,
                    userRatingOutOf5 = rating5,
                    tmdbId = tmdbId,
                    isFavorite = isFavorite,
                    collectionName = collection
                )
            )
        }

        return ParseBatch(formatName, parsedRows)
    }

    fun serializeToCsv(items: List<MediaEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("watchpilot_id,tmdb_id,title,year,media_type,status,rating,is_favorite,watched_episodes,total_episodes,custom_watchlist,genres")
        for (item in items) {
            val tmdbNumeric = item.id.removePrefix("tmdb_m_").removePrefix("tmdb_tv_").toLongOrNull()?.toString().orEmpty()
            sb.appendLine(
                listOf(
                    escapeCsv(item.id),
                    escapeCsv(tmdbNumeric),
                    escapeCsv(item.title),
                    item.releaseYear.toString(),
                    escapeCsv(item.mediaType),
                    escapeCsv(item.status),
                    String.format(Locale.US, "%.1f", item.getUserRatingInStars()),
                    item.isFavorite.toString(),
                    item.watchedEpisodes.toString(),
                    item.totalEpisodes.toString(),
                    escapeCsv(item.customWatchlist.orEmpty()),
                    escapeCsv(item.genres)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun serializeToJson(items: List<MediaEntity>): String {
        val root = JSONObject()
        root.put("app", "WatchPilot")
        root.put("version", "2.0.0")
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("releaseYear", item.releaseYear)
            obj.put("mediaType", item.mediaType)
            obj.put("status", item.status)
            obj.put("userRating", item.userRating.toDouble())
            obj.put("userReview", item.userReview)
            obj.put("isFavorite", item.isFavorite)
            obj.put("watchedEpisodes", item.watchedEpisodes)
            obj.put("totalEpisodes", item.totalEpisodes)
            obj.put("customWatchlist", item.customWatchlist ?: JSONObject.NULL)
            obj.put("genres", item.genres)
            obj.put("posterUrl", item.posterUrl)
            arr.put(obj)
        }
        root.put("items", arr)
        return root.toString(2)
    }

    fun shareExportFile(context: Context, items: List<MediaEntity>, asJson: Boolean): Result<Intent> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val ext = if (asJson) "json" else "csv"
            val mime = if (asJson) "application/json" else "text/csv"
            val file = File(exportDir, "WatchPilot_Backup_$stamp.$ext")
            val content = if (asJson) serializeToJson(items) else serializeToCsv(items)
            file.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "WatchPilot v2.0 Library Backup ($ext)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun splitCsvLines(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    internal fun parseCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    cols.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        cols.add(sb.toString())
        return cols
    }
}
