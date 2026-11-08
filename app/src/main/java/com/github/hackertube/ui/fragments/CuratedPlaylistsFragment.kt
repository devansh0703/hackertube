package com.github.hackertube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.hackertube.R
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.databinding.FragmentHackertubePlaylistsBinding
import com.github.hackertube.ui.adapters.HtPlaylistsAdapter
import kotlinx.coroutines.launch

class CuratedPlaylistsFragment : Fragment(R.layout.fragment_hackertube_playlists) {
    private var _binding: FragmentHackertubePlaylistsBinding? = null
    private val binding get() = _binding!!

    private val adapter = HtPlaylistsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHackertubePlaylistsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.playlistsRecview.layoutManager = LinearLayoutManager(context)
        binding.playlistsRecview.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val playlists = HackerTubeRepository.curatedPlaylists(catalog)

            adapter.submitList(playlists)
            binding.progress.isVisible = false
            binding.empty.isVisible = playlists.isEmpty()
            binding.playlistsRecview.isVisible = playlists.isNotEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
