package com.github.hackertube.api

import com.github.hackertube.api.obj.Subscription
import com.github.hackertube.db.obj.SubscriptionsFeedItem
import com.github.hackertube.repo.FeedProgress
import com.github.hackertube.repo.FeedRefresh
import com.github.hackertube.repo.FeedRepository
import com.github.hackertube.repo.UserDataRepository
import com.github.hackertube.repo.UserDataRepositoryHelper

object SubscriptionHelper {
    @Suppress("DEPRECATION")
    private val userDataRepository: UserDataRepository
        get() = UserDataRepositoryHelper.userDataRepository
    @Suppress("DEPRECATION")
    private val feedRepository: FeedRepository
        get() = UserDataRepositoryHelper.feedRepository

    suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) = userDataRepository.subscribe(channelId, name, uploaderAvatar, verified)

    suspend fun unsubscribe(channelId: String) {
        userDataRepository.unsubscribe(channelId)
        // remove videos from (local) feed
        feedRepository.removeChannel(channelId)
    }
    suspend fun isSubscribed(channelId: String) = userDataRepository.isSubscribed(channelId)
    suspend fun importSubscriptions(newChannels: List<String>) =
        userDataRepository.importSubscriptions(newChannels)

    suspend fun getSubscriptions() =
        userDataRepository.getSubscriptions().sortedBy { it.name.lowercase() }

    suspend fun getSubscriptionChannelIds() = userDataRepository.getSubscriptionChannelIds()
    suspend fun getFeed(refresh: FeedRefresh, onProgressUpdate: (FeedProgress) -> Unit = {}) =
        feedRepository.getFeed(refresh, onProgressUpdate)

    suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) =
        feedRepository.submitFeedItemChange(feedItem)

    suspend fun submitSubscriptionChannelInfosChanged(subscriptions: List<Subscription>) =
        userDataRepository.submitSubscriptionChannelInfosChanged(subscriptions)
}
