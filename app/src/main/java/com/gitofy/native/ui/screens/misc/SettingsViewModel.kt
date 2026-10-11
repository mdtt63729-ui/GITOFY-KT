package com.gitofy.native.ui.screens.misc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.native.git.BenchmarkResult
import com.gitofy.native.git.BenchmarkStore
import com.gitofy.native.settings.GitofySettings
import com.gitofy.native.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    benchmarks: BenchmarkStore,
) : ViewModel() {

    val benchmark: StateFlow<BenchmarkResult?> = benchmarks.last
    val settings: StateFlow<GitofySettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GitofySettings())

    fun setPalette(v: String) = viewModelScope.launch { store.setPalette(v) }
    fun setLanguage(v: String) = viewModelScope.launch { store.setLanguage(v) }
    fun setUiMode(v: String) = viewModelScope.launch { store.setUiMode(v) }
    fun setReduceMotion(v: Boolean) = viewModelScope.launch { store.setReduceMotion(v) }
    fun setFlagSecure(v: Boolean) = viewModelScope.launch { store.setFlagSecure(v) }
    fun setDefaultBranch(v: String) = viewModelScope.launch { store.setDefaultBranch(v) }
}
