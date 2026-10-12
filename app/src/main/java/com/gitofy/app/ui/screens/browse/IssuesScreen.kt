package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import com.gitofy.app.ui.components.pressClickable
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
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.gitofy.app.data.remote.dto.CreateIssueRequest
import com.gitofy.app.data.remote.dto.IssueDto
import com.gitofy.app.data.remote.dto.MergePullRequest
import com.gitofy.app.ui.components.Avatar
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
class IssuesViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<IssueDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<IssueDto>>> = _state.asStateFlow()
    private var owner = ""; private var repo = ""

    fun load(o: String, r: String, state: String = "open") {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.issues(o, r, state) }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    fun create(title: String, body: String) = viewModelScope.launch {
        runCatching { api.createIssue(owner, repo, CreateIssueRequest(title, body.ifBlank { null })) }
            .onSuccess { load(owner, repo) }
    }

    fun merge(number: Int) = viewModelScope.launch {
        runCatching { api.mergePull(owner, repo, number, MergePullRequest()) }.onSuccess { load(owner, repo) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(owner: String, repo: String, onBack: () -> Unit, onOpenIssue: (Int) -> Unit = {}, vm: IssuesViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        GitofyTopBar("Issues · $repo", onBack, actions = {
            IconButton(onClick = { sheet = true }) { Icon(Icons.Filled.Add, "New issue") }
        })
    }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content ->
                if (st.data.isEmpty()) EmptyState("No open issues", "Create one with the + button.", icon = Icons.Filled.Merge, modifier = Modifier.fillMaxSize().padding(padding))
                else LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(st.data, key = { it.id }) { i ->
                        val isPr = i.pullRequest != null
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().animateItem().pressClickable { onOpenIssue(i.number) },
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(i.user?.avatarUrl, i.user?.login ?: "?")
                                Column(Modifier.weight(1f)) {
                                    Text("#${i.number} ${i.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${if (isPr) "pull request" else "issue"} · ${i.state} · ${i.commentsCount} comments",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isPr) {
                                    OutlinedButton(onClick = { vm.merge(i.number) }) { Text("Merge") }
                                }
                            }
                        }
                    }
                }
        }
    }

    if (sheet) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New issue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { sheet = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { vm.create(title, body); sheet = false },
                        enabled = title.isNotBlank(), modifier = Modifier.weight(1f),
                    ) { Text("Create") }
                }
            }
        }
    }
}
