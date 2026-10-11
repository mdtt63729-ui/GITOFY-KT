package com.gitofy.native.upload

import com.gitofy.native.git.GitProgress

/** Everything the upload pipeline needs; [zipUri] is a string so it survives a restart. */
data class UploadRequest(
    val owner: String,
    val repo: String,
    val branch: String,
    val zipUri: String,
    val commitMessage: String,
    val clearFirst: Boolean = false,
    val useLfs: Boolean = true,
    val resume: Boolean = false,
)

/** A neutral progress tick (no UI types), consumed by the ViewModel and the Worker. */
data class PipelineProgress(
    val phase: GitProgress.Phase,
    val percent: Int?,
    val speed: String?,
    val detail: String,
    val stage: String,
    val doneBytes: Long = 0,
    val totalBytes: Long = 0,
)

sealed interface UploadOutcome {
    data class Success(val branch: String, val lfsFiles: Int) : UploadOutcome
    data class Failure(val message: String, val resumable: Boolean) : UploadOutcome
}
