package com.gitofy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Like [Modifier.clickable] but with **no ripple indication**.
 *
 * Compose's default ripple is not clipped to a component's rounded shape, so on
 * pill/card components it shows up as a grey rectangle behind them. The web app uses
 * press-scale instead of a ripple, so every custom-tappable surface here uses this.
 */
@Composable
fun Modifier.pressClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    onClick = onClick,
)
