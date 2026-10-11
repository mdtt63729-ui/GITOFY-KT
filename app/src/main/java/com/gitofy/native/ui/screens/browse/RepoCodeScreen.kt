package com.gitofy.native.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
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
import com.gitofy.native.data.remote.dto.ContentDto
import com.gitofy.native.ui.components.PolishedList
import com.gitofy.native.ui.state.UiState
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
    val title = if (path.isBlank()) repo else path.substringAfterLast('/')
    PolishedList(
        title = title, state = s, onBack = onBack,
        onRetry = { vm.load(owner, repo, path) },
        emptyTitle = "Empty directory",
    ) { c ->
        ListItem(
            leadingContent = { androidx.compose.material3.Icon(if (c.type == "dir") Icons.Filled.Folder else Icons.Filled.Description, null) },
            headlineContent = { Text(if (c.type == "dir") "${c.name}/" else c.name) },
            supportingContent = { Text(if (c.type == "dir") "directory" else "${c.size} bytes") },
            modifier = androidx.compose.ui.Modifier.clickable { onOpen(c.path, c.type == "dir") },
        )
    }
}
