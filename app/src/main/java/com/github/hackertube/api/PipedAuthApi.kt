package com.github.hackertube.api

import com.github.hackertube.api.obj.DeleteUserRequest
import com.github.hackertube.api.obj.EditPlaylistBody
import com.github.hackertube.api.obj.Login
import com.github.hackertube.api.obj.Message
import com.github.hackertube.api.obj.Playlist
import com.github.hackertube.api.obj.Playlists
import com.github.hackertube.api.obj.StreamItem
import com.github.hackertube.api.obj.Subscribe
import com.github.hackertube.api.obj.Subscribed
import com.github.hackertube.api.obj.Subscription
import com.github.hackertube.api.obj.Token
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PipedAuthApi {

    @POST("login")
    suspend fun login(@Body login: Login): Token

    @POST("register")
    suspend fun register(@Body login: Login): Token

    @POST("user/delete")
    suspend fun deleteAccount(
        @Body password: DeleteUserRequest
    )

    @GET("feed")
    suspend fun getFeed(@Query("authToken") token: String?): List<StreamItem>

    @GET("subscribed")
    suspend fun isSubscribed(
        @Query("channelId") channelId: String,
    ): Subscribed

    @GET("subscriptions")
    suspend fun subscriptions(): List<Subscription>

    @POST("subscribe")
    suspend fun subscribe(
        @Body subscribe: Subscribe
    ): Message

    @POST("unsubscribe")
    suspend fun unsubscribe(
        @Body subscribe: Subscribe
    ): Message

    @POST("import")
    suspend fun importSubscriptions(
        @Query("override") override: Boolean,
        @Body channels: List<String>
    ): Message

    @POST("import/playlist")
    suspend fun clonePlaylist(
        @Body editPlaylistBody: EditPlaylistBody
    ): EditPlaylistBody

    @GET("user/playlists")
    suspend fun getUserPlaylists(): List<Playlists>

    @POST("user/playlists/rename")
    suspend fun renamePlaylist(
        @Body editPlaylistBody: EditPlaylistBody
    ): Message

    @PATCH("user/playlists/description")
    suspend fun changePlaylistDescription(
        @Body editPlaylistBody: EditPlaylistBody
    ): Message

    @POST("user/playlists/delete")
    suspend fun deletePlaylist(
        @Body editPlaylistBody: EditPlaylistBody
    ): Message

    @POST("user/playlists/create")
    suspend fun createPlaylist(
        @Body name: Playlists
    ): EditPlaylistBody

    @POST("user/playlists/add")
    suspend fun addToPlaylist(
        @Body editPlaylistBody: EditPlaylistBody
    ): Message

    @POST("user/playlists/remove")
    suspend fun removeFromPlaylist(
        @Body editPlaylistBody: EditPlaylistBody
    ): Message

    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(@Path("playlistId") playlistId: String): Playlist
}