package com.bandit.astro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bandit.astro.AstroViewModel
import com.bandit.astro.data.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun SpotlightCarousel(rows:List<Media>,library:List<LibraryEntry>,m:AstroViewModel) {
    if(rows.isEmpty())return
    val titles=rows.take(10);val p=LocalAppearance.current
    var index by rememberSaveable {mutableIntStateOf(0)};var paused by rememberSaveable {mutableStateOf(false)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var resumed by remember {mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver {_,_->resumed=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)};lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}}
    LaunchedEffect(titles.first().id,p.animations,paused,resumed){if(p.animations&&!paused&&resumed)while(true){delay(8000);index=(index+1)%titles.size}}
    val media=titles[index%titles.size]
    Surface(shape=MaterialTheme.shapes.large) {
        Box(Modifier.fillMaxWidth().heightIn(min=330.dp)) {
            if(p.artwork)Cover(media.banner.ifBlank {media.cover},"",Modifier.matchParentSize(),rounded=false)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.4f),Color.Black.copy(alpha=.95f)))))
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("TRENDING SPOTLIGHT",color=Color.White,fontSize=11.sp)
                Text(media.title,color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
                Text(media.genres.take(3).joinToString(" • "),color=Color.White.copy(alpha=.85f),fontSize=12.sp)
                Text(if(p.spoilers) "Open this story when you're ready. Plot previews are hidden." else plainText(media.description),maxLines=3,color=Color.White,lineHeight=21.sp)
                AstroButton(onClick={m.open(media.id)}){Text("Explore story")}
                val saved=library.any {it.mediaId==media.id}
                TextButton(onClick={m.add(media)},enabled=!saved){Text(if(saved) "✓ In your collection" else "+ Add to collection",color=Color.White)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    TextButton(onClick={paused=true;index=(index-1+titles.size)%titles.size}){Text("←",color=Color.White)}
                    Text("${index%titles.size+1} / ${titles.size}",color=Color.White)
                    TextButton(onClick={paused=!paused}){Text(if(paused) "Play" else "Pause",color=Color.White)}
                    TextButton(onClick={paused=true;index=(index+1)%titles.size}){Text("→",color=Color.White)}
                }
            }
        }
    }
}

@Composable internal fun RecommendationsPanel(library:List<LibraryEntry>,m:AstroViewModel) {
    val anchors=library.filter {it.score>=70||it.status=="COMPLETED"}.sortedByDescending {it.score}.take(20)
    var selected by rememberSaveable {mutableIntStateOf(0)}
    val source by m.suggestionSource.collectAsStateWithLifecycle();val results by m.suggestions.collectAsStateWithLifecycle()
    val loading by m.suggestionLoading.collectAsStateWithLifecycle();val error by m.suggestionError.collectAsStateWithLifecycle()
    val anchor=anchors.firstOrNull {it.mediaId==selected} ?: anchors.firstOrNull()
    LaunchedEffect(anchor?.mediaId){if(anchor!=null)m.recommend(anchor.mediaId)}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Because you liked…",anchor?.title ?: "Your taste is the starting point")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(anchors,key={it.mediaId}){e->FilterChip(selected=e.mediaId==anchor?.mediaId,onClick={selected=e.mediaId},label={Text(e.title,maxLines=1)})}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotBlank())ErrorNotice(error){m.recommend(anchor?.mediaId ?: 0)}
        if(anchors.isEmpty())Text("Complete or rate a few stories to find your next favorite.")
        else if(!loading&&error.isBlank()&&results.second.isEmpty())Text("No new recommendations for this title yet. Try another favorite.")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(results.second,key={it.id}){media->Column(Modifier.width(165.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            MediaCard(media){m.open(media.id)}
            source?.let {a->Text("${genreOverlap(a,media)}% genre overlap",fontSize=12.sp,color=MaterialTheme.colorScheme.primary);Text(recommendationReasons(a,media).joinToString(" • ").ifBlank {"AniList community pick"},fontSize=11.sp)}
        }}}
        if(results.second.isNotEmpty())Text("Genre overlap is a similarity measure, not a predicted rating.",fontSize=11.sp)
    }
}

@Composable internal fun RadarPanel(library:List<LibraryEntry>,m:AstroViewModel) {
    val rows by m.todayAirings.collectAsStateWithLifecycle();val error by m.radarError.collectAsStateWithLifecycle()
    val now=rememberMinute();val today=Instant.ofEpochSecond(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val ids=library.filter {it.type=="ANIME"&&it.status in listOf("CURRENT","REPEATING","PLANNING")}.map {it.mediaId}.sorted().take(50)
    LaunchedEffect(ids,today){m.loadRadar(ids)}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        SectionTitle("On your radar","Today's broadcasts · your local time")
        if(error.isNotBlank())ErrorNotice(error){m.loadRadar(ids)}
        rows.forEach {r->Surface(onClick={m.open(r.media.id)},shape=MaterialTheme.shapes.medium,tonalElevation=2.dp){Column(Modifier.fillMaxWidth().padding(14.dp)){
            Text(r.media.title,fontWeight=FontWeight.SemiBold)
            Text("Episode ${r.episode} · ${Instant.ofEpochSecond(r.airingAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} · ${countdown(r.airingAt,now)}",fontSize=12.sp)
        }}}
        if(rows.isEmpty()&&error.isBlank())Text("No airings loaded for your tracked titles today.",fontSize=13.sp)
        Text("Checks up to 50 watching and planned anime. Broadcast times aren't streaming availability.",fontSize=11.sp)
    }
}
