package com.example.data.recommendation

import android.content.Context
import com.example.data.local.ArtistPlayCount
import com.example.data.local.CachedRecommendationEntity
import com.example.data.local.MoodPlayCount
import com.example.data.local.PlaylistDao
import com.example.data.local.RecommendationDao
import com.example.data.local.RecommendationFeedbackEntity
import com.example.data.local.SongDao
import com.example.data.local.SongPlayCount
import com.example.data.local.SongPlayHistoryEntity
import com.example.data.local.UserTasteProfileEntity
import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.JioSaavnDirectEngine
import com.example.data.network.JukeboxClient
import com.example.data.network.PresetCatalog
import com.example.data.network.SpotifySearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class FeedbackType {
    LIKE,
    DISLIKE,
    NOT_INTERESTED,
    PLAY_MORE_LIKE_THIS,
    DONT_RECOMMEND_ARTIST
}

data class RecommendationSection(
    val categoryKey: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val canRefresh: Boolean = false
)

data class RecommendationSectionsState(
    val isLoading: Boolean = true,
    val isPersonalized: Boolean = true,
    val sections: List<RecommendationSection> = emptyList(),
    val discoverWeekly: List<Song> = emptyList(),
    val similarSongs: List<Song> = emptyList(),
    val lastUpdated: Long = 0L,
    val errorMessage: String? = null
)

class RecommendationEngine(
    val recommendationDao: RecommendationDao,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val context: Context
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val _recommendationsState = MutableStateFlow(RecommendationSectionsState())
    val recommendationsState: StateFlow<RecommendationSectionsState> = _recommendationsState.asStateFlow()

    private val _similarSongsState = MutableStateFlow<List<Song>>(emptyList())
    val similarSongsState: StateFlow<List<Song>> = _similarSongsState.asStateFlow()

    private val _isPersonalizationEnabled = MutableStateFlow(true)
    val isPersonalizationEnabled: StateFlow<Boolean> = _isPersonalizationEnabled.asStateFlow()

    private val _dislikedSongIds = mutableSetOf<String>()
    private val _blockedArtists = mutableSetOf<String>()

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debouncedRefreshJob: Job? = null

    fun scheduleDynamicRefresh() {
        if (!_isPersonalizationEnabled.value) return
        debouncedRefreshJob?.cancel()
        debouncedRefreshJob = engineScope.launch {
            delay(1200)
            refreshAllRecommendations(force = true)
        }
    }

    /**
     * Initializes the recommendation engine: loads cached recommendations immediately,
     * loads user feedback blacklists, and triggers a fresh background recommendation build.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        loadBlacklists()
        loadCachedRecommendations()
        refreshAllRecommendations(force = false)
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        _isPersonalizationEnabled.value = enabled
    }

    private suspend fun loadBlacklists() {
        val excludedSongs = recommendationDao.getExcludedSongIds()
        val blocked = recommendationDao.getBlockedArtists()
        synchronized(this) {
            _dislikedSongIds.clear()
            _dislikedSongIds.addAll(excludedSongs)
            _blockedArtists.clear()
            _blockedArtists.addAll(blocked.map { it.lowercase() })
        }
    }

    /**
     * Track a song playback event and dynamically update user's taste profile.
     */
    suspend fun recordSongPlayEvent(
        song: Song,
        playDurationMs: Long,
        isCompleted: Boolean,
        isSkipped: Boolean
    ) = withContext(Dispatchers.IO) {
        val event = SongPlayHistoryEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            coverUrl = song.coverUrl,
            audioUrl = song.audioUrl,
            durationMs = song.durationMs,
            mood = song.mood,
            timestamp = System.currentTimeMillis(),
            playDurationMs = playDurationMs,
            isCompleted = isCompleted,
            isSkipped = isSkipped
        )
        recommendationDao.insertPlayEvent(event)

        // Update artist affinity
        val primaryArtist = song.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: song.artist
        if (primaryArtist.isNotBlank()) {
            val artistKey = "artist:${primaryArtist.lowercase()}"
            val existing = recommendationDao.getTasteProfile(artistKey)
            val scoreDelta = when {
                isCompleted -> 3.0f
                isSkipped -> -2.5f
                playDurationMs > 30_000L -> 1.0f
                else -> 0.2f
            }
            val newScore = ((existing?.affinityScore ?: 0f) + scoreDelta).coerceAtLeast(0f)
            val newPlayCount = (existing?.playCount ?: 0) + (if (!isSkipped) 1 else 0)
            val newSkipCount = (existing?.skipCount ?: 0) + (if (isSkipped) 1 else 0)
            recommendationDao.upsertTasteProfile(
                UserTasteProfileEntity(
                    key = artistKey,
                    type = "artist",
                    name = primaryArtist,
                    affinityScore = newScore,
                    playCount = newPlayCount,
                    skipCount = newSkipCount
                )
            )
        }

        // Update mood/genre affinity
        if (song.mood.isNotBlank() && song.mood != "Spring" && song.mood != "Unknown") {
            val moodKey = "mood:${song.mood.lowercase()}"
            val existing = recommendationDao.getTasteProfile(moodKey)
            val scoreDelta = if (isCompleted) 2.0f else if (isSkipped) -1.5f else 0.5f
            val newScore = ((existing?.affinityScore ?: 0f) + scoreDelta).coerceAtLeast(0f)
            recommendationDao.upsertTasteProfile(
                UserTasteProfileEntity(
                    key = moodKey,
                    type = "mood",
                    name = song.mood,
                    affinityScore = newScore,
                    playCount = (existing?.playCount ?: 0) + 1,
                    skipCount = (existing?.skipCount ?: 0) + (if (isSkipped) 1 else 0)
                )
            )
        }

        if (isCompleted || isSkipped || playDurationMs > 30_000L) {
            scheduleDynamicRefresh()
        }
    }

    /**
     * Record explicit feedback: Like, Dislike, Not Interested, Block Artist, or More Like This.
     */
    suspend fun recordFeedback(song: Song, feedbackType: FeedbackType) = withContext(Dispatchers.IO) {
        val primaryArtist = song.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: song.artist

        when (feedbackType) {
            FeedbackType.LIKE -> {
                songDao.updateFavorite(song.id, true)
                // Boost artist affinity
                val artistKey = "artist:${primaryArtist.lowercase()}"
                val existing = recommendationDao.getTasteProfile(artistKey)
                recommendationDao.upsertTasteProfile(
                    UserTasteProfileEntity(
                        key = artistKey,
                        type = "artist",
                        name = primaryArtist,
                        affinityScore = (existing?.affinityScore ?: 0f) + 5.0f,
                        playCount = (existing?.playCount ?: 0) + 1,
                        skipCount = existing?.skipCount ?: 0
                    )
                )
            }
            FeedbackType.DISLIKE -> {
                val feedback = RecommendationFeedbackEntity(
                    id = "${song.id}_dislike",
                    targetId = song.id,
                    targetType = "song",
                    feedbackType = "dislike",
                    targetName = song.title
                )
                recommendationDao.insertFeedback(feedback)
                synchronized(this) { _dislikedSongIds.add(song.id) }
                // Penalize artist affinity
                val artistKey = "artist:${primaryArtist.lowercase()}"
                val existing = recommendationDao.getTasteProfile(artistKey)
                if (existing != null) {
                    recommendationDao.upsertTasteProfile(
                        existing.copy(affinityScore = (existing.affinityScore - 4.0f).coerceAtLeast(0f))
                    )
                }
                removeSongFromActiveRecommendations(song.id)
            }
            FeedbackType.NOT_INTERESTED -> {
                val feedback = RecommendationFeedbackEntity(
                    id = "${song.id}_not_interested",
                    targetId = song.id,
                    targetType = "song",
                    feedbackType = "not_interested",
                    targetName = song.title
                )
                recommendationDao.insertFeedback(feedback)
                synchronized(this) { _dislikedSongIds.add(song.id) }
                removeSongFromActiveRecommendations(song.id)
            }
            FeedbackType.DONT_RECOMMEND_ARTIST -> {
                val feedback = RecommendationFeedbackEntity(
                    id = "${primaryArtist.lowercase()}_block",
                    targetId = primaryArtist,
                    targetType = "artist",
                    feedbackType = "block_artist",
                    targetName = primaryArtist
                )
                recommendationDao.insertFeedback(feedback)
                synchronized(this) { _blockedArtists.add(primaryArtist.lowercase()) }
                removeArtistFromActiveRecommendations(primaryArtist)
            }
            FeedbackType.PLAY_MORE_LIKE_THIS -> {
                // Boost artist and mood
                val artistKey = "artist:${primaryArtist.lowercase()}"
                val existing = recommendationDao.getTasteProfile(artistKey)
                recommendationDao.upsertTasteProfile(
                    UserTasteProfileEntity(
                        key = artistKey,
                        type = "artist",
                        name = primaryArtist,
                        affinityScore = (existing?.affinityScore ?: 0f) + 4.0f,
                        playCount = (existing?.playCount ?: 0) + 1,
                        skipCount = existing?.skipCount ?: 0
                    )
                )
                // Trigger similar songs immediate fetch
                fetchSimilarSongs(song)
            }
        }
        scheduleDynamicRefresh()
    }

    private fun removeSongFromActiveRecommendations(songId: String) {
        val current = _recommendationsState.value
        val updatedSections = current.sections.map { section ->
            section.copy(songs = section.songs.filter { it.id != songId })
        }
        val updatedDiscover = current.discoverWeekly.filter { it.id != songId }
        val updatedSimilar = _similarSongsState.value.filter { it.id != songId }
        _recommendationsState.value = current.copy(
            sections = updatedSections,
            discoverWeekly = updatedDiscover
        )
        _similarSongsState.value = updatedSimilar
    }

    private fun removeArtistFromActiveRecommendations(artistName: String) {
        val lower = artistName.lowercase()
        val current = _recommendationsState.value
        val updatedSections = current.sections.map { section ->
            section.copy(songs = section.songs.filter { !it.artist.lowercase().contains(lower) })
        }
        val updatedDiscover = current.discoverWeekly.filter { !it.artist.lowercase().contains(lower) }
        val updatedSimilar = _similarSongsState.value.filter { !it.artist.lowercase().contains(lower) }
        _recommendationsState.value = current.copy(
            sections = updatedSections,
            discoverWeekly = updatedDiscover
        )
        _similarSongsState.value = updatedSimilar
    }

    /**
     * Finds similar songs when a user plays or selects a track.
     * Evaluates same artist, related artists, genre, and musical style.
     */
    suspend fun fetchSimilarSongs(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val primaryArtist = song.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: song.artist
        val results = mutableListOf<Song>()
        val seen = mutableSetOf(song.id)

        // 1. Query JioSaavn for tracks by primary artist
        try {
            val artistTracks = JioSaavnDirectEngine.searchSongs(primaryArtist, moodTag = song.mood, limit = 15)
            for (t in artistTracks) {
                if (filterSong(t) && seen.add(t.id)) {
                    results.add(t)
                }
            }
        } catch (_: Exception) {}

        // 2. Query tracks with same mood or title genre
        if (results.size < 8 && song.mood.isNotBlank() && song.mood != "Spring") {
            try {
                val moodTracks = JioSaavnDirectEngine.searchSongs("${song.mood} hits", moodTag = song.mood, limit = 10)
                for (t in moodTracks) {
                    if (filterSong(t) && seen.add(t.id)) {
                        results.add(t)
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: query Deezer or generic similar pop tracks
        if (results.size < 6) {
            try {
                val fallbackTracks = JioSaavnDirectEngine.searchSongs("${song.title} similar", limit = 10)
                for (t in fallbackTracks) {
                    if (filterSong(t) && seen.add(t.id)) {
                        results.add(t)
                    }
                }
            } catch (_: Exception) {}
        }

        val finalSimilar = results.take(12)
        _similarSongsState.value = finalSimilar
        finalSimilar
    }

    /**
     * Builds and updates all personalized recommendation carousels.
     */
    suspend fun refreshAllRecommendations(force: Boolean = false) = withContext(Dispatchers.IO) {
        _recommendationsState.value = _recommendationsState.value.copy(isLoading = true)

        try {
            loadBlacklists()

            val topArtists = recommendationDao.getTopArtists(5)
            val topMoods = recommendationDao.getTopMoods(3)
            val recentPlays = recommendationDao.getRecentPlays(20)
            val mostPlayed = recommendationDao.getMostPlayedSongs(15)

            // Gather candidate pools concurrently
            val sections = coroutineScope {
                val recForYouDeferred = async { buildRecommendedForYou(topArtists, topMoods) }
                val dailyMixDeferred = async { buildDailyMix(topArtists, recentPlays) }
                val basedOnHistoryDeferred = async { buildBasedOnHistory(recentPlays) }
                val discoverWeeklyDeferred = async { buildDiscoverWeekly(topArtists, topMoods, recentPlays) }
                val trendingDeferred = async { buildTrendingSongs() }
                val recentlyPopularDeferred = async { buildRecentlyPopular() }
                val hiddenGemsDeferred = async { buildHiddenGems() }
                val youMightLikeDeferred = async { buildSongsYouMightLike(topMoods) }

                val recForYou = recForYouDeferred.await()
                val dailyMix = dailyMixDeferred.await()
                val basedOnHistory = basedOnHistoryDeferred.await()
                val discoverWeekly = discoverWeeklyDeferred.await()
                val trending = trendingDeferred.await()
                val recentlyPopular = recentlyPopularDeferred.await()
                val hiddenGems = hiddenGemsDeferred.await()
                val youMightLike = youMightLikeDeferred.await()

                listOf(
                    RecommendationSection(
                        categoryKey = "recommended_for_you",
                        title = "Recommended For You",
                        subtitle = "Personalized picks tuned to your taste",
                        songs = recForYou
                    ),
                    RecommendationSection(
                        categoryKey = "daily_mix",
                        title = "Your Daily Mix",
                        subtitle = "A non-stop mix of your favorites & discoveries",
                        songs = dailyMix,
                        canRefresh = true
                    ),
                    RecommendationSection(
                        categoryKey = "based_on_history",
                        title = "Based On Your Listening History",
                        subtitle = if (recentPlays.isNotEmpty()) "Inspired by ${recentPlays.first().title}" else "Inspired by recent tracks",
                        songs = basedOnHistory
                    ),
                    RecommendationSection(
                        categoryKey = "discover_weekly",
                        title = "Discover Weekly",
                        subtitle = "Fresh tracks you haven't heard yet",
                        songs = discoverWeekly,
                        canRefresh = true
                    ),
                    RecommendationSection(
                        categoryKey = "trending",
                        title = "Trending Songs",
                        subtitle = "What listeners are playing right now",
                        songs = trending
                    ),
                    RecommendationSection(
                        categoryKey = "recently_popular",
                        title = "Recently Popular",
                        subtitle = "Top chartbusters and viral hits",
                        songs = recentlyPopular
                    ),
                    RecommendationSection(
                        categoryKey = "hidden_gems",
                        title = "Hidden Gems",
                        subtitle = "Deep acoustic, indie, and chill cuts",
                        songs = hiddenGems
                    ),
                    RecommendationSection(
                        categoryKey = "songs_you_might_like",
                        title = "Songs You Might Like",
                        subtitle = "Fresh styles and cross-genre favorites",
                        songs = youMightLike
                    )
                ).filter { it.songs.isNotEmpty() }
            }

            // Save to Room cache
            saveRecommendationsToCache(sections)

            val discoverSection = sections.find { it.categoryKey == "discover_weekly" }?.songs ?: emptyList()

            _recommendationsState.value = RecommendationSectionsState(
                isLoading = false,
                isPersonalized = _isPersonalizationEnabled.value,
                sections = sections,
                discoverWeekly = discoverSection,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            android.util.Log.e("RecommendationEngine", "Error generating recommendations: ${e.message}", e)
            loadCachedRecommendations()
            _recommendationsState.value = _recommendationsState.value.copy(
                isLoading = false,
                errorMessage = e.message
            )
        }
    }

    private suspend fun buildRecommendedForYou(
        topArtists: List<ArtistPlayCount>,
        topMoods: List<MoodPlayCount>
    ): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        // 1. Preferred Artists
        for (artist in topArtists.take(3)) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs(artist.artist, limit = 8)
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }

        // 2. Preferred Moods / Genres
        for (mood in topMoods.take(2)) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs("${mood.mood} hits", moodTag = mood.mood, limit = 6)
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback / Cold Start if user has little or no history
        if (results.size < 10) {
            try {
                val coldStart = JioSaavnDirectEngine.searchSongs("Top Trending Hits India", limit = 15)
                for (s in coldStart) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }

        return scoreAndRankCandidates(results, topArtists, topMoods).take(15)
    }

    private suspend fun buildDailyMix(
        topArtists: List<ArtistPlayCount>,
        recentPlays: List<SongPlayHistoryEntity>
    ): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        // Seed based on day of week for a fresh daily variety
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dailyQueries = when (dayOfWeek) {
            Calendar.MONDAY -> listOf("Monday Motivation Energy", "Bollywood Chartbusters")
            Calendar.TUESDAY -> listOf("Feel Good Indie India", "Trending Pop Hits")
            Calendar.WEDNESDAY -> listOf("Midweek Acoustic Chill", "Punjabi 101 Hits")
            Calendar.THURSDAY -> listOf("Throwback 2000s Bollywood", "Global Top Hits")
            Calendar.FRIDAY -> listOf("Friday Night Party Hits", "Desi Hip Hop Rap")
            Calendar.SATURDAY -> listOf("Weekend Dance Party Mix", "Top 50 Hits")
            else -> listOf("Sunday Morning Soulful Lofi", "Peaceful Acoustic Melodies")
        }

        for (q in dailyQueries) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs(q, limit = 10)
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }

        if (topArtists.isNotEmpty()) {
            val randomArtist = topArtists.shuffled().first().artist
            try {
                val artistSongs = JioSaavnDirectEngine.searchSongs(randomArtist, limit = 6)
                for (s in artistSongs) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }

        return results.shuffled().take(18)
    }

    private suspend fun buildBasedOnHistory(recentPlays: List<SongPlayHistoryEntity>): List<Song> {
        if (recentPlays.isEmpty()) return emptyList()
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        val distinctRecentArtists = recentPlays.map { it.artist }.distinct().take(3)
        for (artist in distinctRecentArtists) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs("$artist songs", limit = 6)
                for (s in songs) {
                    // Do not repeat recently played exact songs
                    if (recentPlays.none { it.songId == s.id } && filterSong(s) && seen.add(s.id)) {
                        results.add(s)
                    }
                }
            } catch (_: Exception) {}
        }

        return results.take(15)
    }

    private suspend fun buildDiscoverWeekly(
        topArtists: List<ArtistPlayCount>,
        topMoods: List<MoodPlayCount>,
        recentPlays: List<SongPlayHistoryEntity>
    ): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        val playedSongIds = recentPlays.map { it.songId }.toSet()

        val discoverQueries = mutableListOf(
            "Fresh Finds India Indie",
            "RADAR India Emerging Artists",
            "Bollywood Acoustic Undiscovered",
            "Soulful Lofi New Releases"
        )

        for (mood in topMoods.take(2)) {
            discoverQueries.add("${mood.mood} new discoveries")
        }

        for (q in discoverQueries) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs(q, limit = 8)
                for (s in songs) {
                    if (!playedSongIds.contains(s.id) && filterSong(s) && seen.add(s.id)) {
                        results.add(s)
                    }
                }
            } catch (_: Exception) {}
        }

        return results.shuffled().take(20)
    }

    private suspend fun buildTrendingSongs(): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        try {
            val trending = JioSaavnDirectEngine.searchSongs("Top Trending India", limit = 18)
            for (s in trending) {
                if (filterSong(s) && seen.add(s.id)) results.add(s)
            }
        } catch (_: Exception) {}
        return results.take(15)
    }

    private suspend fun buildRecentlyPopular(): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        try {
            val popular = JioSaavnDirectEngine.searchSongs("Hot Hits Hindi 2024", limit = 18)
            for (s in popular) {
                if (filterSong(s) && seen.add(s.id)) results.add(s)
            }
        } catch (_: Exception) {}
        return results.take(15)
    }

    private suspend fun buildHiddenGems(): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        val queries = listOf("Indie India Acoustic Soul", "Underground Indian Hip Hop", "Sufi Meditative Ghazals")
        for (q in queries) {
            try {
                val gems = JioSaavnDirectEngine.searchSongs(q, limit = 8)
                for (s in gems) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }
        return results.shuffled().take(15)
    }

    private suspend fun buildSongsYouMightLike(topMoods: List<MoodPlayCount>): List<Song> {
        val results = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        val crossGenreQueries = listOf("Global Viral Hits", "I-Pop Melodies", "Chill Lofi Beats Study")
        for (q in crossGenreQueries) {
            try {
                val songs = JioSaavnDirectEngine.searchSongs(q, limit = 8)
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) results.add(s)
                }
            } catch (_: Exception) {}
        }
        return results.shuffled().take(15)
    }

    /**
     * Scores candidates with hybrid factors: artist affinity, genre affinity, recency, and novelty.
     */
    private fun scoreAndRankCandidates(
        candidates: List<Song>,
        topArtists: List<ArtistPlayCount>,
        topMoods: List<MoodPlayCount>
    ): List<Song> {
        val artistWeightMap = topArtists.associate { it.artist.lowercase() to it.playCount.toFloat() }
        val moodWeightMap = topMoods.associate { it.mood.lowercase() to it.playCount.toFloat() }

        return candidates.sortedByDescending { song ->
            var score = 10f
            val lowerArtist = song.artist.lowercase()
            for ((favArtist, weight) in artistWeightMap) {
                if (lowerArtist.contains(favArtist)) {
                    score += 25f + (weight * 2f).coerceAtMost(30f)
                }
            }
            val lowerMood = song.mood.lowercase()
            for ((favMood, weight) in moodWeightMap) {
                if (lowerMood.contains(favMood)) {
                    score += 15f + (weight * 1.5f).coerceAtMost(20f)
                }
            }
            if (song.isFavorite) score += 15f
            score
        }
    }

    /**
     * Filters out disliked songs, blocked artists, and unplayable songs.
     */
    private fun filterSong(song: Song): Boolean {
        synchronized(this) {
            if (_dislikedSongIds.contains(song.id)) return false
            val lowerArtist = song.artist.lowercase()
            if (_blockedArtists.any { lowerArtist.contains(it) }) return false
        }
        // Ensure song has valid metadata
        if (song.title.isBlank() || song.artist.isBlank()) return false
        return true
    }

    /**
     * Loads previously cached recommendations from Room for instant launch & offline support.
     */
    private suspend fun loadCachedRecommendations() = withContext(Dispatchers.IO) {
        val keys = listOf(
            "recommended_for_you",
            "daily_mix",
            "based_on_history",
            "discover_weekly",
            "trending",
            "recently_popular",
            "hidden_gems",
            "songs_you_might_like"
        )
        val loadedSections = mutableListOf<RecommendationSection>()
        for (key in keys) {
            val cached = recommendationDao.getCachedRecommendations(key)
            if (cached != null) {
                val songs = parseSongsJson(cached.songsJson)
                if (songs.isNotEmpty()) {
                    val title = when (key) {
                        "recommended_for_you" -> "Recommended For You"
                        "daily_mix" -> "Your Daily Mix"
                        "based_on_history" -> "Based On Your Listening History"
                        "discover_weekly" -> "Discover Weekly"
                        "trending" -> "Trending Songs"
                        "recently_popular" -> "Recently Popular"
                        "hidden_gems" -> "Hidden Gems"
                        "songs_you_might_like" -> "Songs You Might Like"
                        else -> "Recommended"
                    }
                    val subtitle = when (key) {
                        "recommended_for_you" -> "Personalized picks tuned to your taste"
                        "daily_mix" -> "A non-stop mix of your favorites & discoveries"
                        "based_on_history" -> "Inspired by recent tracks"
                        "discover_weekly" -> "Fresh tracks you haven't heard yet"
                        "trending" -> "What listeners are playing right now"
                        "recently_popular" -> "Top chartbusters and viral hits"
                        "hidden_gems" -> "Deep acoustic, indie, and chill cuts"
                        "songs_you_might_like" -> "Fresh styles and cross-genre favorites"
                        else -> "Curated for you"
                    }
                    loadedSections.add(
                        RecommendationSection(
                            categoryKey = key,
                            title = title,
                            subtitle = subtitle,
                            songs = songs.filter { filterSong(it) },
                            canRefresh = key == "discover_weekly" || key == "daily_mix"
                        )
                    )
                }
            }
        }

        // Offline fallback: if no cached network recommendations exist, generate from local downloads & favorites
        if (loadedSections.isEmpty()) {
            val offlineFavorites = songDao.getFavorites()
            // We can build an offline section from local songs
        }

        if (loadedSections.isNotEmpty()) {
            val discoverSection = loadedSections.find { it.categoryKey == "discover_weekly" }?.songs ?: emptyList()
            _recommendationsState.value = RecommendationSectionsState(
                isLoading = false,
                isPersonalized = _isPersonalizationEnabled.value,
                sections = loadedSections,
                discoverWeekly = discoverSection,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private suspend fun saveRecommendationsToCache(sections: List<RecommendationSection>) = withContext(Dispatchers.IO) {
        for (section in sections) {
            val json = serializeSongsJson(section.songs)
            recommendationDao.saveCachedRecommendations(
                CachedRecommendationEntity(
                    categoryKey = section.categoryKey,
                    songsJson = json,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        recommendationDao.clearHistory()
        refreshAllRecommendations(force = true)
    }

    suspend fun resetPreferences() = withContext(Dispatchers.IO) {
        recommendationDao.clearTasteProfiles()
        recommendationDao.clearAllFeedback()
        recommendationDao.clearRecommendationCache()
        synchronized(this) {
            _dislikedSongIds.clear()
            _blockedArtists.clear()
        }
        refreshAllRecommendations(force = true)
    }

    private fun serializeSongsJson(songs: List<Song>): String {
        val array = JSONArray()
        for (s in songs) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("coverUrl", s.coverUrl)
                put("audioUrl", s.audioUrl)
                put("durationMs", s.durationMs)
                put("mood", s.mood)
                put("sourceEngine", s.sourceEngine)
                put("isFavorite", s.isFavorite)
                put("isDownloaded", s.isDownloaded)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSongsJson(jsonStr: String): List<Song> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Song>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        album = obj.optString("album", "Spring"),
                        coverUrl = obj.optString("coverUrl"),
                        audioUrl = obj.optString("audioUrl"),
                        durationMs = obj.optLong("durationMs", 180000L),
                        mood = obj.optString("mood", "Spring"),
                        sourceEngine = obj.optString("sourceEngine", "jiosaavn"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isDownloaded = obj.optBoolean("isDownloaded", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
