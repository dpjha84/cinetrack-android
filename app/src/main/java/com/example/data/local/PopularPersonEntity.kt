package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem

@Entity(tableName = "popular_people")
data class PopularPersonEntity(
    @PrimaryKey val personId: Long,
    val name: String,
    val profileUrl: String = "",
    val department: String = "Acting",
    val popularity: Double = 0.0,
    val knownForTypes: String = "",
    val knownForIds: String = "",
    val knownForTitles: String = "",
    val pageLoaded: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun supportsMediaType(type: MediaType?): Boolean {
        if (type == null) return true
        val tokens = knownForTypes.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        return tokens.any { it.equals(type.name, ignoreCase = true) }
    }

    fun toSearchResultPerson(knownForLookup: Map<String, MediaEntity> = emptyMap()): TmdbSearchResultItem.Person {
        val resolvedKnownFor = knownForIds.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { knownForLookup[it] }
        return TmdbSearchResultItem.Person(
            id = personId,
            name = name,
            profileUrl = profileUrl.ifBlank { null },
            department = department.ifBlank { "Acting" },
            knownFor = resolvedKnownFor,
            popularity = popularity
        )
    }

    companion object {
        fun fromPerson(person: TmdbSearchResultItem.Person, pageLoaded: Int): PopularPersonEntity {
            val types = person.knownFor.map { it.getMediaTypeEnum().name }.distinct().joinToString(",")
            val ids = person.knownFor.map { it.id }.distinct().joinToString(",")
            val titles = person.knownFor.map { it.title }.filter { it.isNotBlank() }.distinct().joinToString(", ")
            return PopularPersonEntity(
                personId = person.id,
                name = person.name,
                profileUrl = person.profileUrl.orEmpty(),
                department = person.department.ifBlank { "Acting" },
                popularity = person.popularity,
                knownForTypes = types,
                knownForIds = ids,
                knownForTitles = titles,
                pageLoaded = pageLoaded,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
