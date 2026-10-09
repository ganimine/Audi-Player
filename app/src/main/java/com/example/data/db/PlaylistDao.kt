package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getItemsForPlaylist(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeItemFromPlaylist(playlistId: String, mediaId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    // History & Favorites
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getPlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHistory(history: PlaybackHistoryEntity)

    @Query("UPDATE playback_history SET isFavorite = :isFav WHERE mediaId = :mediaId")
    suspend fun setFavorite(mediaId: String, isFav: Boolean)

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getHistoryForMedia(mediaId: String): PlaybackHistoryEntity?
}
