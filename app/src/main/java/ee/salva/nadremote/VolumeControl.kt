package com.nadremote.app

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
        
        Text(
            volumeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light,
                fontSize = fontSize
            ),
            color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        
        // Volume bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(volumePercent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isMuted) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary
                    )
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
            // Volume Down with long press repeat
            RepeatableButton(
                onAction = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVolumeDown() 
                }
            ) { interactionSource ->
                FilledTonalIconButton(
                    onClick = { },
                    modifier = Modifier.size(64.dp),
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
            FilledIconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMuteToggle() 
                },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isMuted) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isMuted) 
                        MaterialTheme.colorScheme.onError 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Volume Up with long press repeat
            RepeatableButton(
                onAction = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVolumeUp() 
                }
            ) { interactionSource ->
                FilledTonalIconButton(
                    onClick = { },
                    modifier = Modifier.size(64.dp),
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


