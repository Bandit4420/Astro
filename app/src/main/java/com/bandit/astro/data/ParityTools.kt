package com.bandit.astro.data

import org.json.JSONArray

internal fun JSONArray?.strings(): List<String> = if(this==null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() && it!="null" }
internal fun parseCredits(rows: JSONArray?): List<Credit> = rows?.objects()?.map { Credit(it.getInt("id"),it.optJSONObject("name")?.text("full").orEmpty(),it.optJSONObject("image")?.text("large").orEmpty(),"") }.orEmpty()
fun collectionView(entries:List<LibraryEntry>,catalog:String,status:String,search:String,sort:String):List<LibraryEntry> {
    val rows=entries.filter { !it.deleted && (catalog=="All" || when(catalog){"Anime"->it.type=="ANIME";"Novels"->it.format=="NOVEL";else->it.type=="MANGA"&&it.format!="NOVEL"}) && (status=="ALL"||it.status==status) && it.title.contains(search.trim(),true) }
    return when(sort){"Title A–Z"->rows.sortedBy { it.title.lowercase() };"Highest score"->rows.sortedByDescending { it.score };else->rows.sortedByDescending { it.updatedAt }}
}
fun genreOverlap(a:Media,b:Media):Int {val union=(a.genres+b.genres).distinct();return if(union.isEmpty())0 else a.genres.intersect(b.genres.toSet()).size*100/union.size}
fun recommendationReasons(a:Media,b:Media):List<String> = (a.tags.intersect(b.tags.toSet())+a.genres.intersect(b.genres.toSet())).take(5)
