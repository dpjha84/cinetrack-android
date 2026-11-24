package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaEntity::class,
        UserPreferencesEntity::class,
        SearchHistoryEntity::class,
        CustomCollectionEntity::class,
        TvProgressEntity::class,
        FavoritePersonEntity::class,
        PopularPersonEntity::class,
        CachedTvEpisodeEntity::class,
        CachedPersonEntity::class,
        CachedSearchResultEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun customCollectionDao(): CustomCollectionDao
    abstract fun tvProgressDao(): TvProgressDao
    abstract fun favoritePersonDao(): FavoritePersonDao
    abstract fun popularPersonDao(): PopularPersonDao
    abstract fun tvEpisodeCacheDao(): TvEpisodeCacheDao
    abstract fun cachedPersonDao(): CachedPersonDao
    abstract fun cachedSearchDao(): CachedSearchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_people` (
                        `id` TEXT NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `profileUrl` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `birthday` TEXT NOT NULL,
                        `placeOfBirth` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // WatchPilot v2.0.0 non-destructive in-place schema upgrade preserving all v1 user libraries
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_items_releaseYear` ON `media_items` (`releaseYear`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `media_items` ADD COLUMN `feedTags` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `popular_people` (
                        `personId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `profileUrl` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `popularity` REAL NOT NULL,
                        `knownForTypes` TEXT NOT NULL,
                        `knownForIds` TEXT NOT NULL,
                        `knownForTitles` TEXT NOT NULL,
                        `pageLoaded` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`personId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `search_history` ADD COLUMN `resultCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tv_episodes_cache` (
                        `mediaId` TEXT NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `overview` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `airDate` TEXT NOT NULL,
                        `runtimeMinutes` INTEGER NOT NULL,
                        `rating` REAL NOT NULL,
                        `episodeCount` INTEGER NOT NULL,
                        `releaseYear` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`, `seasonNumber`, `episodeNumber`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tv_episodes_cache_mediaId` ON `tv_episodes_cache` (`mediaId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_people` (
                        `personId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `profileUrl` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `popularity` REAL NOT NULL,
                        `knownForIds` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`personId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_people_name` ON `cached_people` (`name`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_search_results` (
                        `normalizedQuery` TEXT NOT NULL,
                        `resultKey` TEXT NOT NULL,
                        `resultType` TEXT NOT NULL,
                        `mediaId` TEXT NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`normalizedQuery`, `resultKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_search_results_normalizedQuery` ON `cached_search_results` (`normalizedQuery`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinetrack_database"
                )
                    .addMigrations(
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
