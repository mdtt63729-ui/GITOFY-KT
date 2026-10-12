package com.gitofy.app.ui.screens.browse

import android.content.Intent
import com.gitofy.app.ui.components.pressClickable
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.model.GitHubRelease
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.AccountAvatar
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepoDashboardViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Repository>>(UiState.Loading)
    val state: StateFlow<UiState<Repository>> = _state.asStateFlow()

    private val _releases = MutableStateFlow<List<GitHubRelease>>(emptyList())
    val releases: StateFlow<List<GitHubRelease>> = _releases.asStateFlow()

    private var owner = ""; private var repo = ""

    fun load(o: String, r: String) {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.repo(o, r) }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    fun loadReleases() = viewModelScope.launch {
        _releases.value = runCatching { api.releases(owner, repo) }.getOrDefault(emptyList())
    }
}

@Composable
fun RepoDashboardScreen(
    owner: String,
    repo: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: RepoDashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    val releases by vm.releases.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(topBar = {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(
                    repo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                AccountAvatar(size = 30.dp, modifier = Modifier.padding(end = 4.dp))
                IconButton(onClick = {
                    val url = (s as? UiState.Content)?.data?.htmlUrl
                    if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }) { Icon(Icons.Filled.OpenInNew, "Open on GitHub") }
                Box {
                    IconButton(onClick = { moreOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MenuRow("Share", Icons.Filled.Share) { moreOpen = false; onNavigate("${Routes.REPO_ACTION}") }
                        MenuRow("Branches", Icons.Filled.AccountTree) { moreOpen = false; onNavigate("${Routes.BRANCH_MANAGER}/$owner/$repo") }
                        MenuRow("Issues & pull requests", Icons.Filled.BugReport) { moreOpen = false; onNavigate("${Routes.ISSUES}/$owner/$repo") }
                        MenuRow("New release", Icons.Filled.NewReleases) { moreOpen = false; onNavigate("${Routes.RELEASE_CREATOR}/$owner/$repo") }
                        MenuRow("Repository settings", Icons.Filled.Settings) { moreOpen = false; onNavigate("${Routes.REPO_SETTINGS}/$owner/$repo") }
                        MenuRow("Delete content", Icons.Filled.DeleteOutline, MaterialTheme.colorScheme.error) {
                            moreOpen = false; onNavigate("${Routes.DELETE_REPO}/$owner/$repo")
                        }
                    }
                }
            }
        }
    }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content -> Column(Modifier.fillMaxSize().padding(padding)) {
                TabSwitcher(tab) { tab = it; if (it == 1) vm.loadReleases() }
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(androidx.compose.animation.core.tween(220)) togetherWith fadeOut(androidx.compose.animation.core.tween(160)) },
                    label = "repoTab",
                ) { t ->
                    if (t == 0) OverviewTab(st.data, onNavigate) else ReleasesTab(releases, onNavigate)
                }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(icon, null, tint = tint) },
        onClick = onClick,
    )
}

@Composable
private fun TabSwitcher(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp).padding(4.dp)) {
            val half = maxWidth / 2
            val frac by animateFloatAsState(if (selected == 0) 0f else 1f, label = "tabIndicator")
            Box(
                Modifier.offset(x = half * frac).width(half).fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
            )
            Row(Modifier.fillMaxSize()) {
                TabButton("Overview", selected == 0, { onSelect(0) }, Modifier.weight(1f))
                TabButton("Releases & APKs", selected == 1, { onSelect(1) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).pressClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            label,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OverviewTab(r: Repository, onNavigate: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.fullName, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    Text(r.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(r.description ?: "No description provided", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("★ ${r.stargazersCount}")
                        StatChip("⑂ ${r.forksCount}")
                        r.language?.let { StatChip(it) }
                        StatChip(if (r.isPrivate) "Private" else "Public")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardButton(
                    label = "Upload Project",
                    icon = Icons.Filled.FileUpload,
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                ) { onNavigate(Routes.UPLOAD) }
                DashboardButton(
                    label = "Workflows",
                    icon = Icons.Filled.PlayArrow,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                ) { onNavigate("${Routes.WORKFLOWS}/${r.ownerLogin}/${r.name}") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Routes.REPO_FILES to "Code",
                    Routes.REPO_COMMITS to "Commits",
                    Routes.BRANCH_MANAGER to "Branches",
                    Routes.ISSUES to "Issues & pull requests",
                    Routes.WORKFLOWS to "Workflows",
                    Routes.RELEASE_CREATOR to "New release",
                    Routes.REPO_SETTINGS to "Repository settings",
                ).forEach { (route, label) ->
                    ActionRow(label) { onNavigate("$route/${r.ownerLogin}/${r.name}") }
                }
            }
        }
    }
}

@Composable
private fun ReleasesTab(releases: List<GitHubRelease>, onNavigate: (String) -> Unit) {
    if (releases.isEmpty()) {
        com.gitofy.app.ui.components.EmptyState(
            "No releases", "Publish a release to see it here.",
            icon = Icons.Filled.NewReleases, modifier = Modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(releases, key = { it.id }) { rel ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.NewReleases, null, tint = MaterialTheme.colorScheme.primary)
                            Text(rel.tagName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (rel.name.isNotBlank()) Text(rel.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val apk = rel.assets.firstOrNull { it.name.lowercase().endsWith(".apk") }
                        if (apk != null) Text("APK · ${apk.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun DashboardButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = modifier.height(48.dp).pressClickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(
                label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().pressClickable(onClick = onClick),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
    }
}
