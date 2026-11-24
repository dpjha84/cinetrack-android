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

    @Query("SELECT * FROM media_items WHERE status != 'NONE' OR isFavorite = 1 OR userRating > 0 OR (customWatchlist IS NOT NULL AND customWatchlist != '') ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getTrackedMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'WATCHING' ORDER BY lastWatchedDate DESC")
    fun getContinueWatching(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getFavorites(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'PLAN_TO_WATCH' OR (customWatchlist IS NOT NULL AND customWatchlist != '') OR isFavorite = 1 ORDER BY lastWatchedDate DESC, createdAt DESC")
    fun getWatchlistMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE status != 'NONE' OR (customWatchlist IS NOT NULL AND customWatchlist != '') OR isFavorite = 1 ORDER BY lastWatchedDate DESC, createdAt DESC")
    suspend fun getWatchlistMediaDirect(): List<MediaEntity>

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

    @Query("UPDATE media_items SET posterUrl = :posterUrl, backdropUrl = :backdropUrl WHERE id = :id")
    suspend fun updateMediaImages(id: String, posterUrl: String, backdropUrl: String)

    @Query("UPDATE media_items SET streamingProviders = :streamingProviders WHERE id = :id")
    suspend fun updateStreamingProviders(id: String, streamingProviders: String)

    @Query("UPDATE media_items SET status = 'NONE', isFavorite = 0, userRating = 0.0, userReview = '', watchedEpisodes = 0, watchedSeasons = 0, customWatchlist = NULL, lastWatchedDate = NULL")
    suspend fun resetAllTrackingData()

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MediaEntity>

    @Query("UPDATE media_items SET feedTags = :feedTags WHERE id = :id")
    suspend fun updateFeedTags(id: String, feedTags: String)

    @Query(
        """
        SELECT * FROM media_items
        WHERE feedTags LIKE '%' || :feedTag || '%'
          AND (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:genreFilter IS NULL OR genres LIKE '%' || :genreFilter || '%')
          AND (:languageFilter IS NULL OR originalLanguage = :languageFilter)
        ORDER BY releaseYear DESC, communityRating DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPagedMediaByFeed(
        feedTag: String,
        mediaType: String?,
        genreFilter: String?,
        languageFilter: String?,
        limit: Int,
        offset: Int
    ): List<MediaEntity>

    @Query(
        """
        SELECT * FROM media_items
        WHERE (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:genreFilter IS NULL OR genres LIKE '%' || :genreFilter || '%')
          AND (:languageFilter IS NULL OR originalLanguage = :languageFilter)
        ORDER BY releaseYear DESC, communityRating DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPagedAllCatalogMedia(
        mediaType: String?,
        genreFilter: String?,
        languageFilter: String?,
        limit: Int,
        offset: Int
    ): List<MediaEntity>

    @Query(
        """
        SELECT * FROM media_items
        WHERE title LIKE '%' || :query || '%'
           OR `cast` LIKE '%' || :query || '%'
           OR directorOrCreator LIKE '%' || :query || '%'
           OR genres LIKE '%' || :query || '%'
           OR overview LIKE '%' || :query || '%'
           OR (customWatchlist IS NOT NULL AND customWatchlist LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN status != 'NONE' OR isFavorite = 1 OR (customWatchlist IS NOT NULL AND customWatchlist != '') THEN 0 ELSE 1 END,
          CASE WHEN LOWER(title) LIKE LOWER(:query || '%') THEN 0 ELSE 1 END,
          communityRating DESC
        LIMIT :limit
        """
    )
    suspend fun searchMediaOffline(query: String, limit: Int = 60): List<MediaEntity>

    @Query("DELETE FROM media_items WHERE isCustom = 1")
    suspend fun deleteCustomMedia()

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
