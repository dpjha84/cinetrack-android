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
        FavoritePersonEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun customCollectionDao(): CustomCollectionDao
    abstract fun tvProgressDao(): TvProgressDao
    abstract fun favoritePersonDao(): FavoritePersonDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinetrack_database"
                )
                    .addMigrations(MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
