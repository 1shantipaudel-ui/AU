package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CleanAccentLavender
import com.example.ui.theme.CleanAccentPurple
import com.example.ui.theme.CleanAmber
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.OrbGlowInner
import com.example.ui.theme.OrbGlowOuter

@Composable
fun VoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.22f else if (isSpeaking) 1.14f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val activeColor = when {
        isListening -> CleanPrimary
        isSpeaking -> CleanAmber
        isProcessing -> CleanAccentPurple
        else -> CleanPrimary
    }

    val dynamicBoost = if (isListening) (audioRms * 0.35f) else 0f
    val currentScale = pulseScale + dynamicBoost

    Box(
        modifier = modifier
            .size(136.dp)
            .testTag("voice_orb_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing canvas with wave rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.8f * currentScale

            // Pulsing outer ripple rings
            if (isListening || isSpeaking) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.15f),
                    radius = radius * 1.32f,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = activeColor.copy(alpha = 0.25f),
                    radius = radius * 1.16f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Radial ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.35f),
                        activeColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )
        }

        // Inner Touchable Core
        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(if (isListening || isSpeaking) currentScale.coerceAtMost(1.18f) else 1f)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            when {
                                isListening -> CleanPrimary
                                isSpeaking -> CleanAmber
                                isProcessing -> CleanAccentPurple
                                else -> OrbGlowInner
                            },
                            when {
                                isListening -> CleanAccentPurple
                                isSpeaking -> CleanAccentLavender
                                isProcessing -> CleanPrimary
                                else -> OrbGlowOuter
                            }
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onClick
                )
                .testTag("voice_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            when {
                isListening -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening...",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                isSpeaking -> {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaking...",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                isProcessing -> {
                    SoundwaveVisualizer(color = Color.White)
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Tap to speak",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SoundwaveVisualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 24f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.width(4.dp).height(h2.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(3.dp))
        Box(modifier = Modifier.width(4.dp).height(h3.dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}

