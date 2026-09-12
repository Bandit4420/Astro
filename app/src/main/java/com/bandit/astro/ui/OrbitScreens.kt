package com.bandit.astro.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bandit.astro.*
import com.bandit.astro.data.*
import java.time.*
import kotlin.math.*

private val orbitTools=listOf("Profile","Stats & Wrapped","Anime Quiz","Roulette","Compatibility","Constellations")
@Composable internal fun OrbitToolsRow(tool:(String)->Unit) {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(orbitTools){name->AssistChip(onClick={tool(name)},label={Text(name)})}}}
@Composable internal fun ExtraScreen(name:String,entries:List<LibraryEntry>,p:Preferences,m:AstroViewModel,tool:(String)->Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={tool("")}) {Text("← Back")};Text(name,fontWeight=FontWeight.Bold)}
        when(name) {"Profile"->ProfileScreen(m);"Stats & Wrapped"->StatsScreen(entries,m);"Anime Quiz"->QuizScreen(m,p);"Roulette"->RouletteScreen(entries,m);"Compatibility"->CompatibilityScreen(entries,m);"Constellations"->ConstellationScreen(entries,m)}
    }
}
@Composable internal fun ProfileScreen(m:AstroViewModel) {
    val profile by m.profile.collectAsStateWithLifecycle();val error by m.profileError.collectAsStateWithLifecycle();val account by m.account.collectAsStateWithLifecycle()
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        item {AccountPanel(m)}
        val user=profile
        if(user!=null) {
            if(user.banner.isNotBlank())item{Cover(user.banner,"Profile banner",Modifier.fillMaxWidth().height(140.dp))}
            item{SectionTitle(user.name,"AniList profile #${user.id}")}
            item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("${user.anime.optInt("minutesWatched")/60}h","watched",Modifier.weight(1f));Stat("${user.anime.optInt("episodesWatched")}","episodes",Modifier.weight(1f));Stat("${user.anime.optDouble("meanScore")}","mean score",Modifier.weight(1f))}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("${user.manga.optInt("chaptersRead")}","chapters",Modifier.weight(1f));Stat("${user.manga.optInt("volumesRead")}","volumes",Modifier.weight(1f))}}
            item {SectionTitle("Anime genres");RankChart(user.anime.optJSONArray("genres")?.objects()?.map {it.text("genre") to it.optInt("count")} .orEmpty())}
            item {SectionTitle("Manga genres");RankChart(user.manga.optJSONArray("genres")?.objects()?.map {it.text("genre") to it.optInt("count")}.orEmpty())}
            item {SectionTitle("About");RichAbout(user.about)}
        }
        if(error.isNotBlank())item {ErrorNotice(error){m.refreshProfile()}}
        if(account!=null)item {TextButton(onClick={m.refreshProfile()}) {Text("Refresh profile")}}
    }
}
@Composable internal fun RankChart(rows:List<Pair<String,Int>>) {
    val max=(rows.maxOfOrNull {it.second} ?: 1).coerceAtLeast(1)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {if(rows.isEmpty())Text("Track some stories to see this chart.",fontSize=13.sp)
        rows.forEach {(name,count)->Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,fontSize=12.sp);Text(count.toString(),fontSize=12.sp)};LinearProgressIndicator(progress={count.toFloat()/max},modifier=Modifier.fillMaxWidth().height(8.dp))}}
    }
}
@Composable internal fun StatsScreen(entries:List<LibraryEntry>,m:AstroViewModel) {
    val history by m.history.collectAsStateWithLifecycle();var period by rememberSaveable{mutableStateOf("This month")};val zone=ZoneId.systemDefault();val today=LocalDate.now(zone)
    val start=when(period){"This month"->today.withDayOfMonth(1);"This year"->today.withDayOfYear(1);else->LocalDate.of(1970,1,1)}.atStartOfDay(zone).toInstant().toEpochMilli()
    val events=history.filter {it.at>=start};val rated=entries.filter {it.score>0}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {SectionTitle("Astro stats","Your collection at a glance")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("${entries.count {it.status=="COMPLETED"}}","completed",Modifier.weight(1f));Stat("${entries.filter {it.type=="ANIME"}.sumOf {it.progress.toLong()*it.duration}/60}h","tracked runtime",Modifier.weight(1f));Stat(if(rated.isEmpty()) "—" else "${rated.map {it.score}.average().toInt()}","mean rating",Modifier.weight(1f))}}
        item {SectionTitle("Collection status");RankChart(entries.groupingBy {it.status.pretty()}.eachCount().toList().sortedByDescending {it.second})}
        item {SectionTitle("Favourite genres");RankChart(entries.flatMap {it.genres.split("|").filter(String::isNotBlank)}.groupingBy {it}.eachCount().toList().sortedByDescending {it.second}.take(8))}
        item {SectionTitle("Most tracked studios");RankChart(entries.flatMap {it.studios.split("|").filter(String::isNotBlank)}.groupingBy {it}.eachCount().toList().sortedByDescending {it.second}.take(8))}
        item {SectionTitle("Astro Wrapped","Based on progress changes recorded in this app, starting with this version.");Choice("Period",period,listOf("This month","This year","All recorded history")){period=it.ifBlank {"This month"}}}
        item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("${events.filter {it.unit=="episode"}.sumOf {it.delta}.coerceAtLeast(0)}","net episodes",Modifier.weight(1f));Stat("${events.sumOf {it.minutes}.coerceAtLeast(0)/60}h","logged runtime",Modifier.weight(1f));Stat("${events.filter {it.unit!="episode"}.sumOf {it.delta}.coerceAtLeast(0)}","reading updates",Modifier.weight(1f))}}
        item {SectionTitle("Viewing patterns","Positive progress updates by weekday; edits aren't a watch-time sensor.");RankChart(DayOfWeek.entries.map {day->day.name.pretty() to events.filter {it.delta>0 && Instant.ofEpochMilli(it.at).atZone(zone).dayOfWeek==day}.sumOf {it.delta}})}
        item {SectionTitle("Recent chapter & episode history")}
        items(events.take(30),key={it.id}) {e->Text("${entries.firstOrNull {it.mediaId==e.mediaId}?.title ?: "Title #${e.mediaId}"} · ${if(e.delta>0) "+" else ""}${e.delta} ${e.unit}\n${Instant.ofEpochMilli(e.at).atZone(zone).toLocalDate()}",fontSize=13.sp)}
    }
}
@Composable internal fun QuizScreen(m:AstroViewModel,p:Preferences) {
    val pool by m.spotlight.collectAsStateWithLifecycle();val context=androidx.compose.ui.platform.LocalContext.current
    val prefs=remember {context.getSharedPreferences("astro_quiz",0)}
    var mode by rememberSaveable{mutableStateOf("Cover")};var difficulty by rememberSaveable{mutableStateOf("Easy")}
    var rounds by remember {mutableStateOf<List<QuizRound>>(emptyList())};var index by rememberSaveable{mutableIntStateOf(0)};var answer by rememberSaveable{mutableStateOf("")};var score by rememberSaveable{mutableIntStateOf(0)}
    var wins by remember{mutableIntStateOf(prefs.getInt("wins",0))};var streak by remember{mutableIntStateOf(prefs.getInt("streak",0))}
    val done=rounds.isNotEmpty() && index>=rounds.size
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {Chibi(Modifier.size(84.dp));SectionTitle("Anime Quiz","Guess stories from covers, clues or metadata. No plot trivia.")}
        if(rounds.isEmpty()) {
            item {Choice("Mode",mode,listOf("Cover","Clues","Trivia")){mode=it.ifBlank {"Cover"}};Choice("Difficulty",difficulty,listOf("Easy","Medium","Hard","Hashira")){difficulty=it.ifBlank {"Easy"}}}
            item {Text("$wins quiz wins · $streak day streak");if(wins>=1)Text("🏆 First orbit mastered");if(wins>=10)Text("🌌 Ten quiz victories");if(wins>=100)Text("⚔ 100 trivia wins")}
            item {Button(onClick={rounds=quizRounds(pool,mode,difficulty in listOf("Hard","Hashira"));index=0;answer="";score=0},enabled=pool.size>=4){Text(if(pool.size<4) "Waiting for AniList titles…" else "Start 10 rounds")}}
        } else if(done) {
            item {SectionTitle("${score} / ${rounds.size}","Your knowledge this round: ${score*100/rounds.size}%");Button(onClick={rounds=emptyList();index=0}) {Text("Play again")}}
        } else {
            val round=rounds[index]
            item {Text("Question ${index+1} / ${rounds.size} · $score correct");SectionTitle(round.prompt)}
            if(mode=="Cover")item {Cover(round.media.cover,"Mystery cover",Modifier.width(200.dp).height(280.dp).then(if(answer.isBlank() && difficulty!="Easy") Modifier.blur(if(difficulty=="Hashira") 20.dp else if(difficulty=="Hard") 12.dp else 5.dp) else Modifier))}
            if(mode=="Clues")item {round.clues.take(if(difficulty=="Hashira") 2 else if(difficulty=="Hard") 3 else 4).forEach {Text(it)}}
            items(round.choices){choice->OutlinedButton(onClick={if(answer.isBlank()){answer=choice;if(choice==round.answer)score++}},enabled=answer.isBlank(),modifier=Modifier.fillMaxWidth()){Text(choice)}}
            if(answer.isNotBlank())item {Text(if(answer==round.answer) "Correct! ✨" else "Answer: ${round.answer}",color=MaterialTheme.colorScheme.primary)
                Button(onClick={index++;answer="";if(index>=rounds.size){
                    if(score>=7){wins++;prefs.edit().putInt("wins",wins).apply()}
                    val today=LocalDate.now().toEpochDay();val last=prefs.getLong("lastDay",0);if(today!=last){streak=if(last==today-1)streak+1 else 1;prefs.edit().putLong("lastDay",today).putInt("streak",streak).apply()}
                }}) {Text(if(index==rounds.lastIndex) "See results" else "Next question")}}
        }
    }
}
@Composable internal fun RouletteScreen(entries:List<LibraryEntry>,m:AstroViewModel) {
    var genre by rememberSaveable{mutableStateOf("")};var time by rememberSaveable{mutableStateOf("")};var pick by remember{mutableStateOf<LibraryEntry?>(null)}
    val genres=entries.flatMap {it.genres.split("|").filter(String::isNotBlank)}.distinct().sorted()
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {SectionTitle("Let your orbit choose","Pick a title from Plan to watch / read")}
        item {Choice("Mood / genre",genre,genres){genre=it};Choice("Minutes for one episode",time,listOf("15","25","45","90","180")){time=it}}
        item {Button(onClick={pick=entries.filter {it.status=="PLANNING" && (genre.isBlank()||genre in it.genres.split("|")) && (time.isBlank()||it.type=="ANIME" && it.duration>0 && it.duration<=(time.toIntOrNull() ?: 0))}.randomOrNull();if(pick==null)m.announce("No planned titles match those filters.")}) {Text("Spin my orbit")}}
        pick?.let {e->item {MediaCard(e.asMedia(),Modifier.width(220.dp)){m.open(e.mediaId)}}}
    }
}
@Composable internal fun CompatibilityScreen(entries:List<LibraryEntry>,m:AstroViewModel) {
    var name by rememberSaveable{mutableStateOf("")};val result by m.comparison.collectAsStateWithLifecycle();val error by m.comparisonError.collectAsStateWithLifecycle()
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {SectionTitle("Two orbits, shared stories","Compare a public AniList profile. This does not sign in as them.")}
        item {OutlinedTextField(name,{name=it.take(100)},label={Text("AniList username")},singleLine=true);Button(onClick={m.compare(name)},enabled=name.isNotBlank()){Text("Compare tastes")}}
        if(error.isNotBlank())item {Text(error,color=MaterialTheme.colorScheme.error)}
        result?.let {(user,list)->val (percent,shared)=compatibility(entries,list)
            item {Cover(user.avatar,user.name,Modifier.size(72.dp));SectionTitle(user.name,"${shared.size} shared stories · ${percent?.let {"$it% rating agreement"} ?: "No shared ratings yet"}");Text("Agreement = 100 − average rating difference, using shared rated titles on a 100-point scale.",fontSize=12.sp)}
            items(shared.sortedByDescending {if(it.first.score>0&&it.second.score>0) abs(it.first.score-it.second.score) else -1}.take(40),key={it.first.mediaId}) {(a,b)->TextButton(onClick={m.open(a.mediaId)}) {Text("${a.title}\nYou: ${a.score.takeIf {it>0} ?: "—"} · ${user.name}: ${b.score.takeIf {it>0} ?: "—"}")}}
        }
    }
}
@Composable internal fun ConstellationScreen(entries:List<LibraryEntry>,m:AstroViewModel) {
    val stars=remember(entries){entries.sortedByDescending {it.score}.take(100)};var selected by rememberSaveable{mutableIntStateOf(0)}
    val genres=remember(stars){stars.flatMap {it.genres.split("|").filter(String::isNotBlank)}.distinct().sorted()}
    val points=remember(stars){stars.mapIndexed {i,e->val group=(genres.indexOf(e.genres.substringBefore("|")).coerceAtLeast(0)%8);val theta=group*2*PI/8;val r=.1+(i%6)*.019
        Offset((.5+cos(theta)*.28+cos(i*2.4)*r).toFloat(),(.5+sin(theta)*.28+sin(i*2.4)*r).toFloat())}}
    val accent=MaterialTheme.colorScheme.primary
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {SectionTitle("Anime constellations","Up to 100 collection stars · grouped by first genre · size follows your rating")}
        item {Canvas(Modifier.fillMaxWidth().height(360.dp).background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.large).pointerInput(points){detectTapGestures {tap->val index=points.indices.minByOrNull {i->(Offset(points[i].x*size.width,points[i].y*size.height)-tap).getDistance()};if(index!=null)selected=stars[index].mediaId}}) {
            val active=stars.firstOrNull {it.mediaId==selected}
            if(active!=null){val from=points[stars.indexOf(active)];stars.forEachIndexed {i,e->if(e.mediaId!=selected&&e.genres.split("|").any {it.isNotBlank()&&it in active.genres.split("|")})drawLine(accent.copy(alpha=.25f),Offset(from.x*size.width,from.y*size.height),Offset(points[i].x*size.width,points[i].y*size.height),2f)}}
            stars.forEachIndexed {i,e->val color=Color.hsv(((genres.indexOf(e.genres.substringBefore("|")).coerceAtLeast(0)*47)%360).toFloat(),.35f,1f);drawCircle(color,if(e.mediaId==selected) 12.dp.toPx() else (3+e.score/20).dp.toPx(),Offset(points[i].x*size.width,points[i].y*size.height))}
        }}
        stars.firstOrNull {it.mediaId==selected}?.let {e->item{SectionTitle(e.title,e.genres.replace("|"," • "));Button(onClick={m.open(e.mediaId)}){Text("Open story")}}}
        item {Text("Accessible title list",fontWeight=FontWeight.Bold)}
        items(stars,key={it.mediaId}) {e->TextButton(onClick={selected=e.mediaId}) {Text("${e.title} · ${e.score.takeIf {it>0} ?: "Unrated"}")}}
        if(stars.isEmpty())item {Notice("Your galaxy is waiting","Add stories to your collection to see their connections.")}
    }
}
