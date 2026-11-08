package com.github.hackertube.ui.sheets

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.github.hackertube.api.MediaServiceRepository
import com.github.hackertube.constants.IntentData
import com.github.hackertube.enums.ShareObjectType
import com.github.hackertube.enums.menuoption.ChannelBottomSheetItem
import com.github.hackertube.extensions.TAG
import com.github.hackertube.extensions.toID
import com.github.hackertube.helpers.BackgroundHelper
import com.github.hackertube.helpers.NavigationHelper
import com.github.hackertube.obj.ShareData
import com.github.hackertube.parcelable.PlayerData
import com.github.hackertube.ui.dialogs.ShareDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [channelId] to load the content from the right video.
 */
class ChannelOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var channelId: String
    private var channelName: String? = null
    private var subscribed: Boolean = false
    private var channelAvatar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelId = arguments?.getString(IntentData.channelId)!!
        channelName = arguments?.getString(IntentData.channelName)
        channelAvatar = arguments?.getString(IntentData.channelAvatar)
        subscribed = arguments?.getBoolean(IntentData.isSubscribed, false) ?: false

        setTitle(channelName)

        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            ChannelBottomSheetItem.Share,
            ChannelBottomSheetItem.PlayLatestVideos,
            ChannelBottomSheetItem.PlayOnBackground
        )
        if (subscribed) optionsList.add(ChannelBottomSheetItem.AddToGroup)

        setItems(optionsList) { which ->
            when (optionsList[which]) {
                ChannelBottomSheetItem.Share -> {
                    val bundle = bundleOf(
                        IntentData.id to channelId,
                        IntentData.shareObjectType to ShareObjectType.CHANNEL,
                        IntentData.shareData to ShareData(
                            title = channelName,
                            previewImageUrl = channelAvatar
                        )
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                }

                ChannelBottomSheetItem.AddToGroup -> {
                    val sheet = AddChannelToGroupSheet().apply {
                        arguments = bundleOf(IntentData.channelId to channelId)
                    }
                    sheet.show(parentFragmentManager, null)
                }

                ChannelBottomSheetItem.PlayLatestVideos -> {
                    try {
                        val channel = withContext(Dispatchers.IO) {
                            MediaServiceRepository.instance.getChannel(channelId)
                        }
                        channel.relatedStreams.firstOrNull()?.url?.toID()?.let {
                            NavigationHelper.navigateVideo(
                                requireContext(),
                                PlayerData(
                                    it,
                                    channelId = channelId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG(), e.toString())
                    }
                }

                ChannelBottomSheetItem.PlayOnBackground -> {
                    try {
                        val channel = withContext(Dispatchers.IO) {
                            MediaServiceRepository.instance.getChannel(channelId)
                        }
                        channel.relatedStreams.firstOrNull()?.url?.toID()?.let {
                            BackgroundHelper.playOnBackground(
                                requireContext(),
                                PlayerData(
                                    videoId = it,
                                    channelId = channelId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG(), e.toString())
                    }
                }
            }
        }
    }
}
