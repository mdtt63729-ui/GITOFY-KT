package com.gitofy.app.data.remote

import com.gitofy.app.data.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches a workflow job's log text.
 *
 * GitHub's job-logs endpoint answers with a **302** to a short-lived signed blob URL.
 * We follow that redirect ourselves and drop the Authorization header on the second
 * hop (the blob rejects it). Routing this through the shared Retrofit client — whose
 * auth interceptor stamps `Authorization` + `Accept: application/vnd.github+json` on
 * every request — is what made the logs fail before.
 */
@Singleton
class JobLogFetcher @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val noRedirect = client.newBuilder().followRedirects(false).build()

    suspend fun fetch(owner: String, repo: String, jobId: Long): String = withContext(Dispatchers.IO) {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/actions/jobs/$jobId/logs"
        val token = tokenStore.token()

        val first = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Gitofy-Native")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
            .build()

        noRedirect.newCall(first).execute().use { res ->
            val location = res.header("Location")
            if (res.code in 300..399 && !location.isNullOrBlank()) {
                // Signed blob URL — no auth header, plain GET.
                val signed = Request.Builder().url(location).header("User-Agent", "Gitofy-Native").build()
                client.newCall(signed).execute().use { r ->
                    if (!r.isSuccessful) error("Couldn't download the log (HTTP ${r.code})")
                    r.body?.string().orEmpty()
                }
            } else if (res.isSuccessful) {
                res.body?.string().orEmpty()
            } else if (res.code == 404) {
                error("No log for this job yet (it may still be queued).")
            } else {
                error("Couldn't fetch the log (HTTP ${res.code})")
            }
        }
    }
}
