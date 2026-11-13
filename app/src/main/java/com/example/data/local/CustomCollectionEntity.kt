package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_collections")
data class CustomCollectionEntity(
    @PrimaryKey
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CustomCollectionDao {

    @Query("SELECT * FROM custom_collections ORDER BY createdAt ASC")
    fun getAllCollections(): Flow<List<CustomCollectionEntity>>

    @Query("SELECT COUNT(*) FROM custom_collections")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collection: CustomCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(collections: List<CustomCollectionEntity>)

    @Query("DELETE FROM custom_collections WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM custom_collections")
    suspend fun clearAll()
}
