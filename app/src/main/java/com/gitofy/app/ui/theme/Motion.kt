package com.gitofy.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Material 3 motion primitives.
 *
 * M3 distinguishes *spatial* motion (position/size — can overshoot slightly) from
 * *effects* motion (colour/opacity — must not overshoot). These specs are the
 * app-wide defaults; screens use them via AnimatedVisibility / animate*AsState /
 * SharedTransitionLayout so the whole app moves with one consistent physics.
 */
object GitofyMotion {
    /** Spatial: medium spring, low bounce — the M3 default for containers. */
    val spatialMedium: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Spatial: fast spring for small elements (chips, icons). */
    val spatialFast: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Effects: colour/opacity — critically damped, never overshoots. */
    val effects: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Nav transitions: shared axis durations (ms). */
    const val NAV_ENTER_MS = 300
    const val NAV_EXIT_MS = 250

    /** Content change (tab switches, size changes): smooth, no bounce. */
    val contentChange: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** List item placement (insert / remove / reorder): gentle. */
    val listItem: SpringSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}
