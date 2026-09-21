package com.bandit.astro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Catalog(val label: String) { ANIME("Anime"), MANGA("Manga"), NOVEL("Novels") }

data class Media(
    val id: Int, val title: String, val nativeTitle: String = "", val cover: String = "",
    val banner: String = "", val accent: String = "", val type: String = "ANIME",
    val format: String = "", val status: String = "", val description: String = "",
    val total: Int = 0, val duration: Int = 0, val score: Int = 0, val year: Int = 0,
    val genres: List<String> = emptyList(), val nextEpisode: Int = 0, val nextAiring: Long = 0,
    val trailerId: String = "", val trailerSite: String = "", val malId: Int = 0,
    val volumes: Int = 0, val studios: List<String> = emptyList()
) { val unit: String get() = if (type == "ANIME") "episode" else "chapter" }

data class CastMember(val id: Int, val name: String, val image: String, val role: String,
    val actorName: String, val actorId: Int, val actorImage: String,
    val birthYear: Int = 0, val birthMonth: Int = 0, val birthDay: Int = 0,
    val description: String = "")
data class Credit(val id: Int, val name: String, val image: String, val role: String)
data class Relation(val media: Media, val relation: String)
data class MediaDetail(val media: Media, val characters: List<CastMember>, val castPage: Int = 1,
    val moreCast: Boolean = false, val staff: List<Credit> = emptyList(), val relations: List<Relation> = emptyList(),
    val recommendations: List<Media> = emptyList())
data class Person(val name: String, val image: String, val description: String, val works: List<Media> = emptyList(), val page: Int = 1, val more: Boolean = false)
data class CatalogPage(val media: List<Media>, val hasNext: Boolean)
data class Airing(val id: Int, val episode: Int, val airingAt: Long, val media: Media)

@Entity(tableName = "collection")
data class LibraryEntry(
    @PrimaryKey val mediaId: Int,
    val title: String,
    val cover: String,
    val type: String,
    val format: String,
    val total: Int,
    val duration: Int,
    val progress: Int = 0,
    val status: String = "PLANNING",
    val updatedAt: Long = System.currentTimeMillis(),
    val volumes: Int = 0, val totalVolumes: Int = 0, val score: Int = 0, val notes: String = "",
    val genres: String = "", val studios: String = "", val nextEpisode: Int = 0, val nextAiring: Long = 0,
    val dirty: Boolean = true, val deleted: Boolean = false, val revision: Long = 1,
    val remoteUpdated: Long = 0
)

fun Media.asEntry() = LibraryEntry(id, title, cover, type, format, total, duration, totalVolumes = volumes, genres = genres.joinToString("|"), studios = studios.joinToString("|"), nextEpisode = nextEpisode, nextAiring = nextAiring)
fun LibraryEntry.asMedia() = Media(id = mediaId, title = title, cover = cover, type = type,
    format = format, total = total, duration = duration, volumes = totalVolumes, genres = genres.split("|").filter { it.isNotBlank() }, studios = studios.split("|").filter { it.isNotBlank() }, nextEpisode = nextEpisode, nextAiring = nextAiring)

// Clamp only when AniList publishes a known total. Unknown totals must remain trackable.
fun nextProgress(current: Int, total: Int): Int =
    (current.toLong().coerceAtLeast(0) + 1).coerceAtMost(
        if (total > 0) total.toLong() else Int.MAX_VALUE.toLong()
    ).toInt()

fun episodeIsDue(airingAt: Long, now: Long, watched: Int, episode: Int): Boolean =
    airingAt in (now - 6 * 3600)..now && episode > watched
