package com.github.hackertube.ui.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.constants.PreferenceKeys
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtCategoryInfo
import com.github.hackertube.data.HtCollection
import com.github.hackertube.data.HtPlaylist
import com.github.hackertube.data.toStreamItem
import com.github.hackertube.db.DatabaseHelper
import com.github.hackertube.enums.WatchHistoryStatus
import com.github.hackertube.extensions.runSafely
import com.github.hackertube.extensions.updateIfChanged
import com.github.hackertube.helpers.PlayerHelper
import com.github.hackertube.helpers.PreferenceHelper
import com.github.hackertube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    data class Highlights(
        val category: HtCategoryInfo,
        val videos: List<StreamItem>
    )

    val topPicks: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val highlights: MutableLiveData<Highlights> = MutableLiveData(null)
    val collections: MutableLiveData<List<HtCollection>> = MutableLiveData(null)
    val playlists: MutableLiveData<List<HtPlaylist>> = MutableLiveData(null)
    val continueWatching: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val loadedSuccessfully: MutableLiveData<Boolean> = MutableLiveData(false)

    private val sections: List<LiveData<*>> = listOf(
        topPicks,
        highlights,
        collections,
        playlists,
        continueWatching
    )

    private var loadHomeJob: Job? = null

    fun loadHomeFeed(context: Context, visibleItems: Set<String>) {
        isLoading.value = true

        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            awaitAll(
                async { if (visibleItems.contains(TOP_PICKS)) loadTopPicks(context) },
                async { if (visibleItems.contains(TOPIC)) loadHighlights(context) },
                async { if (visibleItems.contains(COLLECTIONS)) loadCollections(context) },
                async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists(context) },
                async { if (visibleItems.contains(WATCHING)) loadVideosToContinueWatching() }
            )
            loadedSuccessfully.value = sections.any { it.value != null }
            isLoading.value = false
        }
    }

    fun selectCategory(context: Context, categoryId: String) {
        PreferenceHelper.putString(PreferenceKeys.HACKERTUBE_CATEGORY, categoryId)
        viewModelScope.launch {
            loadHighlights(context, categoryId)
        }
    }

    private suspend fun loadTopPicks(context: Context) {
        runSafely(
            onSuccess = { videos -> topPicks.updateIfChanged(videos) },
            ioBlock = {
                val catalog = HackerTubeRepository.catalog(context)
                catalog.videos
                    .asSequence()
                    .filter { it.shelfLife != NEWS_SHELF_LIFE }
                    .take(TOP_PICKS_COUNT)
                    .map { it.toStreamItem() }
                    .toList()
            }
        )
    }

    private suspend fun loadHighlights(
        context: Context,
        requestedCategoryId: String? = null
    ) {
        val catalog = HackerTubeRepository.catalog(context)

        val available = catalog.categories.filter { category ->
            catalog.videos.any { video -> video.categories.any { it.id == category.id } }
        }
        if (available.isEmpty()) return

        val selectedId = requestedCategoryId
            ?: PreferenceHelper.getString(PreferenceKeys.HACKERTUBE_CATEGORY, "")
        val category = available.firstOrNull { it.id == selectedId }
            ?: available.first()

        val videos = HackerTubeRepository
            .videosInCategory(catalog, category.id)
            .take(HIGHLIGHTS_COUNT)
            .map { it.toStreamItem() }

        highlights.updateIfChanged(Highlights(category, videos))
    }

    private suspend fun loadCollections(context: Context) {
        val catalog = HackerTubeRepository.catalog(context)
        val items = HackerTubeRepository.collections(catalog)
        if (items.isNotEmpty()) {
            this.collections.updateIfChanged(items)
        }
    }

    private suspend fun loadPlaylists(context: Context) {
        runSafely(
            onSuccess = { newPlaylists -> playlists.updateIfChanged(newPlaylists) },
            ioBlock = {
                val catalog = HackerTubeRepository.catalog(context)
                HackerTubeRepository.curatedPlaylists(catalog, HOME_PLAYLIST_COUNT)
            }
        )
    }

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return
        runSafely(
            onSuccess = { videos -> continueWatching.updateIfChanged(videos) },
            ioBlock = ::loadWatchingFromDB
        )
    }

    private suspend fun loadWatchingFromDB(): List<StreamItem> {
        val (videos, _) = runCatching {
            UserDataRepositoryHelper.userDataRepository.getWatchHistory(
                20, null,
                WatchHistoryStatus.ALL
            )
        }.getOrElse { Pair(emptyList(), null) }

        return DatabaseHelper
            .filterUnwatched(videos.map { it.video })
    }

    companion object {
        private const val TOP_PICKS_COUNT = 20
        private const val HIGHLIGHTS_COUNT = 10
        private const val HOME_PLAYLIST_COUNT = 12
        private const val NEWS_SHELF_LIFE = "news"

        private const val TOP_PICKS = "featured"
        private const val WATCHING = "watching"
        private const val TOPIC = "trending"
        private const val COLLECTIONS = "bookmarks"
        private const val PLAYLISTS = "playlists"
    }
}
