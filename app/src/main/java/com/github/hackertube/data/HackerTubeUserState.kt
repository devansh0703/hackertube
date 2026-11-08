package com.github.hackertube.data

import android.content.Context

/**
 * Local HackerTube likes and watch-later list, stored on-device like on the website.
 */
object HackerTubeUserState {
    private const val PREFS = "hackertube_user_state"
    private const val LIKED = "liked"
    private const val WATCH_LATER = "watch_later"

    fun liked(context: Context): List<String> = read(context, LIKED)

    fun watchLater(context: Context): List<String> = read(context, WATCH_LATER)

    fun isLiked(context: Context, videoId: String): Boolean = liked(context).contains(videoId)

    fun isWatchLater(context: Context, videoId: String): Boolean =
        watchLater(context).contains(videoId)

    fun toggleLike(context: Context, videoId: String): Boolean =
        toggle(context, LIKED, videoId)

    fun toggleWatchLater(context: Context, videoId: String): Boolean =
        toggle(context, WATCH_LATER, videoId)

    private fun read(context: Context, key: String): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun toggle(context: Context, key: String, videoId: String): Boolean {
        val current = read(context, key).toMutableList()
        val added = if (current.contains(videoId)) {
            current.remove(videoId)
            false
        } else {
            current.add(0, videoId)
            true
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, current.joinToString(","))
            .apply()

        return added
    }
}
