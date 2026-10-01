package com.nadremote.app

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
    pendingPlay: PendingPlay?,
    spotifyStuck: Boolean,
    presets: List<Preset>,
    strings: StringResources,
    onPowerToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMuteToggle: () -> Unit,
    activeSourceId: Int,
    onSourceSelect: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    onOpenSpotify: () -> Unit,
    onOpenSpotifyApp: () -> Unit,
    quickButtonOrder: List<String>,
    onQuickButtonOrderChange: (List<String>) -> Unit,
    presetActions: PresetActions?,
    onBrowseTuneIn: suspend (String?) -> List<BrowseEntry>,
    onPlayBrowseEntry: suspend (BrowseEntry) -> Boolean,
    onSearchTuneIn: suspend (String) -> List<BrowseEntry>,
    onLocalRadio: suspend () -> List<BrowseEntry>,
    onTuneInQuality: suspend (String) -> StreamQuality?,
    radioSheetRequested: Boolean,
    onRadioSheetShown: () -> Unit
) {
    // Check if BluOS is active
    val currentSourceName = nadState.sources[nadState.sourceId]?.lowercase() ?: ""
    val isBluOsSource = currentSourceName.contains("bluos") || 
                        currentSourceName.contains("stream") ||
                        currentSourceName.contains("bluesound") ||
                        nowPlaying.service.isNotBlank()
    
    val hasNowPlaying = isBluOsSource && (nowPlaying.hasContent || pendingPlay != null)

    val panelMode = when (activeSourceId) {
        VirtualSource.RADIO -> PanelMode.RADIO
        VirtualSource.SPOTIFY -> PanelMode.SPOTIFY
        else -> if (isBluOsSource && (hasNowPlaying || presets.isNotEmpty())) PanelMode.BLUOS else PanelMode.NONE
    }

    // Lemmikud kasutaja järjekorras (sama mis kiirnuppudel), jagatud raadio- ja Spotify lemmikuteks
    val orderedPresets = remember(presets, quickButtonOrder) {
        presets.sortedBy { p ->
            quickButtonOrder.indexOf("preset:${p.id}").let { if (it < 0) Int.MAX_VALUE else it }
        }
    }
    val radioPresets = remember(orderedPresets) { orderedPresets.filter { !it.isSpotifyPreset } }
    val spotifyPresets = remember(orderedPresets) { orderedPresets.filter { it.isSpotifyPreset } }

    // Raadio otsinguleht: avaneb Raadio playeri 🔍 nupust või kui Raadio sisendil pole veel jaama
    val haptic = LocalHapticFeedback.current
    var showRadioSheet by remember { mutableStateOf(false) }
    LaunchedEffect(radioSheetRequested) {
        if (radioSheetRequested) {
            showRadioSheet = true
            onRadioSheetShown()
        }
    }
    // Raadio lemmikute haldus (Raadio playeri ✎ nupust)
    var showRadioFavorites by remember { mutableStateOf(false) }
    if (showRadioFavorites) {
        RadioFavoritesSheet(
            radioPresets = radioPresets,
            allPresets = presets,
            quickButtonOrder = quickButtonOrder,
            strings = strings,
            presetActions = presetActions,
            onOrderChange = onQuickButtonOrderChange,
            onSelect = onPresetSelect,
            onSearchStations = { showRadioSheet = true },
            onDismiss = { showRadioFavorites = false }
        )
    }

    if (showRadioSheet) {
        TuneInBrowseSheet(
            presets = presets,
            strings = strings,
            onAddStation = presetActions?.onAddStation,
            onRemovePreset = presetActions?.onDelete,
            onDismiss = { showRadioSheet = false },
            onBrowse = onBrowseTuneIn,
            onSearch = onSearchTuneIn,
            onLocalRadio = onLocalRadio,
            onQuality = onTuneInQuality,
            onPlayEntry = { entry ->
                val ok = onPlayBrowseEntry(entry)
                if (ok) showRadioSheet = false
                ok
            },
            onPulseHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )
    }

    // Paneeli taustale väga nõrk kaanepildi toon; vahetub sujuvalt iga looga
    val artworkColor = rememberArtworkColor(
        if (panelMode != PanelMode.NONE && nowPlaying.hasContent && pendingPlay == null) nowPlaying.imageUrl else ""
    )
    val panelBase = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val panelColor by animateColorAsState(
        targetValue = artworkColor?.copy(alpha = 0.10f)?.compositeOver(panelBase) ?: panelBase,
        animationSpec = tween(700),
        label = "panelTint"
    )

    // Helitugevus on äpi põhjus: see mõõdetakse ESIMESENA ja saab alati täissuuruse.
    // Ülemine osa (toide, sisendid, player) saab ülejäänud ruumi ja keritakse vajadusel —
    // ükski player ei tohi helitugevuse nuppe kokku suruda.
    VolumeFirstLayout(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp),
        top = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        PowerButton(nadState.power, onPowerToggle)
        Spacer(Modifier.height(14.dp))
        
        if (displaySources.isNotEmpty()) {
            SourceSelector(displaySources, activeSourceId, strings, onSourceSelect)
        }
        
        // Playeri kast: sisu sõltub sisendist. Raadio ja Spotify (virtuaalsed sisendid)
        // saavad oma playeri; BluOS sisend jääb tavalise mini-playeri + kiirnuppudega.
        AnimatedVisibility(
            visible = panelMode != PanelMode.NONE,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(22.dp),
                color = panelColor,
                border = hairlineBorder(0.06f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(
                        targetState = panelMode,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "panelMode"
                    ) { mode ->
                        when (mode) {
                            PanelMode.RADIO -> RadioPlayer(
                                nowPlaying = nowPlaying,
                                pending = pendingPlay,
                                radioPresets = radioPresets,
                                strings = strings,
                                onPresetSelect = onPresetSelect,
                                onSearch = { showRadioSheet = true },
                                onEditFavorites = { showRadioFavorites = true }
                            )

                            PanelMode.SPOTIFY -> SpotifyPlayer(
                                nowPlaying = nowPlaying,
                                pending = pendingPlay,
                                spotifyPresets = spotifyPresets,
                                strings = strings,
                                onPlayPause = onPlayPause,
                                onSkipNext = onSkipNext,
                                onSkipPrevious = onSkipPrevious,
                                onPresetSelect = onPresetSelect,
                                onOpenSpotifyApp = onOpenSpotifyApp
                            )

                            PanelMode.BLUOS -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (hasNowPlaying) {
                                    NowPlayingCard(
                                        nowPlaying = nowPlaying,
                                        strings = strings,
                                        onPlayPause = onPlayPause,
                                        onSkipNext = onSkipNext,
                                        onSkipPrevious = onSkipPrevious,
                                        onOpenSpotifyApp = onOpenSpotifyApp,
                                        pending = pendingPlay,
                                        embedded = true
                                    )
                                }
                                if (presets.isNotEmpty()) {
                                    PresetSelector(
                                        presets = presets,
                                        strings = strings,
                                        onSelect = onPresetSelect,
                                        onOpenSpotify = onOpenSpotify,
                                        onBrowseTuneIn = onBrowseTuneIn,
                                        onPlayBrowseEntry = onPlayBrowseEntry,
                                        onSearchTuneIn = onSearchTuneIn,
                                        onLocalRadio = onLocalRadio,
                                        onTuneInQuality = onTuneInQuality,
                                        quickButtonOrder = quickButtonOrder,
                                        onQuickButtonOrderChange = onQuickButtonOrderChange,
                                        presetActions = presetActions,
                                        showTitle = false
                                    )
                                }
                            }

                            PanelMode.NONE -> Unit
                        }
                    }

                    // Nähtav ainult siis, kui Spotify on kinni "connecting" olekus (heli ei tule)
                    AnimatedVisibility(
                        visible = spotifyStuck,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SpotifyStuckHint(strings = strings, onOpenSpotifyApp = onOpenSpotifyApp)
                    }
                }
            }
        }

            }
        },
        volume = {
            VolumeControl(nadState.volume, nadState.mute, strings, onVolumeUp, onVolumeDown, onMuteToggle)
        }
    )
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// POWER BUTTON
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun PowerButton(isOn: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (isOn) colors.primary else colors.surfaceVariant, stateTween(), label = "powerContainer"
    )
    val iconTint by animateColorAsState(
        if (isOn) colors.onPrimary else colors.onSurfaceVariant, stateTween(), label = "powerIcon"
    )
    // Kuma süttib sisselülitamisel korra sujuvalt ja jääb paigale (ei "hinga")
    val haloAlpha by animateFloatAsState(
        if (isOn) 0.16f else 0f, tween(600), label = "powerHalo"
    )
    val haloColor = colors.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .drawBehind {
                if (haloAlpha > 0f) {
                    val radius = size.minDimension * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(haloColor.copy(alpha = haloAlpha), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius
                    )
                }
            }
    ) {
        Surface(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
            modifier = Modifier.size(80.dp).pressScale(interaction),
            shape = CircleShape,
            color = container,
            border = if (isOn) null else hairlineBorder(),
            shadowElevation = if (isOn) 4.dp else 0.dp,
            interactionSource = interaction
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = iconTint
                )
            }
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
            sources[currentSourceId] ?: "-",
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
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (isSelected) colors.primaryContainer else colors.surfaceVariant, stateTween(), label = "sourceContainer"
    )
    val iconTint by animateColorAsState(
        if (isSelected) colors.primary else colors.onSurfaceVariant, stateTween(), label = "sourceIcon"
    )
    val labelColor by animateColorAsState(
        if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant, stateTween(), label = "sourceLabel"
    )
    val borderColor by animateColorAsState(
        if (isSelected) colors.primary.copy(alpha = 0.45f) else colors.onSurface.copy(alpha = 0.08f),
        stateTween(), label = "sourceBorder"
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.size(76.dp).pressScale(interaction),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interaction
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
                tint = iconTint
            )
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = labelColor,
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
        n.contains("spotify") -> Icons.Default.LibraryMusic
        n.contains("tuner") || n.contains("radio") || n.contains("raadio") || n.contains("fm") -> Icons.Rounded.CellTower
        n.contains("phono") || n.contains("vinyl") -> Icons.Default.GraphicEq
        n.contains("aux") -> Icons.Default.Cable
        n.contains("usb") -> Icons.Default.Usb
        n.contains("ps5") || n.contains("ps4") || n.contains("xbox") || 
            n.contains("nintendo") || n.contains("game") -> Icons.Default.SportsEsports
        else -> Icons.Default.Input
    }
}



// ═══════════════════════════════════════════════════════════════════════════
// SPOTIFY VIHJE: Spotify äpp näitab mängimist, aga NAD ei saa heli kätte.
// Aitab ainult seadme vahetus Spotify äpis (telefon -> NAD).
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SpotifyStuckHint(strings: StringResources, onOpenSpotifyApp: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                strings.spotifyNoAudio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenSpotifyApp) {
                Text(strings.openSpotifyApp, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Mida playeri kastis näidata. */
enum class PanelMode { NONE, BLUOS, RADIO, SPOTIFY }

/**
 * Paigutus, kus helitugevuse plokk mõõdetakse esimesena ja saab alati oma täissuuruse.
 * Ülemine osa saab ülejäänud kõrguse; helitugevus paigutatakse ülejäänud vaba ruumi keskele
 * (sama välimus nagu varem Spacer(weight) / Volume / Spacer(weight)).
 */
@Composable
private fun VolumeFirstLayout(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    volume: @Composable () -> Unit
) {
    Layout(contents = listOf(top, volume), modifier = modifier) { (topMeasurables, volumeMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val volumePlaceable = volumeMeasurables.first().measure(loose)
        val topMaxHeight = (constraints.maxHeight - volumePlaceable.height).coerceAtLeast(0)
        val topPlaceable = topMeasurables.first().measure(loose.copy(maxHeight = topMaxHeight))
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            topPlaceable.placeRelative((width - topPlaceable.width) / 2, 0)
            val free = (height - topPlaceable.height - volumePlaceable.height).coerceAtLeast(0)
            volumePlaceable.placeRelative((width - volumePlaceable.width) / 2, topPlaceable.height + free / 2)
        }
    }
}
