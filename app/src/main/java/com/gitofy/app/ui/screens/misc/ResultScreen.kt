package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.ScreenScaffold

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    success: Boolean,
    sha: String,
    files: Int,
    bytes: Long,
    speed: String,
) {
    ScreenScaffold("Result", onBack) { m ->
        Column(m.padding(20.dp), Arrangement.spacedBy(8.dp)) {
            Text(if (success) "Upload complete" else "Upload failed",
                style = MaterialTheme.typography.titleLarge,
                color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            if (success) {
                Text("Commit: ${sha.take(12)}")
                Text("Files: $files")
                Text("Bytes: $bytes")
                Text("Speed: $speed")
            }
        }
    }
}
