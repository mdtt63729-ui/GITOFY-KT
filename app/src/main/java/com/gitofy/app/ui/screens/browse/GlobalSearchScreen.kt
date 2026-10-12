package com.gitofy.app.ui.screens.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gitofy.app.data.remote.dto.CodeItem
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.RepoCard
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _repos = MutableStateFlow<UiState<List<Repository>>>(UiState.Content(emptyList()))
    val repos: StateFlow<UiState<List<Repository>>> = _repos.asStateFlow()
    private val _code = MutableStateFlow<UiState<List<CodeItem>>>(UiState.Content(emptyList()))
    val code: StateFlow<UiState<List<CodeItem>>> = _code.asStateFlow()

    fun searchRepos(q: String) {
        if (q.isBlank()) { _repos.value = UiState.Content(emptyList()); return }
        viewModelScope.launch {
            _repos.value = UiState.Loading
            runCatching { api.searchRepositories(q).items }
                .onSuccess { _repos.value = UiState.Content(it) }
                .onFailure { _repos.value = UiState.Error(it.message ?: "Search failed") }
        }
    }

    fun searchCode(q: String) {
        if (q.isBlank()) { _code.value = UiState.Content(emptyList()); return }
        viewModelScope.launch {
            _code.value = UiState.Loading
            runCatching { api.searchCode(q).items }
                .onSuccess { _code.value = UiState.Content(it) }
                .onFailure { _code.value = UiState.Error(it.message ?: "Search failed") }
        }
    }
}

@Composable
fun GlobalSearchScreen(onBack: () -> Unit, vm: SearchViewModel = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val repos by vm.repos.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()

    Scaffold(topBar = { GitofyTopBar("Search", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it; if (tab == 0) vm.searchRepos(it) else vm.searchCode(it) },
                label = { Text(if (tab == 0) "Search repositories" else "Search code") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tab == 0, { tab = 0; vm.searchRepos(q) }, label = { Text("Repositories") })
                FilterChip(tab == 1, { tab = 1; vm.searchCode(q) }, label = { Text("Code") })
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(androidx.compose.animation.core.tween(220)) togetherWith fadeOut(androidx.compose.animation.core.tween(160)) },
                label = "searchTab",
            ) { t ->
            if (t == 0) {
                when (val s = repos) {
                    is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                    is UiState.Error -> ErrorState(s.message, modifier = Modifier.fillMaxSize())
                    is UiState.Content ->
                        if (s.data.isEmpty()) EmptyState("Search GitHub", "Find repositories by name, topic or owner.", modifier = Modifier.fillMaxSize())
                        else LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) { items(s.data, key = { it.id }) { r -> RepoCard(r) {} } }
                }
            } else {
                when (val s = code) {
                    is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                    is UiState.Error -> ErrorState(s.message, modifier = Modifier.fillMaxSize())
                    is UiState.Content ->
                        if (s.data.isEmpty()) EmptyState("Search code", "Find files across GitHub.", modifier = Modifier.fillMaxSize())
                        else LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.data) { c ->
                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${c.repository.fullName} · ${c.path}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                }
            }
            }
        }
    }
}
