package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE LOWER(query) = LOWER(:query) LIMIT 1")
    suspend fun getByQuery(query: String): SearchHistoryEntity?

    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SearchHistoryEntity>)

    @Query("UPDATE search_history SET resultCount = :resultCount WHERE LOWER(query) = LOWER(:query)")
    suspend fun updateResultCount(query: String, resultCount: Int)

    @Query("DELETE FROM search_history WHERE LOWER(query) = LOWER(:query)")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
