package com.github.hackertube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.hackertube.R
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtArea
import com.github.hackertube.data.HtFilters
import com.github.hackertube.data.toStreamItem
import com.github.hackertube.databinding.FragmentTrendsBinding
import com.github.hackertube.databinding.FragmentTrendsContentBinding
import com.github.hackertube.ui.adapters.VideoCardsAdapter
import com.github.hackertube.ui.base.DynamicLayoutManagerFragment
import com.github.hackertube.ui.sheets.HackerTubeFilterSheet
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class TrendsFragment : Fragment(R.layout.fragment_trends) {
    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    private var filters = HtFilters()
    private var trendsAdapter: TrendsAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrendsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.filterButton.setOnClickListener { showFilterSheet() }
        binding.chipRecent.setOnClickListener {
            filters = filters.copy(sort = if (filters.sort == "recent") "" else "recent")
            applyFilters()
        }
        binding.chipLongs.setOnClickListener {
            filters = filters.copy(duration = if (filters.duration == "xlong") "" else "xlong")
            applyFilters()
        }
        binding.chipGems.setOnClickListener {
            filters = filters.copy(gems = !filters.gems)
            applyFilters()
        }
        binding.chipAll.setOnClickListener {
            filters = HtFilters()
            applyFilters()
        }

        childFragmentManager.setFragmentResultListener(
            HackerTubeFilterSheet.REQUEST_KEY,
            this
        ) { _, result ->
            filters = HtFilters.fromBundle(result)
            applyFilters()
        }

        updateFilterChips()

        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val areas = HackerTubeRepository.areasWithVideos(catalog)
            if (areas.isEmpty()) return@launch

            val adapter = TrendsAdapter(this@TrendsFragment, areas).also {
                it.filters = filters
                trendsAdapter = it
            }
            binding.pager.adapter = adapter

            if (areas.size <= 1) binding.tabLayout.isGone = true
            TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
                tab.text = areas[position].name
            }.attach()
        }
    }

    private fun showFilterSheet() {
        HackerTubeFilterSheet().apply {
            arguments = filters.toBundle()
        }.show(childFragmentManager, HackerTubeFilterSheet::class.java.name)
    }

    private fun applyFilters() {
        updateFilterChips()
        trendsAdapter?.applyFilters(filters)
    }

    private fun updateFilterChips() {
        binding.chipRecent.isChecked = filters.sort == "recent"
        binding.chipLongs.isChecked = filters.duration == "xlong"
        binding.chipGems.isChecked = filters.gems
        binding.chipAll.isChecked = !filters.isActive
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class TrendsAdapter(fragment: Fragment, private val areas: List<HtArea>) :
    FragmentStateAdapter(fragment) {
    private val fragments: MutableList<TrendsContentFragment?> =
        MutableList(areas.size) { null }

    var filters = HtFilters()

    override fun createFragment(position: Int): Fragment {
        val trendContentFragment = TrendsContentFragment().apply {
            arguments = bundleOf(
                ARG_AREA_ID to areas[position].id
            )
            filters = this@TrendsAdapter.filters
        }
        fragments[position] = trendContentFragment
        return trendContentFragment
    }

    override fun getItemCount(): Int {
        return areas.size
    }

    fun getFragmentAt(position: Int): TrendsContentFragment? {
        return fragments[position]
    }

    fun applyFilters(newFilters: HtFilters) {
        filters = newFilters
        fragments.forEach { it?.applyFilters(newFilters) }
    }

    companion object {
        const val ARG_AREA_ID = "areaId"
    }
}

class TrendsContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_trends_content) {
    private var _binding: FragmentTrendsContentBinding? = null
    private val binding get() = _binding!!

    private val adapter = VideoCardsAdapter()
    private var recyclerViewState: Parcelable? = null
    var filters = HtFilters()

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.recview?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrendsContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.recview.adapter = adapter
        binding.recview.layoutManager?.onRestoreInstanceState(recyclerViewState)

        binding.recview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })

        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            loadVideos()
        }

        loadVideos()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        _binding?.recview?.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    fun applyFilters(newFilters: HtFilters) {
        filters = newFilters
        if (_binding != null) loadVideos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadVideos() {
        val areaId = arguments?.getString(TrendsAdapter.ARG_AREA_ID) ?: return
        toggleLoadingIndicator(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = HackerTubeRepository.catalog(requireContext())
            val videos = HackerTubeRepository.videosInArea(catalog, areaId, filters)
                .map { it.toStreamItem() }

            adapter.submitList(videos)
            toggleLoadingIndicator(false)
        }
    }

    private fun toggleLoadingIndicator(show: Boolean) {
        binding.recview.alpha = if (show) 0.3f else 1.0f
        binding.progressBar.isGone = !show
        if (!show) binding.homeRefresh.isRefreshing = false
    }
}
