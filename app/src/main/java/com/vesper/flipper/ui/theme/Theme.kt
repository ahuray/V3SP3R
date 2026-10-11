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
// VESPER BRAND — Terminal / Phosphor Hacker
// ═══════════════════════════════════════════════════════════
// Signature: phosphor-green glow on deep terminal black, CRT amber for
// attention, muted cyan-steel panels. The historic Vesper* names are kept so
// the whole app re-skins by repointing these values; comments note each role.

// Primary: Terminal Green — deep emerald fill, phosphor glow for accents.
// Deep enough that white text/icons on it stay legible (~4.9:1), since the
// accent is used as a fill behind white content across many screens.
val VesperWine = Color(0xFF0E7D47)           // Deep emerald (primary fill)
val VesperWineLight = Color(0xFF35E07A)      // Phosphor green for glow highlights
val VesperWineDark = Color(0xFF084F2D)       // Darker green for pressed states

// Legacy alias — keeps existing references working
val VesperOrange = VesperWine
val VesperOrangeDark = VesperWineDark

// Secondary: Cyan Steel — cold data accent
val VesperSecondary = Color(0xFF24312B)      // Lifted panel, green-tinted
val VesperGunmetal = Color(0xFF2E3D35)       // Lifted panel
val VesperCyan = Color(0xFF34E7E4)           // Data cyan for secondary accents

// Accent: CRT Amber — attention / warning glow
val VesperAccent = Color(0xFFF4B740)         // CRT amber
val VesperGold = Color(0xFFF4B740)
val VesperGoldMuted = Color(0xFFB7891F)      // Muted amber for secondary accents

// Surface Colors — lifted terminal charcoal so body text stays readable on a phone.
val VesperSurface = Color(0xFF24312B)
val VesperSurfaceVariant = Color(0xFF2E3D35)
val VesperBackground = Color(0xFF1A2621)
val VesperBackgroundDeep = Color(0xFF14201C)
val VesperBackgroundGlow = Color(0xFF1E3329)

val VesperBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        VesperBackground,
        VesperBackgroundDeep,
        VesperBackgroundGlow,
        VesperBackground
    )
)

// Risk Colors — terminal-coded
val RiskLow = Color(0xFF2DE37E)      // Phosphor green — clear
val RiskMedium = Color(0xFFF4B740)   // Amber — caution
val RiskHigh = Color(0xFFFF5C66)     // Alert red
val RiskBlocked = Color(0xFF5E6E66)  // Muted green-gray

// Diff Colors
val DiffAdded = Color(0xFF2DE37E)
val DiffRemoved = Color(0xFFFF5C66)
val DiffChanged = Color(0xFFF4B740)
val DiffAddedBackground = Color(0x332DE37E)
val DiffRemovedBackground = Color(0x33FF5C66)

// Chat Colors — distinct from system
val ChatAssistant = Color(0xFF2E3D35)       // Panel bubble
val ChatUser = VesperWineDark                // Deep-green bubble for user
val ChatTool = Color(0xFF24312B)             // Tool results
val ChatToolAccent = VesperGold              // Amber for tool highlights

private val DarkColorScheme = darkColorScheme(
    primary = VesperWine,
    onPrimary = Color(0xFFEAF6EF),
    primaryContainer = VesperWineDark,
    onPrimaryContainer = Color(0xFFBFF4D7),
    secondary = VesperCyan,
    onSecondary = Color(0xFF04140B),
    secondaryContainer = VesperSurfaceVariant,
    onSecondaryContainer = Color(0xFFCDE9E6),
    tertiary = VesperGold,
    onTertiary = Color(0xFF1A1300),
    background = VesperBackground,
    onBackground = Color(0xFFF4FBF7),
    surface = VesperSurface,
    onSurface = Color(0xFFF4FBF7),
    surfaceVariant = VesperSurfaceVariant,
    onSurfaceVariant = Color(0xFFD5E6DC),
    outline = Color(0xFF7FA08F),
    outlineVariant = Color(0xFF4E675C),
    error = RiskHigh,
    onError = Color(0xFF200909)
)

private val LightColorScheme = lightColorScheme(
    primary = VesperWineDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F3DB),
    onPrimaryContainer = Color(0xFF04341F),
    secondary = Color(0xFF0E7F7C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7EEEC),
    onSecondaryContainer = Color(0xFF04312F),
    tertiary = VesperGoldMuted,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F2),
    onBackground = Color(0xFF101713),
    surface = Color.White,
    onSurface = Color(0xFF101713),
    surfaceVariant = Color(0xFFE2EBE5),
    onSurfaceVariant = Color(0xFF46524B),
    outline = Color(0xFF6C786F),
    outlineVariant = Color(0xFFBECABB),
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
