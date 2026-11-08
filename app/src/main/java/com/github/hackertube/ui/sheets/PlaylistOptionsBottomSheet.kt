package com.github.hackertube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.github.hackertube.R
import com.github.hackertube.api.MediaServiceRepository
import com.github.hackertube.api.PlaylistsHelper
import com.github.hackertube.constants.IntentData
import com.github.hackertube.enums.ImportFormat
import com.github.hackertube.enums.PlaylistType
import com.github.hackertube.enums.ShareObjectType
import com.github.hackertube.enums.menuoption.PlayListBottomSheetItem
import com.github.hackertube.extensions.serializable
import com.github.hackertube.extensions.toID
import com.github.hackertube.extensions.toastFromMainDispatcher
import com.github.hackertube.helpers.BackgroundHelper
import com.github.hackertube.helpers.ContextHelper
import com.github.hackertube.helpers.DownloadHelper
import com.github.hackertube.obj.ShareData
import com.github.hackertube.parcelable.PlayerData

import com.github.hackertube.repo.UserDataRepositoryHelper
import com.github.hackertube.ui.activities.MainActivity
import com.github.hackertube.ui.base.BaseActivity
import com.github.hackertube.ui.dialogs.DeletePlaylistDialog
import com.github.hackertube.ui.dialogs.PlaylistDescriptionDialog
import com.github.hackertube.ui.dialogs.RenamePlaylistDialog
import com.github.hackertube.ui.dialogs.ShareDialog
import com.github.hackertube.ui.preferences.BackupRestoreSettings
import com.github.hackertube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var playlistName: String
    private lateinit var playlistId: String
    private lateinit var playlistType: PlaylistType
    private var playlistThumbnail: String? = null

    private var exportFormat: ImportFormat = ImportFormat.NEWPIPE

    private fun buildOptionsList(isBookmarked: Boolean?): List<PlayListBottomSheetItem> {
        // options for the dialog
        val optionsList = mutableListOf(
            PlayListBottomSheetItem.PlayOnBackground,
            PlayListBottomSheetItem.Download
        )

        if (PlayingQueue.isNotEmpty()) optionsList.add(PlayListBottomSheetItem.AddToQueue)

        if (playlistType == PlaylistType.PUBLIC) {
            optionsList.add(PlayListBottomSheetItem.Share)
            optionsList.add(PlayListBottomSheetItem.Clone)

            // only add the bookmark option to the playlist if public
            if (isBookmarked != null) {
                optionsList.add(
                    if (isBookmarked) PlayListBottomSheetItem.RemoveFromBookmarks else PlayListBottomSheetItem.AddToBookmarks
                )
            }
        } else {
            optionsList.add(PlayListBottomSheetItem.Export)
            optionsList.add(PlayListBottomSheetItem.Rename)
            optionsList.add(PlayListBottomSheetItem.ChangeDescription)
            optionsList.add(PlayListBottomSheetItem.Delete)
        }

        return optionsList
    }

    private suspend fun onOptionSelected(
        playListOption: PlayListBottomSheetItem,
        isBookmarked: Boolean
    ) {
        val mFragmentManager = (context as BaseActivity).supportFragmentManager

        when (playListOption) {
            // play the playlist in the background
            PlayListBottomSheetItem.PlayOnBackground -> {
                val playlist = withContext(Dispatchers.IO) {
                    runCatching { PlaylistsHelper.getPlaylist(playlistId, playlistType) }
                }.getOrElse {
                    context?.toastFromMainDispatcher(R.string.error)
                    return
                }

                playlist.relatedStreams.firstOrNull()?.let {
                    BackgroundHelper.playOnBackground(
                        requireContext(),
                        PlayerData(it.url!!.toID(), playlistId = playlistId)
                    )
                }
            }

            PlayListBottomSheetItem.AddToQueue -> {
                PlayingQueue.insertPlaylist(playlistId, playlistType, null)
            }
            // Clone the playlist to the users Piped account
            PlayListBottomSheetItem.Clone -> {
                val context = requireContext()
                val playlistId = withContext(Dispatchers.IO) {
                    runCatching {
                        PlaylistsHelper.clonePlaylist(playlistId)
                    }.getOrNull()
                }
                context.toastFromMainDispatcher(
                    if (playlistId != null) R.string.playlistCloned else R.string.server_error
                )
            }
            // share the playlist
            PlayListBottomSheetItem.Share -> {
                val newShareDialog = ShareDialog()
                newShareDialog.arguments = bundleOf(
                    IntentData.id to playlistId,
                    IntentData.shareObjectType to ShareObjectType.PLAYLIST,
                    IntentData.shareData to ShareData(
                        title = playlistName,
                        previewImageUrl = playlistThumbnail
                    )
                )
                // using parentFragmentManager, childFragmentManager doesn't work here
                newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
            }

            PlayListBottomSheetItem.Delete -> {
                val newDeletePlaylistDialog = DeletePlaylistDialog()
                newDeletePlaylistDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId
                )
                newDeletePlaylistDialog.show(mFragmentManager, null)
            }

            PlayListBottomSheetItem.Rename -> {
                val newRenamePlaylistDialog = RenamePlaylistDialog()
                newRenamePlaylistDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId,
                    IntentData.playlistName to playlistName
                )
                newRenamePlaylistDialog.show(mFragmentManager, null)
            }

            PlayListBottomSheetItem.ChangeDescription -> {
                val newPlaylistDescriptionDialog = PlaylistDescriptionDialog()
                newPlaylistDescriptionDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId,
                    IntentData.playlistDescription to ""
                )
                newPlaylistDescriptionDialog.show(mFragmentManager, null)
            }

            PlayListBottomSheetItem.Download -> {
                DownloadHelper.startDownloadPlaylistDialog(
                    requireContext(),
                    mFragmentManager,
                    playlistId,
                    playlistName,
                    playlistType
                )
            }

            PlayListBottomSheetItem.Export -> {
                val context = requireContext()

                BackupRestoreSettings.createImportFormatDialog(
                    context,
                    R.string.export_playlist,
                    BackupRestoreSettings.exportPlaylistFormatList + listOf(ImportFormat.URLSORIDS)
                ) { format, includeTimestamp ->
                    exportFormat = format
                    ContextHelper.unwrapActivity<MainActivity>(context)
                        .startPlaylistExport(
                            playlistId,
                            playlistName,
                            exportFormat,
                            includeTimestamp
                        )
                }
            }

            else -> {
                withContext(Dispatchers.IO) {
                    if (isBookmarked) {
                        UserDataRepositoryHelper.userDataRepository.deletePlaylistBookmark(
                            playlistId
                        )
                    } else {
                        val bookmark = runCatching {
                            MediaServiceRepository.instance.getPlaylist(playlistId)
                        }.getOrElse { return@withContext }.toPlaylistBookmark(playlistId)
                        UserDataRepositoryHelper.userDataRepository.createPlaylistBookmark(bookmark)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            playlistName = it.getString(IntentData.playlistName)!!
            playlistId = it.getString(IntentData.playlistId)!!
            playlistType = it.serializable(IntentData.playlistType)!!
            playlistThumbnail = it.getString(IntentData.playlistThumbnail)
        }

        setTitle(playlistName)

        var optionsList = buildOptionsList(null)
        setItems(optionsList) { which ->
            onOptionSelected(optionsList[which], false)
        }

        // we have to update the options list as soon as we know whether the playlist is bookmarked
        lifecycleScope.launch(Dispatchers.IO) {
            val isBookmarked = runCatching {
                UserDataRepositoryHelper.userDataRepository.getPlaylistBookmark(playlistId)
            }.getOrNull() != null

            withContext(Dispatchers.Main) {
                optionsList = buildOptionsList(isBookmarked)

                setItems(optionsList) { which ->
                    onOptionSelected(optionsList[which], isBookmarked)
                }
            }
        }
    }

    companion object {
        const val PLAYLIST_OPTIONS_REQUEST_KEY = "playlist_options_request_key"
    }
}
