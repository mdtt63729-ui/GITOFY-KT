package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.CreateReleaseRequest
import com.gitofy.app.ui.components.GitofyTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReleaseCreatorViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg.asStateFlow()
    fun create(o: String, r: String, tag: String, name: String, body: String, draft: Boolean, pre: Boolean) = viewModelScope.launch {
        runCatching { api.createRelease(o, r, CreateReleaseRequest(tag, name.ifBlank { tag }, body, draft, pre)) }
            .onSuccess { _msg.value = "Release ${it.tagName} created" }
            .onFailure { _msg.value = it.message ?: "Failed" }
    }
}

@Composable
fun ReleaseCreatorScreen(owner: String, repo: String, onBack: () -> Unit, vm: ReleaseCreatorViewModel = hiltViewModel()) {
    var tag by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var pre by remember { mutableStateOf(false) }
    val msg by vm.msg.collectAsStateWithLifecycle()

    Scaffold(topBar = { GitofyTopBar("New release · $repo", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(tag, { tag = it }, label = { Text("Tag (v1.2.3)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(title, { title = it }, label = { Text("Release title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Describe this release") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {}, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AttachFile, null)
                Text("  Attach a file")
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Draft", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Save it without publishing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(draft, { draft = it })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pre-release", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Mark it as not ready for everyone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(pre, { pre = it })
                    }
                }
            }
            Button(
                onClick = { vm.create(owner, repo, tag, title, notes, draft, pre) },
                enabled = tag.isNotBlank(), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(),
            ) { Text("Create release", fontWeight = FontWeight.Bold) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
