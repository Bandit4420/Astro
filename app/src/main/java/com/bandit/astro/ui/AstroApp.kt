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
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
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
    Destination("Orbit", Icons.Rounded.Home), Destination("Explore", Icons.Rounded.Explore),
    Destination("Collection", Icons.AutoMirrored.Rounded.LibraryBooks), Destination("Schedule", Icons.Rounded.CalendarMonth),
    Destination("Settings", Icons.Rounded.Tune)
)

@Composable
fun AstroApp(model: AstroViewModel, notificationId: Int, consumed: () -> Unit) {
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val library by model.collection.collectAsStateWithLifecycle()
    val discovery by model.discovery.collectAsStateWithLifecycle()
    val detail by model.detail.collectAsStateWithLifecycle()
    val person by model.person.collectAsStateWithLifecycle()
    val schedule by model.schedule.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(notificationId) { if (notificationId > 0) { model.open(notificationId); consumed() } }
    val accent = if (detail.id > 0) detail.value?.media?.accent.orEmpty() else ""
    AstroTheme(preferences, accent) {
        BackHandler(enabled = person != null || detail.id > 0 || tab != 0) {
            when { person != null -> model.closePerson(); detail.id > 0 -> model.closeDetail(); else -> tab = 0 }
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
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
                    if (detail.id == 0) AssistChip(onClick = { tab = 4 }, label = { Text("On device", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(14.dp)) })
                    Spacer(Modifier.width(12.dp))
                })
            },
            bottomBar = {
                if (detail.id == 0 && person == null) NavigationBar {
                    destinations.forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index },
                            icon = { Icon(item.icon, item.name) }, label = { Text(item.name, fontSize = 10.sp) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    person != null -> PersonScreen(person!!, detail.value?.characters.orEmpty(), preferences.spoilers, model)
                    detail.id > 0 -> DetailScreen(detail, library.firstOrNull { it.mediaId == detail.id }, preferences.spoilers, model)
                    tab == 0 -> OrbitScreen(library, discovery, model) { tab = 1 }
                    tab == 1 -> ExploreScreen(discovery, model)
                    tab == 2 -> CollectionScreen(library, model)
                    tab == 3 -> ScheduleScreen(schedule, library, preferences, model)
                    else -> SettingsScreen(preferences, model)
                }
            }
        }
    }
}

@Composable
private fun OrbitScreen(library: List<LibraryEntry>, discovery: Discovery, model: AstroViewModel, explore: () -> Unit) {
    val current = library.filter { it.status == "CURRENT" }
    val minutes = library.filter { it.type == "ANIME" }.sumOf { it.progress.toLong() * it.duration }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Column {
            Eyebrow("MISSION CONTROL")
            Text("A little space for\nyour stories.", fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
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
        item { SectionTitle("Continue your story", "Watching & reading now") }
        if (current.isEmpty()) item { Notice("Your orbit starts here", "Find a title, add it to your collection, then start tracking your episodes or chapters.", "Explore stories", explore) }
        items(current.take(12), key = { "continue-${it.mediaId}" }) { entry -> EntryRow(entry, model) }
        if (discovery.items.isNotEmpty()) {
            item { SectionTitle("In the spotlight", "Trending on AniList · ${discovery.catalog.label}") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(discovery.items.take(6), key = { it.id }) { MediaCard(it, Modifier.width(170.dp)) { model.open(it.id) } }
                }
            }
        } else if (discovery.loading) item { Loading() }
        else if (discovery.error != null) item { ErrorNotice(discovery.error) { model.loadDiscovery() } }
        item { Text("This preview keeps your collection on this device. AniList account sync is coming next.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ExploreScreen(state: Discovery, model: AstroViewModel) {
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
private fun CollectionScreen(library: List<LibraryEntry>, model: AstroViewModel) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Your collection", "Saved on this device · ${library.size} stories") }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("ALL", "CURRENT", "PLANNING", "COMPLETED", "PAUSED", "DROPPED")) { status ->
                FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(status.pretty()) })
            }
        } }
        val entries = library.filter { filter == "ALL" || it.status == filter }
        if (entries.isEmpty()) item { Notice("Room for a new story", "Add titles from Explore to start your collection.") }
        items(entries, key = { it.mediaId }) { EntryRow(it, model) }
    }
}

@Composable
private fun EntryRow(entry: LibraryEntry, model: AstroViewModel) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(entry.cover, entry.title, Modifier.width(64.dp).height(94.dp).clickable { model.open(entry.mediaId) })
            Column(Modifier.weight(1f).clickable { model.open(entry.mediaId) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${entry.progress} / ${if (entry.total > 0) entry.total else "?"} ${if (entry.type == "ANIME") "episodes" else "chapters"}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.total > 0) LinearProgressIndicator(progress = { (entry.progress.toFloat() / entry.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(entry.status.pretty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            FilledTonalIconButton(onClick = { model.increment(entry.mediaId) }, enabled = entry.total <= 0 || entry.progress < entry.total) {
                Icon(Icons.Rounded.Add, "Add one ${if (entry.type == "ANIME") "episode" else "chapter"} to ${entry.title}")
            }
        }
    }
}

@Composable
private fun DetailScreen(state: DetailState, entry: LibraryEntry?, spoilerSafe: Boolean, model: AstroViewModel) {
    val context = LocalContext.current
    var revealed by rememberSaveable(state.id, spoilerSafe) { mutableStateOf(false) }
    var removeConfirmation by remember { mutableStateOf(false) }
    val hidden = spoilerSafe && !revealed
    val media = state.value?.media
    if (media == null) { if (state.error != null) ErrorNotice(state.error) { model.open(state.id) } else Loading(); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                Cover(media.banner.ifBlank { media.cover }, "", Modifier.fillMaxSize(), rounded = false)
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
                    items(listOf("PLANNING", "CURRENT", "PAUSED", "DROPPED", "COMPLETED")) { status ->
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
                SectionTitle("About this story")
                Text(plainText(media.description).ifBlank { "No description available yet." }, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp)
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
            item { Text(if (state.value?.characters.orEmpty().isEmpty()) "No cast information available yet." else "Showing up to 25 characters. Full credits are available on AniList.",
                Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (state.error != null) item { ErrorNotice(state.error) { model.open(state.id) } }
        if (entry != null) item { TextButton(onClick = { removeConfirmation = true }, modifier = Modifier.padding(horizontal = 20.dp)) { Text("Remove from collection", color = MaterialTheme.colorScheme.error) } }
    }
    if (removeConfirmation) AlertDialog(onDismissRequest = { removeConfirmation = false }, title = { Text("Remove this story?") },
        text = { Text("Its local progress will also be removed.") },
        confirmButton = { TextButton(onClick = { model.remove(media.id); removeConfirmation = false }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removeConfirmation = false }) { Text("Keep") } })
}

@Composable
private fun PersonScreen(state: PersonState, cast: List<CastMember>, spoilers: Boolean, model: AstroViewModel) {
    var revealed by rememberSaveable(state.id, spoilers) { mutableStateOf(false) }
    val person = state.value
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (person == null) item { if (state.error != null) ErrorNotice(state.error) { model.openPerson(state.id, state.staff) } else Loading() }
        else {
            item { Cover(person.image, person.name, Modifier.width(200.dp).height(260.dp)) }
            item { Text(person.name, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
            item {
                if (spoilers && !revealed) Notice("Biography hidden", "Biographies may mention events beyond your progress.", "Reveal biography", { revealed = true })
                else Text(plainText(person.description).ifBlank { "No biography available yet." }, lineHeight = 24.sp)
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
private fun ScheduleScreen(state: ScheduleState, library: List<LibraryEntry>, preferences: Preferences, model: AstroViewModel) {
    val zone = if (preferences.ist) ZoneId.of("Asia/Kolkata") else ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var selected by rememberSaveable(zone.id, monday.toString()) { mutableIntStateOf(today.dayOfWeek.value - 1) }
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(zone.id, monday) { model.refreshSchedule(zone) }
    val date = monday.plusDays(selected.toLong())
    val ids = remember(library) { library.map { it.mediaId }.toSet() }
    val episodes = state.items.filter { Instant.ofEpochSecond(it.airingAt).atZone(zone).toLocalDate() == date && (!onlyMine || it.media.id in ids) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("This week in your universe", "${monday.format(DateTimeFormatter.ofPattern("d MMM"))} – ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))} · ${zone.id}") }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(7) { index -> val day = monday.plusDays(index.toLong())
                FilterChip(selected = selected == index, onClick = { selected = index }, label = { Text(day.format(DateTimeFormatter.ofPattern("EEE d"))) })
            }
        } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = onlyMine, onClick = { onlyMine = !onlyMine }, label = { Text("Only my anime") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { model.refreshSchedule(zone) }, enabled = !state.loading) { Icon(Icons.Rounded.Refresh, "Refresh schedule") }
        } }
        if (state.loading) item { Loading() }
        if (state.error != null) item { ErrorNotice(state.error) { model.refreshSchedule(zone) } }
        if (episodes.isEmpty() && !state.loading && state.error == null) item { Notice("A quiet day in orbit", "No ${if (onlyMine) "tracked " else ""}anime releases found for this day.") }
        items(episodes, key = { it.id }) { airing ->
            Surface(onClick = { model.open(airing.media.id) }, shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(airing.media.cover, airing.media.title, Modifier.width(60.dp).height(88.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(airing.media.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Episode ${airing.episode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Instant.ofEpochSecond(airing.airingAt).atZone(zone).format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(preferences: Preferences, model: AstroViewModel) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> model.flag("reminders", granted) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { SectionTitle("Make yourself at home", "Small details. Your kind of universe.") }
        item { Column {
            Eyebrow("APPEARANCE")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Astro", "AMOLED", "Sakura", "Cyberpunk", "Ocean")) { theme ->
                    FilterChip(selected = preferences.theme == theme, onClick = { model.theme(theme) }, label = { Text(theme) })
                }
            }
            ToggleRow("Artwork accents", "Let title artwork colour the whole screen.", preferences.dynamic) { model.flag("dynamic", it) }
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
        item { Notice("AniList account connection", "This first native preview browses AniList publicly and tracks your collection locally. Secure sign-in and account sync are the next milestone. Your website account is unchanged.") }
        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("BUILT FOR YOUR POCKET")
            Text("Native Compose screens · Android 12+", fontWeight = FontWeight.SemiBold)
            Text("Artwork cache: up to 64 MB on disk. Background requests run only when reminders are enabled. No analytics or advertising SDKs.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Astro 0.1.0 · Native preview", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        } }
    }
}

@Composable
private fun ToggleRow(title: String, description: String, value: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(checked = value, onCheckedChange = changed)
    }
}
@Composable
private fun MediaCard(media: Media, modifier: Modifier = Modifier, clicked: () -> Unit) {
    Column(modifier.clickable(onClick = clicked), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Cover(media.cover, media.title, Modifier.fillMaxWidth().aspectRatio(.69f))
            if (media.score > 0) Surface(color = Color(0xDD0C1216), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Text("★ ${media.score}%", Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 11.sp, color = Color(0xFFFFE5A4))
            }
        }
        Text(media.title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(media.format.pretty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
private fun Cover(url: String, description: String, modifier: Modifier, rounded: Boolean = true) {
    val shape = RoundedCornerShape(if (rounded) 14.dp else 0.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .3f))
        AsyncImage(model = url.ifBlank { null }, contentDescription = description.ifBlank { null }, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    }
}
@Composable
private fun Eyebrow(text: String) { Text(text, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp)) }
@Composable
private fun SectionTitle(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun Notice(title: String, description: String, action: String? = null, clicked: () -> Unit = {}) {
    Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            if (action != null) FilledTonalButton(onClick = clicked) { Text(action) }
        }
    }
}
@Composable
private fun ErrorNotice(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = retry) { Text("Try again") }
    }
}
@Composable
private fun Loading() { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } }
private fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
private fun plainText(value: String) = HtmlCompat.fromHtml(value.replace("<br>", "\n"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
private fun openLink(context: android.content.Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, "No app is available to open this link.", android.widget.Toast.LENGTH_SHORT).show() }
}
