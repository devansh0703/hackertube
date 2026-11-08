package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.data.HackerTubeRepository
import com.github.hackertube.data.HtCollection
import com.github.hackertube.databinding.CarouselPlaylistThumbnailBinding
import com.github.hackertube.helpers.ImageHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.viewholders.CarouselPlaylistViewHolder

class CarouselCategoryAdapter : ListAdapter<HtCollection, CarouselPlaylistViewHolder>(
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
            playlistName.text = item.category.name
            ImageHelper.loadImage(
                HackerTubeRepository.thumbnailUrl(item.topVideo.vid),
                thumbnail
            )

            root.setOnClickListener {
                NavigationHelper.navigateCategory(
                    root.context,
                    item.category.id,
                    item.category.name
                )
            }
            root.setOnLongClickListener { false }
        }
    }
}
