package com.bandit.astro.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiException(message: String, val retryable: Boolean = false) : IOException(message)
class AniListApi(private val client: OkHttpClient, private val endpoint: String = "https://graphql.anilist.co") {
    internal suspend fun query(query: String, variables: JSONObject = JSONObject(), token: String? = null): JSONObject {
        val body = JSONObject().put("query", query).put("variables", variables).toString()
        val request = Request.Builder().url(endpoint).header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .post(body.toRequestBody("application/json".toMediaType())).build()
        val text = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        if (!it.isSuccessful) {
                            val message = if (it.code == 401 || it.code == 403) "AniList authorization expired or was refused. Sign in again." else if (it.code == 429) "AniList is busy. Please try again in a minute." else "AniList could not load this right now (${it.code})."
                            continuation.resumeWithException(ApiException(message, it.code == 429 || it.code >= 500))
                        } else continuation.resume(it.body?.string() ?: "{}")
                    }
                }
            })
        }
        val result = JSONObject(text)
        if ((result.optJSONArray("errors")?.length() ?: 0) > 0)
            throw ApiException("AniList could not complete this request. Please try again.", result.optJSONArray("errors")?.optJSONObject(0)?.optInt("status")?.let { it == 429 || it >= 500 } ?: false)
        return result.optJSONObject("data") ?: throw ApiException("No data was returned by AniList.")
    }

    suspend fun discover(catalog: Catalog, search: String, page: Int, filters: ExploreFilters = ExploreFilters()): CatalogPage {
        val variables = JSONObject().put("page", page).put("type", if (catalog == Catalog.ANIME) "ANIME" else "MANGA")
            .put("search", search.trim().ifEmpty { null })
            .put("format", if (catalog == Catalog.NOVEL) "NOVEL" else filters.format.ifBlank { null } ?: JSONObject.NULL)
            .put("excluded", if (catalog == Catalog.MANGA) JSONArray().put("NOVEL") else JSONObject.NULL)
        filters.variables().keys().forEach { key -> variables.put(key, filters.variables().get(key)) }
        val data = query("""query(${'$'}genre:String, ${'$'}tag:String, ${'$'}year:String, ${'$'}season:MediaSeason, ${'$'}status:MediaStatus, ${'$'}score:Int, ${'$'}episodes:Int, ${'$'}sort:[MediaSort], ${'$'}minEpisodes:Int, ${'$'}page:Int, ${'$'}type:MediaType, ${'$'}search:String, ${'$'}format:MediaFormat, ${'$'}excluded:[MediaFormat]) {
            Page(page:${'$'}page, perPage:20) { pageInfo { hasNextPage }
                media(type:${'$'}type, search:${'$'}search, format:${'$'}format, format_not_in:${'$'}excluded, isAdult:false, genre:${'$'}genre, tag:${'$'}tag, startDate_like:${'$'}year, season:${'$'}season, status:${'$'}status, averageScore_greater:${'$'}score, episodes_lesser:${'$'}episodes, sort:${'$'}sort, episodes_greater:${'$'}minEpisodes) { $MEDIA_FIELDS }
            }
        }""", variables).getJSONObject("Page")
        return CatalogPage(data.getJSONArray("media").objects().map(::parseMedia), data.getJSONObject("pageInfo").optBoolean("hasNextPage"))
    }
    suspend fun detail(id: Int): MediaDetail {
        val data = query("""query(${'$'}id:Int) { Media(id:${'$'}id) { $MEDIA_FIELDS
            characters(sort:[ROLE,RELEVANCE,ID], perPage:25) { pageInfo { hasNextPage } edges { role node { id name { full } image { large } }
                voiceActors(language:JAPANESE, sort:RELEVANCE) { id name { full } image { large } } } }
            staff(perPage:25) { edges { role node { id name { full } image { large } } } }
            relations { edges { relationType node { $MEDIA_FIELDS isAdult } } }
            recommendations(perPage:12, sort:RATING_DESC) { nodes { mediaRecommendation { $MEDIA_FIELDS isAdult } } }
        } }""", JSONObject().put("id", id)).getJSONObject("Media")
        val characters = data.optJSONObject("characters")?.optJSONArray("edges")?.objects()?.map { edge ->
            val node = edge.getJSONObject("node")
            val actor = edge.optJSONArray("voiceActors")?.optJSONObject(0)
            CastMember(node.getInt("id"), node.getJSONObject("name").text("full"), node.optJSONObject("image")?.text("large").orEmpty(),
                edge.text("role"), actor?.optJSONObject("name")?.text("full").orEmpty(), actor?.optInt("id") ?: 0,
                actor?.optJSONObject("image")?.text("large").orEmpty(), parseCredits(edge.optJSONArray("voiceActors")))
        }.orEmpty()
        return MediaDetail(parseMedia(data), characters, moreCast = data.getJSONObject("characters").getJSONObject("pageInfo").optBoolean("hasNextPage"),
            staff = data.optJSONObject("staff")?.optJSONArray("edges")?.objects()?.map { e -> val n=e.getJSONObject("node"); Credit(n.getInt("id"),n.getJSONObject("name").text("full"),n.optJSONObject("image")?.text("large").orEmpty(), e.text("role")) }.orEmpty(),
            relations = data.optJSONObject("relations")?.optJSONArray("edges")?.objects()?.mapNotNull { e -> e.optJSONObject("node")?.takeUnless { it.optBoolean("isAdult") }?.let { Relation(parseMedia(it), e.text("relationType")) } }.orEmpty(),
            recommendations = data.optJSONObject("recommendations")?.optJSONArray("nodes")?.objects()?.mapNotNull { it.optJSONObject("mediaRecommendation")?.takeUnless { n -> n.optBoolean("isAdult") }?.let(::parseMedia) }.orEmpty())
    }
    suspend fun person(id: Int, staff: Boolean, page: Int = 1, language: String = "JAPANESE"): Person {
        val field = if (staff) "Staff" else "Character"
        val facts = if(staff) "age languageV2 homeTown primaryOccupations yearsActive dateOfBirth { year month day }" else "age gender bloodType dateOfBirth { year month day }"
        val connection = if(staff) "staffMedia" else "media"
        val extra = if(staff) "characters(page:$page,perPage:25,sort:FAVOURITES_DESC) { pageInfo { hasNextPage } nodes { id name { full } image { large } } }" else ""
        val edges = if(staff) "" else "edges { voiceActors(language:${if(language=="ENGLISH") "ENGLISH" else "JAPANESE"}) { id name { full } image { large } } }"
        val data = query("""query(${'$'}id:Int) { $field(id:${'$'}id) { name { full native } image { large } description $facts
            $connection(page:$page,perPage:25,sort:POPULARITY_DESC) { pageInfo { hasNextPage } nodes { $MEDIA_FIELDS isAdult } $edges } $extra } }""", JSONObject().put("id",id)).getJSONObject(field)
        val works=data.getJSONObject(connection)
        val birth=data.optJSONObject("dateOfBirth")
        val birthday=listOf("year","month","day").map { birth?.optInt(it)?.takeIf { n->n>0 }?.toString() ?: "?" }.joinToString(" / ")
        val rows=mutableListOf("Age" to data.text("age"),"Birthday" to birthday.takeUnless { it=="? / ? / ?" }.orEmpty())
        if(staff) rows+=listOf("Language" to data.text("languageV2"),"Hometown" to data.text("homeTown"),"Occupations" to data.optJSONArray("primaryOccupations").strings().joinToString(", "),"Active years" to data.optJSONArray("yearsActive").strings().joinToString("–"))
        else rows+=listOf("Gender" to data.text("gender"),"Blood type" to data.text("bloodType"))
        return Person(data.getJSONObject("name").text("full"),data.optJSONObject("image")?.text("large").orEmpty(),data.text("description"),
            works.getJSONArray("nodes").objects().filterNot { it.optBoolean("isAdult") }.map(::parseMedia),page,works.getJSONObject("pageInfo").optBoolean("hasNextPage"),
            data.getJSONObject("name").text("native"), rows.filter { it.second.isNotBlank() },
            works.optJSONArray("edges")?.objects()?.flatMap { parseCredits(it.optJSONArray("voiceActors")) }?.distinctBy { it.id }.orEmpty(),
            parseCredits(data.optJSONObject("characters")?.optJSONArray("nodes")),data.optJSONObject("characters")?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") ?: false)
    }
    suspend fun schedule(from: Long, to: Long, page: Int = 1, ids: List<Int>? = null): Pair<List<Airing>, Boolean> {
        val vars = JSONObject().put("from", from).put("to", to).put("page", page)
            .put("ids", ids?.let { JSONArray(it) } ?: JSONObject.NULL)
        val data = query("""query(${'$'}from:Int, ${'$'}to:Int, ${'$'}page:Int, ${'$'}ids:[Int]) {
            Page(page:${'$'}page, perPage:50) { pageInfo { hasNextPage }
                airingSchedules(airingAt_greater:${'$'}from, airingAt_lesser:${'$'}to, mediaId_in:${'$'}ids, sort:TIME) {
                    id episode airingAt media { $MEDIA_FIELDS isAdult }
                }
            }
        }""", vars).getJSONObject("Page")
        return data.getJSONArray("airingSchedules").objects().mapNotNull {
            val media = it.optJSONObject("media") ?: return@mapNotNull null
            if (media.optBoolean("isAdult")) null else Airing(it.getInt("id"), it.getInt("episode"), it.getLong("airingAt"), parseMedia(media))
        } to data.getJSONObject("pageInfo").optBoolean("hasNextPage")
    }
    companion object {
        internal const val MEDIA_FIELDS = """id idMal volumes studios(isMain:true) { nodes { name } } title { userPreferred native } coverImage { large color } bannerImage type format status
            tags { name isMediaSpoiler isGeneralSpoiler } description(asHtml:false) episodes chapters duration averageScore seasonYear startDate { year } genres
            nextAiringEpisode { episode airingAt } trailer { id site }"""
        internal fun parseMedia(value: JSONObject): Media {
            val cover = value.optJSONObject("coverImage")
            val airing = value.optJSONObject("nextAiringEpisode")
            val trailer = value.optJSONObject("trailer")
            val type = value.text("type")
            return Media(id = value.getInt("id"), title = value.optJSONObject("title")?.text("userPreferred") ?: "Untitled",
                nativeTitle = value.optJSONObject("title")?.text("native").orEmpty(), cover = cover?.text("large").orEmpty(),
                banner = value.text("bannerImage"), accent = cover?.text("color").orEmpty(), type = type,
                format = value.text("format"), status = value.text("status"), description = value.text("description"),
                total = value.optInt(if (type == "ANIME") "episodes" else "chapters"), duration = value.optInt("duration"),
                score = value.optInt("averageScore"), year = value.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 } ?: value.optInt("seasonYear"),
                tags = value.optJSONArray("tags")?.objects()?.filterNot { it.optBoolean("isMediaSpoiler") || it.optBoolean("isGeneralSpoiler") }?.map { it.text("name") }.orEmpty(),
                genres = value.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
                nextEpisode = airing?.optInt("episode") ?: 0, nextAiring = airing?.optLong("airingAt") ?: 0,
                trailerId = trailer?.text("id").orEmpty(), trailerSite = trailer?.text("site").orEmpty(), malId = value.optInt("idMal"), volumes = value.optInt("volumes"), studios = value.optJSONObject("studios")?.optJSONArray("nodes")?.objects()?.map { it.text("name") }.orEmpty())
        }
    }
}
internal fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key, "")
internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
