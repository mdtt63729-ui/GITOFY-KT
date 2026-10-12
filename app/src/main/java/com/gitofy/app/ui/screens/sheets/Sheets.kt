package com.gitofy.app.ui.screens.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepoSheet(onDismiss: () -> Unit, onCreate: (name: String, description: String, isPrivate: Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(12.dp)) {
            Text("New repository", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Private", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Only you can see it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(isPrivate, { isPrivate = it })
            }
            Button(onClick = { onCreate(name, desc, isPrivate) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoPickerSheet(repos: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(6.dp)) {
            Text("Pick a repository", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            repos.take(40).forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(r) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(null, r.substringAfter('/').take(1))
                    Text(r, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onUseAnother: () -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(6.dp)) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            accounts.forEach { (login, avatar) ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(login) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(avatar, login)
                    Column(Modifier.weight(1f)) {
                        Text("@$login", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Signed in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(
                Modifier.fillMaxWidth().clickable { onUseAnother() },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    "Use another account",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}
