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
