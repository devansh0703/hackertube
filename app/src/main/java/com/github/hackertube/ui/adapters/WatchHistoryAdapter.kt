package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.api.obj.WatchHistoryEntry
import com.github.hackertube.constants.IntentData
import com.github.hackertube.databinding.VideoRowBinding
import com.github.hackertube.db.DatabaseHolder
import com.github.hackertube.db.obj.WatchHistoryItem
import com.github.hackertube.extensions.toID
import com.github.hackertube.extensions.toLocalDate
import com.github.hackertube.helpers.ImageHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.parcelable.PlayerData
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.base.BaseActivity
import com.github.hackertube.ui.extensions.setFormattedDuration
import com.github.hackertube.ui.extensions.setWatchProgressLength
import com.github.hackertube.ui.sheets.VideoOptionsBottomSheet
import com.github.hackertube.ui.viewholders.WatchHistoryViewHolder
import com.github.hackertube.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchHistoryAdapter(
    private val isVideoDownloaded: (String) -> Boolean,
    private val getWatchPosition: (String) -> Long?,
    private val onWatchStatusChanged: (WatchHistoryEntry, Boolean) -> Unit
) :
    ListAdapter<WatchHistoryEntry, WatchHistoryViewHolder>(DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val item =  getItem(position)!!
        val video = item.video

        holder.binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploaderName
            videoInfo.text =
                video.uploaded.toLocalDate().takeIf { !video.isLive }?.let { TextUtils.localizeDate(it) }
            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (video.duration != null) {
                // we pass in 0 for the uploadDate, as a future video cannot be watched already
                thumbnailDuration.setFormattedDuration(video.duration, null, 0)
                thumbnailDurationCard.isVisible = true
            } else {
                thumbnailDurationCard.isGone = true
            }

            if (video.uploaderAvatar != null) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
                channelImageContainer.isVisible = true
            } else {
                channelImageContainer.isGone = true
            }

            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, PlayerData(video.url?.toID()))
            }

            val activity = (root.context as BaseActivity)
            val fragmentManager = activity.supportFragmentManager
            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, result ->
                    val isVideoWatched = result.getBoolean(VideoOptionsBottomSheet.IS_VIDEO_WATCHED)
                    onWatchStatusChanged(item, isVideoWatched)
                    currentList.indexOf(item).takeIf { it >= 0 }?.let(::notifyItemChanged)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video)
                sheet.show(fragmentManager, WatchHistoryAdapter::class.java.name)
                true
            }

            if (video.duration != null) watchProgress.setWatchProgressLength(
                getWatchPosition(video.url!!.toID()),
                video.duration
            ) else watchProgress.isGone = true
            downloadBadge.isVisible = isVideoDownloaded(video.url!!.toID())
        }
    }
}
