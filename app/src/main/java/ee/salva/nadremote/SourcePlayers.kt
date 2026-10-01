package com.nadremote.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
// Virtuaalsete sisendite playerid. NAD-i jaoks on mõlemad BluOS sisend, äpis
// eraldi "seadmed": Raadio nagu tuuner, Spotify nagu mängija.
// BluOS-i tavaline mini-player (NowPlayingCard + kiirnupud) jääb BluOS sisendile.
// ═══════════════════════════════════════════════════════════════════════════

/** Kas lemmik on sama jaam/allikas, mis BluOS-is praegu mängib (streamUrl). */
fun Preset.matchesStream(streamUrl: String): Boolean {
    if (url.isBlank() || streamUrl.isBlank()) return false
    return streamUrl == url || streamUrl.startsWith("$url/") || url.startsWith("$streamUrl/")
}

val Preset.isSpotifyPreset: Boolean
    get() = url.startsWith("Spotify:", ignoreCase = true)

// ───────────────────────────────────────────────────────────────────────────
// RAADIO
// ───────────────────────────────────────────────────────────────────────────

@Composable
fun RadioPlayer(
    nowPlaying: NowPlaying,
    pending: PendingPlay?,
    radioPresets: List<Preset>,
    strings: StringResources,
    onPresetSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    onEditFavorites: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val playingRadio = nowPlaying.hasContent && nowPlaying.isRadio && !nowPlaying.isSpotify
    val activePreset = when {
        pending != null -> radioPresets.firstOrNull { it.name == pending.title }
        playingRadio -> radioPresets.firstOrNull { it.matchesStream(nowPlaying.streamUrl) }
        else -> null
    }

    // Jaama nimi: lemmiku nimi (mida kasutaja tunneb) > BluOS-i jaama nimi
    val stationName = when {
        pending != null -> pending.title
        activePreset != null -> activePreset.name
        playingRadio -> nowPlaying.stationName.ifBlank { nowPlaying.track }
        else -> strings.radio
    }
    // Loo info, kui jaam seda edastab (BluOS title2/title3)
    val songInfo = if (pending == null && playingRadio) {
        listOf(nowPlaying.artist, nowPlaying.album).firstOrNull {
            it.isNotBlank() && !it.equals(stationName, true) && !it.equals(nowPlaying.track, true)
        }.orEmpty()
    } else ""
    val subtitle = when {
        pending != null -> strings.connecting
        playingRadio -> songInfo
        else -> strings.chooseStation
    }
    val logo = when {
        pending != null -> pending.imageUrl
        playingRadio -> nowPlaying.imageUrl.ifBlank { activePreset?.imageUrl.orEmpty() }
        else -> ""
    }

    val step: (Int) -> Unit = { dir ->
        if (radioPresets.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val index = activePreset?.let { radioPresets.indexOf(it) } ?: -1
            val next = if (index < 0) 0 else (index + dir + radioPresets.size) % radioPresets.size
            onPresetSelect(radioPresets[next].id)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StationLogo(url = logo, size = 64)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (pending == null && playingRadio && nowPlaying.qualityLabel.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    QualityBadge(nowPlaying.qualityLabel)
                }
            }
            if (pending != null) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onEditFavorites, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = strings.radioFavorites)
                }
                IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Search, contentDescription = strings.searchStations)
                }
            }
        }

        if (radioPresets.isNotEmpty()) {
            val listState = rememberLazyListState()
            val activeIndex = activePreset?.let { radioPresets.indexOf(it) } ?: -1
            // Hoia aktiivne jaam nähtaval
            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tuuneri "station down / up" — liigub lemmikjaamade vahel
                IconButton(onClick = { step(-1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
                LazyRow(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(radioPresets, key = { _, p -> p.id }) { _, preset ->
                        StationChip(
                            preset = preset,
                            active = preset.id == activePreset?.id,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPresetSelect(preset.id)
                            }
                        )
                    }
                }
                IconButton(onClick = { step(1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun StationChip(preset: Preset, active: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        stateTween(), label = "stationBorder"
    )
    val labelColor by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        stateTween(), label = "stationLabel"
    )
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(if (active) 2.dp else 1.dp, borderColor)
        ) {
            if (preset.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = preset.imageUrl,
                    contentDescription = preset.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CellTower, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            preset.name,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StationLogo(url: String, size: Int) {
    if (url.isNotBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(size.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.CellTower,
                    contentDescription = null,
                    modifier = Modifier.size((size / 2.2f).dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// SPOTIFY
// ───────────────────────────────────────────────────────────────────────────

/**
 * Kompaktne Spotify player: albumi kaas on kogu playeri taust (tumeda üleminekuga,
 * et tekst ja nupud oleksid loetavad). Kõrgus fikseeritud, et helitugevusele jääks ruumi.
 */
@Composable
fun SpotifyPlayer(
    nowPlaying: NowPlaying,
    pending: PendingPlay?,
    spotifyPresets: List<Preset>,
    strings: StringResources,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    onOpenSpotifyApp: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hasTrack = pending == null && nowPlaying.isSpotify && nowPlaying.hasContent
    val hasArt = hasTrack && nowPlaying.imageUrl.isNotBlank()
    val content = if (hasArt) Color.White else MaterialTheme.colorScheme.onSurface
    val contentDim = content.copy(alpha = 0.78f)
    var showPlaylists by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (hasArt) {
            AsyncImage(
                model = nowPlaying.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Tume üleminek: ülal kerge (pealkiri), all tugevam (nupud)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.45f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_spotify_official_green),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(96.dp)
                    .alpha(0.12f)
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (hasTrack) nowPlaying.track else "Spotify",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val secondLine = when {
                        pending != null -> strings.connecting
                        hasTrack -> listOf(nowPlaying.artist, nowPlaying.album)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        else -> ""
                    }
                    if (secondLine.isNotBlank()) {
                        Text(
                            secondLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Ava Spotify äpp (otsing, playlistid)
                IconButton(onClick = onOpenSpotifyApp, modifier = Modifier.size(36.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_spotify_official_green),
                        contentDescription = strings.openSpotifyApp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (hasTrack && nowPlaying.totalSeconds > 0) {
                TrackProgress(nowPlaying, content)
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spotify lemmikud (playlistid/albumid NAD-is) menüüs, et ei võtaks ruumi
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (spotifyPresets.isNotEmpty()) {
                        IconButton(onClick = { showPlaylists = true }) {
                            Icon(Icons.Default.QueueMusic, contentDescription = strings.presets, tint = contentDim)
                        }
                        DropdownMenu(expanded = showPlaylists, onDismissRequest = { showPlaylists = false }) {
                            spotifyPresets.forEach { preset ->
                                val active = nowPlaying.isSpotify && preset.matchesStream(nowPlaying.streamUrl)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            preset.name.removePrefix("Spotify: "),
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.QueueMusic, null) },
                                    onClick = {
                                        showPlaylists = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPresetSelect(preset.id)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipPrevious() },
                    enabled = hasTrack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, null, tint = content, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(10.dp))
                if (pending != null) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = content)
                    }
                } else {
                    // Play/paus: hele ring kaane peal, nagu Spotify enda playeris
                    Surface(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPlayPause() },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = if (hasArt) Color.White else MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (nowPlaying.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                tint = if (hasArt) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipNext() },
                    enabled = hasTrack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.SkipNext, null, tint = content, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
                // Tasakaal vasakpoolse lemmikute nupuga, et juhtnupud oleksid täpselt keskel
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

/**
 * Edenemisriba. BluOS saadab asukoha ainult muutuste korral, seega loeme
 * mängimise ajal sekundeid ise edasi viimasest teadaolevast väärtusest.
 */
@Composable
private fun TrackProgress(nowPlaying: NowPlaying, color: Color) {
    var position by remember(nowPlaying.track, nowPlaying.currentSeconds) {
        mutableIntStateOf(nowPlaying.currentSeconds)
    }
    LaunchedEffect(nowPlaying.track, nowPlaying.currentSeconds, nowPlaying.isPlaying) {
        val base = nowPlaying.currentSeconds
        val started = System.currentTimeMillis()
        while (nowPlaying.isPlaying) {
            delay(500)
            val elapsed = ((System.currentTimeMillis() - started) / 1000).toInt()
            position = (base + elapsed).coerceAtMost(nowPlaying.totalSeconds)
        }
    }
    val total = nowPlaying.totalSeconds.coerceAtLeast(1)
    Row(verticalAlignment = Alignment.CenterVertically) {
        val timeStyle = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        Text(formatTime(position), style = timeStyle, color = color.copy(alpha = 0.8f))
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { position.toFloat() / total },
            modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.25f)
        )
        Spacer(Modifier.width(8.dp))
        Text(formatTime(nowPlaying.totalSeconds), style = timeStyle, color = color.copy(alpha = 0.8f))
    }
}

private fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
