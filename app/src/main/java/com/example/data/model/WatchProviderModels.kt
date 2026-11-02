package com.example.data.model

enum class WatchProviderType(val label: String) {
    STREAM("Stream"),
    RENT("Rent"),
    BUY("Buy"),
    FREE("Free")
}

data class WatchProviderItem(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val type: WatchProviderType,
    val webLink: String? = null
)

data class WatchProvidersInfo(
    val regionCode: String,
    val regionLabel: String,
    val stream: List<WatchProviderItem> = emptyList(),
    val rent: List<WatchProviderItem> = emptyList(),
    val buy: List<WatchProviderItem> = emptyList(),
    val free: List<WatchProviderItem> = emptyList(),
    val justWatchLink: String? = null
) {
    val allProviders: List<WatchProviderItem>
        get() = (stream + free + rent + buy).distinctBy { it.name }

    val isEmpty: Boolean
        get() = stream.isEmpty() && rent.isEmpty() && buy.isEmpty() && free.isEmpty()
}
