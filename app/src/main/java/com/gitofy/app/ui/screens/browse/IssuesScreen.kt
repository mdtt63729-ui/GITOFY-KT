package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AssistChip
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
import com.gitofy.app.data.remote.dto.IssueDto
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
class IssuesViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<IssueDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<IssueDto>>> = _state.asStateFlow()
    fun load(o: String, r: String, state: String = "open") = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.issues(o, r, state) }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun IssuesScreen(owner: String, repo: String, onBack: () -> Unit, onOpenIssue: (Int) -> Unit = {}, vm: IssuesViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList(
        title = "Issues · $repo", state = s, onBack = onBack,
        onRetry = { vm.load(owner, repo) }, emptyTitle = "No open issues",
    ) { i ->
        ListItem(
            leadingContent = { Avatar(i.user?.avatarUrl, i.user?.login ?: "?") },
            headlineContent = { Text("#${i.number} ${i.title}", maxLines = 2) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(i.state) })
                    Text("${i.commentsCount} comments", Modifier.padding(top = 8.dp))
                }
            },
            modifier = androidx.compose.ui.Modifier.clickable { onOpenIssue(i.number) },
        )
    }
}
