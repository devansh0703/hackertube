package com.github.hackertube.data

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.github.hackertube.api.obj.ContentItem
import com.github.hackertube.api.obj.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * One category entry as stored on a video record.
 */
@Serializable
data class HtVideoCategory(
    val id: String,
    val name: String,
    val area: String = "",
    @SerialName("area_name") val areaName: String = ""
)

@Serializable
data class HtHn(
    val mentions: Int = 0,
    val recommenders: Int = 0,
    @SerialName("rec_months") val recMonths: Int = 0,
    val strong: Int = 0,
    val first: String? = null,
    val last: String? = null,
    @SerialName("last_rec") val lastRec: String? = null
)

/**
 * A single video from HackerTube's enriched catalogue.
 */
@Serializable
data class HtVideo(
    val vid: String,
    val title: String = "",
    val channel: String = "",
    val summary: String = "",
    @SerialName("why_hn") val whyHn: String = "",
    @SerialName("content_type") val contentType: String = "",
    val level: String = "",
    @SerialName("shelf_life") val shelfLife: String = "",
    @SerialName("primary_topic") val primaryTopic: String = "",
    val tags: List<String> = emptyList(),
    val categories: List<HtVideoCategory> = emptyList(),
    val quote: HtQuote? = null,
    val hn: HtHn = HtHn(),
    val score: Double = 0.0,
    val published: String = "",
    @SerialName("duration_s") val durationSeconds: Long = 0,
    val views: Long = 0,
    val likes: Long = 0,
    val thumbnail: String = "",
    @SerialName("last_rec") val lastRec: String = "",
    val gem: Boolean = false
)

@Serializable
data class HtQuote(
    val text: String = "",
    @SerialName("hn_comment_id") val hnCommentId: Long? = null,
    val year: Int? = null
)

@Serializable
data class HtCategoryInfo(
    val id: String,
    val name: String,
    val description: String = ""
)

@Serializable
data class HtArea(
    val id: String,
    val name: String,
    val categories: List<HtCategoryInfo> = emptyList()
)

@Serializable
data class HtTaxonomy(
    val areas: List<HtArea> = emptyList()
)

@Serializable
data class HtRelatedRef(
    val vid: String,
    @SerialName("shared_recommenders") val sharedRecommenders: Int = 0
)

@Serializable
data class HtRelated(
    val vid: String,
    val related: List<HtRelatedRef> = emptyList()
)

@Serializable
data class HtPlaylistVideo(
    val vid: String,
    val note: String = ""
)

@Serializable
data class HtPlaylist(
    val slug: String,
    val title: String = "",
    val tier: String = "",
    val kind: String = "",
    val pitch: String = "",
    val intro: String = "",
    val minutes: Int = 0,
    val cover: List<String> = emptyList(),
    val videos: List<HtPlaylistVideo> = emptyList()
)

@Serializable
data class HtPlaylistsFile(
    val playlists: List<HtPlaylist> = emptyList()
)

data class HtCatalog(
    val videos: List<HtVideo>,
    val byId: Map<String, HtVideo>,
    val areas: List<HtArea>,
    val categories: List<HtCategoryInfo>,
    val playlists: List<HtPlaylist>,
    val playlistById: Map<String, HtPlaylist>,
    val related: Map<String, List<String>>,
    val searchText: Map<String, String>,
    val categoryCounts: Map<String, Int>,
    val areaCounts: Map<String, Int>
)

/**
 * A taxonomy category together with the number of videos it contains and a video
 * that can be used as a preview thumbnail.
 */
data class HtCollection(
    val category: HtCategoryInfo,
    val videoCount: Int,
    val topVideo: HtVideo
)

/**
 * A subject hit from search: either a topic area or one of its categories.
 */
data class HtSubject(
    val id: String,
    val name: String,
    val description: String,
    val areaId: String,
    val areaName: String,
    val videoCount: Int,
    val topVideoId: String
)

sealed class HtSearchHit {
    data class Video(val video: HtVideo) : HtSearchHit()
    data class Playlist(val playlist: HtPlaylist) : HtSearchHit()
    data class Subject(val subject: HtSubject) : HtSearchHit()
}

/**
 * The exact filters exposed on hackertube.org. Empty values mean "no filter" and
 * the default order is "Best first" (the catalogue's recommendation score order).
 */
data class HtFilters(
    val date: String = "",
    val type: String = "",
    val duration: String = "",
    val gems: Boolean = false,
    val evergreen: Boolean = false,
    val sort: String = ""
) {
    val isActive: Boolean
        get() = hasContentFilters || sort.isNotBlank()

    val hasContentFilters: Boolean
        get() = date.isNotBlank() || type.isNotBlank() || duration.isNotBlank() ||
            gems || evergreen

    fun toBundle(): Bundle = bundleOf(
        KEY_DATE to date,
        KEY_TYPE to type,
        KEY_DURATION to duration,
        KEY_GEMS to gems,
        KEY_EVERGREEN to evergreen,
        KEY_SORT to sort
    )

    companion object {
        const val KEY_DATE = "date"
        const val KEY_TYPE = "type"
        const val KEY_DURATION = "dur"
        const val KEY_GEMS = "gems"
        const val KEY_EVERGREEN = "evergreen"
        const val KEY_SORT = "sort"

        fun fromBundle(bundle: Bundle?): HtFilters = if (bundle == null) {
            HtFilters()
        } else {
            HtFilters(
                date = bundle.getString(KEY_DATE).orEmpty(),
                type = bundle.getString(KEY_TYPE).orEmpty(),
                duration = bundle.getString(KEY_DURATION).orEmpty(),
                gems = bundle.getBoolean(KEY_GEMS, false),
                evergreen = bundle.getBoolean(KEY_EVERGREEN, false),
                sort = bundle.getString(KEY_SORT).orEmpty()
            )
        }
    }
}

data class HtFilterOption(val value: String, val label: String)

data class HtFilterGroup(
    val key: String,
    val title: String,
    val options: List<HtFilterOption>,
    val multiSelect: Boolean = false
)

object HtFilterOptions {
    const val GROUP_DATE = "date"
    const val GROUP_TYPE = "type"
    const val GROUP_DURATION = "duration"
    const val GROUP_FEATURES = "features"
    const val GROUP_SORT = "sort"

    val groups = listOf(
        HtFilterGroup(
            key = GROUP_DATE,
            title = "Upload date",
            options = listOf(
                HtFilterOption("year", "Past year"),
                HtFilterOption("2020s", "2020s"),
                HtFilterOption("2010s", "2010s"),
                HtFilterOption("2000s", "2000s")
            )
        ),
        HtFilterGroup(
            key = GROUP_TYPE,
            title = "Type",
            options = listOf(
                HtFilterOption("talk", "Talk"),
                HtFilterOption("lecture", "Lecture"),
                HtFilterOption("explainer", "Explainer"),
                HtFilterOption("tutorial", "Tutorial"),
                HtFilterOption("documentary", "Documentary"),
                HtFilterOption("interview", "Interview"),
                HtFilterOption("demo", "Demo"),
                HtFilterOption("commentary", "Commentary"),
                HtFilterOption("comedy", "Comedy"),
                HtFilterOption("music", "Music")
            )
        ),
        HtFilterGroup(
            key = GROUP_DURATION,
            title = "Duration",
            options = listOf(
                HtFilterOption("short", "Under 4 minutes"),
                HtFilterOption("medium", "4–20 minutes"),
                HtFilterOption("long", "20–60 minutes"),
                HtFilterOption("xlong", "Over 1 hour")
            )
        ),
        HtFilterGroup(
            key = GROUP_FEATURES,
            title = "Features",
            options = listOf(
                HtFilterOption("gems", "Hidden gems"),
                HtFilterOption("evergreen", "Evergreen only")
            ),
            multiSelect = true
        ),
        HtFilterGroup(
            key = GROUP_SORT,
            title = "Sort by",
            options = listOf(
                HtFilterOption("", "Best first"),
                HtFilterOption("recent", "Recently recommended"),
                HtFilterOption("date", "Upload date"),
                HtFilterOption("old", "Oldest first"),
                HtFilterOption("views", "View count"),
                HtFilterOption("liked", "Like ratio"),
                HtFilterOption("hn", "Most recommended")
            )
        )
    )
}

/**
 * Loads the open HackerTube catalogue bundled in `assets/hackertube` and exposes it
 * to the UI. The export is published on hackertube.org/data under ODC-By 1.0.
 */
object HackerTubeRepository {
    private const val VIDEOS_ASSET = "hackertube/videos.jsonl"
    private const val TAXONOMY_ASSET = "hackertube/taxonomy.json"
    private const val RELATED_ASSET = "hackertube/related.jsonl"
    private const val PLAYLISTS_ASSET = "hackertube/playlists.json"

    const val SEARCH_PAGE_SIZE = 20
    const val PLAYLIST_URL_PREFIX = "hackertube-playlist:"
    const val CATEGORY_URL_PREFIX = "hackertube-category:"
    const val SEARCH_TYPE_SUBJECT = "subject"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val mutex = Mutex()

    @Volatile
    private var cached: HtCatalog? = null

    suspend fun catalog(context: Context): HtCatalog {
        cached?.let { return it }

        return mutex.withLock {
            cached ?: load(context.applicationContext).also { cached = it }
        }
    }

    private suspend fun load(context: Context): HtCatalog = withContext(Dispatchers.IO) {
        val assets = context.assets

        val videos = assets.open(VIDEOS_ASSET).bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                line.takeIf { it.isNotBlank() }?.let {
                    runCatching { json.decodeFromString<HtVideo>(it) }.getOrNull()
                }
            }.toList()
        }.sortedByDescending { it.score }

        val areas = runCatching {
            assets.open(TAXONOMY_ASSET).bufferedReader().use { reader ->
                json.decodeFromString<HtTaxonomy>(reader.readText()).areas
            }
        }.getOrDefault(emptyList())

        val related = runCatching {
            assets.open(RELATED_ASSET).bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    line.takeIf { it.isNotBlank() }?.let {
                        runCatching { json.decodeFromString<HtRelated>(it) }.getOrNull()
                    }
                }.associate { it.vid to it.related.map(HtRelatedRef::vid) }
            }
        }.getOrDefault(emptyMap())

        val playlists = runCatching {
            assets.open(PLAYLISTS_ASSET).bufferedReader().use { reader ->
                json.decodeFromString<HtPlaylistsFile>(reader.readText()).playlists
            }
        }.getOrDefault(emptyList())

        val byId = videos.associateBy { it.vid }

        val categoryCounts = HashMap<String, Int>()
        val areaCounts = HashMap<String, Int>()
        videos.forEach { video ->
            video.categories.forEach { category ->
                categoryCounts[category.id] = (categoryCounts[category.id] ?: 0) + 1
                if (category.area.isNotBlank()) {
                    areaCounts[category.area] = (areaCounts[category.area] ?: 0) + 1
                }
            }
        }

        HtCatalog(
            videos = videos,
            byId = byId,
            areas = areas,
            categories = areas.flatMap { it.categories },
            playlists = playlists.filter { playlist ->
                playlist.videos.any { byId.containsKey(it.vid) }
            },
            playlistById = playlists.associateBy { it.slug },
            related = related.filterKeys { byId.containsKey(it) },
            searchText = videos.associate { video ->
                video.vid to buildString {
                    append(video.title).append(' ')
                    append(video.channel).append(' ')
                    append(video.primaryTopic).append(' ')
                    append(video.summary).append(' ')
                    append(video.whyHn).append(' ')
                    append(video.contentType).append(' ')
                    video.tags.forEach { append(it).append(' ') }
                    video.categories.forEach {
                        append(it.name).append(' ')
                        append(it.areaName).append(' ')
                    }
                }.lowercase()
            },
            categoryCounts = categoryCounts,
            areaCounts = areaCounts
        )
    }

    fun thumbnailUrl(videoId: String) = "https://i.ytimg.com/vi/$videoId/hq2.jpg"

    fun videosInCategory(
        catalog: HtCatalog,
        categoryId: String,
        filters: HtFilters = HtFilters()
    ): List<HtVideo> = filteredVideos(
        catalog,
        catalog.videos.filter { video -> video.categories.any { it.id == categoryId } },
        filters
    )

    fun videosInArea(
        catalog: HtCatalog,
        areaId: String,
        filters: HtFilters = HtFilters()
    ): List<HtVideo> = filteredVideos(
        catalog,
        catalog.videos.filter { video -> video.categories.any { it.area == areaId } },
        filters
    )

    fun longs(catalog: HtCatalog, filters: HtFilters = HtFilters()): List<HtVideo> =
        filteredVideos(
            catalog,
            catalog.videos,
            filters.copy(duration = filters.duration.ifBlank { "xlong" })
        )

    fun filteredVideos(
        catalog: HtCatalog,
        source: List<HtVideo>,
        filters: HtFilters
    ): List<HtVideo> {
        val filtered = source.filter { video ->
            when (filters.duration) {
                "short" -> video.durationSeconds in 1 until SECONDS_4_MIN
                "medium" -> video.durationSeconds in SECONDS_4_MIN..SECONDS_20_MIN
                "long" -> video.durationSeconds in SECONDS_20_MIN..SECONDS_PER_HOUR
                "xlong" -> video.durationSeconds > SECONDS_PER_HOUR
                else -> true
            } && (filters.type.isBlank() || video.contentType == filters.type) &&
                (!filters.gems || video.gem) &&
                (!filters.evergreen || video.shelfLife == "evergreen") &&
                matchesDate(video.published, filters.date)
        }

        return when (filters.sort) {
            "recent" -> filtered.sortedWith(
                compareByDescending<HtVideo> { it.lastRec }.thenByDescending { it.score }
            )

            "date" -> filtered.sortedByDescending { it.published }
            "old" -> filtered.sortedBy { it.published }
            "views" -> filtered.sortedByDescending { it.views }
            "liked" -> filtered.sortedByDescending {
                if (it.views <= 0) 0.0 else it.likes.toDouble() / it.views
            }

            "hn" -> filtered.sortedByDescending { it.score }
            else -> filtered
        }
    }

    private fun matchesDate(published: String, date: String): Boolean {
        if (date.isBlank()) return true
        if (published.isBlank()) return false

        val localDate = runCatching {
            Instant.parse(published).atZone(ZoneOffset.UTC).toLocalDate()
        }.getOrNull() ?: return false

        return when (date) {
            "year" -> !localDate.isBefore(LocalDate.now(ZoneOffset.UTC).minusYears(1))
            "2020s" -> localDate.year in 2020..2029
            "2010s" -> localDate.year in 2010..2019
            "2000s" -> localDate.year in 2000..2009
            else -> true
        }
    }

    /**
     * Category carousels for the home screen, most populated categories first.
     */
    fun collections(catalog: HtCatalog, limit: Int = 12): List<HtCollection> =
        catalog.categories.mapNotNull { category ->
            val videos = videosInCategory(catalog, category.id)
            if (videos.isEmpty()) null else HtCollection(
                category = category,
                videoCount = videos.size,
                topVideo = videos.first()
            )
        }.sortedByDescending { it.videoCount }.take(limit)

    fun areasWithVideos(catalog: HtCatalog): List<HtArea> =
        catalog.areas.filter { area -> (catalog.areaCounts[area.id] ?: 0) > 0 }

    fun curatedPlaylists(catalog: HtCatalog, limit: Int = Int.MAX_VALUE): List<HtPlaylist> =
        catalog.playlists
            .sortedWith(compareBy<HtPlaylist> { if (it.tier == "start") 0 else 1 })
            .take(limit)

    fun playlistVideos(catalog: HtCatalog, playlist: HtPlaylist): List<HtVideo> =
        playlist.videos.mapNotNull { catalog.byId[it.vid] }

    fun playlistNote(playlist: HtPlaylist, videoId: String): String =
        playlist.videos.firstOrNull { it.vid == videoId }?.note.orEmpty()

    fun searchVideos(
        catalog: HtCatalog,
        query: String,
        source: List<HtVideo> = catalog.videos
    ): List<HtVideo> {
        val tokens = query.lowercase().trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return source

        return source.mapNotNull { video ->
            val haystack = catalog.searchText[video.vid].orEmpty()
            var relevance = 0.0

            for (token in tokens) {
                if (!haystack.contains(token)) return@mapNotNull null

                relevance += when {
                    video.title.contains(token, ignoreCase = true) -> 6.0
                    video.channel.contains(token, ignoreCase = true) -> 4.0
                    video.primaryTopic.contains(token, ignoreCase = true) -> 3.0
                    video.tags.any { it.contains(token, ignoreCase = true) } -> 2.5
                    video.categories.any {
                        it.name.contains(token, ignoreCase = true) ||
                            it.areaName.contains(token, ignoreCase = true)
                    } -> 2.0

                    else -> 1.0
                }
            }

            video to (relevance + video.score / 1000.0)
        }.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Search across videos, curated playlists and subjects (areas/categories).
     * Subjects and playlists are surfaced first, then the ranked video matches.
     */
    fun searchHits(
        catalog: HtCatalog,
        query: String,
        offset: Int,
        limit: Int = SEARCH_PAGE_SIZE,
        filters: HtFilters = HtFilters()
    ): List<HtSearchHit> {
        val tokens = query.lowercase().trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return emptyList()

        fun matches(text: String): Boolean =
            tokens.all { text.lowercase().contains(it) }

        val includeSubjectsAndPlaylists = !filters.hasContentFilters

        val subjects = if (!includeSubjectsAndPlaylists) emptyList() else buildList {
            catalog.areas.forEach { area ->
                if (matches(area.name)) {
                    val count = catalog.areaCounts[area.id] ?: 0
                    if (count > 0) {
                        add(
                            HtSubject(
                                id = area.id,
                                name = area.name,
                                description = area.categories.joinToString(", ") { it.name },
                                areaId = area.id,
                                areaName = area.name,
                                videoCount = count,
                                topVideoId = catalog.videos.firstOrNull { video ->
                                    video.categories.any { it.area == area.id }
                                }?.vid.orEmpty()
                            )
                        )
                    }
                }
            }
            catalog.categories.forEach { category ->
                val area = catalog.areas.firstOrNull { area ->
                    area.categories.any { it.id == category.id }
                } ?: return@forEach
                if (matches(category.name + " " + category.description + " " + area.name)) {
                    val count = catalog.categoryCounts[category.id] ?: 0
                    if (count > 0) {
                        add(
                            HtSubject(
                                id = category.id,
                                name = category.name,
                                description = category.description,
                                areaId = area.id,
                                areaName = area.name,
                                videoCount = count,
                                topVideoId = catalog.videos.firstOrNull { video ->
                                    video.categories.any { it.id == category.id }
                                }?.vid.orEmpty()
                            )
                        )
                    }
                }
            }
        }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<HtSubject> { it.name.equals(query.trim(), true) }
                    .thenByDescending { it.name.startsWith(query.trim(), true) }
                    .thenByDescending { it.videoCount }
            )
            .take(6)

        val playlists = if (!includeSubjectsAndPlaylists) emptyList() else catalog.playlists.filter { playlist ->
            matches(
                buildString {
                    append(playlist.title).append(' ')
                    append(playlist.pitch).append(' ')
                    append(playlist.intro).append(' ')
                    playlist.videos.forEach { append(it.note).append(' ') }
                }
            )
        }.take(6)

        val videoSource = filteredVideos(catalog, catalog.videos, filters.copy(sort = ""))
        val rankedVideos = searchVideos(catalog, query, videoSource)
        val videos = if (filters.sort.isBlank()) {
            rankedVideos
        } else {
            filteredVideos(catalog, rankedVideos, filters)
        }

        val hits = buildList {
            subjects.forEach { add(HtSearchHit.Subject(it)) }
            playlists.forEach { add(HtSearchHit.Playlist(it)) }
            videos.forEach { add(HtSearchHit.Video(it)) }
        }

        return hits.drop(offset).take(limit)
    }

    fun suggestions(catalog: HtCatalog, query: String, limit: Int = 8): List<String> {
        if (query.isBlank()) return emptyList()
        val needle = query.trim().lowercase()

        val titles = catalog.videos.asSequence()
            .filter { it.title.lowercase().contains(needle) }
            .map { it.title }
        val channels = catalog.videos.asSequence()
            .filter { it.channel.lowercase().contains(needle) }
            .map { it.channel }
        val topics = catalog.categories.asSequence()
            .filter { it.name.lowercase().contains(needle) }
            .map { it.name }
        val playlists = catalog.playlists.asSequence()
            .filter { it.title.lowercase().contains(needle) }
            .map { it.title }

        return (titles + playlists + channels + topics)
            .distinct()
            .take(limit)
            .toList()
    }

    /**
     * Videos recommended by the same Hacker News commenters, falling back to videos
     * from the same categories when there is no co-recommendation data. Never leaves
     * the HackerTube catalogue.
     */
    fun relatedVideos(catalog: HtCatalog, videoId: String, limit: Int = 20): List<HtVideo> {
        val coRecommended = catalog.related[videoId].orEmpty()
            .mapNotNull { catalog.byId[it] }

        if (coRecommended.size >= limit) return coRecommended.take(limit)

        val categoryIds = catalog.byId[videoId]?.categories
            ?.map { it.id }
            ?.toSet()
            .orEmpty()

        if (categoryIds.isEmpty()) return coRecommended

        val sameTopic = catalog.videos.asSequence()
            .filter { video ->
                video.vid != videoId && video.categories.any { it.id in categoryIds }
            }
            .toList()

        return (coRecommended + sameTopic)
            .distinctBy { it.vid }
            .take(limit)
    }

    private const val SECONDS_4_MIN = 4 * 60L
    private const val SECONDS_20_MIN = 20 * 60L
    private const val SECONDS_PER_HOUR = 60 * 60L
}

fun HtVideo.toStreamItem(): StreamItem = StreamItem(
    url = vid,
    type = StreamItem.TYPE_STREAM,
    title = title,
    thumbnail = HackerTubeRepository.thumbnailUrl(vid),
    uploaderName = channel,
    uploaderVerified = false,
    uploadedDate = published,
    duration = durationSeconds.takeIf { it > 0 },
    views = views,
    shortDescription = summary.ifBlank { whyHn },
    uploaded = published.toEpochMillis(),
    isShort = false,
    live = false
)

fun HtVideo.toContentItem(): ContentItem = ContentItem(
    url = vid,
    type = StreamItem.TYPE_STREAM,
    thumbnail = HackerTubeRepository.thumbnailUrl(vid),
    title = title,
    duration = durationSeconds,
    views = views,
    isShort = false,
    uploaderVerified = false,
    uploaderName = channel,
    uploaded = published.toEpochMillis(),
    shortDescription = summary.ifBlank { whyHn }
)

fun HtSearchHit.toContentItem(): ContentItem = when (this) {
    is HtSearchHit.Video -> video.toContentItem()

    is HtSearchHit.Playlist -> ContentItem(
        url = HackerTubeRepository.PLAYLIST_URL_PREFIX + playlist.slug,
        type = StreamItem.TYPE_PLAYLIST,
        thumbnail = playlist.cover.firstOrNull()
            ?.let(HackerTubeRepository::thumbnailUrl)
            .orEmpty(),
        name = playlist.title,
        description = playlist.pitch,
        uploaderName = buildString {
            append("Curated playlist · ")
            append(playlist.videos.size)
            append(" videos")
            if (playlist.minutes > 0) {
                append(" · ")
                append(playlist.minutes)
                append(" min")
            }
        },
        videos = playlist.videos.size.toLong()
    )

    is HtSearchHit.Subject -> ContentItem(
        url = HackerTubeRepository.CATEGORY_URL_PREFIX + subject.id,
        type = HackerTubeRepository.SEARCH_TYPE_SUBJECT,
        thumbnail = subject.topVideoId
            .takeIf { it.isNotBlank() }
            ?.let(HackerTubeRepository::thumbnailUrl)
            .orEmpty(),
        name = subject.name,
        description = subject.description,
        uploaderName = "Subject · ${subject.areaName}",
        videos = subject.videoCount.toLong()
    )
}

fun isCuratedPlaylistUrl(url: String?): Boolean =
    url?.startsWith(HackerTubeRepository.PLAYLIST_URL_PREFIX) == true

fun isHackerTubeCategoryUrl(url: String?): Boolean =
    url?.startsWith(HackerTubeRepository.CATEGORY_URL_PREFIX) == true

fun curatedPlaylistSlug(url: String): String =
    url.removePrefix(HackerTubeRepository.PLAYLIST_URL_PREFIX)

fun hackerTubeCategoryId(url: String): String =
    url.removePrefix(HackerTubeRepository.CATEGORY_URL_PREFIX)

private fun String.toEpochMillis(): Long =
    runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(0L)
