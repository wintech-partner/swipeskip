package org.example.autoshorts.ui

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import org.example.autoshorts.AutoshortsApp
import org.example.autoshorts.core.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Resource-file checks run on the JVM; the working directory of Gradle unit tests is the module (app/). */
class StringResourcesTest {
    private val res = File("src/main/res")

    private fun load(dir: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
    }

    private val locales = res.listFiles()!!.map { it.name }.filter { it.startsWith("values-") && File(it.let { n -> File(res, n) }, "strings.xml").exists() }
    private val placeholder = Regex("%(\\d\\$)?[sd]")

    @Test fun `sixteen languages are present`() = assertEquals(16, locales.size)

    @Test fun `every locale has every key`() {
        val en = load("values").keys
        locales.forEach { assertEquals(it, emptySet<String>(), en - load(it).keys) }
    }

    @Test fun `placeholders match the English source in every locale`() {
        val en = load("values")
        locales.forEach { loc ->
            load(loc).forEach { (k, v) ->
                assertEquals("$loc/$k", placeholder.findAll(en.getValue(k)).map { it.value }.sorted().toList(),
                    placeholder.findAll(v).map { it.value }.sorted().toList())
            }
        }
    }

    @Test fun `no translation is left identical to a long English sentence`() {
        val en = load("values")
        locales.forEach { loc ->
            load(loc).forEach { (k, v) -> assertTrue("$loc/$k untranslated", v != en.getValue(k) || en.getValue(k).length < 20) }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = AutoshortsApp::class, qualifiers = "ar-rSA")
class RtlTest {
    @get:Rule val compose = createComposeRule()

    @Test fun arabicRendersRightToLeft() {
        var direction: LayoutDirection? = null
        val vm = MainViewModel(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        compose.setContent {
            direction = LocalLayoutDirection.current
            OnboardingScreen(vm, UiState(settings = AppSettings()), onDone = {})
        }
        assertEquals(LayoutDirection.Rtl, direction)
        compose.onNodeWithText("فهمت وأوافق").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = AutoshortsApp::class, qualifiers = "de")
class LongStringTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longestGermanTextsStillRenderOnASmallScreen() {
        val vm = MainViewModel(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        compose.setContent {
            HomeScreen(vm, UiState(settings = AppSettings(consentGiven = true), serviceConnected = false), {}, {}, {}, {})
        }
        compose.onNodeWithText("Einrichtung fortsetzen").assertIsDisplayed()
    }
}
