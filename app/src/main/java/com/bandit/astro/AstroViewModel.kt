package com.bandit.astro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bandit.astro.data.*
import com.bandit.astro.notifications.AiringNotifications
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*

data class Discovery(val catalog: Catalog = Catalog.ANIME, val search: String = "",
    val items: List<Media> = emptyList(), val page: Int = 0, val hasMore: Boolean = true,
    val loading: Boolean = false, val error: String? = null)
data class DetailState(val id: Int = 0, val value: MediaDetail? = null, val loading: Boolean = false, val error: String? = null)
data class PersonState(val id: Int, val staff: Boolean, val value: Person? = null, val error: String? = null)
data class ScheduleState(val items: List<Airing> = emptyList(), val loading: Boolean = false, val error: String? = null)

class AstroViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AstroApplication
    private val dao = app.database.dao()
    val collection = dao.observeCollection().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        viewModelScope.launch {
            app.preferences.flow.map { it.reminders }.distinctUntilChanged().collect { AiringNotifications.schedule(app, it) }
        }
    }
    fun search(value: String, catalog: Catalog = _discovery.value.catalog) {
        discoverJob?.cancel()
        _discovery.value = Discovery(catalog = catalog, search = value)
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
            val result = app.api.discover(old.catalog, old.search, page)
            val items = ((if (more) old.items else emptyList()) + result.media).distinctBy { it.id }.take(80)
            _discovery.value = old.copy(items = items, page = page, hasMore = result.hasNext && items.size < 80, loading = false)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _discovery.value = old.copy(loading = false, error = friendly(e)) }
    }
    fun open(id: Int) {
        detailJob?.cancel(); closePerson()
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
    fun closeDetail() { detailJob?.cancel(); closePerson(); _detail.value = DetailState() }
    fun openPerson(id: Int, staff: Boolean) {
        personJob?.cancel(); _person.value = PersonState(id, staff)
        personJob = viewModelScope.launch {
            try { _person.value = PersonState(id, staff, app.api.person(id, staff)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _person.value = PersonState(id, staff, error = friendly(e)) }
        }
    }
    fun closePerson() { personJob?.cancel(); _person.value = null }
    fun refreshSchedule(zone: ZoneId) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            _schedule.value = _schedule.value.copy(loading = true, error = null)
            try {
                val monday = LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val from = monday.atStartOfDay(zone).toEpochSecond()
                val to = monday.plusDays(7).atStartOfDay(zone).toEpochSecond()
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
    private fun write(action: suspend () -> Unit) { viewModelScope.launch {
        try { action() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _message.emit("Could not save that change. Please try again.") }
    } }
    fun add(media: Media) = write { dao.add(media.asEntry()); _message.emit("Added to your local collection") }
    fun increment(id: Int) = write { dao.increment(id) }
    fun decrement(id: Int) = write { dao.decrement(id) }
    fun remove(id: Int) = write { dao.remove(id); _message.emit("Removed from your local collection") }
    fun status(id: Int, value: String) = write { dao.status(id, value) }
    fun theme(value: String) = write { app.preferences.theme(value) }
    fun flag(name: String, value: Boolean) = write { app.preferences.flag(name, value) }
    private fun friendly(e: Exception) = if (e is ApiException) e.message ?: "AniList is unavailable." else "Couldn't connect. Check your connection and try again."
}
