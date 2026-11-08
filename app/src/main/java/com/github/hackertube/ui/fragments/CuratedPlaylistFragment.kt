package com.github.hackertube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.hackertube.R
import com.github.hackertube.constants.IntentData
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtVideo
import com.github.hackertube.data.toStreamItem
import com.github.hackertube.databinding.FragmentCuratedPlaylistBinding
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.parcelable.PlayerData
import com.github.hackertube.ui.adapters.PlaylistAdapter
import com.github.hackertube.ui.adapters.PlaylistItem
import com.github.hackertube.util.PlayingQueue
import kotlinx.coroutines.launch

class CuratedPlaylistFragment : Fragment(R.layout.fragment_curated_playlist) {
    private var _binding: FragmentCuratedPlaylistBinding? = null
    private val binding get() = _binding!!

    private var videos: List<HtVideo> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCuratedPlaylistBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        val slug = arguments?.getString(IntentData.playlistSlug) ?: run {
            findNavController().popBackStack()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val playlist = catalog.playlistById[slug] ?: run {
                findNavController().popBackStack()
                return@launch
            }

            videos = HackerTubeRepository.playlistVideos(catalog, playlist)

            binding.curatedTitle.text = playlist.title
            binding.curatedPitch.text = playlist.pitch
            binding.curatedInfo.text = getString(
                R.string.hackertube_curated_playlist_info,
                videos.size,
                playlist.minutes
            )
            binding.curatedIntro.text = playlist.intro
            binding.curatedIntro.isVisible = playlist.intro.isNotBlank()

            val adapter = PlaylistAdapter("hackertube:$slug") { streamItem ->
                val video = videos.firstOrNull { it.vid == streamItem.url }
                if (video != null) play(video)
            }
            binding.curatedRecview.layoutManager = LinearLayoutManager(context)
            binding.curatedRecview.adapter = adapter
            adapter.submitList(videos.mapIndexed { index, video ->
                PlaylistItem(video.toStreamItem(), index)
            })

            binding.curatedPlayAll.setOnClickListener {
                videos.firstOrNull()?.let(::play)
            }
            binding.curatedPlayAll.isVisible = videos.isNotEmpty()
            binding.curatedProgress.isVisible = false
        }
    }

    private fun play(video: HtVideo) {
        if (videos.isEmpty()) return

        val streams = videos.map { it.toStreamItem() }
        val selected = streams.firstOrNull { it.url == video.vid } ?: return

        PlayingQueue.clear()
        PlayingQueue.setStreams(streams)
        PlayingQueue.updateCurrent(selected)

        NavigationHelper.navigateVideo(
            requireContext(),
            PlayerData(video.vid, keepQueue = true)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
