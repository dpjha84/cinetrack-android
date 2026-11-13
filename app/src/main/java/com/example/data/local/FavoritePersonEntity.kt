package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_people")
data class FavoritePersonEntity(
    @PrimaryKey val id: String,
    val personId: Long,
    val name: String,
    val profileUrl: String = "",
    val department: String = "Acting",
    val birthday: String = "",
    val placeOfBirth: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildStorageId(personId: Long, name: String): String {
            return if (personId > 0L) {
                "tmdb_p_$personId"
            } else {
                val slug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                "name_${slug.ifBlank { "unknown" }}"
            }
        }
    }
}
