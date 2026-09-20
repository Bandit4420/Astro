package com.bandit.astro.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PolishRegressionTest {
    @Test fun `unfiltered schedule omits null media filter and optional heavy fields`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"Page":{"pageInfo":{"hasNextPage":false},"airingSchedules":[{"id":5,"episode":2,"airingAt":200,"media":{"id":1,"type":"ANIME","title":{"userPreferred":"Test"}}}]}}}"""))
            val(rows,more)=AniListApi(OkHttpClient(),server.url("/").toString()).schedule(100,300)
            val body=JSONObject(server.takeRequest().body.readUtf8());val query=body.getString("query")
            assertFalse(query.contains("mediaId_in"));assertFalse(body.getJSONObject("variables").has("ids"))
            assertFalse(query.contains("studios"));assertEquals("Test",rows.single().media.title);assertFalse(more)
        }finally{server.shutdown()}
    }
    @Test fun `empty personal schedule does not request every anime`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val result=AniListApi(OkHttpClient(),server.url("/").toString()).schedule(100,300,ids=emptyList())
            assertTrue(result.first.isEmpty());assertEquals(0,server.requestCount)
        }finally{server.shutdown()}
    }
    @Test fun `profile survives a failed statistics section without pretending counts are zero`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"Viewer":{"id":44,"name":"Bandit","about":"Hello","avatar":{"large":"avatar"}}}}"""))
            server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
            server.enqueue(MockResponse().setBody("""{"data":{"Viewer":{"statistics":{"manga":{"chaptersRead":12}}}}}"""))
            server.enqueue(MockResponse().setBody("""{"data":{"Viewer":{"favourites":{"anime":{"nodes":[]}}}}}"""))
            val user=AniListApi(OkHttpClient(),server.url("/").toString()).profile(token="test-token")
            assertEquals("Bandit",user.name);assertEquals("Hello",user.about)
            assertEquals(0,user.anime.length());assertEquals(12,user.manga.getInt("chaptersRead"));assertEquals(1,user.warnings.size)
            val queries=(1..4).map {JSONObject(server.takeRequest().body.readUtf8()).getString("query")}
            assertFalse(queries[0].contains("statistics"));assertFalse(queries[1].contains("chaptersRead"));assertFalse(queries[2].contains("minutesWatched"))
        }finally{server.shutdown()}
    }
    @Test fun `song credits retain episode counts and are cached without duplicate requests`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"openings":["Opening (eps 1-12)"],"endings":["Ending (eps 2-11)"]}}"""))
            val api=MusicApi(OkHttpClient(),server.url("/v4").toString())
            val first=api.songs(123);val second=api.songs(123)
            assertEquals(first,second);assertEquals(1,server.requestCount)
            assertEquals("/v4/anime/123/themes",server.takeRequest().path)
            assertTrue(first.first.single().coverage.contains("12 episodes"));assertTrue(first.second.single().coverage.contains("10 episodes"))
        }finally{server.shutdown()}
    }
    @Test fun `rate limited songs are not cached or automatically hammered`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(429))
            val api=MusicApi(OkHttpClient(),server.url("/v4").toString())
            try {api.songs(123);fail("Expected rate limit")}catch(e:ApiException){assertTrue(e.message!!.contains("minute"))}
            assertEquals(1,server.requestCount)
            server.enqueue(MockResponse().setBody("""{"data":{"openings":[],"endings":[]}}"""))
            assertTrue(api.songs(123).first.isEmpty());assertEquals(2,server.requestCount)
        }finally{server.shutdown()}
    }
}
