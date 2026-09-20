package com.bandit.astro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class OrbitNotice(
    override val message:String,
    val description:String="",
    val mood:String="info",
    override val actionLabel:String?=null,
    override val withDismissAction:Boolean=true,
    override val duration:SnackbarDuration=SnackbarDuration.Long
):SnackbarVisuals

@Composable internal fun RichChibiNotice(data:SnackbarData) {
    val notice=data.visuals as? OrbitNotice
    val accent=if(notice?.mood=="error")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(Modifier.padding(12.dp),shape=MaterialTheme.shapes.large,tonalElevation=8.dp,shadowElevation=6.dp){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
            Chibi(Modifier.size(70.dp))
            Column(Modifier.weight(1f).padding(horizontal=10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(data.visuals.message,fontWeight=FontWeight.Bold,color=accent)
                if(!notice?.description.isNullOrBlank())Text(notice!!.description,fontSize=12.sp)
                data.visuals.actionLabel?.let {label->FilledTonalButton(onClick={data.performAction()}){Text(label)}}
            }
            TextButton(onClick={data.dismiss()}){Text("×")}
        }
    }
}
