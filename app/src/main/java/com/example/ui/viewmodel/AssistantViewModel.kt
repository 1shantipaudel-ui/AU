package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.db.AppDatabase
import com.example.data.model.ActionType
import com.example.data.model.AssistantAction
import com.example.data.model.AssistantLog
import com.example.data.model.AssistantResponse
import com.example.data.model.LanguageMode
import com.example.data.model.VoiceNote
import com.example.domain.DeviceController
import com.example.domain.SpeechManager
import com.example.domain.VoiceAssistantEngine
import com.example.domain.WakeWordManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MessageUiModel(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val text: String,
    val detectedLanguage: String = "en",
    val action: AssistantAction? = null,
    val actionResultText: String? = null,
    val isActionSuccess: Boolean = true,
    val triggeredWakeWord: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val voiceNoteDao = database.voiceNoteDao()
    private val assistantLogDao = database.assistantLogDao()

    private val deviceController = DeviceController(application)
    private val voiceAssistantEngine = VoiceAssistantEngine()
    private val wakeWordManager = WakeWordManager(application)

    private var speechManager: SpeechManager? = null

    private val _messages = MutableStateFlow<List<MessageUiModel>>(emptyList())
    val messages: StateFlow<List<MessageUiModel>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(LanguageMode.AUTO)
    val selectedLanguage: StateFlow<LanguageMode> = _selectedLanguage.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _wakeWord = MutableStateFlow(wakeWordManager.getWakeWord())
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _isHandsFreeWakeEnabled = MutableStateFlow(wakeWordManager.isHandsFreeEnabled())
    val isHandsFreeWakeEnabled: StateFlow<Boolean> = _isHandsFreeWakeEnabled.asStateFlow()

    private val _showWakeWordDialog = MutableStateFlow(false)
    val showWakeWordDialog: StateFlow<Boolean> = _showWakeWordDialog.asStateFlow()

    private val _showNotesSheet = MutableStateFlow(false)
    val showNotesSheet: StateFlow<Boolean> = _showNotesSheet.asStateFlow()

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification: StateFlow<String?> = _statusNotification.asStateFlow()

    val savedNotes: StateFlow<List<VoiceNote>> = voiceNoteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGeminiCloudEnabled: Boolean = GeminiApiClient.isApiKeyConfigured()

    init {
        initSpeechManager()
        addWelcomeMessage()
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(
            context = getApplication(),
            onSpeechResult = { text ->
                processUserPrompt(text)
            },
            onError = { error ->
                _statusNotification.value = error
            },
            onTtsDone = {
                if (_isHandsFreeWakeEnabled.value) {
                    viewModelScope.launch {
                        delay(350)
                        if (!_isListening.value && !_isSpeaking.value && !_isProcessing.value) {
                            speechManager?.startListening(_selectedLanguage.value)
                        }
                    }
                }
            },
            onSpeechIdle = {
                if (_isHandsFreeWakeEnabled.value && !_isSpeaking.value && !_isProcessing.value) {
                    viewModelScope.launch {
                        delay(400)
                        if (_isHandsFreeWakeEnabled.value && !_isListening.value && !_isSpeaking.value && !_isProcessing.value) {
                            speechManager?.startListening(_selectedLanguage.value)
                        }
                    }
                }
            }
        )

        viewModelScope.launch {
            speechManager?.isListening?.collect { _isListening.value = it }
        }
        viewModelScope.launch {
            speechManager?.isSpeaking?.collect { _isSpeaking.value = it }
        }
        viewModelScope.launch {
            speechManager?.audioRms?.collect { _audioRms.value = it }
        }
    }

    fun startHandsFreeListeningIfEnabled() {
        if (_isHandsFreeWakeEnabled.value && !_isListening.value && !_isSpeaking.value && !_isProcessing.value) {
            speechManager?.startListening(_selectedLanguage.value)
        }
    }

    private fun addWelcomeMessage() {
        val currentWord = wakeWordManager.getWakeWord()
        val welcome = MessageUiModel(
            isUser = false,
            text = "Namaste & Hello! I'm ALEX, your hands-free AI voice assistant. Say \"$currentWord\" or tap the orb to talk to me. I speak English, Hindi, and Nepali!",
            detectedLanguage = "en"
        )
        _messages.value = listOf(welcome)
    }

    fun setLanguageMode(mode: LanguageMode) {
        _selectedLanguage.value = mode
    }

    fun setCustomWakeWord(newWakeWord: String) {
        val saved = wakeWordManager.setWakeWord(newWakeWord)
        _wakeWord.value = saved
        _statusNotification.value = "Custom wake word updated to \"$saved\""
    }

    fun setHandsFreeMode(enabled: Boolean) {
        wakeWordManager.setHandsFreeEnabled(enabled)
        _isHandsFreeWakeEnabled.value = enabled
        _statusNotification.value = if (enabled) "Hands-free auto-listening enabled" else "Hands-free auto-listening disabled"
    }

    fun openWakeWordDialog() {
        _showWakeWordDialog.value = true
    }

    fun closeWakeWordDialog() {
        _showWakeWordDialog.value = false
    }

    fun testWakeWord() {
        val currentWord = _wakeWord.value
        val testResponse = wakeWordManager.getWakeWordGreeting(_selectedLanguage.value, currentWord)
        speechManager?.speak(testResponse, _selectedLanguage.value.localeCode)
        _statusNotification.value = "Testing \"$currentWord\" response"
    }

    fun toggleListening() {
        if (_isListening.value) {
            speechManager?.stopListening()
        } else {
            speechManager?.startListening(_selectedLanguage.value)
        }
    }

    fun stopSpeaking() {
        speechManager?.stopSpeaking()
    }

    fun replayAudio(text: String, lang: String) {
        speechManager?.speak(text, lang)
    }

    fun processUserPrompt(query: String) {
        if (query.isBlank()) return

        // 1. Check if user is asking to change/set wake word via voice command
        val wakeChangeRequest = wakeWordManager.checkWakeWordChangeRequest(query)
        if (wakeChangeRequest != null) {
            val savedWakeWord = wakeWordManager.setWakeWord(wakeChangeRequest)
            _wakeWord.value = savedWakeWord

            val userMessage = MessageUiModel(
                isUser = true,
                text = query,
                detectedLanguage = _selectedLanguage.value.localeCode
            )
            _messages.value = _messages.value + userMessage

            val replyText = when (_selectedLanguage.value) {
                LanguageMode.NEPALI -> "वेक वर्ड परिवर्तन गरी '$savedWakeWord' राखियो! अब मलाई $savedWakeWord भनेर बोलाउन सक्नुहुन्छ।"
                LanguageMode.HINDI -> "वेक वर्ड बदलकर '$savedWakeWord' कर दिया गया है! अब आप मुझे $savedWakeWord कहकर आवाज़ लगा सकते हैं।"
                else -> "Custom wake word updated to '$savedWakeWord'! You can now activate me by saying $savedWakeWord anytime."
            }

            val assistantMessage = MessageUiModel(
                isUser = false,
                text = replyText,
                detectedLanguage = _selectedLanguage.value.localeCode,
                triggeredWakeWord = savedWakeWord
            )
            _messages.value = _messages.value + assistantMessage
            speechManager?.speak(replyText, _selectedLanguage.value.localeCode)

            viewModelScope.launch {
                assistantLogDao.insertLog(
                    AssistantLog(
                        userPrompt = query,
                        assistantReply = replyText,
                        detectedLanguage = _selectedLanguage.value.localeCode,
                        actionTypeName = "SET_WAKE_WORD",
                        actionPayload = savedWakeWord,
                        isSuccess = true
                    )
                )
            }
            return
        }

        // 2. Check for Wake Word presence
        val wakeResult = wakeWordManager.analyzeWakeWord(query)
        val triggeredWakeWord = if (wakeResult.isTriggered) wakeResult.matchedWakeWord else null

        // 3. If user said ONLY the wake word (e.g. "Jarvis" or "Hey Assistant")
        if (wakeResult.isTriggered && wakeResult.isOnlyWakeWord) {
            val userMessage = MessageUiModel(
                isUser = true,
                text = query,
                detectedLanguage = _selectedLanguage.value.localeCode,
                triggeredWakeWord = triggeredWakeWord
            )
            _messages.value = _messages.value + userMessage

            val greeting = wakeWordManager.getWakeWordGreeting(_selectedLanguage.value, wakeResult.matchedWakeWord)
            val assistantMessage = MessageUiModel(
                isUser = false,
                text = greeting,
                detectedLanguage = _selectedLanguage.value.localeCode,
                triggeredWakeWord = triggeredWakeWord
            )
            _messages.value = _messages.value + assistantMessage
            speechManager?.speak(greeting, _selectedLanguage.value.localeCode)

            // Auto-listen for the follow-up command
            viewModelScope.launch {
                delay(1200)
                speechManager?.startListening(_selectedLanguage.value)
            }
            return
        }

        // 4. Clean command if wake word prefix was used
        val effectiveCommand = if (wakeResult.isTriggered && wakeResult.strippedCommand.isNotBlank()) {
            wakeResult.strippedCommand
        } else {
            query
        }

        val userMessage = MessageUiModel(
            isUser = true,
            text = query,
            detectedLanguage = _selectedLanguage.value.localeCode,
            triggeredWakeWord = triggeredWakeWord
        )
        _messages.value = _messages.value + userMessage

        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val response: AssistantResponse = voiceAssistantEngine.processQuery(effectiveCommand, _selectedLanguage.value)

                var actionResultText: String? = null
                var isActionSuccess = true

                // Execute action on device if present
                if (response.action != null && response.action.actionType != ActionType.CHAT_REPLY) {
                    if (response.action.actionType == ActionType.SAVE_NOTE) {
                        val title = response.action.target ?: "Voice Note"
                        val content = response.action.value ?: effectiveCommand
                        voiceNoteDao.insertNote(VoiceNote(title = title, content = content))
                        actionResultText = "Saved in Quick Notes: \"$content\""
                    } else {
                        val result = deviceController.executeAction(response.action)
                        actionResultText = result.message
                        isActionSuccess = result.isSuccess
                        if (response.action.actionType == ActionType.TOGGLE_FLASHLIGHT) {
                            _isFlashlightOn.value = deviceController.isFlashlightOn()
                        }
                    }
                }

                val assistantMessage = MessageUiModel(
                    isUser = false,
                    text = response.spokenText,
                    detectedLanguage = response.detectedLanguage,
                    action = response.action,
                    actionResultText = actionResultText,
                    isActionSuccess = isActionSuccess,
                    triggeredWakeWord = triggeredWakeWord
                )
                _messages.value = _messages.value + assistantMessage

                // Speak response aloud via TTS
                speechManager?.speak(response.spokenText, response.detectedLanguage)

                // Log to database
                assistantLogDao.insertLog(
                    AssistantLog(
                        userPrompt = query,
                        assistantReply = response.spokenText,
                        detectedLanguage = response.detectedLanguage,
                        actionTypeName = response.action?.actionType?.name ?: ActionType.CHAT_REPLY.name,
                        actionPayload = response.action?.target ?: response.action?.value,
                        isSuccess = isActionSuccess
                    )
                )
            } catch (e: Exception) {
                val fallbackText = "Sorry, I ran into an issue: ${e.message}"
                _messages.value = _messages.value + MessageUiModel(
                    isUser = false,
                    text = fallbackText,
                    detectedLanguage = "en",
                    isActionSuccess = false
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleFlashlightDirect() {
        val newState = !_isFlashlightOn.value
        val result = deviceController.executeAction(
            AssistantAction(
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                value = if (newState) "on" else "off"
            )
        )
        _isFlashlightOn.value = deviceController.isFlashlightOn()
        _statusNotification.value = result.message
    }

    fun executeQuickAction(action: AssistantAction) {
        val result = deviceController.executeAction(action)
        _statusNotification.value = result.message
        if (action.actionType == ActionType.TOGGLE_FLASHLIGHT) {
            _isFlashlightOn.value = deviceController.isFlashlightOn()
        }
    }

    fun deleteVoiceNote(note: VoiceNote) {
        viewModelScope.launch {
            voiceNoteDao.deleteNote(note)
            _statusNotification.value = "Note deleted"
        }
    }

    fun saveManualNote(title: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            voiceNoteDao.insertNote(VoiceNote(title = title.ifBlank { "Quick Note" }, content = content))
            _statusNotification.value = "Note saved!"
        }
    }

    fun openNotesSheet() {
        _showNotesSheet.value = true
    }

    fun closeNotesSheet() {
        _showNotesSheet.value = false
    }

    fun openHelpDialog() {
        _showHelpDialog.value = true
    }

    fun closeHelpDialog() {
        _showHelpDialog.value = false
    }

    fun clearNotification() {
        _statusNotification.value = null
    }

    fun clearConversation() {
        _messages.value = emptyList()
        addWelcomeMessage()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.release()
    }
}

