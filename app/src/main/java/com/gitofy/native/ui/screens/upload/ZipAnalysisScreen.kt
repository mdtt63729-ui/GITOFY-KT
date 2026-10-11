package com.gitofy.native.ui.screens.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.native.ui.components.ScreenScaffold

@Composable
fun ZipAnalysisScreen(onBack: () -> Unit, added: Int = 0, modified: Int = 0, deleted: Int = 0, unchanged: Int = 0) {
    ScreenScaffold("ZIP analysis", onBack) { m ->
        Column(m.padding(20.dp), Arrangement.spacedBy(8.dp)) {
            Text("Smart diff", style = MaterialTheme.typography.titleLarge)
            Text("Added: $added")
            Text("Modified: $modified")
            Text("Deleted: $deleted")
            Text("Unchanged: $unchanged")
        }
    }
}
