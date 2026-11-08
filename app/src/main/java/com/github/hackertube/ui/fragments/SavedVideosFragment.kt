package com.github.hackertube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.hackertube.R
import com.github.hackertube.constants.IntentData
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HackerTubeUserState
import com.github.hackertube.data.toStreamItem
import com.github.hackertube.databinding.FragmentSavedVideosBinding
import com.github.hackertube.ui.adapters.VideoCardsAdapter
import com.github.hackertube.ui.base.DynamicLayoutManagerFragment
import kotlinx.coroutines.launch

class SavedVideosFragment : DynamicLayoutManagerFragment(R.layout.fragment_saved_videos) {
    private var _binding: FragmentSavedVideosBinding? = null
    private val binding get() = _binding!!

    private val adapter = VideoCardsAdapter()
    private var recyclerViewState: Parcelable? = null
    private var mode: String = MODE_LIKED

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.savedRecview?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSavedVideosBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        mode = arguments?.getString(IntentData.savedVideosMode) ?: MODE_LIKED
        binding.savedTitle.text = getString(
            if (mode == MODE_LATER) R.string.hackertube_watch_later
            else R.string.hackertube_liked_videos
        )

        binding.savedRecview.adapter = adapter
        binding.savedRecview.layoutManager?.onRestoreInstanceState(recyclerViewState)

        binding.savedRecview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })

        loadVideos()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) loadVideos()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        _binding?.savedRecview?.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val ids = if (mode == MODE_LATER) {
                HackerTubeUserState.watchLater(requireContext())
            } else {
                HackerTubeUserState.liked(requireContext())
            }

            val videos = ids.mapNotNull { catalog.byId[it] }
            adapter.submitList(videos.map { it.toStreamItem() })

            binding.savedProgress.isVisible = false
            binding.savedEmpty.isVisible = videos.isEmpty()
            binding.savedRecview.isVisible = videos.isNotEmpty()
        }
    }

    companion object {
        const val MODE_LIKED = "liked"
        const val MODE_LATER = "later"
    }
}
