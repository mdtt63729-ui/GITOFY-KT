package com.gitofy.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Which main tab the floating nav is showing. */
enum class NavTab { HOME, INBOX }

// The web app's motion curves, ported 1:1 from index.css.
private val ScreenInEase = CubicBezierEasing(0.22f, 0.92f, 0.28f, 1f)
private val NavBounceEase = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1.15f)
private val NavHideEase = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val MenuOpenEase = CubicBezierEasing(0.34f, 1.35f, 0.64f, 1f)
private val MenuCloseEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val FabEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The floating pill bottom navigation from the web app (FloatingNav.tsx).
 *
 * The wrapper is fully transparent — only the pill itself is opaque, so nothing
 * box-like appears around the bar. Two-edge spring indicator: on a tab change the
 * leading edge springs first and the trailing edge follows ~90 ms later, so the
 * highlight visibly *stretches* between the two tabs.
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
        enter = slideInVertically(tween(460, easing = NavBounceEase)) { it } +
            scaleIn(tween(460, easing = NavBounceEase), initialScale = 0.94f) +
            fadeIn(tween(220)),
        exit = slideOutVertically(tween(230, easing = NavHideEase)) { it } + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 14.dp,
            modifier = Modifier.height(64.dp).widthIn(min = 240.dp, max = 280.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
                val half = maxWidth / 2

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

                // Two-edge spring indicator. NB: explicit width + fillMaxHeight (not
                // fillMaxSize, which would override the width and fill the whole pill).
                Box(
                    Modifier
                        .offset(x = half * leftFrac)
                        .width(half * (rightFrac - leftFrac) + half)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )

                Row(Modifier.fillMaxSize()) {
                    NavButton(
                        selected = current == NavTab.HOME,
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        label = "Home",
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(NavTab.HOME) },
                        modifier = Modifier.weight(1f),
                    )
                    NavButton(
                        selected = current == NavTab.INBOX,
                        selectedIcon = Icons.Filled.Mail,
                        unselectedIcon = Icons.Outlined.Mail,
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
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "navColor",
    )
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "navScale")
    val iconAlpha by animateFloatAsState(if (selected) 1f else 0.72f, tween(200), label = "navAlpha")
    val labelWidth by animateDpAsState(
        if (selected) 80.dp else 0.dp,
        tween(240, easing = MenuOpenEase), label = "navLabelW",
    )
    val labelAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "navLabelA")

    Row(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(
                if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp).scale(iconScale).alpha(iconAlpha),
            )
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                        .size(15.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) { Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 9.sp) }
            }
        }
        Box(Modifier.width(labelWidth).clipToBounds().alpha(labelAlpha)) {
            Text(
                label, color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1, softWrap = false,
            )
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
 * The expandable FAB menu from the web app (FabMenu.tsx).
 *
 * Host it with `Modifier.fillMaxSize()` so the 28%-black scrim (paired with a real
 * backdrop blur applied to the page content by the host) covers everything,
 * including the nav bar. Items rise from below the FAB, staggered, and the FAB
 * morphs its corner radius while the +/× icons cross-rotate.
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
    LaunchedEffect(visible) { if (!visible && expanded) onToggle() }

    val shown = visible || expanded
    val containerOffset by animateDpAsState(if (shown) 0.dp else 104.dp, tween(if (shown) 380 else 220), label = "fabX")
    val containerScale by animateFloatAsState(if (shown) 1f else 0.9f, tween(if (shown) 380 else 220, easing = NavBounceEase), label = "fabScale")
    val containerAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(220), label = "fabA")

    Box(modifier) {
        // Backdrop scrim (the blur itself is applied to the content by the host).
        AnimatedVisibility(expanded, enter = fadeIn(tween(280)), exit = fadeOut(tween(180))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { onToggle() })
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .offset(x = containerOffset)
                .scale(containerScale)
                .alpha(containerAlpha),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Displayed top-to-bottom as Delete, Create, Update, Upload so Upload
            // (index 0) ends up closest to the FAB — exactly the web app's order.
            val ordered = items.withIndex().reversed()
            ordered.forEach { (index, item) ->
                val openDelay = if (index == 0) 0 else index * 35
                val closeDelay = (items.size - 1 - index) * 25
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(300, delayMillis = openDelay)) +
                        slideInVertically(tween(300, delayMillis = openDelay, easing = MenuOpenEase)) { (index + 1) * 36 } +
                        scaleIn(tween(300, delayMillis = openDelay, easing = MenuOpenEase), initialScale = 0.65f),
                    exit = fadeOut(tween(180, delayMillis = closeDelay, easing = MenuCloseEase)) +
                        scaleOut(tween(180, delayMillis = closeDelay, easing = MenuCloseEase), targetScale = 0.65f),
                ) {
                    FabPill(item) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); item.onClick() }
                }
            }

            FabButton(expanded = expanded, onToggle = onToggle)
        }
    }
}

@Composable
private fun FabButton(expanded: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val radius by animateDpAsState(if (expanded) 28.dp else 16.dp, tween(240, easing = FabEase), label = "fabRadius")
    val bg by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        tween(240), label = "fabBg",
    )
    val fg by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        tween(240), label = "fabFg",
    )
    val plusRot by animateFloatAsState(if (expanded) 90f else 0f, tween(220, easing = FabEase), label = "plusRot")
    val plusAlpha by animateFloatAsState(if (expanded) 0f else 1f, tween(220, easing = FabEase), label = "plusA")
    val plusScale by animateFloatAsState(if (expanded) 0.6f else 1f, tween(220, easing = FabEase), label = "plusS")
    val closeRot by animateFloatAsState(if (expanded) 0f else (-90f), tween(220, easing = FabEase), label = "closeRot")
    val closeAlpha by animateFloatAsState(if (expanded) 1f else 0f, tween(220, easing = FabEase), label = "closeA")
    val closeScale by animateFloatAsState(if (expanded) 1f else 0.6f, tween(220, easing = FabEase), label = "closeS")

    Surface(
        shape = RoundedCornerShape(radius),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (expanded) 18.dp else 14.dp,
        modifier = Modifier.size(56.dp).clickable {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Add, contentDescription = null, tint = fg,
                modifier = Modifier.size(26.dp).rotate(plusRot).scale(plusScale).alpha(plusAlpha),
            )
            Icon(
                Icons.Filled.Close, contentDescription = null, tint = fg,
                modifier = Modifier.size(26.dp).rotate(closeRot).scale(closeScale).alpha(closeAlpha),
            )
        }
    }
}

@Composable
private fun FabPill(item: FabItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = item.container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = Modifier.height(56.dp).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 20.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                item.label, color = item.content,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, softWrap = false,
            )
            Icon(item.icon, contentDescription = null, tint = item.content, modifier = Modifier.size(24.dp))
        }
    }
}
