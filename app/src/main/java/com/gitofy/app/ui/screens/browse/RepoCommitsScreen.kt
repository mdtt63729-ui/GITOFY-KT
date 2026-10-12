package com.gitofy.app.ui.screens.browse

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
import com.gitofy.app.data.remote.dto.CommitDto
import com.gitofy.app.ui.components.Avatar
import com.gitofy.app.ui.components.PolishedList
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitsViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<CommitDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CommitDto>>> = _state.asStateFlow()
    fun load(o: String, r: String, sha: String? = null) = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.commits(o, r, sha) }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun RepoCommitsScreen(owner: String, repo: String, onBack: () -> Unit, vm: CommitsViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList("Commits · $repo", s, onBack, onRetry = { vm.load(owner, repo) }, emptyTitle = "No commits") { c ->
        ListItem(
            leadingContent = { Avatar(c.author?.avatarUrl, c.commit.author.name) },
            headlineContent = { Text(c.commit.message.lineSequence().first(), maxLines = 2) },
            supportingContent = { Text("${c.sha.take(7)} • ${c.commit.author.name}") },
        )
    }
}
