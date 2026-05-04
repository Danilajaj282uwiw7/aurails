package com.example.auralis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auralis.MainActivity
import com.example.auralis.databinding.FragmentLibraryBinding
import com.example.auralis.data.repository.MusicRepository
import com.example.auralis.ui.adapters.SongAdapter
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicRepository: MusicRepository
    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        musicRepository = MusicRepository(requireContext())
        songAdapter = SongAdapter { song ->
            val service = (activity as MainActivity).getMusicService()
            service?.setPlaylist(listOf(song), 0)
            val playerFragment = PlayerFragment()
            (activity as MainActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, playerFragment).commit()
        }
        binding.rvSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSongs.adapter = songAdapter

        lifecycleScope.launch {
            musicRepository.refreshSongs()
            musicRepository.getAllSongs().collect { songs ->
                songAdapter.submitList(songs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
