package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.JobDto
import com.gitofy.app.ui.components.LoadingBox
import com.gitofy.app.ui.components.MessageBox
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunDetailViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<JobDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<JobDto>>> = _state.asStateFlow()
    fun load(o: String, r: String, id: Long) = viewModelScope.launch {
        runCatching { api.jobs(o, r, id).jobs }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
}

@Composable
fun WorkflowRunDetailScreen(owner: String, repo: String, runId: Long, onBack: () -> Unit, onOpenLog: () -> Unit = {}, vm: RunDetailViewModel = hiltViewModel()) {
    androidx.compose.runtime.LaunchedEffect(runId) { vm.load(owner, repo, runId) }
    val s by vm.state.collectAsStateWithLifecycle()
    ScreenScaffold("Run #$runId", onBack) { m ->
        when (val st = s) {
            is UiState.Loading -> LoadingBox(m)
            is UiState.Error -> MessageBox("Couldn't load run", st.message, m)
            is UiState.Content -> Column(m.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) { Text("View live log") }
                st.data.forEach { job ->
                    Text(job.name, style = MaterialTheme.typography.titleMedium)
                    Text("${job.status} ${job.conclusion ?: ""}", style = MaterialTheme.typography.bodySmall)
                    job.steps.forEach { step -> Text("• ${step.number}. ${step.name} — ${step.conclusion ?: step.status}", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
