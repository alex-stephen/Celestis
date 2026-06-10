package com.alexstephen.celestis80085.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import celestis.composeapp.generated.resources.Exo2_Bold
import celestis.composeapp.generated.resources.Exo2_ExtraBold
import celestis.composeapp.generated.resources.Exo2_Italic
import celestis.composeapp.generated.resources.Exo2_Light
import celestis.composeapp.generated.resources.Exo2_Medium
import celestis.composeapp.generated.resources.Exo2_Regular
import celestis.composeapp.generated.resources.Exo2_SemiBold
import celestis.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

// Deep Space Palette
private val DeepCharcoal = Color(0xFF0B0E14)
private val SurfaceContainerColor = Color(0xFF1A1C22)
private val AstroBlue = Color(0xFF85FFD9)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006494),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF50606E),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF191C1E)
)

private val DarkColors = darkColorScheme(
    primary = AstroBlue,
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68),
    onPrimaryContainer = Color(0xFFC5E7FF),

    secondary = Color(0xFFB5C9DA),
    onSecondary = Color(0xFF203849),
    secondaryContainer = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF000000),

    surface = DeepCharcoal,
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF40484F),
    onSurfaceVariant = Color(0xFFC0C8CF),
    surfaceContainer = SurfaceContainerColor,

    background = DeepCharcoal,
    onBackground = Color(0xFFE1E2E5),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
private fun exo2FontFamily() = FontFamily(
    Font(Res.font.Exo2_Light, FontWeight.Light),
    Font(Res.font.Exo2_Regular, FontWeight.Normal),
    Font(Res.font.Exo2_Italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.Exo2_Medium, FontWeight.Medium),
    Font(Res.font.Exo2_SemiBold, FontWeight.SemiBold),
    Font(Res.font.Exo2_Bold, FontWeight.Bold),
    Font(Res.font.Exo2_ExtraBold, FontWeight.ExtraBold)
)

@Composable
private fun celestisTypography(): Typography {
    val exo2 = exo2FontFamily()
    val defaults = Typography()

    return Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = exo2),
        displayMedium = defaults.displayMedium.copy(fontFamily = exo2),
        displaySmall = defaults.displaySmall.copy(fontFamily = exo2),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = exo2),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = exo2),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = exo2),
        titleLarge = defaults.titleLarge.copy(fontFamily = exo2),
        titleMedium = defaults.titleMedium.copy(fontFamily = exo2),
        titleSmall = defaults.titleSmall.copy(fontFamily = exo2),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = exo2),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = exo2),
        bodySmall = defaults.bodySmall.copy(fontFamily = exo2),
        labelLarge = defaults.labelLarge.copy(fontFamily = exo2),
        labelMedium = defaults.labelMedium.copy(fontFamily = exo2),
        labelSmall = defaults.labelSmall.copy(fontFamily = exo2)
    )
}

@Composable
fun CelestisTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Hardcoded to always use dark theme
    val colorScheme = DarkColors
    val typography = celestisTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
