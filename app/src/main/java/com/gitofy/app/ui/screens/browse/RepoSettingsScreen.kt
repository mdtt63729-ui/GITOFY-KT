package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.UpdateRepoRequest
import com.gitofy.app.model.Repository
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
class RepoSettingsViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Repository>>(UiState.Loading)
    val state: StateFlow<UiState<Repository>> = _state.asStateFlow()
    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg.asStateFlow()
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
    fun save(name: String, desc: String, isPrivate: Boolean, archived: Boolean, defaultBranch: String) = viewModelScope.launch {
        runCatching { api.updateRepo(owner, repo, UpdateRepoRequest(name, desc, isPrivate, archived, defaultBranch)) }
            .onSuccess { _state.value = UiState.Content(it); _msg.value = "Saved" }
            .onFailure { _msg.value = it.message ?: "Save failed" }
    }
    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        runCatching { api.deleteRepo(owner, repo) }.onSuccess { onDone() }.onFailure { _msg.value = it.message }
    }
}

@Composable
fun RepoSettingsScreen(owner: String, repo: String, onBack: () -> Unit, onDeleted: () -> Unit, vm: RepoSettingsViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()

    Scaffold(topBar = { GitofyTopBar("Repository settings", onBack) }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content -> {
                val r = st.data
                var name by remember(r) { mutableStateOf(r.name) }
                var desc by remember(r) { mutableStateOf(r.description ?: "") }
                var priv by remember(r) { mutableStateOf(r.isPrivate) }
                var archived by remember(r) { mutableStateOf(r.archived) }
                var branch by remember(r) { mutableStateOf(r.defaultBranch) }
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionCard("General") {
                        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Private", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Switch(priv, { priv = it })
                        }
                    }
                    SectionCard("Default branch") {
                        OutlinedTextField(branch, { branch = it }, label = { Text("Default branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    SectionCard("Danger zone") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Archive this repository", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Make it read-only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(archived, { archived = it })
                        }
                        OutlinedButton(onClick = { vm.delete(onDeleted) }, modifier = Modifier.fillMaxWidth()) { Text("Delete repository") }
                    }
                    Button(
                        onClick = { vm.save(name, desc, priv, archived, branch) },
                        shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save changes", fontWeight = FontWeight.Bold) }
                    msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
