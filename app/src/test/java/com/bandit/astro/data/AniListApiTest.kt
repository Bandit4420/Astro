package com.bandit.astro.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AniListApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AniListApi
    @Before fun before() { server = MockWebServer(); server.start(); api = AniListApi(OkHttpClient(), server.url("/").toString()) }
    @After fun after() { server.shutdown() }
    @Test fun `nullable AniList fields do not appear as null text`() {
        val media = AniListApi.parseMedia(JSONObject("""{"id":1,"type":"ANIME","title":{"userPreferred":"Test","native":null},"coverImage":null,"episodes":null,"description":null,"bannerImage":null}"""))
        assertEquals("Test", media.title)
        assertEquals("", media.description)
        assertEquals("", media.banner)
        assertEquals(0, media.total)
    }
    @Test fun `novel discovery uses manga type and novel format`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"Page":{"media":[],"pageInfo":{"hasNextPage":false}}}}"""))
        val result = api.discover(Catalog.NOVEL, "  Spice and Wolf  ", 2)
        val vars = JSONObject(server.takeRequest().body.readUtf8()).getJSONObject("variables")
        assertEquals("MANGA", vars.getString("type"))
        assertEquals("NOVEL", vars.getString("format"))
        assertEquals("Spice and Wolf", vars.getString("search"))
        assertEquals(2, vars.getInt("page"))
        assertFalse(result.hasNext)
    }
    @Test fun `rate limiting is retryable with a useful message`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        try { api.discover(Catalog.ANIME, "", 1); fail("Expected rate limit") }
        catch (error: ApiException) { assertTrue(error.retryable); assertTrue(error.message!!.contains("minute")) }
    }
    @Test fun `GraphQL errors are not treated as an empty successful list`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":null,"errors":[{"message":"Unavailable"}]}"""))
        try { api.discover(Catalog.ANIME, "", 1); fail("Expected API error") }
        catch (error: ApiException) { assertFalse(error.retryable) }
    }
}
