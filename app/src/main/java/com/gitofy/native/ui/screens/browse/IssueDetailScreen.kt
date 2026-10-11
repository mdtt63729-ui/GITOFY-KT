package com.gitofy.native.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.native.data.remote.GitHubApiService
import com.gitofy.native.data.remote.dto.CommentRequest
import com.gitofy.native.data.remote.dto.IssueCommentDto
import com.gitofy.native.data.remote.dto.IssueDto
import com.gitofy.native.data.remote.dto.UpdateIssueRequest
import com.gitofy.native.ui.components.LoadingBox
import com.gitofy.native.ui.components.MessageBox
import com.gitofy.native.ui.components.ScreenScaffold
import com.gitofy.native.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IssueDetailViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _issue = MutableStateFlow<UiState<IssueDto>>(UiState.Loading)
    val issue: StateFlow<UiState<IssueDto>> = _issue.asStateFlow()
    private val _comments = MutableStateFlow<List<IssueCommentDto>>(emptyList())
    val comments: StateFlow<List<IssueCommentDto>> = _comments.asStateFlow()
    private var owner = ""; private var repo = ""; private var number = 0

    fun load(o: String, r: String, n: Int) {
        owner = o; repo = r; number = n
        viewModelScope.launch {
            runCatching { api.issues(o, r, "all").first { it.number == n } }
                .onSuccess { _issue.value = UiState.Content(it) }
                .onFailure { _issue.value = UiState.Error(it.message ?: "Failed") }
            _comments.value = runCatching { api.issueComments(o, r, n) }.getOrDefault(emptyList())
        }
    }

    fun comment(body: String) = viewModelScope.launch {
        runCatching { api.addIssueComment(owner, repo, number, CommentRequest(body)) }
            .onSuccess { load(owner, repo, number) }
    }

    fun toggle(state: String) = viewModelScope.launch {
        runCatching { api.setIssueState(owner, repo, number, UpdateIssueRequest(state)) }
            .onSuccess { load(owner, repo, number) }
    }
}

@Composable
fun IssueDetailScreen(owner: String, repo: String, number: Int, onBack: () -> Unit, vm: IssueDetailViewModel = hiltViewModel()) {
    LaunchedEffect(number) { vm.load(owner, repo, number) }
    val issue by vm.issue.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    var body by remember { mutableStateOf("") }

    ScreenScaffold("#$number", onBack) { m ->
        when (val s = issue) {
            is UiState.Loading -> LoadingBox(m)
            is UiState.Error -> MessageBox("Couldn't load issue", s.message, m)
            is UiState.Content -> {
                val i = s.data
                LazyColumn(m.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(i.title, style = MaterialTheme.typography.titleLarge)
                            Text("${i.state} • ${i.user?.login ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            i.body?.let { Text(it) }
                            OutlinedButton(onClick = { vm.toggle(if (i.state == "open") "closed" else "open") }) {
                                Text(if (i.state == "open") "Close issue" else "Reopen issue")
                            }
                        }
                    }
                    items(comments) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(c.user?.login ?: "", style = MaterialTheme.typography.labelMedium)
                                Text(c.body)
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(body, { body = it }, label = { Text("Add a comment") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { if (body.isNotBlank()) { vm.comment(body); body = "" } }, modifier = Modifier.fillMaxWidth()) {
                                Text("Comment")
                            }
                        }
                    }
                }
            }
        }
    }
}
