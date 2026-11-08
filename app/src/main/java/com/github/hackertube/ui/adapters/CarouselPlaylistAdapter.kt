package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.constants.IntentData
import com.github.hackertube.databinding.CarouselPlaylistThumbnailBinding
import com.github.hackertube.enums.PlaylistType
import com.github.hackertube.helpers.ImageHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.base.BaseActivity
import com.github.hackertube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.hackertube.ui.viewholders.CarouselPlaylistViewHolder

data class CarouselPlaylist(
    val id: String,
    val title: String?,
    val thumbnail: String?
)

class CarouselPlaylistAdapter(private val playlistType: PlaylistType) : ListAdapter<CarouselPlaylist, CarouselPlaylistViewHolder>(
    DiffUtilItemCallback()
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarouselPlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return CarouselPlaylistViewHolder(CarouselPlaylistThumbnailBinding.inflate(layoutInflater))
    }

    override fun onBindViewHolder(
        holder: CarouselPlaylistViewHolder,
        position: Int
    ) {
        val item = getItem(position)!!

        with(holder.binding) {
            playlistName.text = item.title
            ImageHelper.loadImage(item.thumbnail, thumbnail)

            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, item.id, playlistType)
            }

            root.setOnLongClickListener {
                val playlistOptionsDialog = PlaylistOptionsBottomSheet()
                playlistOptionsDialog.arguments = bundleOf(
                    IntentData.playlistId to item.id,
                    IntentData.playlistName to item.title,
                    IntentData.playlistType to playlistType,
                    IntentData.playlistThumbnail to item.thumbnail,
                )
                playlistOptionsDialog.show((root.context as BaseActivity).supportFragmentManager)

                true
            }
        }
    }
}