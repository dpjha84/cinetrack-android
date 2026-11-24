package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R

object AppStoreActions {

    fun playStoreWebUrl(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun openPlayStoreForRating(context: Context) {
        val pkg = context.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$pkg")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(playStoreWebUrl(pkg))
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launchedMarket = runCatching {
            context.startActivity(marketIntent)
            true
        }.getOrDefault(false)

        if (!launchedMarket) {
            val launchedWeb = runCatching {
                context.startActivity(webIntent)
                true
            }.getOrDefault(false)
            if (!launchedWeb) {
                Toast.makeText(
                    context,
                    context.getString(R.string.rate_app_store_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun shareWatchPilotApp(context: Context) {
        val shareUrl = playStoreWebUrl(context.packageName)
        val shareBody = context.getString(R.string.share_app_message, shareUrl)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_app_chooser_title)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }
}
