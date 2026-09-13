package com.bandit.astro.data

import org.junit.Assert.*
import org.junit.Test

class ParityPolicyTest {
    private fun entry()=LibraryEntry(1,"Frieren","https://example.com/cover.jpg","ANIME","TV",28,24,progress=10,remoteUpdated=100)
    @Test fun `OAuth checks state and expiry before accepting a token`() {
        val values=mapOf("state" to "nonce","access_token" to "example-token","token_type" to "Bearer","expires_in" to "3600")
        assertEquals(3_601_000,validateOAuth(values,"nonce",0,1000).expiresAt)
        for((state,now) in listOf("different" to 1000L,"nonce" to 900001L,"nonce" to -1L)) {
            try {validateOAuth(values,state,0,now);fail("Untrusted response accepted")}catch(_:IllegalArgumentException){}
        }
        try {validateOAuth(values+ ("expires_in" to "0"),"nonce",0,1000);fail("Expired token accepted")}catch(_:IllegalArgumentException){}
    }
    @Test fun `remote edits conflict but acknowledged identical progress does not`() {
        val local=entry()
        assertTrue(syncConflict(local,local.copy(progress=12,remoteUpdated=101,dirty=false)))
        assertFalse(syncConflict(local,local.copy(remoteUpdated=101,dirty=false)))
        assertTrue(syncConflict(local,null))
        assertFalse(syncConflict(local.copy(remoteUpdated=0),null))
        assertTrue(syncConflict(local.copy(deleted=true),local.copy(remoteUpdated=101)))
    }
    @Test fun `backup is account bound and contains no token`() {
        val text=backupJson(44,listOf(entry().copy(notes="Private thought",volumes=2)),listOf(HistoryEvent(mediaId=1,delta=1,unit="episode")))
        val (rows,events)=parseBackup(text,44)
        assertEquals("Private thought",rows.single().notes);assertEquals(2,rows.single().volumes);assertEquals(1,events.size)
        assertFalse(text.contains("token"));assertTrue(rows.single().dirty)
        try {parseBackup(text,99);fail("Cross-account import accepted")}catch(_:IllegalArgumentException){}
    }
    @Test fun `song ranges count their union and never invent coverage`() {
        assertEquals("Episodes 1-5, 4-8, 10 · 9 episodes",parseSongCredit("Song (eps 1-5, 4-8, 10)").coverage)
        assertEquals("Episode coverage not listed",parseSongCredit("Song").coverage)
        assertTrue(parseSongCredit("Song (eps 8-2)").coverage.contains("unavailable"))
    }
    @Test fun `quiz choices have exactly one answer even with duplicate titles`() {
        val media=(1..20).map {Media(it,"Title $it",format="TV",genres=listOf("Fantasy"))}
        for(mode in listOf("Cover","Clues","Trivia")) {
            val rounds=quizRounds(media+media.first(),mode,true)
            assertEquals(10,rounds.size)
            rounds.forEach {assertEquals(4,it.choices.size);assertEquals(4,it.choices.distinct().size);assertEquals(1,it.choices.count {s->s==it.answer})}
        }
    }
    @Test fun `compatibility excludes unrated titles`() {
        val a=listOf(entry().copy(score=80),entry().copy(mediaId=2,score=0))
        val b=listOf(entry().copy(score=60),entry().copy(mediaId=2,score=100))
        val (score,shared)=compatibility(a,b)
        assertEquals(80,score);assertEquals(2,shared.size)
        assertNull(compatibility(a.map {it.copy(score=0)},b).first)
    }
    @Test fun `backup refuses unsafe image schemes and invalid progress`() {
        val backup=backupJson(0,listOf(entry()),emptyList())
        for(invalid in listOf(backup.replace("https://example.com/cover.jpg","file:///private"),backup.replace("\"progress\": 10","\"progress\": -1"))) {
            try{parseBackup(invalid,0);fail("Invalid backup accepted")}catch(_:IllegalArgumentException){}
        }
    }
    @Test fun `website backups preserve account notes progress and ratings`() {
        val original=backupJson(44,listOf(entry().copy(score=85,notes="Keep me")),emptyList())
        val web=toWebBackup(original);val imported=parseBackup(web,44).first.single()
        assertEquals(85,imported.score);assertEquals(10,imported.progress);assertEquals("Keep me",imported.notes)
    }
    @Test fun `profile images support AniList widths and hide marked spoilers`() {
        val source="img300(https://example.com/a.png) img50%(https://example.com/b.png) ![c](https://example.com/c.png) ~!img(https://example.com/spoiler.png)!~"
        assertEquals(3,profileParts(source).count {it.first=="image"})
        assertEquals(4,profileParts(source,true).count {it.first=="image"})
        assertTrue(profileParts(source).any {it.second.contains("[Spoiler hidden]")})
        assertEquals(0,profileParts("img(file:///private)").count {it.first=="image"})
    }
}
