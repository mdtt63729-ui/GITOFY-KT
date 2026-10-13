package com.gitofy.app.ui.screens.browse

import com.gitofy.app.ui.components.BouncyIconButton
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.gitofy.app.model.ReleaseAsset
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.AccountAvatar
import com.gitofy.app.ui.components.BouncyButton
import com.gitofy.app.ui.components.BouncyFilledTonalButton
import com.gitofy.app.ui.components.BouncyOutlinedButton
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepoDashboardViewModel @Inject constructor(
    private val api: GitHubApiService,
    private val downloader: com.gitofy.app.update.ApkDownloader,
    private val fileDownloader: com.gitofy.app.data.remote.FileDownloader,
    private val tokenStore: com.gitofy.app.data.TokenStore,
) : ViewModel() {
    companion object { const val ZIP_ID = -1L }
    private val _state = MutableStateFlow<UiState<Repository>>(UiState.Loading)
    val state: StateFlow<UiState<Repository>> = _state.asStateFlow()

    private val _releases = MutableStateFlow<List<GitHubRelease>>(emptyList())
    val releases: StateFlow<List<GitHubRelease>> = _releases.asStateFlow()

    private val _downloads = MutableStateFlow<Map<Long, com.gitofy.app.update.DownloadProgress>>(emptyMap())
    val downloads: StateFlow<Map<Long, com.gitofy.app.update.DownloadProgress>> = _downloads.asStateFlow()
    private val _downloaded = MutableStateFlow<Set<Long>>(emptySet())
    val downloaded: StateFlow<Set<Long>> = _downloaded.asStateFlow()

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

    /** Real in-app download of a release asset (APK), streaming progress as it goes. */
    fun download(rel: GitHubRelease, asset: ReleaseAsset) = viewModelScope.launch {
        val info = com.gitofy.app.update.ReleaseInfo(
            tag = rel.tagName,
            version = rel.tagName,
            apkName = asset.name,
            downloadUrl = asset.browserDownloadUrl,
            apiAssetUrl = "https://api.github.com/repos/$owner/$repo/releases/assets/${asset.id}",
            notes = rel.body ?: "",
            publishedAt = rel.publishedAt,
        )
        runCatching {
            downloader.download(info, tokenStore.token()).collect { p ->
                _downloads.value = _downloads.value + (asset.id to p)
            }
        }.onSuccess { _downloaded.value = _downloaded.value + asset.id }
    }

    /** Download the repository's source code as a ZIP (repos/…/zipball/<branch>). */
    fun downloadZip(branch: String) = viewModelScope.launch {
        val url = "https://api.github.com/repos/$owner/$repo/zipball/$branch"
        runCatching {
            fileDownloader.download(url, "$repo-$branch.zip", withAuth = true).collect { p ->
                _downloads.value = _downloads.value + (ZIP_ID to p)
            }
        }.onSuccess { _downloaded.value = _downloaded.value + ZIP_ID }
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
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(topBar = {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BouncyIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(
                    repo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                AccountAvatar(size = 30.dp, modifier = Modifier.padding(end = 4.dp))
                BouncyIconButton(onClick = {
                    val url = (s as? UiState.Content)?.data?.htmlUrl
                    if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }) { Icon(Icons.Filled.OpenInNew, "Open on GitHub") }
                Box {
                    BouncyIconButton(onClick = { moreOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MenuRow("Share", Icons.Filled.Share) {
                            moreOpen = false
                            val url = (s as? UiState.Content)?.data?.htmlUrl.orEmpty()
                            val title = (s as? UiState.Content)?.data?.name ?: repo
                            if (url.isNotBlank()) runCatching {
                                val i = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                context.startActivity(Intent.createChooser(i, "Share repository"))
                            }
                        }
                        MenuRow("Branches", Icons.Filled.AccountTree) { moreOpen = false; onNavigate("${Routes.BRANCH_MANAGER}/$owner/$repo") }
                        MenuRow("Issues & pull requests", Icons.Filled.BugReport) { moreOpen = false; onNavigate("${Routes.ISSUES}/$owner/$repo") }
                        MenuRow("New release", Icons.Filled.NewReleases) { moreOpen = false; onNavigate("${Routes.RELEASE_CREATOR}/$owner/$repo") }
                        MenuRow("Repository settings", Icons.Filled.Settings) { moreOpen = false; onNavigate("${Routes.REPO_SETTINGS}/$owner/$repo") }
                        MenuRow("Delete content", Icons.Filled.DeleteForever, MaterialTheme.colorScheme.error) {
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
                    if (t == 0) OverviewTab(st.data, onNavigate, downloads, downloaded, onDownloadZip = { vm.downloadZip(it) })
                    else ReleasesTab(releases, downloads, downloaded, onDownload = { rel, asset -> vm.download(rel, asset) })
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
private fun OverviewTab(
    r: Repository,
    onNavigate: (String) -> Unit,
    downloads: Map<Long, com.gitofy.app.update.DownloadProgress>,
    downloaded: Set<Long>,
    onDownloadZip: (String) -> Unit,
) {
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
                BouncyButton(
                    onClick = { onNavigate(Routes.UPLOAD) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Project", maxLines = 1)
                }
                BouncyFilledTonalButton(
                    onClick = { onNavigate("${Routes.WORKFLOWS}/${r.ownerLogin}/${r.name}") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Workflows", maxLines = 1)
                }
            }
        }
        item {
            val zipProg = downloads[RepoDashboardViewModel.ZIP_ID]
            val zipDone = downloaded.contains(RepoDashboardViewModel.ZIP_ID)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Source code (ZIP)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(r.defaultBranch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            zipDone -> Text("Downloaded", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            zipProg != null -> Text("${zipProg.percent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            else -> Text(
                                "Download ZIP",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.pressClickable { onDownloadZip(r.defaultBranch) }.padding(6.dp),
                            )
                        }
                    }
                    if (zipProg != null && !zipDone) {
                        LinearProgressIndicator(progress = { zipProg.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        // Every repository destination is a real Material 3 button here, with proper M3
        // icons — two per row. The ⋯ menu keeps the same actions as a shortcut.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RepoActionButton("Code", Icons.Filled.Folder, Modifier.weight(1f)) { onNavigate("${Routes.REPO_FILES}/${r.ownerLogin}/${r.name}") }
                    RepoActionButton("Commits", Icons.Filled.History, Modifier.weight(1f)) { onNavigate("${Routes.REPO_COMMITS}/${r.ownerLogin}/${r.name}") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RepoActionButton("Branches", Icons.Filled.AccountTree, Modifier.weight(1f)) { onNavigate("${Routes.BRANCH_MANAGER}/${r.ownerLogin}/${r.name}") }
                    RepoActionButton("Issues", Icons.Filled.BugReport, Modifier.weight(1f)) { onNavigate("${Routes.ISSUES}/${r.ownerLogin}/${r.name}") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RepoActionButton("New release", Icons.Filled.NewReleases, Modifier.weight(1f)) { onNavigate("${Routes.RELEASE_CREATOR}/${r.ownerLogin}/${r.name}") }
                    RepoActionButton("Settings", Icons.Filled.Settings, Modifier.weight(1f)) { onNavigate("${Routes.REPO_SETTINGS}/${r.ownerLogin}/${r.name}") }
                }
                BouncyOutlinedButton(
                    onClick = { onNavigate("${Routes.DELETE_REPO}/${r.ownerLogin}/${r.name}") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete content", maxLines = 1, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ReleasesTab(
    releases: List<GitHubRelease>,
    downloads: Map<Long, com.gitofy.app.update.DownloadProgress>,
    downloaded: Set<Long>,
    onDownload: (GitHubRelease, ReleaseAsset) -> Unit,
) {
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
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.NewReleases, null, tint = MaterialTheme.colorScheme.primary)
                            Text(rel.tagName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (rel.name.isNotBlank()) Text(rel.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        val apks = rel.assets.filter { it.name.lowercase().endsWith(".apk") }
                        if (apks.isEmpty()) {
                            Text("No APK assets in this release.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else apks.forEach { asset ->
                            val prog = downloads[asset.id]
                            val isDone = downloaded.contains(asset.id)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.primary)
                                        Column(Modifier.weight(1f)) {
                                            Text(asset.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${"%.1f".format(asset.size / 1024.0 / 1024.0)} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        when {
                                            isDone -> Text("Downloaded", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            prog != null -> Text("${prog.percent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            else -> Text(
                                                "Download",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.pressClickable { onDownload(rel, asset) }.padding(6.dp),
                                            )
                                        }
                                    }
                                    if (prog != null && !isDone) {
                                        LinearProgressIndicator(
                                            progress = { prog.percent / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Text(
                                            "${prog.percent}% · ${"%.1f".format(prog.speedBps / 1024.0 / 1024.0)} MB/s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
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
private fun RepoActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BouncyOutlinedButton(onClick = onClick, shape = RoundedCornerShape(50), modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().pressClickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}
