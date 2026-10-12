package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.util.ErrorLog
import com.gitofy.app.util.LogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ErrorLogViewModel @Inject constructor(log: ErrorLog) : ViewModel() {
    val entries: StateFlow<List<LogEntry>> = log.entries
}

@Composable
fun ErrorLogScreen(onBack: () -> Unit, vm: ErrorLogViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    Scaffold(topBar = { GitofyTopBar("Error log", onBack) }) { padding ->
        if (entries.isEmpty()) {
            EmptyState("No logs", "Diagnostics from this session appear here.", modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp)) {
                items(entries) { e ->
                    Text(
                        e.formatted(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
