package com.example.auralis.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.auralis.data.database.AppDatabase
import com.example.auralis.data.models.Playlist
import com.example.auralis.data.models.PlaylistSong
import com.example.auralis.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val songDao = AppDatabase.getInstance(context).songDao()
    private val playlistDao = AppDatabase.getInstance(context).playlistDao()
    private val playlistSongDao = AppDatabase.getInstance(context).playlistSongDao()

    // Songs
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    suspend fun refreshSongs() {
        withContext(Dispatchers.IO) {
            val songs = scanAllSongs()
            songDao.insertAll(songs)
        }
    }

    private fun scanAllSongs(): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown"
                val artist = it.getString(artistCol) ?: "Unknown"
                val album = it.getString(albumCol) ?: "Unknown"
                val duration = it.getLong(durCol)
                val path = it.getString(pathCol)
                val albumId = it.getLong(albumIdCol)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()
                songList.add(Song(id, title, artist, album, duration, path, albumArtUri))
            }
        }
        return songList
    }

    // Playlists
    suspend fun createPlaylist(name: String): Long = playlistDao.insert(Playlist(name = name))

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistSongDao.deleteByPlaylistId(playlist.id)
        playlistDao.delete(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, order: Int) {
        val existing = playlistSongDao.getEntry(playlistId, songId)
        if (existing == null) {
            playlistSongDao.insert(PlaylistSong(playlistId = playlistId, songId = songId, order = order))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val entry = playlistSongDao.getEntry(playlistId, songId)
        entry?.let { playlistSongDao.delete(it) }
    }

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun getSongsForPlaylist(playlistId: Long): List<Song> {
        val songIds = playlistSongDao.getSongIdsForPlaylist(playlistId)
        val songs = mutableListOf<Song>()
        for (id in songIds) {
            songDao.getSongById(id)?.let { songs.add(it) }
        }
        return songs
    }
}
