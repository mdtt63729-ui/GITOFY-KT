package com.gitofy.native.ui.screens.misc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.native.update.ApkDownloader
import com.gitofy.native.update.ApkInstaller
import com.gitofy.native.update.DownloadProgress
import com.gitofy.native.update.ReleaseInfo
import com.gitofy.native.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data class Available(val info: ReleaseInfo, val current: String) : UpdateUi
    data object UpToDate : UpdateUi
    data class Downloading(val progress: DownloadProgress) : UpdateUi
    data object Installing : UpdateUi
    data class Failed(val message: String) : UpdateUi
}

@HiltViewModel
class AppStoreViewModel @Inject constructor(
    private val checker: UpdateChecker,
    private val downloader: ApkDownloader,
    private val installer: ApkInstaller,
) : ViewModel() {

    private val _ui = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val ui: StateFlow<UpdateUi> = _ui.asStateFlow()

    /** Set from the host activity (BuildConfig.VERSION_NAME equivalent). */
    var currentVersion: String = "0.2.0"

    fun check() {
        viewModelScope.launch {
            _ui.value = UpdateUi.Checking
            val info = checker.fetchLatest()
            _ui.value = when {
                info == null -> UpdateUi.Failed("Could not reach GitHub releases")
                com.gitofy.native.update.VersionCompare.isUpdateAvailable(info.version, currentVersion) ->
                    UpdateUi.Available(info, currentVersion)
                else -> UpdateUi.UpToDate
            }
        }
    }

    fun update(info: ReleaseInfo, token: String?) {
        viewModelScope.launch {
            runCatching {
                downloader.download(info, token).collect { _ui.value = UpdateUi.Downloading(it) }
                _ui.value = UpdateUi.Installing
                val path = downloader.lastPath ?: error("Download path missing")
                installer.install(path)
            }.onFailure { _ui.value = UpdateUi.Failed(it.message ?: "Update failed") }
        }
    }
}
