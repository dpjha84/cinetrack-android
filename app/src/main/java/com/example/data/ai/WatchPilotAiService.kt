package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.TitleAiBrief
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class WatchPilotAiService {

    companion object {
        private const val TAG = "WatchPilotAiService"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val AI_TIMEOUT_MS = 60_000L
        private const val MAX_IMAGE_DIMENSION = 1280
    }

    data class RawScannedItem(
        val title: String,
        val releaseYear: Int?,
        val mediaType: MediaType,
        val note: String
    )

    data class RawConciergePick(
        val title: String,
        val releaseYear: Int,
        val mediaType: MediaType,
        val whyItFits: String,
        val matchScore: Int
    )

    private val snapScanSchema by lazy {
        val itemSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Exact English or widely known release title of the movie or TV series"),
                "releaseYear" to Schema.integer(description = "4-digit release year if known", nullable = true),
                "mediaType" to Schema.enumeration(
                    values = listOf("MOVIE", "TV_SHOW"),
                    description = "Whether the title is a MOVIE or TV_SHOW"
                ),
                "note" to Schema.string(description = "Short 4-10 word context from the image (e.g., Top 10 Sci-Fi pick, Reel caption)")
            ),
            optionalProperties = listOf("releaseYear")
        )
        Schema.obj(
            properties = mapOf(
                "items" to Schema.array(
                    items = itemSchema,
                    description = "All movies and TV series visible in the screenshot, up to 12 titles"
                )
            )
        )
    }

    private val conciergeSchema by lazy {
        val pickSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Exact movie or TV series title"),
                "releaseYear" to Schema.integer(description = "4-digit release year"),
                "mediaType" to Schema.enumeration(
                    values = listOf("MOVIE", "TV_SHOW"),
                    description = "MOVIE or TV_SHOW"
                ),
                "whyItFits" to Schema.string(description = "Punchy 1-2 sentence personalized pitch explaining why this fits tonight's mood, time budget, and taste"),
                "matchScore" to Schema.integer(description = "Match percentage between 85 and 99")
            )
        )
        Schema.obj(
            properties = mapOf(
                "picks" to Schema.array(
                    items = pickSchema,
                    description = "Exactly 3 personalized movie or TV recommendations"
                )
            )
        )
    }

    private val titleBriefSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "personalMatchScore" to Schema.integer(description = "Personalized match score from 75 to 99"),
                "watchPriorityTag" to Schema.string(description = "Short 2-3 word label like Must-Watch Tonight, Weekend Binge, or Slow-Burn Gem"),
                "fitSummary" to Schema.string(description = "1-2 concise sentences explaining why this title fits the user's personal taste, favorites, and vibe."),
                "seasonRecap" to Schema.string(description = "Strictly spoiler-free premise hook or catch-up summary up to the user's current progress without spoiling twists or future seasons.")
            )
        )
    }

    // Lazy initialization per Firebase AI Logic contract (never evaluate at construction time)
    private val visionScannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = snapScanSchema
            }
        )
    }

    private val conciergeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = conciergeSchema
            }
        )
    }

    private val titleBriefModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = titleBriefSchema
            }
        )
    }

    suspend fun extractMediaFromScreenshot(bitmap: Bitmap): Result<List<RawScannedItem>> {
        return try {
            val safeBitmap = downscaleBitmapIfNeeded(bitmap)
            val response = withTimeout(AI_TIMEOUT_MS) {
                visionScannerModel.generateContent(
                    content {
                        image(safeBitmap)
                        text(
                            "Extract every movie or TV series title shown in this screenshot " +
                                "(social media Reel/TikTok/Short caption, poster grid, streaming app row, or Top-10 list). " +
                                "Ignore UI chrome, battery icons, usernames, and generic words. Return up to 12 distinct titles."
                        )
                    }
                )
            }

            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("AI Vision Scanner returned an empty response.")
            )
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("items")
            val extracted = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val title = obj.optString("title", "").trim()
                        if (title.isBlank()) continue
                        val year = if (obj.isNull("releaseYear")) null else obj.optInt("releaseYear", 0).takeIf { it in 1900..2035 }
                        val typeStr = obj.optString("mediaType", "MOVIE")
                        val mediaType = if (typeStr.equals("TV_SHOW", ignoreCase = true)) MediaType.TV_SHOW else MediaType.MOVIE
                        val note = obj.optString("note", "Detected in screenshot").trim()
                        add(RawScannedItem(title = title, releaseYear = year, mediaType = mediaType, note = note))
                    }
                }
            }.distinctBy { "${it.title.lowercase()}_${it.mediaType.name}" }

            Result.success(extracted)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Screenshot vision scan timed out", e)
            Result.failure(IllegalStateException("Vision scan timed out after 60 seconds. Please try a clearer screenshot."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot vision scan failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to analyze screenshot."))
        }
    }

    suspend fun generateConciergePicks(
        mood: String,
        timeBudget: String,
        audience: String,
        regionCode: String,
        subscribedProviders: Set<String>,
        favoriteAndTopRatedTitles: List<String>,
        unwatchedWatchlistTitles: List<String>
    ): Result<List<RawConciergePick>> {
        return try {
            val ottContext = if (subscribedProviders.isNotEmpty()) {
                "Prefer titles streaming in region $regionCode on: ${subscribedProviders.joinToString(", ")}."
            } else {
                "User is watching in region $regionCode."
            }
            val tasteContext = if (favoriteAndTopRatedTitles.isNotEmpty()) {
                "User loves: ${favoriteAndTopRatedTitles.take(10).joinToString(", ")}."
            } else {
                "User enjoys critically acclaimed modern movies and gripping series."
            }
            val watchlistContext = if (unwatchedWatchlistTitles.isNotEmpty()) {
                "User's current unwatched watchlist includes: ${unwatchedWatchlistTitles.take(15).joinToString(", ")}. Include 1-2 great matches from their watchlist if they fit!"
            } else {
                ""
            }

            val prompt = buildString {
                appendLine("You are WatchPilot's Tonight's AI Concierge.")
                appendLine("Mood: $mood")
                appendLine("Time Budget: $timeBudget")
                appendLine("Watching With: $audience")
                appendLine(ottContext)
                appendLine(tasteContext)
                if (watchlistContext.isNotBlank()) appendLine(watchlistContext)
                appendLine("Recommend exactly 3 distinct, real movies or TV series tailored to these constraints.")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                conciergeModel.generateContent(prompt)
            }

            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("AI Concierge returned an empty response.")
            )
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("picks")
            val picks = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val title = obj.optString("title", "").trim()
                        if (title.isBlank()) continue
                        val year = obj.optInt("releaseYear", 2024).coerceIn(1920, 2030)
                        val typeStr = obj.optString("mediaType", "MOVIE")
                        val type = if (typeStr.equals("TV_SHOW", ignoreCase = true)) MediaType.TV_SHOW else MediaType.MOVIE
                        val why = obj.optString("whyItFits", "Matches your $mood vibe tonight.").trim()
                        val score = obj.optInt("matchScore", 94).coerceIn(80, 99)
                        add(RawConciergePick(title, year, type, why, score))
                    }
                }
            }

            if (picks.isEmpty()) {
                Result.failure(IllegalStateException("No recommendations returned. Please try again."))
            } else {
                Result.success(picks)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Concierge timed out", e)
            Result.failure(IllegalStateException("Tonight's AI Concierge timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Concierge failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI Concierge request failed."))
        }
    }

    suspend fun generateTitleAiBrief(
        media: MediaEntity,
        favoriteAndTopRatedTitles: List<String>,
        subscribedProviders: Set<String>
    ): Result<TitleAiBrief> {
        return try {
            val isTv = media.getMediaTypeEnum() == MediaType.TV_SHOW
            val watchedSeasonsCount = media.watchedSeasons.coerceAtLeast(0)
            val progressInstruction = if (isTv) {
                when {
                    watchedSeasonsCount > 0 ->
                        "The user has finished $watchedSeasonsCount season(s) of ${media.totalSeasons.coerceAtLeast(1)}. Give a crisp spoiler-free 'Previously On...' catch-up summarizing Season 1 to Season $watchedSeasonsCount ONLY so they can jump straight into the next season. NEVER spoil Season ${watchedSeasonsCount + 1} or later endings."
                    media.watchedEpisodes > 0 ->
                        "The user has watched ${media.watchedEpisodes} of ${media.totalEpisodes} episodes. Give a strictly spoiler-free early-series refresher covering the premise and main characters without spoiling later twists."
                    else ->
                        "The user has not started this series yet. Give an atmospheric, strictly spoiler-free Season 1 setup & pacing preview."
                }
            } else {
                "Give a strictly spoiler-free tone, pacing, and mood setup for this movie without revealing plot twists or the ending."
            }

            val tasteLine = if (favoriteAndTopRatedTitles.isNotEmpty()) {
                "User's favorite/top-rated titles: ${favoriteAndTopRatedTitles.take(8).joinToString(", ")}."
            } else {
                "User enjoys engaging storytelling, strong performances, and high production value."
            }

            val prompt = buildString {
                appendLine("You are WatchPilot's Spoiler-Free AI Companion.")
                appendLine("Target Title: ${media.title} (${media.releaseYear}) [${media.mediaType}]")
                appendLine("Genres: ${media.genres.ifBlank { "Drama" }}")
                if (media.overview.isNotBlank()) {
                    appendLine("Overview: ${media.overview.take(350)}")
                }
                appendLine(tasteLine)
                if (subscribedProviders.isNotEmpty()) {
                    appendLine("User's streaming subscriptions: ${subscribedProviders.joinToString(", ")}.")
                }
                appendLine(progressInstruction)
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                titleBriefModel.generateContent(prompt)
            }

            val jsonText = response.text ?: return Result.failure(
                IllegalStateException("AI Companion returned an empty response.")
            )
            val obj = JSONObject(jsonText)
            val score = obj.optInt("personalMatchScore", 91).coerceIn(70, 99)
            val tag = obj.optString("watchPriorityTag", "Strong Taste Match").trim().ifBlank { "Taste Match" }
            val fit = obj.optString("fitSummary", "").trim()
            val recap = obj.optString("seasonRecap", "").trim()

            if (fit.isBlank() && recap.isBlank()) {
                Result.failure(IllegalStateException("Could not build an AI brief for this title."))
            } else {
                Result.success(
                    TitleAiBrief(
                        mediaId = media.id,
                        personalMatchScore = score,
                        watchPriorityTag = tag,
                        fitSummary = fit.ifBlank { "Strong thematic match with your favorite genres." },
                        seasonRecap = recap.ifBlank { "Spoiler-free premise ready whenever you hit play." }
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI title brief timed out", e)
            Result.failure(IllegalStateException("AI brief timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI title brief failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Could not generate AI recap."))
        }
    }

    private fun downscaleBitmapIfNeeded(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val longest = maxOf(width, height)
        if (longest <= MAX_IMAGE_DIMENSION) return source
        val ratio = MAX_IMAGE_DIMENSION.toFloat() / longest.toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
