package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LanguageMode
import com.example.domain.WakeWordManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WakeWordUnitTest {

    private lateinit var wakeWordManager: WakeWordManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        wakeWordManager = WakeWordManager(context)
        wakeWordManager.setWakeWord("Hi Alex")
    }

    @Test
    fun testDefaultWakeWord() {
        assertEquals("Hi Alex", wakeWordManager.getWakeWord())
    }

    @Test
    fun testHiAlexTrigger() {
        val result = wakeWordManager.analyzeWakeWord("Hi Alex turn on the flashlight")
        assertTrue(result.isTriggered)
        assertEquals("Hi Alex", result.matchedWakeWord)
        assertEquals("turn on the flashlight", result.strippedCommand)
        assertFalse(result.isOnlyWakeWord)

        val resultOnly = wakeWordManager.analyzeWakeWord("Hi Alex")
        assertTrue(resultOnly.isTriggered)
        assertTrue(resultOnly.isOnlyWakeWord)

        val resultAlexOnly = wakeWordManager.analyzeWakeWord("Alex")
        assertTrue(resultAlexOnly.isTriggered)
        assertTrue(resultAlexOnly.isOnlyWakeWord)

        val resultWakeUp = wakeWordManager.analyzeWakeWord("Wake up Alex")
        assertTrue(resultWakeUp.isTriggered)
        assertTrue(resultWakeUp.isOnlyWakeWord)

        val resultWakeUpCommand = wakeWordManager.analyzeWakeWord("Wake up Alex turn on flashlight")
        assertTrue(resultWakeUpCommand.isTriggered)
        assertEquals("turn on flashlight", resultWakeUpCommand.strippedCommand)
    }

    @Test
    fun testHandsFreeDefaultEnabled() {
        assertTrue(wakeWordManager.isHandsFreeEnabled())
    }

    @Test
    fun testSetCustomWakeWord() {
        val saved = wakeWordManager.setWakeWord("Jarvis")
        assertEquals("Jarvis", saved)
        assertEquals("Jarvis", wakeWordManager.getWakeWord())

        val result = wakeWordManager.analyzeWakeWord("Jarvis turn on the flashlight")
        assertTrue(result.isTriggered)
        assertEquals("Jarvis", result.matchedWakeWord)
        assertEquals("turn on the flashlight", result.strippedCommand)
        assertFalse(result.isOnlyWakeWord)
    }

    @Test
    fun testOnlyWakeWordSpoken() {
        wakeWordManager.setWakeWord("Nova")
        val result = wakeWordManager.analyzeWakeWord("Nova")
        assertTrue(result.isTriggered)
        assertTrue(result.isOnlyWakeWord)
        assertEquals("", result.strippedCommand)
    }

    @Test
    fun testVoiceChangeWakeWordEnglish() {
        val requested = wakeWordManager.checkWakeWordChangeRequest("Change wake word to Jarvis")
        assertNotNull(requested)
        assertEquals("Jarvis", requested)

        val requested2 = wakeWordManager.checkWakeWordChangeRequest("Set my wake word to Nova")
        assertNotNull(requested2)
        assertEquals("Nova", requested2)
    }

    @Test
    fun testVoiceChangeWakeWordHindi() {
        val requested = wakeWordManager.checkWakeWordChangeRequest("वेक वर्ड जार्विस रखो")
        assertNotNull(requested)
        assertEquals("जार्विस", requested)
    }

    @Test
    fun testVoiceChangeWakeWordNepali() {
        val requested = wakeWordManager.checkWakeWordChangeRequest("वेक वर्ड साथी राख")
        assertNotNull(requested)
        assertEquals("साथी", requested)
    }
}
