package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PopularPersonDao {

    @Query("SELECT * FROM popular_people ORDER BY pageLoaded ASC, popularity DESC, personId ASC")
    fun getAllPopularPeople(): Flow<List<PopularPersonEntity>>

    @Query(
        """
        SELECT * FROM popular_people
        WHERE (:mediaType IS NULL OR knownForTypes LIKE '%' || :mediaType || '%')
        ORDER BY pageLoaded ASC, popularity DESC, personId ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPagedPopularPeople(
        mediaType: String?,
        limit: Int,
        offset: Int
    ): List<PopularPersonEntity>

    @Query("SELECT COUNT(*) FROM popular_people")
    suspend fun countPopularPeople(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PopularPersonEntity>)

    @Query("DELETE FROM popular_people")
    suspend fun clearAll()
}
