package com.gitofy.app.ui.screens.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.ScreenScaffold

@Composable
fun LoginDiagnosticsScreen(onBack: () -> Unit, clientId: String, signedIn: Boolean) {
    ScreenScaffold("Login diagnostics", onBack) { m ->
        Column(m.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Client id", style = MaterialTheme.typography.titleMedium)
            Text(clientId, style = MaterialTheme.typography.bodySmall)
            Text("Signed in", style = MaterialTheme.typography.titleMedium)
            Text(if (signedIn) "yes" else "no")
            Text("Transport", style = MaterialTheme.typography.titleMedium)
            Text("Device Flow (native OkHttp, no secret in the app).")
        }
    }
}
