package com.bandit.astro.data

import org.json.JSONArray
import org.json.JSONObject

/** Converts the website's version-2 backup to the validated native interchange format. */
fun fromWebBackup(text:String,owner:Int):String {
    val d=JSONObject(text)
    require(d.optString("app")=="astro" && d.optInt("version")==2) {"Choose an Astro website version-2 or Android backup."}
    val sourceOwner=if(d.isNull("accountId")) 0 else d.getInt("accountId")
    require(sourceOwner==owner) {"This website backup belongs to a different account. Sign in to its account first."}
    val orbit=d.optJSONObject("orbit") ?: JSONObject();val notes=orbit.optJSONObject("notes") ?: JSONObject()
    val entries=d.getJSONArray("entries").objects().map {e->val media=e.getJSONObject("media");val title=media.getJSONObject("title");val id=media.getInt("id");val type=media.optString("type","ANIME")
        val cover=media.optJSONObject("coverImage")?.text("large").orEmpty()
        val score=e.optDouble("score",0.0);require(score.isFinite() && score in 0.0..10.0)
        JSONObject().put("id",id).put("title",title.text("english").ifBlank {title.text("romaji")}).put("type",type).put("format",media.text("format"))
            .put("cover",if(cover.startsWith("https://")) cover else "").put("total",media.optInt(if(type=="ANIME")"episodes" else "chapters"))
            .put("duration",media.optInt("duration")).put("progress",e.getInt("progress")).put("volumes",e.optInt("progressVolumes"))
            .put("totalVolumes",media.optInt("volumes")).put("score",kotlin.math.round(score*10).toInt()).put("notes",notes.optString(id.toString(),""))
            .put("status",e.getString("status")).put("genres",media.optJSONArray("genres")?.let {a->(0 until a.length()).map {a.getString(it)}.joinToString("|")}.orEmpty())
            .put("studios",media.optJSONObject("studios")?.optJSONArray("nodes")?.objects()?.joinToString("|"){it.text("name")}.orEmpty())
    }
    val history=orbit.optJSONArray("history")?.objects().orEmpty().map {e->JSONObject(e.toString()).put("unit",e.getString("kind"))}
    return JSONObject().put("format","astro-native").put("version",1).put("owner",owner).put("entries",JSONArray(entries)).put("history",JSONArray(history))
        .put("quiz",orbit.optJSONArray("quiz") ?: JSONArray()).toString()
}
fun toWebBackup(native:String):String {
    val d=JSONObject(native);val notes=JSONObject()
    val entries=d.getJSONArray("entries").objects().map {e->val id=e.getInt("id");notes.put(id.toString(),e.text("notes"));val type=e.getString("type")
        val media=JSONObject().put("id",id).put("title",JSONObject().put("romaji",e.getString("title")).put("english",e.getString("title")))
            .put("coverImage",JSONObject().put("large",e.text("cover"))).put("type",type).put("format",e.text("format"))
            .put(if(type=="ANIME")"episodes" else "chapters",e.getInt("total")).put("volumes",e.optInt("totalVolumes")).put("duration",e.optInt("duration"))
            .put("genres",JSONArray(e.text("genres").split("|").filter(String::isNotBlank)))
            .put("studios",JSONObject().put("nodes",JSONArray()))
        JSONObject().put("media",media).put("progress",e.getInt("progress")).put("progressVolumes",e.optInt("volumes")).put("score",e.optInt("score")/10.0).put("status",e.getString("status")).put("local",true)
    }
    val history=d.optJSONArray("history")?.objects().orEmpty().map {e->JSONObject(e.toString()).put("kind",e.getString("unit"))}
    return JSONObject().put("app","astro").put("version",2).put("accountId",d.getInt("owner").takeIf {it>0} ?: JSONObject.NULL)
        .put("exportedAt",java.time.Instant.now().toString()).put("entries",JSONArray(entries)).put("orbit",JSONObject().put("version",1).put("notes",notes).put("history",JSONArray(history))
            .put("quiz",d.optJSONArray("quiz") ?: JSONArray()).put("deleted",JSONArray()).put("spoilers",true).put("reminders",false)).toString(2)
}
