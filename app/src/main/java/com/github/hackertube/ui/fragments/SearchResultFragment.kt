package com.github.hackertube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.hackertube.R
import com.github.hackertube.data.HtFilters
import com.github.hackertube.databinding.FragmentSearchResultBinding
import com.github.hackertube.extensions.ceilHalf
import com.github.hackertube.ui.activities.MainActivity
import com.github.hackertube.ui.adapters.SearchResultsAdapter
import com.github.hackertube.ui.base.DynamicLayoutManagerFragment
import com.github.hackertube.ui.extensions.setOnBackPressed
import com.github.hackertube.ui.models.SearchResultViewModel
import com.github.hackertube.ui.sheets.HackerTubeFilterSheet
import com.github.hackertube.util.TextUtils.toTimeInSeconds
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class SearchResultFragment : DynamicLayoutManagerFragment(R.layout.fragment_search_result) {
    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<SearchResultFragmentArgs>()
    private val viewModel by viewModels<SearchResultViewModel>()

    private val mainActivity get() = activity as MainActivity
    private var recyclerViewState: Parcelable? = null
    private var filters = HtFilters()

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.searchRecycler?.layoutManager = GridLayoutManager(context, gridItems.ceilHalf())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSearchResultBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // fixes a bug that the search query will stay the old one when searching for multiple
        // different queries in a row and navigating to the previous ones through back presses
        mainActivity.setQuerySilent(args.query)

        // the old YouTube content-type chips are not relevant to the HackerTube catalogue
        binding.filterChipGroup.isGone = true

        binding.searchFilterButton.setOnClickListener {
            HackerTubeFilterSheet().apply {
                arguments = filters.toBundle()
            }.show(childFragmentManager, HackerTubeFilterSheet::class.java.name)
        }
        binding.searchChipRecent.setOnClickListener {
            filters = filters.copy(sort = if (filters.sort == "recent") "" else "recent")
            applyFilters()
        }
        binding.searchChipLongs.setOnClickListener {
            filters = filters.copy(duration = if (filters.duration == "xlong") "" else "xlong")
            applyFilters()
        }
        binding.searchChipGems.setOnClickListener {
            filters = filters.copy(gems = !filters.gems)
            applyFilters()
        }
        binding.searchChipAll.setOnClickListener {
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

        val timeStamp = args.query.toHttpUrlOrNull()?.queryParameter("t")?.toTimeInSeconds()
        val searchResultsAdapter = SearchResultsAdapter(timeStamp ?: 0)
        binding.searchRecycler.adapter = searchResultsAdapter

        binding.searchRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchResultsAdapter.loadStateFlow.collect {
                    val isLoading = it.source.refresh is LoadState.Loading
                    binding.progress.isVisible = isLoading
                    binding.searchResultsLayout.isGone = isLoading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResultsFlow.collectLatest {
                    searchResultsAdapter.submitData(it)
                }
            }
        }

        viewModel.searchSuggestion.observe(viewLifecycleOwner) { suggestion ->
            binding.searchSuggestionContainer.isVisible = suggestion != null
            binding.searchSuggestionContainer.setOnClickListener(null)
            if (suggestion == null) return@observe

            val (suggestion, corrected) = suggestion
            binding.searchSuggestion.text = suggestion
            binding.searchSuggestionLabel.text = if (corrected) {
                getString(R.string.showing_results_for)
            } else {
                binding.searchSuggestionContainer.setOnClickListener {
                    mainActivity.setQuery(suggestion, true)
                }
                getString(R.string.did_you_mean)
            }
        }

        setOnBackPressed {
            findNavController().popBackStack(R.id.searchFragment, true) ||
                    findNavController().popBackStack()
        }
    }

    private fun applyFilters() {
        updateFilterChips()
        viewModel.setFilters(filters)
    }

    private fun updateFilterChips() {
        binding.searchChipRecent.isChecked = filters.sort == "recent"
        binding.searchChipLongs.isChecked = filters.duration == "xlong"
        binding.searchChipGems.isChecked = filters.gems
        binding.searchChipAll.isChecked = !filters.isActive
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        binding.searchRecycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
