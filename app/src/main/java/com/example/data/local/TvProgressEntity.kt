package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Stores progress for TV Shows.
 * - When [episodeNumber] == 0, this represents the watch status for the entire Season [seasonNumber].
 * - When [episodeNumber] > 0, this represents the watch status for Episode [episodeNumber] in Season [seasonNumber].
 */
@Entity(
    tableName = "tv_progress",
    primaryKeys = ["mediaId", "seasonNumber", "episodeNumber"]
)
data class TvProgressEntity(
    val mediaId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val status: String, // "WATCHED", "WATCHING", "PLAN_TO_WATCH", "UNWATCHED"
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface TvProgressDao {

    @Query("SELECT * FROM tv_progress WHERE mediaId = :mediaId")
    fun getProgressForMedia(mediaId: String): Flow<List<TvProgressEntity>>

    @Query("SELECT * FROM tv_progress WHERE mediaId = :mediaId")
    suspend fun getProgressForMediaDirect(mediaId: String): List<TvProgressEntity>

    @Query("SELECT * FROM tv_progress")
    suspend fun getAllProgressList(): List<TvProgressEntity>

    @Query("SELECT * FROM tv_progress WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun getSpecificProgress(mediaId: String, seasonNumber: Int, episodeNumber: Int): TvProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TvProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TvProgressEntity>)

    @Query("DELETE FROM tv_progress WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun deleteProgress(mediaId: String, seasonNumber: Int, episodeNumber: Int)

    @Query("DELETE FROM tv_progress WHERE mediaId = :mediaId")
    suspend fun deleteAllForMedia(mediaId: String)

    @Query("DELETE FROM tv_progress")
    suspend fun clearAll()
}
