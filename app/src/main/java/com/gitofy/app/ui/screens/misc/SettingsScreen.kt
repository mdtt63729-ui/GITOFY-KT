package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.ui.components.BenchmarkChart
import com.gitofy.app.ui.components.SectionTitle
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.theme.Palette

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val benchmark by vm.benchmark.collectAsStateWithLifecycle()
    ScreenScaffold("Settings", onBack) { m ->
        Column(m.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle("Appearance")
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(14.dp), Arrangement.spacedBy(10.dp)) {
                    Text("Palette", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Palette.entries.take(3).forEach { p ->
                            FilterChip(s.palette == p.label, { vm.setPalette(p.label) }, label = { Text(p.label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Palette.entries.drop(3).forEach { p ->
                            FilterChip(s.palette == p.label, { vm.setPalette(p.label) }, label = { Text(p.label) })
                        }
                    }
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(s.uiMode == "light", { vm.setUiMode("light") }, label = { Text("Light") })
                        FilterChip(s.uiMode == "dark", { vm.setUiMode("dark") }, label = { Text("Dark") })
                        FilterChip(s.uiMode == "system", { vm.setUiMode("system") }, label = { Text("System") })
                    }
                    Text("Language", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(s.language == "en", { vm.setLanguage("en") }, label = { Text("English") })
                        FilterChip(s.language == "bn", { vm.setLanguage("bn") }, label = { Text("বাংলা") })
                    }
                }
            }

            SectionTitle("Behaviour")
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(14.dp), Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Reduce motion", Modifier.weight(1f)); Switch(s.reduceMotion, { vm.setReduceMotion(it) })
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Screen-capture protection", Modifier.weight(1f)); Switch(s.flagSecure, { vm.setFlagSecure(it) })
                    }
                }
            }
            SectionTitle("Upload tuning")
            if (benchmark == null) {
                Text(
                    "Run an upload to see which compression level was chosen for your link.",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        BenchmarkChart(benchmark!!)
                    }
                }
            }

            SectionTitle("Defaults")
            Text("Default branch: ${s.defaultBranch}", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))
        }
    }
}
