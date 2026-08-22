package com.example.domain

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onTtsDone: (() -> Unit)? = null,
    private val onSpeechIdle: (() -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private var speechRate = 1.01f
    private var speechPitch = 1.03f

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    textToSpeech?.setAudioAttributes(audioAttributes)
                } catch (e: Exception) {
                    Log.w("SpeechManager", "AudioAttributes setup error: ${e.message}")
                }

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onTtsDone?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.w("SpeechManager", "TTS initialization failed: $status")
            }
        }
    }

    /**
     * Finds and applies the most natural, human-like voice available for the target locale.
     */
    private fun applyBestHumanVoice(locale: Locale) {
        if (textToSpeech == null) return
        try {
            val availableVoices = textToSpeech?.voices ?: return
            val matchingVoices = availableVoices.filter { voice ->
                voice.locale.language.equals(locale.language, ignoreCase = true)
            }

            if (matchingVoices.isNotEmpty()) {
                val bestVoice = matchingVoices.maxByOrNull { voice ->
                    var score = 0
                    if (voice.quality == Voice.QUALITY_VERY_HIGH) score += 35
                    else if (voice.quality == Voice.QUALITY_HIGH) score += 25
                    else if (voice.quality == Voice.QUALITY_NORMAL) score += 10

                    if (voice.latency == Voice.LATENCY_VERY_LOW || voice.latency == Voice.LATENCY_LOW) score += 20
                    else if (voice.latency == Voice.LATENCY_NORMAL) score += 10

                    val name = voice.name.lowercase()
                    if (name.contains("natural") || name.contains("neural") || name.contains("wavenet")) score += 30
                    if (name.contains("female") || name.contains("sfg") || name.contains("hie") || name.contains("iom")) score += 15

                    if (voice.isNetworkConnectionRequired) score -= 5
                    score
                }

                if (bestVoice != null) {
                    textToSpeech?.voice = bestVoice
                    Log.d("SpeechManager", "Selected high-quality human voice: ${bestVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w("SpeechManager", "Error selecting voice: ${e.message}")
        }
    }

    /**
     * Strips markdown, emojis, asterisks, and code artifacts so TTS pronounces cleanly like a human speaker.
     */
    fun sanitizeTextForSpeech(input: String): String {
        return input
            // Remove markdown bold / italics / strikethrough
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            .replace(Regex("~~(.*?)~~"), "$1")
            // Remove code blocks and inline code
            .replace(Regex("```[a-zA-Z]*\\s*([\\s\\S]*?)```"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            // Remove bullet points / hashtags
            .replace(Regex("(?m)^[\\s*#>-]+\\s*"), "")
            // Remove URLs
            .replace(Regex("https?://\\S+"), "link")
            // Remove emoji symbols for natural reading
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Replace multiple spaces/newlines with single space
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun startListening(languageMode: LanguageMode = LanguageMode.AUTO) {
        stopSpeaking()
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _audioRms.value = 0.2f
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB to +10dB to 0.0 .. 1.0 range
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioRms.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioRms.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRms.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue for voice recognition"
                            else -> "Voice recognition stopped ($error)"
                        }
                        Log.d("SpeechManager", "Speech recognition error: $msg")
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onSpeechIdle?.invoke()
                        } else {
                            onError(msg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRms.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            onSpeechResult(text)
                        } else {
                            onSpeechIdle?.invoke()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            _audioRms.value = 0.5f
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                when (languageMode) {
                    LanguageMode.HINDI -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN"))
                    }
                    LanguageMode.NEPALI -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ne-NP")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ne-NP", "hi-IN", "en-US"))
                    }
                    LanguageMode.ENGLISH -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    }
                    LanguageMode.AUTO -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "ne-NP", "en-US"))
                    }
                }
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                onError("Failed to start voice recognition: ${e.message}")
            }
        } else {
            onError("Speech recognition not available on this device")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Stop listening error: ${e.message}")
        }
        _isListening.value = false
        _audioRms.value = 0f
    }

    fun speak(text: String, langCode: String = "auto") {
        if (!isTtsReady || textToSpeech == null) {
            Log.w("SpeechManager", "TTS is not ready yet")
            return
        }

        val cleanSpeech = sanitizeTextForSpeech(text)
        if (cleanSpeech.isBlank()) return

        val locale = when (langCode.lowercase()) {
            "hi", "hin" -> Locale("hi", "IN")
            "ne", "nep" -> Locale("ne", "NP")
            else -> Locale.US
        }

        try {
            val res = textToSpeech?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to Hindi for Nepali if Nepali TTS pack isn't installed on device, or English
                if (langCode == "ne") {
                    textToSpeech?.setLanguage(Locale("hi", "IN"))
                } else {
                    textToSpeech?.setLanguage(Locale.US)
                }
            }

            // Apply best natural human voice available on this device
            applyBestHumanVoice(locale)

            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)

            val utteranceId = "utterance_${System.currentTimeMillis()}"
            textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error speaking text: ${e.message}")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            textToSpeech?.stop()
        }
        _isSpeaking.value = false
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechManager", "Destroy recognizer error: ${e.message}")
        }
    }

    fun release() {
        destroyRecognizer()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e("SpeechManager", "Release TTS error: ${e.message}")
        }
    }
}
