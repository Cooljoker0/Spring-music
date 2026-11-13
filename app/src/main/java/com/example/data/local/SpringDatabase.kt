package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedSongEntity::class,
        UserPlaylistEntity::class,
        SongPlayHistoryEntity::class,
        UserTasteProfileEntity::class,
        RecommendationFeedbackEntity::class,
        CachedRecommendationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SpringDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recommendationDao(): RecommendationDao

    companion object {
        @Volatile
        private var INSTANCE: SpringDatabase? = null

        fun getInstance(context: Context): SpringDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpringDatabase::class.java,
                    "spring_music.db"
                ).fallbackToDestructiveMigration(true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
