package com.gitofy.app.ui.screens.browse

import android.content.Intent
import com.gitofy.app.ui.components.pressClickable
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.gitofy.app.data.remote.dto.ContentDto
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
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
class RepoCodeViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<ContentDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ContentDto>>> = _state.asStateFlow()
    fun load(o: String, r: String, path: String, ref: String? = null) = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.contents(o, r, path, ref) }
            .onSuccess { _state.value = UiState.Content(it.sortedByDescending { c -> c.type == "dir" }) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoCodeScreen(
    owner: String,
    repo: String,
    path: String,
    onBack: () -> Unit,
    onOpen: (String, Boolean) -> Unit,
    vm: RepoCodeViewModel = hiltViewModel(),
) {
    LaunchedEffect(owner, repo, path) { vm.load(owner, repo, path) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sheetFile by remember { mutableStateOf<ContentDto?>(null) }

    Scaffold(topBar = { GitofyTopBar(repo, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Breadcrumb
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val crumb = if (path.isBlank()) "root" else "root / " + path.split("/").joinToString(" / ")
                Text(crumb, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when (val st = s) {
                is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo, path) }, modifier = Modifier.fillMaxSize())
                is UiState.Content ->
                    if (st.data.isEmpty()) EmptyState("This folder is empty.", icon = Icons.Filled.Folder, modifier = Modifier.fillMaxSize())
                    else LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(st.data, key = { it.path }) { c ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth().pressClickable {
                                    if (c.type == "dir") onOpen(c.path, true) else sheetFile = c
                                },
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(if (c.type == "dir") Icons.Filled.Folder else Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(if (c.type == "dir") "${c.name}/" else c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(if (c.type == "dir") "directory" else "${c.size} bytes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    sheetFile?.let { file ->
        ModalBottomSheet(onDismissRequest = { sheetFile = null }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ActionItem("Open", Icons.Filled.OpenInNew) {
                    sheetFile = null
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$owner/$repo/blob/HEAD/${file.path}"))) }
                }
                ActionItem("Download", Icons.Filled.Download) {
                    sheetFile = null
                    file.downloadUrl?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
                }
                ActionItem("Edit", Icons.Filled.Edit) {
                    sheetFile = null
                    onOpen(file.path, false)
                }
            }
        }
    }
}

@Composable
private fun ActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressClickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
