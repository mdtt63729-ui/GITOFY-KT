package com.gitofy.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.Avatar
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.FabItem
import com.gitofy.app.ui.components.FabMenu
import com.gitofy.app.ui.components.FloatingNav
import com.gitofy.app.ui.components.NavTab
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.screens.sheets.CreateRepoSheet
import com.gitofy.app.ui.state.UiState

private enum class RepoFilter(val label: String) { ALL("All"), PRIVATE("Private"), PUBLIC("Public"), PINNED("Pinned") }

@Composable
fun HomeScreen(
    onOpenRepo: (String, String) -> Unit,
    onNavigate: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hasToken by vm.hasToken.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()
    val avatarUrl by vm.avatarUrl.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(RepoFilter.ALL) }

    // Hide the nav + FAB while scrolling down, reveal them when scrolling up or
    // back near the top (the original app's auto-hide behaviour).
    val listState = rememberLazyListState()
    var navVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var last = 0
        snapshotFlow { listState.firstVisibleItemIndex * 1000 + listState.firstVisibleItemScrollOffset }
            .collect { cur ->
                val delta = cur - last
                when {
                    cur < 60 -> navVisible = true
                    delta > 14 -> navVisible = false
                    delta < -14 -> navVisible = true
                }
                last = cur
            }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().blur(if (fabOpen) 18.dp else 0.dp)) {
                HomeAppBar(
                    username = username,
                    repoCount = (s as? UiState.Content)?.data?.size ?: 0,
                    onSearch = { onNavigate(Routes.APP_STORE) },
                    onRefresh = { vm.refresh() },
                    onSettings = { onNavigate(Routes.SETTINGS) },
                    onAccounts = { onNavigate(Routes.GITHUB_SETTINGS) },
                    avatarUrl = avatarUrl,
                )

                when (val st = s) {
                    is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                    is UiState.Error -> ErrorState(st.message, onRetry = { vm.refresh() }, modifier = Modifier.fillMaxSize())
                    is UiState.Content -> {
                        val filtered = st.data.filter {
                            when (filter) {
                                RepoFilter.ALL -> true
                                RepoFilter.PRIVATE -> it.isPrivate
                                RepoFilter.PUBLIC -> !it.isPrivate
                                RepoFilter.PINNED -> false
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (!hasToken) {
                                item { ConnectGitHubCard(onSetToken = { onNavigate(Routes.SETTINGS) }) }
                            }
                            item { FilterRow(filter) { filter = it } }
                            if (filtered.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "No repositories",
                                        body = if (hasToken) "Tap + to create one." else "Connect GitHub to see your repos.",
                                    )
                                }
                            } else {
                                items(filtered, key = { it.id }) { r -> Box(Modifier.animateItem()) { RepoCard(r) { onOpenRepo(r.ownerLogin, r.name) } } }
                            }
                        }
                    }
                }
            }

        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)) {
            FloatingNav(
                current = NavTab.HOME,
                onSelect = { if (it == NavTab.INBOX) onNavigate(Routes.INBOX) },
                unread = 0,
                visible = navVisible,
            )
        }

        FabMenu(
            expanded = fabOpen,
            onToggle = { fabOpen = !fabOpen },
            visible = navVisible,
            items = listOf(
                FabItem("Upload project", Icons.Filled.FileUpload,
                    MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                    { fabOpen = false; onNavigate(Routes.UPLOAD) }),
                FabItem("Update repo", Icons.Filled.Sync,
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                    { fabOpen = false; onNavigate(Routes.MULTI_UPLOAD) }),
                FabItem("Create repo", Icons.Filled.Add,
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
                    { fabOpen = false; showCreate = true }),
                FabItem("Delete repo", Icons.Filled.DeleteOutline,
                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                    { fabOpen = false; onNavigate(Routes.REPO_ACTION) }),
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showCreate) {
        CreateRepoSheet(
            onDismiss = { showCreate = false },
            onCreate = { n, d, p -> vm.create(n, d, p); showCreate = false },
        )
    }
}

@Composable
private fun HomeAppBar(
    username: String,
    repoCount: Int,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onAccounts: () -> Unit,
    avatarUrl: String,
) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Gitofy", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text(
                    "${if (username.isNotBlank()) "@$username" else "GitHub"} · $repoCount repositories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppBarIcon(Icons.Filled.Search, "Search", onSearch)
            AppBarIcon(Icons.Filled.Refresh, "Refresh", onRefresh)
            AppBarIcon(Icons.Filled.Settings, "Settings", onSettings)
            Box(Modifier.padding(start = 6.dp).clip(CircleShape).clickable { onAccounts() }) {
                Avatar(avatarUrl.ifBlank { null }, username.ifBlank { "U" }, size = 36.dp)
            }
        }
    }
}

@Composable
private fun AppBarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.size(40.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun FilterRow(selected: RepoFilter, onSelect: (RepoFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RepoFilter.entries.forEach { f ->
            val active = selected == f
            val bg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                tween(220), label = "chipBg",
            )
            val fg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(220), label = "chipFg",
            )
            val border by animateColorAsState(
                if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                tween(220), label = "chipBorder",
            )
            val scale by animateFloatAsState(if (active) 1f else 0.97f, spring(dampingRatio = 0.5f), label = "chipScale")
            Surface(
                shape = RoundedCornerShape(50),
                color = bg,
                border = BorderStroke(1.dp, border),
                modifier = Modifier.scale(scale).clip(RoundedCornerShape(50)).clickable { onSelect(f) },
            ) {
                Text(
                    f.label,
                    color = fg,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConnectGitHubCard(onSetToken: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp)) }
                Column {
                    Text("Connect your GitHub Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Enter your token to fetch your real repositories and push ZIP diffs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSetToken() },
            ) {
                Text(
                    "Set GitHub Token (PAT)",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun RepoCard(repo: Repository, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(repo.owner?.avatarUrl, repo.ownerLogin, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(repo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PrivacyChip(repo.isPrivate)
                }
                Text(
                    repo.description ?: "No description provided",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repo.language?.let { lang ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(lang, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(repo.defaultBranch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (repo.stargazersCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Star, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyChip(isPrivate: Boolean) {
    val bg = if (isPrivate) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isPrivate) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(if (isPrivate) Icons.Filled.Lock else Icons.Filled.Public, null, Modifier.size(10.dp), tint = fg)
            Text(if (isPrivate) "Private" else "Public", style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}
