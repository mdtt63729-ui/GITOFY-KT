package com.gitofy.app.ui.screens.upload

import com.gitofy.app.ui.components.BouncyButton
import com.gitofy.app.ui.components.BouncyOutlinedButton
import com.gitofy.app.ui.components.BouncyTextButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.git.GitProgress
import com.gitofy.app.ui.UploadState
import com.gitofy.app.ui.UploadViewModel
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.util.formatSize

private val STEPS = listOf("Reading archive", "Extracting files", "Indexing & hashing", "Comparing with GitHub", "Ready")

private fun stepFor(phase: GitProgress.Phase, detail: String): Int = when {
    detail.contains("Clon", true) -> 0
    detail.contains("Extract", true) -> 1
    phase == GitProgress.Phase.COUNTING || phase == GitProgress.Phase.COMPRESSING -> 2
    phase == GitProgress.Phase.UPLOADING -> 3
    phase == GitProgress.Phase.WRITING || phase == GitProgress.Phase.RECEIVING || phase == GitProgress.Phase.RESOLVING -> 4
    detail.contains("Tuned", true) -> 2
    else -> 2
}

@Composable
fun M3UploadFlowScreen(onBack: () -> Unit, onNavigate: (String) -> Unit, vm: UploadViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked = it }

    ScreenScaffold("Upload project", onBack) { m ->
        Column(
            m.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Repository / Branch
            OutlinedTextField(owner, { owner = it }, label = { Text("Repository owner") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(repo, { repo = it }, label = { Text("Repository") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(branch, { branch = it }, label = { Text("Branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            BouncyOutlinedButton(onClick = { picker.launch("application/zip") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                Text(if (picked == null) "Pick ZIP file" else "ZIP selected ✓", fontWeight = FontWeight.Bold)
            }

            BouncyButton(
                onClick = { picked?.let { vm.uploadZip(owner, repo, branch, it, "Upload from Gitofy") } },
                enabled = picked != null && owner.isNotBlank() && repo.isNotBlank(),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Push to GitHub", fontWeight = FontWeight.Bold) }

            BouncyButton(
                onClick = { picked?.let { vm.uploadInBackground(owner, repo, branch, it, "Upload from Gitofy") } },
                enabled = picked != null && owner.isNotBlank() && repo.isNotBlank(),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Upload in background") }

            val resumable = owner.isNotBlank() && repo.isNotBlank() && vm.hasResumable(owner, repo)
            if (resumable) {
                BouncyOutlinedButton(onClick = { vm.resume(owner, repo, "Upload from Gitofy") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                    Text("Resume previous upload")
                }
            }

            if (pending.isNotEmpty()) {
                Text("Pending uploads", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                pending.forEach { cp ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${cp.owner}/${cp.repo} · ${cp.stage}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                BouncyTextButton(onClick = { vm.resume(cp.owner, cp.repo, "Upload from Gitofy") }) { Text("Resume") }
                                BouncyTextButton(onClick = { vm.discard(cp.repo) }) { Text("Discard") }
                            }
                            if (cp.remainingText.isNotEmpty()) {
                                Text(cp.remainingText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (cp.lfsTotalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { (cp.lfsDoneBytes.toFloat() / cp.lfsTotalBytes).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            when (val s = state) {
                is UploadState.Idle -> {
                    Text(
                        "Your local ZIP file is safe.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is UploadState.Running -> {
                    PipelineStepper(stepFor(s.phase, s.detail))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(phaseLabel(s), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (s.percent != null) {
                                LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                                Text("${s.percent}%", style = MaterialTheme.typography.labelSmall)
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            if (s.totalBytes > 0) StatRow("Data Size", "${formatSize(s.doneBytes)} / ${formatSize(s.totalBytes)}")
                            s.speed?.let { StatRow("Network Speed", it) }
                            Text(s.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                    }
                    BouncyOutlinedButton(onClick = { vm.cancelBackground(repo) }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel upload")
                    }
                }
                is UploadState.Done -> {
                    PipelineStepper(STEPS.size)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Upload complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Branch: ${s.branch}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            if (s.lfsFiles > 0) Text("${s.lfsFiles} file(s) went to Git LFS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    BouncyButton(onClick = { onNavigate(Routes.HOME) }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
                is UploadState.Failed -> {
                    Text("Failed: ${s.message}", color = MaterialTheme.colorScheme.error)
                    if (s.resumable && owner.isNotBlank() && repo.isNotBlank()) {
                        BouncyOutlinedButton(onClick = { vm.resume(owner, repo, "Upload from Gitofy") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                            Text("Resume")
                        }
                    }
                }
            }
        }
    }
}

private fun phaseLabel(s: UploadState.Running): String {
    val d = s.detail
    return when {
        d.isNotBlank() && !d.startsWith("LFS ") -> d
        s.phase == GitProgress.Phase.UPLOADING -> "Uploading changes…"
        s.phase == GitProgress.Phase.WRITING -> "Pushing to GitHub…"
        s.phase == GitProgress.Phase.RECEIVING -> "Comparing with GitHub…"
        s.phase == GitProgress.Phase.COUNTING -> "Indexing files…"
        s.phase == GitProgress.Phase.COMPRESSING -> "Preparing Git…"
        else -> "Uploading changes…"
    }
}

@Composable
private fun PipelineStepper(currentStep: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        STEPS.forEachIndexed { i, label ->
            val done = i < currentStep
            val active = i == currentStep
            val tint = if (done || active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(
                            when {
                                done -> MaterialTheme.colorScheme.primary
                                active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                    } else if (active) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
