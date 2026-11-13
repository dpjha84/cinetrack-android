package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePersonDao {

    @Query("SELECT * FROM favorite_people ORDER BY createdAt DESC")
    fun getAllFavoritePeople(): Flow<List<FavoritePersonEntity>>

    @Query("SELECT * FROM favorite_people ORDER BY createdAt DESC")
    suspend fun getAllFavoritePeopleList(): List<FavoritePersonEntity>

    @Query("SELECT * FROM favorite_people WHERE id = :id")
    suspend fun getById(id: String): FavoritePersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: FavoritePersonEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(people: List<FavoritePersonEntity>)

    @Query("DELETE FROM favorite_people WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM favorite_people")
    suspend fun clearAll()
}
