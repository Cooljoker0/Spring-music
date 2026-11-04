package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedSongEntity::class, UserPlaylistEntity::class], version = 1, exportSchema = false)
abstract class SpringDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

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
