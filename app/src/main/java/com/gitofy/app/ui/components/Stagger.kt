package com.gitofy.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

/**
 * Subtle content stagger (the supplied PRD, §6).
 *
 * Each logical group of content fades in (alpha 0 → 1) and rises 6 dp, delayed by
 * [index] × 24 ms. It runs **once** per composition — never again on scroll or
 * recomposition — and is skipped entirely when reduced motion is on.
 *
 * Use it on a screen's main content groups (heading, first section, cards), not on
 * every individual list row.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reduced) {
            shown = true
        } else {
            delay((index.coerceIn(0, 6) * 24).toLong())
            shown = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "staggerAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (shown) 0.dp else 6.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "staggerOffset",
    )
    Box(modifier.offset(y = offsetY).alpha(alpha)) { content() }
}
