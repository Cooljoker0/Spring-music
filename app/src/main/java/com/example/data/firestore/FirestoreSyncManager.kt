package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.AuthManager
import com.example.data.local.PlaylistDao
import com.example.data.local.SavedSongEntity
import com.example.data.local.SongDao
import com.example.data.local.UserPlaylistEntity
import com.example.data.model.Song
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {
    private val tag = "FirestoreSyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private var playlistListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow<String>("Idle")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _cloudSettings = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val cloudSettings: StateFlow<Map<String, Any?>> = _cloudSettings.asStateFlow()

    init {
        scope.launch {
            AuthManager.currentUser.collect { user ->
                if (user != null) {
                    onUserSignedIn(user.uid, user.email.orEmpty(), user.displayName.orEmpty(), user.photoUrl?.toString().orEmpty())
                } else {
                    onUserSignedOut()
                }
            }
        }
    }

    private fun onUserSignedIn(userId: String, email: String, displayName: String, avatarUrl: String) {
        scope.launch {
            try {
                _syncStatus.value = "Syncing..."
                // 1. Upsert User Profile
                val userDoc = db.collection("users").document(userId)
                val profileData = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "email" to email,
                    "displayName" to displayName,
                    "avatarUrl" to avatarUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnFailureListener { e ->
                        handleFirestoreError(e, OperationType.WRITE, userDoc.path)
                    }

                // 2. Attach real-time listener for Playlists
                playlistListener?.remove()
                playlistListener = db.collection("users").document(userId).collection("playlists")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            handleFirestoreError(error, OperationType.LIST, "users/$userId/playlists")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            scope.launch {
                                for (doc in snapshot.documents) {
                                    val id = doc.getString("id") ?: doc.id
                                    val title = doc.getString("title") ?: continue
                                    val desc = doc.getString("description").orEmpty()
                                    val cover = doc.getString("coverUrl").orEmpty()
                                    val count = doc.getLong("songCount")?.toInt() ?: 0
                                    val songsJson = doc.getString("songsJson").orEmpty()
                                    val isSpotify = doc.getBoolean("isSpotifyImport") ?: false

                                    val localEntity = UserPlaylistEntity(
                                        id = id,
                                        title = title,
                                        description = desc,
                                        coverUrl = cover,
                                        songCount = count,
                                        songsJson = songsJson,
                                        isSpotifyImport = isSpotify
                                    )
                                    playlistDao.insertPlaylist(localEntity)
                                }
                            }
                        }
                    }

                // 3. Attach real-time listener for Settings
                settingsListener?.remove()
                val settingsDoc = db.collection("users").document(userId).collection("settings").document("preferences")
                settingsListener = settingsDoc.addSnapshotListener { doc, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.GET, settingsDoc.path)
                        return@addSnapshotListener
                    }
                    if (doc != null && doc.exists()) {
                        _cloudSettings.value = doc.data ?: emptyMap()
                    }
                }

                // 4. Initial sync of local playlists to cloud if cloud is empty
                pushLocalPlaylistsToCloud(userId)

                _syncStatus.value = "Synced"
            } catch (e: Exception) {
                Log.e(tag, "Sync failed: ${e.message}", e)
                _syncStatus.value = "Sync error"
            }
        }
    }

    private suspend fun pushLocalPlaylistsToCloud(userId: String) {
        try {
            val localList = playlistDao.getAllPlaylistsSync()
            val collectionRef = db.collection("users").document(userId).collection("playlists")
            for (pl in localList) {
                val doc = collectionRef.document(pl.id)
                val data = mapOf(
                    "id" to pl.id,
                    "userId" to userId,
                    "title" to pl.title,
                    "description" to pl.description,
                    "coverUrl" to pl.coverUrl,
                    "songCount" to pl.songCount,
                    "songsJson" to pl.songsJson,
                    "isSpotifyImport" to pl.isSpotifyImport,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                doc.set(data, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to push local playlists to cloud: ${e.message}")
        }
    }

    private fun onUserSignedOut() {
        playlistListener?.remove()
        playlistListener = null
        settingsListener?.remove()
        settingsListener = null
        _syncStatus.value = "Signed out"
        _cloudSettings.value = emptyMap()
    }

    suspend fun savePlaylist(playlist: UserPlaylistEntity) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("playlists").document(playlist.id)
        val data = mapOf(
            "id" to playlist.id,
            "userId" to uid,
            "title" to playlist.title,
            "description" to playlist.description,
            "coverUrl" to playlist.coverUrl,
            "songCount" to playlist.songCount,
            "songsJson" to playlist.songsJson,
            "isSpotifyImport" to playlist.isSpotifyImport,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("playlists").document(playlistId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun saveSettings(
        audioQuality: String? = null,
        stopOnTaskRemoved: Boolean? = null,
        customThemePrompt: String? = null,
        primaryColorHex: String? = null,
        accentColorHex: String? = null
    ) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("settings").document("preferences")
        val data = mutableMapOf<String, Any>(
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        audioQuality?.let { data["audioQuality"] = it }
        stopOnTaskRemoved?.let { data["stopOnTaskRemoved"] = it }
        customThemePrompt?.let { data["customThemePrompt"] = it }
        primaryColorHex?.let { data["primaryColorHex"] = it }
        accentColorHex?.let { data["accentColorHex"] = it }

        try {
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun syncLikedSong(song: Song, isLiked: Boolean) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("likedSongs").document(song.id)
        try {
            if (isLiked) {
                val data = mapOf(
                    "id" to song.id,
                    "userId" to uid,
                    "songId" to song.id,
                    "title" to song.title,
                    "artist" to song.artist,
                    "album" to song.album,
                    "coverUrl" to song.coverUrl,
                    "streamUrl" to song.audioUrl,
                    "durationMs" to song.durationMs.toInt(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            } else {
                docRef.delete().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }
}
