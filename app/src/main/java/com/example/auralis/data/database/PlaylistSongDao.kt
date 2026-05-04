package com.example.auralis.data.database

import androidx.room.*
import com.example.auralis.data.models.PlaylistSong

@Dao
interface PlaylistSongDao {
    @Insert
    suspend fun insert(playlistSong: PlaylistSong)

    @Delete
    suspend fun delete(playlistSong: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY `order` ASC")
    suspend fun getSongIdsForPlaylist(playlistId: Long): List<Long>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun getEntry(playlistId: Long, songId: Long): PlaylistSong?
}
