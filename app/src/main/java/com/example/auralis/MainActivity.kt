package com.example.auralis

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.auralis.databinding.ActivityMainBinding
import com.example.auralis.service.MusicService
import com.example.auralis.ui.LibraryFragment
import com.example.auralis.ui.PlaylistsFragment
import com.example.auralis.ui.PlayerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, MusicService::class.java))
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        requestPermissions()
        setupBottomNav()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LibraryFragment())
                .commit()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
                return
            }
        }
    }

    private fun setupBottomNav() {
        val navView: BottomNavigationView = binding.bottomNav
        navView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_library -> LibraryFragment()
                R.id.nav_playlists -> PlaylistsFragment()
                R.id.nav_player -> PlayerFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }
    }

    fun getMusicService(): MusicService? = musicService

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(serviceConnection)
    }
}
