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
    private fun screenshot(name:String) {
        compose.waitForIdle()
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val resolver=instrumentation.targetContext.contentResolver
        val values=android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/AstroTest")
        }
        val uri=resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
        instrumentation.uiAutomation.takeScreenshot()?.let {image->resolver.openOutputStream(uri)!!.use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};image.recycle()}

    }
    @Test fun primaryNavigationAndProfileOpen() {
        compose.onNodeWithText("MISSION CONTROL").assertExists();screenshot("01-orbit")
        compose.onNodeWithText("Collection").performClick()
        compose.onNodeWithText("Your collection").assertExists()
        compose.onNodeWithText("Search your collection").performTextInput("Frieren")
        compose.onNodeWithText("Novels").performClick()
        compose.onNodeWithText("Search your collection").performTextClearance()
        screenshot("02-collection")
        compose.onNodeWithText("Explore").performClick()
        compose.onNodeWithText("A whole world awaits").assertExists();screenshot("03-explore")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Make yourself at home").assertExists();screenshot("04-settings")
        compose.onNodeWithContentDescription("Profile & sign in").performClick()
        compose.onNodeWithText("AniList account").assertExists()
        compose.onNodeWithText("Sign in with AniList").assertIsEnabled()
        compose.onNodeWithText("Advanced login settings").performClick()
        compose.onNodeWithText("Native client ID").assertExists();screenshot("05-account")
    }
}
