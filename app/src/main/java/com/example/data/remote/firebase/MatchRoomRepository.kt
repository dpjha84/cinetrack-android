package com.example.data.remote.firebase

import com.example.data.model.MatchCandidateCard
import com.example.data.model.MatchRoomState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class MatchRoomRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val COLLECTION_ROOMS = "match_rooms"
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun generateRoomCode(): String {
            return (1..6).map { CODE_CHARS[Random.nextInt(CODE_CHARS.length)] }.joinToString("")
        }
    }

    private fun requireUid(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google to create or join live Swipe-to-Match rooms.")
    }

    suspend fun createRoom(
        hostName: String,
        regionCode: String,
        selectedProviders: List<String>,
        candidates: List<MatchCandidateCard>,
        roomCodeOverride: String? = null
    ): Result<MatchRoomState> {
        return try {
            val uid = requireUid()
            val roomCode = (roomCodeOverride ?: generateRoomCode()).uppercase().trim()
            val cleanHostName = hostName.trim().ifBlank { "WatchPilot Host" }.take(80)
            val providersCsv = selectedProviders.joinToString(",").take(500)
            val candidatesJson = serializeCandidates(candidates.take(18))
            val now = System.currentTimeMillis()

            val payload = hashMapOf<String, Any?>(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "hostName" to cleanHostName,
                "regionCode" to regionCode.ifBlank { "US" }.take(16),
                "selectedProviders" to providersCsv,
                "status" to "OPEN",
                "participantUids" to listOf(uid),
                "candidatesJson" to candidatesJson,
                "swipesByUid" to emptyMap<String, String>(),
                "matchedMediaId" to null,
                "matchedTitle" to null,
                "matchedPosterUrl" to null,
                "createdAt" to now,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection(COLLECTION_ROOMS).document(roomCode)
            docRef.set(payload).await()
            val snap = docRef.get().await()
            val parsed = parseRoomSnapshot(snap)
                ?: throw IllegalStateException("Failed to read created room $roomCode.")
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(rawRoomCode: String): Result<MatchRoomState> {
        return try {
            val uid = requireUid()
            val roomCode = rawRoomCode.uppercase().trim()
            if (!roomCode.matches(Regex("^[A-Z0-9]{6}$"))) {
                return Result.failure(IllegalArgumentException("Enter a valid 6-character room code (e.g., WP2026)."))
            }

            val docRef = firestore.collection(COLLECTION_ROOMS).document(roomCode)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Room $roomCode not found. Check the code with your host."))
            }

            val currentRoom = parseRoomSnapshot(snap)
                ?: return Result.failure(IllegalStateException("Could not parse room $roomCode."))

            if (uid !in currentRoom.participantUids) {
                if (currentRoom.status != "OPEN") {
                    return Result.failure(IllegalStateException("Room $roomCode is already ${currentRoom.status.lowercase()}."))
                }
                if (currentRoom.participantUids.size >= 10) {
                    return Result.failure(IllegalStateException("Room $roomCode is full (max 10 participants)."))
                }
                val updatedParticipants = currentRoom.participantUids + uid
                docRef.update(
                    mapOf(
                        "participantUids" to updatedParticipants,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val updatedSnap = docRef.get().await()
                return Result.success(parseRoomSnapshot(updatedSnap) ?: currentRoom.copy(participantUids = updatedParticipants))
            }

            Result.success(currentRoom)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordRightSwipe(rawRoomCode: String, candidate: MatchCandidateCard): Result<MatchRoomState> {
        return try {
            val uid = requireUid()
            val roomCode = rawRoomCode.uppercase().trim()
            val docRef = firestore.collection(COLLECTION_ROOMS).document(roomCode)
            val snap = docRef.get().await()
            val room = parseRoomSnapshot(snap)
                ?: return Result.failure(IllegalStateException("Room $roomCode no longer exists."))

            val myExistingLikes = room.swipesByUid[uid].orEmpty()
            val updatedLikes = (myExistingLikes + candidate.id).filter { it.isNotBlank() }
            val updatedParticipants = if (uid in room.participantUids) room.participantUids else (room.participantUids + uid)

            // Check if EVERY participant in a 2+ person room has now swiped right on this title!
            val mergedSwipes = room.swipesByUid.toMutableMap().apply {
                put(uid, updatedLikes.toSet())
            }
            val isMutualMatch = updatedParticipants.size >= 2 &&
                updatedParticipants.all { participantId ->
                    candidate.id in mergedSwipes[participantId].orEmpty()
                }

            val updates = mutableMapOf<String, Any?>(
                "participantUids" to updatedParticipants,
                "swipesByUid.$uid" to updatedLikes.joinToString(","),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isMutualMatch && room.status == "OPEN") {
                updates["status"] = "MATCHED"
                updates["matchedMediaId"] = candidate.id
                updates["matchedTitle"] = candidate.title
                updates["matchedPosterUrl"] = candidate.posterUrl
            }

            docRef.update(updates).await()
            val updatedSnap = docRef.get().await()
            Result.success(parseRoomSnapshot(updatedSnap) ?: room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeRoom(rawRoomCode: String): Result<Unit> {
        return try {
            requireUid()
            val roomCode = rawRoomCode.uppercase().trim()
            firestore.collection(COLLECTION_ROOMS).document(roomCode).update(
                mapOf(
                    "status" to "CLOSED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRoom(rawRoomCode: String): Flow<MatchRoomState?> = callbackFlow {
        val roomCode = rawRoomCode.uppercase().trim()
        val registration = firestore.collection(COLLECTION_ROOMS).document(roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                } else {
                    trySend(parseRoomSnapshot(snapshot))
                }
            }
        awaitClose { registration.remove() }
    }

    private fun serializeCandidates(candidates: List<MatchCandidateCard>): String {
        val arr = JSONArray()
        for (c in candidates) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title.take(120))
            obj.put("releaseYear", c.releaseYear)
            obj.put("mediaType", c.mediaType)
            obj.put("genres", c.genres.take(80))
            obj.put("rating", c.rating.toDouble())
            obj.put("posterUrl", c.posterUrl.take(300))
            obj.put("providersLabel", c.providersLabel.take(120))
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseCandidates(json: String): List<MatchCandidateCard> {
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", "")
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        MatchCandidateCard(
                            id = id,
                            title = title,
                            releaseYear = obj.optInt("releaseYear", 2024),
                            mediaType = obj.optString("mediaType", "MOVIE"),
                            genres = obj.optString("genres", ""),
                            rating = obj.optDouble("rating", 7.5).toFloat(),
                            posterUrl = obj.optString("posterUrl", ""),
                            providersLabel = obj.optString("providersLabel", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRoomSnapshot(doc: DocumentSnapshot): MatchRoomState? {
        val code = doc.getString("roomCode") ?: doc.id
        val hostUid = doc.getString("hostUid") ?: return null
        val hostName = doc.getString("hostName") ?: "Host"
        val regionCode = doc.getString("regionCode") ?: "US"
        val providersStr = doc.getString("selectedProviders").orEmpty()
        val selectedProviders = providersStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val status = doc.getString("status") ?: "OPEN"
        @Suppress("UNCHECKED_CAST")
        val participantUids = (doc.get("participantUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val candidatesJson = doc.getString("candidatesJson") ?: "[]"
        val candidates = parseCandidates(candidatesJson)

        @Suppress("UNCHECKED_CAST")
        val rawSwipes = doc.get("swipesByUid") as? Map<String, Any?> ?: emptyMap()
        val swipesByUid = rawSwipes.mapValues { (_, v) ->
            when (v) {
                is String -> v.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                is List<*> -> v.mapNotNull { it as? String }.toSet()
                else -> emptySet()
            }
        }

        return MatchRoomState(
            roomCode = code,
            hostUid = hostUid,
            hostName = hostName,
            regionCode = regionCode,
            selectedProviders = selectedProviders,
            status = status,
            participantUids = participantUids,
            candidates = candidates,
            swipesByUid = swipesByUid,
            matchedMediaId = doc.getString("matchedMediaId"),
            matchedTitle = doc.getString("matchedTitle"),
            matchedPosterUrl = doc.getString("matchedPosterUrl"),
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }
}
