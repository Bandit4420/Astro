package com.bandit.astro.ui

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable internal fun BiographyText(value:String, maxLines:Int=Int.MAX_VALUE) {
    val context=LocalContext.current
    val renderer=remember(context) { Markwon.builder(context)
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create(android.text.util.Linkify.WEB_URLS))
        .usePlugin(object:AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder:MarkwonConfiguration.Builder) {
                builder.linkResolver { _,link -> openLink(context,link) }
            }
        }).build() }
    // AniList treats single newlines as explicit line breaks, unlike CommonMark.
    val content=remember(value,renderer) {renderer.toMarkdown(value.replace("\r\n","\n").replace("\n","  \n"))}
    val textColor=MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val linkColor=MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(modifier=Modifier.fillMaxWidth(), factory={ctx-> TextView(ctx).apply {
        textSize=15f;includeFontPadding=false;setLineSpacing(0f,1.45f)
        typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        movementMethod=LinkMovementMethod.getInstance()
    }}, update={view->
        view.setTextColor(textColor);view.setLinkTextColor(linkColor);view.maxLines=maxLines
        if(view.tag !== content) {renderer.setParsedMarkdown(view,content);view.tag=content}
    })
}

@Composable internal fun PersonFacts(facts:List<Pair<String,String>>) {
    if(facts.isEmpty())return
    Surface(shape=MaterialTheme.shapes.large,tonalElevation=1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            facts.forEachIndexed {index,(label,value)->
                Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                }
                if(index<facts.lastIndex)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
            }
        }
    }
}

@Composable internal fun PersonHeader(person:com.bandit.astro.data.Person,staff:Boolean) {
    Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Cover(person.image,person.name,Modifier.width(112.dp).height(166.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Eyebrow(if(staff) "STAFF & VOICES" else "CHARACTER")
                Text(person.name,style=MaterialTheme.typography.headlineMedium)
                if(person.nativeName.isNotBlank())Text(person.nativeName,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
