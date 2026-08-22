package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.data.model.LanguageMode
import com.example.domain.LanguageDetector
import com.example.domain.VoiceAssistantEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VoiceAI Assistant", appName)
  }

  @Test
  fun `detect Nepali language`() {
    val lang = LanguageDetector.detectLanguage("फ्ल्यासलाइट बालिदिनुहोस्")
    assertEquals(LanguageMode.NEPALI, lang)
  }

  @Test
  fun `detect Hindi language`() {
    val lang = LanguageDetector.detectLanguage("फ्लैशलाइट चालू करो")
    assertEquals(LanguageMode.HINDI, lang)
  }

  @Test
  fun `detect English language`() {
    val lang = LanguageDetector.detectLanguage("Turn on the flashlight for me")
    assertEquals(LanguageMode.ENGLISH, lang)
  }

  @Test
  fun `process flashlight local intent`() {
    val engine = VoiceAssistantEngine()
    val res = engine.processLocally("फ्ल्यासलाइट बाल", LanguageMode.NEPALI)
    assertEquals(ActionType.TOGGLE_FLASHLIGHT, res.action?.actionType)
    assertEquals("ne", res.detectedLanguage)
    assertTrue(res.spokenText.contains("फ्ल्यासलाइट"))
  }
}
