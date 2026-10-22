package com.workplanner.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Olive700,
    onPrimary = Paper,
    primaryContainer = Sage200,
    onPrimaryContainer = Olive900,
    secondary = Olive500,
    onSecondary = Paper,
    secondaryContainer = Sage100,
    onSecondaryContainer = Olive800,
    tertiary = Clay,
    onTertiary = Paper,
    tertiaryContainer = ColorTokens.clayContainer,
    onTertiaryContainer = ColorTokens.onClayContainer,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Linen,
    onSurfaceVariant = InkSoft,
    surfaceTint = Olive600,
    outline = OutlineSoft,
    outlineVariant = ColorTokens.outlineVariant,
    error = ColorTokens.error,
    onError = Paper,
    errorContainer = ColorTokens.errorContainer,
    onErrorContainer = ColorTokens.onErrorContainer,
    inverseSurface = Olive900,
    inverseOnSurface = Cream,
    scrim = ColorTokens.scrim
)

private val DarkColors = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = Olive800,
    onPrimaryContainer = Sage200,
    secondary = Leaf400,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightSurfaceHigh,
    onSecondaryContainer = Sage200,
    tertiary = ColorTokens.clayDark,
    onTertiary = NightOnPrimary,
    tertiaryContainer = ColorTokens.clayDarkContainer,
    onTertiaryContainer = ColorTokens.clayDarkOnContainer,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextSoft,
    surfaceTint = NightPrimary,
    outline = NightOutline,
    outlineVariant = ColorTokens.nightOutlineVariant,
    error = ColorTokens.errorDark,
    onError = NightOnPrimary,
    errorContainer = ColorTokens.errorDarkContainer,
    onErrorContainer = ColorTokens.errorDarkOnContainer,
    inverseSurface = NightText,
    inverseOnSurface = NightBg,
    scrim = ColorTokens.scrim
)

private val WorkPlannerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/** A few colours that Material's scheme does not carry on its own. */
object ColorTokens {
    val clayContainer = androidx.compose.ui.graphics.Color(0xFFF4DFD4)
    val onClayContainer = androidx.compose.ui.graphics.Color(0xFF4A2010)
    val clayDark = androidx.compose.ui.graphics.Color(0xFFE9A488)
    val clayDarkContainer = androidx.compose.ui.graphics.Color(0xFF5A2C1B)
    val clayDarkOnContainer = androidx.compose.ui.graphics.Color(0xFFFFDBCD)
    val outlineVariant = androidx.compose.ui.graphics.Color(0xFFDDD8C6)
    val error = androidx.compose.ui.graphics.Color(0xFFB3261E)
    val errorContainer = androidx.compose.ui.graphics.Color(0xFFF9DEDC)
    val onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410E0B)
    val errorDark = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
    val errorDarkContainer = androidx.compose.ui.graphics.Color(0xFF93000A)
    val errorDarkOnContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
    val nightOutlineVariant = androidx.compose.ui.graphics.Color(0xFF333A27)
    val scrim = androidx.compose.ui.graphics.Color(0xFF000000)
}

@Composable
fun WorkPlannerTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WorkPlannerTypography,
        shapes = WorkPlannerShapes,
        content = content
    )
}
