package com.github.hackertube.repo

import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.db.obj.SubscriptionsFeedItem

data class FeedProgress(
    val currentProgress: Int,
    val total: Int
)

sealed class FeedRefresh {
    object Automatically: FeedRefresh()
    object All: FeedRefresh()
    class OnlySome(val channelIds: List<String>): FeedRefresh()
}

interface FeedRepository {
    /**
     * Loads the user's video feed based on the subscribed channels.
     * Depending on [refresh], the feed is refetched from YouTube.
     */
    suspend fun getFeed(
        refresh: FeedRefresh,
        onProgressUpdate: (FeedProgress) -> Unit = {}
    ): List<StreamItem>
    suspend fun removeChannel(channelId: String) {}
    suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) {}
}