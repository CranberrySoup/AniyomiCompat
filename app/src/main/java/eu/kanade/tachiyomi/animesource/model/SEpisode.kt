package eu.kanade.tachiyomi.animesource.model

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.io.Serializable

fun List<SEpisode>.toEpisodeList(): List<Episode> {
    return this.sortedBy { it.episode_number }.map { episode ->
        Episode(
            data = episode.toJson(),
            name = episode.name,
            date = episode.date_upload,
            episode = null,
        )
    }
}

interface SEpisode : Serializable {

    var url: String

    var name: String

    var date_upload: Long

    var episode_number: Float

    var scanlator: String?

    fun copyFrom(other: SEpisode) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        episode_number = other.episode_number
        scanlator = other.scanlator
    }

    fun toEpisode(): Episode {
        return Episode(
            data = this.toJson(),
            name = name,
            date = date_upload,
            episode = episode_number.toInt(),
        )
    }

    companion object {
        fun create(): SEpisode {
            return SEpisodeImpl()
        }

        fun fromData(data: String): SEpisodeImpl? {
            return tryParseJson(data)
        }
    }
}
