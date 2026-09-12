package com.bandit.astro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun primaryNavigationAndProfileOpen() {
        compose.onNodeWithText("MISSION CONTROL").assertExists()
        compose.onNodeWithText("Collection").performClick()
        compose.onNodeWithText("Your collection").assertExists()
        compose.onNodeWithText("Explore").performClick()
        compose.onNodeWithText("A whole world awaits").assertExists()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Make yourself at home").assertExists()
        compose.onNodeWithContentDescription("Profile & sign in").performClick()
        compose.onNodeWithText("AniList account").assertExists()
        compose.onNodeWithText("Set up native login").performClick()
        compose.onNodeWithText("Native client ID").assertExists()
    }
}
