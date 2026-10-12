package com.gitofy.app.upload

import android.content.Context
import android.net.Uri
import com.gitofy.app.data.TokenStore
import com.gitofy.app.git.BenchmarkStore
import com.gitofy.app.git.GitBinary
import com.gitofy.app.git.GitEngine
import com.gitofy.app.git.GitProgress
import com.gitofy.app.git.LfsClient
import com.gitofy.app.git.TransportBenchmark
import com.gitofy.app.git.TransportProfiles
import com.gitofy.app.git.UploadCheckpoint
import com.gitofy.app.net.NetworkMonitor
import com.gitofy.app.util.formatSize
import com.gitofy.app.widget.WidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The whole push pipeline, extracted so both the UI ([com.gitofy.app.ui.UploadViewModel])
 * and the background [UploadWorker] run exactly the same code.
 *
 * Stages: shallow clone -> extract -> benchmark/tune -> Git LFS -> commit -> push,
 * with a checkpoint after each stage so a drop (or an app restart) can resume.
 */
@Singleton
class UploadPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
    private val network: NetworkMonitor,
    private val http: OkHttpClient,
    private val json: Json,
    private val benchmarks: BenchmarkStore,
) {
    /** Null when the APK was built without the bundled git binary. */
    private val binary: GitBinary? by lazy { runCatching { GitBinary.resolve(context) }.getOrNull() }
    private val lfs = LfsClient(http, json)

    private val lfsThreshold = GitEngine.LARGE_FILE_LIMIT
    private val benchmarkSampleBytes = 5L * 1024 * 1024

    fun uploadsDir(): File = File(context.filesDir, "uploads").apply { mkdirs() }
    fun workDir(repo: String): File = File(uploadsDir(), repo)

    suspend fun run(req: UploadRequest, onProgress: (PipelineProgress) -> Unit): UploadOutcome {
        val git = binary ?: return UploadOutcome.Failure(
            "This build has no bundled git binary. Build one with tools/build-git-android.sh (or enable 'build_git' in the CI workflow).",
            false,
        )
        val token = tokenStore.token()
            ?: return UploadOutcome.Failure("Not signed in — no GitHub token.", false)

        val work = workDir(req.repo)
        var cp: UploadCheckpoint = if (req.resume) {
            UploadCheckpoint.load(work)
                ?: UploadCheckpoint(req.owner, req.repo, req.branch, UploadCheckpoint.Stage.NONE, zipUri = req.zipUri)
        } else {
            work.deleteRecursively(); UploadCheckpoint.clear(work)
            UploadCheckpoint(req.owner, req.repo, req.branch, UploadCheckpoint.Stage.NONE, zipUri = req.zipUri)
        }
        val zipUri = Uri.parse(req.zipUri)

        return coroutineScope {
            var netJob: Job? = null
            try {
                val net = network.current()
                val base = TransportProfiles.forNetwork(net.metered, net.upstreamKbps)
                val profileFlow = MutableStateFlow(base)

                fun emit(phase: GitProgress.Phase, pct: Int?, speed: String?, detail: String) =
                    onProgress(PipelineProgress(phase, pct, speed, detail, cp.stage.name, cp.lfsDoneBytes, cp.lfsTotalBytes))

                // 1. Shallow clone.
                if (cp.stage.ordinal < UploadCheckpoint.Stage.CLONED.ordinal) {
                    emit(GitProgress.Phase.OTHER, 0, null, "Cloning (${base.name} link)…")
                    GitEngine(git, base).cloneShallow(token, "https://github.com/${req.owner}/${req.repo}.git", work)
                    cp = cp.copy(stage = UploadCheckpoint.Stage.CLONED); cp.save(UploadCheckpoint.fileFor(work))
                }
                if (req.clearFirst) clearWorkingTree(work)

                // 2. Extract.
                if (cp.stage.ordinal < UploadCheckpoint.Stage.EXTRACTED.ordinal) {
                    emit(GitProgress.Phase.OTHER, 0, null, "Extracting…")
                    extractZip(zipUri, work)
                    cp = cp.copy(stage = UploadCheckpoint.Stage.EXTRACTED); cp.save(UploadCheckpoint.fileFor(work))
                }

                // 3. Benchmark ~5 MB; record for Settings; pick the profile.
                val files = work.walkTopDown().filter { it.isFile && !it.path.contains("/.git/") }.toList()
                val totalBytes = files.sumOf { it.length() }
                val sample = sampleOf(files, benchmarkSampleBytes)
                val result = TransportBenchmark.analyze(sample, totalBytes, net.upstreamKbps, base.name)
                benchmarks.record(result)
                val level = result.levels.firstOrNull { it.chosen }?.level ?: 6
                profileFlow.value = base.copy(compression = level)
                cp = cp.copy(totalBytes = totalBytes); cp.save(UploadCheckpoint.fileFor(work))
                emit(GitProgress.Phase.OTHER, 0, null, "Tuned: compression $level, pack.window ${profileFlow.value.packWindow} (${base.name} link)")

                // Re-tune on a network switch (Wi-Fi <-> cellular).
                netJob = launch {
                    network.changes().collect { info ->
                        val base2 = TransportProfiles.forNetwork(info.metered, info.upstreamKbps)
                        val lvl2 = TransportBenchmark.chooseCompression(sample, totalBytes, info.upstreamKbps)
                        val p2 = base2.copy(compression = lvl2)
                        if (p2 != profileFlow.value) {
                            profileFlow.value = p2
                            emit(GitProgress.Phase.OTHER, null, null, "Network changed → re-tuned: compression $lvl2 (${base2.name} link)")
                        }
                    }
                }

                // 4. Oversized files -> Git LFS (skip already-uploaded, byte progress).
                if (cp.stage.ordinal < UploadCheckpoint.Stage.LFS.ordinal) {
                    val big = files.filter { it.length() >= lfsThreshold }
                    if (big.isNotEmpty()) {
                        if (!req.useLfs) {
                            val list = big.take(5).joinToString("\n") { "• ${it.relativeTo(work).path} (${it.length() / (1024 * 1024)} MB)" }
                            return@coroutineScope UploadOutcome.Failure("Files >= 100 MB need Git LFS (disabled):\n$list", false)
                        }
                        val exts = mutableSetOf<String>()
                        val done = cp.doneOids.toMutableSet()
                        var doneBytes = cp.lfsDoneBytes
                        val lfsTotalBytes = big.sumOf { it.length() }
                        cp = cp.copy(lfsTotalFiles = big.size, lfsTotalBytes = lfsTotalBytes)
                        for ((i, f) in big.withIndex()) {
                            val oid = lfs.oidOf(f)
                            if (oid in done) continue
                            val total = f.length()
                            val baseDone = doneBytes
                            val obj = lfs.upload(req.owner, req.repo, token, f) { sent, size ->
                                val pct = if (size > 0) ((sent * 100) / size).toInt() else 0
                                onProgress(
                                    PipelineProgress(
                                        GitProgress.Phase.UPLOADING, pct, null,
                                        "LFS ${i + 1}/${big.size}: ${f.name} — ${formatSize(sent)} / ${formatSize(total)}",
                                        cp.stage.name, baseDone + sent, lfsTotalBytes,
                                    ),
                                )
                            }
                            f.writeText(lfs.pointer(obj.oid, obj.size))
                            f.name.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { exts.add(it) }
                            done.add(obj.oid)
                            doneBytes += total
                            cp = cp.copy(doneOids = done.toList(), lfsDoneFiles = done.size, lfsDoneBytes = doneBytes)
                            cp.save(UploadCheckpoint.fileFor(work))
                        }
                        if (exts.isNotEmpty()) appendGitattributes(work, exts)
                    }
                    cp = cp.copy(stage = UploadCheckpoint.Stage.LFS); cp.save(UploadCheckpoint.fileFor(work))
                }

                // 5. Commit + push (retried; each attempt uses the current profile).
                if (cp.stage.ordinal < UploadCheckpoint.Stage.COMMITTED.ordinal) {
                    GitEngine(git, profileFlow.value).commitAll(work, req.commitMessage)
                    cp = cp.copy(stage = UploadCheckpoint.Stage.COMMITTED); cp.save(UploadCheckpoint.fileFor(work))
                }
                pushWithRetry({ GitEngine(git, profileFlow.value) }, work, token, req.branch) { p ->
                    onProgress(PipelineProgress(p.phase, p.percent, p.speed, p.raw, cp.stage.name))
                }

                UploadCheckpoint.clear(work)
                WidgetState.write(context, "${req.owner}/${req.repo}", "Pushed to ${req.branch}")
                UploadOutcome.Success(req.branch, cp.doneOids.size)
            } catch (t: Throwable) {
                cp.save(UploadCheckpoint.fileFor(work))
                UploadOutcome.Failure(t.message ?: t.javaClass.simpleName, resumable = true)
            } finally {
                netJob?.cancel()
            }
        }
    }

    private suspend fun pushWithRetry(
        engineFor: () -> GitEngine, work: File, token: String, branch: String,
        onTick: (GitProgress) -> Unit, attempts: Int = 3,
    ) {
        var last: Throwable? = null
        for (i in 1..attempts) {
            try {
                engineFor().push(work, token, branch).collect { onTick(it) }
                return
            } catch (t: Throwable) {
                last = t
                if (i < attempts) { onTick(GitProgress(GitProgress.Phase.OTHER, null, null, null, "Push failed — retrying ($i/$attempts)…")); delay(2000L * i) }
            }
        }
        throw last ?: IllegalStateException("push failed")
    }

    private fun sampleOf(files: List<File>, maxBytes: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (f in files) {
            if (out.size() >= maxBytes) break
            runCatching {
                f.inputStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (out.size() < maxBytes) {
                        val n = input.read(buf, 0, minOf(buf.size.toLong(), maxBytes - out.size()).toInt())
                        if (n <= 0) break
                        out.write(buf, 0, n)
                    }
                }
            }
        }
        return out.toByteArray()
    }

    private fun clearWorkingTree(dir: File) {
        dir.listFiles()?.forEach { f -> if (f.name != ".git") f.deleteRecursively() }
    }

    private fun appendGitattributes(dir: File, exts: Set<String>) {
        val file = File(dir, ".gitattributes")
        val existing = if (file.exists()) file.readText() else ""
        val lines = exts.filter { !existing.contains("*.$it ") }.map { "*.$it filter=lfs diff=lfs merge=lfs -text" }
        if (lines.isNotEmpty()) {
            file.writeText(existing + (if (existing.isNotEmpty() && !existing.endsWith("\n")) "\n" else "") + lines.joinToString("\n") + "\n")
        }
    }

    private fun extractZip(zipUri: Uri, dest: File) {
        val destPath = dest.canonicalPath
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = File(dest, entry.name)
                    if (!target.canonicalPath.startsWith(destPath)) { entry = zip.nextEntry; continue }
                    if (entry.isDirectory) target.mkdirs()
                    else { target.parentFile?.mkdirs(); target.outputStream().use { zip.copyTo(it) } }
                    zip.closeEntry(); entry = zip.nextEntry
                }
            }
        } ?: error("Could not open the selected file")
    }
}
