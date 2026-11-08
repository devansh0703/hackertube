package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtPlaylist
import com.github.hackertube.databinding.CarouselPlaylistThumbnailBinding
import com.github.hackertube.helpers.ImageHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.viewholders.CarouselPlaylistViewHolder

class CarouselHtPlaylistAdapter : ListAdapter<HtPlaylist, CarouselPlaylistViewHolder>(
    DiffUtilItemCallback()
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarouselPlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return CarouselPlaylistViewHolder(CarouselPlaylistThumbnailBinding.inflate(layoutInflater))
    }

    override fun onBindViewHolder(holder: CarouselPlaylistViewHolder, position: Int) {
        val item = getItem(position) ?: return

        with(holder.binding) {
            playlistName.text = item.title
            val videoId = item.cover.firstOrNull() ?: item.videos.firstOrNull()?.vid
            if (videoId != null) {
                ImageHelper.loadImage(HackerTubeRepository.thumbnailUrl(videoId), thumbnail)
            }

            root.setOnClickListener {
                NavigationHelper.navigateCuratedPlaylist(root.context, item.slug)
            }
            root.setOnLongClickListener { false }
        }
    }
}
