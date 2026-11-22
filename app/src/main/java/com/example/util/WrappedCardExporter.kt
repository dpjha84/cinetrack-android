package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.ViewingStats
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object WrappedCardExporter {

    /**
     * Renders a crisp 1080x1350 Instagram/WhatsApp-ready "WatchPilot Wrapped" Cinema DNA card
     * and returns an ACTION_SEND share intent via FileProvider.
     */
    fun createShareableWrappedIntent(
        context: Context,
        userName: String,
        stats: ViewingStats
    ): Result<Intent> {
        return runCatching {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val cardsDir = File(context.cacheDir, "cards").apply { mkdirs() }
            val outFile = File(cardsDir, "WatchPilot_Wrapped.png")
            try {
                val canvas = Canvas(bitmap)

                // 1. Deep Obsidian -> Midnight Gradient Background
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        intArrayOf(
                            Color.parseColor("#0D1117"),
                            Color.parseColor("#151C28"),
                            Color.parseColor("#0A0E14")
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // 2. Outer Gold Border Frame
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = Color.parseColor("#55F5C518")
                }
                canvas.drawRoundRect(RectF(44f, 44f, width - 44f, height - 44f), 48f, 48f, borderPaint)

                // 3. Header Brand Pill
                val goldColor = Color.parseColor("#F5C518")
                val cyanColor = Color.parseColor("#00E5FF")
                val mutedColor = Color.parseColor("#9BA4B5")

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = goldColor
                    textSize = 36f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    letterSpacing = 0.14f
                }
                canvas.drawText("WATCHPILOT • CINEMA DNA", 96f, 136f, brandPaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 68f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val safeUser = userName.ifBlank { "Cinephile" }.take(22)
                canvas.drawText("$safeUser's Wrapped", 96f, 224f, titlePaint)

                val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cyanColor
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("${stats.cinephileBadge} ${stats.cinephileRank}", 96f, 286f, rankPaint)

                // 4. Stat Cards Grid (2x2)
                val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1B2332")
                }
                val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedColor
                    textSize = 28f
                }
                val statValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 58f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                fun drawStatBox(left: Float, top: Float, right: Float, bottom: Float, label: String, value: String, accent: Int) {
                    canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, cardPaint)
                    statValuePaint.color = accent
                    canvas.drawText(value, left + 36f, top + 98f, statValuePaint)
                    canvas.drawText(label, left + 36f, top + 148f, statLabelPaint)
                }

                drawStatBox(96f, 340f, 520f, 525f, "Screen Time Logged", stats.formattedDuration, goldColor)
                drawStatBox(560f, 340f, 984f, 525f, "Movies Completed", stats.completedMoviesCount.toString(), Color.WHITE)
                drawStatBox(96f, 555f, 520f, 740f, "Episodes Tracked", stats.totalEpisodesLogged.toString(), cyanColor)
                val avgStr = if (stats.averageUserRating > 0f) String.format(Locale.US, "★ %.1f", stats.averageUserRating) else "—"
                drawStatBox(560f, 555f, 984f, 740f, "Avg Personal Rating", avgStr, goldColor)

                // 5. Top Genres Bars
                val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Top Genres", 96f, 830f, sectionPaint)

                val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#222C3F") }
                val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = goldColor }
                val genreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 30f
                }

                val genresToDraw = stats.topGenres.take(4)
                if (genresToDraw.isEmpty()) {
                    statLabelPaint.textSize = 30f
                    canvas.drawText("Log movies & series in WatchPilot to unlock your genre DNA!", 96f, 895f, statLabelPaint)
                } else {
                    genresToDraw.forEachIndexed { idx, g ->
                        val yTop = 865f + (idx * 84f)
                        canvas.drawText("${g.genre} (${g.count})", 96f, yTop + 26f, genreTextPaint)
                        canvas.drawRoundRect(RectF(96f, yTop + 38f, 984f, yTop + 58f), 10f, 10f, barTrackPaint)
                        val fillW = (888f * g.percentage.coerceIn(0.08f, 1f))
                        barFillPaint.color = if (idx == 0) goldColor else cyanColor
                        canvas.drawRoundRect(RectF(96f, yTop + 38f, 96f + fillW, yTop + 58f), 10f, 10f, barFillPaint)
                    }
                }

                // 6. Footer Signature
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedColor
                    textSize = 26f
                }
                val topEra = stats.decadeBreakdown.firstOrNull()?.decadeLabel ?: "Modern Cinema"
                canvas.drawText("Favorite Era: $topEra  •  Completion Rate: ${stats.completionRatePercent}%", 96f, 1235f, footerPaint)

                val footerBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = goldColor
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Tracked with WatchPilot", 96f, 1280f, footerBrandPaint)

                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            } finally {
                bitmap.recycle()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "My WatchPilot Cinema DNA: ${stats.cinephileBadge} ${stats.cinephileRank} (${stats.formattedDuration} watched, ${stats.completedMoviesCount} movies, ${stats.totalEpisodesLogged} episodes)!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
