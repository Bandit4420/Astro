package com.bandit.astro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

class QuizStore(context:Context,owner:Int) {
    private val prefs=context.getSharedPreferences("astro-quiz-$owner",Context.MODE_PRIVATE)
    fun records():JSONArray=try {JSONArray(prefs.getString("records","[]"))}catch(_:Exception){JSONArray()}
    fun add(correct:Int,total:Int,mode:String,difficulty:String,ids:List<Int>) {
        val all=records().objects()+JSONObject().put("at",System.currentTimeMillis()).put("correct",correct).put("total",total).put("mode",mode.lowercase()).put("difficulty",difficulty).put("universes",JSONArray(ids))
        prefs.edit().putString("records",JSONArray(all.takeLast(1000)).toString()).apply()
    }
    fun merge(incoming:JSONArray) {
        val valid=incoming.objects().filter {it.optLong("at") in 1..(System.currentTimeMillis()+60000) && it.optInt("total") in 1..10 && it.optInt("correct",-1) in 0..it.optInt("total") && it.optJSONArray("universes")!=null}
        val rows=(records().objects()+valid).distinctBy {it.optLong("at")}.sortedBy {it.optLong("at")}.takeLast(1000)
        prefs.edit().putString("records",JSONArray(rows).toString()).apply()
    }
    fun wins()=records().objects().count {it.optInt("correct")*10>=it.optInt("total")*7}
    fun mastered()=records().objects().filter {it.optInt("correct")==it.optInt("total")}.flatMap {it.getJSONArray("universes").let {a->(0 until a.length()).map {i->a.optInt(i)}}}.distinct().size
    fun streak():Int {
        val zone=ZoneId.systemDefault();val days=records().objects().map {Instant.ofEpochMilli(it.optLong("at")).atZone(zone).toLocalDate()}.toSet()
        var day=LocalDate.now(zone);if(day !in days) day=day.minusDays(1)
        var count=0;while(day in days){count++;day=day.minusDays(1)};return count
    }
}
