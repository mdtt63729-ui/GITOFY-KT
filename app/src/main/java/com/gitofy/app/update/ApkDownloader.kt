package com.gitofy.app.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Mirrors updater.ts DownloadProgress. */
data class DownloadProgress(val percent: Int, val received: Long, val total: Long, val speedBps: Long)

/**
 * Streams an APK to cacheDir with progress — the native replacement for the web
 * app's bridge-driven HttpURLConnection download. Same progress fields.
 */
@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    fun download(info: ReleaseInfo, token: String?): Flow<DownloadProgress> = flow {
        val req = Request.Builder()
            .url(if (!token.isNullOrBlank()) info.apiAssetUrl else info.downloadUrl)
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/octet-stream")
                }
            }
            .build()

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, info.apkName)

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("Download failed: HTTP ${res.code}")
            val body = res.body ?: error("Empty response body")
            val total = body.contentLength()
            val start = System.currentTimeMillis()
            var received = 0L
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        received += n
                        val elapsed = ((System.currentTimeMillis() - start) / 1000.0).coerceAtLeast(0.001)
                        emit(
                            DownloadProgress(
                                percent = if (total > 0) ((received * 100) / total).toInt() else 0,
                                received = received,
                                total = total,
                                speedBps = (received / elapsed).toLong(),
                            )
                        )
                    }
                }
            }
        }
        // Remember the path for the installer.
        lastPath = out.absolutePath
    }.flowOn(Dispatchers.IO)

    var lastPath: String? = null
        private set
}
