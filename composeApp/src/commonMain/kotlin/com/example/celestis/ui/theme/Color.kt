package com.example.celestis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep Space Palette
private val DeepCharcoal = Color(0xFF0B0E14)
private val SurfaceContainerColor = Color(0xFF1A1C22)
private val AstroBlue = Color(0xFF8DCDFF)

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
    secondaryContainer = Color(0xFF364F60),
    onSecondaryContainer = Color(0xFFD1E5F6),

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
fun CelestisTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Hardcoded to always use dark theme
    val colorScheme = DarkColors
    
//    val celestisTypography = try {
//        Typography(
//            bodyLarge = TextStyle(
//                fontFamily = FontFamily(Font(Res.font.montserrat_medium)),
//                fontWeight = FontWeight.Normal,
//                fontSize = 16.sp,
//                lineHeight = 24.sp
//            )
//        )
//    } catch (e: Exception) {
//        // Fallback to default typography if font fails to load
//        Typography()
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Use default typography for now
        content = content
    )
}
