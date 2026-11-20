package com.example

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleMediaPlayFromSearch(intent)
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMediaPlayFromSearch(intent)
    }

    private fun handleMediaPlayFromSearch(intent: Intent?) {
        if (intent?.action != "android.media.action.MEDIA_PLAY_FROM_SEARCH") return
        val query = intent.getStringExtra(SearchManager.QUERY)?.trim().orEmpty()
        if (query.isBlank()) return
        val app = applicationContext as? SpringApplication ?: return
        lifecycleScope.launch {
            val results = runCatching { app.repository.searchSongs(query, "Voice Search") }.getOrNull().orEmpty()
            if (results.isNotEmpty()) {
                app.playerManager.playSong(results.first(), results)
            }
        }
    }
}
