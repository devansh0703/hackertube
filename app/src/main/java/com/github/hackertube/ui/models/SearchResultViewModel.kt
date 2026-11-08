package com.github.hackertube.ui.models

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.hackertube.data.HtFilters
import com.github.hackertube.ui.dialogs.ShareDialog
import com.github.hackertube.ui.fragments.SearchResultFragmentArgs
import com.github.hackertube.ui.models.sources.SearchPagingSource
import com.github.hackertube.util.TextUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class SearchResultViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val args = SearchResultFragmentArgs.fromSavedStateHandle(savedStateHandle)

    // parse search URLs from YouTube entered in the search bar
    private val searchQuery = TextUtils.getVideoIdFromUri(args.query.toUri())?.let { videoId ->
        "${ShareDialog.YOUTUBE_FRONTEND_URL}/watch?v=$videoId"
    } ?: args.query

    private val filtersMutableData = MutableStateFlow(HtFilters())

    val searchSuggestion = MutableLiveData<Pair<String, Boolean>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsFlow = filtersMutableData.flatMapLatest { filters ->
        Pager(
            PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                SearchPagingSource(getApplication(), searchQuery, filters)
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    fun setFilters(filters: HtFilters) {
        filtersMutableData.value = filters
    }
}
