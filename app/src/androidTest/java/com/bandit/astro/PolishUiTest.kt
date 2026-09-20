package com.bandit.astro

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandit.astro.data.Preferences
import com.bandit.astro.ui.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PolishUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun biographyFactsHaveSeparateRowsAndLinksRemainClickable() {
        compose.setContent {AstroTheme(Preferences(),""){Column(Modifier.padding(20.dp)) {
            PersonFacts(listOf("Age" to "19","Birthday" to "22 November","Gender" to "Male"))
            BiographyText("**Biography**\n[Official profile](https://anilist.co/character/1)\nA separate paragraph.")
        }}}
        val age=compose.onNodeWithText("19").fetchSemanticsNode().boundsInRoot
        val birth=compose.onNodeWithText("22 November").fetchSemanticsNode().boundsInRoot
        val gender=compose.onNodeWithText("Male").fetchSemanticsNode().boundsInRoot
        assertTrue(birth.top>=age.bottom);assertTrue(gender.top>=birth.bottom)
        compose.waitForIdle()
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val activity=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
            fun texts(view:android.view.View):List<android.widget.TextView> =
                if(view is android.widget.TextView)listOf(view) else if(view is android.view.ViewGroup)(0 until view.childCount).flatMap {texts(view.getChildAt(it))} else emptyList()
            val text=texts(activity.window.decorView).first {it.text.contains("Official profile")}
            val spans=(text.text as android.text.Spanned).getSpans(0,text.text.length,android.text.style.ClickableSpan::class.java)
            assertTrue(spans.isNotEmpty());assertTrue(text.text.contains("\n"))
        }
    }
}
