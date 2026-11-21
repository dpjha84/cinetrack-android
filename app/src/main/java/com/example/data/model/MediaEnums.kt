package com.example.data.model

enum class MediaType(val label: String) {
    MOVIE("Movie"),
    TV_SHOW("TV Show")
}

enum class WatchStatus(val label: String) {
    NONE("Not Tracked"),
    PLAN_TO_WATCH("Watchlist"),
    WATCHING("Watching"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped")
}

enum class HomeCatalogSection(val feedTag: String, val displayPrefix: String) {
    POPULAR("POPULAR", "Popular"),
    NOW_PLAYING("NOW_PLAYING", "Now Playing"),
    UPCOMING("UPCOMING", "Upcoming"),
    POPULAR_PEOPLE("POPULAR_PEOPLE", "Popular People")
}
