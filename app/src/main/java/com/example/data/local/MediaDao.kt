package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status != 'NONE' OR isFavorite = 1 OR userRating > 0 ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getTrackedMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'WATCHING' ORDER BY lastWatchedDate DESC")
    fun getContinueWatching(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getFavorites(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'PLAN_TO_WATCH' OR customWatchlist IS NOT NULL OR isFavorite = 1 ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getWatchlistMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaById(id: String): Flow<MediaEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaByIdDirect(id: String): MediaEntity?

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    suspend fun getAllMediaList(): List<MediaEntity>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun countTotal(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaEntity)

    @Update
    suspend fun update(item: MediaEntity)

    @Query("UPDATE media_items SET status = :status, lastWatchedDate = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE media_items SET watchedEpisodes = :episodes, lastWatchedDate = :timestamp WHERE id = :id")
    suspend fun updateWatchedEpisodes(id: String, episodes: Int, timestamp: Long)

    @Query("UPDATE media_items SET userRating = :rating, userReview = :review WHERE id = :id")
    suspend fun updateRatingAndReview(id: String, rating: Float, review: String)

    @Query("UPDATE media_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("UPDATE media_items SET customWatchlist = :watchlist WHERE id = :id")
    suspend fun updateCustomWatchlist(id: String, watchlist: String?)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
