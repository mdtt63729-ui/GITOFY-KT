package com.gitofy.native.ui.screens.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.gitofy.native.ui.components.ScreenScaffold

@Composable
fun GitHubSettingsScreen(onBack: () -> Unit, onSignedOut: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    var pat by remember { mutableStateOf("") }
    val s by vm.settings.collectAsStateWithLifecycle()
    ScreenScaffold("GitHub settings", onBack) { m ->
        Column(m.padding(16.dp), Arrangement.spacedBy(12.dp)) {
            Text("Signed in as ${s.githubUsername.ifBlank { "—" }}", style = MaterialTheme.typography.titleMedium)
            Text("Client id: ${com.gitofy.native.auth.AuthConfig.CLIENT_ID}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(pat, { pat = it }, label = { Text("New Personal Access Token") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = onSignedOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        }
    }
}
