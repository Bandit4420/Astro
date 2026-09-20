package com.bandit.astro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bandit.astro.*
import com.bandit.astro.data.*

@Composable internal fun ExploreFilterPanel(state:Discovery,m:AstroViewModel) {
    var open by remember {mutableStateOf(false)};var draft by remember(state.filters){mutableStateOf(state.filters)};var studio by remember{mutableStateOf("")}
    val studios by m.studios.collectAsStateWithLifecycle()
    Row(Modifier.padding(horizontal=20.dp)) {TextButton(onClick={draft=state.filters;open=true}) {Text("Refine your orbit")};if(state.filters!=ExploreFilters())TextButton(onClick={m.filter(ExploreFilters())}) {Text("Reset")}}
    if(open) AlertDialog(onDismissRequest={open=false},title={Text("Explore filters")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Choice("Genre",draft.genre,listOf("Action","Adventure","Comedy","Drama","Fantasy","Horror","Mahou Shoujo","Mecha","Music","Mystery","Psychological","Romance","Sci-Fi","Slice of Life","Sports","Supernatural","Thriller")){draft=draft.copy(genre=it)}
        if(draft.studioId==0) OutlinedTextField(draft.tag,{draft=draft.copy(tag=it.take(80))},label={Text("Tag (e.g. Found Family)")})
        OutlinedTextField(draft.year,{draft=draft.copy(year=it.filter(Char::isDigit).take(4))},label={Text("Release year")},singleLine=true)
        if(state.catalog==Catalog.ANIME) Choice("Season",draft.season,listOf("WINTER","SPRING","SUMMER","FALL")){draft=draft.copy(season=it)}
        if(state.catalog!=Catalog.NOVEL) Choice("Format",draft.format,if(state.catalog==Catalog.ANIME)listOf("TV","TV_SHORT","MOVIE","SPECIAL","OVA","ONA","MUSIC") else listOf("MANGA","ONE_SHOT")){draft=draft.copy(format=it)}
        Choice("Status",draft.status,listOf("RELEASING","FINISHED","NOT_YET_RELEASED","HIATUS","CANCELLED")){draft=draft.copy(status=it)}
        Choice("Minimum score",draft.minScore,listOf("50","60","70","80","90")){draft=draft.copy(minScore=it)}
        if(state.catalog==Catalog.ANIME) {
            OutlinedTextField(draft.minEpisodes,{draft=draft.copy(minEpisodes=it.filter(Char::isDigit).take(5))},label={Text("Minimum episodes")})
            OutlinedTextField(draft.maxEpisodes,{draft=draft.copy(maxEpisodes=it.filter(Char::isDigit).take(5))},label={Text("Maximum episodes")})
            OutlinedTextField(studio,{studio=it;m.studioSearch(it)},label={Text("Search studios")})
            studios.forEach {s->TextButton(onClick={draft=draft.copy(studioId=s.first,studioName=s.second,tag="",sort="POPULARITY_DESC");studio="";m.studioSearch("")}) {Text(s.second)}}
            if(draft.studioId>0) {Text("Studio: ${draft.studioName}. Other filters apply as each studio page arrives.");TextButton(onClick={draft=draft.copy(studioId=0,studioName="")}) {Text("Clear studio")}}
        }
        if(draft.studioId==0) Choice("Sort",draft.sort,listOf("TRENDING_DESC","POPULARITY_DESC","SCORE_DESC","START_DATE_DESC","TITLE_ROMAJI")){draft=draft.copy(sort=it.ifBlank {"TRENDING_DESC"})}
    }},confirmButton={TextButton(onClick={m.filter(draft);open=false}) {Text("Apply")}},dismissButton={TextButton(onClick={open=false}) {Text("Cancel")}})
}
@Composable internal fun Choice(label:String,value:String,options:List<String>,changed:(String)->Unit) {var open by remember{mutableStateOf(false)};Box {
    AstroOutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {Text("$label: ${value.ifBlank {"Any"}.pretty()}")}
    DropdownMenu(expanded=open,onDismissRequest={open=false}) {(listOf("")+options).forEach {v->DropdownMenuItem(text={Text(v.ifBlank {"Any"}.pretty())},onClick={changed(v);open=false})}}
}}
