package com.gitofy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gitofy.app.model.Repository

/** A consistent top bar with an optional back button and actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitofyTopBar(title: String, onBack: (() -> Unit)? = null, avatar: Boolean = true, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            actions()
            if (avatar) AccountAvatar(size = 32.dp, modifier = Modifier.padding(end = 8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/** Circular avatar: shows the initial, then the real image when it loads. */
@Composable
fun Avatar(url: String?, name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        }
    }
}

@Composable
fun StatChip(text: String, icon: ImageVector = Icons.Filled.Star) {
    AssistChip(onClick = {}, label = { Text(text) }, leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) })
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String = "",
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(28.dp),
        ) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (body.isNotEmpty()) {
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(28.dp),
        ) {
            Icon(Icons.Filled.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRetry != null) Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

/** A shimmering placeholder list shown while data loads. */
@Composable
fun SkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).alpha(alpha))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).alpha(alpha))
                    Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).alpha(alpha))
                }
            }
        }
    }
}

/** A polished repository row. */
@Composable
fun RepoCard(repo: Repository, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressClickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(repo.owner?.avatarUrl, repo.ownerLogin)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (repo.isPrivate) Text("• private", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                repo.description ?: "${repo.ownerLogin} • ${repo.language ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("★ ${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                Text("⑂ ${repo.forksCount}", style = MaterialTheme.typography.labelSmall)
                repo.language?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

/** A polished list screen: top bar, skeleton, empty/error states, rows. */
@Composable
fun <T> PolishedList(
    title: String,
    state: com.gitofy.app.ui.state.UiState<List<T>>,
    onBack: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    emptyTitle: String = "Nothing here",
    emptyBody: String = "",
    emptyIcon: ImageVector = Icons.Outlined.Inbox,
    actions: @Composable () -> Unit = {},
    key: (T) -> Any = { it.hashCode() },
    header: @Composable () -> Unit = {},
    row: @Composable (T) -> Unit,
) {
    androidx.compose.material3.Scaffold(topBar = { GitofyTopBar(title, onBack, actions = actions) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            header()
            when (state) {
                is com.gitofy.app.ui.state.UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                is com.gitofy.app.ui.state.UiState.Error -> ErrorState(state.message, onRetry, Modifier.fillMaxSize())
                is com.gitofy.app.ui.state.UiState.Content ->
                    if (state.data.isEmpty()) {
                        EmptyState(emptyTitle, emptyBody, emptyIcon, modifier = Modifier.fillMaxSize())
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize()) {
                            items(state.data, key = key) { item -> row(item) }
                        }
                    }
            }
        }
    }
}

/** A small bar chart of the last upload-tuning benchmark, for Settings. */
@Composable
fun BenchmarkChart(result: com.gitofy.app.git.BenchmarkResult) {
    val maxSec = result.levels.maxOfOrNull { it.estimatedTotalSec }?.coerceAtLeast(0.0001) ?: 0.0001
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${result.profileName} link · ~${com.gitofy.app.util.formatSize(result.totalBytes)} · ${result.upstreamKbps / 1000} Mbps up",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        result.levels.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("L${l.level}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(24.dp))
                Box(
                    Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((l.estimatedTotalSec / maxSec).toFloat().coerceIn(0.02f, 1f))
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (l.chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary),
                    )
                }
                Text(
                    "%.1fs%s".format(l.estimatedTotalSec, if (l.chosen) " ✓" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        val chosen = result.levels.firstOrNull { it.chosen }?.level
        Text("Chosen compression: level $chosen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}
