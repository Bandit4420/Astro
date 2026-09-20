package com.bandit.astro.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class WebsiteParityTest {
    private fun entry(id:Int,type:String="ANIME",format:String="TV",title:String="Story",score:Int=0)=LibraryEntry(id,title,"",type,format,12,24,score=score)
    @Test fun `collection filters distinguish novels from manga and never show deleted entries`() {
        val rows=listOf(entry(1,title="Alpha"),entry(2,"MANGA","MANGA","alpha manga"),entry(3,"MANGA","NOVEL","Alpha novel"),entry(4,title="Alpha removed").copy(deleted=true))
        assertEquals(listOf(2),collectionView(rows,"Manga","ALL","ALPHA","Title A–Z").map {it.mediaId})
        assertEquals(listOf(3),collectionView(rows,"Novels","ALL","alpha","Highest score").map {it.mediaId})
        assertEquals(3,collectionView(rows,"All","ALL","","Recently updated").size)
        assertTrue(collectionView(rows,"All","COMPLETED","","Highest score").isEmpty())
    }
    @Test fun `recommendations omit both media and general spoiler tags`() {
        val m=AniListApi.parseMedia(JSONObject("""{"id":1,"tags":[{"name":"Found Family","isMediaSpoiler":false},{"name":"Secret identity","isMediaSpoiler":true},{"name":"Twist","isGeneralSpoiler":true}]}"""))
        assertEquals(listOf("Found Family"),m.tags)
        assertEquals(0,genreOverlap(m,m))
    }
    @Test fun `login identity does not depend on profile statistics or favorites`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"Viewer":{"id":42,"name":"Bandit","avatar":{"large":"avatar.jpg"}}}}"""))
            val user=AniListApi(OkHttpClient(),server.url("/").toString()).identity("test-token")
            val request=server.takeRequest();val query=JSONObject(request.body.readUtf8()).getString("query")
            assertEquals(42,user.id);assertEquals("Bearer test-token",request.getHeader("Authorization"))
            assertFalse(query.contains("statistics"));assertFalse(query.contains("favourites"))
        }finally{server.shutdown()}
    }
    @Test fun `default discovery omits absent filters instead of sending null arguments`() {
        val(q,v)=discoveryRequest(Catalog.ANIME,"",1,ExploreFilters())
        assertFalse(q.contains("format_not_in"));assertFalse(q.contains("search:"));assertFalse(q.contains("format:"))
        assertFalse(q.contains("episodes_lesser"));assertTrue(q.contains("sort:"))
        assertFalse(v.has("excluded"));assertEquals("ANIME",v.getString("type"))
    }
    @Test fun `manga excludes novels and selected filters remain parameterized`() {
        val(q,v)=discoveryRequest(Catalog.MANGA,"Quoted \"title\"",2,ExploreFilters(genre="Fantasy",minScore="80"))
        assertTrue(q.contains("format_not_in:"));assertFalse(q.contains("Quoted"))
        assertEquals("NOVEL",v.getJSONArray("excluded").getString(0));assertEquals(79,v.getInt("score"))
        assertEquals("Fantasy",v.getString("genre"))
    }
    @Test fun `access denial is not presented as expired login`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
            try {AniListApi(OkHttpClient(),server.url("/").toString()).discover(Catalog.ANIME,"",1);fail("Expected access denial")}
            catch(e:ApiException){assertFalse(e.retryable);assertTrue(e.message!!.contains("access restriction"));assertFalse(e.message!!.contains("expired"))}
        }finally{server.shutdown()}
    }
    @Test fun `cast query sends requested language and preserves multiple actors`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"Media":{"characters":{"pageInfo":{"hasNextPage":true},"edges":[{"role":"MAIN","node":{"id":1,"name":{"full":"Hero"},"image":{"large":""}},"voiceActors":[{"id":2,"name":{"full":"Adult voice"}},{"id":3,"name":{"full":"Young voice"}}]}]}}}}"""))
            val(rows,more)=AniListApi(OkHttpClient(),server.url("/").toString()).cast(10,2,"ENGLISH")
            val request=JSONObject(server.takeRequest().body.readUtf8())
            assertEquals("ENGLISH",request.getJSONObject("variables").getString("language"))
            assertEquals(listOf(2,3),rows.first().actors.map {it.id});assertTrue(more)
        }finally{server.shutdown()}
    }
    @Test fun `discovery respects provider pagination past eighty titles`() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"data":{"Page":{"media":[{"id":81}],"pageInfo":{"hasNextPage":true}}}}"""))
            val result=AniListApi(OkHttpClient(),server.url("/").toString()).discover(Catalog.ANIME,"",5)
            assertEquals(81,result.media.single().id);assertTrue(result.hasNext)
            assertEquals(5,JSONObject(server.takeRequest().body.readUtf8()).getJSONObject("variables").getInt("page"))
        }finally{server.shutdown()}
    }
}
