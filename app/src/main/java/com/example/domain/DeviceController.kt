package com.example.domain

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import com.example.data.model.ActionType
import com.example.data.model.AssistantAction
import java.net.URLEncoder

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var isTorchOn = false

    fun isFlashlightOn(): Boolean = isTorchOn

    fun executeAction(action: AssistantAction): ExecutionResult {
        return try {
            when (action.actionType) {
                ActionType.TOGGLE_FLASHLIGHT -> {
                    val turnOn = action.value?.equals("on", ignoreCase = true)
                        ?: action.value?.equals("true", ignoreCase = true)
                        ?: !isTorchOn
                    val success = setFlashlight(turnOn)
                    if (success) {
                        ExecutionResult(
                            isSuccess = true,
                            message = if (turnOn) "Flashlight turned ON" else "Flashlight turned OFF"
                        )
                    } else {
                        ExecutionResult(isSuccess = false, message = "Could not access device flashlight")
                    }
                }

                ActionType.OPEN_APP -> {
                    val appName = action.target ?: action.value ?: "Settings"
                    openApplication(appName)
                }

                ActionType.PLAY_YOUTUBE -> {
                    val query = action.target ?: action.value ?: "music"
                    playYouTube(query)
                }

                ActionType.PLAY_SPOTIFY -> {
                    val query = action.target ?: action.value ?: "top hits"
                    playSpotify(query)
                }

                ActionType.WEB_SEARCH -> {
                    val query = action.target ?: action.value ?: ""
                    runWebSearch(query)
                }

                ActionType.PHONE_CALL -> {
                    val phone = action.target ?: action.value ?: ""
                    makePhoneCall(phone)
                }

                ActionType.SEND_SMS -> {
                    val phone = action.target ?: ""
                    val message = action.value ?: action.extra ?: ""
                    sendSms(phone, message)
                }

                ActionType.SEND_WHATSAPP -> {
                    val phone = action.target ?: ""
                    val message = action.value ?: action.extra ?: ""
                    sendWhatsApp(phone, message)
                }

                ActionType.OPEN_WIFI -> {
                    openSettingsIntent(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
                }

                ActionType.OPEN_BLUETOOTH -> {
                    openSettingsIntent(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
                }

                ActionType.OPEN_AIRPLANE_MODE -> {
                    openSettingsIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Airplane Mode Settings")
                }

                ActionType.OPEN_DND -> {
                    openSettingsIntent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, "Do Not Disturb Settings")
                }

                ActionType.ADJUST_VOLUME -> {
                    val direction = action.value?.lowercase() ?: "up"
                    adjustVolume(direction)
                }

                ActionType.OPEN_BRIGHTNESS -> {
                    openSettingsIntent(Settings.ACTION_DISPLAY_SETTINGS, "Display & Brightness Settings")
                }

                ActionType.SET_ALARM -> {
                    val hour = action.hour ?: 7
                    val minute = action.minute ?: 0
                    val msg = action.extra ?: action.value ?: "Voice Assistant Alarm"
                    setAlarm(hour, minute, msg)
                }

                ActionType.SET_TIMER -> {
                    val seconds = action.seconds ?: ((action.minute ?: 1) * 60)
                    val msg = action.extra ?: action.value ?: "Timer"
                    setTimer(seconds, msg)
                }

                ActionType.CREATE_CALENDAR_EVENT -> {
                    val title = action.target ?: action.value ?: "New Event"
                    val desc = action.extra ?: "Created via Voice Assistant"
                    createCalendarEvent(title, desc)
                }

                ActionType.SAVE_NOTE, ActionType.CHAT_REPLY, ActionType.UNKNOWN -> {
                    ExecutionResult(isSuccess = true, message = "Processed successfully")
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceController", "Error executing action: ${e.message}", e)
            ExecutionResult(isSuccess = false, message = "Execution error: ${e.localizedMessage}")
        }
    }

    private fun setFlashlight(enabled: Boolean): Boolean {
        if (cameraManager == null) return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: "0"

            cameraManager.setTorchMode(cameraId, enabled)
            isTorchOn = enabled
            true
        } catch (e: Exception) {
            Log.e("DeviceController", "Torch error: ${e.message}")
            false
        }
    }

    private fun openApplication(appName: String): ExecutionResult {
        val lower = appName.lowercase().trim()
        val pkg = when {
            lower.contains("youtube") -> "com.google.android.youtube"
            lower.contains("spotify") -> "com.spotify.music"
            lower.contains("whatsapp") -> "com.whatsapp"
            lower.contains("camera") || lower.contains("कैमरा") || lower.contains("क्यामेरा") -> "com.google.android.GoogleCamera"
            lower.contains("map") || lower.contains("नक्शा") -> "com.google.android.apps.maps"
            lower.contains("chrome") || lower.contains("browser") || lower.contains("गुगल") -> "com.android.chrome"
            lower.contains("clock") || lower.contains("alarm") || lower.contains("घडी") || lower.contains("घड़ी") -> "com.google.android.deskclock"
            lower.contains("calendar") || lower.contains("पात्रो") -> "com.google.android.calendar"
            lower.contains("calc") || lower.contains("हिसाब") -> "com.google.android.calculator"
            lower.contains("setting") || lower.contains("सेटिङ") || lower.contains("सेटिंग") -> "com.android.settings"
            else -> null
        }

        if (pkg != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ExecutionResult(true, "Opening $appName")
            }
        }

        // Fallback search or store view
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Searching for $appName in Store")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, "open app $appName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "Searched for $appName")
        }
    }

    private fun playYouTube(query: String): ExecutionResult {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ExecutionResult(true, "Playing '$query' on YouTube")
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to launch YouTube: ${e.message}")
        }
    }

    private fun playSpotify(query: String): ExecutionResult {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ExecutionResult(true, "Searching '$query' on Spotify")
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to open Spotify: ${e.message}")
        }
    }

    private fun runWebSearch(query: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Searching web for '$query'")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "Searching Google for '$query'")
        }
    }

    private fun makePhoneCall(phoneNumber: String): ExecutionResult {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse(if (cleanPhone.isNotBlank()) "tel:$cleanPhone" else "tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, if (cleanPhone.isNotBlank()) "Dialing $cleanPhone" else "Opening Phone Dialer")
        } catch (e: Exception) {
            ExecutionResult(false, "Cannot open dialer: ${e.message}")
        }
    }

    private fun sendSms(phoneNumber: String, message: String): ExecutionResult {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val uri = if (cleanPhone.isNotBlank()) Uri.parse("smsto:$cleanPhone") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Prepared SMS message for $cleanPhone")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open SMS: ${e.message}")
        }
    }

    private fun sendWhatsApp(phoneNumber: String, message: String): ExecutionResult {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ExecutionResult(true, "Opening WhatsApp message")
            } else {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                ExecutionResult(true, "Opening WhatsApp Web")
            }
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open WhatsApp: ${e.message}")
        }
    }

    private fun openSettingsIntent(actionName: String, label: String): ExecutionResult {
        return try {
            val intent = Intent(actionName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Opened $label")
        } catch (e: Exception) {
            // fallback to main settings
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                ExecutionResult(true, "Opened Settings")
            } catch (ex: Exception) {
                ExecutionResult(false, "Cannot open $label: ${ex.message}")
            }
        }
    }

    private fun adjustVolume(direction: String): ExecutionResult {
        if (audioManager == null) return ExecutionResult(false, "Audio Manager unavailable")
        return try {
            when (direction) {
                "up", "increase", "higher" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ExecutionResult(true, "Volume increased")
                }
                "down", "decrease", "lower" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ExecutionResult(true, "Volume decreased")
                }
                "mute", "silent", "off" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    ExecutionResult(true, "Volume muted")
                }
                "unmute", "max" -> {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
                    ExecutionResult(true, "Volume set to maximum")
                }
                else -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                    ExecutionResult(true, "Volume control opened")
                }
            }
        } catch (e: Exception) {
            ExecutionResult(false, "Error adjusting volume: ${e.message}")
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ExecutionResult(true, "Alarm set for $timeStr: $message")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not set alarm: ${e.message}")
        }
    }

    private fun setTimer(seconds: Int, message: String): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val sec = seconds % 60
            val dur = if (mins > 0) "$mins min $sec sec" else "$sec seconds"
            ExecutionResult(true, "Timer started for $dur")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not set timer: ${e.message}")
        }
    }

    private fun createCalendarEvent(title: String, description: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Calendar event created: $title")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open calendar: ${e.message}")
        }
    }
}

data class ExecutionResult(
    val isSuccess: Boolean,
    val message: String
)
