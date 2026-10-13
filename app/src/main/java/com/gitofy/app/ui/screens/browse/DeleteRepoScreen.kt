package com.gitofy.app.ui.screens.browse

import com.gitofy.app.ui.components.BouncyButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.ScreenScaffold

/** Destructive-action confirmation (screens/security/DangerSheet). */
@Composable
fun DeleteRepoScreen(owner: String, repo: String, onBack: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    ScreenScaffold("Delete repository", onBack) { m ->
        Column(m.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This permanently deletes $owner/$repo.", color = MaterialTheme.colorScheme.error)
            Text("Type the repository name to confirm.", style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.OutlinedTextField(
                value = typed, onValueChange = { typed = it },
                label = { Text(repo) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            BouncyButton(
                onClick = onConfirm,
                enabled = typed == repo,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete forever") }
        }
    }
}
