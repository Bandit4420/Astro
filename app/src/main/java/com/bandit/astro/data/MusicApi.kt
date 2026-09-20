package com.bandit.astro.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

typealias SongCredits = Pair<List<Song>,List<Song>>

/** On-demand public credits; one request at a time, bounded in-memory cache, no polling. */
class MusicApi(private val client:OkHttpClient, private val endpoint:String="https://api.jikan.moe/v4") {
    private val lock=Mutex()
    private val cache=object:LinkedHashMap<Int,Pair<Long,SongCredits>>(24,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Int,Pair<Long,SongCredits>>?)=size>24
    }
    suspend fun songs(malId:Int):SongCredits = lock.withLock {
        require(malId>0)
        cache[malId]?.takeIf {System.currentTimeMillis()-it.first < 6*60*60*1000L}?.let {return@withLock it.second}
        val text=suspendCancellableCoroutine<String> {continuation->
            val call=client.newCall(Request.Builder().url("$endpoint/anime/$malId/themes").header("Accept","application/json").build())
            continuation.invokeOnCancellation {call.cancel()}
            call.enqueue(object:Callback {
                override fun onFailure(call:Call,e:IOException) {if(continuation.isActive)continuation.resumeWithException(e)}
                override fun onResponse(call:Call,response:Response) {response.use {
                    if(!continuation.isActive)return
                    if(it.isSuccessful)continuation.resume(it.body?.string().orEmpty())
                    else continuation.resumeWithException(ApiException(when(it.code) {
                        429->"Song credits are rate-limited. Please retry in a minute."
                        404->"No song credits are published for this title yet."
                        403->"The song provider denied this request (403)."
                        else->"Song credits could not load (${it.code}). Tap Retry."
                    },it.code==429||it.code>=500))
                }}
            })
        }
        val data=JSONObject(text).optJSONObject("data") ?: throw ApiException("The song provider returned no credits. Tap Retry.")
        fun rows(key:String)=data.optJSONArray(key)?.let {a->(0 until a.length()).map {parseSongCredit(a.getString(it))}}.orEmpty()
        (rows("openings") to rows("endings")).also {cache[malId]=System.currentTimeMillis() to it}
    }
}
