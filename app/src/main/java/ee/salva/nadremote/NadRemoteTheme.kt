package com.nadremote.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
// Oma kindel palett: sügav süsihall + üks summutatud merevaigu-aktsent
// (NAD-i esipaneeli VFD-ekraani toon). Dynamic color (taustapildi värvid) on
// teadlikult välja lülitatud, et äpp näeks igas telefonis ühesugune välja.
// Aktsendi vahetamiseks piisab Amber* väärtuste muutmisest.
// ═══════════════════════════════════════════════════════════════════════════

private val AmberDark = Color(0xFFE0A85A)
private val AmberLight = Color(0xFF9A6424)

private val DarkColors = darkColorScheme(
    primary = AmberDark,
    onPrimary = Color(0xFF1E1407),
    primaryContainer = Color(0xFF3A2D1B),
    onPrimaryContainer = Color(0xFFF4D9AE),
    inversePrimary = AmberLight,
    secondary = Color(0xFFC9B8A0),
    onSecondary = Color(0xFF1E1A14),
    secondaryContainer = Color(0xFF2A2A2E),
    onSecondaryContainer = Color(0xFFE6E4E0),
    tertiary = Color(0xFF9FB4C7),
    onTertiary = Color(0xFF0F1A24),
    background = Color(0xFF0F1012),
    onBackground = Color(0xFFECEBE8),
    surface = Color(0xFF141518),
    onSurface = Color(0xFFECEBE8),
    surfaceVariant = Color(0xFF222327),
    onSurfaceVariant = Color(0xFFA4A3A8),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF45464B),
    outlineVariant = Color(0xFF2E2F33),
    error = Color(0xFFE5645B),
    onError = Color(0xFF200806),
    errorContainer = Color(0xFF3B1714),
    onErrorContainer = Color(0xFFF6C9C4),
    surfaceBright = Color(0xFF2B2C30),
    surfaceDim = Color(0xFF0F1012),
    surfaceContainerLowest = Color(0xFF0B0C0E),
    surfaceContainerLow = Color(0xFF17181B),
    surfaceContainer = Color(0xFF1B1C1F),
    surfaceContainerHigh = Color(0xFF202124),
    surfaceContainerHighest = Color(0xFF26272B)
)

private val LightColors = lightColorScheme(
    primary = AmberLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E7D2),
    onPrimaryContainer = Color(0xFF3A2610),
    inversePrimary = AmberDark,
    secondary = Color(0xFF6E6254),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E6E1),
    onSecondaryContainer = Color(0xFF1F1D1A),
    tertiary = Color(0xFF4F6577),
    onTertiary = Color.White,
    background = Color(0xFFF7F6F3),
    onBackground = Color(0xFF1B1B1D),
    surface = Color(0xFFFCFBF9),
    onSurface = Color(0xFF1B1B1D),
    surfaceVariant = Color(0xFFEDEBE7),
    onSurfaceVariant = Color(0xFF5E5D61),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFB9B7B2),
    outlineVariant = Color(0xFFDCDAD5),
    error = Color(0xFFC0392F),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDA),
    onErrorContainer = Color(0xFF410E0A),
    surfaceBright = Color(0xFFFCFBF9),
    surfaceDim = Color(0xFFE3E1DD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F6F3),
    surfaceContainer = Color(0xFFF2F1EE),
    surfaceContainerHigh = Color(0xFFECEAE7),
    surfaceContainerHighest = Color(0xFFE6E4E1)
)

@Composable
fun NadRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
