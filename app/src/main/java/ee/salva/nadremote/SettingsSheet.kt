package com.nadremote.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
    val autoReconnect by vm.autoReconnect.collectAsState()

    var showAdvanced by remember { mutableStateOf(false) }
    var showConnectionTools by remember { mutableStateOf(false) }
    var showFavoritePicker by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var ipInput by remember(savedIp) { mutableStateOf(savedIp) }

    var selectedFavorites by remember(favoriteSources) { mutableStateOf(favoriteSources.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsTitleRow(strings = strings, model = nadState.model.ifBlank { "NAD" })
            }

            if (connectionStatus == ConnectionStatus.CONNECTED && nadState.enabledSources.isNotEmpty()) {
                item {
                    SettingsCard {
                        SettingsSectionHeader(
                            icon = Icons.Default.Tune,
                            title = strings.favoriteSources,
                            subtitle = strings.favoriteSourcesDesc
                        )

                        ExpandableSelector(
                            title = if (favoriteSources.isEmpty()) strings.selectUpTo4
                            else favoriteSources.mapNotNull { nadState.sources[it] }.joinToString(", "),
                            expanded = showFavoritePicker,
                            onClick = { showFavoritePicker = !showFavoritePicker }
                        )

                        AnimatedVisibility(
                            visible = showFavoritePicker,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${selectedFavorites.size}/4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                nadState.enabledSources.forEach { (id, name) ->
                                    val isSelected = id in selectedFavorites
                                    val canSelect = selectedFavorites.size < 4 || isSelected

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFavorites = if (isSelected) selectedFavorites - id
                                            else if (canSelect) selectedFavorites + id
                                            else selectedFavorites
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
            }

            item {
                SettingsCard {
                    SettingsSectionHeader(
                        icon = Icons.Default.Palette,
                        title = strings.theme,
                        subtitle = strings.language
                    )

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
                            shape = RoundedCornerShape(14.dp)
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
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOption(strings.themeSystem, Icons.Default.Brightness6, currentTheme == AppTheme.SYSTEM, { vm.setTheme(AppTheme.SYSTEM) }, Modifier.weight(1f))
                        ThemeOption(strings.themeLight, Icons.Default.LightMode, currentTheme == AppTheme.LIGHT, { vm.setTheme(AppTheme.LIGHT) }, Modifier.weight(1f))
                        ThemeOption(strings.themeDark, Icons.Default.DarkMode, currentTheme == AppTheme.DARK, { vm.setTheme(AppTheme.DARK) }, Modifier.weight(1f))
                    }
                }
            }

            item {
                SettingsCard {
                    ExpandableSelector(
                        title = strings.advancedSettings,
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced }
                    )

                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.autoConnect, style = MaterialTheme.typography.bodyLarge)
                                        Text(strings.autoConnectDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = autoReconnect, onCheckedChange = { vm.setAutoReconnect(it) })
                                }
                            }

                            ExpandableSelector(
                                title = strings.deviceConnection,
                                expanded = showConnectionTools,
                                onClick = { showConnectionTools = !showConnectionTools }
                            )

                            AnimatedVisibility(
                                visible = showConnectionTools,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                                    if (devices.isNotEmpty() && connectionStatus != ConnectionStatus.CONNECTED) {
                                        Text(
                                            strings.discoveredDevices,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            devices.forEach { device ->
                                                Surface(
                                                    onClick = { vm.connect(device.ip); onDismiss() },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(device.name.ifBlank { "NAD" }, fontWeight = FontWeight.Medium)
                                                            Text(
                                                                device.ip,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }

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
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

                                    TextButton(onClick = { vm.clearDevice(); ipInput = "" }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Delete, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.clearSavedDevice)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SettingsTitleRow(
    strings: StringResources,
    model: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(strings.settings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(model, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpandableSelector(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeviceConnectionCard(
    connectionStatus: ConnectionStatus,
    deviceName: String,
    savedIp: String,
    error: String?,
    strings: StringResources,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onScan: () -> Unit
) {
    val containerColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(shape = RoundedCornerShape(16.dp), color = containerColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                            ConnectionStatus.CONNECTING -> Icons.Default.Sync
                            ConnectionStatus.ERROR -> Icons.Default.Error
                            ConnectionStatus.DISCONNECTED -> Icons.Default.WifiOff
                        },
                        null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(10.dp))
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
                                if (!error.isNullOrBlank()) {
                                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            ConnectionStatus.DISCONNECTED -> {
                                Text(strings.disconnected, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(strings.tapToConnect, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (connectionStatus == ConnectionStatus.CONNECTING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

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

                    ConnectionStatus.CONNECTING -> Unit
                }
            }
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 3.dp else 0.dp,
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
