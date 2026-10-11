package com.gitofy.native.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.native.git.GitProgress
import com.gitofy.native.git.UploadCheckpoint
import com.gitofy.native.upload.UploadOutcome
import com.gitofy.native.upload.UploadPipeline
import com.gitofy.native.upload.UploadRequest
import com.gitofy.native.upload.UploadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UploadState {
    data object Idle : UploadState
    data class Running(
        val phase: GitProgress.Phase,
        val percent: Int?,
        val speed: String?,
        val detail: String,
    ) : UploadState
    data class Done(val branch: String, val lfsFiles: Int) : UploadState
    data class Failed(val message: String, val resumable: Boolean) : UploadState
}

/**
 * Thin UI wrapper over [UploadPipeline]: exposes state + the pending-upload list,
 * and can hand the work to [UploadScheduler] to run in the background.
 */
@HiltViewModel
class UploadViewModel @Inject constructor(
    app: Application,
    private val pipeline: UploadPipeline,
    private val scheduler: UploadScheduler,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    private val _pending = MutableStateFlow<List<UploadCheckpoint>>(emptyList())
    val pending: StateFlow<List<UploadCheckpoint>> = _pending.asStateFlow()

    init { refreshPending() }

    fun refreshPending() { _pending.value = UploadCheckpoint.pendingIn(pipeline.uploadsDir()) }

    fun hasResumable(owner: String, repo: String): Boolean {
        val work = pipeline.workDir(repo)
        val cp = UploadCheckpoint.load(work) ?: return false
        return cp.stage != UploadCheckpoint.Stage.PUSHED && File(work, ".git").exists()
    }

    fun discard(repo: String) {
        UploadCheckpoint.discard(pipeline.uploadsDir(), repo)
        refreshPending()
    }

    fun uploadZip(
        owner: String, repo: String, branch: String, zipUri: Uri,
        commitMessage: String, clearFirst: Boolean = false, useLfs: Boolean = true,
    ) = run(UploadRequest(owner, repo, branch, zipUri.toString(), commitMessage, clearFirst, useLfs, resume = false))

    fun resume(owner: String, repo: String, commitMessage: String) {
        val cp = UploadCheckpoint.load(pipeline.workDir(repo)) ?: run {
            _state.value = UploadState.Failed("Nothing to resume.", false); return
        }
        run(UploadRequest(cp.owner, cp.repo, cp.branch, cp.zipUri ?: "", commitMessage, resume = true))
    }

    /** Hand the upload to WorkManager so it continues even if the app is closed. */
    fun uploadInBackground(
        owner: String, repo: String, branch: String, zipUri: Uri,
        commitMessage: String, clearFirst: Boolean = false, useLfs: Boolean = true,
    ) {
        scheduler.enqueue(UploadRequest(owner, repo, branch, zipUri.toString(), commitMessage, clearFirst, useLfs, resume = false))
        _state.value = UploadState.Running(GitProgress.Phase.OTHER, null, null, "Queued — running in the background.")
        refreshPending()
    }

    fun cancelBackground(repo: String) = scheduler.cancel(repo)

    private fun run(req: UploadRequest) {
        viewModelScope.launch {
            val outcome = pipeline.run(req) { p ->
                _state.value = UploadState.Running(p.phase, p.percent, p.speed, p.detail)
            }
            _state.value = when (outcome) {
                is UploadOutcome.Success -> UploadState.Done(outcome.branch, outcome.lfsFiles)
                is UploadOutcome.Failure -> UploadState.Failed(outcome.message, outcome.resumable)
            }
            refreshPending()
        }
    }
}
