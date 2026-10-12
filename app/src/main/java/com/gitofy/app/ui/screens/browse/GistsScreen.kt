package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.data.remote.dto.CreateGistRequest
import com.gitofy.app.data.remote.dto.GistDto
import com.gitofy.app.data.remote.dto.GistFileContent
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GistsViewModel @Inject constructor(private val api: GitHubApiService) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<GistDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<GistDto>>> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { api.gists() }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Failed") }
    }
    fun create(description: String, filename: String, content: String, isPublic: Boolean) = viewModelScope.launch {
        runCatching { api.createGist(CreateGistRequest(description.ifBlank { null }, isPublic, mapOf(filename to GistFileContent(content)))) }
            .onSuccess { load() }
    }
    fun delete(id: String) = viewModelScope.launch {
        runCatching { api.deleteGist(id) }.onSuccess { load() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistsScreen(onBack: () -> Unit, vm: GistsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        GitofyTopBar("Gists", onBack, actions = {
            IconButton(onClick = { sheet = true }) { Icon(Icons.Filled.Add, "New gist") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Your GitHub snippets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
            when (val st = s) {
                is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                is UiState.Error -> ErrorState(st.message, onRetry = { vm.load() }, modifier = Modifier.fillMaxSize())
                is UiState.Content ->
                    if (st.data.isEmpty()) EmptyState("No gists yet", "Create your first gist with the + button.", icon = Icons.Filled.Code, modifier = Modifier.fillMaxSize())
                    else LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(st.data, key = { it.id }) { g ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().animateItem()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.Code, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(g.description ?: g.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${g.files.size} file(s) • ${if (g.isPublic) "public" else "secret"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { vm.delete(g.id) }) { Icon(Icons.Filled.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
            }
        }
    }

    if (sheet) {
        var desc by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("snippet.txt") }
        var content by remember { mutableStateOf("") }
        var pub by remember { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New gist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("File name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(content, { content = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Public gist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Anyone can find it. Off keeps it secret.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(pub, { pub = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { sheet = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { vm.create(desc, name, content, pub); sheet = false },
                        enabled = content.isNotBlank(), modifier = Modifier.weight(1f),
                    ) { Text("Create") }
                }
            }
        }
    }
}
