package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Tracks every playback event locally to understand user listening habits.
 */
@Entity(tableName = "song_play_history")
data class SongPlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val mood: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playDurationMs: Long = 0,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false
)

/**
 * Stores aggregate affinity scores for artists, genres, moods, and albums.
 */
@Entity(tableName = "user_taste_profile")
data class UserTasteProfileEntity(
    @PrimaryKey val key: String, // e.g. "artist:Arijit Singh", "genre:bollywood", "mood:chill"
    val type: String, // "artist", "genre", "mood", "album"
    val name: String,
    val affinityScore: Float,
    val playCount: Int,
    val skipCount: Int,
    val lastInteractedAt: Long = System.currentTimeMillis()
)

/**
 * Tracks user explicit feedback: dislikes, not interested, blocked artists.
 */
@Entity(tableName = "recommendation_feedback")
data class RecommendationFeedbackEntity(
    @PrimaryKey val id: String, // targetId + "_" + feedbackType
    val targetId: String, // songId or artistName
    val targetType: String, // "song", "artist"
    val feedbackType: String, // "dislike", "not_interested", "block_artist", "more_like_this"
    val targetName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Caches generated recommendation song lists to keep launch fast and support offline mode.
 */
@Entity(tableName = "cached_recommendations")
data class CachedRecommendationEntity(
    @PrimaryKey val categoryKey: String, // "recommended_for_you", "daily_mix", "discover_weekly", etc.
    val songsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface RecommendationDao {

    // --- Play History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: SongPlayHistoryEntity)

    @Query("SELECT * FROM song_play_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPlays(limit: Int = 50): List<SongPlayHistoryEntity>

    @Query("SELECT * FROM song_play_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPlaysFlow(limit: Int = 30): Flow<List<SongPlayHistoryEntity>>

    @Query("SELECT artist, COUNT(*) as playCount FROM song_play_history WHERE isSkipped = 0 GROUP BY artist ORDER BY playCount DESC LIMIT :limit")
    suspend fun getTopArtists(limit: Int = 10): List<ArtistPlayCount>

    @Query("SELECT mood, COUNT(*) as playCount FROM song_play_history WHERE isSkipped = 0 AND mood != '' GROUP BY mood ORDER BY playCount DESC LIMIT :limit")
    suspend fun getTopMoods(limit: Int = 10): List<MoodPlayCount>

    @Query("SELECT songId, title, artist, album, coverUrl, audioUrl, durationMs, mood, COUNT(*) as playCount FROM song_play_history WHERE isSkipped = 0 GROUP BY songId ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayedSongs(limit: Int = 20): List<SongPlayCount>

    @Query("DELETE FROM song_play_history")
    suspend fun clearHistory()

    // --- Taste Profile ---
    @Query("SELECT * FROM user_taste_profile WHERE type = :type ORDER BY affinityScore DESC LIMIT :limit")
    suspend fun getTopAffinities(type: String, limit: Int = 10): List<UserTasteProfileEntity>

    @Query("SELECT * FROM user_taste_profile WHERE `key` = :key LIMIT 1")
    suspend fun getTasteProfile(key: String): UserTasteProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasteProfile(profile: UserTasteProfileEntity)

    @Query("DELETE FROM user_taste_profile")
    suspend fun clearTasteProfiles()

    // --- Feedback ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: RecommendationFeedbackEntity)

    @Query("SELECT targetId FROM recommendation_feedback WHERE targetType = 'song' AND feedbackType IN ('dislike', 'not_interested')")
    suspend fun getExcludedSongIds(): List<String>

    @Query("SELECT targetId FROM recommendation_feedback WHERE targetType = 'artist' AND feedbackType = 'block_artist'")
    suspend fun getBlockedArtists(): List<String>

    @Query("SELECT * FROM recommendation_feedback ORDER BY timestamp DESC")
    fun getAllFeedbackFlow(): Flow<List<RecommendationFeedbackEntity>>

    @Query("DELETE FROM recommendation_feedback WHERE id = :id")
    suspend fun removeFeedback(id: String)

    @Query("DELETE FROM recommendation_feedback")
    suspend fun clearAllFeedback()

    // --- Cache ---
    @Query("SELECT * FROM cached_recommendations WHERE categoryKey = :key LIMIT 1")
    suspend fun getCachedRecommendations(key: String): CachedRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedRecommendations(cached: CachedRecommendationEntity)

    @Query("DELETE FROM cached_recommendations")
    suspend fun clearRecommendationCache()
}

data class ArtistPlayCount(
    val artist: String,
    val playCount: Int
)

data class MoodPlayCount(
    val mood: String,
    val playCount: Int
)

data class SongPlayCount(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val mood: String,
    val playCount: Int
)
