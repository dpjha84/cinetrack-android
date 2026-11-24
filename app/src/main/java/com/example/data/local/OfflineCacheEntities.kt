package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.TmdbSearchResultItem

/**
 * Persists TV season metadata (episodeNumber == 0) and episode metadata (episodeNumber > 0)
 * in Room so saved watchlist TV series open with complete season/episode guides offline.
 */
@Entity(
    tableName = "tv_episodes_cache",
    primaryKeys = ["mediaId", "seasonNumber", "episodeNumber"],
    indices = [Index("mediaId")]
)
data class CachedTvEpisodeEntity(
    val mediaId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val imageUrl: String,
    val airDate: String,
    val runtimeMinutes: Int,
    val rating: Float,
    val episodeCount: Int,
    val releaseYear: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toSeasonItem(seriesTitle: String, fallbackBackdropUrl: String): SeasonItem = SeasonItem(
        seriesId = mediaId,
        seriesTitle = seriesTitle,
        seasonNumber = seasonNumber,
        name = title,
        overview = overview,
        posterUrl = imageUrl,
        backdropUrl = fallbackBackdropUrl,
        episodeCount = episodeCount.coerceAtLeast(1),
        releaseYear = releaseYear,
        airDate = airDate,
        rating = rating
    )

    fun toEpisodeItem(
        seriesTitle: String,
        director: String,
        writer: String
    ): EpisodeItem = EpisodeItem(
        id = "${mediaId}_s${seasonNumber}_e${episodeNumber}",
        seriesId = mediaId,
        seriesTitle = seriesTitle,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title,
        overview = overview,
        stillUrl = imageUrl,
        airDate = airDate,
        runtimeMinutes = runtimeMinutes,
        rating = rating,
        director = director,
        writer = writer
    )

    companion object {
        fun fromSeason(season: SeasonItem, now: Long = System.currentTimeMillis()): CachedTvEpisodeEntity =
            CachedTvEpisodeEntity(
                mediaId = season.seriesId,
                seasonNumber = season.seasonNumber,
                episodeNumber = 0,
                title = season.name,
                overview = season.overview,
                imageUrl = season.posterUrl,
                airDate = season.airDate,
                runtimeMinutes = 0,
                rating = season.rating,
                episodeCount = season.episodeCount,
                releaseYear = season.releaseYear,
                updatedAt = now
            )

        fun fromEpisode(episode: EpisodeItem, now: Long = System.currentTimeMillis()): CachedTvEpisodeEntity =
            CachedTvEpisodeEntity(
                mediaId = episode.seriesId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                title = episode.title,
                overview = episode.overview,
                imageUrl = episode.stillUrl,
                airDate = episode.airDate,
                runtimeMinutes = episode.runtimeMinutes,
                rating = episode.rating,
                episodeCount = 1,
                releaseYear = 0,
                updatedAt = now
            )
    }
}

@Dao
interface TvEpisodeCacheDao {
    @Query(
        """
        SELECT * FROM tv_episodes_cache
        WHERE mediaId = :mediaId AND episodeNumber = 0
        ORDER BY seasonNumber ASC
        """
    )
    suspend fun getCachedSeasons(mediaId: String): List<CachedTvEpisodeEntity>

    @Query(
        """
        SELECT * FROM tv_episodes_cache
        WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber AND episodeNumber > 0
        ORDER BY episodeNumber ASC
        """
    )
    suspend fun getCachedEpisodes(mediaId: String, seasonNumber: Int): List<CachedTvEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedTvEpisodeEntity>)

    @Query("DELETE FROM tv_episodes_cache WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: String)

    @Query("DELETE FROM tv_episodes_cache")
    suspend fun clearAll()
}

/**
 * Caches people discovered via multi-search or person search in Room for offline search replay
 * without altering Home's curated Popular Stars table.
 */
@Entity(
    tableName = "cached_people",
    indices = [Index("name")]
)
data class CachedPersonEntity(
    @PrimaryKey val personId: Long,
    val name: String,
    val profileUrl: String,
    val department: String,
    val popularity: Double,
    val knownForIds: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toPersonItem(mediaById: Map<String, MediaEntity>): TmdbSearchResultItem.Person {
        val knownFor = knownForIds.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { mediaById[it] }
        return TmdbSearchResultItem.Person(
            id = personId,
            name = name,
            profileUrl = profileUrl.ifBlank { null },
            department = department.ifBlank { "Acting" },
            knownFor = knownFor,
            popularity = popularity
        )
    }

    companion object {
        fun fromPerson(person: TmdbSearchResultItem.Person, now: Long = System.currentTimeMillis()): CachedPersonEntity =
            CachedPersonEntity(
                personId = person.id,
                name = person.name,
                profileUrl = person.profileUrl.orEmpty(),
                department = person.department,
                popularity = person.popularity,
                knownForIds = person.knownFor.joinToString(",") { it.id },
                updatedAt = now
            )
    }
}

@Dao
interface CachedPersonDao {
    @Query("SELECT * FROM cached_people WHERE personId IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CachedPersonEntity>

    @Query(
        """
        SELECT * FROM cached_people
        WHERE name LIKE '%' || :query || '%'
           OR department LIKE '%' || :query || '%'
        ORDER BY popularity DESC
        LIMIT 30
        """
    )
    suspend fun searchByQuery(query: String): List<CachedPersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<CachedPersonEntity>)

    @Query("DELETE FROM cached_people")
    suspend fun clearAll()
}

/**
 * Caches normalized search query -> ordered result entries in Room so re-running
 * Search History queries offline restores the exact movie, TV show, and person hits.
 */
@Entity(
    tableName = "cached_search_results",
    primaryKeys = ["normalizedQuery", "resultKey"],
    indices = [Index("normalizedQuery")]
)
data class CachedSearchResultEntity(
    val normalizedQuery: String,
    val resultKey: String,
    val resultType: String, // "MEDIA" or "PERSON"
    val mediaId: String = "",
    val personId: Long = 0L,
    val position: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface CachedSearchDao {
    @Query(
        """
        SELECT * FROM cached_search_results
        WHERE normalizedQuery = :normalizedQuery
        ORDER BY position ASC
        """
    )
    suspend fun getResultsForQuery(normalizedQuery: String): List<CachedSearchResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedSearchResultEntity>)

    @Query("DELETE FROM cached_search_results WHERE normalizedQuery = :normalizedQuery")
    suspend fun deleteForQuery(normalizedQuery: String)

    @Query("DELETE FROM cached_search_results")
    suspend fun clearAll()
}
