package com.gitofy.native.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.native.data.remote.GitHubApiService
import com.gitofy.native.data.remote.dto.JobDto
import com.gitofy.native.ui.components.LoadingBox
import com.gitofy.native.ui.components.MessageBox
import com.gitofy.native.ui.components.ScreenScaffold
import com.gitofy.native.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunLogViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _jobs = MutableStateFlow<List<JobDto>>(emptyList())
    val jobs: StateFlow<List<JobDto>> = _jobs.asStateFlow()
    private val _log = MutableStateFlow<UiState<String>>(UiState.Loading)
    val log: StateFlow<UiState<String>> = _log.asStateFlow()
    private var owner = ""; private var repo = ""; private var runId = 0L; private var selected: Long? = null

    fun start(o: String, r: String, id: Long, pollMs: Long = 5000) {
        owner = o; repo = r; runId = id
        viewModelScope.launch {
            while (true) {
                val js = runCatching { api.jobs(owner, repo, runId).jobs }.getOrDefault(emptyList())
                _jobs.value = js
                val target = selected ?: js.firstOrNull()?.id
                if (target != null) loadLog(target)
                delay(pollMs)
            }
        }
    }

    fun select(jobId: Long) { selected = jobId; viewModelScope.launch { loadLog(jobId) } }

    private suspend fun loadLog(jobId: Long) {
        runCatching { api.jobLogs(owner, repo, jobId).string() }
            .onSuccess { _log.value = UiState.Content(it.takeLast(20000)) }
            .onFailure { _log.value = UiState.Error(it.message ?: "No log") }
    }
}

@Composable
fun RunLogScreen(owner: String, repo: String, runId: Long, onBack: () -> Unit, vm: RunLogViewModel = hiltViewModel()) {
    LaunchedEffect(runId) { vm.start(owner, repo, runId) }
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Long?>(null) }

    ScreenScaffold("Live log · run #$runId", onBack) { m ->
        Column(m.fillMaxSize().padding(12.dp), Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                jobs.forEach { j ->
                    FilterChip(selected == j.id, { selected = j.id; vm.select(j.id) }, label = { Text(j.name) })
                }
            }
            when (val s = log) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> MessageBox("Log unavailable", s.message)
                is UiState.Content -> Text(
                    s.data,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
