package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.model.RegionalDefaults
import com.example.ui.components.GoogleSignInIcon
import com.example.ui.components.LocalizationPreferencesDialog
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo
import com.example.ui.viewmodel.MediaViewModel
import com.example.util.AppStoreActions
import com.example.util.FeatureFlags
import com.example.util.findActivity
import androidx.compose.ui.unit.Dp

@Composable
fun ProfileScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val profileColors = WatchPilotColors.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val monetizationState by viewModel.monetizationState.collectAsStateWithLifecycle()
    val ottSummary by viewModel.ottMoneySaverSummary.collectAsStateWithLifecycle()
    val watchAlerts by viewModel.watchAlerts.collectAsStateWithLifecycle()
    val isImportingCsv by viewModel.isImportingCsv.collectAsStateWithLifecycle()
    val csvImportMessage by viewModel.csvImportMessage.collectAsStateWithLifecycle()

    var showRegionDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setProAlertsEnabled(true)
        if (!granted) {
            Toast.makeText(
                context,
                "In-app alerts enabled. Allow notifications in Settings for lock-screen push alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLibraryFromUri(context, uri)
        }
    }

    val adaptive = LocalAdaptiveLayoutInfo.current
    val sectionWidthMod = if (adaptive.isTabletOrWide) {
        Modifier.widthIn(max = 860.dp)
    } else {
        Modifier
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 96.dp)
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
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sectionWidthMod)
                        .align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Sync, Pro & Cinema DNA",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (currentUser != null) profileColors.greenChipContainer else profileColors.goldChipContainer,
                            border = BorderStroke(1.dp, if (currentUser != null) profileColors.greenChipBorder else profileColors.goldChipBorder),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (currentUser != null) profileColors.greenAccent else profileColors.goldAccent,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // WatchPilot Pro Membership Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    1.dp,
                    if (monetizationState.isPro) profileColors.goldAccent else profileColors.goldChipBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("card_watchpilot_pro")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            val proColors = WatchPilotColors.current
                            Surface(
                                shape = CircleShape,
                                color = proColors.goldChipContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = proColors.goldAccent,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (monetizationState.isPro) monetizationState.planType.displayLabel else "WatchPilot Pro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = proColors.goldAccent
                                )
                                Text(
                                    text = when {
                                        monetizationState.isPro && FeatureFlags.ENABLE_AI_FEATURES ->
                                            "Ad-free • AI Concierge, Vision & Rooms"
                                        monetizationState.isPro ->
                                            "Ad-free • OTT Saver, Match Rooms & Insights"
                                        FeatureFlags.ENABLE_AI_FEATURES ->
                                            "Ad-free • OTT Saver, AI & Live Rooms"
                                        else ->
                                            "OTT Saver, Ad-Free & Live Match Rooms"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val tierColors = WatchPilotColors.current
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (monetizationState.isPro) tierColors.greenChipContainer else tierColors.goldChipContainer,
                            border = BorderStroke(1.dp, if (monetizationState.isPro) tierColors.greenChipBorder else tierColors.goldChipBorder)
                        ) {
                            Text(
                                text = if (monetizationState.isPro) "ACTIVE" else "FREE TIER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (monetizationState.isPro) tierColors.greenAccent else tierColors.goldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!monetizationState.isPro) {
                        val ctaColors = WatchPilotColors.current
                        Button(
                            onClick = { viewModel.openPaywall() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ctaColors.goldSolidBg,
                                contentColor = ctaColors.onGoldSolid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_upgrade_pro_profile")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upgrade to WatchPilot Pro",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (
                        monetizationState.planType == com.example.data.model.PlanType.PRO_MONTHLY ||
                        monetizationState.planType == com.example.data.model.PlanType.PRO_ANNUAL
                    ) {
                        val subSku = if (monetizationState.planType == com.example.data.model.PlanType.PRO_ANNUAL) {
                            com.example.data.billing.MonetizationManager.SKU_PRO_ANNUAL
                        } else {
                            com.example.data.billing.MonetizationManager.SKU_PRO_MONTHLY
                        }
                        OutlinedButton(
                            onClick = { AppStoreActions.openPlayStoreSubscriptions(context, subSku) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, profileColors.goldChipBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = profileColors.goldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_manage_subscription_profile")
                        ) {
                            Text(
                                text = "Manage Subscription in Google Play",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.restorePurchases() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, profileColors.neutralChipBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_purchases_profile")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Restore Purchases",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (BuildConfig.DEBUG) {
                            FilledTonalButton(
                                onClick = {
                                    if (monetizationState.isPro) {
                                        viewModel.deactivateSandboxPro()
                                    } else {
                                        viewModel.activateSandboxPro()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = profileColors.cyanChipContainer,
                                    contentColor = profileColors.cyanAccent
                                ),
                                border = BorderStroke(1.dp, profileColors.cyanChipBorder),
                                modifier = Modifier.testTag("btn_toggle_sandbox_pro")
                            ) {
                                Text(
                                    text = if (monetizationState.isPro) "Exit Sandbox" else "Sandbox Preview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (!monetizationState.billingStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = monetizationState.billingStatusMessage.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = profileColors.cyanAccent
                        )
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
                    .then(sectionWidthMod)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("card_account_status")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (currentUser != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = profileColors.goldSolidBg,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser?.displayName?.firstOrNull()?.uppercase()
                                            ?: currentUser?.email?.firstOrNull()?.uppercase()
                                            ?: "U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = profileColors.onGoldSolid
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "WatchPilot Member",
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
                                        tint = profileColors.greenAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cloud Sync Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = profileColors.greenAccent
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = profileColors.goldSolidBg,
                                    contentColor = profileColors.onGoldSolid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sync_now")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), color = profileColors.onGoldSolid, strokeWidth = 2.dp)
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
                                border = BorderStroke(1.dp, profileColors.neutralChipBorder),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_sign_out")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", maxLines = 1, softWrap = false, fontSize = 13.sp)
                            }
                        }
                    } else {
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
                                    text = "Saved on device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = profileColors.cyanAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign in for cloud backup & live rooms.",
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = profileColors.goldSolidBg,
                                contentColor = profileColors.onGoldSolid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_google_signin")
                        ) {
                            GoogleSignInIcon(modifier = Modifier.testTag("icon_google_signin"))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!syncMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = syncMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = profileColors.greenAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // OTT Subscription Money-Saver Radar, Alerts & Library Portability
        item {
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sectionWidthMod)
                ) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "OTT & Data",
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
                    .testTag("card_ott_money_saver")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = profileColors.greenChipContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = profileColors.greenAccent,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "OTT Money-Saver Radar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (ottSummary.subscribedCount > 0) {
                                        "${ottSummary.subscribedCount} apps (~${ottSummary.estimatedMonthlySpendLabel}/mo)"
                                    } else {
                                        "Pick paid apps to spot savings."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (ottSummary.potentialMonthlySavingsUsd > 0f) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = profileColors.greenChipContainer,
                                border = BorderStroke(1.dp, profileColors.greenChipBorder)
                            ) {
                                Text(
                                    text = "Save ${ottSummary.potentialMonthlySavingsShortLabel}/mo",
                                    color = profileColors.greenAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.openOttMoneySaver() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = profileColors.greenChipContainer,
                            contentColor = profileColors.greenAccent
                        ),
                        border = BorderStroke(1.dp, profileColors.greenChipBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_ott_audit")
                    ) {
                        Text(
                            text = if (ottSummary.subscribedCount > 0) "View OTT Audit & Savings" else "Select My Streaming Apps",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Streaming & New-Season Alerts (Pro)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .testTag("card_pro_alerts")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = profileColors.cyanChipContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = profileColors.cyanAccent,
                                            modifier = Modifier.padding(9.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Streaming & Season Alerts",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (!monetizationState.isPro) {
                                                val proPillColors = WatchPilotColors.current
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = proPillColors.goldChipContainer,
                                                    border = BorderStroke(1.dp, proPillColors.goldChipBorder)
                                                ) {
                                                    Text(
                                                        text = "PRO",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = proPillColors.goldAccent,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (monetizationState.proAlertsEnabled) {
                                                "Watching for new seasons & titles landing on your OTT apps"
                                            } else {
                                                "Get alerted when Watchlist items hit your subscribed apps"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = monetizationState.proAlertsEnabled,
                                    onCheckedChange = { wantEnabled ->
                                        if (!wantEnabled) {
                                            viewModel.setProAlertsEnabled(false)
                                        } else if (!monetizationState.isPro) {
                                            viewModel.openPaywall("Unlock Streaming Availability & New-Season Alerts with WatchPilot Pro.")
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setProAlertsEnabled(true)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = profileColors.onCyanSolid,
                                        checkedTrackColor = profileColors.cyanSolidBg
                                    ),
                                    modifier = Modifier.testTag("switch_pro_alerts")
                                )
                            }

                            if (monetizationState.isPro && monetizationState.proAlertsEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (watchAlerts.isEmpty()) {
                                            "No new updates yet — tap Check Now to scan"
                                        } else {
                                            "${watchAlerts.size} active watchlist update(s)"
                                        },
                                        fontSize = 12.sp,
                                        color = profileColors.cyanAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { viewModel.refreshWatchAlertsNow() },
                                        modifier = Modifier.testTag("btn_refresh_watch_alerts")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Check Alerts Now",
                                            tint = profileColors.cyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Check Now", fontSize = 12.sp, color = profileColors.cyanAccent, fontWeight = FontWeight.Bold)
                                    }
                                }

                                watchAlerts.take(4).forEach { alert ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.selectMedia(alert.mediaId) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = alert.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${alert.alertType.badgeLabel} • ${alert.detailMessage}",
                                                    fontSize = 11.sp,
                                                    color = profileColors.greenAccent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            androidx.compose.material3.IconButton(
                                                onClick = { viewModel.dismissWatchAlert(alert.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss alert",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Import & Export Library Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .testTag("card_library_portability")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Import & Backup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Free CSV import • CSV/JSON export with Pro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    csvPickerLauncher.launch(
                                        arrayOf(
                                            "text/*",
                                            "text/comma-separated-values",
                                            "application/csv",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                enabled = !isImportingCsv,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = profileColors.goldSolidBg,
                                    contentColor = profileColors.onGoldSolid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_profile_import_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isImportingCsv) "Importing CSV..." else "Import Watchlist CSV (Free)",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.exportLibrary(context, asJson = false) { intent ->
                                    context.startActivity(Intent.createChooser(intent, "Export WatchPilot CSV"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, profileColors.neutralChipBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Export CSV",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportLibrary(context, asJson = true) { intent ->
                                    context.startActivity(Intent.createChooser(intent, "Export WatchPilot JSON"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, profileColors.neutralChipBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_json")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Export JSON",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!csvImportMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = csvImportMessage.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = profileColors.cyanAccent
                        )
                    }
                }
            }
            }
        }

        // Preferences & Region Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
            ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preferences",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setDarkTheme(!isDarkTheme) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = stringResource(R.string.pref_theme_title),
                                tint = WatchPilotColors.current.goldAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.pref_theme_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(
                                        if (isDarkTheme) {
                                            R.string.pref_theme_dark_subtitle
                                        } else {
                                            R.string.pref_theme_light_subtitle
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.setDarkTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = profileColors.onGoldSolid,
                                checkedTrackColor = profileColors.goldSolidBg
                            ),
                            modifier = Modifier.testTag("switch_dark_theme")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

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
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = profileColors.goldAccent)
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
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = profileColors.cyanAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Catalog Language",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = profileColors.cyanChipContainer,
                                        border = BorderStroke(1.dp, profileColors.cyanChipBorder)
                                    ) {
                                        Text(
                                            text = RegionalDefaults.getLanguageLabel(userPrefs.languageCode),
                                            color = profileColors.cyanAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Titles, overviews & search language",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("Where to Watch Links", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Show streaming apps on title pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = userPrefs.showStreamingProviders,
                            onCheckedChange = { viewModel.toggleStreamingProviders(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = profileColors.onGoldSolid,
                                checkedTrackColor = profileColors.goldSolidBg
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("Usage Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Anonymous insights to improve app features", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = analyticsEnabled,
                            onCheckedChange = { viewModel.setAnalyticsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = profileColors.onGoldSolid,
                                checkedTrackColor = profileColors.goldSolidBg
                            ),
                            modifier = Modifier.testTag("switch_analytics_enabled")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.reopenTastePersonalization() }
                            .padding(vertical = 10.dp)
                            .testTag("btn_profile_retune_taste"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = profileColors.goldAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Personalize Taste", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "Update genres, stars & watched titles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }
        }

        // Viewing Analytics, Wrapped Share & Cinema DNA
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
            ) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Cinema DNA & Wrapped",
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
                    .testTag("card_cinephile_rank")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColors = WatchPilotColors.current
                    Surface(
                        shape = CircleShape,
                        color = badgeColors.goldChipContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = stats.cinephileBadge, fontSize = 26.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = stats.cinephileRank,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColors.goldAccent
                        )
                        Text(
                            text = "${stats.completedMoviesCount} Movies • ${stats.totalEpisodesLogged} TV Episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.shareWrappedCard(context) { intent ->
                                context.startActivity(Intent.createChooser(intent, "Share WatchPilot Wrapped"))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = badgeColors.goldChipContainer,
                            contentColor = badgeColors.goldAccent
                        ),
                        border = BorderStroke(1.dp, badgeColors.goldChipBorder),
                        modifier = Modifier.testTag("btn_share_wrapped")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share WatchPilot Wrapped",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Wrapped",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            }
        }

        // Stats Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
            ) {
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
                    tint = profileColors.cyanAccent,
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "Avg Rating",
                    value = if (stats.ratedItemsCount > 0) String.format("%.1f ★", stats.averageUserRating) else "—",
                    subtitle = "${stats.ratedItemsCount} Rated Titles",
                    icon = Icons.Default.Star,
                    tint = profileColors.goldAccent,
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
                    tint = profileColors.greenAccent,
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "TV Episodes",
                    value = "${stats.totalEpisodesLogged}",
                    subtitle = "Episodes watched",
                    icon = Icons.Default.Tv,
                    tint = profileColors.redAccent,
                    modifier = Modifier.weight(1f)
                )
            }
            }
        }

        // Cinema DNA (Completion Rate, Favorite Era / Decades & Top Language — Pro Insights)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = if (!monetizationState.isPro) BorderStroke(1.dp, profileColors.goldChipBorder) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("card_cinema_dna")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = profileColors.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cinema DNA Insights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!monetizationState.isPro) {
                            val dnaLockColors = WatchPilotColors.current
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = dnaLockColors.goldChipContainer,
                                border = BorderStroke(1.dp, dnaLockColors.goldChipBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = dnaLockColors.goldAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PRO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = dnaLockColors.goldAccent
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val dnaColors = WatchPilotColors.current
                    if (monetizationState.isPro) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = dnaColors.cyanChipContainer,
                            border = BorderStroke(1.dp, dnaColors.cyanChipBorder)
                        ) {
                            Text(
                                text = "${stats.completionRatePercent}% Completion • ${stats.topOriginLanguage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = dnaColors.cyanAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (stats.decadeBreakdown.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            stats.decadeBreakdown.take(4).forEach { decadeStat ->
                                val countText = if (decadeStat.count == 1) "1 title" else "${decadeStat.count} titles"
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = decadeStat.decadeLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )
                                        Text(
                                            text = countText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = dnaColors.cyanAccent,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { decadeStat.percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = dnaColors.cyanSolidBg,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Log titles to unlock your stats.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Unlock your Completion Rate %, Top Origin Language, and Favorite Decade & Era Breakdown with WatchPilot Pro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                viewModel.openPaywall("Unlock deep Cinema DNA Insights (Completion Rate, Era Breakdown & Language DNA) with WatchPilot Pro.")
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = dnaColors.goldChipContainer,
                                contentColor = dnaColors.goldAccent
                            ),
                            border = BorderStroke(1.dp, dnaColors.goldChipBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_unlock_cinema_dna_pro")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlock Cinema DNA Insights", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                        .then(sectionWidthMod)
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
                            val countText = if (genreStat.count == 1) "1 title" else "${genreStat.count} titles"
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = genreStat.genre,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        text = countText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = profileColors.goldAccent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { genreStat.percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = profileColors.goldSolidBg,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }

        // About WatchPilot, TMDB & JustWatch Attribution & Play Store Data Safety Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sectionWidthMod)
            ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = profileColors.goldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WatchPilot",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = profileColors.neutralChipContainer,
                            border = BorderStroke(1.dp, profileColors.neutralChipBorder)
                        ) {
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = profileColors.goldAccent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This product uses the TMDB API but is not endorsed or certified by TMDB. Regional streaming availability data is powered by JustWatch via TMDB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.rateWatchPilotApp(context) }
                            .padding(vertical = 8.dp)
                            .testTag("btn_profile_rate_app"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = stringResource(R.string.rate_app_title),
                                tint = profileColors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.rate_app_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.rate_app_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.shareWatchPilotApp(context) }
                            .padding(vertical = 8.dp)
                            .testTag("btn_profile_share_app"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_app_title),
                                tint = profileColors.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.share_app_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.share_app_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.openLegalCompliance() }
                            .padding(vertical = 8.dp)
                            .testTag("btn_open_legal_compliance"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = profileColors.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Privacy, Terms & Attribution",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentUser != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountConfirm = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, profileColors.redChipBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = profileColors.redChipContainer,
                                contentColor = profileColors.redAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_delete_account")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            }
        }
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Permanently deletes your WatchPilot account, library, and ratings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirm = false
                        viewModel.deleteAccountAndCloudData(context) { _, _ -> }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = profileColors.redAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("btn_confirm_delete_account")
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
            }
        )
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
