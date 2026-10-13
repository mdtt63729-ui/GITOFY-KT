package com.gitofy.app.data.remote

import android.content.Context
import com.gitofy.app.data.TokenStore
import com.gitofy.app.update.DownloadProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams any GitHub file (a release asset, or the repository's code ZIP from
 * `repos/{owner}/{repo}/zipball/{ref}`) into cacheDir, emitting byte progress.
 */
@Singleton
class FileDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    var lastPath: String? = null
        private set

    fun download(url: String, fileName: String, withAuth: Boolean): Flow<DownloadProgress> = flow {
        val token = if (withAuth) tokenStore.token() else null
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Gitofy-Native")
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/octet-stream")
                }
            }
            .build()

        val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val out = File(dir, fileName)

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("Download failed (HTTP ${res.code})")
            val body = res.body ?: error("Empty response")
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
        lastPath = out.absolutePath
    }.flowOn(Dispatchers.IO)
}
