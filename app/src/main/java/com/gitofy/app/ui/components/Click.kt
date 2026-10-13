package com.gitofy.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A tap target with a **bouncy press** and **no ripple**.
 *
 * On press the surface springs down to 0.94× and springs back with an overshoot on
 * release — the "bouncy" feel the app wants. There is no ripple indication: Compose's
 * default ripple is not clipped to a rounded shape, so on pill/card components it drew
 * a grey rectangle behind them.
 *
 * The scale uses a `graphicsLayer` transform only (no alpha) — an alpha layer is what
 * made elevated surfaces render a grey square shadow.
 */
@Composable
fun Modifier.pressClickable(
    enabled: Boolean = true,
    bounce: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && bounce && enabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Bouncy press for a **real Material 3 component** (Button, IconButton, …). Pass the
 * component's own [MutableInteractionSource] and it springs down/back with the same
 * overshoot as [pressClickable].
 */
@Composable
fun Modifier.bouncyPress(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMedium),
        label = "bouncyPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
