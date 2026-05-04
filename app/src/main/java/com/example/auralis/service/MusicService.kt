package com.example.auralis.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.auralis.MainActivity
import com.example.auralis.R
import com.example.auralis.data.models.Song
import java.io.IOException

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex = -1
    private var isPlaying = false

    private lateinit var mediaSession: MediaSessionCompat
    private var notificationManager: NotificationManager? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        setupMediaSession()
        registerReceiver(notificationReceiver, IntentFilter(NotificationReceiver.ACTION_UPDATE))
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    resumePlayback()
                }
                override fun onPause() {
                    super.onPause()
                    pausePlayback()
                }
                override fun onSkipToNext() {
                    super.onSkipToNext()
                    playNext()
                }
                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    playPrevious()
                }
            })
            isActive = true
        }
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0, 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build()
        )
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int) {
        playlist = songs
        currentIndex = startIndex
        playSongAtIndex(currentIndex)
    }

    private fun playSongAtIndex(index: Int) {
        if (index !in playlist.indices) return
        currentSong = playlist[index]
        currentIndex = index
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(currentSong!!.path)
                prepare()
                start()
                setOnCompletionListener {
                    playNext()
                }
            }
            isPlaying = true
            updatePlaybackState()
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun resumePlayback() {
        mediaPlayer?.start()
        isPlaying = true
        updatePlaybackState()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlaybackState()
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    fun playNext() {
        if (currentIndex + 1 < playlist.size) {
            playSongAtIndex(currentIndex + 1)
        } else {
            pausePlayback()
        }
    }

    fun playPrevious() {
        if (currentIndex - 1 >= 0) {
            playSongAtIndex(currentIndex - 1)
        }
    }

    fun getCurrentSong(): Song? = currentSong
    fun isPlaying(): Boolean = isPlaying
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun seekTo(position: Int) { mediaPlayer?.seekTo(position) }

    private fun buildNotification(): Notification {
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, NotificationReceiver::class.java).apply { action = "PREV" }
        val prevPending = PendingIntent.getBroadcast(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val playIntent = Intent(this, NotificationReceiver::class.java).apply { action = "PLAY_PAUSE" }
        val playPending = PendingIntent.getBroadcast(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = Intent(this, NotificationReceiver::class.java).apply { action = "NEXT" }
        val nextPending = PendingIntent.getBroadcast(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Auralis")
            .setContentText(currentSong?.artist ?: "Не играет")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setStyle(style)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
            .addAction(playPauseIcon, "Play/Pause", playPending)
            .addAction(R.drawable.ic_skip_next, "Next", nextPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Музыкальный плеер",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setSound(null, null) }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        unregisterReceiver(notificationReceiver)
    }

    companion object {
        private const val CHANNEL_ID = "auralis_music_channel"
        private const val NOTIFICATION_ID = 1
    }

    inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "PREV" -> playPrevious()
                "PLAY_PAUSE" -> if (isPlaying) pausePlayback() else resumePlayback()
                "NEXT" -> playNext()
            }
        }
    }
    private val notificationReceiver = NotificationReceiver()
}
