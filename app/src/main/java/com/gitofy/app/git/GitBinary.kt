package com.gitofy.app.git

import android.content.Context
import java.io.File

/**
 * Resolves the bundled git C binary and builds a safe, hermetic environment
 * for it.
 *
 * The binary ships in the APK as `jniLibs/<abi>/libgit.so`. Android extracts any
 * `lib*.so` from jniLibs into [android.content.pm.ApplicationInfo.nativeLibraryDir],
 * which is one of the few locations the OS still allows exec from on Android 10+
 * (executing a file from the writable data dir is blocked). So we run it straight
 * from there — the same trick Termux relies on.
 */
class GitBinary private constructor(
    val executable: File,
    val home: File,
    val tmp: File,
) {
    /** Environment that keeps git non-interactive and independent of the device. */
    fun baseEnv(extra: Map<String, String> = emptyMap()): Map<String, String> = buildMap {
        put("HOME", home.absolutePath)
        put("TMPDIR", tmp.absolutePath)
        put("GIT_TERMINAL_PROMPT", "0")          // never block waiting for credentials
        put("GIT_ASKPASS", "/system/bin/true")   // fail fast instead of prompting
        put("GIT_CONFIG_NOSYSTEM", "1")
        put("GIT_CONFIG_GLOBAL", File(home, ".gitconfig").absolutePath)
        put("GIT_LFS_SKIP_SMUDGE", "1")
        put("LC_ALL", "C")
        putAll(extra)
    }

    companion object {
        fun resolve(context: Context): GitBinary {
            val nativeDir = File(context.applicationInfo.nativeLibraryDir)
            val exe = File(nativeDir, "libgit.so")
            require(exe.exists()) {
                "Bundled git not found at ${exe.absolutePath}. " +
                    "Run tools/build-git-android.sh <abi> first."
            }
            require(exe.canExecute()) { "Bundled git is not executable: ${exe.absolutePath}" }
            val home = File(context.filesDir, "git-home").apply { mkdirs() }
            val tmp = File(context.cacheDir, "git-tmp").apply { mkdirs() }
            return GitBinary(exe, home, tmp)
        }
    }
}
