package com.github.hackertube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.hackertube.R
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtFilters
import com.github.hackertube.data.toStreamItem
import com.github.hackertube.databinding.FragmentCategoryBinding
import com.github.hackertube.ui.adapters.VideoCardsAdapter
import com.github.hackertube.ui.base.DynamicLayoutManagerFragment
import com.github.hackertube.ui.sheets.HackerTubeFilterSheet
import kotlinx.coroutines.launch

class CategoryFragment : DynamicLayoutManagerFragment(R.layout.fragment_category) {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<CategoryFragmentArgs>()
    private val adapter = VideoCardsAdapter()
    private var recyclerViewState: Parcelable? = null
    private var filters = HtFilters()

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.recview?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCategoryBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.recview.adapter = adapter
        binding.recview.layoutManager?.onRestoreInstanceState(recyclerViewState)
        binding.categoryTitle.text = args.categoryName

        binding.recview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })

        binding.filterButton.setOnClickListener {
            HackerTubeFilterSheet().apply {
                arguments = filters.toBundle()
            }.show(childFragmentManager, HackerTubeFilterSheet::class.java.name)
        }

        childFragmentManager.setFragmentResultListener(
            HackerTubeFilterSheet.REQUEST_KEY,
            this
        ) { _, result ->
            filters = HtFilters.fromBundle(result)
            loadVideos()
        }

        binding.homeRefresh.setOnRefreshListener { loadVideos() }
        loadVideos()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        _binding?.recview?.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadVideos() {
        binding.progressBar.isGone = false
        binding.homeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val videos = HackerTubeRepository.videosInCategory(catalog, args.categoryId, filters)

            adapter.submitList(videos.map { it.toStreamItem() })
            binding.categoryTitle.text = getString(
                R.string.category_video_count,
                args.categoryName,
                videos.size
            )
            binding.progressBar.isGone = true
            binding.homeRefresh.isRefreshing = false
        }
    }
}
