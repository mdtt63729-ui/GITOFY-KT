package com.gitofy.app.ui.screens.browse

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.JobDto
import com.gitofy.app.model.WorkflowRun
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
class RunDetailViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<WorkflowRun>>(UiState.Loading)
    val state: StateFlow<UiState<WorkflowRun>> = _state.asStateFlow()
    private val _jobs = MutableStateFlow<List<JobDto>>(emptyList())
    val jobs: StateFlow<List<JobDto>> = _jobs.asStateFlow()
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()
    private var owner = ""; private var repo = ""

    fun load(o: String, r: String, id: Long) {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.run(o, r, id) }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
            val jobs = runCatching { api.jobs(o, r, id).jobs }.getOrDefault(emptyList())
            _jobs.value = jobs
            jobs.firstOrNull()?.let { j ->
                _log.value = runCatching { api.jobLogs(o, r, j.id).string() }.getOrDefault("")
            }
        }
    }
}

@Composable
fun WorkflowRunDetailScreen(owner: String, repo: String, runId: Long, onBack: () -> Unit, onOpenLog: () -> Unit = {}, vm: RunDetailViewModel = hiltViewModel()) {
    LaunchedEffect(runId) { vm.load(owner, repo, runId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = {
        GitofyTopBar("Run #$runId", onBack, actions = {
            IconButton(onClick = {
                val url = (s as? UiState.Content)?.data?.htmlUrl
                if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
            }) { Icon(Icons.Filled.Download, "Download") }
            IconButton(onClick = {
                val url = (s as? UiState.Content)?.data?.htmlUrl ?: ""
                runCatching {
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }
                    context.startActivity(Intent.createChooser(i, "Share"))
                }
            }) { Icon(Icons.Filled.Share, "Share") }
        })
    }) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.load(owner, repo, runId) }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val run = st.data
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(run.name.ifBlank { "Run #${run.runNumber}" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${run.status} ${run.conclusion ?: ""} • ${run.headBranch ?: ""} • ${run.event}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = onOpenLog, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) { Text("View live log") }

                Text("JOBS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                jobs.forEach { job ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${job.status} ${job.conclusion ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            job.steps.forEach { step ->
                                Text("• ${step.number}. ${step.name} — ${step.conclusion ?: step.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (log.isNotBlank()) {
                    Text("GitHub log · adaptive tail", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    val tail = log.lines().takeLast(40)
                    val firstError = log.lines().firstOrNull { it.contains("error", true) && it.contains("##[error]", true) } ?: log.lines().firstOrNull { it.contains("##[error]") }
                    if (firstError != null) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("First error", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(firstError, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            tail.forEach { line ->
                                Text(line, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
