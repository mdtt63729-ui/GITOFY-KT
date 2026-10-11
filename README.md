# Gitofy Native — Kotlin + Jetpack Compose (Termux-speed uploads)

A 100% native port of the Gitofy GitHub client. No WebView for app UI; the upload
core ships the real **git (C) binary** driven by `ProcessBuilder` for Termux-level
push speed. See `GITOFY-KOTLIN-PLAN.md` for the full phased plan.

**Current version: v1.3.1 (CI SDK-setup fix).** Version and zip name bump on
every delivered update.

## What's in v0.3.0 (adds to v0.2.0)
- Startup gate (integrity → auth → route), biometric app-lock prompt, FLAG_SECURE.
- Real permission flows; deep links; Glance widget; danger klaxon.
- Live run-log screen; issue detail with comments; file/workflow editor.
- Repo-code drill-down navigation; every button wired to a real call.

## What's in v0.2.0
- Everything in v0.1.0, plus:
- **Every route is a real screen** (auth, browse, upload, actions, settings,
  sheets, security, dev) — no placeholders.
- **Full GitHub REST surface** (repos, issues, pulls, Actions, releases, gists,
  search, branches, contents, commits).
- **Device-flow login** + PAT + session state.
- **Update system** (release check → streamed APK download → in-app install),
  the same flow the web app had, native.
- Room cache, DataStore settings, encrypted token, error log, deep links,
  notifications, integrity check, biometric app lock.

## Layout
```
app/src/main/java/com/gitofy/native/
  GitofyApp.kt                 @HiltAndroidApp
  MainActivity.kt              theme + NavHost host
  di/                          Hilt modules (network, database, datastore)
  data/                        TokenStore, remote (Retrofit), local (Room)
  settings/                    GitofySettings + SettingsStore (DataStore)
  model/                       domain models (ported from types/index.ts)
  git/                         GitBinary, GitEngine, GitProgress (bundled git C)
  ui/theme/                    Color (12 schemes), Theme, Type, Shape, Motion
  ui/navigation/               Routes (29) + GitofyNavHost
  ui/screens/                  screens (home live; others placeholder)
tools/build-git-android.sh     cross-compile git → jniLibs/<abi>/libgit.so
```

## Build on GitHub (no local toolchain)
Push to `main`, or run **Actions → Build unsigned APK → Run workflow**. You get an
**unsigned release APK** (`gitofy-release-unsigned`) and a debug APK
(`gitofy-debug`, installable). For a **signed** APK, run **Bootstrap signing secrets** once (permanent keystore +
repo secrets) and then **Build signed release APK** → artifact
`gitofy-release-signed`. Enable the `build_git` input to also cross-compile the
bundled git binary. See `.github/workflows/README.md`.

## Build locally
```bash
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/26.1.10909125
tools/build-git-android.sh arm64-v8a     # once per ABI
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleRelease
```
One APK per ABI; `jniLibs.useLegacyPackaging = true` keeps `libgit.so` extracted
and executable from `nativeLibraryDir`.
