package com.gitofy.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** The six M3 palettes the web app shipped (lightThemes / darkThemes). */
enum class Palette(val label: String) {
    PINK("pink"), EMERALD("emerald"), INDIGO("indigo"),
    VIOLET("violet"), CRIMSON("crimson"), CYAN("cyan");

    companion object {
        fun fromId(id: String?): Palette =
            entries.firstOrNull { it.label == id } ?: CRIMSON   // defaultSettings.palette
    }
}

/** Non-standard diff colours (added/modified/deleted), exposed via CompositionLocal. */
val LocalDiffColors = staticCompositionLocalOf { PinkDiff }

/** True when the user asked for reduced motion — content stagger is skipped. */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun GitofyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: Palette = Palette.CRIMSON,
    reduceMotion: Boolean = false,
    dynamicColor: Boolean = false,
    backgroundHex: String = "",
    fontFamily: String = "system",
    fontWeight: String = "normal",
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        when (palette) {
            Palette.PINK -> if (darkTheme) pinkDark else pinkLight
            Palette.EMERALD -> if (darkTheme) emeraldDark else emeraldLight
            Palette.INDIGO -> if (darkTheme) indigoDark else indigoLight
            Palette.VIOLET -> if (darkTheme) violetDark else violetLight
            Palette.CRIMSON -> if (darkTheme) crimsonDark else crimsonLight
            Palette.CYAN -> if (darkTheme) cyanDark else cyanLight
        }
    }
    // Optional custom canvas colour — overrides the scheme's background + surface.
    val tint = if (backgroundHex.isNotBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(backgroundHex)) }.getOrNull()
    } else null
    val scheme = if (tint != null) base.copy(background = tint, surface = tint) else base
    val diff = when (palette) {
        Palette.PINK -> PinkDiff
        Palette.EMERALD -> EmeraldDiff
        Palette.INDIGO -> IndigoDiff
        Palette.VIOLET -> VioletDiff
        Palette.CRIMSON -> CrimsonDiff
        Palette.CYAN -> CyanDiff
    }
    CompositionLocalProvider(
        LocalDiffColors provides diff,
        LocalReducedMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = gitofyTypography(fontFamily, fontWeight, fontScale),
            shapes = GitofyShapes,
            content = content,
        )
    }
}
