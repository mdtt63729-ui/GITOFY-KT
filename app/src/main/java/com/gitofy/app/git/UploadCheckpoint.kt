package com.gitofy.app.git

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A resumable-upload checkpoint. git push itself is not resumable, but the
 * *pipeline* around it is: if the connection drops we keep the shallow clone,
 * the extracted tree, the LFS objects already uploaded and the commit, and a
 * resume re-enters at the stage that failed instead of starting over.
 *
 * The file lives **next to** the working tree (never inside it), so `git add -A`
 * can never commit it.
 */
@Serializable
data class UploadCheckpoint(
    val owner: String,
    val repo: String,
    val branch: String,
    val stage: Stage,
    /** The picked ZIP, so a resume can re-extract without the UI. */
    val zipUri: String? = null,
    /** LFS object ids already uploaded, so a resume skips them. */
    val doneOids: List<String> = emptyList(),
    /** Progress numbers for the pending-uploads display. */
    val totalBytes: Long = 0,
    val lfsTotalFiles: Int = 0,
    val lfsDoneFiles: Int = 0,
    val lfsTotalBytes: Long = 0,
    val lfsDoneBytes: Long = 0,
) {
    /** Human summary of how much is left, e.g. "2/5 files · 120 MB / 300 MB". */
    val remainingText: String
        get() = when {
            lfsTotalFiles > 0 -> "$lfsDoneFiles/$lfsTotalFiles LFS files · ${mb(lfsDoneBytes)} / ${mb(lfsTotalBytes)}"
            totalBytes > 0 -> "project ${mb(totalBytes)}"
            else -> ""
        }

    private fun mb(b: Long): String = "%.1f MB".format(b / (1024.0 * 1024.0))

    enum class Stage { NONE, CLONED, EXTRACTED, LFS, COMMITTED, PUSHED }

    fun save(file: File) {
        runCatching { file.writeText(Json.encodeToString(serializer(), this)) }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** cacheDir/work/<repo>.ckpt.json — beside the work dir, not inside it. */
        fun fileFor(workDir: File): File = File(workDir.parentFile, workDir.name + ".ckpt.json")

        fun load(workDir: File): UploadCheckpoint? =
            runCatching { json.decodeFromString(serializer(), fileFor(workDir).readText()) }.getOrNull()

        fun clear(workDir: File) { fileFor(workDir).delete() }

        /** Every not-yet-finished checkpoint under [uploadsDir] (survives restarts). */
        fun pendingIn(uploadsDir: File): List<UploadCheckpoint> =
            (uploadsDir.listFiles { f -> f.name.endsWith(".ckpt.json") } ?: emptyArray())
                .mapNotNull { f ->
                    runCatching { json.decodeFromString(serializer(), f.readText()) }.getOrNull()
                        ?.takeIf { it.stage != Stage.PUSHED }
                }

        fun discard(uploadsDir: File, repo: String) {
            File(uploadsDir, "$repo.ckpt.json").delete()
            File(uploadsDir, repo).deleteRecursively()
        }
    }
}
