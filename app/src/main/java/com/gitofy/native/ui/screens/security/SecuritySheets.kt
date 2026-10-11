package com.gitofy.native.ui.screens.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The "unofficial app" danger sheet. In the web app this was a red M3 sheet with
 * ripples, danger rings and a klaxon. Here it is the same red M3 surface, and the
 * danger sound/rings are added in the motion-polish phase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerSheet(onDismiss: () -> Unit, onDownloadOfficial: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp), Arrangement.spacedBy(12.dp)) {
            Text("This is an unofficial app", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFC52A3A))
            Text("The app signature does not match the official release. Download the official build.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onDownloadOfficial, modifier = Modifier.fillMaxWidth()) { Text("Download official app") }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Later") }
        }
    }
}

/** Full-screen blocking overlay shown while the integrity check is failing. */
@Composable
fun SecurityOverlay(message: String, onDismiss: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Security check failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onDismiss != null) Button(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/** The update sheet (sec.update.*) used by the updater. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(version: String, fileName: String, onUpdate: () -> Unit, onLater: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onLater) {
        Column(Modifier.fillMaxWidth().padding(24.dp), Arrangement.spacedBy(12.dp)) {
            Text("A new version is ready", style = MaterialTheme.typography.headlineSmall)
            Text("$version available", style = MaterialTheme.typography.titleMedium)
            Text("File: $fileName", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) { Text("Update") }
            OutlinedButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) { Text("Later") }
        }
    }
}
