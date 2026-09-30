package com.nadremote.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun NowPlayingCard(
    nowPlaying: NowPlaying,
    strings: StringResources,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenSpotifyApp: (() -> Unit)? = null,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isRadioText = nowPlaying.isRadio && !nowPlaying.isSpotify
    val primaryMaxLines = if (isRadioText) 2 else 1
    val secondaryMaxLines = if (isRadioText) 2 else 1

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art / station logo. Spotify puhul avab pildile vajutus telefonis Spotify äpi
            // (otsing, playlistid); nurgas väike logo annab sellest märku.
            val openSpotify = onOpenSpotifyApp?.takeIf { nowPlaying.isSpotify }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .then(
                        if (openSpotify != null) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClickLabel = "Spotify") {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    openSpotify()
                                }
                        } else Modifier
                    )
            ) {
                if (nowPlaying.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = nowPlaying.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default icon
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when {
                                    nowPlaying.isSpotify -> Icons.Default.MusicNote
                                    nowPlaying.isRadio -> Icons.Default.Radio
                                    else -> Icons.Default.MusicNote
                                },
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (openSpotify != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(18.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_spotify_official_green),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            
            // Title, subtitle, album
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nowPlaying.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = primaryMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (nowPlaying.displaySubtitle.isNotBlank()) {
                    Text(
                        nowPlaying.displaySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = secondaryMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Album - ainult on-demand muusika puhul
                if (nowPlaying.displayAlbum.isNotBlank()) {
                    Text(
                        nowPlaying.displayAlbum,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Playback controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Previous - ainult on-demand sisu puhul
                if (nowPlaying.isOnDemand) {
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipPrevious() 
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Play/Pause
                FilledTonalIconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause() 
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (nowPlaying.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Next - ainult on-demand sisu puhul
                if (nowPlaying.isOnDemand) {
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipNext() 
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (embedded) {
        Box(modifier = modifier.fillMaxWidth().animateContentSize()) {
            content()
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            content()
        }
    }
}


