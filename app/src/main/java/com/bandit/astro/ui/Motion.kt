package com.bandit.astro.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bandit.astro.data.Preferences

@Composable internal fun powerAwarePreferences(p:Preferences):Preferences {
    val context=LocalContext.current
    val power=remember(context){context.getSystemService(PowerManager::class.java)}
    var saver by remember {mutableStateOf(power.isPowerSaveMode)}
    DisposableEffect(context) {
        val receiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){saver=power.isPowerSaveMode}}
        androidx.core.content.ContextCompat.registerReceiver(context,receiver,IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {context.unregisterReceiver(receiver)}
    }
    val effective=if(saver)p.copy(animations=false,blur=false,highRefresh=false) else p
    val owner=LocalLifecycleOwner.current
    DisposableEffect(context,owner,effective.highRefresh) {
        val activity=context.hostActivity()
        fun refresh(resumed:Boolean) {
            if(activity!=null) {
                val current=activity.display?.mode
                val rate=if(resumed&&effective.highRefresh)activity.display?.supportedModes?.filter {
                    it.physicalWidth==current?.physicalWidth && it.physicalHeight==current?.physicalHeight
                }?.maxOfOrNull {it.refreshRate} ?: 0f else 0f
                activity.window.attributes=activity.window.attributes.apply {preferredRefreshRate=rate}
            }
        }
        val observer=LifecycleEventObserver {_,_->refresh(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
        owner.lifecycle.addObserver(observer);refresh(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {owner.lifecycle.removeObserver(observer);refresh(false)}
    }
    return effective
}

@Composable internal fun Modifier.pageEntrance(route:String,enabled:Boolean):Modifier {
    val progress=remember {Animatable(1f)}
    val distance=with(LocalDensity.current){12.dp.toPx()}
    LaunchedEffect(route,enabled) {
        if(enabled) {progress.snapTo(0f);progress.animateTo(1f,tween(220,easing=FastOutSlowInEasing))}
        else progress.snapTo(1f)
    }
    return graphicsLayer {alpha=progress.value;translationY=(1f-progress.value)*distance}
}

private tailrec fun Context.hostActivity():Activity? = when(this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.hostActivity()
    else -> null
}
