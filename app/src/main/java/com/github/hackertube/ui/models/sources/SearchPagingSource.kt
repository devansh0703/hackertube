package com.github.hackertube.ui.models.sources

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.hackertube.api.obj.ContentItem
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtFilters
import com.github.hackertube.data.toContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches only the bundled HackerTube catalogue.
 */
class SearchPagingSource(
    private val context: Context,
    private val searchQuery: String,
    private val filters: HtFilters = HtFilters()
) : PagingSource<String, ContentItem>() {
    override fun getRefreshKey(state: PagingState<String, ContentItem>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ContentItem> {
        return try {
            val offset = params.key?.toIntOrNull() ?: 0
            val results = withContext(Dispatchers.IO) {
                val catalog = HackerTubeRepository.catalog(context)
                HackerTubeRepository.searchHits(
                    catalog,
                    searchQuery,
                    offset,
                    filters = filters
                ).map { it.toContentItem() }
            }

            LoadResult.Page(
                data = results,
                prevKey = null,
                nextKey = if (results.size == HackerTubeRepository.SEARCH_PAGE_SIZE) {
                    (offset + HackerTubeRepository.SEARCH_PAGE_SIZE).toString()
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
