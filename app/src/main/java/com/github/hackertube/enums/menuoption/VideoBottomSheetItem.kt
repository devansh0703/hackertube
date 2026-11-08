package com.github.hackertube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.hackertube.R
import com.github.hackertube.obj.bottomsheetitem.BottomSheetItem
import com.github.hackertube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class VideoBottomSheetItem(
    @StringRes titleStringRes: Int,
    @DrawableRes drawableRes: Int,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {
    data object PlayOnBackground : VideoBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        drawableRes = R.drawable.ic_headphones,
    )

    data object PlayNext : VideoBottomSheetItem(
        titleStringRes = R.string.play_next,
        drawableRes = R.drawable.ic_playlist,
    )

    data object AddToQueue : VideoBottomSheetItem(
        titleStringRes = R.string.add_to_queue,
        drawableRes = R.drawable.ic_queue,
    )

    data object MarkAsUnwatched : VideoBottomSheetItem(
        titleStringRes = R.string.mark_as_unwatched,
        drawableRes = R.drawable.ic_eye_crossed_out,
    )

    data object MarkAsWatched : VideoBottomSheetItem(
        titleStringRes = R.string.mark_as_watched,
        drawableRes = R.drawable.ic_eye,
    )

    data object Like : VideoBottomSheetItem(
        titleStringRes = R.string.hackertube_like,
        drawableRes = R.drawable.ic_like,
    )

    data object WatchLater : VideoBottomSheetItem(
        titleStringRes = R.string.hackertube_watch_later,
        drawableRes = R.drawable.ic_time_outlined,
    )

    data object AddToPlaylist : VideoBottomSheetItem(
        titleStringRes = R.string.addToPlaylist,
        drawableRes = R.drawable.ic_playlist_add,
    )

    data object Download : VideoBottomSheetItem(
        titleStringRes = R.string.download,
        drawableRes = R.drawable.ic_download,
    )

    data object Share : VideoBottomSheetItem(
        titleStringRes = R.string.share,
        drawableRes = R.drawable.ic_share,
    )
}