package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM saved_songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<SavedSongEntity>>

    @Query("SELECT * FROM saved_songs WHERE isDownloaded = 1 ORDER BY title ASC")
    fun getDownloads(): Flow<List<SavedSongEntity>>

    @Query("SELECT * FROM saved_songs WHERE lastPlayedTimestamp IS NOT NULL ORDER BY lastPlayedTimestamp DESC LIMIT 30")
    fun getRecentlyPlayed(): Flow<List<SavedSongEntity>>

    @Query("SELECT * FROM saved_songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SavedSongEntity?

    @Query("SELECT isFavorite FROM saved_songs WHERE id = :songId")
    fun isFavoriteFlow(songId: String): Flow<Boolean?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SavedSongEntity)

    @Query("UPDATE saved_songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun updateFavorite(songId: String, isFav: Boolean)

    @Query("UPDATE saved_songs SET isDownloaded = :isDl, localFilePath = :filePath WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDl: Boolean, filePath: String?)

    @Query("UPDATE saved_songs SET lastPlayedTimestamp = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayed(songId: String, timestamp: Long)

    @Query("DELETE FROM saved_songs WHERE isDownloaded = 1 AND isFavorite = 0 AND lastPlayedTimestamp IS NULL")
    suspend fun cleanOrphanedDownloads()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM user_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<UserPlaylistEntity>>

    @Query("SELECT * FROM user_playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsSync(): List<UserPlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: UserPlaylistEntity)

    @Query("SELECT * FROM user_playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): UserPlaylistEntity?

    @Query("DELETE FROM user_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)
}

