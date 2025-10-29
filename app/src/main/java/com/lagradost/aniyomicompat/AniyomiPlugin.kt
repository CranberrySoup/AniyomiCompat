package com.lagradost.aniyomicompat

import android.app.Application
import android.content.Context
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.R.string.extensions
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.safeAsync
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.toNewSearchResponseList
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.di.AppModule
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


enum class EpisodeSortMethods(val num: Int) {
    None(0),
    Ascending(1),
    Reverse(2),
}

fun sortEpisodes(sortMethod: Int, episodes: List<SEpisode>): List<SEpisode> {
    return when (sortMethod) {
        EpisodeSortMethods.Reverse.num -> episodes.reversed()
        EpisodeSortMethods.Ascending.num -> episodes.sortedBy { it.episode_number }
        else -> episodes
    }
}

fun List<SEpisode>.toEpisodeList(api: MainAPI): List<Episode> {
    return sortEpisodes(AniyomiPlugin.aniyomiSortingMethod, this).map { episode ->
        api.newEpisode(
            episode.toJson(),
        ) {
            name = episode.name
            date = episode.date_upload
            this.episode = null
        }
    }
}

class AniyomiPlugin : Plugin() {
    companion object {
        private const val sortingMethodKey = "ANIYOMI_SORTING_METHOD"
        var aniyomiSortingMethod: Int
            get() = getKey(sortingMethodKey) ?: EpisodeSortMethods.Ascending.num
            set(value) {
                setKey(sortingMethodKey, value)
            }
    }

    override fun load(context: Context) {
        Injekt.importModule(CustomAppModule(context.applicationContext as Application))
        val extensions = AnimeExtensionLoader.loadExtensions(context)
        extensions.forEach {
            if (it is AnimeLoadResult.Success) {
                val mainApis = it.extension.getMainApis()
                println("Loaded Aniyomi apis: $mainApis")
                mainApis.forEach(::registerMainAPI)
            }
        }
        println("Loaded Aniyomi extensions: $extensions")
    }
}

fun SAnime.toSearchResponse(api: MainAPI): AnimeSearchResponse {
    return api.newAnimeSearchResponse(
        title,
        // Hack to fully preserve data within the url
        this.toJson(),
    ) {
        this.posterUrl = thumbnail_url
    }
}

fun SAnime.Companion.fromData(data: String): SAnimeImpl? {
    return tryParseJson(data)
}

fun SEpisode.Companion.fromData(data: String): SEpisodeImpl? {
    return tryParseJson(data)
}

private fun AnimeExtension.Installed.getMainApis(): List<MainAPI> {
    val sources = sources.filterIsInstance<AnimeCatalogueSource>()
    return sources.map { source ->
        object : MainAPI() {
            override var lang = if (source.lang == "all") "uni" else source.lang
            override var name =
                source.name + (if (sources.size > 1) " (${source.lang.capitalize()})" else "") + " ⦁"
            override val supportedTypes = super.supportedTypes.toMutableSet().apply {
                if (isNsfw) add(TvType.NSFW)
            }

            override suspend fun search(query: String, page: Int): SearchResponseList {
                return source.getSearchAnime(page, query, source.getFilterList()).animes.map {
                    it.toSearchResponse(this)
                }.toNewSearchResponseList()
            }

            val popular = MainPageData("Popular", "1")
            val latest = MainPageData("Latest", "2")
            override val mainPage = listOf(popular, latest)
            override val hasMainPage = true

            override suspend fun getMainPage(
                page: Int,
                request: MainPageRequest
            ): HomePageResponse? {
                val data = runCatching {
                    when (request.data) {
                        popular.data -> source.getPopularAnime(page)
                        latest.data -> source.getLatestUpdates(page)
                        else -> null
                    }
                }.getOrNull() ?: return null

                return newHomePageResponse(
                    listOf(
                        HomePageList(
                            request.name,
                            data.animes.map { it.toSearchResponse(this) },
                            false
                        )
                    ), data.hasNextPage
                )
            }

            override suspend fun load(url: String): LoadResponse? {
                val sAnime = SAnime.fromData(url) ?: return null
                val details = source.getAnimeDetails(sAnime)
                val title = runCatching { details.title }.getOrNull() ?: sAnime.title
                val episodes = safeAsync {
                    source.getEpisodeList(sAnime).toEpisodeList(this)
                } ?: emptyList()

                return newAnimeLoadResponse(title, url, TvType.Anime) {
                    this.posterUrl = details.thumbnail_url ?: sAnime.thumbnail_url
                    this.tags = details.getGenres()
                    this.plot = details.description
                    this.episodes = mutableMapOf(
                        DubStatus.None to episodes
                    )
                }
            }

            override suspend fun loadLinks(
                data: String,
                isCasting: Boolean,
                subtitleCallback: (SubtitleFile) -> Unit,
                callback: (ExtractorLink) -> Unit
            ): Boolean {
                val sEpisode = SEpisode.fromData(data) ?: return false

                source.getVideoList(sEpisode).forEach { video ->
                    val qualityRegex = Regex("""[\s:](\d+p)\s*$""")
                    val groups = qualityRegex.find(video.videoTitle)?.groupValues
                    val qualityString = groups?.getOrNull(1)
//                    val wholeString = groups?.getOrNull(0)
                    val quality = getQualityFromName(qualityString)
//                    val videoName = video.quality.replace(wholeString ?: "", "")
                    val headers = (video.headers?.toMultimap()
                        ?.mapValues { it.value.firstOrNull() ?: "" }
                        ?.toMutableMap()
                        ?: (source as? AnimeHttpSource)?.headers?.toMultimap()
                            ?.mapValues { it.value.firstOrNull() ?: "" }
                            ?.toMutableMap() ?: mutableMapOf()).apply {

                        // Set the appropriate user agent forcefully.
                        this.keys.filter { key ->
                            key.equals("user-agent", ignoreCase = true)
                        }.forEach { key ->
                            this[key] = Injekt.get<NetworkHelper>().defaultUserAgentProvider()
                        }
                    }

                    callback.invoke(
                        newExtractorLink(
                            video.videoTitle,
                            video.videoTitle,
                            video.videoUrl,
                            INFER_TYPE,
                        ) {
                            this.quality = quality
                            this.headers = headers
                        }
                    )
                    video.subtitleTracks.forEach { subtitle ->
                        subtitleCallback.invoke(
                            newSubtitleFile(subtitle.lang, subtitle.url)
                        )
                    }
                }
                return true
            }
        }
    }
}
