package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtPlaylist
import com.github.hackertube.databinding.PlaylistsRowBinding
import com.github.hackertube.helpers.ImageHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.viewholders.PlaylistsViewHolder

class HtPlaylistsAdapter : ListAdapter<HtPlaylist, PlaylistsViewHolder>(
    DiffUtilItemCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PlaylistsViewHolder(PlaylistsRowBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: PlaylistsViewHolder, position: Int) {
        val item = getItem(position) ?: return

        with(holder.binding) {
            playlistTitle.text = item.title
            playlistDescription.text = item.pitch.ifBlank { item.intro }
            videoCount.text = item.videos.size.toString()

            val cover = item.cover.firstOrNull() ?: item.videos.firstOrNull()?.vid
            if (cover != null) {
                ImageHelper.loadImage(HackerTubeRepository.thumbnailUrl(cover), playlistThumbnail)
            }

            root.setOnClickListener {
                NavigationHelper.navigateCuratedPlaylist(root.context, item.slug)
            }
            root.setOnLongClickListener { false }
            bookmarkPlaylist.visibility = android.view.View.GONE
        }
    }
}
