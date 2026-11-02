package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val regionCode: String = "AUTO", // "AUTO", "US", "GB", "IN", "JP", "KR", etc.
    val languageCode: String = "all", // "all", "en", "ja", "ko", "es", "hi", "fr", "de"
    val autoDetect: Boolean = true,
    val filterCatalogByLanguage: Boolean = false,
    val showStreamingProviders: Boolean = true,
    val customTmdbApiKey: String = ""
)

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesDirect(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: UserPreferencesEntity)
}
