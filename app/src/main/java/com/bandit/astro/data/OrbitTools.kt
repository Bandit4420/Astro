package com.bandit.astro.data

import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.ceil
import java.time.*

data class Song(val title: String, val coverage: String)
fun parseSongCredit(value: String): Song {
    val match=Regex("\\(\\s*(?:eps?\\.?|episodes?)\\s*:?\\s+([^)]*)\\)\\s*$",RegexOption.IGNORE_CASE).find(value) ?: return Song(value,"Episode coverage not listed")
    val raw=match.groupValues[1].trim(); val title=value.substring(0,match.range.first).trim()
    val ranges=raw.replace('–','-').replace('—','-').replace(Regex("\\band\\b",RegexOption.IGNORE_CASE),",").split(",").map {
        val m=Regex("^(\\d+)\\s*(?:-\\s*(\\d+))?$").matchEntire(it.trim()) ?: return Song(title,"Episodes $raw · Count unavailable")
        val a=m.groupValues[1].toLongOrNull() ?: return Song(title,"Episodes $raw · Count unavailable")
        val b=m.groupValues[2].ifBlank { m.groupValues[1] }.toLongOrNull() ?: return Song(title,"Episodes $raw · Count unavailable")
        if(a<1 || b<a || b>1000000) return Song(title,"Episodes $raw · Count unavailable")
        a to b
    }.sortedBy { it.first }
    var end=0L;var count=0L
    for((a,b) in ranges) { count+=(b-maxOf(a,end+1)+1).coerceAtLeast(0);end=maxOf(end,b) }
    return Song(title,"Episodes $raw · $count episode${if(count==1L) "" else "s"}")
}
data class QuizRound(val media: Media, val prompt: String, val choices: List<String>, val answer: String, val clues: List<String>)
fun quizRounds(pool: List<Media>, mode: String, hard: Boolean): List<QuizRound> {
    val unique=pool.distinctBy { it.title.lowercase() }
    if(unique.size<4) return emptyList()
    return unique.shuffled().take(10).map { m ->
        val others=unique.filter { it.id!=m.id }.shuffled().let { rows -> if(hard) rows.sortedByDescending { n -> n.genres.count { it in m.genres } } else rows }
        val clues=listOf("Genres: ${m.genres.joinToString()}","Debut: ${m.year.takeIf { it>0 } ?: "Unknown"}","Format: ${m.format}","Studios: ${m.studios.joinToString().ifBlank { "Not listed" }}")
        if(mode=="Trivia") {
            data class Question(val prompt:String,val answer:String,val decoys:List<String>)
            val questions=mutableListOf(Question("What format is this title?",m.format.ifBlank {m.type},listOf("TV","MOVIE","MANGA","NOVEL","OVA","ONA","ONE_SHOT")))
            if(m.year>0)questions+=Question("In which year did this entry first release?",m.year.toString(),listOf(m.year-2,m.year+1,m.year-5).map {it.toString()})
            if(m.genres.isNotEmpty())questions+=Question("Which genre is listed for this title?",m.genres.first(),listOf("Action","Adventure","Comedy","Drama","Fantasy","Horror","Music","Mystery","Romance","Sci-Fi","Sports").filter {it !in m.genres})
            val count=if(m.format=="NOVEL")m.volumes else m.total
            if(count>0)questions+=Question("How many ${if(m.format=="NOVEL") "volumes" else m.unit+"s"} are listed for this entry?",count.toString(),listOf(count+2,(count-3).coerceAtLeast(1),count+12,count+7).map {it.toString()})
            val q=questions.random();val choices=(listOf(q.answer)+q.decoys.filter {it!=q.answer}.distinct().shuffled().take(3)).shuffled()
            QuizRound(m,"${m.title} — ${q.prompt}",choices,q.answer,emptyList())
        } else QuizRound(m,if(mode=="Cover") "Which story is hiding in this cover?" else "Which title fits these clues?",(listOf(m.title)+others.take(3).map { it.title }).shuffled(),m.title,clues)
    }
}
fun compatibility(a: List<LibraryEntry>, b: List<LibraryEntry>): Pair<Int?,List<Pair<LibraryEntry,LibraryEntry>>> {
    val lookup=b.associateBy { it.mediaId }; val shared=a.mapNotNull { x -> lookup[x.mediaId]?.let { x to it } }
    val rated=shared.filter { it.first.score>0 && it.second.score>0 }
    return (if(rated.isEmpty()) null else (100-rated.map { kotlin.math.abs(it.first.score-it.second.score) }.average()).toInt()) to shared
}
fun backupJson(owner: Int, entries: List<LibraryEntry>, history: List<HistoryEvent>): String = JSONObject().put("format","astro-native").put("version",1).put("owner",owner)
    .put("entries",JSONArray(entries.filterNot { it.deleted }.map { e -> JSONObject().put("id",e.mediaId).put("title",e.title).put("cover",e.cover).put("type",e.type).put("format",e.format)
        .put("total",e.total).put("duration",e.duration).put("progress",e.progress).put("volumes",e.volumes).put("totalVolumes",e.totalVolumes).put("score",e.score)
        .put("notes",e.notes).put("status",e.status).put("genres",e.genres).put("studios",e.studios).put("updatedAt",e.updatedAt) }))
    .put("history",JSONArray(history.map { e -> JSONObject().put("id",e.id).put("mediaId",e.mediaId).put("at",e.at).put("delta",e.delta).put("unit",e.unit).put("minutes",e.minutes) })).toString(2)
fun parseBackup(text: String, owner: Int): Pair<List<LibraryEntry>,List<HistoryEvent>> {
    require(text.length<=10_000_000) { "Backup exceeds 10 MB." }
    val initial=JSONObject(text)
    val d=if(initial.optString("app")=="astro") JSONObject(fromWebBackup(text,owner)) else initial
    require(d.optString("format")=="astro-native" && d.optInt("version")==1) { "Use an Astro Android JSON backup." }
    require(d.getInt("owner")==owner) { "This backup belongs to a different account. Switch to its account before importing." }
    val rows=d.getJSONArray("entries");require(rows.length()<=20000)
    val entries=rows.objects().map { e ->
        val id=e.getInt("id");val progress=e.getInt("progress");val volumes=e.optInt("volumes");val score=e.optInt("score");val total=e.optInt("total");val totalVolumes=e.optInt("totalVolumes")
        require(id>0 && progress in 0..1000000 && volumes in 0..1000000 && score in 0..100 && total in 0..1000000 && totalVolumes in 0..1000000)
        val status=e.getString("status");require(status in listOf("CURRENT","PLANNING","COMPLETED","DROPPED","PAUSED","REPEATING"))
        val type=e.getString("type");require(type in listOf("ANIME","MANGA"))
        val cover=e.optString("cover");require(cover.isEmpty() || cover.startsWith("https://"))
        LibraryEntry(id,e.getString("title").take(1000),cover,type,e.optString("format"),total,e.optInt("duration").coerceIn(0,10000),progress,status,
            e.optLong("updatedAt",System.currentTimeMillis()),volumes,totalVolumes,score,e.optString("notes").take(10000),e.optString("genres"),e.optString("studios"))
    }
    val history=d.optJSONArray("history")?.objects().orEmpty().map { e ->
        val unit=e.getString("unit"); require(unit in listOf("episode","chapter","volume")); require(e.getInt("delta") in -1000000..1000000)
        HistoryEvent(e.getString("id"),e.getInt("mediaId"),e.getLong("at"),e.getInt("delta"),unit,e.optInt("minutes").coerceIn(-10000000,10000000))
    }
    return entries.distinctBy { it.mediaId } to history
}
fun calendarFile(rows: List<Airing>): String {
    fun stamp(time: Long)=java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(time))
    fun escape(s:String)=s.replace("\\","\\\\").replace("\n","\\n").replace("\r","").replace(",","\\,").replace(";","\\;")
    return (listOf("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Astro//Airing//EN")+rows.flatMap {
        listOf("BEGIN:VEVENT","UID:anilist-${it.id}@astro","DTSTAMP:${stamp(Instant.now().epochSecond)}","DTSTART:${stamp(it.airingAt)}",
            "DTEND:${stamp(it.airingAt+(it.media.duration.takeIf { n -> n>0 } ?: 24)*60)}","SUMMARY:${escape(it.media.title)} — Episode ${it.episode}","END:VEVENT")
    }+"END:VCALENDAR").joinToString("\r\n")
}
