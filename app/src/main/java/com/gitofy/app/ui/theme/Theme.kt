package com.gitofy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

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

@Composable
fun GitofyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: Palette = Palette.CRIMSON,
    content: @Composable () -> Unit,
) {
    val scheme = when (palette) {
        Palette.PINK -> if (darkTheme) pinkDark else pinkLight
        Palette.EMERALD -> if (darkTheme) emeraldDark else emeraldLight
        Palette.INDIGO -> if (darkTheme) indigoDark else indigoLight
        Palette.VIOLET -> if (darkTheme) violetDark else violetLight
        Palette.CRIMSON -> if (darkTheme) crimsonDark else crimsonLight
        Palette.CYAN -> if (darkTheme) cyanDark else cyanLight
    }
    val diff = when (palette) {
        Palette.PINK -> PinkDiff
        Palette.EMERALD -> EmeraldDiff
        Palette.INDIGO -> IndigoDiff
        Palette.VIOLET -> VioletDiff
        Palette.CRIMSON -> CrimsonDiff
        Palette.CYAN -> CyanDiff
    }
    CompositionLocalProvider(LocalDiffColors provides diff) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GitofyTypography,
            shapes = GitofyShapes,
            content = content,
        )
    }
}
