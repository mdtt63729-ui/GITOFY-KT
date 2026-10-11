package com.gitofy.native.git

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fast wrapper around the **bundled git (C) binary** — the Termux approach.
 *
 * There is deliberately **no JGit and no libgit2 here**. git's C implementation
 * (pack generation + delta compression + the Smart-HTTP transport) is what gives
 * Termux its push speed, so the app ships and drives the real binary.
 *
 * Performance choices, tuned for large projects (>50 MB, >100 MB):
 *  - `protocol.version=2`          — faster ref negotiation
 *  - `pack.threads=0`              — pack with every core
 *  - `pack.windowMemory`/`deltaCacheSize`/`packSizeLimit` — big-repo pack tuning
 *  - `core.bigFileThreshold=1m`    — don't delta-compress big binaries (CPU win)
 *  - `feature.manyFiles=true`      — index v4 + untracked cache for many files
 *  - `http.postBuffer` 500 MB      — send a big pack in one request, no chunking
 *  - `http.maxRequestBuffer` 100 M — larger smart-HTTP request buffer
 *  - `http.lowSpeedLimit/Time`     — never abort a slow-but-alive upload
 *  - `--thin` push                 — omit objects the server already has
 *  - HTTP/1.1 by default           — git/Termux default; avoids HTTP/2 upload
 *                                    flow-control stalls on multi-hundred-MB packs
 *                                    (opt in to HTTP/2 via [http2])
 *  - token via `GIT_CONFIG_*` env  — never in argv, never in `ps`/logs
 */
class GitEngine(
    private val git: GitBinary,
    private val profile: TransportProfile = TransportProfiles.balanced,
) {
    companion object {
        /** GitHub hard-blocks a single file >= 100 MiB. */
        const val LARGE_FILE_LIMIT = 100L * 1024 * 1024
        /** GitHub warns from 50 MiB. */
        const val WARN_FILE_LIMIT = 50L * 1024 * 1024
    }

    private fun configArgs(): List<String> = buildList {
        fun c(k: String, v: String) { add("-c"); add("$k=$v") }
        c("protocol.version", "2")
        c("core.compression", profile.compression.toString())
        c("pack.threads", "0")
        c("pack.window", profile.packWindow.toString())
        c("pack.windowMemory", profile.windowMemory)
        c("pack.packSizeLimit", "2g")
        c("pack.deltaCacheSize", "128m")
        c("http.version", profile.httpVersion)
        c("http.postBuffer", profile.postBuffer)
        c("http.maxRequestBuffer", "100M")
        c("http.lowSpeedLimit", "0")
        c("http.lowSpeedTime", "999999")
        c("core.bigFileThreshold", "1m")
        c("feature.manyFiles", "true")
        c("gc.auto", "0")
        c("advice.detachedHead", "false")
    }

    private fun command(repoDir: File?, args: List<String>): List<String> = buildList {
        add(git.executable.absolutePath)
        if (repoDir != null) { add("-C"); add(repoDir.absolutePath) }
        addAll(configArgs())
        addAll(args)
    }

    /**
     * GitHub accepts HTTP Basic with username `x-access-token` and the token as
     * the password. Handed to git through the GIT_CONFIG_* env vars so the
     * credential never appears in the argument list.
     */
    private fun authEnv(token: String?): Map<String, String> {
        if (token.isNullOrBlank()) return emptyMap()
        val basic = Base64.encodeToString(
            "x-access-token:$token".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        return mapOf(
            "GIT_CONFIG_COUNT" to "1",
            "GIT_CONFIG_KEY_0" to "http.extraHeader",
            "GIT_CONFIG_VALUE_0" to "AUTHORIZATION: basic $basic",
        )
    }

    private fun newProcess(repoDir: File?, token: String?, args: List<String>): Process {
        val pb = ProcessBuilder(command(repoDir, args))
        pb.environment().putAll(git.baseEnv(authEnv(token)))
        return pb.start()
    }

    /** Run a short command to completion and return stdout. Throws [GitException]. */
    suspend fun run(repoDir: File?, token: String?, vararg args: String): String =
        withContext(Dispatchers.IO) {
            val proc = newProcess(repoDir, token, args.toList())
            // Drain both pipes concurrently so a chatty command can never deadlock.
            val outBuf = StringBuilder()
            val errBuf = StringBuilder()
            val tOut = Thread { runCatching { proc.inputStream.bufferedReader().forEachLine { outBuf.appendLine(it) } } }.apply { isDaemon = true; start() }
            val tErr = Thread { runCatching { proc.errorStream.bufferedReader().forEachLine { errBuf.appendLine(it) } } }.apply { isDaemon = true; start() }
            val code = proc.waitFor()
            tOut.join(500); tErr.join(500)
            if (code != 0) throw GitException(code, command(repoDir, args.toList()), errBuf.toString())
            outBuf.toString()
        }

    /** Stream a long command, emitting a [GitProgress] for each meaningful stderr line. */
    fun runStreaming(repoDir: File?, token: String?, vararg args: String): Flow<GitProgress> = flow {
        val proc = newProcess(repoDir, token, args.toList())
        val cmd = command(repoDir, args.toList())

        // Drain stdout on a daemon thread so the child never blocks on a full pipe.
        val drain = Thread { runCatching { proc.inputStream.readBytes() } }.apply {
            isDaemon = true
            start()
        }

        var lastRaw: String? = null
        var lastLine: String? = null
        try {
            proc.errorStream.bufferedReader().forEachLine { line ->
                // git rewrites progress with '\r'; normalise to individual ticks.
                for (piece in line.replace('\r', '\n').split('\n')) {
                    val tick = GitProgress.parse(piece) ?: continue
                    if (piece != lastRaw) { lastRaw = piece; lastLine = piece; emit(tick) }
                }
            }
            val code = proc.waitFor()
            if (code != 0) throw GitException(code, cmd, lastLine ?: "")
        } finally {
            runCatching { drain.join(200) }
            runCatching { proc.destroy() }
        }
    }.flowOn(Dispatchers.IO)

    // --- High-level operations --------------------------------------------------

    /**
     * Shallow, single-branch clone (depth 1, no tags). This is the minimum needed
     * to have a base commit to push onto, so a large remote history is never
     * downloaded — the same trick that makes a Termux push quick.
     */
    suspend fun cloneShallow(token: String?, remote: String, into: File): String {
        into.parentFile?.mkdirs()
        return run(
            null, token,
            "clone", "--depth", "1", "--no-tags", "--single-branch", "--progress",
            remote, into.absolutePath,
        )
    }

    /** `git fetch --depth 1` to refresh the remote head before a diff. */
    suspend fun fetchShallow(repoDir: File, token: String?, branch: String): String =
        run(repoDir, token, "fetch", "--depth", "1", "origin", branch)

    /** `git status --porcelain` — empty means the working tree is clean. */
    suspend fun porcelain(repoDir: File): String =
        run(repoDir, null, "status", "--porcelain")

    /**
     * Stage everything and commit if anything changed. Returns true if a commit
     * was made. `-A` picks up adds, edits and deletions in one pass.
     */
    suspend fun commitAll(repoDir: File, message: String): Boolean {
        run(repoDir, null, "add", "-A")
        if (porcelain(repoDir).isBlank()) return false
        run(
            repoDir, null,
            "-c", "user.name=Gitofy",
            "-c", "user.email=gitofy@users.noreply.github.com",
            "commit", "-m", message,
        )
        return true
    }

    /**
     * Push the current HEAD to `branch`, streaming progress. `--thin` keeps the
     * pack small by omitting objects the server already has; `--progress` forces
     * the throughput lines we parse.
     */
    fun push(repoDir: File, token: String?, branch: String, thin: Boolean = true): Flow<GitProgress> {
        val args = buildList {
            add("push")
            if (thin) add("--thin")
            add("--progress")
            add("origin")
            add("HEAD:$branch")
        }
        return runStreaming(repoDir, token, *args.toTypedArray())
    }

    /**
     * Files at or above [limit]. GitHub blocks any single file >= 100 MiB, so the
     * upload flow checks this before pushing and points the user at Git LFS.
     */
    fun oversizedFiles(dir: File, limit: Long = LARGE_FILE_LIMIT): List<Pair<String, Long>> =
        dir.walkTopDown()
            .filter { it.isFile && it.length() >= limit }
            .map { it.relativeTo(dir).path to it.length() }
            .toList()
}
