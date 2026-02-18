package com.nadremote.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    vm: NadViewModel,
    onDismiss: () -> Unit
) {
    val strings by vm.strings.collectAsState()
    val savedIp by vm.savedIp.collectAsState()
    val devices by vm.devices.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val connectionStatus by vm.connectionStatus.collectAsState()
    val error by vm.error.collectAsState()
    val currentTheme by vm.theme.collectAsState()
    val currentLanguage by vm.language.collectAsState()
    val nadState by vm.nadState.collectAsState()
    val favoriteSources by vm.favoriteSources.collectAsState()
    val favoritePresets by vm.favoritePresets.collectAsState()
    val presets by vm.presets.collectAsState()
    val autoReconnect by vm.autoReconnect.collectAsState()

    var showAdvanced by remember { mutableStateOf(false) }
    var showFavoritePicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    var ipInput by remember(savedIp) { mutableStateOf(savedIp) }
    
    // Local state for favorite selection
    var selectedFavorites by remember(favoriteSources) { mutableStateOf(favoriteSources.toSet()) }
    var selectedPresetFavorites by remember(favoritePresets) { mutableStateOf(favoritePresets) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(strings.settings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            // Device section
            item { SettingsSectionHeader(strings.deviceConnection) }

            item {
                DeviceConnectionCard(
                    connectionStatus = connectionStatus,
                    deviceName = nadState.model.ifBlank { "NAD" },
                    savedIp = savedIp,
                    error = error,
                    strings = strings,
                    onConnect = { vm.reconnect() },
                    onDisconnect = { vm.disconnect() },
                    onScan = { vm.scanSubnet() }
                )
            }

            // Discovered devices
            if (devices.isNotEmpty() && connectionStatus != ConnectionStatus.CONNECTED) {
                item {
                    Text(strings.discoveredDevices, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                items(devices.take(3)) { device ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { vm.connect(device.ip); onDismiss() }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(device.name, fontWeight = FontWeight.Medium)
                                Text(device.ip, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Favorite sources picker
            if (connectionStatus == ConnectionStatus.CONNECTED && nadState.enabledSources.isNotEmpty()) {
                item { SettingsSectionHeader(strings.favoriteSources) }
                
                item {
                    Surface(
                        onClick = { showFavoritePicker = !showFavoritePicker },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (favoriteSources.isEmpty()) strings.selectUpTo4 
                                    else favoriteSources.mapNotNull { nadState.sources[it] }.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(strings.favoriteSourcesDesc, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                if (showFavoritePicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Favorite picker chips
                if (showFavoritePicker) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            nadState.enabledSources.forEach { (id, name) ->
                                val isSelected = id in selectedFavorites
                                val canSelect = selectedFavorites.size < 4 || isSelected
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedFavorites = selectedFavorites - id
                                        } else if (canSelect) {
                                            selectedFavorites = selectedFavorites + id
                                        }
                                        vm.setFavoriteSources(selectedFavorites.toList().sorted())
                                    },
                                    label = { Text(name) },
                                    enabled = canSelect || isSelected,
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            if (selectedFavorites.isNotEmpty()) {
                                TextButton(
                                    onClick = { 
                                        selectedFavorites = emptySet()
                                        vm.setFavoriteSources(emptyList())
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Clear all")
                                }
                            }
                        }
                    }
                }
            }

            // Language
            item { SettingsSectionHeader(strings.language) }

            // Favorite presets picker (show when connected and presets available)
            if (connectionStatus == ConnectionStatus.CONNECTED && presets.isNotEmpty()) {
                item { SettingsSectionHeader(strings.presets) }
                
                item {
                    Surface(
                        onClick = { showPresetPicker = !showPresetPicker },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (selectedPresetFavorites.isEmpty()) strings.selectUpTo3
                                    else selectedPresetFavorites.mapNotNull { id -> 
                                        presets.find { it.id == id }?.name 
                                    }.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(strings.favoritePresetsDesc, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                if (showPresetPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (showPresetPicker) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Valitud presetid Ã¼leval - jÃ¤rjekord on oluline!
                            if (selectedPresetFavorites.isNotEmpty()) {
                                Text(
                                    "âœ“ ${selectedPresetFavorites.size}/3",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            presets.forEach { preset ->
                                val isSelected = preset.id in selectedPresetFavorites
                                val canSelect = selectedPresetFavorites.size < 3 || isSelected
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPresetFavorites = if (isSelected) {
                                            selectedPresetFavorites - preset.id
                                        } else if (canSelect) {
                                            selectedPresetFavorites + preset.id
                                        } else {
                                            selectedPresetFavorites
                                        }
                                        vm.setFavoritePresets(selectedPresetFavorites)
                                    },
                                    label = { Text(preset.name) },
                                    enabled = canSelect || isSelected,
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            if (selectedPresetFavorites.isNotEmpty()) {
                                TextButton(
                                    onClick = { 
                                        selectedPresetFavorites = emptyList()
                                        vm.setFavoritePresets(emptyList())
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Clear all")
                                }
                            }
                        }
                    }
                }
            }
            item {
                var languageExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentLanguage.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.displayName) },
                                onClick = {
                                    vm.setLanguage(language)
                                    languageExpanded = false
                                },
                                leadingIcon = {
                                    if (currentLanguage == language) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Theme
            item { SettingsSectionHeader(strings.theme) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption(strings.themeSystem, Icons.Default.Brightness6, currentTheme == AppTheme.SYSTEM,
                        { vm.setTheme(AppTheme.SYSTEM) }, Modifier.weight(1f))
                    ThemeOption(strings.themeLight, Icons.Default.LightMode, currentTheme == AppTheme.LIGHT,
                        { vm.setTheme(AppTheme.LIGHT) }, Modifier.weight(1f))
                    ThemeOption(strings.themeDark, Icons.Default.DarkMode, currentTheme == AppTheme.DARK,
                        { vm.setTheme(AppTheme.DARK) }, Modifier.weight(1f))
                }
            }

            // Advanced settings
            item {
                Surface(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(strings.advancedSettings, style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(
                            if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showAdvanced) {
                // Auto-reconnect toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.autoConnect, style = MaterialTheme.typography.bodyMedium)
                            Text(strings.autoConnectDesc, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoReconnect, onCheckedChange = { vm.setAutoReconnect(it) })
                    }
                }
                
                // Manual IP
                item {
                    Text(strings.manualIp, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                }

                item {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.deviceIp) },
                        placeholder = { Text(strings.deviceIpPlaceholder) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (ipInput.isNotBlank()) vm.connect(ipInput) }),
                        trailingIcon = {
                            if (ipInput.isNotBlank()) {
                                IconButton(onClick = { ipInput = "" }) { Icon(Icons.Default.Clear, null) }
                            }
                        }
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { vm.connect(ipInput); onDismiss() },
                            modifier = Modifier.weight(1f),
                            enabled = ipInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Link, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.connect)
                        }
                        OutlinedButton(onClick = { vm.scanSubnet() }, modifier = Modifier.weight(1f)) {
                            if (isScanning) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isScanning) strings.scanning else strings.scanNetwork)
                        }
                    }
                }

                // Scanned devices
                if (devices.isNotEmpty()) {
                    item {
                        Text(strings.discoveredDevices, style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    items(devices) { device ->
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { vm.connect(device.ip); onDismiss() }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(device.name, fontWeight = FontWeight.Medium)
                                    Text(device.ip, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Forget device
                item {
                    TextButton(onClick = { vm.clearDevice(); ipInput = "" }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clearSavedDevice)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DeviceConnectionCard(
    connectionStatus: ConnectionStatus, deviceName: String, savedIp: String, error: String?,
    strings: StringResources, onConnect: () -> Unit, onDisconnect: () -> Unit, onScan: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                            ConnectionStatus.CONNECTING -> Icons.Default.Sync
                            ConnectionStatus.ERROR -> Icons.Default.Error
                            else -> Icons.Default.WifiOff
                        },
                        null, modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> {
                                Text(deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(strings.connected, style = MaterialTheme.typography.bodySmall)
                            }
                            ConnectionStatus.CONNECTING -> {
                                Text(strings.connecting, style = MaterialTheme.typography.titleMedium)
                            }
                            ConnectionStatus.ERROR -> {
                                Text(strings.error, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            }
                            else -> {
                                Text(strings.disconnected, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(strings.tapToConnect, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> {
                        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.LinkOff, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.disconnect)
                        }
                    }
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                        if (savedIp.isNotBlank()) {
                            Button(onClick = onConnect, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.connect)
                            }
                        }
                        OutlinedButton(
                            onClick = onScan,
                            modifier = if (savedIp.isBlank()) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.scanNetwork)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun ThemeOption(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


