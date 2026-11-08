package com.github.hackertube.repo

import com.github.hackertube.constants.PreferenceKeys
import com.github.hackertube.enums.SyncServerType
import com.github.hackertube.helpers.PreferenceHelper

object UserDataRepositoryHelper {
    private val loggedIn get() = PreferenceHelper.getToken().isNotBlank()
    val syncServerType: SyncServerType
        get() = when (PreferenceHelper.getString(PreferenceKeys.SYNC_SERVER_TYPE, "none")) {
            "piped" -> SyncServerType.PIPED
            "hackertube" -> SyncServerType.LIBRETUBE
            else -> SyncServerType.NONE
        }

    @Deprecated("DO NOT use this directly, use the wrappers from PlaylistHelper and SubscriptionHelper instead!")
    val userDataRepository: UserDataRepository
        get() = when (syncServerType) {
            SyncServerType.PIPED -> PipedUserDataRepository()
            SyncServerType.LIBRETUBE -> HackerTubeSyncServerUserDataRepository()
            else -> LocalUserDataRepository()
        }

    @Deprecated("DO NOT use this directly, use the wrappers from SubscriptionHelper instead!")
    val feedRepository: FeedRepository
        get() = when (syncServerType to loggedIn) {
            SyncServerType.PIPED to true -> PipedAccountFeedRepository()
            else -> LocalFeedRepository()
        }
}