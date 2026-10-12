package com.gitofy.app.ui.screens.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
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
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.BranchDto
import com.gitofy.app.ui.components.PolishedList
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
    fun load(o: String, r: String) = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.branches(o, r) }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun BranchManagerScreen(owner: String, repo: String, onBack: () -> Unit, vm: BranchesViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList("Branches · $repo", s, onBack, onRetry = { vm.load(owner, repo) }, emptyTitle = "No branches") { b ->
        ListItem(
            leadingContent = { Icon(Icons.Filled.CallSplit, null) },
            headlineContent = { Text(b.name) },
            supportingContent = { Text(b.commit.sha.take(7)) },
        )
    }
}
