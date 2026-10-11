# Building the bundled git binary

`build-git-android.sh` cross-compiles upstream **git (C)** plus its static
dependencies (zlib, OpenSSL, PCRE2, curl) with the Android NDK, then copies the
resulting binary to `app/src/main/jniLibs/<abi>/libgit.so`.

Why this and not JGit: git's C pack generation and delta compression are what
give Termux its push speed. JGit is pure-JVM and noticeably slower / more
memory-hungry for the same pack, so bundling the real binary is the only way to
match it.

## One-time setup
1. Install the Android NDK (r26 or newer) via the SDK Manager.
2. `export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/26.1.10909125`
3. Build each ABI you ship:
   ```
   tools/build-git-android.sh arm64-v8a
   tools/build-git-android.sh armeabi-v7a
   ```

## Version pins (latest)
- git **2.56.0** (latest feature release, 2026-09-28)
- curl **8.22.0**, OpenSSL **3.6.5**, zlib **1.3.2**, PCRE2 **10.49**
Override any with an env var, e.g. `GIT_VERSION=2.56.0 tools/build-git-android.sh arm64-v8a`.

## Why the real git binary (and not JGit)
The app never uses JGit or libgit2. git's C implementation of pack generation,
delta compression and the Smart-HTTP transport is exactly what makes Termux fast;
shipping and driving the real binary is the only way to match it.

## Notes / gotchas
- **Exec location.** The binary is named `libgit.so` on purpose. Android extracts
  `lib*.so` files from `jniLibs` into `nativeLibraryDir`, and that is one of the
  few directories the OS still lets you execute from (Android 10+ blocks exec
  from the writable data dir). `app/build.gradle.kts` sets
  `jniLibs.useLegacyPackaging = true` so the file is extracted at install time.
- **Static linking.** curl/OpenSSL/zlib/PCRE2 are linked statically into the one
  binary, so nothing else has to ship in `jniLibs`. Expect ~8–14 MB per ABI.
- **Version pinning.** Override with env vars, e.g.
  `GIT_VERSION=2.46.0 CURL_VERSION=8.9.1 tools/build-git-android.sh arm64-v8a`.
- **Play Store.** Shipping a full git binary is legal (it is a native binary in
  the APK, not downloaded code), but reviewers may ask about it. Distributing
  through CI releases / side-loading avoids that conversation.
- If a configure step fails, check that `PATH` picked up the NDK clang wrappers
  (`$TOOLCHAIN/bin`) and that `$API` matches your `minSdk`.
