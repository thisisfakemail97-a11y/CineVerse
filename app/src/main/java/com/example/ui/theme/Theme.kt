package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode(val displayName: String, val subtitle: String) {
    LIQUID_GLASS(
        "Liquid Glass",
        "True optical glass with contour edge glow, specular lens refraction & multi-color breathing aura"
    ),
    PRISM_GLASS(
        "Prism Glass",
        "Chromatic dispersion, rainbow caustic edge refraction & crystal lens flare"
    ),
    BIOLUMINESCENT_NEON(
        "Bioluminescent Neon",
        "Pulsating electric contour auroras, glowing glass rims & deep ocean luminescence"
    ),
    SMOKY_OBSIDIAN(
        "Smoky Frosted Obsidian",
        "Satin smoked obsidian glass with sharp diamond specular rim & subtle carbon glow"
    )
}

private val CineverseColorScheme =
    darkColorScheme(
        primary = ElectricCyan,
        onPrimary = DeepBlack,
        secondary = IcyBlue,
        onSecondary = DeepBlack,
        background = DeepBlack,
        onBackground = FrostWhite,
        surface = MidnightNavy,
        onSurface = FrostWhite,
        surfaceVariant = GlassSurfaceDark,
        onSurfaceVariant = SteelBlue,
        outline = GlassBorder
    )

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.LIQUID_GLASS }
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

@Composable
fun CineverseTheme(
    themeMode: ThemeMode = ThemeMode.LIQUID_GLASS,
    liquidGlassEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalLiquidGlassEnabled provides true
    ) {
        MaterialTheme(
            colorScheme = CineverseColorScheme,
            typography = Typography,
            content = content
        )
    }
}
