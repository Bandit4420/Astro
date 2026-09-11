package com.bandit.astro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandit.astro.ui.AstroApp

class MainActivity : ComponentActivity() {
    private var notificationMediaId by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationMediaId = intent.getIntExtra("mediaId", 0)
        setContent {
            val model: AstroViewModel = viewModel()
            AstroApp(model, notificationMediaId) { notificationMediaId = 0 }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationMediaId = intent.getIntExtra("mediaId", 0)
    }
}
