package com.bandit.astro

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONObject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bandit.astro.data.*
import com.bandit.astro.notifications.AiringNotifications
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*

data class Discovery(val catalog: Catalog = Catalog.ANIME, val search: String = "",
    val items: List<Media> = emptyList(), val page: Int = 0, val hasMore: Boolean = true,
    val loading: Boolean = false, val error: String? = null, val filters: ExploreFilters = ExploreFilters())
data class DetailState(val id: Int = 0, val value: MediaDetail? = null, val loading: Boolean = false, val error: String? = null, val filters: ExploreFilters = ExploreFilters())
data class PersonState(val id: Int, val staff: Boolean, val value: Person? = null, val error: String? = null)
data class ScheduleState(val items: List<Airing> = emptyList(), val loading: Boolean = false, val error: String? = null, val filters: ExploreFilters = ExploreFilters())

class AstroViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AstroApplication
    private val dao get() = app.database.dao()
    val account = app.account.session
    val syncState = app.sync.state
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val collection = account.map { it?.id ?: 0 }.distinctUntilChanged().flatMapLatest { id -> app.databaseFor(id).dao().observeCollection().onStart { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val history = account.map { it?.id ?: 0 }.distinctUntilChanged().flatMapLatest { id -> app.databaseFor(id).dao().observeHistory().onStart { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val profile = MutableStateFlow<UserProfile?>(null)
    val profileError = MutableStateFlow("")
    val spotlight = MutableStateFlow<List<Media>>(emptyList())
    val quizPool = MutableStateFlow<List<Media>>(emptyList())
    val quizLoading = MutableStateFlow(false)
    val suggestions = MutableStateFlow<Pair<String,List<Media>>>("" to emptyList())
    val songs = MutableStateFlow<Pair<List<Song>,List<Song>>?>(null)
    val songsError = MutableStateFlow("")
    val songsLoading = MutableStateFlow(false)
    val studios = MutableStateFlow<List<Pair<Int,String>>>(emptyList())
    val comparison = MutableStateFlow<Pair<UserProfile,List<LibraryEntry>>?>(null)
    val comparisonError = MutableStateFlow("")
    private var songsJob: Job? = null
    private var profileJob: Job? = null
    private var studioJob: Job? = null
    private var compareJob: Job? = null
    private var castJob: Job? = null
    val castLoading = MutableStateFlow(false)
    val preferences = app.preferences.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Preferences())
    private val _discovery = MutableStateFlow(Discovery())
    val discovery = _discovery.asStateFlow()
    private val _detail = MutableStateFlow(DetailState())
    val detail = _detail.asStateFlow()
    private val _person = MutableStateFlow<PersonState?>(null)
    val person = _person.asStateFlow()
    private val _schedule = MutableStateFlow(ScheduleState())
    val schedule = _schedule.asStateFlow()
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _message.asSharedFlow()
    private var discoverJob: Job? = null
    private var detailJob: Job? = null
    private var personJob: Job? = null
    private var scheduleJob: Job? = null
    private val detailCache = object : LinkedHashMap<Int, MediaDetail>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, MediaDetail>?) = size > 12
    }
    init {
        loadDiscovery()
        viewModelScope.launch { try { spotlight.value = app.api.discover(Catalog.ANIME,"",1).media } catch (_: Exception) {} }
        viewModelScope.launch { combine(account, app.preferences.flow) { session,p -> (session?.id ?: 0) to p.autoSync }.distinctUntilChanged().collect { (_, enabled) -> AutoSync.schedule(app,enabled) } }
        viewModelScope.launch { account.map { it?.id }.distinctUntilChanged().collect { profile.value=null; profileError.value=""; comparison.value=null; suggestions.value="" to emptyList(); closeDetail(); refreshProfile() } }
        viewModelScope.launch {
            app.preferences.flow.map { it.reminders }.distinctUntilChanged().collect { AiringNotifications.schedule(app, it) }
        }
    }
    fun search(value: String, catalog: Catalog = _discovery.value.catalog) {
        discoverJob?.cancel()
        _discovery.value = Discovery(catalog = catalog, search = value, filters = if(catalog==_discovery.value.catalog) _discovery.value.filters else ExploreFilters())
        discoverJob = viewModelScope.launch { delay(350); fetchDiscovery(false) }
    }
    fun loadDiscovery(more: Boolean = false) {
        if (_discovery.value.loading) return
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch { fetchDiscovery(more) }
    }
    private suspend fun fetchDiscovery(more: Boolean) {
        val old = _discovery.value
        if (more && (!old.hasMore || old.items.size >= 80)) return
        _discovery.value = old.copy(loading = true, error = null)
        try {
            val page = if (more) old.page + 1 else 1
            val result = if(old.filters.studioId>0) app.api.studio(old.filters.studioId,page,old.search,old.filters) else app.api.discover(old.catalog, old.search, page, old.filters)
            val items = ((if (more) old.items else emptyList()) + result.media).distinctBy { it.id }.take(80)
            _discovery.value = old.copy(items = items, page = page, hasMore = result.hasNext && items.size < 80, loading = false)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _discovery.value = old.copy(loading = false, error = friendly(e)) }
    }
    fun open(id: Int) {
        detailJob?.cancel(); castJob?.cancel(); castLoading.value=false; songsJob?.cancel(); songs.value=null; songsError.value=""; songsLoading.value=false; closePerson()
        _detail.value = DetailState(id, detailCache[id], loading = detailCache[id] == null)
        detailJob = viewModelScope.launch {
            try {
                val result = app.api.detail(id)
                detailCache[id] = result
                _detail.value = DetailState(id, result)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _detail.value = _detail.value.copy(loading = false, error = friendly(e)) }
        }
    }
    fun closeDetail() { detailJob?.cancel(); castJob?.cancel(); castLoading.value=false; songsJob?.cancel(); songs.value=null; songsError.value=""; songsLoading.value=false; castJob?.cancel();songsJob?.cancel(); closePerson(); _detail.value = DetailState() }
    fun openPerson(id: Int, staff: Boolean) {
        personJob?.cancel(); _person.value = PersonState(id, staff)
        personJob = viewModelScope.launch {
            try { _person.value = PersonState(id, staff, app.api.person(id, staff)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _person.value = PersonState(id, staff, error = friendly(e)) }
        }
    }
    fun closePerson() { personJob?.cancel(); _person.value = null }
    fun refreshSchedule(zone: ZoneId, offset: Int = 0) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            _schedule.value = _schedule.value.copy(loading = true, error = null)
            try {
                val monday = LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val from = monday.plusWeeks(offset.toLong()).atStartOfDay(zone).toEpochSecond()
                val to = monday.plusWeeks(offset.toLong()).plusDays(7).atStartOfDay(zone).toEpochSecond()
                val items = mutableListOf<Airing>()
                var page = 1
                do {
                    val (batch, more) = app.api.schedule(from - 1, to, page)
                    items += batch
                    page++
                } while (more)
                _schedule.value = ScheduleState(items.distinctBy { it.id }.sortedBy { it.airingAt })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _schedule.value = _schedule.value.copy(loading = false, error = friendly(e)) }
        }
    }
    fun announce(text: String) { _message.tryEmit(text) }
    private fun task(action: suspend () -> Unit) { viewModelScope.launch {
        try { action() } catch(e: CancellationException) { throw e } catch(e: Exception) { announce(e.message ?: "Could not finish. Please try again.") }
    } }
    private fun editEntry(id: Int, unit: String = "", change: (LibraryEntry)->LibraryEntry) {
        val owner=account.value?.id ?: 0; val db=app.databaseFor(owner)
        task {
            db.withTransaction {
                val old=db.dao().entry(id) ?: return@withTransaction
                val next=change(old).copy(updatedAt=System.currentTimeMillis(),dirty=true,revision=old.revision+1)
                db.dao().put(next)
                val delta=if(unit=="volume") next.volumes-old.volumes else next.progress-old.progress
                if(unit.isNotBlank() && delta!=0) db.dao().event(HistoryEvent(mediaId=id,delta=delta,unit=if(old.type=="ANIME") "episode" else unit, minutes=if(old.type=="ANIME") delta*old.duration else 0))
            }
            if((account.value?.id ?: 0)==owner) AutoSync.schedule(app,app.preferences.flow.first().autoSync,true)
        }
    }
    fun add(media: Media) { val owner=account.value?.id ?: 0; task {
        val d=app.databaseFor(owner).dao();val existing=d.entry(media.id)
        if(existing==null) d.add(media.asEntry()) else if(existing.deleted) d.put(existing.copy(deleted=false,dirty=true,revision=existing.revision+1))
        announce("Added to your collection"); if((account.value?.id ?: 0)==owner) AutoSync.schedule(app,app.preferences.flow.first().autoSync,true)
    } }
    fun increment(id:Int)=editEntry(id,"chapter") { e -> val p=nextProgress(e.progress,e.total); e.copy(progress=p,status=if(e.total>0 && p>=e.total) "COMPLETED" else "CURRENT") }
    fun decrement(id:Int)=editEntry(id,"chapter") { it.copy(progress=(it.progress-1).coerceAtLeast(0),status=if(it.status=="COMPLETED") "CURRENT" else it.status) }
    fun volumes(id:Int,amount:Int)=editEntry(id,"volume") { it.copy(volumes=(it.volumes+amount).coerceIn(0,if(it.totalVolumes>0) it.totalVolumes else 1000000)) }
    fun score(id:Int,value:Int)=editEntry(id) { it.copy(score=value.coerceIn(0,100)) }
    fun remove(id:Int)=editEntry(id) { it.copy(deleted=true) }
    fun status(id:Int,value:String)=editEntry(id,"chapter") { it.copy(status=value,progress=if(value=="COMPLETED" && it.total>0) it.total else it.progress) }
    fun notes(id:Int,value:String) { val db=app.database; task { db.withTransaction { db.dao().entry(id)?.let { db.dao().put(it.copy(notes=value.take(10000))) } } } }
    fun theme(value:String)=task { app.preferences.theme(value) }
    fun flag(name:String,value:Boolean)=task { app.preferences.flag(name,value) }
    fun number(name:String,value:Float)=task { app.preferences.number(name,value) }
    fun filter(value:ExploreFilters) { discoverJob?.cancel(); _discovery.value=Discovery(catalog=_discovery.value.catalog,search=_discovery.value.search,filters=value);loadDiscovery() }
    fun studioSearch(value:String) { studioJob?.cancel(); studioJob=viewModelScope.launch { delay(400);try { studios.value=if(value.trim().length<2) emptyList() else app.api.studios(value.trim()) } catch(e:CancellationException){throw e} catch(_:Exception){studios.value=emptyList()} } }
    fun clientId()=app.account.clientId
    fun beginLogin(id:String): Uri { app.account.clientId=id.trim(); return app.account.begin() }
    fun finishLogin(uri:Uri) = task {
        val response=app.account.callback(uri)
        val viewer=app.api.viewer(response.token)
        app.preferences.flag("autoSync",false)
        app.account.save(Session(viewer.id,viewer.name,viewer.avatar,response.token,response.expiresAt))
        profile.value=viewer; announce("Welcome, ${viewer.name}. Tap Sync to import your AniList collection.")
    }
    fun logout() { app.account.logout(); AutoSync.schedule(app,false); app.sync.state.value=SyncState(); announce("Signed out. Your account library remains stored separately.") }
    fun sync(forceId:Int=0,useRemote:Boolean=false)=task { app.sync.run(forceId,useRemote);refreshProfile() }
    fun refreshProfile() {
        profileJob?.cancel();val s=account.value ?: return
        profileJob=viewModelScope.launch { try { val result=app.api.viewer(s.token); if(account.value?.id==s.id) { profile.value=result;profileError.value="" } }
            catch(e:CancellationException){throw e} catch(e:Exception){if(account.value?.id==s.id) profileError.value=e.message ?: "Could not load profile."} }
    }
    fun moreCast() { val d=_detail.value.value ?: return; if(castLoading.value||!d.moreCast)return
        castLoading.value=true; castJob=viewModelScope.launch { try { val (rows,more)=app.api.cast(d.media.id,d.castPage+1)
            if(_detail.value.id==d.media.id) _detail.value=_detail.value.copy(value=d.copy(characters=(d.characters+rows).distinctBy { it.id },castPage=d.castPage+1,moreCast=more))
        } catch(e:CancellationException){throw e} catch(e:Exception){announce(e.message ?: "Could not load more characters.")} finally {castLoading.value=false} }
    }
    fun expandFranchise() {val d=_detail.value.value ?: return
        task {val relations=app.api.expandRelations(d.relations.map {it.media.id}.take(40));if(_detail.value.id==d.media.id)_detail.value=_detail.value.copy(value=_detail.value.value?.copy(relations=(d.relations+relations).distinctBy {it.media.id}));announce("Connected titles expanded")}
    }
    fun moreWorks() { val s=_person.value ?: return;val p=s.value ?: return; if(!p.more)return
        personJob?.cancel();personJob=viewModelScope.launch { try { val n=app.api.person(s.id,s.staff,p.page+1);_person.value=s.copy(value=n.copy(works=(p.works+n.works).distinctBy { it.id })) }
        catch(e:CancellationException){throw e} catch(e:Exception){announce(e.message ?: "Could not load works.")} }
    }
    fun loadSongs() { val id=_detail.value.value?.media?.malId ?: 0; if(id<=0||songsLoading.value)return
        songsLoading.value=true;songsError.value="";songsJob=viewModelScope.launch { try {
            val data=withContext(Dispatchers.IO) { app.http.newCall(okhttp3.Request.Builder().url("https://api.jikan.moe/v4/anime/$id/themes").build()).execute().use {
                if(!it.isSuccessful) throw ApiException(if(it.code==429) "Song credits are busy. Retry in a minute." else "Could not load song credits.")
                JSONObject(it.body?.string() ?: "{}").getJSONObject("data")
            } }
            fun rows(key:String)=data.getJSONArray(key).let { a -> (0 until a.length()).map { parseSongCredit(a.getString(it)) } }
            songs.value=rows("openings") to rows("endings")
        } catch(e:CancellationException){throw e} catch(e:Exception){songsError.value=e.message ?: "Could not load songs."} finally {songsLoading.value=false} }
    }
    fun recommend() { val entry=collection.value.filter { it.status=="COMPLETED" || it.score>=70 }.maxByOrNull { it.score } ?: return
        task { val owner=account.value?.id; val result=app.api.detail(entry.mediaId); if(account.value?.id != owner) return@task; suggestions.value=entry.title to result.recommendations.filter { m -> collection.value.none { it.mediaId==m.id } } }
    }
    fun compare(name:String) { compareJob?.cancel();comparison.value=null;comparisonError.value="";val owner=account.value?.id ?: 0
        compareJob=viewModelScope.launch { try { val p=app.api.profile(name.trim());val entries=app.api.remoteList(p.id);if((account.value?.id ?: 0)==owner) comparison.value=p to entries.map { it.entry } }
            catch(e:CancellationException){throw e} catch(e:Exception){comparisonError.value=e.message ?: "Could not compare this public profile."} }
    }
    fun loadQuizPool(catalog:Catalog) { task { quizLoading.value=true;try {quizPool.value=app.api.discover(catalog,"",1).media} finally {quizLoading.value=false} } }
    suspend fun backup(web:Boolean=false):String { val owner=account.value?.id ?: 0;val db=app.databaseFor(owner)
        val data=db.withTransaction { JSONObject(backupJson(owner,db.dao().entries(),db.dao().history())).put("quiz",QuizStore(app,owner).records()).toString(2) }
        return if(web) toWebBackup(data) else data
    }
    fun importBackup(text:String)=task {
        val owner=account.value?.id ?: 0;val (entries,events)=parseBackup(text,owner);val db=app.databaseFor(owner)
        app.preferences.flag("autoSync",false)
        db.withTransaction { for(e in entries) { val old=db.dao().entry(e.mediaId); if(old==null) db.dao().put(e) else if(old.notes.isBlank() && e.notes.isNotBlank()) db.dao().put(old.copy(notes=e.notes)) }; for(e in events) db.dao().event(e) }
        val imported=JSONObject(if(JSONObject(text).optString("app")=="astro") fromWebBackup(text,owner) else text)
        QuizStore(app,owner).merge(imported.optJSONArray("quiz") ?: org.json.JSONArray())
        announce("Imported missing titles, notes, quiz and history. Existing progress kept; auto-sync is off for review.")
    }
    private fun friendly(e:Exception)=if(e is ApiException) e.message ?: "AniList is unavailable." else "Couldn't connect. Check your connection and try again."
}
