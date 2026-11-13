package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RegionalDefaults
import com.example.data.model.ViewingStats
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.viewmodel.MediaViewModel

@Composable
fun ProfileScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var showRegionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Profile & Account",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Movieverse Cloud Sync & Viewing Analytics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (currentUser != null) CinemaGreen.copy(alpha = 0.2f) else CinemaGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (currentUser != null) CinemaGreen else CinemaGold,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Account Status Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("card_account_status")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (currentUser != null) {
                        // Signed-in State
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CinemaGold,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser?.displayName?.firstOrNull()?.uppercase()
                                            ?: currentUser?.email?.firstOrNull()?.uppercase()
                                            ?: "U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "Movieverse Member",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUser?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CinemaGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cloud Sync Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinemaGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncWithCloud() },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sync_now")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", maxLines = 1, softWrap = false, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Cloud", maxLines = 1, softWrap = false, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.signOut(context) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_sign_out")
                            ) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", maxLines = 1, softWrap = false, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Guest Mode State
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guest Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Device Storage (Local Room DB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Log in with your Google account to automatically backup your watchlists, episode tracking, and ratings to the cloud and access them from any device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.signInWithGoogle(
                                        activity = activity,
                                        onSuccess = { user ->
                                            Toast.makeText(context, "Welcome, ${user.displayName ?: "cinephile"}!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_google_signin")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!syncMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = syncMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Preferences & Region Section
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preferences & Regional Catalog",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Region Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRegionDialog = true }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = CinemaGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Streaming Region", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (userPrefs.autoDetect) "Auto (${RegionalDefaults.getRegionLabel(effectiveRegion)})" else RegionalDefaults.getRegionLabel(userPrefs.regionCode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Content Language Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRegionDialog = true }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = CinemaCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Catalog Language", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CinemaCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = RegionalDefaults.getLanguageLabel(userPrefs.languageCode),
                                            color = CinemaCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Controls movie & TV titles, overviews & search result localization",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Show streaming providers toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Where to Watch Providers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Display Netflix, Prime, Hotstar & direct app links", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = userPrefs.showStreamingProviders,
                            onCheckedChange = { viewModel.toggleStreamingProviders(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold, checkedTrackColor = CinemaGold.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // Viewing Analytics & Stats (Integrated from Stats)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Viewing Statistics & Cinephile Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Cinephile Rank Badge Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("card_cinephile_rank")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CinemaGold.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = stats.cinephileBadge, fontSize = 26.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stats.cinephileRank,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold
                        )
                        Text(
                            text = "${stats.completedMoviesCount} Movies • ${stats.totalEpisodesLogged} TV Episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Stats Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(
                    title = "Watch Time",
                    value = stats.formattedDuration,
                    subtitle = "${stats.totalTimeMinutes / 60} Total Hours",
                    icon = Icons.Default.AccessTime,
                    tint = CinemaCyan,
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "Avg Rating",
                    value = if (stats.ratedItemsCount > 0) String.format("%.1f ★", stats.averageUserRating) else "—",
                    subtitle = "${stats.ratedItemsCount} Rated Titles",
                    icon = Icons.Default.Star,
                    tint = CinemaGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(
                    title = "Movies Logged",
                    value = "${stats.completedMoviesCount}",
                    subtitle = "Finished films",
                    icon = Icons.Default.Movie,
                    tint = CinemaGreen,
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "TV Episodes",
                    value = "${stats.totalEpisodesLogged}",
                    subtitle = "Episodes watched",
                    icon = Icons.Default.Tv,
                    tint = CinemaRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Genres Section
        if (stats.topGenres.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Top Genres",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        stats.topGenres.take(4).forEach { genreStat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = genreStat.genre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(text = "${genreStat.count} titles", style = MaterialTheme.typography.bodySmall, color = CinemaGold)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { genreStat.percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = CinemaGold,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }

        // About Movieverse, TMDB Legal Attribution & Play Store Compliance Section
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "About & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("card_about_legal")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Movieverse",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "v1.0 (Build 1)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This product uses the TMDB API but is not endorsed or certified by TMDB. Streaming availability data is referenced for entertainment discovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showRegionDialog) {
        LocalizationPreferencesDialog(
            currentPreferences = userPrefs,
            effectiveRegionCode = effectiveRegion,
            activeApiKey = viewModel.getActiveTmdbApiKey(),
            onDismiss = { showRegionDialog = false },
            onSave = { regionCode, autoDetect, langCode, filterOnly, tmdbKey ->
                viewModel.updateRegion(regionCode, autoDetect)
                viewModel.updateLanguage(langCode)
                viewModel.toggleFilterByLanguage(filterOnly)
                viewModel.updateCustomTmdbApiKey(tmdbKey)
                showRegionDialog = false
            }
        )
    }
}

@Composable
private fun ProfileMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .padding(7.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = tint
            )
        }
    }
}
