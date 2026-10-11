package com.vesper.flipper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════
// VESPER BRAND — Omarchy Tokyo Night
// ═══════════════════════════════════════════════════════════
// Colors follow the Omarchy Tokyo Night theme
// (/usr/share/omarchy/themes/tokyo-night/colors.toml).
// The historic Vesper* names are kept so the whole app re-skins by
// repointing these values. Primary is the theme's blue accent. Text on
// that fill is the night background, which stays readable on the blue.

// Primary: Tokyo Night blue. Dark text on this fill, not white.
val VesperWine = Color(0xFF7AA2F7)           // Blue accent (primary fill)
val VesperWineLight = Color(0xFF7DCFFF)      // Bright cyan for glow highlights
val VesperWineDark = Color(0xFF3D59A1)       // Deeper blue for pressed states

// Legacy alias — keeps existing references working
val VesperOrange = VesperWine
val VesperOrangeDark = VesperWineDark

// Secondary: night panels and the theme cyan
val VesperSecondary = Color(0xFF24283B)      // Lighter background
val VesperGunmetal = Color(0xFF292E42)       // Selection
val VesperCyan = Color(0xFF7DCFFF)           // Bright cyan

// Accent: Tokyo Night yellow for attention, magenta for the attack kit
val VesperAccent = Color(0xFFE0AF68)         // Yellow
val VesperGold = Color(0xFFE0AF68)
val VesperGoldMuted = Color(0xFF9A7340)
val VesperMagenta = Color(0xFFBB9AF7)

// Surface Colors — Tokyo Night backgrounds.
val VesperSurface = Color(0xFF24283B)
val VesperSurfaceVariant = Color(0xFF292E42)
val VesperBackground = Color(0xFF1A1B26)
val VesperBackgroundDeep = Color(0xFF13141C)
val VesperBackgroundGlow = Color(0xFF24283B)

val VesperBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        VesperBackground,
        VesperBackgroundDeep,
        VesperBackgroundGlow,
        VesperBackground
    )
)

/** Page wash that follows the active scheme, so light and dark tabs share one canvas. */
@Composable
fun vesperPageBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        colors = listOf(scheme.background, scheme.surface, scheme.background)
    )
}

// Risk Colors — Tokyo Night
val RiskLow = Color(0xFF9ECE6A)      // Green — clear
val RiskMedium = Color(0xFFE0AF68)   // Yellow — caution
val RiskHigh = Color(0xFFF7768E)     // Red
val RiskBlocked = Color(0xFF565F89)  // Dark foreground

// Diff Colors
val DiffAdded = Color(0xFF9ECE6A)
val DiffRemoved = Color(0xFFF7768E)
val DiffChanged = Color(0xFFE0AF68)
val DiffAddedBackground = Color(0x339ECE6A)
val DiffRemovedBackground = Color(0x33F7768E)

// Chat Colors — night panels
val ChatAssistant = Color(0xFF292E42)
val ChatUser = VesperWineDark
val ChatTool = Color(0xFF24283B)
val ChatToolAccent = VesperGold

private val DarkColorScheme = darkColorScheme(
    primary = VesperWine,
    onPrimary = Color(0xFF1A1B26),
    primaryContainer = VesperWineDark,
    onPrimaryContainer = Color(0xFFC0CAF5),
    secondary = VesperCyan,
    onSecondary = Color(0xFF1A1B26),
    secondaryContainer = VesperSurfaceVariant,
    onSecondaryContainer = Color(0xFFC0CAF5),
    tertiary = VesperGold,
    onTertiary = Color(0xFF1A1B26),
    background = VesperBackground,
    onBackground = Color(0xFFC0CAF5),
    surface = VesperSurface,
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = VesperSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B1D6),
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF414868),
    error = RiskHigh,
    onError = Color(0xFF200909)
)

private val LightColorScheme = lightColorScheme(
    primary = VesperWineDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5DEF8),
    onPrimaryContainer = Color(0xFF1A1B26),
    secondary = Color(0xFF3D5A80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E2F2),
    onSecondaryContainer = Color(0xFF1A1B26),
    tertiary = VesperGoldMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E4C8),
    onTertiaryContainer = Color(0xFF1A1B26),
    background = Color(0xFFE6E9F5),
    onBackground = Color(0xFF1A1B26),
    surface = Color(0xFFF4F6FC),
    onSurface = Color(0xFF1A1B26),
    surfaceVariant = Color(0xFFD9DEEF),
    onSurfaceVariant = Color(0xFF414868),
    outline = Color(0xFF7A82A0),
    outlineVariant = Color(0xFFC5CAE0),
    error = RiskHigh,
    onError = Color.White
)

// Sharp, boxy corners for a crisp terminal-panel feel.
private val VesperShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

private val BaseTypography = androidx.compose.material3.Typography()

val VesperTypography = BaseTypography.copy(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = 2.0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 1.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.8.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun VesperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VesperTypography,
        shapes = VesperShapes,
        content = content
    )
}
