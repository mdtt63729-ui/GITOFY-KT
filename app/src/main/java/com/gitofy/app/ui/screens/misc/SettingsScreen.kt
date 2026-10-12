package com.gitofy.app.ui.screens.misc

import androidx.compose.foundation.BorderStroke
import com.gitofy.app.ui.components.pressClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.settings.GitofySettings
import com.gitofy.app.ui.components.BenchmarkChart
import com.gitofy.app.ui.components.ScreenScaffold
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.theme.Palette

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val benchmark by vm.benchmark.collectAsStateWithLifecycle()

    ScreenScaffold("Settings & Customization", onBack) { m ->
        LazyColumn(
            m.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Appearance") }
            item {
                SettingsCard {
                    ChipRow("Theme", listOf("light" to "Light", "dark" to "Dark", "system" to "System"), s.uiMode) { vm.setUiMode(it) }
                    ChipRow("M3 Accent Palette", Palette.entries.map { it.label to it.label.replaceFirstChar { c -> c.uppercase() } }, s.palette) { vm.setPalette(it) }
                    ChipRow("Font", listOf("josefin" to "Josefin", "poppins" to "Poppins", "montserrat" to "Montserrat", "orbitron" to "Orbitron", "playfair" to "Playfair", "system" to "System"), s.fontFamily) { vm.setFontFamily(it) }
                    ChipRow("UI Density", listOf("compact" to "Compact", "normal" to "Normal", "comfortable" to "Comfortable"), s.uiDensity) { vm.setUiDensity(it) }
                }
            }

            item { SectionHeader("Behaviour") }
            item {
                SettingsCard {
                    SwitchRow("Haptic Feedback", "Vibrate on taps and actions", s.haptics) { vm.setHaptics(it) }
                    SwitchRow("Reduce Motion", "Disable springs and heavy transforms", s.reduceMotion) { vm.setReduceMotion(it) }
                    SwitchRow("Auto-Hide Floating Nav", "Hide the bottom nav while scrolling", s.autoHideNav) { vm.setAutoHideNav(it) }
                    ChipRow("Language", listOf("en" to "EN", "bn" to "বাংলা"), s.language) { vm.setLanguage(it) }
                }
            }

            item { SectionHeader("Repositories") }
            item {
                SettingsCard {
                    NavRow("Default Branch", s.defaultBranch) { }
                    ChipRow("Default New Repo Visibility", listOf("private" to "Private", "public" to "Public"), s.defaultVisibility) { vm.setDefaultVisibility(it) }
                    SwitchRow("Strip Archive Root Folder", "Remove the top folder when uploading a ZIP", s.stripRootFolder) { vm.setStripRootFolder(it) }
                }
            }

            item { SectionHeader("Security") }
            item {
                SettingsCard {
                    ChipRow("App Lock", listOf("off" to "Off", "biometric" to "Biometric"), s.appLockMode) { vm.setAppLockMode(it) }
                    SwitchRow("Blur the app preview in Recents", "Screen-capture protection (FLAG_SECURE)", s.flagSecure) { vm.setFlagSecure(it) }
                }
            }

            item { SectionHeader("GitHub") }
            item {
                SettingsCard {
                    NavRow("GitHub OAuth Client ID", s.githubClientId.ifBlank { com.gitofy.app.auth.AuthConfig.CLIENT_ID }) { onNavigate(Routes.LOGIN_DIAGNOSTICS) }
                    NavRow("Personal Access Token (fallback)", "Add or replace your PAT") { onNavigate(Routes.LOGIN) }
                    NavRow("GitHub account", s.githubUsername.ifBlank { "Not signed in" }) { onNavigate(Routes.GITHUB_SETTINGS) }
                }
            }

            item { SectionHeader("More") }
            item {
                SettingsCard {
                    NavRow("Gists", "Your GitHub gists") { onNavigate(Routes.GISTS) }
                    NavRow("Search everything", "Search repositories and code") { onNavigate(Routes.GLOBAL_SEARCH) }
                    NavRow("Error log", "Diagnostics from this session") { onNavigate(Routes.ERROR_LOG) }
                }
            }

            item { SectionHeader("Upload tuning") }
            item {
                SettingsCard {
                    if (benchmark == null) {
                        Text(
                            "Run an upload to see which compression level was chosen for your link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp),
                        )
                    } else {
                        BenchmarkChart(benchmark!!)
                    }
                }
            }

            item { SectionHeader("Danger zone") }
            item {
                SettingsCard {
                    NavRow("Sign out", "Clear the token and return to login", danger = true) { onNavigate(Routes.LOGIN) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
