package com.github.hackertube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PlaylistResponse(
    @SerialName(value = "playlist")
    val playlist: Playlist,

    @SerialName(value = "videos")
    val videos: List<CreateVideo>
) {
    fun toPipedPlaylist(): com.github.hackertube.api.obj.Playlist = com.github.hackertube.api.obj.Playlist(
        name = playlist.title,
        description = playlist.description,
        thumbnailUrl = playlist.thumbnailUrl,
        videos = playlist.videoCount.toInt(),
        relatedStreams = videos.map { it.toStreamItem() }
    )
}

