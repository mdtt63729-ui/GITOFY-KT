package com.gitofy.native.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Workflow-run / upload notifications (utils/runNotifications.ts). */
@Singleton
class Notifications @Inject constructor(@ApplicationContext private val context: Context) {

    companion object { const val CHANNEL_RUNS = "workflow_runs"; const val CHANNEL_UPLOADS = "uploads" }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNS, "Workflow runs", NotificationManager.IMPORTANCE_DEFAULT),
        )
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_UPLOADS, "Uploads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun notify(channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
