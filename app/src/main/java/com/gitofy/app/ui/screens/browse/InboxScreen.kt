package com.gitofy.app.ui.screens.browse

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.NotificationDto
import com.gitofy.app.ui.components.PolishedList
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NotificationDto>>> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.notifications() }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun InboxScreen(onBack: () -> Unit, vm: InboxViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    PolishedList("Inbox", s, onBack, onRetry = { vm.load() }, emptyTitle = "You're all caught up") { n ->
        ListItem(
            headlineContent = { Text(n.subject.title, maxLines = 2) },
            supportingContent = { Text("${n.repository?.fullName ?: ""} • ${n.reason}") },
        )
    }
}
