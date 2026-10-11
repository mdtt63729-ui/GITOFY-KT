package com.gitofy.native.ui.screens.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepoSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(12.dp)) {
            Text("New repository", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) { Text("Private"); Switch(isPrivate, { isPrivate = it }) }
            Button(onClick = { onCreate(name, desc, isPrivate) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoPickerSheet(
    repos: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(8.dp)) {
            Text("Pick a repository", style = MaterialTheme.typography.titleLarge)
            repos.take(30).forEach { r ->
                Button(onClick = { onPick(r) }, modifier = Modifier.fillMaxWidth()) { Text(r) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), Arrangement.spacedBy(8.dp)) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge)
            accounts.forEach { a ->
                Button(onClick = { onPick(a) }, modifier = Modifier.fillMaxWidth()) { Text(a) }
            }
        }
    }
}
