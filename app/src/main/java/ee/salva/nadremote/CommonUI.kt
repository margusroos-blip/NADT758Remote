package com.nadremote.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CONNECTION INDICATOR
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun ConnectionIndicator(status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)    // Green
        ConnectionStatus.CONNECTING -> Color(0xFFFFC107)   // Yellow
        ConnectionStatus.ERROR -> Color(0xFFF44336)        // Red
        ConnectionStatus.DISCONNECTED -> Color(0xFF9E9E9E) // Gray
    }
    
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionBanner(
    status: ConnectionStatus,
    strings: StringResources,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status == ConnectionStatus.CONNECTED) return

    val (icon, tint, message) = when (status) {
        ConnectionStatus.CONNECTING -> Triple(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.tertiary,
            "${strings.connecting}  •  ${strings.tapToConnect}"
        )
        ConnectionStatus.ERROR -> Triple(
            Icons.Default.ErrorOutline,
            MaterialTheme.colorScheme.error,
            "${strings.error}  •  ${strings.openSettings}"
        )
        ConnectionStatus.DISCONNECTED -> Triple(
            Icons.Default.WifiOff,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "${strings.notConnected}  •  ${strings.tapToConnect}"
        )
        ConnectionStatus.CONNECTED -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primary,
            strings.connected
        )
    }

    Surface(
        onClick = onOpenSettings,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = tint)
            Text(
                text = message,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// DISCONNECTED SCREEN
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun DisconnectedScreen(
    status: ConnectionStatus,
    strings: StringResources,
    onOpenSettings: () -> Unit,
    onReconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = when (status) {
            ConnectionStatus.CONNECTING -> Icons.Default.Sync
            ConnectionStatus.ERROR -> Icons.Default.ErrorOutline
            else -> Icons.Default.WifiOff
        }
        
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = when (status) {
                ConnectionStatus.CONNECTING -> strings.connecting
                ConnectionStatus.ERROR -> strings.error
                else -> strings.notConnected
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(Modifier.height(32.dp))
        
        if (status != ConnectionStatus.CONNECTING) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.openSettings)
                }
                
                if (status == ConnectionStatus.ERROR || status == ConnectionStatus.DISCONNECTED) {
                    Button(onClick = onReconnect) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.tryAgain)
                    }
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}


