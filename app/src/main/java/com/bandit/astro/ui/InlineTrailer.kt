package com.bandit.astro.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bandit.astro.data.Media

@SuppressLint("SetJavaScriptEnabled")
@Composable internal fun InlineTrailer(media:Media) {
    if(media.trailerSite!="youtube"||!media.trailerId.matches(Regex("[A-Za-z0-9_-]{11}")))return
    var playing by rememberSaveable(media.id){mutableStateOf(false)}
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        if(!playing)AstroOutlinedButton(onClick={playing=true}){Text("Play trailer here")}
        else {
            val view=remember(media.id){WebView(context).apply {
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                settings.allowFileAccess=false;settings.allowContentAccess=false
                settings.mediaPlaybackRequiresUserGesture=true
                webChromeClient=WebChromeClient()
                webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean = !(r.url.scheme=="https"&&r.url.host=="www.youtube-nocookie.com")}
                val embed="https://www.youtube-nocookie.com/embed/${media.trailerId}?playsinline=1&origin=https%3A%2F%2Fastro-anime-bandit.bandit44.chatgpt.site"
                loadDataWithBaseURL("https://astro-anime-bandit.bandit44.chatgpt.site/","<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:#000'><iframe width='100%' height='100%' style='border:0' src='$embed' allow='encrypted-media; fullscreen; picture-in-picture' allowfullscreen></iframe></body></html>","text/html","UTF-8",null)
            }}
            DisposableEffect(view,lifecycle){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)view.onPause() else if(event==Lifecycle.Event.ON_RESUME)view.onResume()};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);view.stopLoading();view.destroy()}}
            AndroidView(factory={view},modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f))
            TextButton(onClick={playing=false}){Text("Close trailer")}
            Text("If this trailer can't be embedded, use the YouTube button below.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
