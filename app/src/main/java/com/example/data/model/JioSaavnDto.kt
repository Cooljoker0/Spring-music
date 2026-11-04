package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: SearchDataDto? = null
)

@JsonClass(generateAdapter = true)
data class SearchDataDto(
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<SongItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistsResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: PlaylistDataDto? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistDataDto(
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<PlaylistItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistDetailResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: PlaylistDetailDataDto? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistDetailDataDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "songCount") val songCount: Any? = null,
    @Json(name = "songs") val songs: List<SongItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistItemDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "songCount") val songCount: Any? = null
)

@JsonClass(generateAdapter = true)
data class SongItemDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "duration") val duration: Any? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "album") val album: Any? = null,
    @Json(name = "artists") val artists: Any? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "downloadUrl") val downloadUrl: Any? = null
)
