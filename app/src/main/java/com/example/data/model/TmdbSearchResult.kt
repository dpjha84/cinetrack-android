package com.example.data.model

import com.example.data.local.MediaEntity

sealed class TmdbSearchResultItem {
    data class Media(
        val entity: MediaEntity,
        val isMovie: Boolean
    ) : TmdbSearchResultItem()

    data class Person(
        val id: Long,
        val name: String,
        val profileUrl: String?,
        val department: String,
        val knownFor: List<MediaEntity>,
        val popularity: Double = 0.0
    ) : TmdbSearchResultItem()
}

data class CastMemberItem(
    val id: Long = 0L,
    val name: String,
    val character: String? = null,
    val profileUrl: String? = null
)

enum class MultiSearchCategory(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV_SHOWS("TV Shows"),
    PEOPLE("People")
}
