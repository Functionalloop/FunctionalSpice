package com.functional.spice.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light mode only: no dynamic color and no dark scheme, so a Samsung phone in dark mode
 * still shows the warm off-white UI (android:forceDarkAllowed=false in themes.xml too).
 */
private val LightColorScheme = lightColorScheme(
    primary = DeepEspresso,
    onPrimary = WarmAlabaster,
    primaryContainer = SoftCream,
    onPrimaryContainer = DeepEspresso,
    secondary = WarmCardamom,
    onSecondary = Color.White,
    secondaryContainer = SelectedCard,
    onSecondaryContainer = DeepEspresso,
    tertiary = WarmCardamom,
    onTertiary = Color.White,
    background = WarmAlabaster,
    onBackground = Charcoal,
    surface = WarmAlabaster,
    onSurface = Charcoal,
    surfaceVariant = SoftCream,
    onSurfaceVariant = MutedStone,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmAlabaster,
    surfaceContainer = WarmAlabaster,
    surfaceContainerHigh = SoftCream,
    surfaceContainerHighest = SoftCream,
    outline = SoftCreamBorder,
    outlineVariant = SoftCreamBorder,
    error = Danger,
    onError = Color.White,
    scrim = Charcoal
)

@Composable
fun FunctionalSpiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = SpiceTypography,
        content = content
    )
}
