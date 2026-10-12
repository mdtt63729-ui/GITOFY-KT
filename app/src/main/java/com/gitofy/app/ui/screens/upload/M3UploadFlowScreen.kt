package com.gitofy.app.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.ui.UploadState
import com.gitofy.app.ui.UploadViewModel
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.navigation.Routes

@Composable
fun M3UploadFlowScreen(onBack: () -> Unit, onNavigate: (String) -> Unit, vm: UploadViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var picked by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> picked = uri }

    ScreenScaffold("Upload project", onBack) { m ->
        Column(m.padding(16.dp), Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(owner, { owner = it }, label = { Text("Owner") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(repo, { repo = it }, label = { Text("Repository") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(branch, { branch = it }, label = { Text("Branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { picker.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (picked == null) "Pick ZIP" else "ZIP selected")
            }
            Button(
                onClick = { picked?.let { vm.uploadZip(owner, repo, branch, it, "Upload from Gitofy") } },
                enabled = picked != null && owner.isNotBlank() && repo.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Push to GitHub") }

            Button(
                onClick = { picked?.let { vm.uploadInBackground(owner, repo, branch, it, "Upload from Gitofy") } },
                enabled = picked != null && owner.isNotBlank() && repo.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Upload in background") }

            // Offer resume when a previous upload to this repo was interrupted.
            val resumable = owner.isNotBlank() && repo.isNotBlank() && vm.hasResumable(owner, repo)
            if (resumable) {
                OutlinedButton(
                    onClick = { vm.resume(owner, repo, "Upload from Gitofy") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Resume previous upload") }
            }

            if (pending.isNotEmpty()) {
                Text("Pending uploads", style = MaterialTheme.typography.titleMedium)
                pending.forEach { cp ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${cp.owner}/${cp.repo} · ${cp.stage}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { vm.resume(cp.owner, cp.repo, "Upload from Gitofy") }) { Text("Resume") }
                            TextButton(onClick = { vm.discard(cp.repo) }) { Text("Discard") }
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

            when (val s = state) {
                is UploadState.Idle -> Text("Bundled git · pack + HTTP/2 · thin push.")
                is UploadState.Running -> {
                    Text("Phase: ${s.phase}", style = MaterialTheme.typography.titleMedium)
                    s.percent?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
                    s.speed?.let { Text("Throughput: $it") }
                    Text(s.detail, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
                is UploadState.Done -> {
                    Text("Pushed to $owner/$repo ($branch).")
                    if (s.lfsFiles > 0) Text("${s.lfsFiles} file(s) went to Git LFS.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onNavigate(Routes.HOME) }) { Text("Done") }
                }
                is UploadState.Failed -> {
                    Text("Failed: ${s.message}", color = MaterialTheme.colorScheme.error)
                    if (s.resumable && owner.isNotBlank() && repo.isNotBlank()) {
                        OutlinedButton(
                            onClick = { vm.resume(owner, repo, "Upload from Gitofy") },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Resume") }
                    }
                }
            }
        }
    }
}
