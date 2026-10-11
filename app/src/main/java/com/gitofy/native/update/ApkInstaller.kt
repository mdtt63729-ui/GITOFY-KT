package com.gitofy.native.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installs a downloaded APK via PackageInstaller (API 26+), falling back to the
 * system install intent. Replaces the web app's bridge installApk().
 */
@Singleton
class ApkInstaller @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun install(apkPath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(apkPath)
        if (!file.exists()) return@withContext false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { installViaSession(file) }.getOrElse { return@withContext installViaIntent(file) }
            true
        } else {
            installViaIntent(file)
        }
    }

    private fun installViaSession(file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            file.inputStream().use { input ->
                session.openWrite("base.apk", 0, file.length()).use { out -> input.copyTo(out); session.fsync(out) }
            }
            val intent = Intent(context, InstallReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    private fun installViaIntent(file: File): Boolean = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
