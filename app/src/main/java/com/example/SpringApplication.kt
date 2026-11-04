package com.example

import android.app.Application
import com.example.data.local.SpringDatabase
import com.example.data.repository.MusicRepository
import com.example.player.PlayerManager

import com.example.data.firestore.FirestoreSyncManager
import com.example.ui.theme.AiThemeManager

class SpringApplication : Application() {
    lateinit var database: SpringDatabase
        private set

    lateinit var firestoreSyncManager: FirestoreSyncManager
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playerManager: PlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        AiThemeManager.init(this)
        database = SpringDatabase.getInstance(this)
        firestoreSyncManager = FirestoreSyncManager(this, database.playlistDao(), database.songDao())
        repository = MusicRepository(database.songDao(), database.playlistDao(), this, firestoreSyncManager)
        playerManager = PlayerManager(this)
    }
}

