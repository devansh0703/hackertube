package com.github.hackertube.repo

import com.github.hackertube.api.RetrofitInstance
import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.helpers.PreferenceHelper

class PipedAccountFeedRepository : FeedRepository {
    override suspend fun getFeed(
        refresh: FeedRefresh,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        val token = PreferenceHelper.getToken()

        return RetrofitInstance.pipedAuthApi.getFeed(token)
    }
}