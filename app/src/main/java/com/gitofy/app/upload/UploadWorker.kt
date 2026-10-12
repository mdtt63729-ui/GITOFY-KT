package com.gitofy.app.upload

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the upload pipeline in the background, so a push keeps going even if the
 * user leaves the app. It shows a foreground notification with live progress and
 * retries on a transient failure (the pipeline's checkpoint makes the retry cheap).
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: UploadPipeline,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val owner = inputData.getString(KEY_OWNER) ?: return Result.failure()
        val repo = inputData.getString(KEY_REPO) ?: return Result.failure()
        val branch = inputData.getString(KEY_BRANCH) ?: "main"
        val zipUri = inputData.getString(KEY_ZIP) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: "Upload from Gitofy"

        val req = UploadRequest(
            owner = owner, repo = repo, branch = branch, zipUri = zipUri,
            commitMessage = message,
            clearFirst = inputData.getBoolean(KEY_CLEAR, false),
            useLfs = inputData.getBoolean(KEY_LFS, true),
            resume = inputData.getBoolean(KEY_RESUME, false),
        )

        setForeground(foreground("Uploading $owner/$repo…", 0))

        // setProgress / setForeground are suspend; pump progress through a flow so
        // they run inside a coroutine rather than inside the (non-suspend) callback.
        val progress = MutableStateFlow<PipelineProgress?>(null)
        val outcome = coroutineScope {
            val pump = launch {
                progress.filterNotNull().collect { p ->
                    setProgress(workDataOf("percent" to (p.percent ?: 0), "detail" to p.detail))
                    setForeground(foreground(p.detail, p.percent ?: 0))
                }
            }
            val result = pipeline.run(req) { p -> progress.value = p }
            pump.cancel()
            result
        }

        return when (outcome) {
            is UploadOutcome.Success -> Result.success(workDataOf("branch" to outcome.branch))
            is UploadOutcome.Failure -> if (outcome.resumable) Result.retry() else Result.failure(workDataOf("error" to outcome.message))
        }
    }

    private fun foreground(text: String, percent: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "uploads")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Gitofy upload")
            .setContentText(text)
            .setOngoing(true)
            .setProgress(100, percent.coerceIn(0, 100), percent == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    companion object {
        const val NOTIF_ID = 4201
        const val KEY_OWNER = "owner"
        const val KEY_REPO = "repo"
        const val KEY_BRANCH = "branch"
        const val KEY_ZIP = "zipUri"
        const val KEY_MESSAGE = "message"
        const val KEY_CLEAR = "clearFirst"
        const val KEY_LFS = "useLfs"
        const val KEY_RESUME = "resume"
    }
}
