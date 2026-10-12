package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.DispatchRequest
import com.gitofy.app.model.WorkflowItem
import com.gitofy.app.model.WorkflowRun
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkflowsViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<WorkflowItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<WorkflowItem>>> = _state.asStateFlow()
    private val _runs = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val runs: StateFlow<List<WorkflowRun>> = _runs.asStateFlow()
    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg.asStateFlow()
    private var owner = ""; private var repo = ""

    fun load(o: String, r: String, branch: String) {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.workflows(o, r).workflows }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
            _runs.value = runCatching { api.runs(o, r).workflow_runs }.getOrDefault(emptyList())
        }
    }

    fun run(id: String, branch: String) = viewModelScope.launch {
        runCatching { api.dispatch(owner, repo, id, DispatchRequest(branch)) }
            .onSuccess { _msg.value = "Workflow started on $branch" }
            .onFailure { _msg.value = it.message ?: "Could not start workflow" }
    }
}

@Composable
fun WorkflowsScreen(owner: String, repo: String, onBack: () -> Unit, vm: WorkflowsViewModel = hiltViewModel()) {
    var branch by remember { mutableStateOf("main") }
    LaunchedEffect(owner, repo) { vm.load(owner, repo, branch) }
    val s by vm.state.collectAsStateWithLifecycle()
    val runs by vm.runs.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        GitofyTopBar("GitHub Actions", onBack, actions = {
            IconButton(onClick = { vm.load(owner, repo, branch) }) { Icon(Icons.Filled.Refresh, "Refresh") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                branch, { branch = it }, label = { Text("Target Branch") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
            when (val st = s) {
                is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo, branch) }, modifier = Modifier.fillMaxSize())
                is UiState.Content -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (st.data.isEmpty() && runs.isEmpty()) {
                        item { EmptyState("No workflows in this repository", "Add a workflow under .github/workflows.", icon = Icons.Filled.PlayArrow) }
                    }
                    if (st.data.isNotEmpty()) {
                        item { Text("WORKFLOWS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        items(st.data, key = { it.id }) { w ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(w.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(w.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(onClick = { vm.run(w.id.toString(), branch) }) { Text("Run") }
                                }
                            }
                        }
                    }
                    item { Text("RECENT RUNS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                    if (runs.isEmpty()) item { Text("No runs executed yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else items(runs, key = { it.id }) { r ->
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(r.name.ifBlank { "#${r.runNumber}" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${r.status} ${r.conclusion ?: ""} • ${r.headBranch ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
