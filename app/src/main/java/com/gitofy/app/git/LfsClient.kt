package com.gitofy.app.git

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.security.MessageDigest

@Serializable data class LfsObject(val oid: String, val size: Long)

@Serializable
private data class LfsBatchRequest(
    val operation: String,
    val transfers: List<String> = listOf("basic"),
    val objects: List<LfsObject>,
)

@Serializable private data class LfsAction(val href: String, val header: Map<String, String> = emptyMap())
@Serializable private data class LfsActions(val upload: LfsAction? = null, val download: LfsAction? = null)
@Serializable private data class LfsError(val code: Int = 0, val message: String = "")
@Serializable private data class LfsObjectResp(val oid: String, val size: Long, val actions: LfsActions? = null, val error: LfsError? = null)
@Serializable private data class LfsBatchResponse(val transfer: String = "basic", val objects: List<LfsObjectResp> = emptyList())

/**
 * A minimal Git LFS client — no `git-lfs` binary to ship.
 *
 * For any file GitHub would hard-block (>= 100 MiB), the upload flow calls
 * [upload] to push the object to GitHub's LFS storage over the standard batch
 * API, then replaces the working-tree file with an LFS pointer so git commits
 * the tiny pointer instead of the giant blob. This is what lets a single
 * 100 MB+ file push successfully.
 */
class LfsClient(private val client: OkHttpClient, private val json: Json) {

    /**
     * Uploads [file] and returns its (oid, size). If it already exists on the
     * server the PUT is skipped. [onProgress] is called with (bytesSent, total)
     * as the object streams, so the UI can show a byte-level progress bar.
     */
    suspend fun upload(
        owner: String,
        repo: String,
        token: String,
        file: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): LfsObject =
        withContext(Dispatchers.IO) {
            val oid = sha256(file)
            val size = file.length()

            val url = "https://github.com/$owner/$repo.git/info/lfs/objects/batch"
            val auth = "Basic " + Base64.encodeToString(
                "x-access-token:$token".toByteArray(Charsets.UTF_8), Base64.NO_WRAP,
            )
            val lfsJson = "application/vnd.git-lfs+json".toMediaType()
            val body = json.encodeToString(
                LfsBatchRequest.serializer(),
                LfsBatchRequest("upload", objects = listOf(LfsObject(oid, size))),
            )
            val req = Request.Builder().url(url)
                .header("Accept", "application/vnd.git-lfs+json")
                .header("Content-Type", "application/vnd.git-lfs+json")
                .header("Authorization", auth)
                .post(body.toRequestBody(lfsJson))
                .build()

            val respText = client.newCall(req).execute().use { it.body?.string().orEmpty() }
            val parsed = json.decodeFromString(LfsBatchResponse.serializer(), respText)
            val obj = parsed.objects.firstOrNull { it.oid == oid }
                ?: error("LFS batch returned no object for $oid")
            obj.error?.let { error("LFS error ${it.code}: ${it.message}") }

            val up = obj.actions?.upload
            if (up != null) {
                val total = file.length()
                val putBody = object : RequestBody() {
                    override fun contentType() = "application/octet-stream".toMediaType()
                    override fun contentLength() = total
                    override fun writeTo(sink: BufferedSink) {
                        // Stream in 64 KB chunks, reporting bytes as they go out.
                        file.inputStream().use { input ->
                            val buf = ByteArray(64 * 1024)
                            var sent = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                sink.write(buf, 0, n)
                                sent += n
                                onProgress(sent, total)
                            }
                        }
                    }
                }
                val putReq = Request.Builder().url(up.href)
                    .apply { up.header.forEach { (k, v) -> header(k, v) } }
                    .put(putBody)
                    .build()
                client.newCall(putReq).execute().use { r ->
                    if (!r.isSuccessful) error("LFS upload failed: HTTP ${r.code}")
                }
            }
            LfsObject(oid, size)
        }

    /** The LFS object id (sha256) for [file] — lets a resume skip uploaded objects. */
    fun oidOf(file: File): String = sha256(file)

    /** The pointer file git should commit in place of the real object. */
    fun pointer(oid: String, size: Long): String =
        "version https://git-lfs.github.com/spec/v1\noid sha256:$oid\nsize $size\n"

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
