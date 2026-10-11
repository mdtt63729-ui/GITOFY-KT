package com.gitofy.native.ui.screens.browse

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gitofy.native.model.Repository
import com.gitofy.native.ui.components.PolishedList
import com.gitofy.native.ui.components.RepoCard
import com.gitofy.native.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Repository>>>(UiState.Content(emptyList()))
    val state: StateFlow<UiState<List<Repository>>> = _state.asStateFlow()
    fun search(q: String) {
        if (q.isBlank()) { _state.value = UiState.Content(emptyList()); return }
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.searchRepositories(q).items }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Search failed") }
        }
    }
}

@Composable
fun GlobalSearchScreen(onBack: () -> Unit, vm: SearchViewModel = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList(
        title = "Search", state = s, onBack = onBack,
        emptyTitle = "Search GitHub", emptyBody = "Find repositories by name, topic or owner.",
        header = {
            OutlinedTextField(
                value = q, onValueChange = { q = it; vm.search(it) },
                label = { Text("Search GitHub repositories") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
        },
    ) { r -> RepoCard(r) {} }
}
