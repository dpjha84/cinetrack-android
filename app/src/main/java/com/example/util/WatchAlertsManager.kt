package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.WatchAlertEvent
import com.example.data.model.WatchAlertType
import com.example.data.model.WatchStatus
import org.json.JSONArray
import org.json.JSONObject

data class WatchAlertSnapshot(
    val knownSeasons: Int,
    val wasOnMyApps: Boolean
)

/**
 * Pure Kotlin alert diffing & notification helper for WatchPilot Pro
 * "Streaming Availability & New-Season Alerts".
 */
object WatchAlertsManager {

    private const val PREFS_NAME = "watchpilot_alerts_prefs"
    private const val KEY_SNAPSHOTS_JSON = "tracked_snapshots_json"
    private const val KEY_RECENT_ALERTS_JSON = "recent_alerts_json"
    private const val CHANNEL_ID = "watchpilot_pro_alerts"

    /**
     * Compares current tracked watchlist/watching titles against stored snapshots.
     * Pure JVM-safe function (unit tested without Android Context).
     */
    fun detectWatchAlerts(
        trackedMedia: List<MediaEntity>,
        previousSnapshots: Map<String, WatchAlertSnapshot>,
        subscribedProviders: Set<String>,
        regionCode: String,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<List<WatchAlertEvent>, Map<String, WatchAlertSnapshot>> {
        val nextSnapshots = LinkedHashMap<String, WatchAlertSnapshot>(previousSnapshots)
        val newAlerts = mutableListOf<WatchAlertEvent>()

        val canonicalSubs = subscribedProviders
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        val activeTracked = trackedMedia.filter { item ->
            val st = item.getWatchStatusEnum()
            st == WatchStatus.PLAN_TO_WATCH || st == WatchStatus.WATCHING || item.isFavorite
        }

        for (item in activeTracked) {
            val streamProviders = item.getProvidersForRegion(regionCode)
                .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
                .filter { it.isNotBlank() }
                .distinct()

            val matchingApps = if (canonicalSubs.isEmpty()) {
                emptyList()
            } else {
                streamProviders.filter { it in canonicalSubs }
            }
            val isNowOnMyApps = matchingApps.isNotEmpty()
            val prev = previousSnapshots[item.id]

            if (prev != null) {
                if (!prev.wasOnMyApps && isNowOnMyApps) {
                    val appNames = matchingApps.take(2).joinToString(" & ")
                    newAlerts += WatchAlertEvent(
                        id = "stream_${item.id}_${nowMs}",
                        mediaId = item.id,
                        title = item.title,
                        posterUrl = item.posterUrl,
                        alertType = WatchAlertType.NOW_ON_MY_APPS,
                        detailMessage = "Now streaming on $appNames ($regionCode)",
                        detectedAtMs = nowMs
                    )
                }

                if (
                    item.getMediaTypeEnum() == MediaType.TV_SHOW &&
                    prev.knownSeasons > 0 &&
                    item.totalSeasons > prev.knownSeasons
                ) {
                    newAlerts += WatchAlertEvent(
                        id = "season_${item.id}_s${item.totalSeasons}",
                        mediaId = item.id,
                        title = item.title,
                        posterUrl = item.posterUrl,
                        alertType = WatchAlertType.NEW_SEASON,
                        detailMessage = "Season ${item.totalSeasons} detected (${item.totalEpisodes} total eps)",
                        detectedAtMs = nowMs
                    )
                }
            }

            nextSnapshots[item.id] = WatchAlertSnapshot(
                knownSeasons = item.totalSeasons.coerceAtLeast(0),
                wasOnMyApps = isNowOnMyApps
            )
        }

        return newAlerts to nextSnapshots
    }

    /**
     * Also surfaces active actionable availability highlights for Pro users
     * (titles in Plan to Watch / Watching that match their subscribed OTT apps)
     * so the Alerts Feed is immediately helpful on first opt-in.
     */
    fun buildInstantAvailabilityHighlights(
        trackedMedia: List<MediaEntity>,
        subscribedProviders: Set<String>,
        regionCode: String
    ): List<WatchAlertEvent> {
        if (subscribedProviders.isEmpty()) return emptyList()
        val canonicalSubs = subscribedProviders
            .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
            .filter { it.isNotBlank() }
            .toSet()

        return trackedMedia
            .asSequence()
            .filter {
                val st = it.getWatchStatusEnum()
                st == WatchStatus.PLAN_TO_WATCH || st == WatchStatus.WATCHING
            }
            .mapNotNull { item ->
                val matches = item.getProvidersForRegion(regionCode)
                    .map { OttMoneySaverCalculator.canonicalizeProvider(it) }
                    .filter { it in canonicalSubs }
                    .distinct()
                if (matches.isEmpty()) null else WatchAlertEvent(
                    id = "ready_${item.id}",
                    mediaId = item.id,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    alertType = WatchAlertType.NOW_ON_MY_APPS,
                    detailMessage = "Ready to stream on ${matches.take(2).joinToString(" • ")}",
                    detectedAtMs = System.currentTimeMillis()
                )
            }
            .take(5)
            .toList()
    }

    fun loadRecentAlerts(context: Context): List<WatchAlertEvent> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT_ALERTS_JSON, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val type = runCatching {
                        WatchAlertType.valueOf(o.optString("type", WatchAlertType.NOW_ON_MY_APPS.name))
                    }.getOrDefault(WatchAlertType.NOW_ON_MY_APPS)
                    add(
                        WatchAlertEvent(
                            id = o.optString("id"),
                            mediaId = o.optString("mediaId"),
                            title = o.optString("title"),
                            posterUrl = o.optString("posterUrl"),
                            alertType = type,
                            detailMessage = o.optString("detail"),
                            detectedAtMs = o.optLong("ts", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun evaluateAndDispatch(
        context: Context,
        trackedMedia: List<MediaEntity>,
        subscribedProviders: Set<String>,
        regionCode: String,
        alertsEnabled: Boolean
    ): List<WatchAlertEvent> {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevSnapshots = loadSnapshots(prefs.getString(KEY_SNAPSHOTS_JSON, "").orEmpty())

        val (newlyDetected, updatedSnapshots) = detectWatchAlerts(
            trackedMedia = trackedMedia,
            previousSnapshots = prevSnapshots,
            subscribedProviders = subscribedProviders,
            regionCode = regionCode
        )
        saveSnapshots(prefs, updatedSnapshots)

        val existing = loadRecentAlerts(appContext)
        val combined = if (newlyDetected.isNotEmpty()) {
            (newlyDetected + existing).distinctBy { it.mediaId + "_" + it.alertType.name }.take(15)
        } else if (existing.isEmpty() && alertsEnabled) {
            buildInstantAvailabilityHighlights(trackedMedia, subscribedProviders, regionCode)
        } else {
            existing
        }
        saveRecentAlerts(prefs, combined)

        if (alertsEnabled && newlyDetected.isNotEmpty()) {
            dispatchSystemNotification(appContext, newlyDetected)
        }
        return combined
    }

    fun dismissAlert(context: Context, alertId: String): List<WatchAlertEvent> {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = loadRecentAlerts(appContext).filterNot { it.id == alertId }
        saveRecentAlerts(prefs, next)
        return next
    }

    fun clearAllAlerts(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT_ALERTS_JSON).apply()
    }

    private fun dispatchSystemNotification(context: Context, alerts: List<WatchAlertEvent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)
        val first = alerts.firstOrNull() ?: return
        val title = if (alerts.size == 1) {
            "${first.title} • ${first.alertType.badgeLabel}"
        } else {
            "${alerts.size} Watchlist Updates"
        }
        val body = if (alerts.size == 1) {
            first.detailMessage
        } else {
            alerts.take(2).joinToString(" • ") { "${it.title}: ${it.detailMessage}" }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            5101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_watchpilot_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(5101, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WatchPilot Streaming & Season Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when tracked titles arrive on your streaming apps or release new seasons."
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun loadSnapshots(raw: String): Map<String, WatchAlertSnapshot> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    val entry = obj.optJSONObject(key) ?: return@forEach
                    put(
                        key,
                        WatchAlertSnapshot(
                            knownSeasons = entry.optInt("s", 0),
                            wasOnMyApps = entry.optBoolean("o", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveSnapshots(
        prefs: android.content.SharedPreferences,
        snapshots: Map<String, WatchAlertSnapshot>
    ) {
        val obj = JSONObject()
        snapshots.forEach { (id, snap) ->
            obj.put(id, JSONObject().apply {
                put("s", snap.knownSeasons)
                put("o", snap.wasOnMyApps)
            })
        }
        prefs.edit().putString(KEY_SNAPSHOTS_JSON, obj.toString()).apply()
    }

    private fun saveRecentAlerts(
        prefs: android.content.SharedPreferences,
        alerts: List<WatchAlertEvent>
    ) {
        val arr = JSONArray()
        alerts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("mediaId", a.mediaId)
                put("title", a.title)
                put("posterUrl", a.posterUrl)
                put("type", a.alertType.name)
                put("detail", a.detailMessage)
                put("ts", a.detectedAtMs)
            })
        }
        prefs.edit().putString(KEY_RECENT_ALERTS_JSON, arr.toString()).apply()
    }
}
