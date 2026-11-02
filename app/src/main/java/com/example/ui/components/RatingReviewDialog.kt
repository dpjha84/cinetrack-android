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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CinemaGold

@Composable
fun RatingReviewDialog(
    mediaTitle: String,
    currentRating: Float,
    currentReview: String,
    onDismiss: () -> Unit,
    onSave: (rating: Float, review: String) -> Unit
) {
    // Normalize to 1-5 star scale
    val initialRating = when {
        currentRating > 5.0f -> (currentRating / 2.0f).coerceIn(1.0f, 5.0f)
        currentRating > 0f -> currentRating.coerceIn(1.0f, 5.0f)
        else -> 4.0f
    }
    var rating by remember { mutableFloatStateOf(initialRating) }
    var review by remember { mutableStateOf(currentReview) }

    val ratingLabel = when {
        rating >= 4.8f -> "🏆 Masterpiece"
        rating >= 4.0f -> "🌟 Great"
        rating >= 3.0f -> "👍 Good"
        rating >= 2.0f -> "😐 Fair"
        else -> "👎 Poor"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("rating_review_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rate (1–5 Stars)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save your personal score in local Room database",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mediaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Interactive Large Stars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isFilled = rating >= i.toFloat() - 0.25f
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Star $i",
                            tint = if (isFilled) CinemaGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { rating = i.toFloat() }
                                .padding(4.dp)
                                .testTag("star_$i")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Score Display & Quality Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.1f / 5.0", rating),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CinemaGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CinemaGold.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = ratingLabel,
                            color = CinemaGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Slider with 0.5 steps (1.0 to 5.0)
                Slider(
                    value = rating,
                    onValueChange = { rating = Math.round(it * 2f) / 2f },
                    valueRange = 1f..5f,
                    steps = 7, // 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5
                    colors = SliderDefaults.colors(
                        thumbColor = CinemaGold,
                        activeTrackColor = CinemaGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset pills (1★, 2★, 3★, 4★, 5★)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(1f, 2f, 3f, 4f, 5f).forEach { star ->
                        FilterChip(
                            selected = rating == star,
                            onClick = { rating = star },
                            label = { Text("${star.toInt()} ★") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Personal Review & Notes (Optional)") },
                    placeholder = { Text("What did you think of the cast, story, or cinematography?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("input_review"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(rating, review.trim()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("btn_save_rating")
                    ) {
                        Text("Save Rating", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
