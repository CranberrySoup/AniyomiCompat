package eu.kanade.tachiyomi.extension.anime.model

import android.graphics.drawable.Drawable
import androidx.preference.PreferenceScreen
import com.lagradost.awaitSingle
import com.lagradost.cloudstream3.APIHolder.capitalize
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.suspendSafeApiCall
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import eu.kanade.domain.source.anime.model.AnimeSourceData
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.toEpisodeList
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URI


sealed class AnimeExtension {
    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String?
    abstract val isNsfw: Boolean
    abstract val hasReadme: Boolean
    abstract val hasChangelog: Boolean

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val hasReadme: Boolean,
        override val hasChangelog: Boolean,
        val pkgFactory: String?,
        val sources: List<AnimeSource>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val isUnofficial: Boolean = false,
    ) : AnimeExtension() {
        fun getMainApis(): List<MainAPI> {
            val sources = sources.filterIsInstance<AnimeCatalogueSource>()
            return sources.map { source ->
                object : MainAPI() {
                    override var lang = if (source.lang == "all") "uni" else source.lang
                    override var name =
                        source.name + (if (sources.size > 1) " (${source.lang.capitalize()})" else "") + " ⦁"
                    override val supportedTypes = super.supportedTypes.toMutableSet().apply {
                        if (isNsfw) add(TvType.NSFW)
                    }

                    override suspend fun search(query: String): List<SearchResponse> {
                        return source.fetchSearchAnime(1, query, AnimeFilterList())
                            .awaitSingle().animes.map {
                                it.toSearchResponse(this.name)
                            }
                    }

                    fun canShowPreferenceScreen(): Boolean = source is ConfigurableAnimeSource
                    fun getPkgName(): String = pkgName
                    fun showPreferenceScreen(screen: PreferenceScreen) {
                        if (source is ConfigurableAnimeSource) {
                            source.setupPreferenceScreen(screen)
                        }
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
                                popular.data -> source.fetchPopularAnime(page)
                                latest.data -> source.fetchLatestUpdates(page)
                                else -> null
                            }?.awaitSingle()
                        }.getOrNull() ?: return null

                        return HomePageResponse(
                            listOf(
                                HomePageList(
                                    request.name,
                                    data.animes.map { it.toSearchResponse(this.name) },
                                    false
                                )
                            ), data.hasNextPage
                        )
                    }

                    override suspend fun load(url: String): LoadResponse? {
                        val sAnime = SAnime.fromData(url) ?: return null
                        val details = source.getAnimeDetails(sAnime)
                        val title = runCatching { details.title }.getOrNull() ?: sAnime.title
                        val episodes = suspendSafeApiCall {
                            source.getEpisodeList(sAnime).toEpisodeList()
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
                            val groups = qualityRegex.find(video.quality)?.groupValues
                            val qualityString = groups?.getOrNull(1)
                            val wholeString = groups?.getOrNull(0)
                            val quality = getQualityFromName(qualityString)
                            val videoName = video.quality.replace(wholeString ?: "", "")
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
                                    this[key] = Injekt.get<NetworkHelper>().defaultUserAgent
                                }
                            }

                            val videoUrl = video.videoUrl ?: video.url

                            callback.invoke(
                                ExtractorLink(
                                    videoName.trim(),
                                    videoName.trim(),
                                    videoUrl,
                                    "",
                                    quality,
                                    // java.net.URISyntaxException needs to be properly addressed
                                    isM3u8 = runCatching {
                                        URI(videoUrl).path?.substringAfterLast(".")
                                            ?.contains("m3u")
                                    }.getOrNull() == true,
                                    headers = headers
                                )
                            )
                            video.subtitleTracks.forEach { subtitle ->
                                subtitleCallback.invoke(
                                    SubtitleFile(subtitle.lang, subtitle.url)
                                )
                            }
                        }
                        return true
                    }
                }
            }
        }
    }

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val hasReadme: Boolean,
        override val hasChangelog: Boolean,
        val sources: List<AvailableAnimeSources>,
        val apkName: String,
        val iconUrl: String,
    ) : AnimeExtension()

    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
        override val lang: String? = null,
        override val isNsfw: Boolean = false,
        override val hasReadme: Boolean = false,
        override val hasChangelog: Boolean = false,
    ) : AnimeExtension()
}

data class AvailableAnimeSources(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
) {
    fun toAnimeSourceData(): AnimeSourceData {
        return AnimeSourceData(
            id = this.id,
            lang = this.lang,
            name = this.name,
        )
    }
}
