package com.gitofy.app.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.domain.RepoRepository
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.UploadState
import com.gitofy.app.ui.UploadViewModel
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
class MultiRepoViewModel @Inject constructor(repo: RepoRepository) : ViewModel() {
    private val _repos = MutableStateFlow<UiState<List<Repository>>>(UiState.Loading)
    val repos: StateFlow<UiState<List<Repository>>> = _repos.asStateFlow()
    init {
        viewModelScope.launch {
            runCatching { repo.refresh() }
                .onSuccess { _repos.value = UiState.Content(it) }
                .onFailure { _repos.value = UiState.Error(it.message ?: "Failed") }
        }
    }
}

@Composable
fun MultiRepoUploadScreen(onBack: () -> Unit, upload: UploadViewModel = hiltViewModel(), vm: MultiRepoViewModel = hiltViewModel()) {
    val state by upload.state.collectAsStateWithLifecycle()
    val reposState by vm.repos.collectAsStateWithLifecycle()
    val selected = remember { mutableStateListOf<String>() }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("Upload from Gitofy") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked = it }

    Scaffold(topBar = { GitofyTopBar("Send to more repositories", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedButton(onClick = { picker.launch("application/zip") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(if (picked == null) "Pick ZIP file" else "ZIP selected ✓", fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                message, { message = it }, label = { Text("Commit message") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            when (val s = reposState) {
                is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize())
                is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                is UiState.Content -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(s.data, key = { it.id }) { r ->
                        val key = "${r.ownerLogin}/${r.name}"
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = key in selected, onCheckedChange = { if (it) selected.add(key) else selected.remove(key) })
                            Text(r.fullName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            (state as? UploadState.Running)?.let { Text("${it.phase} ${it.percent ?: 0}% ${it.detail}", modifier = Modifier.padding(horizontal = 16.dp)) }
            Button(
                onClick = {
                    val uri = picked ?: return@Button
                    selected.forEach { line ->
                        val parts = line.split('/', limit = 2)
                        if (parts.size == 2) upload.uploadZip(parts[0], parts[1], "main", uri, message)
                    }
                },
                enabled = picked != null && selected.isNotEmpty(),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Send to ${selected.size} repositor${if (selected.size == 1) "y" else "ies"}", fontWeight = FontWeight.Bold) }
        }
    }
}
