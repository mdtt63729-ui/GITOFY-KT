package com.gitofy.native.ui.screens.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.native.ui.UploadState
import com.gitofy.native.ui.UploadViewModel
import com.gitofy.native.ui.components.ScreenScaffold

@Composable
fun MultiRepoUploadScreen(onBack: () -> Unit, vm: UploadViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var targets by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked = it }

    ScreenScaffold("Multi-repo upload", onBack) { m ->
        Column(m.padding(16.dp), Arrangement.spacedBy(12.dp)) {
            Text("One target per line: owner/repo")
            OutlinedTextField(
                value = targets, onValueChange = { targets = it },
                label = { Text("owner/repo") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { picker.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (picked == null) "Pick ZIP" else "ZIP selected")
            }
            Button(
                onClick = {
                    val uri = picked ?: return@Button
                    targets.lineSequence().map { it.trim() }.filter { it.contains('/') }.forEach { line ->
                        val parts = line.split('/', limit = 2)
                        vm.uploadZip(parts[0], parts[1], "main", uri, "Upload from Gitofy")
                    }
                },
                enabled = picked != null && targets.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Upload to all") }
            (state as? UploadState.Running)?.let { Text("${it.phase} ${it.percent ?: 0}% ${it.detail}") }
        }
    }
}
