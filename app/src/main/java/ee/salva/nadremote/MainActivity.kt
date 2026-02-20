package com.nadremote.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: NadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by vm.theme.collectAsState()
            val darkTheme = when (theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            NadRemoteTheme(darkTheme = darkTheme) {
                NadRemoteApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NadRemoteApp(vm: NadViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    val connectionStatus by vm.connectionStatus.collectAsState()
    val nadState by vm.nadState.collectAsState()
    val savedIp by vm.savedIp.collectAsState()
    val strings by vm.strings.collectAsState()
    val displaySources by vm.displaySources.collectAsState()
    val nowPlaying by vm.nowPlaying.collectAsState()
    val presets by vm.presets.collectAsState()
    val quickButtonOrder by vm.quickButtonOrder.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Auto-reconnect when app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.tryAutoReconnect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(connectionStatus, savedIp) {
        if (connectionStatus == ConnectionStatus.DISCONNECTED && savedIp.isBlank()) {
            showSettings = true
        }
    }

    DisposableEffect(Unit) {
        vm.startMdns()
        onDispose { vm.stopMdns() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.appName, fontWeight = FontWeight.Bold)
                        if (nadState.model.isNotBlank()) {
                            Text(nadState.model, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    ConnectionIndicator(connectionStatus)
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, strings.settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            RemoteScreen(
                nadState = nadState,
                displaySources = displaySources,
                nowPlaying = nowPlaying,
                presets = presets,
                strings = strings,
                onPowerToggle = vm::powerToggle,
                onVolumeUp = vm::volumeUp,
                onVolumeDown = vm::volumeDown,
                onMuteToggle = vm::muteToggle,
                onSourceSelect = vm::setSource,
                onPlayPause = { if (nowPlaying.isPlaying) vm.blueOsPause() else vm.blueOsPlay() },
                onSkipNext = vm::blueOsNext,
                onSkipPrevious = vm::blueOsPrevious,
                onPresetSelect = vm::playPreset,
                onOpenSpotify = {
                    coroutineScope.launch {
                        val started = vm.startSpotifyOnBlueOs()
                        if (!started) {
                            Toast.makeText(
                                context,
                                "Spotify seanssi ei leitud BluOS-ist. Käivita Spotify Connect üks kord ja proovi uuesti.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                quickButtonOrder = quickButtonOrder,
                onQuickButtonOrderChange = vm::setQuickButtonOrder,
                onBrowseTuneIn = vm::browseTuneIn,
                onPlayBrowseEntry = vm::playBrowseEntry
            )

        }
    }

    if (showSettings) {
        SettingsSheet(vm) { showSettings = false }
    }
}


