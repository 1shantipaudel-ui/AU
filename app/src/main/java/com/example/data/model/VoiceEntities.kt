package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class LanguageMode(val displayName: String, val flag: String, val localeCode: String) {
    AUTO("Auto-Detect", "🌐", "auto"),
    HINDI("हिन्दी", "🇮🇳", "hi"),
    NEPALI("नेपाली", "🇳🇵", "ne"),
    ENGLISH("English", "🇬🇧", "en")
}

enum class ActionType {
    TOGGLE_FLASHLIGHT,
    OPEN_APP,
    PLAY_YOUTUBE,
    PLAY_SPOTIFY,
    WEB_SEARCH,
    PHONE_CALL,
    SEND_SMS,
    SEND_WHATSAPP,
    OPEN_WIFI,
    OPEN_BLUETOOTH,
    OPEN_AIRPLANE_MODE,
    OPEN_DND,
    ADJUST_VOLUME,
    OPEN_BRIGHTNESS,
    SET_ALARM,
    SET_TIMER,
    CREATE_CALENDAR_EVENT,
    SAVE_NOTE,
    CHAT_REPLY,
    UNKNOWN
}

@JsonClass(generateAdapter = true)
data class AssistantAction(
    val actionType: ActionType,
    val target: String? = null,
    val value: String? = null,
    val extra: String? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val seconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class AssistantResponse(
    val spokenText: String,
    val detectedLanguage: String,
    val action: AssistantAction? = null
)

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assistant_logs")
data class AssistantLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userPrompt: String,
    val assistantReply: String,
    val detectedLanguage: String,
    val actionTypeName: String,
    val actionPayload: String?,
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
