package eu.kanade.tachiyomi.extension.anime.model

import android.graphics.drawable.Drawable
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
import com.lagradost.cloudstream3.mvvm.normalSafeApiCall
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import eu.kanade.domain.source.anime.model.AnimeSourceData
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
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
                    override var lang = source.lang
                    override var name =
                        source.name + if (sources.size > 1) " (${source.lang.capitalize()})" else ""
                    override val supportedTypes = super.supportedTypes.toMutableSet().apply {
                        if (isNsfw) add(TvType.NSFW)
                    }

                    override suspend fun search(query: String): List<SearchResponse> {
                        return source.fetchSearchAnime(1, query, AnimeFilterList())
                            .awaitSingle().animes.map {
                                it.toSearchResponse(this.name)
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
                        val data = when (request.data) {
                            popular.data -> normalSafeApiCall { source.fetchPopularAnime(page) }
                            latest.data -> normalSafeApiCall { source.fetchLatestUpdates(page) }
                            else -> null
                        }?.awaitSingle() ?: return null

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

                        return newAnimeLoadResponse(details.title, url, TvType.Anime) {
                            posterUrl = details.thumbnail_url
                            tags = details.getGenres()
                            plot = details.description
                            episodes = mutableMapOf(
                                DubStatus.None to source.getEpisodeList(sAnime)
                                    .map { it.toEpisode() }.sortedBy { it.episode })
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
                            val qualityRegex = Regex("""[\s^](\d+p)[\s$]""")
                            val qualityString =
                                qualityRegex.find(video.quality)?.groupValues?.getOrNull(1)
                            val quality = getQualityFromName(qualityString)
                            val videoName = video.quality.replace(qualityString ?: "", "")
                            callback.invoke(
                                ExtractorLink(
                                    this.name,
                                    this.name + " " + videoName.trim(),
                                    video.url,
                                    "",
                                    quality,
                                    // java.net.URISyntaxException needs to be properly addressed
                                    isM3u8 = normalSafeApiCall {
                                        URI(video.url).path?.substringAfterLast(".")
                                            ?.contains("m3u")
                                    } == true,
                                    headers = video.headers?.toMap() ?: emptyMap()
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
