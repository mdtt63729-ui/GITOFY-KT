package com.gitofy.app.upload

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues background uploads (one unique job per repo). */
@Singleton
class UploadScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun enqueue(req: UploadRequest) {
        val data = workDataOf(
            UploadWorker.KEY_OWNER to req.owner,
            UploadWorker.KEY_REPO to req.repo,
            UploadWorker.KEY_BRANCH to req.branch,
            UploadWorker.KEY_ZIP to req.zipUri,
            UploadWorker.KEY_MESSAGE to req.commitMessage,
            UploadWorker.KEY_CLEAR to req.clearFirst,
            UploadWorker.KEY_LFS to req.useLfs,
            UploadWorker.KEY_RESUME to req.resume,
        )
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(tag(req.repo))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(tag(req.repo), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(repo: String) = WorkManager.getInstance(context).cancelUniqueWork(tag(repo))

    private fun tag(repo: String) = "gitofy-upload-$repo"
}
