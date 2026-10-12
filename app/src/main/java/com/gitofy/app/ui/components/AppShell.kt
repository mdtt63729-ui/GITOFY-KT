package com.gitofy.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Which main tab the floating nav is showing. */
enum class NavTab { HOME, INBOX }

/**
 * The floating pill bottom navigation from the web app (FloatingNav.tsx).
 *
 * The indicator uses the original's **two-edge spring**: when the tab changes the
 * leading edge springs to the new tab first, and the trailing edge follows ~90 ms
 * later, so the highlight visibly *stretches* between the two tabs (a "liquid"
 * feel) instead of sliding as one rigid block.
 */
@Composable
fun FloatingNav(
    current: NavTab,
    onSelect: (NavTab) -> Unit,
    unread: Int = 0,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(stiffness = Spring.StiffnessLow)) { it } + fadeIn(),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 12.dp,
            modifier = Modifier.height(64.dp).widthIn(min = 240.dp, max = 280.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
                val half = maxWidth / 2

                // Two-edge spring: leading moves now, trailing follows after a beat.
                var leadingTab by remember { mutableStateOf(current) }
                var trailingTab by remember { mutableStateOf(current) }
                LaunchedEffect(current) {
                    leadingTab = current
                    delay(90)
                    trailingTab = current
                }
                val leading by animateFloatAsState(
                    targetValue = if (leadingTab == NavTab.HOME) 0f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "leading",
                )
                val trailing by animateFloatAsState(
                    targetValue = if (trailingTab == NavTab.HOME) 0f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                    label = "trailing",
                )
                val leftFrac = minOf(leading, trailing)
                val rightFrac = maxOf(leading, trailing)

                Box(
                    Modifier
                        .offset(x = half * leftFrac)
                        .width(half * (rightFrac - leftFrac) + half)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )

                Row(Modifier.fillMaxSize()) {
                    NavButton(
                        selected = current == NavTab.HOME,
                        icon = Icons.Filled.Home,
                        label = "Home",
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(NavTab.HOME) },
                        modifier = Modifier.weight(1f),
                    )
                    NavButton(
                        selected = current == NavTab.INBOX,
                        icon = Icons.Outlined.Mail,
                        label = "Inbox",
                        badge = unread,
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(NavTab.INBOX) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "navScale")
    Row(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(
                icon, contentDescription = label, tint = content,
                modifier = Modifier.size(22.dp).scale(scale),
            )
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                        .size(15.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) { Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 9.sp) }
            }
        }
        AnimatedVisibility(visible = selected, enter = fadeIn(tween(200)), exit = fadeOut(tween(120))) {
            Text(label, color = content, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One item in the expandable FAB menu. */
data class FabItem(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    val onClick: () -> Unit,
)

/**
 * The expandable FAB menu from the web app (FabMenu.tsx): a scrim, four labelled
 * pills (staggered), and the main +/× FAB. Tapping the FAB opens/closes the menu.
 * [visible] lets the host hide it while scrolling (it collapses when hidden).
 */
@Composable
fun FabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<FabItem>,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    // Collapse automatically if the host hides the FAB.
    LaunchedEffect(visible) { if (!visible && expanded) onToggle() }

    Box(modifier) {
        // Scrim
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { onToggle() },
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(160, delayMillis = (items.size - 1 - index) * 40)) +
                        slideInVertically(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) { it / 3 } +
                        scaleIn(spring(dampingRatio = 0.55f), initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f),
                ) {
                    FabPill(item) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); item.onClick() }
                }
            }

            AnimatedVisibility(visible = visible, enter = slideInVertically { it * 2 } + fadeIn(), exit = slideOutVertically { it * 2 } + fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(60.dp).clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) 45f else 0f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                            label = "fabRot",
                        )
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp).rotate(rotation),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FabPill(item: FabItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = item.container,
        shadowElevation = 6.dp,
        modifier = Modifier.height(56.dp).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(item.icon, contentDescription = null, tint = item.content)
            Text(item.label, color = item.content, style = MaterialTheme.typography.titleMedium)
        }
    }
}
