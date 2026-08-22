package com.example.domain

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.model.ActionType
import com.example.data.model.AssistantAction
import com.example.data.model.AssistantResponse
import com.example.data.model.LanguageMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.regex.Pattern

class VoiceAssistantEngine {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(AssistantResponse::class.java)

    suspend fun processQuery(userInput: String, preferredLanguage: LanguageMode): AssistantResponse = withContext(Dispatchers.IO) {
        val detectedLang = LanguageDetector.detectLanguage(userInput, preferredLanguage)

        if (GeminiApiClient.isApiKeyConfigured()) {
            try {
                val cloudResponse = callGeminiApi(userInput, detectedLang)
                if (cloudResponse != null) {
                    return@withContext cloudResponse
                }
            } catch (e: Exception) {
                Log.w("VoiceAssistantEngine", "Cloud Gemini call failed, falling back to local engine: ${e.message}")
            }
        }

        // Fast, empathetic local engine fallback
        processLocally(userInput, detectedLang)
    }

    private suspend fun callGeminiApi(userInput: String, lang: LanguageMode): AssistantResponse? {
        val systemPrompt = """
            You are Alex, a super smart, direct, and human-like AI voice assistant on Android (activated with "Hi Alex").
            Your core principles:
            1. GIVE DIRECT ANSWERS: Answer questions directly, concisely, and factually in 1-2 clear, informative sentences. Strictly avoid filler, fluff, and robotic disclaimers.
            2. DO COMMANDS IMMEDIATELY: When user asks to control the phone (flashlight, alarm, timer, apps, youtube, spotify, notes, search, volume, settings, calls, messages), execute the action immediately and confirm with a direct, crisp 1-sentence confirmation.
            3. NATURAL HUMAN SPEECH: Sound like an articulate, warm human peer. Never use markdown formatting (no asterisks, bold, bullet points, headers, or raw URLs) in 'spokenText'.
            4. Language:
            - If user speaks Hindi (or Hinglish), respond in natural conversational Hindi.
            - If user speaks Nepali (or Romanized Nepali), respond in natural authentic Nepali.
            - If user speaks English, respond in fluent, crisp English.

            Output MUST be strictly valid JSON matching this schema:
            {
              "spokenText": "Direct, natural 1-2 sentence spoken reply without markdown or filler",
              "detectedLanguage": "${lang.localeCode}",
              "action": {
                "actionType": "TOGGLE_FLASHLIGHT" | "OPEN_APP" | "PLAY_YOUTUBE" | "PLAY_SPOTIFY" | "WEB_SEARCH" | "PHONE_CALL" | "SEND_SMS" | "SEND_WHATSAPP" | "OPEN_WIFI" | "OPEN_BLUETOOTH" | "OPEN_AIRPLANE_MODE" | "OPEN_DND" | "ADJUST_VOLUME" | "OPEN_BRIGHTNESS" | "SET_ALARM" | "SET_TIMER" | "CREATE_CALENDAR_EVENT" | "SAVE_NOTE" | "CHAT_REPLY",
                "target": "target identifier or app or query or phone",
                "value": "action value like 'on'/'off' or message",
                "extra": "extra message details",
                "hour": 7,
                "minute": 30,
                "seconds": 120
              }
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userInput))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f,
                responseMimeType = "application/json"
            )
        )

        val response = GeminiApiClient.apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (!text.isNullOrBlank()) {
            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            return responseAdapter.fromJson(cleanJson)
        }
        return null
    }

    fun processLocally(input: String, lang: LanguageMode): AssistantResponse {
        val lower = input.lowercase().trim()
        val langCode = when (lang) {
            LanguageMode.HINDI -> "hi"
            LanguageMode.NEPALI -> "ne"
            else -> "en"
        }

        // 1. Flashlight / Torch
        if (matchesAny(lower, "flashlight", "torch", "फ्लैशलाइट", "टॉर्च", "बत्ती", "फ्ल्यासलाइट")) {
            val turnOff = matchesAny(lower, "off", "band", "bujhao", "निभाउ", "बन्द", "निभाइदिनु", "बुझाओ")
            val isOff = turnOff
            val spoken = when (lang) {
                LanguageMode.NEPALI -> if (isOff) "फ्ल्यासलाइट बन्द गरियो।" else "फ्ल्यासलाइट बालिएको छ।"
                LanguageMode.HINDI -> if (isOff) "फ्लैशलाइट बंद कर दी गई है।" else "फ्लैशलाइट चालू कर दी गई है।"
                else -> if (isOff) "Flashlight turned off." else "Flashlight turned on."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.TOGGLE_FLASHLIGHT,
                    value = if (isOff) "off" else "on"
                )
            )
        }

        // 2. Alarm
        if (matchesAny(lower, "alarm", "wake me up", "अलार्म", "उठाउनु", "जगाओ")) {
            val time = extractTime(lower)
            val hour = time.first
            val minute = time.second
            val timeStr = String.format("%02d:%02d", hour, minute)
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "$timeStr को लागि अलार्म सेट भयो।"
                LanguageMode.HINDI -> "$timeStr बजे का अलार्म सेट हो गया है।"
                else -> "Alarm set for $timeStr."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.SET_ALARM,
                    hour = hour,
                    minute = minute,
                    extra = "Voice Alarm"
                )
            )
        }

        // 3. Timer
        if (matchesAny(lower, "timer", "टाइमर", "गन्ती", "काउन्टडाउन")) {
            val seconds = extractMinutes(lower) * 60
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "${seconds / 60} मिनेटको टाइमर सुरु गरियो।"
                LanguageMode.HINDI -> "${seconds / 60} मिनट का टाइमर शुरू कर दिया गया है।"
                else -> "Starting ${seconds / 60} minute timer."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.SET_TIMER,
                    seconds = if (seconds > 0) seconds else 300,
                    value = "Timer"
                )
            )
        }

        // 4. YouTube
        if (matchesAny(lower, "youtube", "play on youtube", "यूट्यूब", "युट्युब", "गीत", "भिडियो")) {
            val query = extractSongQuery(lower, "youtube", "यूट्यूब", "युट्युब")
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "युट्युबमा '$query' बजाइँदै छ।"
                LanguageMode.HINDI -> "यूट्यूब पर '$query' चलाया जा रहा है।"
                else -> "Playing '$query' on YouTube."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.PLAY_YOUTUBE,
                    target = query
                )
            )
        }

        // 5. Spotify
        if (matchesAny(lower, "spotify", "स्पोटिफाइ", "स्पॉटिफ़ाई")) {
            val query = extractSongQuery(lower, "spotify", "स्पोटिफाइ", "स्पॉटिफ़ाई")
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "स्पोटिफाइमा '$query' बजाइँदै छ।"
                LanguageMode.HINDI -> "स्पॉटिफ़ाई पर '$query' चलाया जा रहा है।"
                else -> "Playing '$query' on Spotify."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.PLAY_SPOTIFY,
                    target = query
                )
            )
        }

        // 6. WhatsApp
        if (matchesAny(lower, "whatsapp", "व्हाट्सएप", "ह्वाट्सएप", "मेसेज")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "ह्वाट्सएप मेसेज तयार छ।"
                LanguageMode.HINDI -> "व्हाट्सएप संदेश खोला जा रहा है।"
                else -> "Opening WhatsApp message."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.SEND_WHATSAPP,
                    value = input
                )
            )
        }

        // 7. Phone Call
        if (matchesAny(lower, "call", "dial", "फोन गर", "कल करो", "फोन लगाओ")) {
            val phone = extractPhoneNumber(lower)
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "फोन डायलर खोलिँदै छ।"
                LanguageMode.HINDI -> "फोन डायलर खोला जा रहा है।"
                else -> "Opening phone dialer."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.PHONE_CALL,
                    target = phone
                )
            )
        }

        // 8. SMS
        if (matchesAny(lower, "sms", "text", "एसएमएस", "सन्देश", "संदेश")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "एसएमएस मेसेन्जर खोलिँदै छ।"
                LanguageMode.HINDI -> "एसएमएस मैसेंजर खोला जा रहा है।"
                else -> "Opening SMS messenger."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.SEND_SMS,
                    value = input
                )
            )
        }

        // 9. Quick Note
        if (matchesAny(lower, "note", "save note", "टिप्पणी", "नोट", "याद राख", "लिखो")) {
            val noteContent = input.replace(Regex("(?i)(note|save note|take note|नोट बनाओ|टिप्पणी लेख|याद राख)"), "").trim()
            val finalNote = if (noteContent.isNotBlank()) noteContent else "Voice Note: $input"
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "नोट सुरक्षित गरियो।"
                LanguageMode.HINDI -> "नोट सेव कर लिया गया है।"
                else -> "Note saved."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.SAVE_NOTE,
                    target = "Voice Note",
                    value = finalNote
                )
            )
        }

        // 10. Volume
        if (matchesAny(lower, "volume", "sound", "आवाज", "आवाज़", "ध्वनि")) {
            val direction = if (matchesAny(lower, "down", "kam", "ghatao", "कम", "घटाउ", "धीमा")) "down"
            else if (matchesAny(lower, "mute", "silent", "चुप", "मौन")) "mute"
            else "up"
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "ध्वनि मिलाइयो।"
                LanguageMode.HINDI -> "आवाज़ सेट हो गई है।"
                else -> "Volume adjusted."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.ADJUST_VOLUME,
                    value = direction
                )
            )
        }

        // 11. Wi-Fi / Bluetooth / Settings / Brightness
        if (matchesAny(lower, "wifi", "wi-fi", "वाइफाइ", "वाई-फाई")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "वाइफाइ सेटिङ खोलिँदै छ।"
                LanguageMode.HINDI -> "वाई-फ़ाई सेटिंग्स खोली जा रही हैं।"
                else -> "Opening Wi-Fi settings."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(actionType = ActionType.OPEN_WIFI)
            )
        }

        if (matchesAny(lower, "bluetooth", "ब्लुटुथ", "ब्लूटूथ")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "ब्लुटुथ सेटिङ खोलिँदै छ।"
                LanguageMode.HINDI -> "ब्लूटूथ सेटिंग्स खोली जा रही हैं।"
                else -> "Opening Bluetooth settings."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(actionType = ActionType.OPEN_BLUETOOTH)
            )
        }

        if (matchesAny(lower, "brightness", "उज्यालो", "चमक", "डिस्प्ले", "स्क्रीन")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "डिस्प्ले ब्राइटनेस सेटिङ खोलिँदै छ।"
                LanguageMode.HINDI -> "ब्राइटनेस सेटिंग्स खोली जा रही हैं।"
                else -> "Opening display brightness settings."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(actionType = ActionType.OPEN_BRIGHTNESS)
            )
        }

        // 12. App Launching (Camera, Chrome, Calculator, Maps, Settings)
        if (matchesAny(lower, "open", "launch", "खोलो", "खोल", "खोल्नुस", "खोल्नुहोस्", "चलाओ")) {
            val app = extractAppName(lower)
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "$app खोलिँदै छ।"
                LanguageMode.HINDI -> "$app खोला जा रहा है।"
                else -> "Opening $app."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(
                    actionType = ActionType.OPEN_APP,
                    target = app
                )
            )
        }

        // 13. Conversational / Empathetic Greetings & General Chat
        if (matchesAny(lower, "hi alex", "hey alex", "hello alex", "alex", "hello", "hi", "hey", "नमस्ते", "नमस्कार", "के छ", "कस्तो छ", "कैसे हो", "क्या हाल")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "नमस्ते! म एलेक्स यहाँ छु। आज के सहयोग गरूँ?"
                LanguageMode.HINDI -> "नमस्ते! मैं एलेक्स हूँ। बताइए क्या काम करना है?"
                else -> "Hi Alex here. How can I help you today?"
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(actionType = ActionType.CHAT_REPLY)
            )
        }

        if (matchesAny(lower, "who are you", "who r u", "what is your name", "तिम्रो नाम के हो", "तिमी को हौ", "तपाईं को हो", "तुम कौन हो", "आपका नाम क्या है", "आप कौन हैं")) {
            val spoken = when (lang) {
                LanguageMode.NEPALI -> "म एलेक्स, तपाईंको भ्वाइस असिस्टेन्ट हुँ। म फोनका सबै कामहरू तुरुन्त गर्न सक्छु।"
                LanguageMode.HINDI -> "मैं एलेक्स हूँ, आपका हैंड्स-फ्री वॉइस असिस्टेंट। मैं फोन कंट्रोल्स और आपके सभी काम कर सकता हूँ।"
                else -> "I'm Alex, your hands-free voice assistant. I can control your phone, answer questions, and execute commands."
            }
            return AssistantResponse(
                spokenText = spoken,
                detectedLanguage = langCode,
                action = AssistantAction(actionType = ActionType.CHAT_REPLY)
            )
        }

        // 14. Default fallback: Web Search / Direct Answer
        val spoken = when (lang) {
            LanguageMode.NEPALI -> "गुगलमा '$input' खोजिँदै छ।"
            LanguageMode.HINDI -> "गूगल पर '$input' सर्च किया जा रहा है।"
            else -> "Searching Google for '$input'."
        }
        return AssistantResponse(
            spokenText = spoken,
            detectedLanguage = langCode,
            action = AssistantAction(
                actionType = ActionType.WEB_SEARCH,
                target = input
            )
        )
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractTime(text: String): Pair<Int, Int> {
        val pattern = Pattern.compile("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm|बजे|बजेको)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 7
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, minute)
        }
        // Default 7:00 AM
        return Pair(7, 0)
    }

    private fun extractMinutes(text: String): Int {
        val pattern = Pattern.compile("(\\d+)\\s*(min|minute|मिनेट|मिनट|sec|second|सेकेन्ड|सेकंड)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val num = matcher.group(1)?.toIntOrNull() ?: 5
            val unit = matcher.group(2)?.lowercase() ?: "min"
            return if (unit.startsWith("s")) (num / 60).coerceAtLeast(1) else num
        }
        return 5
    }

    private fun extractSongQuery(text: String, vararg prefixes: String): String {
        var query = text
        for (p in prefixes) {
            query = query.replace(p, "")
        }
        query = query.replace(Regex("(?i)(play|song|गाना|गीत|बजाओ|बजाउ|चलाओ|sunao|music)"), "").trim()
        return if (query.isNotBlank()) query else "Top trending songs"
    }

    private fun extractPhoneNumber(text: String): String {
        val digits = text.filter { it.isDigit() || it == '+' }
        return if (digits.length >= 3) digits else ""
    }

    private fun extractAppName(text: String): String {
        val clean = text.replace(Regex("(?i)(open|launch|खोलो|खोल|खोल्नुस|खोल्नुहोस्|app|application)"), "").trim()
        return if (clean.isNotBlank()) clean else "Camera"
    }
}
