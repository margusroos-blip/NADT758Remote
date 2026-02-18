package com.nadremote.app

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteScreen(
    nadState: NadState,
    displaySources: Map<Int, String>,
    nowPlaying: NowPlaying,
    presets: List<Preset>,
    favoritePresets: List<Int>,
    strings: StringResources,
    onPowerToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMuteToggle: () -> Unit,
    onSourceSelect: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    onOpenSpotify: () -> Unit
) {
    // Check if BluOS is active
    val currentSourceName = nadState.sources[nadState.sourceId]?.lowercase() ?: ""
    val isBluOsSource = currentSourceName.contains("bluos") || 
                        currentSourceName.contains("stream") ||
                        currentSourceName.contains("bluesound") ||
                        nowPlaying.service.isNotBlank()
    
    // Get favorite presets to display - sÃ¤ilitame kasutaja valitud jÃ¤rjekorra
    val displayPresets = if (favoritePresets.isNotEmpty()) {
        favoritePresets.mapNotNull { id -> presets.find { it.id == id } }.take(3)
    } else {
        presets.take(3)
    }
    
    // State for showing all presets
    var showAllPresets by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerButton(nadState.power, onPowerToggle)
        Spacer(Modifier.height(24.dp))
        
        if (displaySources.isNotEmpty()) {
            SourceSelector(displaySources, nadState.sourceId, strings, onSourceSelect)
        }
        
        // BluOS Presets (with Spotify button integrated)
        AnimatedVisibility(
            visible = isBluOsSource && presets.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            PresetSelector(
                presets = displayPresets,
                strings = strings,
                onSelect = onPresetSelect,
                onShowAll = { showAllPresets = true },
                onOpenSpotify = onOpenSpotify,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        // Now Playing - only show when BluOS source is selected AND has content
        AnimatedVisibility(
            visible = isBluOsSource && nowPlaying.hasContent,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NowPlayingCard(
                nowPlaying = nowPlaying,
                strings = strings,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))
        VolumeControl(nadState.volume, nadState.mute, strings, onVolumeUp, onVolumeDown, onMuteToggle)
        Spacer(Modifier.weight(1f))
    }
    
    // All Presets Bottom Sheet
    if (showAllPresets) {
        AllPresetsSheet(
            presets = presets,
            strings = strings,
            onSelect = { presetId ->
                onPresetSelect(presetId)
                showAllPresets = false
            },
            onDismiss = { showAllPresets = false }
        )
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// POWER BUTTON
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun PowerButton(isOn: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isOn) 8.dp else 0.dp,
        shadowElevation = if (isOn) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SOURCE SELECTOR
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun SourceSelector(
    sources: Map<Int, String>,
    currentSourceId: Int,
    strings: StringResources,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            strings.input,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            sources[currentSourceId] ?: "â€“",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        val sortedSources = sources.toList().sortedBy { it.first }.take(4)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            sortedSources.forEach { (id, name) ->
                SourceButton(name, id == currentSourceId) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(id)
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun SourceButton(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val icon = getSourceIcon(name)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(76.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = name,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getSourceIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        n.contains("tv") || n.contains("hdmi") || n.contains("telia") -> Icons.Default.Tv
        n.contains("stream") || n.contains("bluos") -> Icons.Default.Wifi
        n.contains("bluetooth") || n.contains("bt") -> Icons.Default.Bluetooth
        n.contains("cd") || n.contains("disc") -> Icons.Default.Album
        n.contains("tuner") || n.contains("radio") || n.contains("fm") -> Icons.Default.Radio
        n.contains("phono") || n.contains("vinyl") -> Icons.Default.GraphicEq
        n.contains("aux") -> Icons.Default.Cable
        n.contains("usb") -> Icons.Default.Usb
        n.contains("ps5") || n.contains("ps4") || n.contains("xbox") || 
            n.contains("nintendo") || n.contains("game") -> Icons.Default.SportsEsports
        else -> Icons.Default.Input
    }
}


