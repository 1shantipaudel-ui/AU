package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.data.model.LanguageMode
import com.example.domain.SpeechManager
import com.example.domain.VoiceAssistantEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testSpeechSanitizerRemovesMarkdownAndSymbols() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val speechManager = SpeechManager(context, {}, {})

        val raw = "**Flashlight** turned *on*! Visit `https://google.com` - item #1 🚀"
        val clean = speechManager.sanitizeTextForSpeech(raw)

        assertFalse(clean.contains("**"))
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("`"))
        assertFalse(clean.contains("https://"))
        assertTrue(clean.contains("Flashlight turned on!"))
    }

    @Test
    fun testVoiceAssistantEngineDirectFlashlightCommand() {
        val engine = VoiceAssistantEngine()

        val responseEn = engine.processLocally("turn on flashlight", LanguageMode.ENGLISH)
        assertNotNull(responseEn.action)
        assertEquals(ActionType.TOGGLE_FLASHLIGHT, responseEn.action?.actionType)
        assertEquals("on", responseEn.action?.value)
        assertEquals("Flashlight turned on.", responseEn.spokenText)

        val responseHi = engine.processLocally("फ्लैशलाइट चालू करो", LanguageMode.HINDI)
        assertNotNull(responseHi.action)
        assertEquals(ActionType.TOGGLE_FLASHLIGHT, responseHi.action?.actionType)
        assertEquals("फ्लैशलाइट चालू कर दी गई है।", responseHi.spokenText)

        val responseNe = engine.processLocally("फ्ल्यासलाइट बाल", LanguageMode.NEPALI)
        assertNotNull(responseNe.action)
        assertEquals(ActionType.TOGGLE_FLASHLIGHT, responseNe.action?.actionType)
        assertEquals("फ्ल्यासलाइट बालिएको छ।", responseNe.spokenText)
    }

    @Test
    fun testVoiceAssistantEngineDirectAlarmCommand() {
        val engine = VoiceAssistantEngine()

        val response = engine.processLocally("set alarm for 6 am", LanguageMode.ENGLISH)
        assertNotNull(response.action)
        assertEquals(ActionType.SET_ALARM, response.action?.actionType)
        assertEquals(6, response.action?.hour)
        assertEquals("Alarm set for 06:00.", response.spokenText)
    }
}


