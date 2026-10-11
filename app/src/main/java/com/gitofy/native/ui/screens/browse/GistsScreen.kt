package com.gitofy.native.ui.screens.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.native.data.remote.GitHubApiService
import com.gitofy.native.data.remote.dto.GistDto
import com.gitofy.native.ui.components.PolishedList
import com.gitofy.native.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GistsViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<GistDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<GistDto>>> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.gists() }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun GistsScreen(onBack: () -> Unit, vm: GistsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList("Gists", s, onBack, onRetry = { vm.load() }, emptyTitle = "No gists") { g ->
        ListItem(
            leadingContent = { Icon(Icons.Filled.Code, null) },
            headlineContent = { Text(g.description ?: g.id, maxLines = 1) },
            supportingContent = { Text("${g.files.size} file(s) • ${if (g.isPublic) "public" else "secret"}") },
        )
    }
}
