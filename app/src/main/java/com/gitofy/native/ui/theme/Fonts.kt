package com.gitofy.native.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * The web app offered 6 font choices. Compose needs a FontFamily per choice.
 *
 * These are mapped to the platform families so the app never depends on a font
 * download to render. To use the real downloadable Google Fonts (Josefin Sans,
 * Poppins, Montserrat, Orbitron, Playfair Display), add the font-provider certs
 * (res/values/font_certs.xml) and swap the mappings for GoogleFont families —
 * the selection UI and settings keys already match.
 */
object Fonts {
    val choices = listOf("josefin", "poppins", "montserrat", "orbitron", "playfair", "system")

    fun familyFor(id: String): FontFamily = when (id) {
        "josefin", "poppins", "montserrat" -> FontFamily.SansSerif
        "orbitron" -> FontFamily.Monospace
        "playfair" -> FontFamily.Serif
        else -> FontFamily.Default
    }
}
