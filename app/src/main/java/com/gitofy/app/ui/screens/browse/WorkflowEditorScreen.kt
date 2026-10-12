package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.UpdateFileRequest
import com.gitofy.app.ui.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Base64
import javax.inject.Inject

@HiltViewModel
class WorkflowEditorViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()
    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg.asStateFlow()
    private var owner = ""; private var repo = ""; private var path = ""; private var sha: String? = null

    fun load(o: String, r: String, p: String) {
        owner = o; repo = r; path = p
        viewModelScope.launch {
            runCatching { api.file(o, r, p) }
                .onSuccess {
                    sha = it.sha
                    _text.value = it.content?.let { c -> String(Base64.decode(c.replace("\n", ""), Base64.DEFAULT)) } ?: ""
                }
                .onFailure { _msg.value = it.message }
        }
    }

    fun save(message: String) = viewModelScope.launch {
        val encoded = Base64.encodeToString(_text.value.toByteArray(), Base64.NO_WRAP)
        runCatching { api.updateFile(owner, repo, path, UpdateFileRequest(message, encoded, sha)) }
            .onSuccess { _msg.value = "Saved" }
            .onFailure { _msg.value = it.message ?: "Save failed" }
    }
}

@Composable
fun WorkflowEditorScreen(owner: String, repo: String, path: String, onBack: () -> Unit, vm: WorkflowEditorViewModel = hiltViewModel()) {
    LaunchedEffect(path) { vm.load(owner, repo, path) }
    val text by vm.text.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()
    var local by remember(text) { mutableStateOf(text) }

    ScreenScaffold("Edit · ${path.substringAfterLast('/')}", onBack) { m ->
        Column(m.fillMaxSize().padding(12.dp), Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = local, onValueChange = { local = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Button(onClick = { vm.save("Update $path") }, modifier = Modifier.fillMaxWidth()) { Text("Commit changes") }
            msg?.let { Text(it) }
        }
    }
}
