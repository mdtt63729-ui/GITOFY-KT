package com.gitofy.native.ui.screens.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.native.data.remote.GitHubApiService
import com.gitofy.native.model.WorkflowItem
import com.gitofy.native.ui.components.PolishedList
import com.gitofy.native.ui.state.UiState
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
    fun load(o: String, r: String) = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.workflows(o, r).workflows }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun WorkflowsScreen(owner: String, repo: String, onBack: () -> Unit, vm: WorkflowsViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList("Workflows · $repo", s, onBack, onRetry = { vm.load(owner, repo) }, emptyTitle = "No workflows") { w ->
        ListItem(
            leadingContent = { Icon(Icons.Filled.PlayArrow, null) },
            headlineContent = { Text(w.name) },
            supportingContent = { Text(w.path) },
        )
    }
}
