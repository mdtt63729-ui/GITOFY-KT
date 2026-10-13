package com.gitofy.app.ui.screens.browse

import com.gitofy.app.ui.components.BouncyIconButton
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.JobDto
import com.gitofy.app.data.remote.dto.StepDto
import com.gitofy.app.model.WorkflowRun
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.components.pressClickable
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
    private var owner = ""; private var repo = ""

    fun load(o: String, r: String, id: Long) {
        owner = o; repo = r
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { api.run(o, r, id) }
                .onSuccess { _state.value = UiState.Content(it) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
            _jobs.value = runCatching { api.jobs(o, r, id).jobs }.getOrDefault(emptyList())
        }
    }
}

@Composable
fun WorkflowRunDetailScreen(owner: String, repo: String, runId: Long, onBack: () -> Unit, onOpenJob: (Long) -> Unit = {}, vm: RunDetailViewModel = hiltViewModel()) {
    LaunchedEffect(runId) { vm.load(owner, repo, runId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = {
        GitofyTopBar("Run #$runId", onBack, actions = {
            BouncyIconButton(onClick = {
                val url = (s as? UiState.Content)?.data?.htmlUrl
                if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
            }) { Icon(Icons.Filled.Download, "Download") }
            BouncyIconButton(onClick = {
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
                Text("JOBS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                jobs.forEach { job ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().pressClickable { onOpenJob(job.id) },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val (jIcon, jTint) = statusIcon(job.status, job.conclusion)
                                Icon(jIcon, contentDescription = null, tint = jTint, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        job.status + (job.conclusion?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (job.steps.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            job.steps.forEach { step -> StepRow(step) }
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun StepRow(step: StepDto) {
    val (icon, tint) = statusIcon(step.status, step.conclusion)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                step.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                step.conclusion ?: step.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (step.conclusion == "failure") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun statusIcon(status: String, conclusion: String?): Pair<ImageVector, Color> = when {
    conclusion == "success" -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
    conclusion == "failure" || conclusion == "timed_out" || conclusion == "cancelled" -> Icons.Filled.Cancel to MaterialTheme.colorScheme.error
    conclusion == "skipped" -> Icons.Filled.RemoveCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant
    status == "in_progress" || status == "queued" -> Icons.Filled.Autorenew to MaterialTheme.colorScheme.tertiary
    else -> Icons.Outlined.Circle to MaterialTheme.colorScheme.onSurfaceVariant
}
