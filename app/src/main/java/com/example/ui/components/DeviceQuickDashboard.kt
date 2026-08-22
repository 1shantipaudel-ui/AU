package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CleanAccentPurple
import com.example.ui.theme.CleanAmber
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanGreen
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DeviceQuickDashboard(
    isFlashlightOn: Boolean,
    isCloudEnabled: Boolean,
    notesCount: Int,
    wakeWord: String,
    onFlashlightToggle: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenWakeWordSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CleanSurface)
            .border(1.dp, CleanCardBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("device_quick_dashboard"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Identity & Engine Badge with Alex Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenWakeWordSettings() }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CleanPrimaryContainer)
                    .border(1.5.dp, CleanPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.alex_logo),
                    contentDescription = "Alex Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ALEX",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = TextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isCloudEnabled) CleanGreen else CleanAmber)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCloudEnabled) "Gemini Active" else "Smart AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCloudEnabled) CleanGreen else CleanAmber
                    )
                }
            }
        }

        // Quick Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Wake Word Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CleanPrimaryContainer.copy(alpha = 0.6f))
                    .border(1.dp, CleanPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { onOpenWakeWordSettings() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("dashboard_wake_word_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Wake Word Settings",
                    tint = CleanPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = wakeWord,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanPrimary,
                    maxLines = 1
                )
            }

            // Flashlight button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isFlashlightOn) CleanAmber.copy(alpha = 0.15f) else CleanSurfaceVariant)
                    .border(
                        1.dp,
                        if (isFlashlightOn) CleanAmber else CleanCardBorder,
                        CircleShape
                    )
                    .clickable { onFlashlightToggle() }
                    .testTag("dashboard_torch_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Toggle Torch",
                    tint = if (isFlashlightOn) CleanAmber else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Notes Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CleanSurfaceVariant)
                    .border(1.dp, CleanCardBorder, CircleShape)
                    .clickable { onOpenNotes() }
                    .testTag("dashboard_notes_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Saved Notes",
                    tint = CleanPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Help Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CleanSurfaceVariant)
                    .border(1.dp, CleanCardBorder, CircleShape)
                    .clickable { onOpenHelp() }
                    .testTag("dashboard_help_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help Guide",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

