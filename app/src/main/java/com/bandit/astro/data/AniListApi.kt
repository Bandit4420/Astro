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
    private suspend fun query(query: String, variables: JSONObject = JSONObject()): JSONObject {
        val body = JSONObject().put("query", query).put("variables", variables).toString()
        val request = Request.Builder().url(endpoint).header("Accept", "application/json")
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
                            val message = if (it.code == 429) "AniList is busy. Please try again in a minute." else "AniList could not load this right now (${it.code})."
                            continuation.resumeWithException(ApiException(message, it.code == 429 || it.code >= 500))
                        } else continuation.resume(it.body?.string() ?: "{}")
                    }
                }
            })
        }
        val result = JSONObject(text)
        if ((result.optJSONArray("errors")?.length() ?: 0) > 0)
            throw ApiException("AniList could not complete this request. Please try again.")
        return result.optJSONObject("data") ?: throw ApiException("No data was returned by AniList.")
    }

    suspend fun discover(catalog: Catalog, search: String, page: Int): CatalogPage {
        val variables = JSONObject().put("page", page).put("type", if (catalog == Catalog.ANIME) "ANIME" else "MANGA")
            .put("search", search.trim().ifEmpty { null })
            .put("format", if (catalog == Catalog.NOVEL) "NOVEL" else JSONObject.NULL)
            .put("excluded", if (catalog == Catalog.MANGA) JSONArray().put("NOVEL") else JSONObject.NULL)
        val data = query("""query(${'$'}page:Int, ${'$'}type:MediaType, ${'$'}search:String, ${'$'}format:MediaFormat, ${'$'}excluded:[MediaFormat]) {
            Page(page:${'$'}page, perPage:20) { pageInfo { hasNextPage }
                media(type:${'$'}type, search:${'$'}search, format:${'$'}format, format_not_in:${'$'}excluded, isAdult:false, sort:[TRENDING_DESC,POPULARITY_DESC]) { $MEDIA_FIELDS }
            }
        }""", variables).getJSONObject("Page")
        return CatalogPage(data.getJSONArray("media").objects().map(::parseMedia), data.getJSONObject("pageInfo").optBoolean("hasNextPage"))
    }
    suspend fun detail(id: Int): MediaDetail {
        val data = query("""query(${'$'}id:Int) { Media(id:${'$'}id) { $MEDIA_FIELDS
            characters(sort:[ROLE,RELEVANCE,ID], perPage:25) { edges { role node { id name { full } image { large } }
                voiceActors(language:JAPANESE, sort:RELEVANCE) { id name { full } image { large } } } }
        } }""", JSONObject().put("id", id)).getJSONObject("Media")
        val characters = data.optJSONObject("characters")?.optJSONArray("edges")?.objects()?.map { edge ->
            val node = edge.getJSONObject("node")
            val actor = edge.optJSONArray("voiceActors")?.optJSONObject(0)
            CastMember(node.getInt("id"), node.getJSONObject("name").text("full"), node.optJSONObject("image")?.text("large").orEmpty(),
                edge.text("role"), actor?.optJSONObject("name")?.text("full").orEmpty(), actor?.optInt("id") ?: 0,
                actor?.optJSONObject("image")?.text("large").orEmpty())
        }.orEmpty()
        return MediaDetail(parseMedia(data), characters)
    }
    suspend fun person(id: Int, staff: Boolean): Person {
        val field = if (staff) "Staff" else "Character"
        val data = query("""query(${'$'}id:Int) { $field(id:${'$'}id) { name { full } image { large } description } }""",
            JSONObject().put("id", id)).getJSONObject(field)
        return Person(data.getJSONObject("name").text("full"), data.optJSONObject("image")?.text("large").orEmpty(), data.text("description"))
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
        private const val MEDIA_FIELDS = """id title { userPreferred native } coverImage { large color } bannerImage type format status
            description(asHtml:false) episodes chapters duration averageScore seasonYear genres
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
                score = value.optInt("averageScore"), year = value.optInt("seasonYear"),
                genres = value.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
                nextEpisode = airing?.optInt("episode") ?: 0, nextAiring = airing?.optLong("airingAt") ?: 0,
                trailerId = trailer?.text("id").orEmpty(), trailerSite = trailer?.text("site").orEmpty())
        }
    }
}
private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key, "")
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
