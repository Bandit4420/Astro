package com.bandit.astro.data

/** AniList markdown image variants; URLs stay data, never executable HTML. */
fun profileParts(value:String,revealSpoilers:Boolean=false):List<Pair<String,String>> {
    val source=value.replace(Regex("~!([\\s\\S]*?)!~")) {if(revealSpoilers) it.groupValues[1] else "[Spoiler hidden]"}
    val regex=Regex("""!\[[^]]*]\((https://[^\s)]+)(?:\s+["'][^"']*["'])?\)|img(?:\d+(?:%|px)?)?\((https://[^\s)]+)\)|<img\b[^>]*\bsrc\s*=\s*["'](https://[^"']+)["'][^>]*>""",RegexOption.IGNORE_CASE)
    val parts=mutableListOf<Pair<String,String>>();var cursor=0
    for(match in regex.findAll(source)) {
        if(match.range.first>cursor)parts+="text" to source.substring(cursor,match.range.first)
        parts+="image" to match.groupValues.drop(1).first {it.isNotBlank()}.replace("&amp;","&")
        cursor=match.range.last+1
    }
    if(cursor<source.length)parts+="text" to source.substring(cursor)
    return parts
}
