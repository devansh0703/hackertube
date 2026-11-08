package com.github.hackertube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.hackertube.R
import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.constants.PreferenceKeys.HOME_TAB_CONTENT
import com.github.hackertube.data.HtCollection
import com.github.hackertube.data.HtPlaylist
import com.github.hackertube.databinding.FragmentHomeBinding
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.helpers.PreferenceHelper
import com.github.hackertube.ui.activities.SettingsActivity
import com.github.hackertube.ui.adapters.CarouselCategoryAdapter
import com.github.hackertube.ui.adapters.CarouselHtPlaylistAdapter
import com.github.hackertube.ui.adapters.VideoCardsAdapter
import com.github.hackertube.ui.models.HomeViewModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val topPicksAdapter = VideoCardsAdapter(columnWidthDp = 250f)
    private val highlightsAdapter = VideoCardsAdapter()
    private val collectionAdapter = CarouselCategoryAdapter()
    private val playlistAdapter = CarouselHtPlaylistAdapter()
    private val watchingAdapter = VideoCardsAdapter(columnWidthDp = 250f)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.bookmarksRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())
        binding.playlistsRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val collectionsSnapHelper = CarouselSnapHelper()
        collectionsSnapHelper.attachToRecyclerView(binding.bookmarksRV)

        val playlistsSnapHelper = CarouselSnapHelper()
        playlistsSnapHelper.attachToRecyclerView(binding.playlistsRV)

        binding.trendingRV.adapter = highlightsAdapter
        binding.featuredRV.adapter = topPicksAdapter
        binding.bookmarksRV.adapter = collectionAdapter
        binding.playlistsRV.adapter = playlistAdapter
        binding.playlistsRV.adapter?.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                if (itemCount == 0) {
                    binding.playlistsRV.isGone = true
                    binding.playlistsTV.isGone = true
                }
            }
        })
        binding.watchingRV.adapter = watchingAdapter

        headerTextView(binding.featuredTV)?.setText(R.string.top_picks)
        headerTextView(binding.bookmarksTV)?.setText(R.string.collections)

        with(homeViewModel) {
            topPicks.observe(viewLifecycleOwner, ::showTopPicks)
            highlights.observe(viewLifecycleOwner, ::showHighlights)
            collections.observe(viewLifecycleOwner, ::showCollections)
            playlists.observe(viewLifecycleOwner, ::showPlaylists)
            continueWatching.observe(viewLifecycleOwner, ::showContinueWatching)
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }

        binding.featuredTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.watchingTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_watchHistoryFragment)
        }

        binding.trendingTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.playlistsTV.setOnClickListener {
            NavigationHelper.navigateCuratedPlaylists(requireContext())
        }

        binding.bookmarksTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.trendingRegion.isGone = true
        binding.trendingCategory.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }

        binding.changeInstance.setOnClickListener {
            redirectToIntentSettings()
        }
    }

    override fun onResume() {
        super.onResume()

        if (homeViewModel.loadedSuccessfully.value == false) {
            fetchHomeFeed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        binding.nothingHere.isGone = true
        val defaultItems = resources.getStringArray(R.array.homeTabItemsValues)
        val visibleItems = PreferenceHelper.getStringSet(HOME_TAB_CONTENT, defaultItems.toSet())

        homeViewModel.loadHomeFeed(requireContext(), visibleItems)
    }

    private fun showTopPicks(streamItems: List<StreamItem>?) {
        if (streamItems == null) return

        makeVisible(binding.featuredRV, binding.featuredTV)
        topPicksAdapter.submitList(streamItems.take(20))
    }

    private fun showHighlights(highlights: HomeViewModel.Highlights?) {
        if (highlights == null) return

        makeVisible(binding.trendingRV, binding.trendingTV)
        headerTextView(binding.trendingTV)?.text =
            getString(R.string.top_in_topic, highlights.category.name)
        highlightsAdapter.submitList(highlights.videos.take(10))
    }

    private fun showCollections(collections: List<HtCollection>?) {
        if (collections == null) return

        makeVisible(binding.bookmarksTV, binding.bookmarksRV)
        collectionAdapter.submitList(collections)
    }

    private fun showPlaylists(playlists: List<HtPlaylist>?) {
        if (playlists == null) return

        makeVisible(binding.playlistsRV, binding.playlistsTV)
        playlistAdapter.submitList(playlists)
    }

    private fun showContinueWatching(unwatchedVideos: List<StreamItem>?) {
        if (unwatchedVideos == null) return

        makeVisible(binding.watchingRV, binding.watchingTV)
        watchingAdapter.submitList(unwatchedVideos)
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progress.isVisible = !binding.refresh.isRefreshing
        binding.nothingHere.isVisible = false
        binding.scroll.alpha = 0.3f
    }

    private fun hideLoading() {
        binding.progress.isVisible = false
        binding.refresh.isRefreshing = false

        val hasContent = homeViewModel.loadedSuccessfully.value == true
        if (hasContent) {
            showContent()
        } else {
            showNothingHere()
        }
        binding.scroll.alpha = 1.0f
    }

    private fun showNothingHere() {
        binding.nothingHere.isVisible = true
        binding.scroll.isVisible = false
    }

    private fun showContent() {
        binding.nothingHere.isVisible = false
        binding.scroll.isVisible = true
    }

    private fun headerTextView(container: View): TextView? =
        (container as? android.view.ViewGroup)?.getChildAt(0) as? TextView

    private fun redirectToIntentSettings() {
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.REDIRECT_KEY, SettingsActivity.REDIRECT_TO_INTENT_SETTINGS)
        }
        startActivity(settingsIntent)
    }

    private fun makeVisible(vararg views: View) {
        views.forEach { it.isVisible = true }
    }
}
