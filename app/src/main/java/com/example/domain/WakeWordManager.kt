package com.example.domain

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.LanguageMode
import java.util.regex.Pattern

data class WakeWordResult(
    val isTriggered: Boolean,
    val matchedWakeWord: String,
    val strippedCommand: String,
    val isOnlyWakeWord: Boolean
)

class WakeWordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voice_assistant_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CUSTOM_WAKE_WORD = "custom_wake_word"
        const val KEY_HANDS_FREE_MODE = "hands_free_wake_mode"
        const val DEFAULT_WAKE_WORD = "Hi Alex"

        val PRESET_WAKE_WORDS = listOf(
            "Hi Alex",
            "Alex",
            "Hey Assistant",
            "Jarvis",
            "Namaste Sathi",
            "Suno Dost",
            "Nova",
            "Chitti",
            "Mitra",
            "Computer"
        )
    }

    fun getWakeWord(): String {
        return prefs.getString(KEY_CUSTOM_WAKE_WORD, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD
    }

    fun setWakeWord(wakeWord: String): String {
        val clean = cleanWakeWord(wakeWord)
        val finalWord = if (clean.isNotBlank()) clean else DEFAULT_WAKE_WORD
        prefs.edit().putString(KEY_CUSTOM_WAKE_WORD, finalWord).apply()
        return finalWord
    }

    fun isHandsFreeEnabled(): Boolean {
        return prefs.getBoolean(KEY_HANDS_FREE_MODE, true)
    }

    fun setHandsFreeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HANDS_FREE_MODE, enabled).apply()
    }

    fun cleanWakeWord(input: String): String {
        return input.trim()
            .replace(Regex("^[.,!?:;\"'\\s]+|[.,!?:;\"'\\s]+$"), "")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Checks if the user's speech command contains a request to set or change the wake word.
     * E.g. "Change wake word to Jarvis", "Set wake word to Nova", "वेक वर्ड जार्विस रखो", "वेक वर्ड साथी राख"
     */
    fun checkWakeWordChangeRequest(input: String): String? {
        val text = input.trim()
        val patterns = listOf(
            // English: "set wake word to Jarvis", "change wake word to Hey Sparky", "wake word is Jarvis"
            Pattern.compile("(?i)(?:set|change|update|make|switch)\\s+(?:my\\s+)?(?:wake\\s*word|name|call\\s*you)\\s+(?:to|as|is)?\\s*[:\"]?\\s*([^.!?]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:wake\\s*word)\\s+(?:to|is|set\\s+to)\\s*[:\"]?\\s*([^.!?]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)call\\s+you\\s+([^.!?]+)", Pattern.CASE_INSENSITIVE),
            // Hindi: "वेक वर्ड जार्विस रखो", "वेक वर्ड बदलकर नोवा कर दो", "तुम्हारा नाम जार्विस है"
            Pattern.compile("(?:वेक\\s*वर्ड|नाम)\\s+(?:बदलकर|को)?\\s*([^।!?]+?)\\s*(?:रखो|करो|कर\\s*दो|सेट\\s*करो|होगा)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("तुम्हारा\\s+नाम\\s+([^।!?]+?)\\s*(?:है|रखो|राख)", Pattern.CASE_INSENSITIVE),
            // Nepali: "वेक वर्ड साथी राख", "वेक वर्ड परिवर्तन गरी नोवा राख", "तिम्रो नाम जार्विस राख"
            Pattern.compile("(?:वेक\\s*वर्ड|नाम)\\s+(?:परिवर्तन\\s*गरी|लाई)?\\s*([^।!?]+?)\\s*(?:राख|राख्नुस|राख्नुहोस्|बनाउ|बनाउनुहोस्)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("तिम्रो\\s+नाम\\s+([^।!?]+?)\\s*(?:राख|हो|बनाउ)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val extracted = matcher.group(1)?.trim()
                if (!extracted.isNullOrBlank()) {
                    val cleaned = cleanWakeWord(extracted)
                        .replace(Regex("(?i)^(to|is|as|हो|है|राख)\\s+"), "")
                    if (cleaned.isNotBlank() && cleaned.length in 2..30) {
                        return cleaned
                    }
                }
            }
        }
        return null
    }

    /**
     * Inspects input for the current custom wake word (or default variations).
     */
    fun analyzeWakeWord(input: String): WakeWordResult {
        val currentWakeWord = getWakeWord()
        val text = input.trim()

        // Normalize text for flexible matching
        val normalizedInput = text.lowercase().replace(Regex("[.,!?:;\"']"), "")
        val normalizedWake = currentWakeWord.lowercase().replace(Regex("[.,!?:;\"']"), "")

        // Also accept common variations of current wake word (e.g. if wake word is "Hi Alex", also accept "Alex", "Hey Alex", "Hello Alex", "Wake up Alex", "Alex wake up")
        val wakeVariations = mutableListOf(normalizedWake)
        if (normalizedWake.startsWith("hi ")) {
            wakeVariations.add(normalizedWake.removePrefix("hi ").trim())
        }
        if (normalizedWake.startsWith("hey ")) {
            wakeVariations.add(normalizedWake.removePrefix("hey ").trim())
        }
        if (normalizedWake.startsWith("hello ")) {
            wakeVariations.add(normalizedWake.removePrefix("hello ").trim())
        }
        if (normalizedWake.startsWith("suno ")) {
            wakeVariations.add(normalizedWake.removePrefix("suno ").trim())
        }
        if (normalizedWake.startsWith("namaste ")) {
            wakeVariations.add(normalizedWake.removePrefix("namaste ").trim())
        }
        if (normalizedWake.startsWith("wake up ")) {
            wakeVariations.add(normalizedWake.removePrefix("wake up ").trim())
        }

        // Check if input begins with or contains any of the wake word variations
        for (wake in wakeVariations) {
            if (wake.isBlank()) continue

            // 1. Exact match (User said ONLY the wake word or wake up phrase)
            if (normalizedInput == wake || 
                normalizedInput == "hi $wake" || 
                normalizedInput == "hey $wake" || 
                normalizedInput == "hello $wake" || 
                normalizedInput == "suno $wake" || 
                normalizedInput == "namaste $wake" ||
                normalizedInput == "wake up $wake" ||
                normalizedInput == "$wake wake up" ||
                normalizedInput == "wake up"
            ) {
                return WakeWordResult(
                    isTriggered = true,
                    matchedWakeWord = currentWakeWord,
                    strippedCommand = "",
                    isOnlyWakeWord = true
                )
            }

            // 2. Input starts with wake word + command (e.g. "Hi Alex turn on flashlight", "Wake up Alex turn on torch")
            val regexPrefix = Pattern.compile("^\\s*(?:hi\\s+|hey\\s+|hello\\s+|suno\\s+|namaste\\s+|wake\\s*up\\s+)?\\Q$wake\\E(?:\\s+wake\\s*up)?\\s*[,:-]?\\s*(.*)$", Pattern.CASE_INSENSITIVE)
            val matcherPrefix = regexPrefix.matcher(text)
            if (matcherPrefix.find()) {
                val remainder = matcherPrefix.group(1)?.trim() ?: ""
                return WakeWordResult(
                    isTriggered = true,
                    matchedWakeWord = currentWakeWord,
                    strippedCommand = remainder,
                    isOnlyWakeWord = remainder.isBlank()
                )
            }

            // 3. Input contains wake word anywhere (e.g. "Can you help me Alex turn off torch")
            val regexContains = Pattern.compile("\\b(?:wake\\s*up\\s+)?\\Q$wake\\E(?:\\s+wake\\s*up)?\\b\\s*[,:-]?\\s*(.*)$", Pattern.CASE_INSENSITIVE)
            val matcherContains = regexContains.matcher(text)
            if (matcherContains.find()) {
                val remainder = matcherContains.group(1)?.trim() ?: ""
                val fullStripped = text.replace(Regex("(?i)\\b(?:hi\\s+|hey\\s+|hello\\s+|suno\\s+|namaste\\s+|wake\\s*up\\s+)?\\Q$wake\\E(?:\\s+wake\\s*up)?\\b[,:-]?"), "").trim()
                return WakeWordResult(
                    isTriggered = true,
                    matchedWakeWord = currentWakeWord,
                    strippedCommand = if (remainder.isNotBlank()) remainder else fullStripped,
                    isOnlyWakeWord = remainder.isBlank() && fullStripped.isBlank()
                )
            }
        }

        return WakeWordResult(
            isTriggered = false,
            matchedWakeWord = currentWakeWord,
            strippedCommand = text,
            isOnlyWakeWord = false
        )
    }

    /**
     * Greeting when user speaks only the wake word.
     */
    fun getWakeWordGreeting(lang: LanguageMode, wakeWord: String): String {
        return when (lang) {
            LanguageMode.NEPALI -> "हजुर $wakeWord यहाँ छु! भन्नुहोस्, म तपाईंलाई के सहयोग गरूँ?"
            LanguageMode.HINDI -> "हाँ जी, $wakeWord हाज़िर है! बताइए, मैं आपकी क्या मदद कर सकता हूँ?"
            else -> "Yes! $wakeWord is listening. How can I help you?"
        }
    }
}
