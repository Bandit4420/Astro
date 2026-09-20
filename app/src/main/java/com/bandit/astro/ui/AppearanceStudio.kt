package com.bandit.astro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bandit.astro.AstroViewModel
import com.bandit.astro.data.Preferences

private val swatches=listOf("Mint" to "#A5E9CE","Ice" to "#98D8FF","Sakura" to "#FFB5D0","Lilac" to "#C8B5FF","Gold" to "#F5CE91","Coral" to "#FF9B91","White" to "#E0E3E8")

@Composable internal fun AppearanceStudio(p:Preferences,m:AstroViewModel) {
    Column(Modifier.testTag("appearance:${p.theme}:${p.amoled}"),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        SectionTitle("Appearance","A little more you. Every detail.")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(themeNames,key={it}) {name->
            val preview=when(name){"Sakura"->Color(0xFFFFB5D0);"Lavender","Cyberpunk"->Color(0xFFC8B5FF);"Golden Hour"->Color(0xFFF5CE91);"Ocean","Midnight Blue","Liquid Glass"->Color(0xFF98D8FF);else->Color(0xFFA5E9CE)}
            val selected=p.theme.equals(name,true)
            Surface(onClick={m.theme(name)},modifier=Modifier.width(132.dp),shape=MaterialTheme.shapes.large,
                color=if(name=="AMOLED")Color.Black else Color(0xFF141B23),
                border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)preview else Color.White.copy(alpha=.14f))) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){repeat(3){Box(Modifier.size(if(it==0)26.dp else 18.dp).clip(CircleShape).background(preview.copy(alpha=1f-it*.25f)))}}
                    Box(Modifier.fillMaxWidth().height(35.dp).clip(MaterialTheme.shapes.small).background(preview.copy(alpha=.15f)))
                    Text(name,style=MaterialTheme.typography.labelLarge,color=Color.White)
                    Text(if(selected)"Selected" else "Preview",style=MaterialTheme.typography.labelSmall,color=preview)
                }
            }
        }}
        SettingsGroup("Color & contrast") {
            ToggleRow("AMOLED black","Pure-black canvas with your chosen colors. Turns off background glass effects.",p.amoled||p.theme=="AMOLED") {enabled->
                if(!enabled&&p.theme=="AMOLED")m.theme("Astro")
                m.flag("amoled",enabled)
            }
            AccentPicker("Interface accent",p.accentHex){m.appearanceText("accentHex",it)}
            AccentPicker("Button accent",p.buttonAccent){m.appearanceText("buttonAccent",it)}
            AccentPicker("Navigation accent",p.navigationAccent){m.appearanceText("navigationAccent",it)}
            ToggleRow("Artwork accents","Let title artwork color the interface. Your button and navigation choices stay independent.",p.dynamic){m.flag("dynamic",it)}
        }
        SettingsGroup("Surfaces & motion") {AppearanceOptions(p,m)}
        SettingsGroup("Typography") {
            Text("Heading style",style=MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Modern","System","Serif")){style->
                FilterChip(selected=p.fontStyle==style,onClick={m.appearanceText("fontStyle",style)},label={Text(style)})
            }}
            Text("Your universe, beautifully in focus.",style=MaterialTheme.typography.headlineMedium)
            Text("Readable spacing, a calmer hierarchy, and support for Android's font-size setting.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun SettingsGroup(title:String,content:@Composable ColumnScope.()->Unit) {
    Surface(shape=MaterialTheme.shapes.large,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.22f)),tonalElevation=1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable private fun AccentPicker(title:String,value:String,changed:(String)->Unit) {
    var custom by rememberSaveable(title,value){mutableStateOf(value)}
    var editing by rememberSaveable(title){mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,style=MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item {FilterChip(selected=value.isBlank(),onClick={changed("")},label={Text("Follow theme")})}
            items(swatches){(name,hex)->FilterChip(selected=value.equals(hex,true),onClick={changed(hex)},
                leadingIcon={Box(Modifier.size(14.dp).clip(CircleShape).background(parseAccent(hex,Color.White)))},label={Text(name)})}
            item{AssistChip(onClick={editing=!editing},label={Text("Custom")})}
        }
        if(editing) {
            OutlinedTextField(custom,{custom=it.take(7)},label={Text("Hex color · #RRGGBB")},singleLine=true,modifier=Modifier.fillMaxWidth())
            TextButton(onClick={changed(custom);editing=false},enabled=custom.matches(Regex("#[0-9a-fA-F]{6}"))){Text("Apply color")}
        }
    }
}

@Composable internal fun AstroButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit) {
    val p=LocalAppearance.current;val accent=parseAccent(p.buttonAccent,MaterialTheme.colorScheme.primary)
    val interaction=remember {MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed&&p.animations).97f else 1f,spring(stiffness=700f),label="Button press")
    Button(onClick,modifier.graphicsLayer {scaleX=scale;scaleY=scale},enabled=enabled,interactionSource=interaction,
        colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=accentContent(accent)),content=content)
}
@Composable internal fun AstroOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit) {
    val accent=parseAccent(LocalAppearance.current.buttonAccent,MaterialTheme.colorScheme.primary)
    OutlinedButton(onClick,modifier,enabled=enabled,colors=ButtonDefaults.outlinedButtonColors(contentColor=accent),
        border=BorderStroke(1.dp,accent.copy(alpha=if(enabled).5f else .15f)),content=content)
}

@Composable internal fun AstroProgressButton(onClick:()->Unit,enabled:Boolean,content:@Composable ()->Unit) {
    val accent=parseAccent(LocalAppearance.current.buttonAccent,MaterialTheme.colorScheme.primary)
    FilledTonalIconButton(onClick,enabled=enabled,colors=IconButtonDefaults.filledTonalIconButtonColors(
        containerColor=accent,contentColor=accentContent(accent)),content=content)
}
