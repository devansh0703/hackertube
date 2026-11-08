package com.github.hackertube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.hackertube.R
import com.github.hackertube.obj.bottomsheetitem.BottomSheetItem
import com.github.hackertube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class ChannelBottomSheetItem(
    @StringRes titleStringRes: Int,
    @DrawableRes drawableRes: Int?,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {
    data object Share : ChannelBottomSheetItem(
        titleStringRes = R.string.share,
        drawableRes = R.drawable.ic_share,
    )

    data object PlayLatestVideos : ChannelBottomSheetItem(
        titleStringRes = R.string.play_latest_videos,
        drawableRes = R.drawable.ic_playlist,
    )

    data object PlayOnBackground : ChannelBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        drawableRes = R.drawable.ic_headphones,
    )

    data object AddToGroup : ChannelBottomSheetItem(
        titleStringRes = R.string.add_to_group,
        drawableRes = R.drawable.ic_add_to_bookmark,
    )
}