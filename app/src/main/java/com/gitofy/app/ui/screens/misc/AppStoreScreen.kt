package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.util.formatSize
import com.gitofy.app.util.formatSpeed

@Composable
fun AppStoreScreen(onBack: () -> Unit, vm: AppStoreViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ScreenScaffold("Updates", onBack) { m ->
        Column(m.padding(16.dp), Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.SystemUpdate, null)
                    Column {
                        Text("Gitofy", style = MaterialTheme.typography.titleMedium)
                        Text("Installed ${vm.currentVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            when (val s = ui) {
                UpdateUi.Idle -> Button(onClick = { vm.check() }, modifier = Modifier.fillMaxWidth()) { Text("Check for updates") }
                UpdateUi.Checking -> Text("Checking…")
                UpdateUi.UpToDate -> Text("You're on the latest version.", color = MaterialTheme.colorScheme.primary)
                is UpdateUi.Available -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
                        Text("Version ${s.info.version} is ready", style = MaterialTheme.typography.titleLarge)
                        Text(s.info.notes.take(500), style = MaterialTheme.typography.bodySmall)
                        Text("File: ${s.info.apkName}", style = MaterialTheme.typography.labelSmall)
                        Button(onClick = { vm.update(s.info, null) }, modifier = Modifier.fillMaxWidth()) { Text("Update") }
                        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Later") }
                    }
                }
                is UpdateUi.Downloading -> {
                    Text("Downloading ${s.progress.percent}%")
                    LinearProgressIndicator(progress = { s.progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${formatSize(s.progress.received)} / ${formatSize(s.progress.total)} • ${formatSpeed(s.progress.speedBps)}",
                        style = MaterialTheme.typography.bodySmall)
                }
                UpdateUi.Installing -> Text("Installing…")
                is UpdateUi.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
