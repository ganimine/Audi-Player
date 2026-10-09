package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverGradientIndex: Int,
    val createdAt: Long
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "mediaId"])
data class PlaylistItemEntity(
    val playlistId: String,
    val mediaId: String,
    val orderIndex: Int
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val mediaId: String,
    val lastPositionMs: Long,
    val lastPlayedAt: Long,
    val isFavorite: Boolean = false
)
