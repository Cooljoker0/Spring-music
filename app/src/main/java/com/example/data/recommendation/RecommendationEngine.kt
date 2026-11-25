package com.example.data.recommendation

import android.content.Context
import com.example.data.local.CachedRecommendationEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.RecommendationDao
import com.example.data.local.RecommendationFeedbackEntity
import com.example.data.local.SongDao
import com.example.data.local.SongPlayHistoryEntity
import com.example.data.local.UserTasteProfileEntity
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.JioSaavnDirectEngine
import com.example.data.network.PresetCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.exp
import kotlin.random.Random

enum class FeedbackType {
    LIKE,
    DISLIKE,
    NOT_INTERESTED,
    PLAY_MORE_LIKE_THIS,
    DONT_RECOMMEND_ARTIST
}

data class MadeForYouMix(
    val id: String,
    val mixNumber: Int? = null,
    val badgeLabel: String,
    val title: String,
    val subtitle: String,
    val accentColorHex: Long,
    val collageUrls: List<String>,
    val songs: List<Song>,
    val seedArtist: String = ""
) {
    fun toPlaylist(): Playlist = Playlist(
        id = "mix_$id",
        title = title,
        subtitle = "$badgeLabel • ${songs.size} tracks • $subtitle",
        coverUrl = collageUrls.firstOrNull().orEmpty(),
        songCount = songs.size,
        songs = songs
    )
}

data class RecommendationSection(
    val categoryKey: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val canRefresh: Boolean = false,
    val badgeText: String? = null,
    val seedImageUrl: String? = null,
    val sectionGroup: String = "discover" // "repeat", "personalized", "discover"
)

data class RecommendationSectionsState(
    val isLoading: Boolean = true,
    val isPersonalized: Boolean = true,
    val madeForYouMixes: List<MadeForYouMix> = emptyList(),
    val sections: List<RecommendationSection> = emptyList(),
    val discoverWeekly: List<Song> = emptyList(),
    val similarSongs: List<Song> = emptyList(),
    val lastUpdated: Long = 0L,
    val errorMessage: String? = null
)

private data class TasteContext(
    val isPersonalized: Boolean,
    val artistWeights: Map<String, Float>,
    val moodWeights: Map<String, Float>,
    val topArtistsRanked: List<String>,
    val topMoodsRanked: List<String>,
    val recentPlays: List<SongPlayHistoryEntity>,
    val mostPlayedSongs: List<Song>,
    val favoriteSongs: List<Song>,
    val playlistSongs: List<Song>,
    val seedSongs: List<Song>,
    val playedSongIds: Set<String>,
    val favoriteSongIds: Set<String>,
    val recentlySkippedIds: Set<String>,
    val artistDisplayNames: Map<String, String>
)

class RecommendationEngine(
    val recommendationDao: RecommendationDao,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val context: Context
) {
    companion object {
        private const val CACHE_TTL_MS = 3L * 60L * 60L * 1000L // 3 hours
        private const val CACHE_KEY_MIXES = "made_for_you_mixes"
        private const val CACHE_KEY_SECTIONS = "spotify_sections_v2"
        private const val PREF_PERSONALIZATION = "personalization_enabled"
    }

    private val prefs = context.getSharedPreferences("spring_prefs", Context.MODE_PRIVATE)

    private val _recommendationsState = MutableStateFlow(RecommendationSectionsState())
    val recommendationsState: StateFlow<RecommendationSectionsState> = _recommendationsState.asStateFlow()

    private val _similarSongsState = MutableStateFlow<List<Song>>(emptyList())
    val similarSongsState: StateFlow<List<Song>> = _similarSongsState.asStateFlow()

    private val _isPersonalizationEnabled = MutableStateFlow(
        prefs.getBoolean(PREF_PERSONALIZATION, true)
    )
    val isPersonalizationEnabled: StateFlow<Boolean> = _isPersonalizationEnabled.asStateFlow()

    private val _dislikedSongIds = mutableSetOf<String>()
    private val _blockedArtists = mutableSetOf<String>()

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debouncedRefreshJob: Job? = null

    fun scheduleDynamicRefresh() {
        if (!_isPersonalizationEnabled.value) return
        debouncedRefreshJob?.cancel()
        debouncedRefreshJob = engineScope.launch {
            delay(1800)
            refreshAllRecommendations(force = true)
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        loadBlacklists()
        val hasFreshCache = loadCachedRecommendations()
        if (!hasFreshCache) {
            refreshAllRecommendations(force = true)
        }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        _isPersonalizationEnabled.value = enabled
        prefs.edit().putBoolean(PREF_PERSONALIZATION, enabled).apply()
        engineScope.launch {
            refreshAllRecommendations(force = true)
        }
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

    private fun extractPrimaryArtist(artistField: String): String {
        return artistField
            .split(",", "&", "feat.", "ft.", " x ", "•")
            .firstOrNull()
            ?.trim()
            .orEmpty()
    }

    /**
     * Track a playback event and update artist & mood affinity weights.
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

        val primaryArtist = extractPrimaryArtist(song.artist)
        if (primaryArtist.isNotBlank() && !primaryArtist.equals("Spring Music", true)) {
            val artistKey = "artist:${primaryArtist.lowercase()}"
            val existing = recommendationDao.getTasteProfile(artistKey)
            val scoreDelta = when {
                isCompleted -> 3.5f
                isSkipped -> -2.5f
                playDurationMs > 30_000L -> 1.5f
                else -> 0.4f
            }
            val newScore = ((existing?.affinityScore ?: 0f) + scoreDelta).coerceIn(0f, 200f)
            val newPlayCount = (existing?.playCount ?: 0) + (if (!isSkipped) 1 else 0)
            val newSkipCount = (existing?.skipCount ?: 0) + (if (isSkipped) 1 else 0)
            recommendationDao.upsertTasteProfile(
                UserTasteProfileEntity(
                    key = artistKey,
                    type = "artist",
                    name = primaryArtist,
                    affinityScore = newScore,
                    playCount = newPlayCount,
                    skipCount = newSkipCount,
                    lastInteractedAt = System.currentTimeMillis()
                )
            )
        }

        val normalizedMood = song.mood.trim()
        if (normalizedMood.isNotBlank() &&
            !normalizedMood.equals("Spring", true) &&
            !normalizedMood.equals("Spring Vibe", true) &&
            !normalizedMood.equals("Unknown", true)
        ) {
            val moodKey = "mood:${normalizedMood.lowercase()}"
            val existing = recommendationDao.getTasteProfile(moodKey)
            val scoreDelta = when {
                isCompleted -> 2.2f
                isSkipped -> -1.2f
                else -> 0.6f
            }
            val newScore = ((existing?.affinityScore ?: 0f) + scoreDelta).coerceIn(0f, 200f)
            recommendationDao.upsertTasteProfile(
                UserTasteProfileEntity(
                    key = moodKey,
                    type = "mood",
                    name = normalizedMood,
                    affinityScore = newScore,
                    playCount = (existing?.playCount ?: 0) + (if (!isSkipped) 1 else 0),
                    skipCount = (existing?.skipCount ?: 0) + (if (isSkipped) 1 else 0),
                    lastInteractedAt = System.currentTimeMillis()
                )
            )
        }

        if (isCompleted || isSkipped || playDurationMs > 30_000L) {
            scheduleDynamicRefresh()
        }
    }

    /**
     * Record explicit feedback: Like, Dislike, Not Interested, Block Artist, or Play More Like This.
     */
    suspend fun recordFeedback(song: Song, feedbackType: FeedbackType) = withContext(Dispatchers.IO) {
        val primaryArtist = extractPrimaryArtist(song.artist).ifBlank { song.artist }

        when (feedbackType) {
            FeedbackType.LIKE -> {
                songDao.updateFavorite(song.id, true)
                boostArtistAffinity(primaryArtist, 5.5f)
            }
            FeedbackType.DISLIKE -> {
                recommendationDao.insertFeedback(
                    RecommendationFeedbackEntity(
                        id = "${song.id}_dislike",
                        targetId = song.id,
                        targetType = "song",
                        feedbackType = "dislike",
                        targetName = song.title
                    )
                )
                synchronized(this) { _dislikedSongIds.add(song.id) }
                penalizeArtistAffinity(primaryArtist, 4.0f)
                removeSongFromActiveRecommendations(song.id)
            }
            FeedbackType.NOT_INTERESTED -> {
                recommendationDao.insertFeedback(
                    RecommendationFeedbackEntity(
                        id = "${song.id}_not_interested",
                        targetId = song.id,
                        targetType = "song",
                        feedbackType = "not_interested",
                        targetName = song.title
                    )
                )
                synchronized(this) { _dislikedSongIds.add(song.id) }
                removeSongFromActiveRecommendations(song.id)
            }
            FeedbackType.DONT_RECOMMEND_ARTIST -> {
                recommendationDao.insertFeedback(
                    RecommendationFeedbackEntity(
                        id = "${primaryArtist.lowercase()}_block",
                        targetId = primaryArtist,
                        targetType = "artist",
                        feedbackType = "block_artist",
                        targetName = primaryArtist
                    )
                )
                synchronized(this) { _blockedArtists.add(primaryArtist.lowercase()) }
                removeArtistFromActiveRecommendations(primaryArtist)
            }
            FeedbackType.PLAY_MORE_LIKE_THIS -> {
                boostArtistAffinity(primaryArtist, 4.5f)
                fetchSimilarSongs(song)
            }
        }
        scheduleDynamicRefresh()
    }

    private suspend fun boostArtistAffinity(artistName: String, boost: Float) {
        if (artistName.isBlank()) return
        val key = "artist:${artistName.lowercase()}"
        val existing = recommendationDao.getTasteProfile(key)
        recommendationDao.upsertTasteProfile(
            UserTasteProfileEntity(
                key = key,
                type = "artist",
                name = artistName,
                affinityScore = ((existing?.affinityScore ?: 0f) + boost).coerceIn(0f, 200f),
                playCount = (existing?.playCount ?: 0) + 1,
                skipCount = existing?.skipCount ?: 0,
                lastInteractedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun penalizeArtistAffinity(artistName: String, penalty: Float) {
        if (artistName.isBlank()) return
        val key = "artist:${artistName.lowercase()}"
        val existing = recommendationDao.getTasteProfile(key) ?: return
        recommendationDao.upsertTasteProfile(
            existing.copy(
                affinityScore = (existing.affinityScore - penalty).coerceAtLeast(0f),
                lastInteractedAt = System.currentTimeMillis()
            )
        )
    }

    private fun removeSongFromActiveRecommendations(songId: String) {
        val current = _recommendationsState.value
        val updatedSections = current.sections.map { section ->
            section.copy(songs = section.songs.filter { it.id != songId })
        }.filter { it.songs.isNotEmpty() }
        val updatedMixes = current.madeForYouMixes.map { mix ->
            mix.copy(songs = mix.songs.filter { it.id != songId })
        }.filter { it.songs.isNotEmpty() }
        val updatedDiscover = current.discoverWeekly.filter { it.id != songId }
        val updatedSimilar = _similarSongsState.value.filter { it.id != songId }
        _recommendationsState.value = current.copy(
            madeForYouMixes = updatedMixes,
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
        }.filter { it.songs.isNotEmpty() }
        val updatedMixes = current.madeForYouMixes.map { mix ->
            mix.copy(songs = mix.songs.filter { !it.artist.lowercase().contains(lower) })
        }.filter { it.songs.isNotEmpty() }
        val updatedDiscover = current.discoverWeekly.filter { !it.artist.lowercase().contains(lower) }
        val updatedSimilar = _similarSongsState.value.filter { !it.artist.lowercase().contains(lower) }
        _recommendationsState.value = current.copy(
            madeForYouMixes = updatedMixes,
            sections = updatedSections,
            discoverWeekly = updatedDiscover
        )
        _similarSongsState.value = updatedSimilar
    }

    /**
     * Builds Spotify-style Song Radio: seed track + acoustically/artist-similar recommendations
     * re-ranked with user's taste vector and artist separation.
     */
    suspend fun buildTrackRadio(seedSong: Song, limit: Int = 24): List<Song> = withContext(Dispatchers.IO) {
        loadBlacklists()
        val taste = buildTasteContext()
        val rawCandidates = JioSaavnDirectEngine.getSongRecommendations(
            songId = seedSong.id,
            title = seedSong.title,
            artist = seedSong.artist,
            moodTag = seedSong.mood,
            limit = 32
        )
        val filtered = rawCandidates
            .filter { it.id != seedSong.id && filterSong(it) }
            .distinctBy { it.id }
        val ranked = rankAndInterleave(
            candidates = filtered,
            taste = taste,
            preferFamiliar = false,
            seedArtistOverride = extractPrimaryArtist(seedSong.artist)
        ).take((limit - 1).coerceAtLeast(1))

        val radioList = listOf(seedSong) + ranked
        _similarSongsState.value = ranked.take(12)
        radioList
    }

    /**
     * Generates fresh Autoplay continuation tracks when the active queue reaches the end.
     */
    suspend fun getAutoplayContinuation(
        seedSong: Song,
        excludeIds: Set<String>,
        limit: Int = 10
    ): List<Song> = withContext(Dispatchers.IO) {
        loadBlacklists()
        val taste = buildTasteContext()
        val pool = mutableListOf<Song>()
        val seen = excludeIds.toMutableSet().apply { add(seedSong.id) }

        val directRecs = JioSaavnDirectEngine.getSongRecommendations(
            songId = seedSong.id,
            title = seedSong.title,
            artist = seedSong.artist,
            moodTag = seedSong.mood,
            limit = 22
        )
        for (s in directRecs) {
            if (filterSong(s) && seen.add(s.id)) pool.add(s)
        }

        if (pool.size < limit && taste.topArtistsRanked.isNotEmpty()) {
            val topArtist = taste.topArtistsRanked.first()
            val extra = runCatching {
                JioSaavnDirectEngine.searchSongs(topArtist, limit = 10)
            }.getOrDefault(emptyList())
            for (s in extra) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }

        rankAndInterleave(
            candidates = pool,
            taste = taste,
            preferFamiliar = false,
            seedArtistOverride = extractPrimaryArtist(seedSong.artist)
        ).take(limit)
    }

    /**
     * Finds similar songs for the player sheet ("You May Also Like") using JioSaavn reco.getreco + taste scoring.
     */
    suspend fun fetchSimilarSongs(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val taste = buildTasteContext()
        val candidates = JioSaavnDirectEngine.getSongRecommendations(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            moodTag = song.mood,
            limit = 18
        ).filter { it.id != song.id && filterSong(it) }

        val ranked = rankAndInterleave(
            candidates = candidates,
            taste = taste,
            preferFamiliar = false,
            seedArtistOverride = extractPrimaryArtist(song.artist)
        ).take(12)

        _similarSongsState.value = ranked
        ranked
    }

    /**
     * Builds all Spotify-style Made For You Daily Mixes and home recommendation carousels.
     */
    suspend fun refreshAllRecommendations(force: Boolean = false) = withContext(Dispatchers.IO) {
        val currentState = _recommendationsState.value
        if (!force &&
            currentState.sections.isNotEmpty() &&
            currentState.madeForYouMixes.isNotEmpty() &&
            currentState.isPersonalized == _isPersonalizationEnabled.value &&
            (System.currentTimeMillis() - currentState.lastUpdated) < CACHE_TTL_MS
        ) {
            return@withContext
        }

        _recommendationsState.value = currentState.copy(isLoading = true, errorMessage = null)

        try {
            loadBlacklists()
            val taste = buildTasteContext()

            val (mixes, sections) = coroutineScope {
                val mixesDeferred = async { buildMadeForYouMixes(taste) }
                val onRepeatDeferred = async { buildOnRepeatSection(taste) }
                val becauseArtistDeferred = async { buildBecauseYouListenToSection(taste) }
                val recForYouDeferred = async { buildRecommendedForYou(taste) }
                val becauseLikedDeferred = async { buildBecauseYouLikedSection(taste) }
                val discoverWeeklyDeferred = async { buildDiscoverWeeklySection(taste) }
                val trendingDeferred = async { buildTrendingSection(taste) }
                val hiddenGemsDeferred = async { buildHiddenGemsSection(taste) }

                val builtMixes = mixesDeferred.await()
                val builtSections = listOfNotNull(
                    onRepeatDeferred.await(),
                    becauseArtistDeferred.await(),
                    recForYouDeferred.await(),
                    becauseLikedDeferred.await(),
                    discoverWeeklyDeferred.await(),
                    trendingDeferred.await(),
                    hiddenGemsDeferred.await()
                ).filter { it.songs.isNotEmpty() }

                builtMixes to builtSections
            }

            if (mixes.isNotEmpty() || sections.isNotEmpty()) {
                saveRecommendationsToCache(mixes, sections)
            }

            val discoverSongs = mixes.find { it.id == "discover_weekly" }?.songs
                ?: sections.find { it.categoryKey == "discover_weekly" }?.songs
                ?: emptyList()

            _recommendationsState.value = RecommendationSectionsState(
                isLoading = false,
                isPersonalized = _isPersonalizationEnabled.value,
                madeForYouMixes = mixes,
                sections = sections,
                discoverWeekly = discoverSongs,
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

    /**
     * Aggregates all local taste signals (Taste Profile + Time-Decayed History + Liked Songs + Playlists)
     * into a unified TasteContext.
     */
    private suspend fun buildTasteContext(): TasteContext {
        if (!_isPersonalizationEnabled.value) {
            return TasteContext(
                isPersonalized = false,
                artistWeights = emptyMap(),
                moodWeights = emptyMap(),
                topArtistsRanked = emptyList(),
                topMoodsRanked = emptyList(),
                recentPlays = emptyList(),
                mostPlayedSongs = emptyList(),
                favoriteSongs = emptyList(),
                playlistSongs = emptyList(),
                seedSongs = emptyList(),
                playedSongIds = emptySet(),
                favoriteSongIds = emptySet(),
                recentlySkippedIds = emptySet(),
                artistDisplayNames = emptyMap()
            )
        }

        val artistProfiles = recommendationDao.getTopAffinities("artist", 20)
        val moodProfiles = recommendationDao.getTopAffinities("mood", 10)
        val recentPlays = recommendationDao.getRecentPlays(40)
        val mostPlayedEntities = recommendationDao.getMostPlayedSongs(20)
        val favoriteEntities = runCatching { songDao.getFavorites().first() }.getOrDefault(emptyList())
        val playlistEntities = runCatching { playlistDao.getAllPlaylistsSync() }.getOrDefault(emptyList())

        val favoriteSongs = favoriteEntities.map { it.toSong() }.filter { filterSong(it) }
        val playlistSongs = playlistEntities.flatMap { parseSongsJson(it.songsJson) }.filter { filterSong(it) }
        val mostPlayedSongs = mostPlayedEntities.map {
            Song(
                id = it.songId,
                title = it.title,
                artist = it.artist,
                album = it.album,
                coverUrl = it.coverUrl,
                audioUrl = it.audioUrl,
                durationMs = it.durationMs,
                mood = it.mood,
                isFavorite = favoriteSongs.any { fav -> fav.id == it.songId }
            )
        }.filter { filterSong(it) }

        val artistWeights = mutableMapOf<String, Float>()
        val moodWeights = mutableMapOf<String, Float>()
        val artistDisplayNames = mutableMapOf<String, String>()
        val now = System.currentTimeMillis()

        fun addArtistWeight(rawArtist: String, delta: Float) {
            val primary = extractPrimaryArtist(rawArtist)
            if (primary.isBlank() || primary.equals("Spring Music", true) || primary.equals("Various Artists", true)) return
            val key = primary.lowercase()
            artistDisplayNames.putIfAbsent(key, primary)
            artistWeights[key] = ((artistWeights[key] ?: 0f) + delta).coerceAtLeast(0f)
        }

        fun addMoodWeight(rawMood: String, delta: Float) {
            val clean = rawMood.trim()
            if (clean.isBlank() || clean.equals("Spring", true) || clean.equals("Spring Vibe", true)) return
            val key = clean.lowercase()
            moodWeights[key] = ((moodWeights[key] ?: 0f) + delta).coerceAtLeast(0f)
        }

        // 1. Persisted UserTasteProfileEntity scores with subtle recency boost
        for (p in artistProfiles) {
            val ageHours = ((now - p.lastInteractedAt).coerceAtLeast(0L) / 3_600_000.0).toFloat()
            val recencyMultiplier = (0.7f + 0.5f * exp((-ageHours / 168f).toDouble()).toFloat())
            addArtistWeight(p.name, p.affinityScore * recencyMultiplier)
        }
        for (m in moodProfiles) {
            addMoodWeight(m.name, m.affinityScore)
        }

        // 2. Time-decayed recent listening history
        val recentlySkipped = mutableSetOf<String>()
        val cutoff48h = now - 48L * 3600L * 1000L
        for (play in recentPlays) {
            val ageDays = ((now - play.timestamp).coerceAtLeast(0L) / 86_400_000.0).toFloat()
            val decay = exp((-ageDays / 10f).toDouble()).toFloat()
            if (play.isSkipped) {
                if (play.timestamp >= cutoff48h) recentlySkipped.add(play.songId)
                addArtistWeight(play.artist, -1.5f * decay)
            } else {
                val base = if (play.isCompleted) 2.5f else 1.2f
                addArtistWeight(play.artist, base * decay)
                addMoodWeight(play.mood, 1.2f * decay)
            }
        }

        // 3. Liked songs are strong positive Spotify taste signals
        for (fav in favoriteSongs) {
            addArtistWeight(fav.artist, 4.5f)
            addMoodWeight(fav.mood, 2.0f)
        }

        // 4. Imported / custom playlists contribute secondary taste clusters
        for (plSong in playlistSongs.take(80)) {
            addArtistWeight(plSong.artist, 1.8f)
            addMoodWeight(plSong.mood, 0.8f)
        }

        val topArtistsRanked = artistWeights.entries
            .filter { it.value > 0.5f }
            .sortedByDescending { it.value }
            .mapNotNull { artistDisplayNames[it.key] }

        val topMoodsRanked = moodWeights.entries
            .filter { it.value > 0.5f }
            .sortedByDescending { it.value }
            .map { entry -> entry.key.replaceFirstChar { it.uppercase() } }

        val seedSongs = (favoriteSongs + mostPlayedSongs + playlistSongs.take(12))
            .distinctBy { it.id }

        val playedIds = recentPlays.map { it.songId }.toSet() + mostPlayedSongs.map { it.id }.toSet()
        val favIds = favoriteSongs.map { it.id }.toSet()

        return TasteContext(
            isPersonalized = true,
            artistWeights = artistWeights,
            moodWeights = moodWeights,
            topArtistsRanked = topArtistsRanked,
            topMoodsRanked = topMoodsRanked,
            recentPlays = recentPlays,
            mostPlayedSongs = mostPlayedSongs,
            favoriteSongs = favoriteSongs,
            playlistSongs = playlistSongs,
            seedSongs = seedSongs,
            playedSongIds = playedIds,
            favoriteSongIds = favIds,
            recentlySkippedIds = recentlySkipped,
            artistDisplayNames = artistDisplayNames
        )
    }

    /**
     * Generates Spotify's signature "Made For You" horizontal shelf:
     * Daily Mix 1, Daily Mix 2, Daily Mix 3, Discover Weekly, and On Repeat.
     */
    private suspend fun buildMadeForYouMixes(taste: TasteContext): List<MadeForYouMix> = coroutineScope {
        val mixes = mutableListOf<MadeForYouMix>()
        val usedAcrossMixes = mutableSetOf<String>()

        val mixColors = listOf(
            0xFFF4F5F7, // Soft White
            0xFFAEB4C0, // Liquid Silver
            0xFF858B98, // Muted Pewter
            0xFFC4CAD4, // Frost Silver
            0xFFD8DCE4  // Pearl Mist
        )

        // Helper to gather a clustered Daily Mix from 1-2 anchor artists + seed recommendations
        suspend fun createDailyMix(
            mixNum: Int,
            anchorArtists: List<String>,
            fallbackQueries: List<String>,
            moodHint: String?,
            accentHex: Long
        ): MadeForYouMix? {
            val pool = mutableListOf<Song>()
            val seen = mutableSetOf<String>()

            // Include a few familiar favorites matching these artists first
            for (fav in taste.seedSongs) {
                val matchesAnchor = anchorArtists.any { a -> fav.artist.contains(a, ignoreCase = true) }
                if (matchesAnchor && filterSong(fav) && seen.add(fav.id)) {
                    pool.add(fav)
                }
            }

            // Pull track recommendations from an anchor seed song if available
            val anchorSeed = taste.seedSongs.firstOrNull { s ->
                anchorArtists.any { a -> s.artist.contains(a, ignoreCase = true) }
            }
            if (anchorSeed != null) {
                val recs = JioSaavnDirectEngine.getSongRecommendations(
                    songId = anchorSeed.id,
                    title = anchorSeed.title,
                    artist = anchorSeed.artist,
                    moodTag = moodHint ?: anchorSeed.mood,
                    limit = 16
                )
                for (s in recs) {
                    if (filterSong(s) && seen.add(s.id) && !usedAcrossMixes.contains(s.id)) {
                        pool.add(s)
                    }
                }
            }

            // Query each anchor artist
            for (artist in anchorArtists) {
                val songs = runCatching {
                    JioSaavnDirectEngine.searchSongs(artist, moodTag = moodHint ?: "Daily Mix $mixNum", limit = 10)
                }.getOrDefault(emptyList())
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) pool.add(s)
                }
            }

            // Fallback queries when cold start or sparse history
            if (pool.size < 14) {
                for (q in fallbackQueries) {
                    val songs = runCatching {
                        JioSaavnDirectEngine.searchSongs(q, moodTag = moodHint ?: "Daily Mix $mixNum", limit = 10)
                    }.getOrDefault(emptyList())
                    for (s in songs) {
                        if (filterSong(s) && seen.add(s.id)) pool.add(s)
                    }
                }
            }

            if (pool.isEmpty()) return null
            val ranked = rankAndInterleave(
                candidates = pool,
                taste = taste,
                preferFamiliar = true,
                seedArtistOverride = anchorArtists.firstOrNull()
            ).take(20)

            ranked.take(8).forEach { usedAcrossMixes.add(it.id) }

            val displayArtists = ranked
                .map { extractPrimaryArtist(it.artist) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
            val subtitle = if (displayArtists.isNotEmpty()) {
                displayArtists.joinToString(", ") + " and more"
            } else {
                "Personalized blend of favorites & fresh picks"
            }

            val collage = ranked.mapNotNull { it.coverUrl.takeIf { url -> url.isNotBlank() } }
                .distinct()
                .take(4)

            return MadeForYouMix(
                id = "daily_mix_$mixNum",
                mixNumber = mixNum,
                badgeLabel = "DAILY MIX $mixNum",
                title = "Daily Mix $mixNum",
                subtitle = subtitle,
                accentColorHex = accentHex,
                collageUrls = collage,
                songs = ranked,
                seedArtist = anchorArtists.firstOrNull().orEmpty()
            )
        }

        val artists = taste.topArtistsRanked
        val moods = taste.topMoodsRanked

        val daySeed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val mix1Deferred = async {
            createDailyMix(
                mixNum = 1,
                anchorArtists = artists.sliceSafe(0, 2),
                fallbackQueries = listOf("Today's Top Hits", "Global Viral Pop"),
                moodHint = moods.getOrNull(0),
                accentHex = mixColors[0]
            )
        }
        val mix2Deferred = async {
            createDailyMix(
                mixNum = 2,
                anchorArtists = artists.sliceSafe(2, 4),
                fallbackQueries = if (daySeed % 2 == 0) listOf("Indie Acoustic Chill", "Prateek Kuhad") else listOf("Lo-Fi Chill Beats", "Acoustic Soul"),
                moodHint = moods.getOrNull(1),
                accentHex = mixColors[1]
            )
        }
        val mix3Deferred = async {
            createDailyMix(
                mixNum = 3,
                anchorArtists = artists.sliceSafe(4, 6),
                fallbackQueries = listOf("Hip Hop Workout Energy", "R&B Midnight Grooves"),
                moodHint = moods.getOrNull(2) ?: "Energy",
                accentHex = mixColors[2]
            )
        }
        val discoverMixDeferred = async {
            val discoverSongs = buildDiscoverWeeklyPool(taste)
            if (discoverSongs.isNotEmpty()) {
                val artistsSummary = discoverSongs.map { extractPrimaryArtist(it.artist) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
                    .joinToString(", ")
                MadeForYouMix(
                    id = "discover_weekly",
                    mixNumber = null,
                    badgeLabel = "DISCOVER WEEKLY",
                    title = "Discover Weekly",
                    subtitle = if (artistsSummary.isNotBlank()) "Fresh finds: $artistsSummary and more" else "Your weekly mixtape of fresh music",
                    accentColorHex = mixColors[3],
                    collageUrls = discoverSongs.mapNotNull { it.coverUrl.takeIf { u -> u.isNotBlank() } }.distinct().take(4),
                    songs = discoverSongs
                )
            } else null
        }

        listOfNotNull(
            mix1Deferred.await(),
            mix2Deferred.await(),
            mix3Deferred.await(),
            discoverMixDeferred.await()
        ).forEach { mixes.add(it) }

        // On Repeat Mix when user has real play/favorite history
        val onRepeatPool = (taste.mostPlayedSongs + taste.favoriteSongs)
            .distinctBy { it.id }
            .filter { filterSong(it) }
            .take(20)
        if (onRepeatPool.size >= 3) {
            val onRepeatArtists = onRepeatPool.map { extractPrimaryArtist(it.artist) }.distinct().take(3).joinToString(", ")
            mixes.add(
                1,
                MadeForYouMix(
                    id = "on_repeat",
                    mixNumber = null,
                    badgeLabel = "ON REPEAT",
                    title = "On Repeat",
                    subtitle = if (onRepeatArtists.isNotBlank()) "Your most played: $onRepeatArtists" else "Songs you keep coming back to",
                    accentColorHex = mixColors[4],
                    collageUrls = onRepeatPool.mapNotNull { it.coverUrl.takeIf { u -> u.isNotBlank() } }.distinct().take(4),
                    songs = onRepeatPool
                )
            )
        }

        mixes
    }

    private fun buildOnRepeatSection(taste: TasteContext): RecommendationSection? {
        if (!taste.isPersonalized) return null
        val songs = (taste.mostPlayedSongs + taste.favoriteSongs)
            .distinctBy { it.id }
            .filter { filterSong(it) }
            .take(15)
        if (songs.size < 3) return null
        return RecommendationSection(
            categoryKey = "on_repeat",
            title = "On Repeat",
            subtitle = "Tracks you can't stop playing",
            songs = songs,
            badgeText = "YOUR ROTATION",
            sectionGroup = "repeat"
        )
    }

    private suspend fun buildBecauseYouListenToSection(taste: TasteContext): RecommendationSection? {
        if (!taste.isPersonalized || taste.topArtistsRanked.isEmpty()) return null
        val topArtist = taste.topArtistsRanked.first()
        val seedSong = taste.seedSongs.firstOrNull { it.artist.contains(topArtist, ignoreCase = true) }

        val pool = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        if (seedSong != null) {
            val recs = JioSaavnDirectEngine.getSongRecommendations(
                songId = seedSong.id,
                title = seedSong.title,
                artist = seedSong.artist,
                moodTag = seedSong.mood,
                limit = 16
            )
            for (s in recs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }

        val artistHits = runCatching {
            JioSaavnDirectEngine.searchSongs("$topArtist radio", limit = 12)
        }.getOrDefault(emptyList())
        for (s in artistHits) {
            if (filterSong(s) && seen.add(s.id)) pool.add(s)
        }

        if (pool.isEmpty()) return null
        val ranked = rankAndInterleave(
            candidates = pool,
            taste = taste,
            preferFamiliar = false,
            seedArtistOverride = topArtist
        ).take(15)

        return RecommendationSection(
            categoryKey = "because_you_listen",
            title = "Because you listen to $topArtist",
            subtitle = "Similar artists & sonic matches",
            songs = ranked,
            badgeText = "SEED ARTIST",
            seedImageUrl = seedSong?.coverUrl?.takeIf { it.isNotBlank() }
                ?: ranked.firstOrNull { it.artist.contains(topArtist, true) }?.coverUrl,
            sectionGroup = "personalized"
        )
    }

    private suspend fun buildBecauseYouLikedSection(taste: TasteContext): RecommendationSection? {
        if (!taste.isPersonalized) return null
        val seed = taste.favoriteSongs.firstOrNull()
            ?: taste.playlistSongs.firstOrNull()
            ?: taste.recentPlays.firstOrNull()?.let {
                Song(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    coverUrl = it.coverUrl,
                    audioUrl = it.audioUrl,
                    durationMs = it.durationMs,
                    mood = it.mood
                )
            } ?: return null

        val recs = JioSaavnDirectEngine.getSongRecommendations(
            songId = seed.id,
            title = seed.title,
            artist = seed.artist,
            moodTag = seed.mood,
            limit = 18
        ).filter { it.id != seed.id && filterSong(it) }

        if (recs.isEmpty()) return null
        val ranked = rankAndInterleave(
            candidates = recs,
            taste = taste,
            preferFamiliar = false,
            seedArtistOverride = extractPrimaryArtist(seed.artist)
        ).take(15)

        return RecommendationSection(
            categoryKey = "because_you_liked",
            title = "More like ${seed.title}",
            subtitle = "Inspired by ${extractPrimaryArtist(seed.artist).ifBlank { "your library" }}",
            songs = ranked,
            badgeText = "SONG RADIO",
            seedImageUrl = seed.coverUrl.takeIf { it.isNotBlank() },
            sectionGroup = "personalized"
        )
    }

    private suspend fun buildRecommendedForYou(taste: TasteContext): RecommendationSection {
        val pool = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        // Multi-seed track-to-track discovery from user's top seeds
        for (seed in taste.seedSongs.take(3)) {
            val recs = runCatching {
                JioSaavnDirectEngine.getSongRecommendations(
                    songId = seed.id,
                    title = seed.title,
                    artist = seed.artist,
                    moodTag = seed.mood,
                    limit = 10
                )
            }.getOrDefault(emptyList())
            for (s in recs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }

        // Top artists & moods
        for (artist in taste.topArtistsRanked.take(3)) {
            val songs = runCatching { JioSaavnDirectEngine.searchSongs(artist, limit = 8) }.getOrDefault(emptyList())
            for (s in songs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }
        for (mood in taste.topMoodsRanked.take(2)) {
            val songs = runCatching { JioSaavnDirectEngine.searchSongs("$mood hits", moodTag = mood, limit = 8) }.getOrDefault(emptyList())
            for (s in songs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }

        // Balanced global + regional cold-start if pool is small
        if (pool.size < 12) {
            for (q in listOf("Today's Top Hits", "Global Top 50", "Top Trending Hits")) {
                val songs = runCatching { JioSaavnDirectEngine.searchSongs(q, limit = 10) }.getOrDefault(emptyList())
                for (s in songs) {
                    if (filterSong(s) && seen.add(s.id)) pool.add(s)
                }
            }
        }

        val ranked = rankAndInterleave(
            candidates = pool,
            taste = taste,
            preferFamiliar = true
        ).take(18)

        return RecommendationSection(
            categoryKey = "recommended_for_you",
            title = if (taste.isPersonalized && taste.seedSongs.isNotEmpty()) "Recommended for you" else "Picked for you",
            subtitle = "Hybrid blend tuned to your sonic fingerprint",
            songs = ranked,
            canRefresh = true,
            badgeText = "FOR YOU",
            sectionGroup = "personalized"
        )
    }

    private suspend fun buildDiscoverWeeklyPool(taste: TasteContext): List<Song> {
        val pool = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        // Seed recommendations excluding already-played & liked tracks
        for (seed in taste.seedSongs.shuffled().take(2)) {
            val recs = runCatching {
                JioSaavnDirectEngine.getSongRecommendations(
                    songId = seed.id,
                    title = seed.title,
                    artist = seed.artist,
                    moodTag = seed.mood,
                    limit = 12
                )
            }.getOrDefault(emptyList())
            for (s in recs) {
                if (!taste.playedSongIds.contains(s.id) &&
                    !taste.favoriteSongIds.contains(s.id) &&
                    filterSong(s) &&
                    seen.add(s.id)
                ) {
                    pool.add(s)
                }
            }
        }

        val discoverQueries = mutableListOf(
            "Fresh Finds Indie",
            "New Music Friday Global",
            "Emerging Artists Chill"
        )
        taste.topMoodsRanked.take(2).forEach { mood ->
            discoverQueries.add("$mood new releases")
        }

        for (q in discoverQueries) {
            val songs = runCatching { JioSaavnDirectEngine.searchSongs(q, limit = 8) }.getOrDefault(emptyList())
            for (s in songs) {
                if (!taste.playedSongIds.contains(s.id) &&
                    !taste.favoriteSongIds.contains(s.id) &&
                    filterSong(s) &&
                    seen.add(s.id)
                ) {
                    pool.add(s)
                }
            }
        }

        return rankAndInterleave(
            candidates = pool,
            taste = taste,
            preferFamiliar = false
        ).take(20)
    }

    private suspend fun buildDiscoverWeeklySection(taste: TasteContext): RecommendationSection {
        val songs = buildDiscoverWeeklyPool(taste)
        return RecommendationSection(
            categoryKey = "discover_weekly",
            title = "Discover Weekly",
            subtitle = "Fresh unplayed tracks curated for your taste",
            songs = songs,
            canRefresh = true,
            badgeText = "WEEKLY MIXTAPE",
            sectionGroup = "discover"
        )
    }

    private suspend fun buildTrendingSection(taste: TasteContext): RecommendationSection {
        val pool = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        for (q in listOf("Viral Hits Worldwide", "Top Trending Charts")) {
            val songs = runCatching { JioSaavnDirectEngine.searchSongs(q, limit = 12) }.getOrDefault(emptyList())
            for (s in songs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }
        val ranked = rankAndInterleave(pool, taste, preferFamiliar = false).take(16)
        return RecommendationSection(
            categoryKey = "trending",
            title = "Trending Now",
            subtitle = "Charts and viral hits climbing right now",
            songs = ranked,
            sectionGroup = "discover"
        )
    }

    private suspend fun buildHiddenGemsSection(taste: TasteContext): RecommendationSection {
        val pool = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        val queries = listOf("Late Night Acoustic Soul", "Indie Dream Pop Chill", "Midnight Lofi Grooves")
        for (q in queries) {
            val songs = runCatching { JioSaavnDirectEngine.searchSongs(q, limit = 7) }.getOrDefault(emptyList())
            for (s in songs) {
                if (filterSong(s) && seen.add(s.id)) pool.add(s)
            }
        }
        val ranked = rankAndInterleave(pool, taste, preferFamiliar = false).take(15)
        return RecommendationSection(
            categoryKey = "hidden_gems",
            title = "Hidden Gems",
            subtitle = "Deep indie, acoustic & midnight cuts",
            songs = ranked,
            sectionGroup = "discover"
        )
    }

    /**
     * Spotify-style hybrid scoring + artist-separation interleaving:
     * Combines artist affinity, mood match, favorite/skip signals, and enforces
     * spacing so consecutive tracks are never dominated by the same artist.
     */
    private fun rankAndInterleave(
        candidates: List<Song>,
        taste: TasteContext,
        preferFamiliar: Boolean,
        seedArtistOverride: String? = null
    ): List<Song> {
        if (candidates.isEmpty()) return emptyList()

        val scored = candidates.map { song ->
            var score = 10f
            val primaryArtist = extractPrimaryArtist(song.artist).lowercase()
            val fullArtistLower = song.artist.lowercase()

            // Artist affinity match
            for ((favArtist, weight) in taste.artistWeights) {
                if (fullArtistLower.contains(favArtist)) {
                    score += 18f + (weight * 1.4f).coerceAtMost(35f)
                }
            }

            // Seed artist similarity bump (gentle, not overwhelming)
            if (!seedArtistOverride.isNullOrBlank() &&
                fullArtistLower.contains(seedArtistOverride.lowercase())
            ) {
                score += 14f
            }

            // Mood match
            val moodLower = song.mood.lowercase()
            for ((favMood, weight) in taste.moodWeights) {
                if (moodLower.contains(favMood)) {
                    score += 10f + (weight * 1.1f).coerceAtMost(18f)
                }
            }

            // Familiar vs novelty bias
            val isFav = song.isFavorite || taste.favoriteSongIds.contains(song.id)
            val isPlayed = taste.playedSongIds.contains(song.id)
            if (preferFamiliar) {
                if (isFav) score += 16f
                else if (isPlayed) score += 6f
            } else {
                if (!isPlayed && !isFav) score += 10f
            }

            // Skip suppression
            if (taste.recentlySkippedIds.contains(song.id)) {
                score -= 35f
            }

            // Subtle freshness tie-breaker
            score += Random.nextFloat() * 5.5f

            Triple(song, primaryArtist, score)
        }.sortedByDescending { it.third }

        // Greedy Spotify artist-separation pass (avoid repeating the same artist within 2 slots)
        val pool = scored.toMutableList()
        val result = mutableListOf<Song>()
        val cooldown = ArrayDeque<String>()

        while (pool.isNotEmpty()) {
            var chosenIdx = pool.indexOfFirst { (_, artistKey, _) ->
                artistKey.isBlank() || !cooldown.contains(artistKey)
            }
            if (chosenIdx < 0) chosenIdx = 0

            val chosen = pool.removeAt(chosenIdx)
            result.add(chosen.first)
            if (chosen.second.isNotBlank()) {
                cooldown.addLast(chosen.second)
                if (cooldown.size > 2) {
                    cooldown.removeFirst()
                }
            }
        }

        return result
    }

    private fun filterSong(song: Song): Boolean {
        synchronized(this) {
            if (_dislikedSongIds.contains(song.id)) return false
            val lowerArtist = song.artist.lowercase()
            if (_blockedArtists.any { lowerArtist.contains(it) }) return false
        }
        return song.title.isNotBlank() && song.artist.isNotBlank()
    }

    /**
     * Loads cached mixes and carousels from Room for instant cold launch & offline fallback.
     * Returns true if fresh (< CACHE_TTL_MS) cache matching current personalization preference was loaded.
     */
    private suspend fun loadCachedRecommendations(): Boolean = withContext(Dispatchers.IO) {
        val cachedMixesEntity = recommendationDao.getCachedRecommendations(CACHE_KEY_MIXES)
        val cachedSectionsEntity = recommendationDao.getCachedRecommendations(CACHE_KEY_SECTIONS)

        var mixes = cachedMixesEntity?.let { parseMixesJson(it.songsJson) }
            ?.map { mix -> mix.copy(songs = mix.songs.filter { filterSong(it) }) }
            ?.filter { it.songs.isNotEmpty() }
            ?: emptyList()

        var sections = cachedSectionsEntity?.let { parseSectionsJson(it.songsJson) }
            ?.map { sec -> sec.copy(songs = sec.songs.filter { filterSong(it) }) }
            ?.filter { it.songs.isNotEmpty() }
            ?: emptyList()

        // Complete offline fallback using local Favorites, Downloads & Saved Playlists
        if (sections.isEmpty() && mixes.isEmpty()) {
            val offlineFavs = runCatching { songDao.getFavorites().first().map { it.toSong() } }.getOrDefault(emptyList())
            val offlineDls = runCatching { songDao.getDownloads().first().map { it.toSong() } }.getOrDefault(emptyList())
            val offlinePlaylists = runCatching {
                playlistDao.getAllPlaylistsSync().flatMap { parseSongsJson(it.songsJson) }
            }.getOrDefault(emptyList())
            val offlinePool = (offlineFavs + offlineDls + offlinePlaylists)
                .distinctBy { it.id }
                .filter { filterSong(it) }

            val fallbackList = if (offlinePool.isNotEmpty()) {
                offlinePool
            } else {
                PresetCatalog.PRESET_SONGS.filter { filterSong(it) }
            }

            if (fallbackList.isNotEmpty()) {
                val offlineMix = MadeForYouMix(
                    id = "daily_mix_1",
                    mixNumber = 1,
                    badgeLabel = "DAILY MIX 1",
                    title = "Daily Mix 1",
                    subtitle = fallbackList.map { extractPrimaryArtist(it.artist) }.distinct().take(3).joinToString(", ") + " and more",
                    accentColorHex = 0xFFE8C57B,
                    collageUrls = fallbackList.mapNotNull { it.coverUrl.takeIf { u -> u.isNotBlank() } }.distinct().take(4),
                    songs = fallbackList.take(20)
                )
                mixes = listOf(offlineMix)
                sections = listOf(
                    RecommendationSection(
                        categoryKey = "recommended_for_you",
                        title = if (offlinePool.isNotEmpty()) "Your Library Picks" else "Recommended for you",
                        subtitle = if (offlinePool.isNotEmpty()) "Available offline from your favorites & downloads" else "Starter tracks ready to play",
                        songs = fallbackList.take(15),
                        canRefresh = true,
                        badgeText = "FOR YOU",
                        sectionGroup = "personalized"
                    )
                )
            }
        }

        if (sections.isNotEmpty() || mixes.isNotEmpty()) {
            val discoverSection = mixes.find { it.id == "discover_weekly" }?.songs
                ?: sections.find { it.categoryKey == "discover_weekly" }?.songs
                ?: emptyList()
            val updatedAt = maxOf(
                cachedMixesEntity?.updatedAt ?: 0L,
                cachedSectionsEntity?.updatedAt ?: 0L
            )
            _recommendationsState.value = RecommendationSectionsState(
                isLoading = false,
                isPersonalized = _isPersonalizationEnabled.value,
                madeForYouMixes = mixes,
                sections = sections,
                discoverWeekly = discoverSection,
                lastUpdated = updatedAt
            )
            val isFresh = updatedAt > 0L && (System.currentTimeMillis() - updatedAt) < CACHE_TTL_MS
            return@withContext isFresh
        }
        false
    }

    private suspend fun saveRecommendationsToCache(
        mixes: List<MadeForYouMix>,
        sections: List<RecommendationSection>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (mixes.isNotEmpty()) {
            recommendationDao.saveCachedRecommendations(
                CachedRecommendationEntity(
                    categoryKey = CACHE_KEY_MIXES,
                    songsJson = serializeMixesJson(mixes),
                    updatedAt = now
                )
            )
        }
        if (sections.isNotEmpty()) {
            recommendationDao.saveCachedRecommendations(
                CachedRecommendationEntity(
                    categoryKey = CACHE_KEY_SECTIONS,
                    songsJson = serializeSectionsJson(sections),
                    updatedAt = now
                )
            )
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        recommendationDao.clearHistory()
        recommendationDao.clearRecommendationCache()
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

    private fun <T> List<T>.sliceSafe(from: Int, toExclusive: Int): List<T> {
        if (isEmpty() || from >= size) return emptyList()
        return subList(from, minOf(toExclusive, size))
    }

    private fun serializeSongsArray(songs: List<Song>): JSONArray {
        val array = JSONArray()
        for (s in songs) {
            array.put(
                JSONObject().apply {
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
            )
        }
        return array
    }

    private fun parseSongsArray(array: JSONArray?): List<Song> {
        if (array == null) return emptyList()
        val list = mutableListOf<Song>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
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
        return list
    }

    private fun parseSongsJson(jsonStr: String): List<Song> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            parseSongsArray(JSONArray(jsonStr))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeMixesJson(mixes: List<MadeForYouMix>): String {
        val arr = JSONArray()
        for (mix in mixes) {
            val obj = JSONObject().apply {
                put("id", mix.id)
                if (mix.mixNumber != null) put("mixNumber", mix.mixNumber)
                put("badgeLabel", mix.badgeLabel)
                put("title", mix.title)
                put("subtitle", mix.subtitle)
                put("accentColorHex", mix.accentColorHex)
                put("seedArtist", mix.seedArtist)
                put("collageUrls", JSONArray(mix.collageUrls))
                put("songs", serializeSongsArray(mix.songs))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseMixesJson(jsonStr: String): List<MadeForYouMix> {
        if (jsonStr.isBlank()) return emptyList()
        val result = mutableListOf<MadeForYouMix>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val collageArr = obj.optJSONArray("collageUrls")
                val collage = mutableListOf<String>()
                if (collageArr != null) {
                    for (j in 0 until collageArr.length()) {
                        val u = collageArr.optString(j)
                        if (u.isNotBlank()) collage.add(u)
                    }
                }
                result.add(
                    MadeForYouMix(
                        id = obj.optString("id"),
                        mixNumber = if (obj.has("mixNumber")) obj.optInt("mixNumber") else null,
                        badgeLabel = obj.optString("badgeLabel", "DAILY MIX"),
                        title = obj.optString("title", "Daily Mix"),
                        subtitle = obj.optString("subtitle"),
                        accentColorHex = obj.optLong("accentColorHex", 0xFFE8C57B),
                        collageUrls = collage,
                        songs = parseSongsArray(obj.optJSONArray("songs")),
                        seedArtist = obj.optString("seedArtist")
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializeSectionsJson(sections: List<RecommendationSection>): String {
        val arr = JSONArray()
        for (sec in sections) {
            val obj = JSONObject().apply {
                put("categoryKey", sec.categoryKey)
                put("title", sec.title)
                put("subtitle", sec.subtitle)
                put("canRefresh", sec.canRefresh)
                if (sec.badgeText != null) put("badgeText", sec.badgeText)
                if (sec.seedImageUrl != null) put("seedImageUrl", sec.seedImageUrl)
                put("sectionGroup", sec.sectionGroup)
                put("songs", serializeSongsArray(sec.songs))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseSectionsJson(jsonStr: String): List<RecommendationSection> {
        if (jsonStr.isBlank()) return emptyList()
        val result = mutableListOf<RecommendationSection>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                result.add(
                    RecommendationSection(
                        categoryKey = obj.optString("categoryKey"),
                        title = obj.optString("title"),
                        subtitle = obj.optString("subtitle"),
                        songs = parseSongsArray(obj.optJSONArray("songs")),
                        canRefresh = obj.optBoolean("canRefresh", false),
                        badgeText = obj.optString("badgeText").takeIf { it.isNotBlank() },
                        seedImageUrl = obj.optString("seedImageUrl").takeIf { it.isNotBlank() },
                        sectionGroup = obj.optString("sectionGroup", "discover")
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }
}
