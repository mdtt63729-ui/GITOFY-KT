package com.gitofy.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.ui.navigation.Routes

@Composable
fun LoginScreen(onNavigate: (String) -> Unit, vm: LoginViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var pat by remember { mutableStateOf("") }

    LaunchedEffect(ui) { if (ui is LoginUi.Success) onNavigate(Routes.HOME) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text("Login with GitHub", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text("No secret ships in the app — device flow only.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))

        when (val s = ui) {
            LoginUi.Idle -> Button(onClick = { vm.startDeviceFlow() }, modifier = Modifier.fillMaxWidth()) {
                Text("Start device login")
            }
            is LoginUi.Waiting -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter this code on GitHub", style = MaterialTheme.typography.titleMedium)
                    Text(s.code.userCode, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(s.code.verificationUri, style = MaterialTheme.typography.bodySmall)
                    CircularProgressIndicator()
                    Text("${s.note} (${s.secondsLeft}s)", style = MaterialTheme.typography.bodySmall)
                }
            }
            LoginUi.Success -> Text("Signed in. Opening…", textAlign = TextAlign.Center)
            is LoginUi.Failed -> Text("Failed: ${s.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(26.dp))
        Text("Or use a Personal Access Token", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pat, onValueChange = { pat = it },
            label = { Text("ghp_…") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.signInWithPat(pat) }, enabled = pat.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Sign in with token")
        }
    }
}
