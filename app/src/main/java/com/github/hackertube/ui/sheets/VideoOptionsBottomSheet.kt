package com.github.hackertube.ui.sheets

import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.github.hackertube.R
import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.api.obj.WatchHistoryEntryMetadata
import com.github.hackertube.constants.IntentData
import com.github.hackertube.constants.PreferenceKeys
import com.github.hackertube.data.HackerTubeUserState
import com.github.hackertube.enums.ShareObjectType
import com.github.hackertube.enums.menuoption.VideoBottomSheetItem
import com.github.hackertube.extensions.parcelable
import com.github.hackertube.extensions.toID
import com.github.hackertube.extensions.toastFromMainDispatcher
import com.github.hackertube.helpers.DownloadHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.helpers.PlayerHelper
import com.github.hackertube.helpers.PreferenceHelper
import com.github.hackertube.obj.ShareData
import com.github.hackertube.parcelable.PlayerData
import com.github.hackertube.repo.UserDataRepositoryHelper
import com.github.hackertube.ui.activities.MainActivity
import com.github.hackertube.ui.dialogs.AddToPlaylistDialog
import com.github.hackertube.ui.dialogs.ShareDialog
import com.github.hackertube.ui.fragments.SubscriptionsFragment
import com.github.hackertube.util.PlayingQueue
import com.github.hackertube.util.PlayingQueueMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [streamItem] to load the content from the right video.
 */
class VideoOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var streamItem: StreamItem

    private suspend fun onOptionSelect(
        option: VideoBottomSheetItem,
        videoId: String,
        playlistId: String?
    ) {
        when (option) {
            VideoBottomSheetItem.Like -> {
                val liked = HackerTubeUserState.toggleLike(requireContext(), videoId)
                Toast.makeText(
                    requireContext(),
                    if (liked) R.string.hackertube_added_to_liked
                    else R.string.hackertube_removed_from_liked,
                    Toast.LENGTH_SHORT
                ).show()
            }

            VideoBottomSheetItem.WatchLater -> {
                val saved = HackerTubeUserState.toggleWatchLater(requireContext(), videoId)
                Toast.makeText(
                    requireContext(),
                    if (saved) R.string.hackertube_added_to_watch_later
                    else R.string.hackertube_removed_from_watch_later,
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Start the background mode
            VideoBottomSheetItem.PlayOnBackground -> {
                NavigationHelper.navigateVideo(
                    requireContext(),
                    playerData = PlayerData(
                        videoId = videoId,
                        playlistId = playlistId,
                    ),
                    audioOnlyPlayerRequested = true
                )
            }
            // Add Video to Playlist Dialog
            VideoBottomSheetItem.AddToPlaylist -> {
                AddToPlaylistDialog().apply {
                    arguments = bundleOf(IntentData.videoInfo to streamItem)
                }.show(
                    parentFragmentManager,
                    AddToPlaylistDialog::class.java.name
                )
            }

            VideoBottomSheetItem.Download -> {
                DownloadHelper.startDownloadDialog(
                    requireContext(),
                    parentFragmentManager,
                    videoId
                )
            }

            VideoBottomSheetItem.Share -> {
                val bundle = bundleOf(
                    IntentData.id to videoId,
                    IntentData.shareObjectType to ShareObjectType.VIDEO,
                    IntentData.shareData to ShareData(
                        title = streamItem.title,
                        previewImageUrl = streamItem.thumbnail
                    )
                )
                val newShareDialog = ShareDialog()
                newShareDialog.arguments = bundle
                // using parentFragmentManager is important here
                newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
            }

            VideoBottomSheetItem.PlayNext -> {
                PlayingQueue.addAsNext(streamItem)
            }

            VideoBottomSheetItem.AddToQueue -> {
                PlayingQueue.add(streamItem)
            }

            VideoBottomSheetItem.MarkAsWatched -> {
                withContext(Dispatchers.IO) {
                    if (PlayerHelper.watchHistoryEnabled) {
                        runCatching {
                            UserDataRepositoryHelper.userDataRepository.addToWatchHistory(
                                streamItem.toWatchHistoryEntry(
                                    Long.MAX_VALUE
                                )
                            )
                        }
                    }

                    val watchPosition = WatchHistoryEntryMetadata(
                        videoId = videoId,
                        addedDate = -1,
                        finished = true,
                        positionMillis = Long.MAX_VALUE
                    )
                    UserDataRepositoryHelper.userDataRepository
                        .updateWatchHistoryEntry(watchPosition)
                }
                if (PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)) {
                    // get the host fragment containing the current fragment
                    val navHostFragment = (context as MainActivity).supportFragmentManager
                        .findFragmentById(R.id.fragment) as NavHostFragment?
                    // get the current fragment
                    val fragment = navHostFragment?.childFragmentManager?.fragments
                        ?.firstOrNull() as? SubscriptionsFragment
                    fragment?.removeItem(videoId)
                }
                setFragmentResult(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    bundleOf(IS_VIDEO_WATCHED to true)
                )
            }

            VideoBottomSheetItem.MarkAsUnwatched -> {
                withContext(Dispatchers.IO) {
                    try {
                        UserDataRepositoryHelper.userDataRepository.removeFromWatchHistory(
                            videoId
                        )
                    } catch (e: Exception) {
                        context?.toastFromMainDispatcher(e.message.orEmpty())
                    }
                }
                setFragmentResult(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    bundleOf(IS_VIDEO_WATCHED to false)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        streamItem = arguments?.parcelable(IntentData.streamItem)!!
        val playlistId = arguments?.getString(IntentData.playlistId)

        val videoId = streamItem.url?.toID() ?: return

        setTitle(streamItem.title)

        var optionsList = buildList {
            add(VideoBottomSheetItem.Like)
            add(VideoBottomSheetItem.WatchLater)
            add(VideoBottomSheetItem.AddToPlaylist)
            if (!streamItem.isLive) add(VideoBottomSheetItem.Download)
            add(VideoBottomSheetItem.Share)
        }

        setItems(optionsList) { which ->
            onOptionSelect(optionsList[which], videoId, playlistId)
        }

        // these options are only available for other videos than the currently playing one
        if (PlayingQueue.getCurrent()?.url?.toID() != videoId) {
            getOptionsForNotActivePlayback(videoId) { addedOptions ->
                optionsList = addedOptions + optionsList

                setItems(optionsList) { which ->
                    onOptionSelect(optionsList[which], videoId, playlistId)
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    private fun getOptionsForNotActivePlayback(
        videoId: String,
        onOptionsList: (List<VideoBottomSheetItem>) -> Unit
    ) {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList: MutableList<VideoBottomSheetItem> =
            mutableListOf(VideoBottomSheetItem.PlayOnBackground)

        // Check whether the player is running and add queue options
        if (PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.ONLINE) {
            optionsList += VideoBottomSheetItem.PlayNext
            optionsList += VideoBottomSheetItem.AddToQueue
        }

        if (!PlayerHelper.watchPositionsAny && !PlayerHelper.watchHistoryEnabled) {
            onOptionsList(optionsList)
            return
        }

        // show the mark as watched or unwatched option if watch positions are enabled
        lifecycleScope.launch(Dispatchers.IO) {
            val watchHistoryEntry =
                UserDataRepositoryHelper.userDataRepository.getFromWatchHistory(videoId)

            if (watchHistoryEntry != null) {
                optionsList += VideoBottomSheetItem.MarkAsUnwatched
            }

            if (watchHistoryEntry?.metadata?.finished != true) {
                optionsList += VideoBottomSheetItem.MarkAsWatched
            }

            withContext(Dispatchers.Main) {
                onOptionsList(optionsList)
            }
        }
    }

    companion object {
        const val VIDEO_OPTIONS_SHEET_REQUEST_KEY = "video_options_sheet_request_key"
        const val IS_VIDEO_WATCHED = "isVideoWatched"
    }
}
