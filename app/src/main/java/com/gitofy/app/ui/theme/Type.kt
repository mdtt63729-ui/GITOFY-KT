package com.gitofy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The web app offered six font choices. Without bundling webfonts, each choice maps to a
 * distinct platform family so picking one visibly changes the type — and the weight
 * choice (Light / Normal / Medium / Bold) really applies on top.
 */
fun fontFamilyFor(id: String): FontFamily = when (id) {
    "orbitron" -> FontFamily.Monospace
    "playfair" -> FontFamily.Serif
    "josefin" -> FontFamily.Cursive
    "poppins", "montserrat" -> FontFamily.SansSerif
    else -> FontFamily.Default
}

fun fontWeightFor(id: String): FontWeight = when (id) {
    "light" -> FontWeight.Light
    "medium" -> FontWeight.Medium
    "bold" -> FontWeight.Bold
    else -> FontWeight.Normal
}

/** Build the app typography from the chosen family, weight and font scale. */
fun gitofyTypography(fontId: String, weightId: String, scale: Float = 1f): Typography {
    val fam = fontFamilyFor(fontId)
    val w = fontWeightFor(weightId)
    fun t(size: Float, weight: FontWeight) = TextStyle(fontFamily = fam, fontWeight = weight, fontSize = (size * scale).sp)
    return Typography(
        displaySmall = t(36f, w),
        headlineSmall = t(24f, w),
        titleLarge = t(22f, w),
        titleMedium = t(16f, w),
        bodyLarge = t(16f, FontWeight.Normal),
        bodyMedium = t(14f, FontWeight.Normal),
        labelLarge = t(14f, w),
        labelMedium = t(12f, w),
    )
}

/** Default typography (used before settings load). */
val GitofyTypography = gitofyTypography("system", "normal")
