package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionType
import com.example.data.model.AssistantAction
import com.example.ui.theme.CleanAccentCyan
import com.example.ui.theme.CleanAccentPurple
import com.example.ui.theme.CleanAmber
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanGreen
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanRed
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ActionCard(
    action: AssistantAction,
    resultText: String?,
    isSuccess: Boolean,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, accentColor) = when (action.actionType) {
        ActionType.TOGGLE_FLASHLIGHT -> Triple(Icons.Default.FlashlightOn, "Flashlight Control", CleanAmber)
        ActionType.SET_ALARM -> Triple(Icons.Default.Alarm, "Alarm Clock", CleanPrimary)
        ActionType.SET_TIMER -> Triple(Icons.Default.HourglassBottom, "Timer", CleanAccentCyan)
        ActionType.PLAY_YOUTUBE -> Triple(Icons.Default.PlayArrow, "YouTube Music", CleanRed)
        ActionType.PLAY_SPOTIFY -> Triple(Icons.Default.MusicNote, "Spotify", CleanGreen)
        ActionType.WEB_SEARCH -> Triple(Icons.Default.Search, "Web Search", CleanAccentCyan)
        ActionType.PHONE_CALL -> Triple(Icons.Default.Call, "Phone Call", CleanGreen)
        ActionType.SEND_SMS -> Triple(Icons.Default.Send, "SMS Messenger", CleanAccentPurple)
        ActionType.SEND_WHATSAPP -> Triple(Icons.Default.Send, "WhatsApp Chat", CleanGreen)
        ActionType.OPEN_WIFI -> Triple(Icons.Default.Wifi, "Wi-Fi Settings", CleanAccentCyan)
        ActionType.OPEN_BLUETOOTH -> Triple(Icons.Default.Bluetooth, "Bluetooth Settings", CleanPrimary)
        ActionType.OPEN_AIRPLANE_MODE -> Triple(Icons.Default.NotificationsOff, "Airplane Mode", CleanAmber)
        ActionType.OPEN_DND -> Triple(Icons.Default.NotificationsOff, "Do Not Disturb", CleanAccentPurple)
        ActionType.ADJUST_VOLUME -> Triple(Icons.Default.VolumeUp, "Volume Control", CleanPrimary)
        ActionType.OPEN_BRIGHTNESS -> Triple(Icons.Default.Brightness6, "Brightness Settings", CleanAmber)
        ActionType.CREATE_CALENDAR_EVENT -> Triple(Icons.Default.Event, "Calendar Event", CleanPrimary)
        ActionType.SAVE_NOTE -> Triple(Icons.Default.NoteAdd, "Voice Note Saved", CleanAmber)
        ActionType.OPEN_APP -> Triple(Icons.Outlined.Apps, "Open App", CleanAccentPurple)
        ActionType.CHAT_REPLY, ActionType.UNKNOWN -> Triple(Icons.Default.Language, "Assistant", CleanPrimary)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CleanSurface)
            .border(1.dp, CleanCardBorder, RoundedCornerShape(16.dp))
            .clickable { onActionClick(action) }
            .padding(12.dp)
            .testTag("action_card_${action.actionType.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!resultText.isNullOrBlank()) {
                        Text(
                            text = resultText,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSuccess) CleanGreen.copy(alpha = 0.12f) else CleanRed.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (isSuccess) "Success" else "Failed",
                    tint = if (isSuccess) CleanGreen else CleanRed,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSuccess) "Executed" else "Failed",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSuccess) CleanGreen else CleanRed
                )
            }
        }
    }
}

