package com.example.data.remote.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CustomCollectionEntity
import com.example.data.local.FavoritePersonEntity
import com.example.data.local.MediaEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.TvProgressEntity
import com.example.data.local.UserPreferencesEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {
    private val TAG = "FirestoreSyncManager"

    private val syncPrefs by lazy {
        context.getSharedPreferences("movieverse_sync_prefs", Context.MODE_PRIVATE)
    }

    fun getLastSyncedUserId(): String? =
        syncPrefs.getString("last_synced_uid", null)?.takeIf { it.isNotBlank() }

    fun setLastSyncedUserId(userId: String?) {
        syncPrefs.edit().apply {
            if (userId.isNullOrBlank()) remove("last_synced_uid")
            else putString("last_synced_uid", userId)
        }.commit()
    }

    val firestore: FirebaseFirestore by lazy {
        val dbId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            "ai-studio-android-cinetrac-4ecf9912-7c55-47f4-8738-c1cd6f12de97"
        }
        FirebaseFirestore.getInstance(dbId)
    }

    suspend fun uploadTrackedMedia(userId: String, item: MediaEntity) {
        if (userId.isBlank()) return
        try {
            val docRef = firestore.collection("users").document(userId)
                .collection("tracked_media").document(item.id)

            val payload = hashMapOf<String, Any?>(
                "id" to item.id,
                "title" to item.title,
                "mediaType" to item.mediaType,
                "releaseYear" to item.releaseYear,
                "genres" to item.genres,
                "posterUrl" to item.posterUrl,
                "backdropUrl" to item.backdropUrl,
                "overview" to item.overview,
                "directorOrCreator" to item.directorOrCreator,
                "cast" to item.cast,
                "communityRating" to item.communityRating,
                "runtimeMinutes" to item.runtimeMinutes,
                "totalSeasons" to item.totalSeasons,
                "totalEpisodes" to item.totalEpisodes,
                "status" to item.status,
                "userRating" to item.userRating,
                "userReview" to item.userReview,
                "isFavorite" to item.isFavorite,
                "lastWatchedDate" to item.lastWatchedDate,
                "watchedEpisodes" to item.watchedEpisodes,
                "watchedSeasons" to item.watchedSeasons,
                "customWatchlist" to item.customWatchlist,
                "isCustom" to item.isCustom,
                "originalLanguage" to item.originalLanguage,
                "originCountry" to item.originCountry,
                "streamingProviders" to item.streamingProviders,
                "createdAt" to item.createdAt,
                "userId" to userId,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload tracked media ${item.id} to Firestore: ${e.message}")
        }
    }

    suspend fun deleteTrackedMedia(userId: String, mediaId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("tracked_media").document(mediaId)
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete tracked media $mediaId in Firestore: ${e.message}")
        }
    }

    suspend fun uploadCollection(userId: String, collection: CustomCollectionEntity) {
        if (userId.isBlank()) return
        try {
            val docRef = firestore.collection("users").document(userId)
                .collection("custom_collections").document(collection.name)

            val payload = hashMapOf(
                "name" to collection.name,
                "description" to collection.description,
                "createdAt" to collection.createdAt,
                "userId" to userId
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload collection ${collection.name} to Firestore: ${e.message}")
        }
    }

    suspend fun deleteCollection(userId: String, name: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("custom_collections").document(name)
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete collection $name in Firestore: ${e.message}")
        }
    }

    suspend fun uploadPreferences(userId: String, prefs: UserPreferencesEntity) {
        if (userId.isBlank()) return
        try {
            val docRef = firestore.collection("users").document(userId)
                .collection("preferences").document("user_preferences")

            val payload = hashMapOf(
                "regionCode" to prefs.regionCode,
                "languageCode" to prefs.languageCode,
                "autoDetect" to prefs.autoDetect,
                "filterCatalogByLanguage" to prefs.filterCatalogByLanguage,
                "showStreamingProviders" to prefs.showStreamingProviders,
                "userId" to userId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload preferences to Firestore: ${e.message}")
        }
    }

    suspend fun uploadSearchHistory(userId: String, query: String) {
        if (userId.isBlank() || query.isBlank()) return
        try {
            val safeId = query.lowercase().replace("/", "_").take(40)
            val docRef = firestore.collection("users").document(userId)
                .collection("search_history").document(safeId)

            val payload = hashMapOf(
                "query" to query,
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload search history to Firestore: ${e.message}")
        }
    }

    suspend fun uploadFavoritePerson(userId: String, person: FavoritePersonEntity) {
        if (userId.isBlank()) return
        try {
            val docRef = firestore.collection("users").document(userId)
                .collection("favorite_people").document(person.id)

            val payload = hashMapOf(
                "id" to person.id,
                "personId" to person.personId,
                "name" to person.name,
                "profileUrl" to person.profileUrl,
                "department" to person.department,
                "birthday" to person.birthday,
                "placeOfBirth" to person.placeOfBirth,
                "createdAt" to person.createdAt,
                "userId" to userId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload favorite person ${person.name} to Firestore: ${e.message}")
        }
    }

    suspend fun deleteFavoritePerson(userId: String, personStorageId: String) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users").document(userId)
                .collection("favorite_people").document(personStorageId)
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete favorite person $personStorageId in Firestore: ${e.message}")
        }
    }

    suspend fun uploadTvProgress(userId: String, progress: TvProgressEntity) {
        if (userId.isBlank()) return
        try {
            val safeMediaId = progress.mediaId.replace("/", "_")
            val docId = "${safeMediaId}_s${progress.seasonNumber}_e${progress.episodeNumber}"
            val docRef = firestore.collection("users").document(userId)
                .collection("tv_progress").document(docId)

            val payload = hashMapOf(
                "mediaId" to progress.mediaId,
                "seasonNumber" to progress.seasonNumber,
                "episodeNumber" to progress.episodeNumber,
                "status" to progress.status,
                "updatedAt" to progress.updatedAt,
                "userId" to userId,
                "serverUpdatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload tv progress ${progress.mediaId}: ${e.message}")
        }
    }

    suspend fun uploadTvProgressList(userId: String, items: List<TvProgressEntity>) {
        if (userId.isBlank() || items.isEmpty()) return
        items.forEach { uploadTvProgress(userId, it) }
    }

    suspend fun pullCloudData(userId: String): CloudDataPackage? {
        if (userId.isBlank()) return null
        return try {
            val userRef = firestore.collection("users").document(userId)

            val mediaSnap = userRef.collection("tracked_media").get().await()
            val mediaList = mediaSnap.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    MediaEntity(
                        id = id,
                        title = title,
                        mediaType = doc.getString("mediaType") ?: "MOVIE",
                        releaseYear = (doc.getLong("releaseYear") ?: 2024L).toInt(),
                        genres = doc.getString("genres") ?: "",
                        posterUrl = doc.getString("posterUrl") ?: "",
                        backdropUrl = doc.getString("backdropUrl") ?: "",
                        overview = doc.getString("overview") ?: "",
                        directorOrCreator = doc.getString("directorOrCreator") ?: "",
                        cast = doc.getString("cast") ?: "",
                        communityRating = (doc.getDouble("communityRating") ?: 0.0).toFloat(),
                        runtimeMinutes = (doc.getLong("runtimeMinutes") ?: 0L).toInt(),
                        totalSeasons = (doc.getLong("totalSeasons") ?: 0L).toInt(),
                        totalEpisodes = (doc.getLong("totalEpisodes") ?: 1L).toInt(),
                        status = doc.getString("status") ?: "NONE",
                        userRating = (doc.getDouble("userRating") ?: 0.0).toFloat(),
                        userReview = doc.getString("userReview") ?: "",
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        lastWatchedDate = doc.getLong("lastWatchedDate"),
                        watchedEpisodes = (doc.getLong("watchedEpisodes") ?: 0L).toInt(),
                        watchedSeasons = (doc.getLong("watchedSeasons") ?: 0L).toInt(),
                        customWatchlist = doc.getString("customWatchlist"),
                        isCustom = doc.getBoolean("isCustom") ?: false,
                        originalLanguage = doc.getString("originalLanguage") ?: "en",
                        originCountry = doc.getString("originCountry") ?: "US",
                        streamingProviders = doc.getString("streamingProviders") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (_: Exception) {
                    null
                }
            }

            val collectionsSnap = userRef.collection("custom_collections").get().await()
            val collections = collectionsSnap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: doc.id
                if (name.isNotBlank()) {
                    CustomCollectionEntity(
                        name = name,
                        description = doc.getString("description") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } else null
            }

            val prefsDoc = userRef.collection("preferences").document("user_preferences").get().await()
            val prefs = if (prefsDoc.exists()) {
                UserPreferencesEntity(
                    id = 1,
                    regionCode = prefsDoc.getString("regionCode") ?: "AUTO",
                    languageCode = prefsDoc.getString("languageCode") ?: "all",
                    autoDetect = prefsDoc.getBoolean("autoDetect") ?: true,
                    filterCatalogByLanguage = prefsDoc.getBoolean("filterCatalogByLanguage") ?: false,
                    showStreamingProviders = prefsDoc.getBoolean("showStreamingProviders") ?: true
                )
            } else null

            val searchSnap = userRef.collection("search_history").get().await()
            val searches = searchSnap.documents.mapNotNull { doc ->
                val q = doc.getString("query")
                if (!q.isNullOrBlank()) {
                    SearchHistoryEntity(
                        query = q,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } else null
            }

            val favPeopleSnap = userRef.collection("favorite_people").get().await()
            val favPeople = favPeopleSnap.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                FavoritePersonEntity(
                    id = id,
                    personId = doc.getLong("personId") ?: 0L,
                    name = name,
                    profileUrl = doc.getString("profileUrl") ?: "",
                    department = doc.getString("department") ?: "Acting",
                    birthday = doc.getString("birthday") ?: "",
                    placeOfBirth = doc.getString("placeOfBirth") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }

            val tvProgressSnap = userRef.collection("tv_progress").get().await()
            val tvProgress = tvProgressSnap.documents.mapNotNull { doc ->
                val mediaId = doc.getString("mediaId") ?: return@mapNotNull null
                val seasonNum = doc.getLong("seasonNumber")?.toInt() ?: return@mapNotNull null
                val episodeNum = doc.getLong("episodeNumber")?.toInt() ?: return@mapNotNull null
                val status = doc.getString("status") ?: return@mapNotNull null
                TvProgressEntity(
                    mediaId = mediaId,
                    seasonNumber = seasonNum,
                    episodeNumber = episodeNum,
                    status = status,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            CloudDataPackage(mediaList, collections, prefs, searches, favPeople, tvProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling data from Firestore: ${e.message}", e)
            null
        }
    }

    suspend fun deleteUserCloudData(userId: String): Boolean {
        if (userId.isBlank()) return false
        return try {
            val userRef = firestore.collection("users").document(userId)
            val subcollections = listOf(
                "tracked_media",
                "custom_collections",
                "preferences",
                "search_history",
                "favorite_people",
                "tv_progress"
            )
            for (sub in subcollections) {
                val snapshot = userRef.collection(sub).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user cloud data: ${e.message}", e)
            false
        }
    }

    data class CloudDataPackage(
        val media: List<MediaEntity>,
        val collections: List<CustomCollectionEntity>,
        val preferences: UserPreferencesEntity?,
        val searchHistory: List<SearchHistoryEntity>,
        val favoritePeople: List<FavoritePersonEntity> = emptyList(),
        val tvProgress: List<TvProgressEntity> = emptyList()
    )
}
