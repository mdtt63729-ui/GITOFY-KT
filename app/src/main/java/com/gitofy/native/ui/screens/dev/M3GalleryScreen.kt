package com.gitofy.native.ui.screens.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gitofy.native.ui.components.ScreenScaffold

/** A live gallery of the M3 components in the current palette. */
@Composable
fun M3GalleryScreen(onBack: () -> Unit) {
    var sw by remember { mutableStateOf(true) }
    var sl by remember { mutableStateOf(0.5f) }
    ScreenScaffold("M3 gallery", onBack) { m ->
        Column(m.padding(16.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Filled button") }
            FilledTonalButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Tonal button") }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Outlined") }
            TextButton(onClick = {}) { Text("Text button") }
            FilterChip(true, {}, label = { Text("Filter chip") })
            androidx.compose.foundation.layout.Row { Text("Switch"); Switch(sw, { sw = it }) }
            Slider(sl, { sl = it })
            LinearProgressIndicator(progress = { sl }, modifier = Modifier.fillMaxWidth())
            Card(Modifier.fillMaxWidth()) { Text("Card surface", Modifier.padding(16.dp)) }
        }
    }
}
