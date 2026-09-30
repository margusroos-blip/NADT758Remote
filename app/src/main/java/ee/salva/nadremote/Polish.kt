package com.nadremote.app

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// UI viimistlus: ühised animatsioonid, ääred ja värvid.
// Paigutust need ei muuda — ainult seda, kuidas olemasolevad elemendid tunduvad.
// ═══════════════════════════════════════════════════════════════════════════

/** Olekumuutuste (sees/väljas, valitud) ühine kestus. */
const val STATE_ANIM_MS = 250

fun <T> stateTween() = tween<T>(durationMillis = STATE_ANIM_MS)

/**
 * Nupp vajub vajutusel korraks ~96% peale ja vetrub tagasi.
 * graphicsLayer ei mõjuta paigutust.
 */
fun Modifier.pressScale(isPressed: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    pressScale(isPressed)
}

/** Peen 1 dp hele serv — tumeda UI sügavus ilma varjudeta. */
@Composable
fun hairlineBorder(alpha: Float = 0.08f): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))

/**
 * Mängiva loo kaanepildi põhivärv (Palette). null, kui pilti pole või seda ei saa lugeda.
 * Pilt tuleb Coili cache'ist, seega lisavõrgupäringut enamasti ei tehta.
 */
@Composable
fun rememberArtworkColor(imageUrl: String): Color? {
    val context = LocalContext.current
    val state = produceState<Color?>(initialValue = null, imageUrl) {
        if (imageUrl.isBlank()) {
            value = null
            return@produceState
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Palette vajab tarkvaralist bitmapi
            .size(96)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        value = bitmap?.let {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(it).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                swatch?.let { s -> Color(s.rgb) }
            }
        }
    }
    return state.value
}

/** Lihtne vajutuse olek kohtadele, kus kasutatakse detectTapGestures'i (onPress). */
class PressState {
    var isPressed by mutableStateOf(false)
}

@Composable
fun rememberPressState() = remember { PressState() }
