package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HayatiLightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavyMid,
    onPrimaryContainer = Color.White,
    secondary = TurquoisePrimary,
    onSecondary = Color.White,
    secondaryContainer = TurquoiseSoft,
    onSecondaryContainer = NavyDeep,
    tertiary = GoldAccent,
    onTertiary = NavyDeep,
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = NavyDeep,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError,
)

private val HayatiDarkColorScheme = darkColorScheme(
    primary = TurquoiseBright,
    onPrimary = NavyDeep,
    primaryContainer = NavyLight,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TurquoisePrimary,
    onSecondary = Color.White,
    secondaryContainer = TurquoiseDarkContainer,
    onSecondaryContainer = TurquoiseSoft,
    tertiary = GoldAccent,
    onTertiary = NavyDeep,
    tertiaryContainer = GoldDarkContainer,
    onTertiaryContainer = GoldSoft,
    background = NavySurfaceDark,
    onBackground = TextPrimaryDark,
    surface = NavyCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCardElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFF87171),
    onError = NavyDeep,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
)

val HayatiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HayatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) HayatiDarkColorScheme else HayatiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HayatiTypography,
        shapes = HayatiShapes,
        content = content,
    )
}
