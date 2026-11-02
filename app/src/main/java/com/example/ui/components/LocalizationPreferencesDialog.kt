package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.UserPreferencesEntity
import com.example.data.model.RegionalDefaults
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen

@Composable
fun LocalizationPreferencesDialog(
    currentPreferences: UserPreferencesEntity,
    effectiveRegionCode: String,
    activeApiKey: String,
    onDismiss: () -> Unit,
    onSave: (regionCode: String, autoDetect: Boolean, languageCode: String, filterOnly: Boolean, tmdbKey: String) -> Unit
) {
    var autoDetect by remember { mutableStateOf(currentPreferences.autoDetect) }
    var selectedRegion by remember { mutableStateOf(if (currentPreferences.autoDetect) "AUTO" else currentPreferences.regionCode) }
    var selectedLanguage by remember { mutableStateOf(currentPreferences.languageCode) }
    var filterOnlyLanguage by remember { mutableStateOf(currentPreferences.filterCatalogByLanguage) }
    var tmdbKeyInput by remember { mutableStateOf(currentPreferences.customTmdbApiKey) }

    val detected = remember { RegionalDefaults.detectDeviceRegion() }
    val isKeyActive = activeApiKey.isNotBlank() && activeApiKey != "YOUR_TMDB_API_KEY"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("localization_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Region, Language & TMDB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Personalize content catalog, regional streaming providers, and connect TMDB for live titles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Region / Country
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Detect Country / Region",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Device detects: ${detected.flag} ${detected.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaCyan
                        )
                    }
                    Switch(
                        checked = autoDetect,
                        onCheckedChange = {
                            autoDetect = it
                            if (it) {
                                selectedRegion = "AUTO"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CinemaGold,
                            checkedTrackColor = CinemaGold.copy(alpha = 0.3f)
                        )
                    )
                }

                if (!autoDetect) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Select Country / Region:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RegionalDefaults.supportedRegions.forEach { region ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRegion = region.code }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRegion.equals(region.code, ignoreCase = true),
                                    onClick = { selectedRegion = region.code },
                                    colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${region.flag}  ${region.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Preferred Media Language
                Text(
                    text = "Preferred Media Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Highlight or filter titles produced in this language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RegionalDefaults.supportedLanguages.chunked(3).forEach { rowLangs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowLangs.forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage.equals(lang.code, ignoreCase = true),
                                    onClick = { selectedLanguage = lang.code },
                                    label = { Text("${lang.flag} ${lang.nativeName}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CinemaGold,
                                        selectedLabelColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter strict toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filter Discover to this language only",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (filterOnlyLanguage) "Active" else "Shows all languages with preference boost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = filterOnlyLanguage,
                        onCheckedChange = { filterOnlyLanguage = it },
                        enabled = !selectedLanguage.equals("all", ignoreCase = true),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CinemaCyan,
                            checkedTrackColor = CinemaCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: TMDB API Key Integration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Api,
                            contentDescription = null,
                            tint = if (isKeyActive) CinemaGreen else CinemaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TMDB API Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isKeyActive) CinemaGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isKeyActive) "Connected" else "Key Not Set",
                            color = if (isKeyActive) CinemaGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enables live search across millions of movies & TV shows, trending sync, and official JustWatch streaming links.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tmdbKeyInput,
                    onValueChange = { tmdbKeyInput = it },
                    label = { Text("TMDB API Key (v3)") },
                    placeholder = { Text("Enter or paste your TMDB API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CinemaGold)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Get a free API key at themoviedb.org/settings/api, or set TMDB_API_KEY in AI Studio Secrets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CinemaCyan
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalRegion = if (autoDetect) "AUTO" else selectedRegion
                            onSave(finalRegion, autoDetect, selectedLanguage, filterOnlyLanguage, tmdbKeyInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("btn_save_localization")
                    ) {
                        Text("Apply Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
