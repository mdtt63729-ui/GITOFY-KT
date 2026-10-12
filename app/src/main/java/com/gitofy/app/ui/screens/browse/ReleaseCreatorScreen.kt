package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.CreateReleaseRequest
import com.gitofy.app.ui.components.ScreenScaffold
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
    fun create(o: String, r: String, tag: String, name: String, body: String, draft: Boolean, prerelease: Boolean) = viewModelScope.launch {
        runCatching { api.createRelease(o, r, CreateReleaseRequest(tag, name.ifBlank { tag }, body, draft, prerelease)) }
            .onSuccess { _msg.value = "Release ${it.tagName} created" }
            .onFailure { _msg.value = it.message ?: "Failed" }
    }
}

@Composable
fun ReleaseCreatorScreen(owner: String, repo: String, onBack: () -> Unit, vm: ReleaseCreatorViewModel = hiltViewModel()) {
    var tag by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var pre by remember { mutableStateOf(false) }
    val msg by vm.msg.collectAsStateWithLifecycle()
    ScreenScaffold("New release · $repo", onBack) { m ->
        Column(m.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(tag, { tag = it }, label = { Text("Tag (v1.2.3)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Draft"); Switch(draft, { draft = it }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Pre-release"); Switch(pre, { pre = it }) }
            Button(onClick = { vm.create(owner, repo, tag, name, body, draft, pre) }, enabled = tag.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create release") }
            msg?.let { Text(it) }
        }
    }
}
