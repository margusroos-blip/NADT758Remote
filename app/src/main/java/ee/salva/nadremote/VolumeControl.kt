package com.nadremote.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VolumeControl(
    volume: Int,
    isMuted: Boolean,
    strings: StringResources,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMuteToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val minVolume = -80
    val maxVolume = 0
    val volumePercent = ((volume - minVolume).toFloat() / (maxVolume - minVolume)).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            strings.volume,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))
        
        // Volume display - smaller font for longer text like "VAIGISTATUD"
        val volumeText = if (isMuted) strings.muted else "$volume dB"
        val fontSize = if (isMuted && volumeText.length > 6) 48.sp else 64.sp
        val displayColor by animateColorAsState(
            if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            stateTween(), label = "volumeText"
        )

        // Number liugleb muutumisel üles/alla; ühelaiused numbrid (tnum), et tekst ei tõmbleks
        AnimatedContent(
            targetState = volumeText,
            transitionSpec = {
                val up = (targetState.substringBefore(" ").toIntOrNull() ?: 0) >
                    (initialState.substringBefore(" ").toIntOrNull() ?: 0)
                val dir = if (up) 1 else -1
                (slideInVertically(tween(180)) { -dir * it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically(tween(180)) { dir * it / 3 } + fadeOut(tween(120)))
            },
            label = "volumeNumber"
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = fontSize,
                    fontFeatureSettings = "tnum"
                ),
                color = displayColor
            )
        }
        Spacer(Modifier.height(16.dp))

        // Volume bar
        val barFraction by animateFloatAsState(volumePercent, tween(160), label = "volumeBar")
        val barColor by animateColorAsState(
            if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            stateTween(), label = "volumeBarColor"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        
        // Min/Max labels
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$minVolume",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$maxVolume dB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))

        // Volume buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume Down with long press repeat. Iga samm annab kerge tiksu, mitte tugeva vibra.
            RepeatableButton(
                onAction = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onVolumeDown()
                }
            ) { interactionSource ->
                FilledTonalIconButton(
                    onClick = { },
                    modifier = Modifier.size(64.dp).pressScale(interactionSource),
                    interactionSource = interactionSource
                ) {
                    Icon(
                        Icons.Default.VolumeDown,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Mute button
            val muteInteraction = remember { MutableInteractionSource() }
            val muteContainer by animateColorAsState(
                if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                stateTween(), label = "muteContainer"
            )
            val muteTint by animateColorAsState(
                if (isMuted) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                stateTween(), label = "muteTint"
            )
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMuteToggle()
                },
                modifier = Modifier.size(72.dp).pressScale(muteInteraction),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = muteContainer),
                interactionSource = muteInteraction
            ) {
                Icon(
                    if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = muteTint
                )
            }

            // Volume Up with long press repeat
            RepeatableButton(
                onAction = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onVolumeUp()
                }
            ) { interactionSource ->
                FilledTonalIconButton(
                    onClick = { },
                    modifier = Modifier.size(64.dp).pressScale(interactionSource),
                    interactionSource = interactionSource
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// REPEATABLE BUTTON (for volume long press)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun RepeatableButton(
    onAction: () -> Unit,
    initialDelay: Long = 500L,
    repeatDelay: Long = 100L,
    content: @Composable (MutableInteractionSource) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onAction() // First action immediately
            delay(initialDelay) // Wait before starting repeat
            while (isPressed) {
                onAction()
                delay(repeatDelay)
            }
        }
    }
    
    content(interactionSource)
}


