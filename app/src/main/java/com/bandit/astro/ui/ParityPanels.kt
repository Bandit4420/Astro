@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bandit.astro.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bandit.astro.*
import com.bandit.astro.R
import com.bandit.astro.data.*
import kotlinx.coroutines.*
import java.time.*

@Composable internal fun Chibi(modifier:Modifier=Modifier) { Image(painterResource(R.drawable.astro_mascot),"Astro mascot",modifier) }
@Composable internal fun ChibiSnackbar(data:SnackbarData) {
    Surface(Modifier.padding(12.dp),shape=RoundedCornerShape(22.dp),tonalElevation=8.dp) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Chibi(Modifier.size(60.dp));Text(data.visuals.message,Modifier.weight(1f).padding(10.dp),fontSize=13.sp)
            TextButton(onClick={data.dismiss()}) { Text("×") }
        }
    }
}
@Composable internal fun AmbientBackdrop(p:Preferences,art:String) {
    val accent=MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if(p.theme=="AMOLED") return@Box
        val blurred=if(p.blur) Modifier.blur(p.blurAmount.dp) else Modifier
        Box(Modifier.fillMaxSize().then(blurred).background(Brush.radialGradient(listOf(accent.copy(alpha=.2f),Color.Transparent)))) {
            if(p.artwork && art.isNotBlank() && p.glass) AsyncImage(art,null,Modifier.fillMaxSize(),alpha=.25f,contentScale=androidx.compose.ui.layout.ContentScale.Crop)
        }
    }
}
@Composable internal fun AppearanceOptions(p:Preferences,m:AstroViewModel) {
    ToggleRow("Liquid glass","Translucent surfaces with artwork behind them.",p.glass) {m.flag("glass",it)}
    ToggleRow("Backdrop blur","Blur the decorative background while keeping text sharp.",p.blur) {m.flag("blur",it)}
    if(p.blur) { Text("Blur strength · ${p.blurAmount.toInt()}");Slider(p.blurAmount,{m.number("blurAmount",it)},valueRange=0f..32f) }
    if(p.glass) {Text("Glass opacity · ${(p.transparency*100).toInt()}%");Slider(p.transparency,{m.number("transparency",it)},valueRange=.35f..1f)}
    ToggleRow("Background artwork","Show title banners and artwork behind glass.",p.artwork) {m.flag("artwork",it)}
    ToggleRow("Image transitions","Fade artwork as it loads.",p.animations) {m.flag("animations",it)}
    ToggleRow("Compact covers","Shorter cover cards in discovery.",p.compact) {m.flag("compact",it)}
    Text("Cover radius · ${p.radius.toInt()}");Slider(p.radius,{m.number("radius",it)},valueRange=0f..32f)
}
@Composable internal fun AccountPanel(m:AstroViewModel) {
    val account by m.account.collectAsStateWithLifecycle();val sync by m.syncState.collectAsStateWithLifecycle();val p by m.preferences.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var client by rememberSaveable { mutableStateOf(m.clientId()) }
    var showSetup by rememberSaveable {mutableStateOf(false)}
    var confirm by remember {mutableStateOf("")}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("AniList account",account?.name ?: "Sign in to track your own AniList account")
        if(account==null) {
            Text("Sign in through AniList in your browser. Public profile searches never grant account access.",fontSize=13.sp)
            if(client.isNotBlank()) Button(onClick={try {context.startActivity(Intent(Intent.ACTION_VIEW,m.beginLogin(client)))} catch(e:Exception){m.announce(e.message ?: "Could not open sign-in.")} }) {Text("Sign in with AniList")}
            TextButton(onClick={showSetup=!showSetup}) {Text(if(client.isBlank()) "Set up native login" else "Native login settings")}
            if(showSetup) {
                Text("Create a separate AniList application named Astro Android with redirect URI astro://oauth/anilist. Enter its public client ID here. Keep the website's application unchanged.",fontSize=13.sp)
                TextButton(onClick={openLink(context,"https://anilist.co/settings/developer")}) {Text("AniList developer settings ↗")}
                OutlinedTextField(client,{client=it.filter(Char::isDigit).take(12)},label={Text("Native client ID")},singleLine=true)
                Button(onClick={try {context.startActivity(Intent(Intent.ACTION_VIEW,m.beginLogin(client)))} catch(e:Exception){m.announce(e.message ?: "Could not start sign-in.")} },enabled=client.isNotBlank()) {Text("Save & sign in")}
            }
        } else {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Cover(account!!.avatar,account!!.name,Modifier.size(52.dp));Text(account!!.name,fontWeight=FontWeight.Bold)}
            Text(sync.message.ifBlank { "Ready to import and sync your AniList collection." },fontSize=13.sp)
            if(sync.running) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row {Button(onClick={m.sync()},enabled=!sync.running) {Text("Sync now")};TextButton(onClick={confirm="logout"}) {Text("Sign out")}}
            ToggleRow("Auto-sync","Queue edits after a short delay; refresh hourly when Android allows. Requires internet.",p.autoSync) {m.flag("autoSync",it)}
            if(sync.at>0) Text("Last synced ${Instant.ofEpochMilli(sync.at).atZone(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM · h:mm a"))}",fontSize=12.sp)
            if(sync.conflict>0) Row {TextButton(onClick={confirm="local"}) {Text("Keep Astro progress")};TextButton(onClick={confirm="remote"}) {Text("Use AniList progress")}}
        }
    }
    if(confirm.isNotBlank()) AlertDialog(onDismissRequest={confirm=""},title={Text(if(confirm=="logout") "Sign out?" else "Resolve this sync conflict?")},
        text={Text(when(confirm){"logout"->"Queued changes stay in this account's separate library. Sign in to the same account to resume syncing.";"local"->"Replace this title's AniList progress, status and rating with the Astro values.";else->"Discard this title's queued Astro changes and keep AniList's values. Private notes are retained."})},
        confirmButton={TextButton(onClick={when(confirm){"logout"->m.logout();"local"->m.sync(sync.conflict);else->m.sync(sync.conflict,true)};confirm=""}) {Text("Confirm")}},
        dismissButton={TextButton(onClick={confirm=""}) {Text("Cancel")}})
}
@Composable internal fun BackupPanel(m:AstroViewModel) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();var imported by remember {mutableStateOf<String?>(null)};var webExport by rememberSaveable {mutableStateOf(false)}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri->if(uri!=null) scope.launch {
        try {val text=m.backup(webExport);withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {it.write(text)} ?: error("Could not open backup file.")};m.announce("Backup exported")}
        catch(e:Exception){m.announce("Could not export backup: ${e.message}")}
    } }
    val load=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) scope.launch {try {
        imported=withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {input-> val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val count=input.read(buffer);if(count<0)break;require(output.size()+count<=10_000_000){"Backup exceeds 10 MB."};output.write(buffer,0,count)}
            output.toString("UTF-8")} ?: error("Could not read backup.")}
    }catch(e:Exception){m.announce(e.message ?: "Could not read backup.")} } }
    Column { SectionTitle("Keep your orbit safe");Text("Back up this account's collection, private notes, quiz records and tracking history. Tokens are excluded.",fontSize=13.sp)
        TextButton(onClick={webExport=true;save.launch("Astro-website-backup.json")}) {Text("Export for the website")}
        Row {TextButton(onClick={webExport=false;save.launch("Astro-backup.json")}) {Text("Export JSON")};TextButton(onClick={load.launch(arrayOf("application/json","text/plain"))}) {Text("Import JSON")}}
    }
    if(imported!=null) AlertDialog(onDismissRequest={imported=null},title={Text("Import missing titles?")},text={Text("Existing progress is kept; empty notes can be filled. Auto-sync will turn off so you can review imported entries. The backup must belong to this account.")},
        confirmButton={TextButton(onClick={m.importBackup(imported!!);imported=null}) {Text("Import")}},dismissButton={TextButton(onClick={imported=null}) {Text("Cancel")}})
}
@Composable internal fun CalendarExport(rows:List<Airing>,m:AstroViewModel) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) {uri->if(uri!=null) scope.launch {try {withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {it.write(calendarFile(rows))} ?: error("Could not open calendar file.")};m.announce("Calendar exported")}catch(e:Exception){m.announce(e.message ?: "Export failed")}}}
    TextButton(onClick={launcher.launch("Astro-schedule.ics")},enabled=rows.isNotEmpty()) {Text("Export this week's calendar")}
}
@Composable internal fun ExpandableDescription(value:String) {var expanded by rememberSaveable(value){mutableStateOf(false)};Column {SectionTitle("About this story");Text(plainText(value).ifBlank {"No description available yet."},maxLines=if(expanded) Int.MAX_VALUE else 5,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick={expanded=!expanded}) {Text(if(expanded) "Show less" else "Read more")}}}
@Composable internal fun RichAbout(value:String) {
    val imageRegex=Regex("!\\[[^]]*]\\((https://[^\\s)]+)\\)|img\\((https://[^\\s)]+)\\)|<img[^>]+src=[\"'](https://[^\"']+)[\"'][^>]*>",RegexOption.IGNORE_CASE)
    val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        var cursor=0
        for(match in imageRegex.findAll(value)) {
            val text=value.substring(cursor,match.range.first);if(text.isNotBlank()) Text(plainText(text),lineHeight=23.sp)
            val url=match.groupValues.drop(1).first {it.isNotBlank()};AsyncImage(url,"Profile image",Modifier.fillMaxWidth().heightIn(max=400.dp).clickable {openLink(context,url)})
            cursor=match.range.last+1
        }
        val tail=value.substring(cursor);if(tail.isNotBlank())Text(plainText(tail),lineHeight=23.sp)
        if(value.isBlank())Text("No biography listed yet.")
    }
}
@Composable internal fun MusicPanel(media:Media,m:AstroViewModel) {
    val songs by m.songs.collectAsStateWithLifecycle();val error by m.songsError.collectAsStateWithLifecycle();val loading by m.songsLoading.collectAsStateWithLifecycle();val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Opening & ending credits","MyAnimeList credits via Jikan")
        if(media.malId==0) Text("No song-credit mapping is available for this title.",fontSize=13.sp)
        else if(songs==null) Button(onClick={m.loadSongs()},enabled=!loading) {Text(if(loading) "Loading OP & ED…" else "Load OP & ED")}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        songs?.let { data -> for((heading,rows) in listOf("Openings · OP" to data.first,"Endings · ED" to data.second)) {
            Text(heading,fontWeight=FontWeight.Bold)
            if(rows.isEmpty())Text("No credits listed.",fontSize=12.sp)
            rows.forEach {song->Surface(shape=MaterialTheme.shapes.medium,tonalElevation=2.dp) {Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(song.title,fontWeight=FontWeight.SemiBold);Text(song.coverage,color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
                Row {TextButton(onClick={openLink(context,"https://www.youtube.com/results?search_query=${Uri.encode(song.title)}")}) {Text("YouTube")};TextButton(onClick={openLink(context,"https://open.spotify.com/search/${Uri.encode(song.title)}")}) {Text("Spotify")}}
            }}}
        } }
    }
}
@Composable internal fun StaffPanel(staff:List<Credit>,m:AstroViewModel) {var expanded by rememberSaveable(staff){mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {SectionTitle("Behind the story");staff.take(if(expanded) staff.size else 6).forEach {c->Row(Modifier.fillMaxWidth().clickable {m.openPerson(c.id,true)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Cover(c.image,c.name,Modifier.size(42.dp));Column {Text(c.name,fontWeight=FontWeight.SemiBold);Text(c.role,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}};if(staff.size>6)TextButton(onClick={expanded=!expanded}) {Text(if(expanded) "Show less" else "All ${staff.size} credits")}}
}
@Composable internal fun FranchisePanel(d:MediaDetail,m:AstroViewModel) {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {SectionTitle("Franchise timeline","Release year order · not an official watch order");TextButton(onClick={m.expandFranchise()}) {Text("Expand connected stories")}
    (listOf(Relation(d.media,"THIS TITLE"))+d.relations).distinctBy {it.media.id}.sortedBy {it.media.year.takeIf {n->n>0} ?: Int.MAX_VALUE}.forEach {r->Row(Modifier.fillMaxWidth().clickable {m.open(r.media.id)},horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {Text(r.media.year.takeIf {it>0}?.toString() ?: "TBA",color=MaterialTheme.colorScheme.primary);Cover(r.media.cover,r.media.title,Modifier.width(40.dp).height(60.dp));Column(Modifier.weight(1f)){Text(r.media.title,fontWeight=FontWeight.SemiBold);Text(r.relation.pretty(),fontSize=11.sp)}}}
}}
@Composable internal fun TrackingExtras(e:LibraryEntry,media:Media,m:AstroViewModel) {
    var note by rememberSaveable(e.mediaId){mutableStateOf(e.notes)};var rating by rememberSaveable(e.mediaId,e.score){mutableFloatStateOf(e.score.toFloat())};var pace by rememberSaveable{mutableFloatStateOf(2f)}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(e.type=="MANGA") {SectionTitle("Reading progress");Text("${e.progress} chapters · ${e.volumes} / ${e.totalVolumes.takeIf {it>0} ?: "?"} volumes")
            Row {TextButton(onClick={m.increment(e.mediaId)}) {Text("+1 chapter")};TextButton(onClick={m.volumes(e.mediaId,1)}) {Text("+1 volume")};TextButton(onClick={m.volumes(e.mediaId,-1)},enabled=e.volumes>0) {Text("−1 volume")}}
        }
        if(e.type=="ANIME") {SectionTitle("Catch-up calculator");val released=if(media.nextEpisode>0) media.nextEpisode-(if(media.nextAiring>Instant.now().epochSecond) 1 else 0) else if(media.status=="FINISHED") media.total else 0
            if(released>0) {val behind=(released-e.progress).coerceAtLeast(0);Text("$behind episodes behind · ${kotlin.math.ceil(behind/pace).toInt()} days at ${pace.toInt()}/day");Slider(pace,{pace=it},valueRange=1f..10f,steps=8);Text("${behind*(media.duration.takeIf {it>0} ?: 24)} minutes${if(media.duration<=0) " (24 min/episode estimate)" else ""}",fontSize=12.sp)} else Text("Waiting for a published airing schedule or finished episode total.",fontSize=13.sp)
            if(media.nextAiring>Instant.now().epochSecond && released>e.progress) {val days=(media.nextAiring-Instant.now().epochSecond)/86400.0;Text("Aim for ${kotlin.math.ceil((released-e.progress)/days).toInt()} episodes/day to catch up before episode ${media.nextEpisode}.",fontSize=12.sp)}
        }
        Text("Your score · ${rating.toInt()} / 100 (0 = unrated)");Slider(rating,{rating=it},onValueChangeFinished={m.score(e.mediaId,rating.toInt())},valueRange=0f..100f)
        SectionTitle("Private notes","Saved only in this account's device library")
        OutlinedTextField(note,{note=it.take(10000)},Modifier.fillMaxWidth(),minLines=3,label={Text("Thoughts for your future self")})
        TextButton(onClick={m.notes(e.mediaId,note);m.announce("Private note saved")},enabled=note!=e.notes) {Text("Save note")}
    }
}
@Composable internal fun TonightPanel(entries:List<LibraryEntry>,m:AstroViewModel) {
    val now=rememberMinute();val next=entries.filter {it.status=="CURRENT" && it.type=="ANIME" && it.nextAiring>now}.minByOrNull {it.nextAiring}
    if(next!=null) Notice("Next in your orbit", "${next.title} · Episode ${next.nextEpisode}\n${countdown(next.nextAiring,now)}","Open story",{m.open(next.mediaId)})
}
@Composable internal fun rememberMinute():Long {var now by remember{mutableLongStateOf(Instant.now().epochSecond)};LaunchedEffect(Unit){while(true){delay(60000);now=Instant.now().epochSecond}};return now}
internal fun countdown(at:Long,now:Long):String {val seconds=at-now;return if(seconds<=0)"Already aired" else "Airs in ${seconds/86400}d ${(seconds%86400)/3600}h ${(seconds%3600)/60}m"}

@Composable internal fun OrbitSummary(entries:List<LibraryEntry>,m:AstroViewModel) {
    val now=rememberMinute();val current=entries.filter {it.status in listOf("CURRENT","REPEATING") && it.type=="ANIME"};val known=current.filter {it.total>0}
    val total=known.sumOf {it.total.toLong()};val watched=known.sumOf {it.progress.coerceAtMost(it.total).toLong()}
    val behind=current.sumOf {if(it.nextEpisode>0) (it.nextEpisode-(if(it.nextAiring>now) 1 else 0)-it.progress).coerceAtLeast(0) else 0}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        SectionTitle("Your orbit")
        Text("$behind known aired episodes to catch up · ${if(total>0) "${watched*100/total}%" else "—"} through current shows")
        if(total>0)LinearProgressIndicator(progress={watched.toFloat()/total},modifier=Modifier.fillMaxWidth())
        Text("Manga update times aren't published by AniList. Track chapters and volumes from your collection.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
