@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bandit.astro.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bandit.astro.*
import com.bandit.astro.data.*
import com.bandit.astro.notifications.AiringNotifications
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.*
import java.time.format.DateTimeFormatter

private data class Destination(val name: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Orbit", Icons.Rounded.Home), Destination("Explore", Icons.Rounded.Search),
    Destination("Collection", Icons.Rounded.List), Destination("Schedule", Icons.Rounded.DateRange),
    Destination("Settings", Icons.Rounded.Settings)
)

@Composable
fun AstroApp(model: AstroViewModel, notificationId: Int, consumed: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val ownedLibrary by model.ownedCollection.collectAsStateWithLifecycle()
    val discovery by model.discovery.collectAsStateWithLifecycle()
    val detail by model.detail.collectAsStateWithLifecycle()
    val person by model.person.collectAsStateWithLifecycle()
    val schedule by model.schedule.collectAsStateWithLifecycle()
    val account by model.account.collectAsStateWithLifecycle()
    val library=ownedLibrary.takeIf {it.owner == (account?.id ?: 0)}?.entries.orEmpty()
    var extra by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(account?.id) { extra="" }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(notificationId) { if (notificationId > 0) { model.open(notificationId); consumed() } }
    val accent = if (detail.id > 0) detail.value?.media?.accent.orEmpty() else ""
    AstroTheme(preferences, accent) {
        BackHandler(enabled = person != null || detail.id > 0 || tab != 0 || extra.isNotBlank()) {
            when { person != null -> model.closePerson(); detail.id > 0 -> model.closeDetail(); extra.isNotBlank() -> extra=""; else -> tab = 0 }
        }
        Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(preferences,detail.value?.media?.banner.orEmpty())
        Scaffold(containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) { ChibiSnackbar(it) } },
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text(if (person != null) "People & stories" else if (detail.id > 0) "In your universe" else "astro.",
                            fontWeight = FontWeight.ExtraBold, fontSize = if (detail.id > 0) 20.sp else 30.sp)
                        if (detail.id == 0) Text("YOUR UNIVERSE, WITH YOU", fontSize = 9.sp, letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }, navigationIcon = {
                    if (person != null || detail.id > 0) IconButton(onClick = { if (person != null) model.closePerson() else model.closeDetail() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }, actions = {
                    IconButton(onClick = { model.flag("spoilers",!preferences.spoilers);model.announce(if(preferences.spoilers) "Spoiler protection off" else "Spoiler protection on") }) {
                        Icon(if(preferences.spoilers) Icons.Rounded.CheckCircle else Icons.Rounded.Info,"Toggle spoiler protection", tint=if(preferences.spoilers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { extra="Profile";model.closeDetail() }) {
                        if(account!=null) Cover(account!!.avatar,account!!.name,Modifier.size(34.dp)) else Icon(Icons.Rounded.Person,"Profile & sign in")
                    }
                    Spacer(Modifier.width(12.dp))
                })
            },
            bottomBar = {
                if (detail.id == 0 && person == null) NavigationBar {
                    destinations.forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { extra=""; tab = index },
                            icon = { Icon(item.icon, item.name) }, label = { Text(item.name, fontSize = 10.sp) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    person != null -> PersonScreen(person!!, detail.value?.characters.orEmpty(), preferences.spoilers, model)
                    detail.id > 0 -> DetailScreen(detail, library.firstOrNull { it.mediaId == detail.id }, preferences.spoilers, model)
                    extra.isNotBlank() -> ExtraScreen(extra,library,preferences,model) { extra=it }
                    tab == 0 -> OrbitScreen(library, discovery, model, { extra=it }) { tab = 1 }
                    tab == 1 -> ExploreScreen(discovery, model)
                    tab == 2 -> CollectionScreen(library, model)
                    tab == 3 -> ScheduleScreen(schedule, library, preferences, model)
                    else -> SettingsScreen(preferences, model) { extra=it }
                }
            }
        }
    }
}

}

@Composable
internal fun OrbitScreen(library: List<LibraryEntry>, discovery: Discovery, model: AstroViewModel, tool: (String)->Unit, explore: () -> Unit) {
    val account by model.account.collectAsStateWithLifecycle()
    val spotlight by model.spotlight.collectAsStateWithLifecycle()
    val recommended by model.suggestions.collectAsStateWithLifecycle()
    val sourceGenres by model.suggestionGenres.collectAsStateWithLifecycle()
    val current = library.filter { it.status in listOf("CURRENT","REPEATING") }
    val minutes = library.filter { it.type == "ANIME" }.sumOf { it.progress.toLong() * it.duration }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Column {
            Eyebrow("MISSION CONTROL")
            Text(account?.name?.let { "Good ${if(LocalTime.now().hour<12) "morning" else if(LocalTime.now().hour<18) "afternoon" else "evening"}, $it." } ?: "A little space for\nyour stories.", fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Pick up where your universe left off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${library.size}", "in your orbit", Modifier.weight(1f))
                Stat("${minutes / 60}h", "tracked runtime", Modifier.weight(1f))
                Stat("${library.count { it.status == "COMPLETED" }}", "completed", Modifier.weight(1f))
            }
        }
        item { OrbitToolsRow(tool) }
        item { TonightPanel(library,model) }
        item { OrbitSummary(library,model) }
        item { SectionTitle("Continue your story", "Watching & reading now") }
        if (current.isEmpty()) item { Notice("Your orbit starts here", "Find a title, add it to your collection, then start tracking your episodes or chapters.", "Explore stories", explore) }
        items(current.take(12), key = { "continue-${it.mediaId}" }) { entry -> EntryRow(entry, model) }
        if (spotlight.isNotEmpty()) {
            item { SectionTitle("In the spotlight", "Trending anime on AniList") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(spotlight.take(10), key = { it.id }) { MediaCard(it, Modifier.width(170.dp)) { model.open(it.id) } }
                }
            }
        } else if (discovery.loading) item { Loading() }
        else if (discovery.error != null) item { ErrorNotice(discovery.error) { model.loadDiscovery() } }
        item { SectionTitle("For you"); TextButton(onClick={model.recommend()}) { Text("Find recommendations from my collection") } }
        if(recommended.second.isNotEmpty()) item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Because you liked ${recommended.first}",fontWeight=FontWeight.Bold)
            Text("AniList community recommendations · shared genres shown below",style=MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)) { items(recommended.second,key={it.id}) { m -> Column(Modifier.width(160.dp)) {
                MediaCard(m) { model.open(m.id) }
                val shared=m.genres.filter {it in sourceGenres};val union=(m.genres+sourceGenres).distinct().size
                Text(if(shared.isEmpty()) "Recommended by AniList users" else "${if(union>0) shared.size*100/union else 0}% genre overlap · ${shared.joinToString(" • ")}",fontSize=11.sp)
            } } }
        } }
    }
}

@Composable
internal fun ExploreScreen(state: Discovery, model: AstroViewModel) {
    val grid = rememberLazyGridState()
    LaunchedEffect(grid, state.page, state.hasMore, state.loading, state.error) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.distinctUntilChanged().collect { index ->
            if (state.items.isNotEmpty() && index >= state.items.size - 5 && state.hasMore && !state.loading && state.error == null)
                model.loadDiscovery(true)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("A whole world awaits", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Catalog.entries) { catalog -> FilterChip(selected = state.catalog == catalog,
                    onClick = { model.search("", catalog) }, label = { Text(catalog.label) }) }
            }
            OutlinedTextField(value = state.search, onValueChange = { model.search(it) }, label = { Text("Search ${state.catalog.label.lowercase()}") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true,
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
        }
        ExploreFilterPanel(state,model)
        LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), state = grid,
            contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(state.items, key = { it.id }) { media -> MediaCard(media) { model.open(media.id) } }
            if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { Loading() }
            if (state.error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorNotice(state.error) { model.loadDiscovery(state.page > 0) }
            }
            if (!state.loading && state.error == null && state.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No stories found. Try another title.", Modifier.padding(vertical = 32.dp))
            }
            if (state.items.isNotEmpty() && state.hasMore && !state.loading && state.error == null)
                item(span = { GridItemSpan(maxLineSpan) }) { TextButton(onClick = { model.loadDiscovery(true) }) { Text("Explore more") } }
            if (!state.hasMore) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(if (state.items.size >= 80) "80 stories explored. Search to discover something more specific." else "You're all caught up.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun CollectionScreen(library: List<LibraryEntry>, model: AstroViewModel) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Your collection", "${library.size} stories · changes saved on device") }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("ALL", "CURRENT", "PLANNING", "COMPLETED", "PAUSED", "DROPPED", "REPEATING")) { status ->
                FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(status.pretty()) })
            }
        } }
        val entries = library.filter { filter == "ALL" || it.status == filter }
        if (entries.isEmpty()) item { Notice("Room for a new story", "Add titles from Explore to start your collection.") }
        items(entries, key = { it.mediaId }) { EntryRow(it, model) }
    }
}

@Composable
internal fun EntryRow(entry: LibraryEntry, model: AstroViewModel) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(entry.cover, entry.title, Modifier.width(64.dp).height(94.dp).clickable { model.open(entry.mediaId) })
            Column(Modifier.weight(1f).clickable { model.open(entry.mediaId) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if(entry.format=="NOVEL") "${entry.volumes} / ${entry.totalVolumes.takeIf { it>0 } ?: "?"} volumes" else "${entry.progress} / ${if (entry.total > 0) entry.total else "?"} ${if (entry.type == "ANIME") "episodes" else "chapters"}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.total > 0) LinearProgressIndicator(progress = { (entry.progress.toFloat() / entry.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(entry.status.pretty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column {
                FilledTonalIconButton(onClick = { if(entry.format=="NOVEL") model.volumes(entry.mediaId,1) else model.increment(entry.mediaId) }, enabled = if(entry.format=="NOVEL") entry.totalVolumes<=0 || entry.volumes<entry.totalVolumes else entry.total <= 0 || entry.progress < entry.total) {
                    Icon(Icons.Rounded.Add, "Add one ${if (entry.type == "ANIME") "episode" else "chapter"} to ${entry.title}")
                }
                IconButton(onClick = { if(entry.format=="NOVEL") model.volumes(entry.mediaId,-1) else model.decrement(entry.mediaId) }, enabled = if(entry.format=="NOVEL") entry.volumes>0 else entry.progress > 0) {
                    Text("−1", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
internal fun DetailScreen(state: DetailState, entry: LibraryEntry?, spoilerSafe: Boolean, model: AstroViewModel) {
    val context = LocalContext.current
    var revealed by rememberSaveable(state.id, spoilerSafe) { mutableStateOf(false) }
    var removeConfirmation by remember { mutableStateOf(false) }
    val hidden = spoilerSafe && !revealed
    val media = state.value?.media
    val appearance=LocalAppearance.current
    if (media == null) { if (state.error != null) ErrorNotice(state.error) { model.open(state.id) } else Loading(); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                if(appearance.artwork) Cover(media.banner.ifBlank { media.cover }, "", Modifier.fillMaxSize(), rounded = false)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                Row(Modifier.align(Alignment.BottomStart).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    Cover(media.cover, media.title, Modifier.width(104.dp).height(152.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Eyebrow(listOf(media.format.pretty(), media.year.takeIf { it > 0 }?.toString().orEmpty()).filter { it.isNotBlank() }.joinToString(" · "))
                        Text(media.title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        if (media.nativeTitle.isNotBlank()) Text(media.nativeTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(media.genres) { genre -> SuggestionChip(onClick = {}, enabled = false, label = { Text(genre, color = MaterialTheme.colorScheme.primary) }) }
            }
            Text("${if (media.score > 0) "★ ${media.score}%  ·  " else ""}${if (media.total > 0) media.total else "?"} ${media.unit}s  ·  ${media.status.pretty()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            if (entry == null) Button(onClick = { model.add(media) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add to my collection")
            } else {
                EntryRow(entry, model)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("PLANNING", "CURRENT", "PAUSED", "DROPPED", "REPEATING", "COMPLETED")) { status ->
                        FilterChip(selected = entry.status == status, onClick = { model.status(media.id, status) }, label = { Text(status.pretty()) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (media.trailerSite == "youtube" && media.trailerId.isNotBlank()) OutlinedButton(onClick = {
                    openLink(context, "https://www.youtube.com/watch?v=${Uri.encode(media.trailerId)}")
                }) { Icon(Icons.Rounded.PlayArrow, null); Text("Trailer") }
                TextButton(onClick = { openLink(context, "https://anilist.co/${if (media.type == "ANIME") "anime" else "manga"}/${media.id}") }) { Text("AniList ↗") }
            }
            if (hidden) Notice("Spoiler protection is on", "AniList doesn't provide episode-level spoiler boundaries. Descriptions and cast are hidden until you reveal them.", "Reveal for this visit", { revealed = true })
            else {
                ExpandableDescription(media.description)
            }
        } }
        if (!hidden) {
            item { Column(Modifier.padding(horizontal = 20.dp)) { SectionTitle("Characters & voices", "Japanese cast · main to supporting") } }
            item { LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(state.value?.characters.orEmpty(), key = { it.id }) { character ->
                    Column(Modifier.width(136.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Cover(character.image, character.name, Modifier.fillMaxWidth().height(170.dp).clickable { model.openPerson(character.id, false) })
                        Text(character.name, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.clickable { model.openPerson(character.id, false) })
                        Text(character.role.pretty(), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        if (character.actorId > 0) TextButton(onClick = { model.openPerson(character.actorId, true) }, contentPadding = PaddingValues(0.dp)) {
                            Text(character.actorName, fontWeight = FontWeight.Normal, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
            } }
            item { val loading by model.castLoading.collectAsStateWithLifecycle()
                if(state.value?.moreCast==true) TextButton(onClick={model.moreCast()},enabled=!loading,modifier=Modifier.padding(horizontal=20.dp)) { Text(if(loading) "Loading cast…" else "More characters") }
                else if(state.value?.characters.orEmpty().isEmpty()) Text("No cast information listed.",Modifier.padding(horizontal=20.dp))
            }
            item { Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                if(media.type=="ANIME") MusicPanel(media,model)
                StaffPanel(state.value?.staff.orEmpty(),model)
                FranchisePanel(state.value!!,model)
                if(state.value!!.recommendations.isNotEmpty()) { SectionTitle("More like this");LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    items(state.value!!.recommendations,key={it.id}) { m -> MediaCard(m,Modifier.width(150.dp)) { model.open(m.id) } }
                } }
            } }

        }
        if(entry!=null) item { Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) { TrackingExtras(entry,media,model) } }
        if (state.error != null) item { ErrorNotice(state.error) { model.open(state.id) } }
        if (entry != null) item { TextButton(onClick = { removeConfirmation = true }, modifier = Modifier.padding(horizontal = 20.dp)) { Text("Remove from collection", color = MaterialTheme.colorScheme.error) } }
    }
    if (removeConfirmation) AlertDialog(onDismissRequest = { removeConfirmation = false }, title = { Text("Remove this story?") },
        text = { Text("Its progress will be removed. If you sync this account, the removal will also be sent to AniList.") },
        confirmButton = { TextButton(onClick = { model.remove(media.id); removeConfirmation = false }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removeConfirmation = false }) { Text("Keep") } })
}

@Composable
internal fun PersonScreen(state: PersonState, cast: List<CastMember>, spoilers: Boolean, model: AstroViewModel) {
    var revealed by rememberSaveable(state.id, spoilers) { mutableStateOf(false) }
    val person = state.value
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (person == null) item { if (state.error != null) ErrorNotice(state.error) { model.openPerson(state.id, state.staff) } else Loading() }
        else {
            item { Cover(person.image, person.name, Modifier.width(200.dp).height(260.dp)) }
            item { Text(person.name, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
            item {
                if (spoilers && !revealed) Notice("Biography hidden", "Biographies may mention events beyond your progress.", "Reveal biography", { revealed = true })
                else RichAbout(person.description,revealSpoilers=true)
            }
            if(!spoilers || revealed) {
                item { SectionTitle("Other works") }
                item { LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)) { items(person.works,key={it.id}) { m -> MediaCard(m,Modifier.width(150.dp)) { model.open(m.id) } } } }
                if(person.more) item { TextButton(onClick={model.moreWorks()}) { Text("More works") } }
            }
            val actor = if (!state.staff) cast.firstOrNull { it.id == state.id && it.actorId > 0 } else null
            if (actor != null) item {
                SectionTitle("Japanese voice actor")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.clickable { model.openPerson(actor.actorId, true) }, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(actor.actorImage, actor.actorName, Modifier.width(80.dp).height(104.dp))
                    Text(actor.actorName, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun ScheduleScreen(state: ScheduleState, library: List<LibraryEntry>, preferences: Preferences, model: AstroViewModel) {
    val zone = if (preferences.ist) ZoneId.of("Asia/Kolkata") else ZoneId.systemDefault()
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now(zone).plusWeeks(offset.toLong())
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var selected by rememberSaveable(zone.id, monday.toString()) { mutableIntStateOf(today.dayOfWeek.value - 1) }
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(zone.id, monday) { model.refreshSchedule(zone,offset) }
    val date = monday.plusDays(selected.toLong())
    val ids = remember(library) { library.map { it.mediaId }.toSet() }
    val now=rememberMinute()
    val episodes = state.items.filter { Instant.ofEpochSecond(it.airingAt).atZone(zone).toLocalDate() == date && (!onlyMine || it.media.id in ids) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("This week in your universe", "${monday.format(DateTimeFormatter.ofPattern("d MMM"))} – ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))} · ${zone.id}") }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick={offset--}) { Text("← Previous") }; TextButton(onClick={offset=0}) { Text("This week") };TextButton(onClick={offset++}) { Text("Next →") }
        } }
        item { CalendarExport(state.items.filter { !onlyMine || it.media.id in ids },model) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(7) { index -> val day = monday.plusDays(index.toLong())
                FilterChip(selected = selected == index, onClick = { selected = index }, label = { Text(day.format(DateTimeFormatter.ofPattern("EEE d"))) })
            }
        } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = onlyMine, onClick = { onlyMine = !onlyMine }, label = { Text("Only my anime") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { model.refreshSchedule(zone,offset) }, enabled = !state.loading) { Icon(Icons.Rounded.Refresh, "Refresh schedule") }
        } }
        if (state.loading) item { Loading() }
        if (state.error != null) item { ErrorNotice(state.error) { model.refreshSchedule(zone,offset) } }
        if (episodes.isEmpty() && !state.loading && state.error == null) item { Notice("A quiet day in orbit", "No ${if (onlyMine) "tracked " else ""}anime releases found for this day.") }
        items(episodes, key = { it.id }) { airing ->
            Surface(onClick = { model.open(airing.media.id) }, shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(airing.media.cover, airing.media.title, Modifier.width(60.dp).height(88.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(airing.media.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Episode ${airing.episode} · ${countdown(airing.airingAt,now)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Instant.ofEpochSecond(airing.airingAt).atZone(zone).format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(preferences: Preferences, model: AstroViewModel, tool:(String)->Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> model.flag("reminders", granted) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { SectionTitle("Make yourself at home", "Small details. Your kind of universe.") }
        item { OrbitToolsRow(tool) }
        item { Column {
            Eyebrow("APPEARANCE")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(themeNames) { theme ->
                    FilterChip(selected = preferences.theme == theme, onClick = { model.theme(theme) }, label = { Text(theme) })
                }
            }
            ToggleRow("Artwork accents", "Let title artwork colour the whole screen.", preferences.dynamic) { model.flag("dynamic", it) }
            AppearanceOptions(preferences,model)
        } }
        item { Column {
            Eyebrow("YOUR EXPERIENCE")
            ToggleRow("Spoiler protection", "Hide descriptions, biographies and cast until you reveal them.", preferences.spoilers) { model.flag("spoilers", it) }
            ToggleRow("Use IST", "Display the schedule in India Standard Time.", preferences.ist) { model.flag("ist", it) }
        } }
        item { Column {
            Eyebrow("NOTIFICATIONS")
            ToggleRow("Episode release reminders", "For currently watching anime. Android may delay alerts to save battery.", preferences.reminders) { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else model.flag("reminders", enabled)
            }
            if (preferences.reminders && !AiringNotifications.allowed(context)) Text("Notifications are blocked in Android settings.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = {
                context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Android notification settings ↗") }
        } }
        item { AccountPanel(model) }
        item { BackupPanel(model) }
        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("BUILT FOR YOUR POCKET")
            Text("Native Compose screens · Android 12+", fontWeight = FontWeight.SemiBold)
            Text("Artwork cache: up to 64 MB on disk. Background requests run only when reminders or auto-sync are enabled. No analytics or advertising SDKs.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Astro 0.2.0 · Parity preview", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        } }
    }
}

@Composable
internal fun ToggleRow(title: String, description: String, value: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(checked = value, onCheckedChange = changed)
    }
}
@Composable
internal fun MediaCard(media: Media, modifier: Modifier = Modifier, clicked: () -> Unit) {
    Column(modifier.clickable(onClick = clicked), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Cover(media.cover, media.title, Modifier.fillMaxWidth().aspectRatio(if(LocalAppearance.current.compact) .85f else .69f))
            if (media.score > 0) Surface(color = Color(0xDD0C1216), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Text("★ ${media.score}%", Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 11.sp, color = Color(0xFFFFE5A4))
            }
        }
        Text(media.title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(media.format.pretty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
internal fun Cover(url: String, description: String, modifier: Modifier, rounded: Boolean = true) {
    val shape = RoundedCornerShape(if (rounded) LocalAppearance.current.radius.dp else 0.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .3f))
        AsyncImage(model = coil.request.ImageRequest.Builder(LocalContext.current).data(url.ifBlank { null }).crossfade(LocalAppearance.current.animations).build(), contentDescription = description.ifBlank { null }, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    }
}
@Composable
internal fun Eyebrow(text: String) { Text(text, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp)) }
@Composable
internal fun SectionTitle(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
internal fun Stat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
internal fun Notice(title: String, description: String, action: String? = null, clicked: () -> Unit = {}) {
    Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Chibi(Modifier.size(64.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            if (action != null) FilledTonalButton(onClick = clicked) { Text(action) }
        }
    }
}
@Composable
internal fun ErrorNotice(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = retry) { Text("Try again") }
    }
}
@Composable
internal fun Loading() { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } }
internal fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
internal fun plainText(value: String) = HtmlCompat.fromHtml(value.replace("<br>", "\n"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
internal fun openLink(context: android.content.Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, "No app is available to open this link.", android.widget.Toast.LENGTH_SHORT).show() }
}
