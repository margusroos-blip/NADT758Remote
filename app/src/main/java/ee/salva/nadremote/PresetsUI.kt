package com.nadremote.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
    quickButtonOrder: List<String> = emptyList(),
    onQuickButtonOrderChange: ((List<String>) -> Unit)? = null,
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
                    Box(
                        modifier = Modifier
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
                            QuickButtonItem.TuneIn -> TuneInButton()
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
            }
        )
    }

    if (showTuneInSheet && onBrowseTuneIn != null && onPlayBrowseEntry != null) {
        TuneInBrowseSheet(
            onDismiss = { showTuneInSheet = false },
            onBrowse = onBrowseTuneIn,
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
        tonalElevation = 0.dp
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
fun TuneInButton(size: Dp = 76.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Radio,
                contentDescription = "TuneIn",
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TuneIn",
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
        tonalElevation = 0.dp
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
        tonalElevation = 0.dp
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

private sealed class QuickButtonItem(val orderKey: String) {
    data object Spotify : QuickButtonItem("spotify")
    data object TuneIn : QuickButtonItem("tunein")
    data class PresetItem(val preset: Preset) : QuickButtonItem("preset:${preset.id}")
}

private fun applyQuickButtonOrder(
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
    size: Dp = 76.dp
) {
    when (button) {
        QuickButtonItem.Spotify -> SpotifyButton(size = size)
        QuickButtonItem.TuneIn -> TuneInButton(size = size)
        is QuickButtonItem.PresetItem -> PresetButton(preset = button.preset, size = size)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickButtonOrderSheet(
    buttons: List<QuickButtonItem>,
    strings: StringResources,
    onDismiss: () -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onActivate: (QuickButtonItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var ordered by remember(buttons) { mutableStateOf(buttons) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val cellStepPx = with(LocalDensity.current) { 92.dp.toPx() }
    val swapThreshold = cellStepPx * 0.55f
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
            Text(
                text = strings.presets,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Long press and drag icons. Top 3 appear on main screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val rows = ordered.chunked(columns)
                rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { colIndex, button ->
                            val index = rowIndex * columns + colIndex
                            key(button.orderKey) {
                                val currentIndex by rememberUpdatedState(index)
                                val currentButton by rememberUpdatedState(button)
                                Box(
                                    modifier = Modifier
                                        .zIndex(if (draggingIndex == currentIndex) 1f else 0f)
                                        .offset {
                                            IntOffset(
                                                x = if (draggingIndex == currentIndex) dragOffsetX.roundToInt() else 0,
                                                y = if (draggingIndex == currentIndex) dragOffsetY.roundToInt() else 0
                                            )
                                        }
                                        .graphicsLayer {
                                            if (draggingIndex == currentIndex) {
                                                scaleX = 1.03f
                                                scaleY = 1.03f
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    if (ordered.size < 2) return@detectDragGesturesAfterLongPress
                                                    draggingIndex = currentIndex
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragEnd = {
                                                    draggingIndex = -1
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingIndex = -1
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (ordered.size < 2) return@detectDragGesturesAfterLongPress
                                                    val current = draggingIndex
                                                    if (current !in ordered.indices) return@detectDragGesturesAfterLongPress

                                                    dragOffsetX += dragAmount.x
                                                    dragOffsetY += dragAmount.y
                                                    var target = current

                                                    if (abs(dragOffsetX) >= abs(dragOffsetY)) {
                                                        if (dragOffsetX > swapThreshold &&
                                                            current % columns < columns - 1 &&
                                                            current + 1 <= ordered.lastIndex
                                                        ) {
                                                            target = current + 1
                                                            dragOffsetX -= cellStepPx
                                                        } else if (dragOffsetX < -swapThreshold &&
                                                            current % columns > 0
                                                        ) {
                                                            target = current - 1
                                                            dragOffsetX += cellStepPx
                                                        }
                                                    } else {
                                                        if (dragOffsetY > swapThreshold &&
                                                            current + columns <= ordered.lastIndex
                                                        ) {
                                                            target = current + columns
                                                            dragOffsetY -= cellStepPx
                                                        } else if (dragOffsetY < -swapThreshold &&
                                                            current - columns >= 0
                                                        ) {
                                                            target = current - columns
                                                            dragOffsetY += cellStepPx
                                                        }
                                                    }

                                                    if (target != current) {
                                                        ordered = ordered.toMutableList().apply {
                                                            add(target, removeAt(current))
                                                        }
                                                        draggingIndex = target
                                                        onOrderChange(ordered.map { it.orderKey })
                                                    }
                                                    change.consume()
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (draggingIndex != -1) return@detectTapGestures
                                                    onActivate(currentButton)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                ) {
                                    QuickButtonVisual(button = button, size = 84.dp)
                                    if (index < 3) {
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
        }
    }
}

private data class TuneInPathNode(
    val title: String,
    val key: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuneInBrowseSheet(
    onDismiss: () -> Unit,
    onBrowse: suspend (String?) -> List<BrowseEntry>,
    onPlayEntry: suspend (BrowseEntry) -> Boolean,
    onPulseHaptic: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var path by remember { mutableStateOf(listOf(TuneInPathNode("TuneIn", null))) }
    var entries by remember { mutableStateOf<List<BrowseEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentKey = path.last().key

    LaunchedEffect(currentKey) {
        loading = true
        error = null
        entries = try {
            onBrowse(currentKey)
        } catch (e: Exception) {
            error = e.message ?: "Browse error"
            emptyList()
        } finally {
            loading = false
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (path.size > 1) {
                        IconButton(
                            onClick = {
                                path = path.dropLast(1)
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                    Text(
                        text = path.last().title.ifBlank { "TuneIn" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                entries.isEmpty() -> {
                    Text(
                        text = "No items found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries) { entry ->
                            Surface(
                                onClick = {
                                    onPulseHaptic()
                                    if (entry.isBrowsable && !entry.isPlayable) {
                                        path = path + TuneInPathNode(
                                            title = entry.title.ifBlank { "Browse" },
                                            key = entry.browseKey
                                        )
                                    } else if (entry.isPlayable && !playing) {
                                        scope.launch {
                                            playing = true
                                            val ok = onPlayEntry(entry)
                                            if (!ok) {
                                                error = "Ei saanud jaama mängima panna"
                                            }
                                            playing = false
                                        }
                                    } else if (entry.isBrowsable) {
                                        path = path + TuneInPathNode(
                                            title = entry.title.ifBlank { "Browse" },
                                            key = entry.browseKey
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = entry.title.ifBlank { "Untitled" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
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
                                    }

                                    if (playing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when {
                                                entry.isBrowsable && !entry.isPlayable -> Icons.Default.ChevronRight
                                                entry.isPlayable -> Icons.Default.PlayArrow
                                                else -> Icons.Default.Radio
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
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


