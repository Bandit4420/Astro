package com.bandit.astro.data

import org.json.JSONArray
import org.json.JSONObject

data class ExploreFilters(val genre: String = "", val tag: String = "", val year: String = "", val season: String = "",
    val format: String = "", val status: String = "", val minScore: String = "", val maxEpisodes: String = "", val minEpisodes: String = "",
    val sort: String = "TRENDING_DESC", val studioId: Int = 0, val studioName: String = "") {
    fun variables(): JSONObject = JSONObject().put("genre", genre.ifBlank { null }).put("tag", tag.ifBlank { null })
        .put("year", year.toIntOrNull()?.let { "$it%" }).put("season", season.ifBlank { null }).put("status", status.ifBlank { null })
        .put("score", minScore.toIntOrNull()?.minus(1)).put("episodes", maxEpisodes.toIntOrNull()?.plus(1))
        .put("minEpisodes", minEpisodes.toIntOrNull()?.minus(1)).put("sort", JSONArray().put(sort))
}
data class UserProfile(val id: Int, val name: String, val avatar: String, val banner: String, val about: String,
    val anime: JSONObject, val manga: JSONObject, val favorites: List<Media> = emptyList(), val characters: List<Credit> = emptyList(), val staff: List<Credit> = emptyList(), val warnings: List<String> = emptyList())
data class RemoteEntry(val entryId: Int, val entry: LibraryEntry)
// Keep authentication independent of optional profile charts and favorite connections.
suspend fun AniListApi.identity(token:String):UserProfile {
    val d=query("query { Viewer { id name avatar { large } } }",token=token).getJSONObject("Viewer")
    return UserProfile(d.getInt("id"),d.text("name"),d.optJSONObject("avatar")?.text("large").orEmpty(),"","",JSONObject(),JSONObject())
}
suspend fun AniListApi.viewer(token: String): UserProfile = profile(token = token)
suspend fun AniListApi.profile(name: String? = null, token: String? = null): UserProfile {
    val field = if (name == null) "Viewer" else "User(name:${'$'}name)"
    val root = if (name == null) "Viewer" else "User"
    val declaration = if (name == null) "" else "(${'$'}name:String)"
    val variables = JSONObject().apply { if(name != null) put("name",name) }
    suspend fun section(selection:String) = query("query$declaration { $field { $selection } }",variables,token).getJSONObject(root)
    val base = section("id name avatar { large } bannerImage about")
    val warnings = mutableListOf<String>()
    suspend fun optional(label:String, selection:String):JSONObject {
        return try { section(selection) }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(e:java.io.IOException) { warnings += "$label unavailable: ${e.message}"; JSONObject() }
    }
    val common = "count meanScore standardDeviation statuses { status count } scores { score count } releaseYears { releaseYear count }"
    val anime = optional("Anime statistics", "statistics { anime { $common minutesWatched episodesWatched genres(limit:8,sort:COUNT_DESC) { genre count minutesWatched } } }")
        .optJSONObject("statistics")?.optJSONObject("anime") ?: JSONObject()
    val manga = optional("Reading statistics", "statistics { manga { $common chaptersRead volumesRead genres(limit:8,sort:COUNT_DESC) { genre count } } }")
        .optJSONObject("statistics")?.optJSONObject("manga") ?: JSONObject()
    val favorites = optional("Favorites", "favourites { anime(perPage:20) { nodes { ${AniListApi.CARD_FIELDS} isAdult } } characters(perPage:20) { nodes { id name { full } image { large } } } staff(perPage:20) { nodes { id name { full } image { large } } } }")
        .optJSONObject("favourites")
    return UserProfile(base.getInt("id"), base.text("name"),base.optJSONObject("avatar")?.text("large").orEmpty(),base.text("bannerImage"),base.text("about"), anime,manga,
        favorites?.optJSONObject("anime")?.optJSONArray("nodes")?.objects()?.filterNot { it.optBoolean("isAdult") }?.map(AniListApi::parseMedia).orEmpty(),
        parseCredits(favorites?.optJSONObject("characters")?.optJSONArray("nodes")),
        parseCredits(favorites?.optJSONObject("staff")?.optJSONArray("nodes")), warnings)
}
suspend fun AniListApi.remoteList(user: Int, token: String? = null): List<RemoteEntry> {
    val all = mutableListOf<RemoteEntry>()
    for (type in listOf("ANIME", "MANGA")) {
        var chunk = 1
        do {
            val d = query("""query(${'$'}user:Int, ${'$'}type:MediaType, ${'$'}chunk:Int) {
                MediaListCollection(userId:${'$'}user,type:${'$'}type,chunk:${'$'}chunk,perChunk:500) {
                    hasNextChunk lists { entries { id progress progressVolumes status score(format:POINT_100) updatedAt
                        media { ${AniListApi.MEDIA_FIELDS} } } }
                }
            }""", JSONObject().put("user", user).put("type", type).put("chunk", chunk), token).getJSONObject("MediaListCollection")
            for (group in d.optJSONArray("lists")?.objects().orEmpty()) for (e in group.optJSONArray("entries")?.objects().orEmpty()) {
                val m = e.optJSONObject("media") ?: continue
                all += RemoteEntry(e.getInt("id"), AniListApi.parseMedia(m).asEntry().copy(progress = e.optInt("progress"),
                    volumes = e.optInt("progressVolumes"), status = e.text("status"), score = e.optInt("score"),
                    remoteUpdated = e.optLong("updatedAt"), updatedAt = e.optLong("updatedAt") * 1000, dirty = false))
            }
            chunk++
        } while (d.optBoolean("hasNextChunk"))
    }
    return all.distinctBy { it.entry.mediaId }
}
suspend fun AniListApi.save(entry: LibraryEntry, token: String): Long {
    val d = query("""mutation(${'$'}id:Int, ${'$'}progress:Int, ${'$'}volumes:Int, ${'$'}status:MediaListStatus, ${'$'}score:Int) {
        SaveMediaListEntry(mediaId:${'$'}id,progress:${'$'}progress,progressVolumes:${'$'}volumes,status:${'$'}status,scoreRaw:${'$'}score) { updatedAt }
    }""", JSONObject().put("id", entry.mediaId).put("progress", entry.progress).put("volumes", entry.volumes).put("status", entry.status).put("score", entry.score), token)
    return d.getJSONObject("SaveMediaListEntry").getLong("updatedAt")
}
suspend fun AniListApi.delete(entryId: Int, token: String) {
    val d = query("mutation(${'$'}id:Int) { DeleteMediaListEntry(id:${'$'}id) { deleted } }", JSONObject().put("id", entryId), token)
    check(d.getJSONObject("DeleteMediaListEntry").optBoolean("deleted")) { "AniList did not confirm removal." }
}
suspend fun AniListApi.cast(id: Int, page: Int, language: String = "JAPANESE"): Pair<List<CastMember>, Boolean> {
    val d = query("""query(${'$'}id:Int, ${'$'}page:Int, ${'$'}language:StaffLanguage) { Media(id:${'$'}id) {
        characters(page:${'$'}page,perPage:25,sort:[ROLE,RELEVANCE,ID]) { pageInfo { hasNextPage } edges { role
            node { id name { full } image { large } } voiceActors(language:${'$'}language,sort:RELEVANCE) { id name { full } image { large } }
        } }
    } }""", JSONObject().put("id", id).put("page", page).put("language", language)).getJSONObject("Media").getJSONObject("characters")
    return d.getJSONArray("edges").objects().map { e ->
        val n=e.getJSONObject("node"); val a=e.optJSONArray("voiceActors")?.optJSONObject(0)
        CastMember(n.getInt("id"),n.getJSONObject("name").text("full"),n.getJSONObject("image").text("large"), e.text("role"),
            a?.optJSONObject("name")?.text("full").orEmpty(),a?.optInt("id") ?: 0,a?.optJSONObject("image")?.text("large").orEmpty(), parseCredits(e.optJSONArray("voiceActors")))
    } to d.getJSONObject("pageInfo").optBoolean("hasNextPage")
}
suspend fun AniListApi.studios(search: String): List<Pair<Int,String>> = query("query(${'$'}s:String) { Page(perPage:8) { studios(search:${'$'}s) { id name } } }",
    JSONObject().put("s",search)).getJSONObject("Page").getJSONArray("studios").objects().map { it.getInt("id") to it.text("name") }
suspend fun AniListApi.studio(id: Int, page: Int, search: String, f: ExploreFilters): CatalogPage {
    val d=query("query(${'$'}id:Int, ${'$'}page:Int) { Studio(id:${'$'}id) { media(page:${'$'}page,perPage:20,sort:POPULARITY_DESC) { pageInfo { hasNextPage } nodes { ${AniListApi.MEDIA_FIELDS} isAdult season } } } }",
        JSONObject().put("id",id).put("page",page)).getJSONObject("Studio").getJSONObject("media")
    val rows=d.getJSONArray("nodes").objects().filterNot { it.optBoolean("isAdult") }.filter { f.season.isBlank() || it.text("season") == f.season }.map(AniListApi::parseMedia).filter {
        it.type=="ANIME" && it.title.contains(search,true) && (f.genre.isBlank()||f.genre in it.genres) && (f.year.isBlank()||it.year.toString()==f.year) &&
            (f.format.isBlank()||it.format==f.format) && (f.status.isBlank()||it.status==f.status) && it.score >= (f.minScore.toIntOrNull() ?: 0) &&
            it.total >= (f.minEpisodes.toIntOrNull() ?: 0) && (f.maxEpisodes.isBlank()||it.total in 1..(f.maxEpisodes.toIntOrNull() ?: Int.MAX_VALUE))
    }
    return CatalogPage(rows,d.getJSONObject("pageInfo").optBoolean("hasNextPage"))
}

suspend fun AniListApi.expandRelations(ids:List<Int>):List<Relation> {
    if(ids.isEmpty())return emptyList()
    val d=query("query(${'$'}ids:[Int]) { Page(perPage:40) { media(id_in:${'$'}ids,isAdult:false) { relations { edges { relationType node { ${AniListApi.MEDIA_FIELDS} isAdult } } } } } }",JSONObject().put("ids",JSONArray(ids)))
    return d.getJSONObject("Page").getJSONArray("media").objects().flatMap {it.getJSONObject("relations").getJSONArray("edges").objects()}.mapNotNull {e->e.optJSONObject("node")?.takeUnless {it.optBoolean("isAdult")}?.let {Relation(AniListApi.parseMedia(it),e.text("relationType"))}}
}
