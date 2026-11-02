package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MediaType
import com.example.data.model.WatchStatus
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold

@Composable
fun AddCustomMediaDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        mediaType: MediaType,
        releaseYear: Int,
        genres: String,
        runtimeMinutes: Int,
        totalSeasons: Int,
        totalEpisodes: Int,
        overview: String,
        director: String,
        cast: String,
        status: WatchStatus,
        initialRating: Float,
        posterUrl: String,
        language: String,
        originCountry: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf(MediaType.MOVIE) }
    var releaseYear by remember { mutableStateOf("2024") }
    var genres by remember { mutableStateOf("Sci-Fi, Drama") }
    var language by remember { mutableStateOf("en") }
    var originCountry by remember { mutableStateOf("US") }
    var runtimeMinutes by remember { mutableStateOf("120") }
    var totalSeasons by remember { mutableStateOf("1") }
    var totalEpisodes by remember { mutableStateOf("10") }
    var overview by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var cast by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(WatchStatus.PLAN_TO_WATCH) }
    var initialRating by remember { mutableFloatStateOf(8.0f) }
    var posterUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_custom_media_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Custom Title",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaType.entries.forEach { type ->
                        FilterChip(
                            selected = mediaType == type,
                            onClick = { mediaType = type },
                            label = { Text(type.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = releaseYear,
                        onValueChange = { releaseYear = it },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    if (mediaType == MediaType.MOVIE) {
                        OutlinedTextField(
                            value = runtimeMinutes,
                            onValueChange = { runtimeMinutes = it },
                            label = { Text("Runtime (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = totalEpisodes,
                            onValueChange = { totalEpisodes = it },
                            label = { Text("Episodes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (mediaType == MediaType.TV_SHOW) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = totalSeasons,
                        onValueChange = { totalSeasons = it },
                        label = { Text("Seasons") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text("Genres (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text(if (mediaType == MediaType.MOVIE) "Director" else "Creator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cast,
                    onValueChange = { cast = it },
                    label = { Text("Cast (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = posterUrl,
                    onValueChange = { posterUrl = it },
                    label = { Text("Poster Image URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language Code (e.g. en, ja)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = originCountry,
                        onValueChange = { originCountry = it },
                        label = { Text("Country Code (e.g. US, JP)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = overview,
                    onValueChange = { overview = it },
                    label = { Text("Overview / Synopsis") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Initial Watch Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(WatchStatus.PLAN_TO_WATCH, WatchStatus.WATCHING, WatchStatus.COMPLETED).forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaCyan,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score / Rating",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.1f / 10", initialRating),
                        style = MaterialTheme.typography.labelLarge,
                        color = CinemaGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = initialRating,
                    onValueChange = { initialRating = it },
                    valueRange = 1f..10f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = CinemaGold,
                        activeTrackColor = CinemaGold
                    )
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            if (title.isBlank()) {
                                errorMessage = "Please enter a title"
                                return@Button
                            }
                            val year = releaseYear.toIntOrNull() ?: 2024
                            val runMin = runtimeMinutes.toIntOrNull() ?: 100
                            val seasons = totalSeasons.toIntOrNull() ?: 1
                            val eps = totalEpisodes.toIntOrNull() ?: 10

                            onConfirm(
                                title.trim(),
                                mediaType,
                                year,
                                genres.trim(),
                                runMin,
                                seasons,
                                eps,
                                overview.trim(),
                                director.trim(),
                                cast.trim(),
                                status,
                                initialRating,
                                posterUrl.trim(),
                                language.trim().ifBlank { "en" },
                                originCountry.trim().ifBlank { "US" }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("btn_save_custom_media")
                    ) {
                        Text("Save Title", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
