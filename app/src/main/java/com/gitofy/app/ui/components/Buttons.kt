package com.gitofy.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * The real Material 3 buttons, each with the app's bouncy press.
 *
 * These are drop-in replacements: they take the same arguments the call sites use
 * (onClick / modifier / enabled / shape / content) and apply
 * [Modifier.bouncyPress] with the component's own interaction source, so the button
 * springs down and back with an overshoot — while keeping M3's ripple, state layer,
 * colours and elevation.
 */

@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(src),
        enabled = enabled,
        interactionSource = src,
        content = content,
    )
}

@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.bouncyPress(src),
        enabled = enabled,
        shape = shape,
        interactionSource = src,
        content = content,
    )
}

@Composable
fun BouncyFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(src),
        enabled = enabled,
        shape = shape,
        interactionSource = src,
        content = content,
    )
}

@Composable
fun BouncyOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(src),
        enabled = enabled,
        shape = shape,
        interactionSource = src,
        content = content,
    )
}

@Composable
fun BouncyTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(src),
        enabled = enabled,
        interactionSource = src,
        content = content,
    )
}
