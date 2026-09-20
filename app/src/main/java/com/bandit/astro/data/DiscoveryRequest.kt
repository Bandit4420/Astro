package com.bandit.astro.data

import org.json.JSONArray
import org.json.JSONObject

// Send only selected filters to the resolver. Values always remain variables.
internal fun discoveryRequest(catalog:Catalog,search:String,page:Int,filters:ExploreFilters):Pair<String,JSONObject> {
    val variables=filters.variables().put("page",page).put("type",if(catalog==Catalog.ANIME) "ANIME" else "MANGA")
    if(search.isNotBlank())variables.put("search",search.trim())
    val format=if(catalog==Catalog.NOVEL) "NOVEL" else filters.format
    if(format.isNotBlank())variables.put("format",format)
    if(catalog==Catalog.MANGA)variables.put("excluded",JSONArray().put("NOVEL"))
    val options=listOf(
        Triple("type","MediaType","type"),Triple("search","String","search"),
        Triple("format","MediaFormat","format"),Triple("excluded","[MediaFormat]","format_not_in"),
        Triple("genre","String","genre"),Triple("tag","String","tag"),Triple("year","String","startDate_like"),
        Triple("season","MediaSeason","season"),Triple("status","MediaStatus","status"),
        Triple("score","Int","averageScore_greater"),Triple("episodes","Int","episodes_lesser"),
        Triple("minEpisodes","Int","episodes_greater"),Triple("sort","[MediaSort]","sort")
    ).filter {variables.has(it.first)&&!variables.isNull(it.first)}
    val definitions=(listOf("${'$'}page:Int")+options.map {(key,type,_)->"${'$'}$key:$type"}).joinToString(",")
    val arguments=(listOf("isAdult:false")+options.map {(key,_,argument)->"$argument:${'$'}$key"}).joinToString(",")
    val query="query($definitions) { Page(page:${'$'}page,perPage:20) { pageInfo { hasNextPage } media($arguments) { ${AniListApi.MEDIA_FIELDS} } } }"
    return query to variables
}
