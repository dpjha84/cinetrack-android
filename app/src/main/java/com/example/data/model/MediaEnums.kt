package com.example.data.model

enum class MediaType(val label: String) {
    MOVIE("Movie"),
    TV_SHOW("TV Show")
}

enum class WatchStatus(val label: String) {
    NONE("Not Tracked"),
    PLAN_TO_WATCH("Plan to Watch"),
    WATCHING("Watching"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped")
}
