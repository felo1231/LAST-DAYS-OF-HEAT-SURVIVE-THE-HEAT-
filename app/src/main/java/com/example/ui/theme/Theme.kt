package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FrostholdColorScheme = darkColorScheme(
    primary = WarmGold,
    onPrimary = SlateAbyss,
    primaryContainer = SlateElevated,
    onPrimaryContainer = PapyrusCream,
    secondary = IceCyan,
    onSecondary = SlateAbyss,
    secondaryContainer = SlateCard,
    onSecondaryContainer = IceGlow,
    tertiary = NileTurquoise,
    onTertiary = SlateAbyss,
    background = SlateAbyss,
    onBackground = FrostWhite,
    surface = SlateSurface,
    onSurface = FrostWhite,
    surfaceVariant = SlateCard,
    onSurfaceVariant = MistText,
    outline = SlateBorder,
    error = CrimsonAlert,
    onError = SlateAbyss
)

private val FrostholdShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun FrostholdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrostholdColorScheme,
        typography = FrostholdTypography,
        shapes = FrostholdShapes,
        content = content
    )
}
