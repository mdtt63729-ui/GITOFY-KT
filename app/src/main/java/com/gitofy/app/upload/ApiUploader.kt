package com.gitofy.app.upload

import android.util.Base64
import com.gitofy.app.data.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes a working tree to GitHub with **no bundled git** — straight through the REST
 * API (blobs → tree → commit → ref). Slower than native git, but it means an upload
 * still succeeds on any build.
 */
@Singleton
class ApiUploader @Inject constructor(
    private val http: OkHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    private val jsonMedia = "application/json".toMediaType()
    private val maxBlob = 95L * 1024 * 1024   // the blobs API rejects > 100 MB

    suspend fun push(
        owner: String,
        repo: String,
        branch: String,
        dir: File,
        message: String,
        clearFirst: Boolean,
        onProgress: (done: Int, total: Int, name: String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val token = tokenStore.token() ?: error("Not signed in — no GitHub token.")
        val api = "https://api.github.com/repos/$owner/$repo"

        fun builder(method: String, url: String, body: JsonObject? = null): Request.Builder {
            val b = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Gitofy-Native")
            when (method) {
                "POST" -> b.post((body?.toString() ?: "{}").toRequestBody(jsonMedia))
                "PATCH" -> b.patch((body?.toString() ?: "{}").toRequestBody(jsonMedia))
            }
            return b
        }

        fun exec(b: Request.Builder): JsonObject {
            http.newCall(b.build()).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) error("GitHub API ${res.code}: ${text.take(300)}")
                return json.parseToJsonElement(text).jsonObject
            }
        }

        // 1. Current head + its base tree (missing branch => we create it below).
        var headSha: String? = null
        var baseTree: String? = null
        runCatching {
            val ref = exec(builder("GET", "$api/git/ref/heads/$branch"))
            headSha = ref["object"]?.jsonObject?.get("sha")?.jsonPrimitive?.content
            if (headSha != null) {
                val commit = exec(builder("GET", "$api/git/commits/$headSha"))
                baseTree = commit["tree"]?.jsonObject?.get("sha")?.jsonPrimitive?.content
            }
        }

        // 2. One blob per file.
        val files = dir.walkTopDown().filter { it.isFile && !it.path.contains("/.git/") }.toList()
        val total = files.size
        val entries = ArrayList<JsonObject>(total)
        files.forEachIndexed { i, f ->
            if (f.length() > maxBlob) error("${f.name} is larger than 95 MB — enable the bundled git build for big files.")
            onProgress(i, total, f.name)
            val content = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
            val blob = exec(builder("POST", "$api/git/blobs", buildJsonObject {
                put("content", JsonPrimitive(content))
                put("encoding", JsonPrimitive("base64"))
            }))
            val sha = blob["sha"]!!.jsonPrimitive.content
            val rel = f.relativeTo(dir).path.replace('\\', '/')
            entries.add(buildJsonObject {
                put("path", JsonPrimitive(rel))
                put("mode", JsonPrimitive("100644"))
                put("type", JsonPrimitive("blob"))
                put("sha", JsonPrimitive(sha))
            })
        }

        // 3. Tree.
        val treeBody = buildJsonObject {
            if (!clearFirst && baseTree != null) put("base_tree", JsonPrimitive(baseTree!!))
            put("tree", JsonArray(entries))
        }
        val treeSha = exec(builder("POST", "$api/git/trees", treeBody))["sha"]!!.jsonPrimitive.content

        // 4. Commit.
        val commitBody = buildJsonObject {
            put("message", JsonPrimitive(message))
            put("tree", JsonPrimitive(treeSha))
            if (headSha != null) put("parents", JsonArray(listOf(JsonPrimitive(headSha!!))))
        }
        val commitSha = exec(builder("POST", "$api/git/commits", commitBody))["sha"]!!.jsonPrimitive.content

        // 5. Move (or create) the branch.
        if (headSha != null) {
            exec(builder("PATCH", "$api/git/refs/heads/$branch", buildJsonObject {
                put("sha", JsonPrimitive(commitSha))
                put("force", JsonPrimitive(false))
            }))
        } else {
            exec(builder("POST", "$api/git/refs", buildJsonObject {
                put("ref", JsonPrimitive("refs/heads/$branch"))
                put("sha", JsonPrimitive(commitSha))
            }))
        }
        onProgress(total, total, "done")
        commitSha
    }
}
