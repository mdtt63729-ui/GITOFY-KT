package com.gitofy.app.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.app.ui.components.ScreenScaffold

/** Generic action-result screen (RepoActionResultScreen in the web app). */
@Composable
fun RepoActionScreen(title: String, message: String, onBack: () -> Unit) {
    ScreenScaffold(title, onBack) { m ->
        Column(m.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
