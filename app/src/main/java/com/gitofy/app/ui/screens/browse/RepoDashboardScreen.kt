package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.Avatar
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.components.StatChip
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepoDashboardViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Repository>>(UiState.Loading)
    val state: StateFlow<UiState<Repository>> = _state.asStateFlow()
    fun load(o: String, r: String) = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.repo(o, r) }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun RepoDashboardScreen(owner: String, repo: String, onBack: () -> Unit, onNavigate: (String) -> Unit, vm: RepoDashboardViewModel = hiltViewModel()) {
    LaunchedEffect(owner, repo) { vm.load(owner, repo) }
    val s by vm.state.collectAsStateWithLifecycle()
    androidx.compose.material3.Scaffold(topBar = { GitofyTopBar("$owner/$repo", onBack) }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content -> {
                val r = st.data
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(r.owner?.avatarUrl, r.ownerLogin)
                                Column {
                                    Text(r.name, style = MaterialTheme.typography.titleLarge)
                                    Text(r.ownerLogin, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(r.description ?: "No description", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip("${r.stargazersCount}")
                                StatChip("${r.forksCount}")
                                r.language?.let { StatChip(it) }
                                StatChip(if (r.isPrivate) "private" else "public")
                            }
                        }
                    }
                    listOf(
                        Routes.REPO_FILES to "Code", Routes.REPO_COMMITS to "Commits",
                        Routes.BRANCH_MANAGER to "Branches", Routes.ISSUES to "Issues",
                        Routes.WORKFLOWS to "Workflows", Routes.RELEASE_CREATOR to "New release",
                        Routes.REPO_SETTINGS to "Settings",
                    ).forEach { (route, label) ->
                        FilledTonalButton(onClick = { onNavigate("$route/$owner/$repo") }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            }
        }
    }
}
