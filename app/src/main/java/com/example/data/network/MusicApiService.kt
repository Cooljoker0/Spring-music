package com.example.data.network

import com.example.data.model.PlaylistDetailResponseDto
import com.example.data.model.PlaylistsResponseDto
import com.example.data.model.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {
    @GET("api/music/search")
    suspend fun searchSongs(@Query("q") query: String): SearchResponseDto

    @GET("api/music/playlists")
    suspend fun searchPlaylists(@Query("q") query: String): PlaylistsResponseDto

    @GET("api/music/playlist/{id}")
    suspend fun getPlaylistDetail(@Path("id") playlistId: String): PlaylistDetailResponseDto
}
