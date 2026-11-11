package com.example.data.network

import com.example.data.model.Playlist
import java.net.URLEncoder

data class SpotifyCatalogEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val songCount: Int,
    val keywords: List<String>,
    val category: String = "Editorial"
)

object SpotifySearchEngine {

    val SPOTIFY_CATALOG: List<SpotifyCatalogEntry> = listOf(
        // Indian Indie & Flagship Playlists
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX5q67ZpWyRrZ",
            title = "Indie India",
            subtitle = "Spotify Editorial • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f0000000288863fc9f87140f09a63aa53",
            songCount = 75,
            keywords = listOf("indie", "india", "indie india", "indian indie", "anuv jain", "prateek kuhad", "independent", "acoustic", "lofi india", "zaeden"),
            category = "Editorial"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZEVXbLZ52XmnySJg",
            title = "Top 50 - India",
            subtitle = "Spotify Charts • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002b80456209214df9275cb1a5e",
            songCount = 50,
            keywords = listOf("top 50", "india", "top 50 india", "top hits india", "charts", "trending", "daily top"),
            category = "Charts"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZEVXbMDoHDwVN2tF",
            title = "Top 50 - Global",
            subtitle = "Spotify Charts • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002a24c58cf3e20d2d3e9c9c34a",
            songCount = 50,
            keywords = listOf("global", "top 50 global", "charts", "world", "trending global", "billboard"),
            category = "Charts"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DXcBWIGoYBM5M",
            title = "Today's Top Hits",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000029bb644264627d33e5c9a4185",
            songCount = 50,
            keywords = listOf("today", "top hits", "today's top hits", "global pop", "billboard", "english hits"),
            category = "Editorial"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX0XUfTFmNBRM",
            title = "Bollywood Butter",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002f90a980757a3e4307ef19688",
            songCount = 50,
            keywords = listOf("bollywood", "butter", "bollywood butter", "hindi", "hot hits hindi", "filmi", "chartbusters", "arijit"),
            category = "Editorial"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX5cZuAHLNjGz",
            title = "Punjabi 101",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000024f2b9637c35ae97d91788267",
            songCount = 50,
            keywords = listOf("punjabi", "punjabi 101", "bhangra", "diljit dosanjh", "sidhu moose wala", "karan aujla", "ap dhillon", "shubh"),
            category = "Regional"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX4WYpdgoIcn6",
            title = "I-Pop Superhits",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002c918ef8f8eb04a7541662589",
            songCount = 50,
            keywords = listOf("ipop", "i-pop", "indian pop", "darshan raval", "armaan malik", "neha kakkar", "pop hindi"),
            category = "Editorial"
        ),
        SpotifyCatalogEntry(
            id = "2sHANPCjmBnITWYakB00Lh",
            title = "Sad Songs Bollywood",
            subtitle = "Spotify Community • 50 songs",
            coverUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=500&q=80",
            songCount = 50,
            keywords = listOf("sad", "sad songs", "sad hindi", "dard", "breakup", "heartbroken", "emotional", "arijit sad"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "38Sx0UXrRMnNrtJlNLsv0Z",
            title = "Sad Song Hindi Remix",
            subtitle = "Spotify Community • 50 songs",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80",
            songCount = 50,
            keywords = listOf("sad remix", "lofi hindi", "slowed reverb", "sad song hindi remix", "broken heart", "vibes"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "4MKclpF124eMEoebEph7EU",
            title = "Upbeat Sad Songs Bollywood",
            subtitle = "Spotify Community • 50 songs",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            songCount = 50,
            keywords = listOf("emraan hashmi", "himesh reshammiya", "kk", "upbeat sad", "2000s bollywood", "nostalgic hindi"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX5KpP2LN299J",
            title = "This Is Taylor Swift",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000021a8c4618da40182c1615f3ec",
            songCount = 50,
            keywords = listOf("taylor swift", "taylor", "swiftie", "1989", "folklore", "midnights", "lover", "red", "eras"),
            category = "Artist"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX76Wlfdnj7AP",
            title = "Beast Mode - Workout Energy",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000025f385c7d0d0fbb3298a0c20a",
            songCount = 50,
            keywords = listOf("workout", "gym", "beast mode", "energy", "fitness", "motivation", "pump", "training", "phonk"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DXdLEN7aqioXM",
            title = "Chill Lofi Beats",
            subtitle = "Spotify Editorial • 60 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002bca487b3370f80b2a8d5f308",
            songCount = 60,
            keywords = listOf("lofi", "chill", "lo-fi", "study", "relax", "sleep", "beats", "chillhop", "vibes"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DXbYM3nMM0oPk",
            title = "Mega Hit Mix",
            subtitle = "Spotify Editorial • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000027f3b89fa5a9ebf5aa7c90b82",
            songCount = 75,
            keywords = listOf("mega hit mix", "mix", "dance", "party", "hits", "club", "remix"),
            category = "Editorial"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DWZeKCadgRdKQ",
            title = "Deep Focus",
            subtitle = "Spotify Editorial • 50 songs",
            coverUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&q=80",
            songCount = 50,
            keywords = listOf("focus", "deep focus", "study", "work", "coding", "concentration", "calm", "ambient"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX4sWSpwq3LiO",
            title = "Peaceful Piano",
            subtitle = "Spotify Editorial • 75 songs",
            coverUrl = "https://images.unsplash.com/photo-1520523839898-5071270498a4?w=500&q=80",
            songCount = 75,
            keywords = listOf("piano", "peaceful piano", "instrumental", "sleep", "relaxing", "peaceful", "calm"),
            category = "Mood"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX5Ejj0EkURtP",
            title = "All Out 2010s",
            subtitle = "Spotify Editorial • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f0000000236a28ff7227e7d6cfec390be",
            songCount = 75,
            keywords = listOf("2010s", "decade", "throwback", "nostalgia", "avicii", "katy perry", "bruno mars"),
            category = "Decades"
        ),
        SpotifyCatalogEntry(
            id = "37i9dQZF1DX4o1oenSJRJd",
            title = "All Out 2000s",
            subtitle = "Spotify Editorial • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002e20b327b0365779c1186e246",
            songCount = 75,
            keywords = listOf("2000s", "throwback", "retro", "nostalgia", "rihanna", "eminem", "britney"),
            category = "Decades"
        )
    )

    /**
     * Searches Spotify playlists by matching against the verified catalog.
     * Computes relevance scores so exact/prefix matches appear first.
     * If no exact match is found, dynamically generates a Spotify mix result.
     */
    fun searchSpotifyPlaylists(query: String): List<Playlist> {
        val qClean = query.trim().lowercase()
        if (qClean.isBlank()) return emptyList()

        val tokens = qClean.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val scored = SPOTIFY_CATALOG.mapNotNull { entry ->
            val titleLower = entry.title.lowercase()
            var score = 0

            // Exact match
            if (titleLower == qClean) {
                score += 150
            } else if (titleLower.startsWith(qClean)) {
                score += 100
            } else if (titleLower.contains(qClean)) {
                score += 70
            }

            // Token match
            var tokensMatched = 0
            for (t in tokens) {
                if (titleLower.contains(t)) {
                    score += 30
                    tokensMatched++
                } else if (entry.keywords.any { it.contains(t) || t.contains(it) }) {
                    score += 20
                    tokensMatched++
                }
            }

            // Keyword exact match
            if (entry.keywords.any { it == qClean }) {
                score += 60
            }

            if (score > 0) {
                Pair(entry, score)
            } else null
        }.sortedByDescending { it.second }

        val results = scored.map { (entry, _) ->
            Playlist(
                id = "spotify_${entry.id}",
                title = entry.title,
                subtitle = entry.subtitle,
                coverUrl = entry.coverUrl,
                songCount = entry.songCount,
                songs = emptyList()
            )
        }.toMutableList()

        // If user searched for something specific that didn't have a direct curated entry,
        // provide a high-quality Spotify Mix candidate so user always has an importable Spotify result!
        if (results.isEmpty() || results.none { it.title.equals(query.trim(), ignoreCase = true) }) {
            val capitalized = query.trim().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            val smartMix = Playlist(
                id = "spotify_smart_${URLEncoder.encode(query.trim(), "UTF-8")}",
                title = "$capitalized (Spotify Mix)",
                subtitle = "Spotify Instant Mix • 25 songs",
                coverUrl = getThematicCover(qClean),
                songCount = 25,
                songs = emptyList()
            )
            // Add as first or companion result
            if (results.isEmpty()) {
                results.add(smartMix)
            } else if (results.size < 4) {
                results.add(1, smartMix)
            }
        }

        return results
    }

    private fun getThematicCover(query: String): String {
        return when {
            query.contains("sad") || query.contains("dard") ->
                "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=500&q=80"
            query.contains("gym") || query.contains("workout") ->
                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&q=80"
            query.contains("romance") || query.contains("love") ->
                "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=500&q=80"
            query.contains("party") || query.contains("dance") ->
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&q=80"
            query.contains("lofi") || query.contains("chill") ->
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"
            query.contains("indie") ->
                "https://i.scdn.co/image/ab67706f0000000288863fc9f87140f09a63aa53"
            else ->
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"
        }
    }
}
