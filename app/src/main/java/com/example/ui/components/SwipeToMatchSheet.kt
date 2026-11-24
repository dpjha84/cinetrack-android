package com.example.ui.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MatchCandidateCard
import com.example.data.model.MatchRoomUiState
import com.example.data.model.MonetizationState
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.WatchPilotColors
import com.example.util.OttMoneySaverCalculator
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SwipeToMatchSheet(
    currentUser: FirebaseUser?,
    matchRoomState: MatchRoomUiState,
    skippedIds: Set<String>,
    monetizationState: MonetizationState,
    onDismiss: () -> Unit,
    onSignInWithGoogle: (Activity) -> Unit,
    onCreateRoom: (List<String>) -> Unit,
    onJoinRoom: (String) -> Unit,
    onSwipeCandidate: (MatchCandidateCard, Boolean) -> Unit,
    onLeaveOrCloseRoom: () -> Unit,
    onOpenMatchedMedia: (mediaId: String, title: String, year: Int, mediaType: String) -> Unit,
    onWatchRewardedTrialAd: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var joinCodeInput by remember { mutableStateOf("") }
    val selectedProviders = remember {
        mutableStateListOf<String>().apply {
            addAll(monetizationState.subscribedProviders.take(4))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("swipe_to_match_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Swipe-to-Match",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Swipe together with your partner or friends — instant alert on mutual match!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Swipe-to-Match")
                }
            }

            if (currentUser == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Sign in with Google for Live Rooms",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Live 6-digit Swipe-to-Match rooms sync swipes in real time across devices via WatchPilot Cloud.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                (context as? Activity)?.let { onSignInWithGoogle(it) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("match_room_google_signin_button")
                        ) {
                            com.example.ui.components.GoogleSignInIcon(modifier = Modifier.testTag("icon_google_match_signin"))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }
                        if (matchRoomState is MatchRoomUiState.Error) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CinemaRed.copy(alpha = 0.14f)),
                                border = BorderStroke(1.dp, CinemaRed.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = matchRoomState.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
                return@Column
            }

            when (matchRoomState) {
                is MatchRoomUiState.Idle, is MatchRoomUiState.Error -> {
                    if (matchRoomState is MatchRoomUiState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaRed.copy(alpha = 0.14f)),
                            border = BorderStroke(1.dp, CinemaRed.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = matchRoomState.message,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Host Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Host a New Room",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = when {
                                        monetizationState.isPro -> "PRO UNLIMITED"
                                        monetizationState.rewardedTrialCredits > 0 ->
                                            "${monetizationState.remainingFreeHostedRooms}/mo Free • ${monetizationState.rewardedTrialCredits} Ad Pass"
                                        else -> "${monetizationState.remainingFreeHostedRooms} Free / mo"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Optional: Filter deck to shared streaming services",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OttMoneySaverCalculator.supportedServices.take(8).forEach { svc ->
                                    val selected = svc.canonicalName in selectedProviders
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) selectedProviders.remove(svc.canonicalName)
                                            else selectedProviders.add(svc.canonicalName)
                                        },
                                        label = { Text(svc.shortLabel, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CinemaGold,
                                            selectedLabelColor = DarkBackground
                                        )
                                    )
                                }
                            }
                            if (!monetizationState.isPro && monetizationState.remainingFreeHostedRooms == 0) {
                                RewardedTrialPassBanner(
                                    monetizationState = monetizationState,
                                    sourceTag = "swipe_match",
                                    onRewardEarned = onWatchRewardedTrialAd
                                )
                            }
                            Button(
                                onClick = { onCreateRoom(selectedProviders.toList()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("create_match_room_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CinemaGold,
                                    contentColor = DarkBackground
                                )
                            ) {
                                Text(
                                    text = if (monetizationState.canHostMatchRoom) "Create 6-Digit Room Code" else "Unlock Unlimited Rooms (Pro)",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Join Card (100% Free for Guests!)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Join Partner or Friend's Room",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = "ALWAYS FREE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGreen,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { joinCodeInput = it.uppercase().take(6) },
                                    placeholder = { Text("Enter 6-digit code") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("join_room_code_input")
                                )
                                Button(
                                    onClick = { onJoinRoom(joinCodeInput) },
                                    enabled = joinCodeInput.length == 6,
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("join_match_room_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CinemaCyan,
                                        contentColor = DarkBackground
                                    )
                                ) {
                                    Text("Join", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is MatchRoomUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CinemaGold)
                    }
                }

                is MatchRoomUiState.Active -> {
                    val room = matchRoomState.room
                    val myUid = currentUser.uid
                    val myLikes = room.swipesByUid[myUid].orEmpty()

                    // Room Code Banner
                    val roomBannerColors = WatchPilotColors.current
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, roomBannerColors.goldChipBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "ROOM CODE: ${room.roomCode}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.5.sp,
                                    color = roomBannerColors.goldAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${room.participantUids.size} participant(s) connected • Host: ${room.hostName}",
                                    fontSize = 11.sp,
                                    color = roomBannerColors.cyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join my WatchPilot Swipe-to-Match room! Enter code: ${room.roomCode}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share Room Code"))
                                    }
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share Room Code", tint = roomBannerColors.goldAccent)
                                }
                            }
                        }
                    }

                    // Mutual Match Celebration!
                    if (room.status == "MATCHED" && !room.matchedMediaId.isNullOrBlank()) {
                        val matchedCandidate = room.candidates.find { it.id == room.matchedMediaId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaGreen.copy(alpha = 0.16f)),
                            border = BorderStroke(2.dp, CinemaGreen),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Celebration,
                                    contentDescription = null,
                                    tint = CinemaGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "IT'S A MUTUAL MATCH!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = CinemaGreen
                                )
                                AsyncImage(
                                    model = room.matchedPosterUrl,
                                    contentDescription = room.matchedTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 130.dp, height = 195.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Text(
                                    text = room.matchedTitle ?: "Tonight's Winner",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        onOpenMatchedMedia(
                                            room.matchedMediaId,
                                            room.matchedTitle ?: "",
                                            matchedCandidate?.releaseYear ?: 2024,
                                            matchedCandidate?.mediaType ?: "MOVIE"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CinemaGold,
                                        contentColor = DarkBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Details & Stream Now", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else {
                        val nextCandidate = room.candidates.firstOrNull { cand ->
                            cand.id !in myLikes && cand.id !in skippedIds
                        }

                        if (nextCandidate == null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "You've swiped through all ${room.candidates.size} titles!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (room.participantUids.size < 2) {
                                            "Share code ${room.roomCode} with your partner or friends — as soon as they swipe right on one of your ${myLikes.size} liked pick(s), WatchPilot triggers an instant match!"
                                        } else {
                                            "Waiting for everyone in room ${room.roomCode} to finish swiping..."
                                        },
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Interactive Deck Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = nextCandidate.posterUrl,
                                        contentDescription = nextCandidate.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 165.dp, height = 245.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DarkBackground)
                                    )
                                    Text(
                                        text = nextCandidate.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${nextCandidate.releaseYear} • ${nextCandidate.genres} • ★ ${String.format(java.util.Locale.US, "%.1f", nextCandidate.rating)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (nextCandidate.providersLabel.isNotBlank()) {
                                        Text(
                                            text = nextCandidate.providersLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CinemaCyan
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onSwipeCandidate(nextCandidate, false) },
                                            shape = CircleShape,
                                            border = BorderStroke(1.5.dp, CinemaRed),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("swipe_pass_button")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = CinemaRed)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pass",
                                                color = CinemaRed,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Button(
                                            onClick = { onSwipeCandidate(nextCandidate, true) },
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CinemaGreen,
                                                contentColor = DarkBackground
                                            ),
                                            modifier = Modifier
                                                .weight(1.35f)
                                                .height(50.dp)
                                                .testTag("swipe_like_button")
                                        ) {
                                            Icon(Icons.Filled.Favorite, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Watch Tonight",
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onLeaveOrCloseRoom,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Leave Room ${room.roomCode}")
                    }
                }
            }
        }
    }
}
