package com.example.auralis.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.auralis.data.models.Song
import com.example.auralis.data.models.Playlist
import com.example.auralis.data.models.PlaylistSong

@Database(
    entities = [Song::class, Playlist::class, PlaylistSong::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auralis_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
