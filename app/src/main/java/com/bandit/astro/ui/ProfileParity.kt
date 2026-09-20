package com.bandit.astro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandit.astro.data.*

@Composable internal fun PeopleStrip(title:String,people:List<Credit>,open:(Int)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        SectionTitle(title)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(people,key={it.id}){person->Column(Modifier.width(125.dp).clickable {open(person.id)}){Cover(person.image,person.name,Modifier.fillMaxWidth().height(160.dp));Text(person.name,fontWeight=FontWeight.SemiBold,maxLines=2)}}}
        if(people.isEmpty())Text("No credits listed.",fontSize=12.sp)
    }
}
@Composable internal fun ProfileCharts(user:UserProfile) {
    var metric by rememberSaveable {mutableStateOf("Titles")}
    val statuses=user.anime.optJSONArray("statuses")?.objects()?.map {it.text("status").pretty() to it.optInt("count")}.orEmpty().filter {it.second>0}
    val colors=listOf(Color(0xFFA5EFCA),Color(0xFFA6BDF8),Color(0xFFDDBCFA),Color(0xFFEFCE95),Color(0xFFEB9EAB),Color(0xFF86DCE4))
    val total=statuses.sumOf {it.second}
    Column(verticalArrangement=Arrangement.spacedBy(22.dp)){
        SectionTitle("Your collection","AniList profile totals")
        Text("${user.anime.optJSONArray("statuses")?.objects()?.firstOrNull {it.text("status")=="COMPLETED"}?.optInt("count") ?: 0} anime completed",fontWeight=FontWeight.Bold)
        if(total>0) {
            Box(Modifier.fillMaxWidth().height(190.dp),contentAlignment=Alignment.Center){
                Canvas(Modifier.size(165.dp)){var start=-90f;statuses.forEachIndexed {i,(_,count)->val sweep=count.toFloat()/total*360;drawArc(colors[i%colors.size],start,sweep,false,size=Size(size.width,size.height),style=Stroke(22.dp.toPx()));start+=sweep}}
                Column(horizontalAlignment=Alignment.CenterHorizontally){Text(total.toString(),fontSize=30.sp,fontWeight=FontWeight.Bold);Text("anime",fontSize=12.sp)}
            }
            statuses.forEachIndexed {i,(label,count)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=colors[i%colors.size]);Text(count.toString())}}
        }else Text("No watch statuses yet.")
        SectionTitle("Favorite anime genres")
        Choice("Genre metric",metric,listOf("Titles","Hours")){metric=it.ifBlank {"Titles"}}
        RankChart(user.anime.optJSONArray("genres")?.objects()?.map {it.text("genre") to if(metric=="Hours")it.optInt("minutesWatched")/60 else it.optInt("count")}.orEmpty())
        SectionTitle("How you rate anime","Score distribution · 0–100")
        RankChart(user.anime.optJSONArray("scores")?.objects()?.filter {it.optInt("score")>0}?.sortedBy {it.optInt("score")}?.map {"${it.optInt("score")} / 100" to it.optInt("count")}.orEmpty())
        SectionTitle("The eras you explore","Anime by release year")
        RankChart(user.anime.optJSONArray("releaseYears")?.objects()?.filter {it.optInt("releaseYear")>0}?.sortedBy {it.optInt("releaseYear")}?.map {it.optInt("releaseYear").toString() to it.optInt("count")}.orEmpty())
    }
}
