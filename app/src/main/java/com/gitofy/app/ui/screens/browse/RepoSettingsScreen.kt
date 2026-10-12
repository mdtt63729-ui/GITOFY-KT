package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.UpdateRepoRequest
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.LoadingBox
import com.gitofy.app.ui.components.MessageBox
import com.gitofy.app.ui.components.ScreenScaffold
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
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()

    fun load(o: String, r: String) = viewModelScope.launch {
        runCatching { api.repo(o, r) }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
    fun save(o: String, r: String, name: String, desc: String, isPrivate: Boolean, archived: Boolean) = viewModelScope.launch {
        runCatching { api.updateRepo(o, r, UpdateRepoRequest(name, desc, isPrivate, archived)) }
            .onSuccess { _state.value = UiState.Content(it); _saved.value = "Saved" }
            .onFailure { _saved.value = it.message ?: "Save failed" }
    }
    fun delete(o: String, r: String, onDone: () -> Unit) = viewModelScope.launch {
        runCatching { api.deleteRepo(o, r) }.onSuccess { onDone() }.onFailure { _saved.value = it.message }
    }
}

@Composable
fun RepoSettingsScreen(owner: String, repo: String, onBack: () -> Unit, onDeleted: () -> Unit, vm: RepoSettingsViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    ScreenScaffold("Repo settings", onBack) { m ->
        when (val st = s) {
            is UiState.Loading -> LoadingBox(m)
            is UiState.Error -> MessageBox("Couldn't load", st.message, m)
            is UiState.Content -> {
                val r = st.data
                var name by remember(r) { mutableStateOf(r.name) }
                var desc by remember(r) { mutableStateOf(r.description ?: "") }
                var isPrivate by remember(r) { mutableStateOf(r.isPrivate) }
                var archived by remember(r) { mutableStateOf(r.archived) }
                Column(m.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Private"); Switch(isPrivate, { isPrivate = it }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Archived"); Switch(archived, { archived = it }) }
                    Button(onClick = { vm.save(owner, repo, name, desc, isPrivate, archived) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                    OutlinedButton(onClick = { vm.delete(owner, repo, onDeleted) }, modifier = Modifier.fillMaxWidth()) { Text("Delete repository") }
                    saved?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
