package com.nadremote.app

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// PRESET SELECTOR (main screen - Spotify + 2 presets + more button)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun PresetSelector(
    presets: List<Preset>,
    strings: StringResources,
    onSelect: (Int) -> Unit,
    onOpenSpotify: (() -> Unit)? = null,
    onBrowseTuneIn: (suspend (String?) -> List<BrowseEntry>)? = null,
    onPlayBrowseEntry: (suspend (BrowseEntry) -> Boolean)? = null,
    onSearchTuneIn: (suspend (String) -> List<BrowseEntry>)? = null,
    onLocalRadio: (suspend () -> List<BrowseEntry>)? = null,
    onTuneInQuality: (suspend (String) -> StreamQuality?)? = null,
    radioSheetRequested: Boolean = false,
    onRadioSheetShown: () -> Unit = {},
    quickButtonOrder: List<String> = emptyList(),
    onQuickButtonOrderChange: ((List<String>) -> Unit)? = null,
    presetActions: PresetActions? = null,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val showSpotify = onOpenSpotify != null
    val showTuneIn = onBrowseTuneIn != null && onPlayBrowseEntry != null
    val candidateButtons = remember(showSpotify, showTuneIn, presets) {
        buildList {
            if (showSpotify) add(QuickButtonItem.Spotify)
            if (showTuneIn) add(QuickButtonItem.TuneIn)
            presets.forEach { add(QuickButtonItem.PresetItem(it)) }
        }
    }
    var orderedButtons by remember(candidateButtons, quickButtonOrder) {
        mutableStateOf(applyQuickButtonOrder(candidateButtons, quickButtonOrder))
    }
    var showTuneInSheet by remember { mutableStateOf(false) }

    // "Raadio" sisend, aga jaama pole veel kuulatud -> ava raadio leht
    LaunchedEffect(radioSheetRequested) {
        if (radioSheetRequested && showTuneIn) {
            showTuneInSheet = true
            onRadioSheetShown()
        }
    }
    var showQuickOrderSheet by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val stepPx = with(LocalDensity.current) { 84.dp.toPx() }
    val swapThreshold = stepPx * 0.55f
    val quickButtons = orderedButtons.take(3)
    val totalRemaining = (orderedButtons.size - quickButtons.size).coerceAtLeast(0)

    val activateButton: (QuickButtonItem) -> Unit = { button ->
        when (button) {
            QuickButtonItem.Spotify -> onOpenSpotify?.invoke()
            QuickButtonItem.TuneIn -> showTuneInSheet = true
            is QuickButtonItem.PresetItem -> onSelect(button.preset.id)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        if (showTitle) {
            Text(
                strings.presets,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))

            quickButtons.forEachIndexed { index, button ->
                key(button.orderKey) {
                    val currentIndex by rememberUpdatedState(index)
                    val currentButton by rememberUpdatedState(button)
                    val press = rememberPressState()
                    Box(
                        modifier = Modifier
                            .pressScale(press.isPressed && draggingIndex == -1)
                            .zIndex(if (draggingIndex == currentIndex) 1f else 0f)
                            .offset {
                                IntOffset(
                                    x = if (draggingIndex == currentIndex) dragOffsetPx.roundToInt() else 0,
                                    y = 0
                                )
                            }
                            .graphicsLayer {
                                if (draggingIndex == currentIndex) {
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        val quickCount = minOf(3, orderedButtons.size)
                                        if (quickCount < 2) return@detectDragGestures
                                        draggingIndex = currentIndex
                                        dragOffsetPx = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = {
                                        draggingIndex = -1
                                        dragOffsetPx = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffsetPx = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        val quickLastIndex = minOf(2, orderedButtons.lastIndex)
                                        if (quickLastIndex < 1) return@detectDragGestures
                                        val current = draggingIndex
                                        if (current !in 0..quickLastIndex) return@detectDragGestures

                                        dragOffsetPx += dragAmount.x
                                        var target = current
                                        if (dragOffsetPx > swapThreshold && current < quickLastIndex) {
                                            target = current + 1
                                            dragOffsetPx -= stepPx
                                        } else if (dragOffsetPx < -swapThreshold && current > 0) {
                                            target = current - 1
                                            dragOffsetPx += stepPx
                                        }

                                        if (target != current) {
                                            orderedButtons = orderedButtons.toMutableList().apply {
                                                add(target, removeAt(current))
                                            }
                                            draggingIndex = target
                                            onQuickButtonOrderChange?.invoke(orderedButtons.map { it.orderKey })
                                        }
                                        change.consume()
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        press.isPressed = true
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            press.isPressed = false
                                        }
                                    },
                                    onTap = {
                                        if (draggingIndex != -1) return@detectTapGestures
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activateButton(currentButton)
                                    }
                                )
                            }
                    ) {
                        when (button) {
                            QuickButtonItem.Spotify -> SpotifyButton()
                            QuickButtonItem.TuneIn -> TuneInButton(label = strings.radio)
                            is QuickButtonItem.PresetItem -> PresetButton(preset = button.preset)
                        }
                    }
                }
            }

            if (totalRemaining > 0) {
                Box(
                    modifier = Modifier.pointerInput(totalRemaining) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showQuickOrderSheet = true
                            }
                        )
                    }
                ) {
                    MorePresetsButton(remainingCount = totalRemaining)
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }

    if (showQuickOrderSheet) {
        QuickButtonOrderSheet(
            buttons = orderedButtons,
            strings = strings,
            onDismiss = { showQuickOrderSheet = false },
            onOrderChange = { orderKeys ->
                orderedButtons = applyQuickButtonOrder(candidateButtons, orderKeys)
                onQuickButtonOrderChange?.invoke(orderedButtons.map { it.orderKey })
            },
            onActivate = { button ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activateButton(button)
            },
            presetActions = presetActions
        )
    }

    if (showTuneInSheet && onBrowseTuneIn != null && onPlayBrowseEntry != null) {
        TuneInBrowseSheet(
            presets = presets,
            strings = strings,
            onAddStation = presetActions?.onAddStation,
            onRemovePreset = presetActions?.onDelete,
            onDismiss = { showTuneInSheet = false },
            onBrowse = onBrowseTuneIn,
            onSearch = onSearchTuneIn,
            onLocalRadio = onLocalRadio,
            onQuality = onTuneInQuality,
            onPlayEntry = { entry ->
                val ok = onPlayBrowseEntry(entry)
                if (ok) {
                    showTuneInSheet = false
                }
                ok
            },
            onPulseHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SPOTIFY BUTTON (sama suurus ja stiil kui preset nupud)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun SpotifyButton(size: Dp = 76.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        border = hairlineBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_spotify_official_green),
                contentDescription = "Spotify",
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Spotify",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TuneInButton(label: String, size: Dp = 76.dp) {
    // "Raadio" nupp (TuneIn + Airable). Ikoon: saatetorn aktsentvärvi ringis —
    // sama kaaluga kui Spotify logo kõrvalnupul, mitte hall üldikoon.
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        border = hairlineBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CellTower,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// PRESET BUTTON
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun PresetButton(
    preset: Preset,
    size: Dp = 76.dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        border = hairlineBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (preset.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = preset.imageUrl,
                    contentDescription = preset.name,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = preset.name,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// MORE PRESETS BUTTON
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun MorePresetsButton(
    remainingCount: Int
) {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        border = hairlineBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "+$remainingCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Lemmikute haldus. Kõik muudavad lemmikuid NAD-is endas (BluOS presetid),
 * seega on muutused näha ka BluOS-i äpis.
 */
class PresetActions(
    val canAddCurrent: Boolean,
    val onAddCurrent: suspend () -> Boolean,
    val onAddStation: suspend (BrowseEntry) -> Boolean,
    val onRename: suspend (Preset, String) -> Boolean,
    val onDelete: suspend (Preset) -> Boolean
)

internal sealed class QuickButtonItem(val orderKey: String) {
    data object Spotify : QuickButtonItem("spotify")
    data object TuneIn : QuickButtonItem("tunein")
    data class PresetItem(val preset: Preset) : QuickButtonItem("preset:${preset.id}")
}

internal fun applyQuickButtonOrder(
    baseButtons: List<QuickButtonItem>,
    savedOrder: List<String>
): List<QuickButtonItem> {
    if (baseButtons.isEmpty()) return baseButtons
    if (savedOrder.isEmpty()) return baseButtons

    val byToken = baseButtons.associateBy { it.orderKey }
    val ordered = mutableListOf<QuickButtonItem>()
    val used = mutableSetOf<String>()

    savedOrder.forEach { token ->
        val item = byToken[token] ?: return@forEach
        ordered.add(item)
        used.add(token)
    }
    baseButtons.forEach { item ->
        if (item.orderKey !in used) ordered.add(item)
    }
    return ordered
}

@Composable
private fun QuickButtonVisual(
    button: QuickButtonItem,
    strings: StringResources,
    size: Dp = 76.dp
) {
    when (button) {
        QuickButtonItem.Spotify -> SpotifyButton(size = size)
        QuickButtonItem.TuneIn -> TuneInButton(label = strings.radio, size = size)
        is QuickButtonItem.PresetItem -> PresetButton(preset = button.preset, size = size)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickButtonOrderSheet(
    buttons: List<QuickButtonItem>,
    strings: StringResources,
    onDismiss: () -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onActivate: (QuickButtonItem) -> Unit,
    presetActions: PresetActions? = null,
    title: String? = null,
    hint: String? = null,
    showTopBadges: Boolean = true,
    onSearchStations: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editMode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Preset?>(null) }
    var deleteTarget by remember { mutableStateOf<Preset?>(null) }

    // Käivitab lemmiku muudatuse NAD-is; ebaõnnestumisel lühike teade
    val runPresetAction: (suspend () -> Boolean) -> Unit = { action ->
        if (!busy) {
            scope.launch {
                busy = true
                val ok = action()
                busy = false
                if (!ok) Toast.makeText(context, strings.presetActionFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var ordered by remember(buttons) { mutableStateOf(buttons) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragTarget by remember { mutableIntStateOf(-1) }
    var fingerPos by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    // Iga ruudustiku koha (indeksi) asukoht; kohad ei liigu, liiguvad ainult ikoonid
    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    // Tavaline hoidja, et koordinaatide uuendus ei käivitaks uut kompositsiooni
    val gridCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val columns = 4
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title ?: strings.presets,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                if (presetActions != null) {
                    TextButton(onClick = { editMode = !editMode }) {
                        Text(if (editMode) strings.presetDone else strings.presetEdit)
                    }
                }
            }

            Text(
                text = if (editMode) strings.presetEditHint else (hint ?: strings.reorderHint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (presetActions != null && presetActions.canAddCurrent) {
                OutlinedButton(
                    onClick = { runPresetAction { presetActions.onAddCurrent() } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.addCurrentPreset)
                }
            }

            // Raadio lemmikute lehel: uue jaama lisamine käib otsingu kaudu
            if (onSearchStations != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSearchStations()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.searchStations)
                }
            }

            // Lohistamine: pikk vajutus tõstab ikooni sõrme külge (ülekate), teised teevad
            // eelvaates ruumi; uus järjekord salvestatakse alles siis, kui sõrme lahti lased.
            // Drag on terve ruudustiku peal (mitte iga ikooni peal), et ikoon ei kukuks
            // sõrme küljest ära, kui see teise ritta liigub.
            val dragged = dragKey?.let { k -> ordered.firstOrNull { it.orderKey == k } }
            val shown = if (dragged != null && dragTarget >= 0) {
                ordered.filter { it.orderKey != dragKey }.toMutableList().apply {
                    add(dragTarget.coerceIn(0, size), dragged)
                }
            } else {
                ordered
            }

            fun slotAt(point: Offset): Int = slotRects.entries
                .filter { it.key < ordered.size }
                .minByOrNull { (it.value.center - point).getDistanceSquared() }
                ?.key ?: -1

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(scrollState, enabled = dragKey == null)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { gridCoords[0] = it }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start ->
                                    if (ordered.size < 2) return@detectDragGesturesAfterLongPress
                                    val hit = slotRects.entries
                                        .firstOrNull { it.key < ordered.size && it.value.contains(start) }
                                        ?: return@detectDragGesturesAfterLongPress
                                    dragKey = ordered[hit.key].orderKey
                                    dragTarget = hit.key
                                    grabOffset = start - hit.value.topLeft
                                    fingerPos = start
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    if (dragKey == null) return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    fingerPos += amount
                                    val target = slotAt(fingerPos)
                                    if (target >= 0 && target != dragTarget) {
                                        dragTarget = target
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    val key = dragKey
                                    val target = dragTarget
                                    val item = ordered.firstOrNull { it.orderKey == key }
                                    if (item != null && target >= 0) {
                                        val newOrder = ordered.filter { it.orderKey != key }.toMutableList().apply {
                                            add(target.coerceIn(0, size), item)
                                        }
                                        if (newOrder != ordered) {
                                            ordered = newOrder
                                            onOrderChange(newOrder.map { it.orderKey })
                                        }
                                    }
                                    dragKey = null
                                    dragTarget = -1
                                },
                                onDragCancel = {
                                    dragKey = null
                                    dragTarget = -1
                                }
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shown.chunked(columns).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEachIndexed { colIndex, button ->
                                val index = rowIndex * columns + colIndex
                                val isDragged = button.orderKey == dragKey
                                key(button.orderKey) {
                                    val currentButton by rememberUpdatedState(button)
                                    Box(
                                        modifier = Modifier
                                            .onGloballyPositioned { cell ->
                                                gridCoords[0]?.let { grid ->
                                                    if (grid.isAttached && cell.isAttached) {
                                                        slotRects[index] = Rect(
                                                            grid.localPositionOf(cell, Offset.Zero),
                                                            cell.size.toSize()
                                                        )
                                                    }
                                                }
                                            }
                                            // Tühi koht, kuhu lohistatav ikoon maandub
                                            .graphicsLayer { alpha = if (isDragged) 0.25f else 1f }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (dragKey != null) return@detectTapGestures
                                                        val tapped = currentButton
                                                        if (editMode) {
                                                            // Muutmisrežiimis: vajutus = nimeta ümber
                                                            if (tapped is QuickButtonItem.PresetItem) renameTarget = tapped.preset
                                                        } else {
                                                            onActivate(tapped)
                                                            onDismiss()
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        QuickButtonVisual(button = button, strings = strings, size = 84.dp)
                                        if (editMode && button is QuickButtonItem.PresetItem && !isDragged) {
                                            Surface(
                                                onClick = { deleteTarget = button.preset },
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(top = 2.dp, start = 2.dp)
                                                    .size(22.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = strings.remove,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onError
                                                    )
                                                }
                                            }
                                        }
                                        if (showTopBadges && index < 3) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 2.dp, end = 2.dp)
                                                    .size(18.dp),
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                tonalElevation = 2.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(columns - row.size) {
                                Spacer(Modifier.size(84.dp))
                            }
                        }
                    }
                }

                // Sõrme küljes olev ikoon: veidi suurem ja varjuga, järgib sõrme täpselt
                if (dragged != null) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (fingerPos.x - grabOffset.x).roundToInt(),
                                    (fingerPos.y - grabOffset.y).roundToInt()
                                )
                            }
                            .zIndex(2f)
                            .graphicsLayer {
                                scaleX = 1.08f
                                scaleY = 1.08f
                                shadowElevation = 18f
                                shape = RoundedCornerShape(16.dp)
                            }
                    ) {
                        QuickButtonVisual(button = dragged, strings = strings, size = 84.dp)
                    }
                }
            }
        }
    }

    renameTarget?.let { preset ->
        var newName by remember(preset.id) { mutableStateOf(preset.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.renamePreset) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(60) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = preset
                        val name = newName.trim()
                        renameTarget = null
                        if (name.isNotBlank() && name != target.name && presetActions != null) {
                            runPresetAction { presetActions.onRename(target, name) }
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    deleteTarget?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.removePresetTitle) },
            text = { Text(preset.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = preset
                        deleteTarget = null
                        if (presetActions != null) {
                            runPresetAction { presetActions.onDelete(target) }
                        }
                    }
                ) { Text(strings.remove, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

private data class TuneInPathNode(
    val title: String,
    val key: String?
)

/**
 * TuneIn: otsinguväli + kohalikud raadiod kohe esimesena (enamasti kuulatakse oma
 * piirkonna jaamu). Kategooriate sirvimine on alles, aga lingi taga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TuneInBrowseSheet(
    presets: List<Preset>,
    strings: StringResources,
    onAddStation: (suspend (BrowseEntry) -> Boolean)?,
    onRemovePreset: (suspend (Preset) -> Boolean)?,
    onDismiss: () -> Unit,
    onBrowse: suspend (String?) -> List<BrowseEntry>,
    onSearch: (suspend (String) -> List<BrowseEntry>)?,
    onLocalRadio: (suspend () -> List<BrowseEntry>)?,
    onQuality: (suspend (String) -> StreamQuality?)?,
    onPlayEntry: suspend (BrowseEntry) -> Boolean,
    onPulseHaptic: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    // false = kohalikud raadiod (vaikimisi), true = kategooriate sirvimine
    var browsing by remember { mutableStateOf(onLocalRadio == null) }
    var path by remember { mutableStateOf(listOf(TuneInPathNode("TuneIn", null))) }
    var entries by remember { mutableStateOf<List<BrowseEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var playingTitle by remember { mutableStateOf<String?>(null) }
    // Jaamad, mida parajasti lemmikuks lisatakse (tärni asemel ring)
    var addingTitles by remember { mutableStateOf(setOf<String>()) }

    val currentKey = path.last().key
    val trimmedQuery = query.trim()
    val searching = onSearch != null && trimmedQuery.length >= 2

    LaunchedEffect(searching, trimmedQuery, browsing, currentKey) {
        failed = false
        if (searching) delay(400) // oota, kuni kirjutamine peatub
        loading = true
        try {
            entries = when {
                searching -> onSearch!!(trimmedQuery)
                !browsing && onLocalRadio != null -> onLocalRadio()
                else -> onBrowse(currentKey)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
            entries = emptyList()
        } finally {
            loading = false
        }
    }

    val openEntry: (BrowseEntry) -> Unit = { entry ->
        onPulseHaptic()
        if (entry.isBrowsable && !entry.isPlayable) {
            query = ""
            browsing = true
            path = path + TuneInPathNode(entry.title.ifBlank { "TuneIn" }, entry.browseKey)
        } else if (entry.isPlayable && playingTitle == null) {
            scope.launch {
                playingTitle = entry.title
                val ok = onPlayEntry(entry)
                playingTitle = null
                if (!ok) Toast.makeText(context, strings.playFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Tärn on lüliti: ☆ lisab lemmikuks, ★ eemaldab (sama lemmiku NAD-ist)
    val toggleStation: (BrowseEntry, Preset?) -> Unit = { entry, existing ->
        val action: (suspend () -> Boolean)? = when {
            existing != null && onRemovePreset != null -> { { onRemovePreset(existing) } }
            existing == null && onAddStation != null -> { { onAddStation(entry) } }
            else -> null
        }
        if (action != null && entry.title !in addingTitles) {
            onPulseHaptic()
            scope.launch {
                addingTitles = addingTitles + entry.title
                val ok = action()
                addingTitles = addingTitles - entry.title
                if (!ok) Toast.makeText(context, strings.presetActionFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!searching && browsing && (path.size > 1 || onLocalRadio != null)) {
                    IconButton(onClick = {
                        if (path.size > 1) path = path.dropLast(1) else browsing = false
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
                Text(
                    text = when {
                        searching -> strings.radio
                        browsing -> path.last().title.ifBlank { "TuneIn" }
                        else -> strings.localRadio
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            if (onSearch != null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(strings.searchStations) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Uus tulemus laeb, vana on veel näha: õhuke riba, et oleks näha, et midagi toimub
            if (loading && entries.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }

            when {
                loading && entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                failed -> Text(
                    text = strings.presetActionFailed,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                entries.isEmpty() -> Text(
                    text = strings.noResults,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        val stationUrl = presetUrlOf(entry)
                        // Tärn käib täpselt selle rea (kataloogi + jaama) kohta, URL-i järgi.
                        // Nii ei eemalda TuneIn-i rea tärn kogemata Airable'i lemmikut.
                        val existingPreset = stationUrl?.let { url ->
                            presets.firstOrNull { it.url == url || it.url.startsWith("$url/") }
                        }
                        StationRow(
                            entry = entry,
                            strings = strings,
                            canAdd = onAddStation != null && stationUrl != null,
                            isPreset = existingPreset != null,
                            isAdding = entry.title in addingTitles,
                            isStarting = playingTitle == entry.title,
                            onQuality = onQuality,
                            onClick = { openEntry(entry) },
                            onAdd = { toggleStation(entry, existingPreset) }
                        )
                    }

                    if (!searching && !browsing) {
                        item {
                            TextButton(
                                onClick = { browsing = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.browseAllCategories)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    entry: BrowseEntry,
    strings: StringResources,
    canAdd: Boolean,
    isPreset: Boolean,
    isAdding: Boolean,
    isStarting: Boolean,
    onQuality: (suspend (String) -> StreamQuality?)?,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    // Kvaliteet laetakse ainult nähtavate ridade kohta (LazyColumn) ja jäetakse meelde
    val stationId = presetUrlOf(entry)?.substringAfter(':', "").orEmpty()
    val quality by produceState<StreamQuality?>(initialValue = null, stationId) {
        value = if (stationId.startsWith("s") && onQuality != null) onQuality(stationId) else null
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (entry.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (entry.isPlayable) Icons.Default.Radio else Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.title.ifBlank { "TuneIn" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Kataloogi silt ("TuneIn" / "Airable") + kvaliteet, kui teada
                val source = presetUrlOf(entry)?.substringBefore(':')?.takeIf { it.isNotBlank() }
                if (source != null || quality != null) {
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        source?.let { QualityBadge(it) }
                        quality?.let { QualityBadge(it.label) }
                    }
                }
            }

            // ⭐ lisa jaam lemmikuks (täidetud tärn = juba lemmik)
            if (canAdd) {
                IconButton(onClick = onAdd, modifier = Modifier.size(40.dp)) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (isPreset) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isPreset) strings.remove else strings.addToPresets,
                            tint = if (isPreset) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (isStarting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (entry.isBrowsable && !entry.isPlayable) Icons.Default.ChevronRight else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// ALL PRESETS SHEET
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPresetsSheet(
    presets: List<Preset>,
    strings: StringResources,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                strings.presets,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Grid of presets - 4 per row
            val rows = presets.chunked(4)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { preset ->
                            PresetListItem(
                                preset = preset,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelect(preset.id)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty space if row is not complete
                        repeat(4 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// PRESET LIST ITEM (for bottom sheet)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun PresetListItem(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (preset.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = preset.imageUrl,
                    contentDescription = preset.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}



/** TuneIn jaama lemmiku-URL (nt "TuneIn:s25480") BluOS-i playUrl-ist; null, kui see pole jaam. */
private fun presetUrlOf(entry: BrowseEntry): String? {
    val play = entry.playUrl.ifBlank { entry.autoplayUrl }
    if (play.isBlank()) return null
    val full = if (play.startsWith("http", ignoreCase = true)) play else "http://nad$play"
    return runCatching { Uri.parse(full).getQueryParameter("url") }.getOrNull()?.takeIf { it.isNotBlank() }
}

/** Kogu kiirnuppude järjekord võtmetena (Spotify, Raadio, kõik lemmikud), nagu PresetSelector seda näitab. */
internal fun quickOrderKeys(presets: List<Preset>, saved: List<String>): List<String> =
    applyQuickButtonOrder(
        buildList {
            add(QuickButtonItem.Spotify)
            add(QuickButtonItem.TuneIn)
            presets.forEach { add(QuickButtonItem.PresetItem(it)) }
        },
        saved
    ).map { it.orderKey }

/**
 * Alamhulga (nt ainult raadio lemmikud) uus järjekord kirjutatakse üldisesse järjekorda
 * samadele kohtadele — teiste nuppude (Spotify jne) asukoht ei muutu.
 */
internal fun mergeSubsetOrder(full: List<String>, subsetNewOrder: List<String>): List<String> {
    val subset = subsetNewOrder.toSet()
    val next = subsetNewOrder.iterator()
    return full.map { key -> if (key in subset && next.hasNext()) next.next() else key }
}

/**
 * Raadio lemmikute haldus (Raadio playeri ✎ nupust): järjekord, ümbernimetamine,
 * eemaldamine ja uue jaama otsing. Sama leht mis lemmikutel, ainult raadiojaamadega.
 */
@Composable
fun RadioFavoritesSheet(
    radioPresets: List<Preset>,
    allPresets: List<Preset>,
    quickButtonOrder: List<String>,
    strings: StringResources,
    presetActions: PresetActions?,
    onOrderChange: (List<String>) -> Unit,
    onSelect: (Int) -> Unit,
    onSearchStations: () -> Unit,
    onDismiss: () -> Unit
) {
    QuickButtonOrderSheet(
        buttons = radioPresets.map { QuickButtonItem.PresetItem(it) },
        strings = strings,
        onDismiss = onDismiss,
        onOrderChange = { radioKeys ->
            onOrderChange(mergeSubsetOrder(quickOrderKeys(allPresets, quickButtonOrder), radioKeys))
        },
        onActivate = { item -> (item as? QuickButtonItem.PresetItem)?.let { onSelect(it.preset.id) } },
        // "Lisa praegu mängiv" siin ei näita — raadiojaamu lisatakse otsingu kaudu
        presetActions = presetActions?.let {
            PresetActions(
                canAddCurrent = false,
                onAddCurrent = it.onAddCurrent,
                onAddStation = it.onAddStation,
                onRename = it.onRename,
                onDelete = it.onDelete
            )
        },
        title = strings.radioFavorites,
        hint = strings.radioReorderHint,
        showTopBadges = false,
        onSearchStations = onSearchStations
    )
}
