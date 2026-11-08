package com.github.hackertube.api.obj

import android.os.Parcelable
import com.github.hackertube.db.DatabaseHelper
import com.github.hackertube.db.obj.LocalPlaylistItem
import com.github.hackertube.db.obj.SubscriptionsFeedItem
import com.github.hackertube.db.obj.WatchHistoryItem
import com.github.hackertube.extensions.toID
import com.github.hackertube.extensions.toLocalDate
import com.github.hackertube.helpers.ProxyHelper
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
@Parcelize
data class StreamItem(
    val url: String? = null,
    val type: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean? = null,
    val uploaded: Long = 0,
    val shortDescription: String? = null,
    val isShort: Boolean = false,
    /**
     * Explicit live state for items where the duration is not known (e.g. the
     * HackerTube catalogue). Falls back to the duration-based guess when null.
     */
    val live: Boolean? = null
) : Parcelable {
    val isLive get() = live ?: (!isShort && ((duration == null) || (duration <= 0L)))
    val isUpcoming get() = uploaded > System.currentTimeMillis()

    fun toLocalPlaylistItem(playlistId: String): LocalPlaylistItem {
        return LocalPlaylistItem(
            playlistId = playlistId.toInt(),
            videoId = url!!.toID(),
            title = title,
            thumbnailUrl = thumbnail?.let { ProxyHelper.unwrapUrl(it) },
            uploader = uploaderName,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar?.let { ProxyHelper.unwrapUrl(it) },
            uploadDate = uploadedDate,
            duration = duration
        )
    }

    fun toFeedItem() = SubscriptionsFeedItem(
        videoId = url!!.toID(),
        title = title,
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaded = uploaded,
        uploaderAvatar = uploaderAvatar,
        uploaderUrl = uploaderUrl,
        duration = duration,
        uploaderVerified = uploaderVerified ?: false,
        shortDescription = shortDescription,
        views = views,
        isShort = isShort
    )
    
    fun toWatchHistoryItem(videoId: String) = WatchHistoryItem(
        videoId = videoId,
        title = title,
        uploadDate = uploaded.toLocalDate(),
        uploader = uploaderName,
        uploaderUrl = uploaderUrl?.toID(),
        uploaderAvatar = uploaderAvatar?.let { ProxyHelper.unwrapUrl(it) },
        thumbnailUrl = thumbnail?.let { ProxyHelper.unwrapUrl(it) },
        duration = duration
    )

    fun toWatchHistoryEntry(positionMillis: Long?) = WatchHistoryEntry(
        metadata = WatchHistoryEntryMetadata(
            videoId = url!!.toID(),
            addedDate = Clock.System.now().toEpochMilliseconds(),
            finished = positionMillis?.let { DatabaseHelper.isVideoWatched(it, duration) } ?: false,
            positionMillis = positionMillis
        ),
        video = this
    )

    companion object {
        const val TYPE_STREAM = "stream"
        const val TYPE_CHANNEL = "channel"
        const val TYPE_PLAYLIST = "playlist"
    }
}
