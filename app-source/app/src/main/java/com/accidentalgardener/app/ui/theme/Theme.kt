package com.accidentalgardener.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Garden palette
val Moss = Color(0xFF3F5E2F)
val Leaf = Color(0xFF6E9B4A)
val Sprout = Color(0xFFC6E0A0)
val Terracotta = Color(0xFFC0623C)
val Clay = Color(0xFFF6D3BE)
val Soil = Color(0xFF4E3A2C)
val SoilLight = Color(0xFF7A5C46)
val Bark = Color(0xFF8A6240)
val Cream = Color(0xFFF7F1E3)
val Parchment = Color(0xFFEFE5CE)
val Sky = Color(0xFF7FB2C9)
val SkyLight = Color(0xFFD7EAF2)
val Sun = Color(0xFFF2C14E)
val Butter = Color(0xFFFBEFC8)
val Ink = Color(0xFF2B2A22)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = Sprout,
    onPrimaryContainer = Color(0xFF1C3010),
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = Clay,
    onSecondaryContainer = Color(0xFF4A1E0C),
    tertiary = Color(0xFF3D7A95),
    onTertiary = Color.White,
    tertiaryContainer = SkyLight,
    onTertiaryContainer = Color(0xFF0E3242),
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Parchment,
    onSurfaceVariant = Color(0xFF5B5546),
    outline = Color(0xFFB7AA8E),
    outlineVariant = Color(0xFFDCD0B5),
    surfaceContainerLowest = Color(0xFFFFFCF5),
    surfaceContainerLow = Color(0xFFF9F4E8),
    surfaceContainer = Color(0xFFF2EAD7),
    surfaceContainerHigh = Color(0xFFECE2CB),
    surfaceContainerHighest = Color(0xFFE6DABF),
)

private val DarkColors = darkColorScheme(
    primary = Sprout,
    onPrimary = Color(0xFF1C3010),
    primaryContainer = Moss,
    onPrimaryContainer = Sprout,
    secondary = Color(0xFFF0A47F),
    onSecondary = Color(0xFF4A1E0C),
    secondaryContainer = Color(0xFF7A3A20),
    onSecondaryContainer = Clay,
    tertiary = Sky,
    onTertiary = Color(0xFF0E3242),
    tertiaryContainer = Color(0xFF24576C),
    onTertiaryContainer = SkyLight,
    background = Color(0xFF1C1B16),
    onBackground = Color(0xFFEDE6D6),
    surface = Color(0xFF1C1B16),
    onSurface = Color(0xFFEDE6D6),
    surfaceVariant = Color(0xFF3A372D),
    onSurfaceVariant = Color(0xFFCFC6B2),
    outline = Color(0xFF8E8571),
    outlineVariant = Color(0xFF4A463A),
    surfaceContainerLowest = Color(0xFF161511),
    surfaceContainerLow = Color(0xFF22211B),
    surfaceContainer = Color(0xFF27251F),
    surfaceContainerHigh = Color(0xFF312F28),
    surfaceContainerHighest = Color(0xFF3B3932),
)

private val Heading = FontFamily.Serif

private val base = Typography()
val GardenTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun AccidentalGardenerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = GardenTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
