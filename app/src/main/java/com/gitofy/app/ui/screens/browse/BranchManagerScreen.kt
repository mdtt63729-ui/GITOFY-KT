package com.gitofy.app.ui.screens.browse

import com.gitofy.app.ui.components.BouncyButton
import com.gitofy.app.ui.components.BouncyIconButton
import com.gitofy.app.ui.components.BouncyTextButton
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.CreateBranchRequest
import com.gitofy.app.data.remote.dto.RenameBranchRequest
import com.gitofy.app.data.remote.dto.BranchDto
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
class BranchesViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<BranchDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BranchDto>>> = _state.asStateFlow()
    private val _default = MutableStateFlow("")
    val default: StateFlow<String> = _default.asStateFlow()
    private var owner = ""; private var repo = ""

    fun load(o: String, r: String) {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.branches(o, r) }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
            _default.value = runCatching { api.repo(o, r).defaultBranch }.getOrDefault("")
        }
    }
    fun create(name: String, sha: String) = viewModelScope.launch {
        runCatching { api.createBranch(owner, repo, CreateBranchRequest("refs/heads/$name", sha)) }.onSuccess { load(owner, repo) }
    }
    fun rename(from: String, to: String) = viewModelScope.launch {
        runCatching { api.renameBranch(owner, repo, from, RenameBranchRequest(to)) }.onSuccess { load(owner, repo) }
    }
    fun delete(name: String) = viewModelScope.launch {
        runCatching { api.deleteBranch(owner, repo, name) }.onSuccess { load(owner, repo) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagerScreen(owner: String, repo: String, onBack: () -> Unit, vm: BranchesViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    val default by vm.default.collectAsStateWithLifecycle()
    var newOpen by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        GitofyTopBar("Branches · $repo", onBack, actions = {
            BouncyIconButton(onClick = { vm.load(owner, repo) }) { Icon(Icons.Filled.Refresh, "Refresh") }
            BouncyIconButton(onClick = { newOpen = true }) { Icon(Icons.Filled.Add, "New branch") }
        })
    }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content ->
                if (st.data.isEmpty()) EmptyState("No branches found.", icon = Icons.Filled.CallSplit, modifier = Modifier.fillMaxSize().padding(padding))
                else LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(st.data, key = { it.name }) { b ->
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.CallSplit, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(b.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (b.name == default) {
                                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("Default", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(b.commit.sha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BouncyTextButton(onClick = { vm.rename(b.name, "${b.name}-renamed") }) { Text("Rename") }
                                    BouncyTextButton(onClick = { vm.delete(b.name) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
        }
    }

    if (newOpen) {
        var name by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { newOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New branch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it }, label = { Text("Branch name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BouncyButton(onClick = { newOpen = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    BouncyButton(
                        onClick = { vm.create(name, (s as? UiState.Content)?.data?.firstOrNull()?.commit?.sha ?: ""); newOpen = false },
                        enabled = name.isNotBlank(), modifier = Modifier.weight(1f),
                    ) { Text("Create") }
                }
            }
        }
    }
}
