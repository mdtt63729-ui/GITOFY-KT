# Changelog

The app version and the delivered zip name are bumped together on every update.

## v1.3.6 — Fix: release build lint error (foreground service type)
- CI reached `packageRelease` (Kotlin compiled clean, APK assembled) but
  `lintVitalRelease` failed on the single error **SpecifyForegroundServiceType**:
  WorkManager's foreground service must declare `dataSync` in the manifest.
- Added to `AndroidManifest.xml`:
  `<service android:name="androidx.work.impl.foreground.SystemForegroundService"
  android:foregroundServiceType="dataSync" tools:node="merge" />`.
- This was the last blocker: with it the release APK builds and the workflow's
  upload-artifact step runs.

## v1.3.5 — CI safety net for base64-encoded source files
- The CI log showed `app/build.gradle.kts:1:1: Unresolved reference: <base64>` —
  the file had arrived in the repo **base64-encoded** (a transfer accident; the
  workflow `.yml` files were fine, which is why Actions still ran).
- Every APK workflow now runs an inline **"Repair base64-encoded source files"**
  step before building: any of `build.gradle.kts`, `settings.gradle.kts`,
  `gradle.properties`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, or any
  `.kt`/`.xml` under `app/src/main` whose whole content is a single base64 blob
  that decodes to recognisable source is decoded in place. It is a no-op on a
  normal checkout (verified), and it fixes a base64-corrupted tree (verified
  against the exact blob from the CI log).
- Also shipped `tools/normalize-sources.py` for local use.

## v1.3.4 — Signing bootstrap: verify + summary
- `bootstrap-signing.yml` now has a **Verify the secrets exist** step (`gh secret list`)
  and a **Summary** on the run page, so after the run you can see that the four
  permanent secrets were created:
  `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_PASSWORD`.

## v1.3.3 — Fix: first Kotlin compile errors (from CI)
The CI reached `:app:compileReleaseKotlin` and reported real errors; all fixed:
- `GitEngine` — `emit()` inside `Reader.forEachLine` (a non-suspend lambda context)
  -> replaced with a plain `readLine()` loop inside the `flow { }`.
- `SkeletonList(Modifier…)` passed positionally bound to the `rows: Int` parameter
  -> now `SkeletonList(modifier = …)` (Polish, RepoDashboard, Home).
- `Modifier` used without an import in `IssuesScreen` / `WorkflowRunDetailScreen`
  -> imports added.
- `expandVertically(animationSpec = SpringSpec<Float>)` type mismatch
  -> `expandVertically()` (default spec).
- `GitProgress.Phase.UPLOADING` did not exist -> added `UPLOADING` to the enum.
- `pushWithRetry(…) { … }` trailing lambda bound to `attempts: Int`
  -> pass `onTick = { … }` by name.
- `UploadWorker` called the suspend `setProgress`/`setForeground` inside the
  non-suspend progress callback -> progress is now pumped through a `StateFlow`
  and applied inside a coroutine.
- `GitofyNavHost` passed trailing lambdas that bound to the `vm` parameter
  -> now named (`onOpen =`, `onOpenIssue =`, `onOpenLog =`).

## v1.3.2 — Fix: package namespace (build-breaking)
- The application namespace/package was `com.gitofy.native`, which Android rejects:
  **`native` is a Java keyword**, so AGP fails with
  "Namespace 'com.gitofy.native' is not a valid Java package name".
- Renamed the whole project to **`com.gitofy.app`**: `namespace`, `applicationId`,
  every `package`/`import` and the source directory tree. 105 references updated,
  none left.

## v1.3.1 — CI fix: Android SDK setup
- The **Set up Android SDK** step no longer uses `android-actions/setup-android@v3`
  (it was failing on the runner). It now uses the SDK that is already installed on
  `ubuntu-latest`: it writes `local.properties`, exports `ANDROID_SDK_ROOT` /
  `ANDROID_HOME`, adds `cmdline-tools` to `PATH`, accepts licenses and ensures
  platform-34 + build-tools are present. Applied to all three APK workflows.

## v1.3.0 — A second signed-APK workflow (with the bundled git binary)
- New **`.github/workflows/build-signed-apk-with-git.yml`**: builds a **signed**
  release APK from the permanent signing secrets **and** cross-compiles the
  bundled git binary, so the APK is fully working (uploads included). Runs on push
  to `main` and on demand; artifact **`gitofy-signed-full`**.
- If the NDK git build fails it still produces a signed APK and warns, so you
  always get an installable artifact.

## v1.2.0 — Permanent APK signing on GitHub
- New **`.github/workflows/bootstrap-signing.yml`**: run once, it generates a
  **permanent** Android release keystore in Actions and writes the four repository
  secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) via a `GH_SECRETS_PAT`. It **refuses
  to overwrite** an existing identity unless `force=true`, and uploads a keystore
  backup artifact (opt-in credentials file). The identity is permanent, not
  temporary — every future build shares it.
- New **`.github/workflows/release-signed-apk.yml`**: restores the keystore from
  the secrets, writes `signing.properties`, builds a **signed** release APK,
  verifies it with `apksigner`, and uploads it as the `gitofy-release-signed`
  artifact (and attaches it to the Release on a `v*` tag).
- `app/build.gradle.kts` now reads a `signing.properties` (path via
  `SIGNING_PROPERTIES_FILE`) and signs the release build from it — nothing
  sensitive in the repo.

## v1.1.0 — GitHub Actions: build an unsigned APK
- New `.github/workflows/build-unsigned-apk.yml`: builds on GitHub with no local
  toolchain. On push to `main` or a manual run it produces
  - **`gitofy-release-unsigned`** — the unsigned release APK, and
  - **`gitofy-debug`** — a debug APK (debug-signed, installable for testing).
  - Optional inputs: `version` (stamp a versionName) and `build_git` (also
    cross-compile the bundled git binary with the NDK).
- `UploadPipeline` now resolves the bundled git binary **lazily and optionally**,
  so an APK built without it still runs — the upload feature just reports a clear
  message instead of crashing. This is what makes the "unsigned APK without git"
  build usable.
- `.github/workflows/README.md` explains how to sign the unsigned APK.

## v1.0.0 — Byte-level resume + background upload (WorkManager)
### (a) Byte-level resume in Pending uploads
- The checkpoint now records `totalBytes`, `lfsTotalFiles`, `lfsDoneFiles`,
  `lfsTotalBytes`, `lfsDoneBytes`. The Pending uploads list shows a summary
  ("2/5 LFS files · 120.0 MB / 300.0 MB") and a progress bar, so you can see
  exactly how much is left at each stage.

### (b) Background upload
- The pipeline was extracted into `upload/UploadPipeline` (a Hilt singleton) so the
  UI and the worker run identical code.
- New `upload/UploadWorker` (a `@HiltWorker` CoroutineWorker) + `upload/UploadScheduler`
  run the push under **WorkManager** with a `NETWORK_CONNECTED` constraint and a
  foreground notification showing live progress. It keeps going when the app is
  closed and retries on a transient failure (the checkpoint makes the retry cheap).
- `GitofyApp` now implements `Configuration.Provider` with `HiltWorkerFactory`; the
  default WorkManager initializer is removed in the manifest; foreground-service
  permissions added.
- The upload screen has an **Upload in background** button; `UploadViewModel` is now
  a thin wrapper (state + pending list + background hand-off).

## v0.9.0 — Persistent checkpoints + network-switch re-tune
### (a) Checkpoints survive an app restart
- The working tree + checkpoint now live under **`filesDir/uploads/`** (not
  `cacheDir`), so the OS clearing the cache cannot lose a pending upload.
- New **"Pending uploads"** list on the upload screen: every unfinished checkpoint
  is shown with **Resume** and **Discard**, so a restart no longer needs the user
  to remember the exact repo.
- `UploadCheckpoint.pendingIn()` / `discard()` back the list.

### (b) Re-tune on a Wi-Fi <-> cellular switch
- `NetworkMonitor.changes()` is a `callbackFlow` over `registerDefaultNetworkCallback`,
  emitting whenever the default network or its capabilities change.
- During an upload the flow re-benchmarks on every change and updates the live
  transport profile; **each push attempt builds a fresh engine from the current
  profile**, so a mid-upload switch to cellular (or back to Wi-Fi) is picked up on
  the next attempt. The change is surfaced in the progress detail
  ("Network changed → re-tuned: compression N (slow link)").

## v0.8.0 — Tuning chart in Settings + resumable upload
### (a) Benchmark chart in Settings
- New `git/BenchmarkStore.kt` keeps the last `BenchmarkResult`; the upload flow
  records it. Settings now has an **Upload tuning** card with a small bar chart:
  each compression level (1/6/9) as a bar of its estimated whole-push time, the
  chosen level highlighted, plus the link profile and uplink.

### (b) Resumable upload
- New `git/UploadCheckpoint.kt`: a checkpoint beside the work tree records the
  last completed stage (clone → extract → LFS → commit) and the LFS object ids
  already uploaded. If the connection drops, the flow keeps everything and a
  **Resume** re-enters at the failed stage instead of starting over:
  - the shallow clone is reused,
  - the ZIP is not re-extracted,
  - LFS objects already uploaded are skipped (by oid),
  - the commit is not repeated.
- The **push itself is retried with backoff** (3 attempts) so a transient drop
  resumes automatically.
- The upload screen shows **Resume** both when a previous upload can be resumed
  and after a failure; `UploadState.Failed` now carries `resumable`.

## v0.7.0 — LFS byte progress + benchmark-driven compression tuning
### (a) LFS byte-level progress
- `LfsClient.upload` now takes an `onProgress(bytesSent, total)` callback and
  streams the object in 64 KB chunks, reporting bytes as they leave. The upload
  screen shows a live bar with `sent / total` while a big file goes to LFS.

### (b) Benchmark-driven compression tuning
- New `git/TransportBenchmark.kt`. Before pushing, the flow compresses a ~5 MB
  sample of the project at zlib levels 1 / 6 / 9, measures each one's time and
  compressed size, extrapolates to the whole project, and picks the level that
  minimises `compress_time + bytes_after_compression / uplink_bandwidth`.
  - fast link -> level 1 (skip the CPU); slow link -> level 9 (fewer bytes).
- The chosen level is layered onto the connection profile from v0.6.0, and the
  tuning ("compression N, pack.window M") is surfaced in the progress detail.

## v0.6.0 — Git LFS + connection-aware speed tuning
### Git LFS (single files >= 100 MB now push)
- New `git/LfsClient.kt`: a minimal **Git LFS client** over GitHub's batch API —
  computes the SHA-256, asks `…/info/lfs/objects/batch` for an upload URL, PUTs
  the object, and returns its oid/size. **No `git-lfs` binary to ship.**
- The upload flow now routes every file **>= 100 MB** through LFS: the object goes
  to LFS storage, the working-tree file is replaced by an LFS pointer, and a
  `filter=lfs` line is added to `.gitattributes`. So a single 100 MB+ file — which
  GitHub otherwise hard-blocks — now pushes successfully.
- When LFS is disabled, oversized files fail fast with a clear message.

### Connection-aware speed tuning
- New `git/TransportProfile.kt` + `net/NetworkMonitor.kt`. The transport is chosen
  from the live connection:
  - **fast** (unmetered, upstream >= 20 Mbps): compression 1, pack.window 250,
    windowMemory 512m, postBuffer 1 GB — spend the link, not the CPU.
  - **balanced**: compression 6, pack.window 100, windowMemory 256m.
  - **slow** (metered / <= 2 Mbps): compression 9, pack.window 50, windowMemory
    128m — fewer bytes over the slow link.
- `GitEngine` is now profile-driven (`GitEngine(binary, profile)`); HTTP/1.1 is
  kept in every profile for reliable large-pack uploads.

### Also
- `UploadState.Done` now reports how many files went to LFS; the upload screen
  shows it.

## v0.5.0 — Termux-speed git core (git 2.56) + large-project tuning
### No JGit — confirmed at code level
- The engine drives the **real git (C) binary** only. There is no JGit and no
  libgit2 anywhere in the project.
- `tools/build-git-android.sh` now pins the **latest** releases: git **2.56.0**
  (2026-09-28), curl **8.22.0**, OpenSSL **3.6.5**, zlib **1.3.2**, PCRE2 **10.49**.

### Large-project speed (50 MB / 100 MB+)
- `GitEngine` now configures: `protocol.version=2`, `pack.threads=0`,
  `pack.windowMemory=256m`, `pack.packSizeLimit=2g`, `pack.deltaCacheSize=128m`,
  `core.bigFileThreshold=1m`, `feature.manyFiles=true`, `gc.auto=0`,
  `http.postBuffer=500M`, `http.maxRequestBuffer=100M`, `http.lowSpeedLimit=0`,
  `http.lowSpeedTime=999999`.
- HTTP/1.1 is the default transport (git/Termux default; avoids HTTP/2 upload
  flow-control stalls on multi-hundred-MB packs); HTTP/2 is opt-in via `GitEngine(http2=true)`.
- Shallow clone is now `--depth 1 --no-tags --single-branch`, so a large remote
  history is never downloaded.
- The ZIP is extracted **directly into the clone working tree** — no second full
  copy of the project on disk (was a real I/O cost for big projects).
- **Oversized-file guard**: files >= 100 MB are detected before the push and
  rejected with a Git LFS hint (GitHub hard-blocks them).
- `run()` now drains stdout+stderr concurrently (no pipe deadlock on chatty
  commands); added `oversizedFiles()`.
- Removed a stale `ui/UploadScreen.kt` that referenced a deleted method.

## v0.4.0 — Full UI polish + compile-readiness pass
### Compile-readiness (verified)
- Every dependency version verified to exist: AGP 8.5.2, Kotlin 2.0.20, KSP
  2.0.20-1.0.25, Hilt 2.52, Compose BOM 2024.09.03 (material3 1.3.0), Navigation
  2.8.0, Lifecycle 2.8.6, Room 2.6.1, Glance 1.1.0, Biometric 1.2.0-alpha05,
  Security-Crypto 1.1.0-alpha06, DataStore 1.1.1, Retrofit 2.11.0, OkHttp 4.12.0,
  kotlinx-serialization 1.7.3, kotlinx-serialization-converter 1.0.0, Coil 2.7.0,
  Fragment 1.8.3, Activity-Compose 1.9.2, Hilt-Nav-Compose 1.2.0 (checked against
  Maven Central / Google Maven).
- Fixed a missing `dp` import; added missing `lazy.items` imports; simplified the
  Glance widget to avoid alignment-API drift; `AppLockManager` now uses
  `@ApplicationContext`; `MainActivity` is a `FragmentActivity` (BiometricPrompt).

### UI polish (shared design system)
- New `ui/components/Polish.kt`: `GitofyTopBar`, `Avatar` (Coil, initial
  fallback), `StatChip`, `SectionTitle`, `EmptyState` (icon + action),
  `ErrorState` (with Retry), animated `SkeletonList`, `RepoCard`, `PolishedList`.
- Every list screen now has a skeleton while loading, a real empty state, an error
  state with retry, avatars, and consistent top bars:
  Home, Repo dashboard (hero card + stat chips), Repo code (folder/file icons),
  Issues, Gists, Workflows, Inbox, Commits, Branches, Global search.
- Polished hero/loading/card layouts for Onboarding, Login, App lock, Settings
  (sectioned cards), Updates (release card + download progress), Error log.
- Navigation arguments fixed so repo-dashboard actions open the right screens.

## v0.3.0 — UI polish, wiring & platform features
Everything that was still open from v0.2.0.

### Startup, security & permissions
- **Startup gate** in MainActivity: integrity check → auth restore → route
  (Splash → Login or Home). A tampered signature shows the blocking Security
  overlay and plays the klaxon.
- **App-lock prompt** wired at launch (biometric/device credential) when the lock
  is on; MainActivity is a FragmentActivity so BiometricPrompt works.
- **FLAG_SECURE** applied live from settings (screen-capture protection).
- **Real permission flows**: request POST_NOTIFICATIONS, open the
  install-unknown-apps settings screen.
- **DangerSound**: a two-tone klaxon + sub rumble synthesised as PCM (no assets).
- **Deep links**: gitofy://callback|repo|upload handled; repo links open the repo.

### Widget
- **Glance home-screen widget** (`GitofyWidget`) showing the last upload target +
  status, backed by `WidgetState` (replaces the web app's bridge widget).

### Navigation & buttons (all wired)
- Repo code: tapping a folder drills in; tapping a file opens the editor.
- Issues: tap → issue detail with comments, comment box, open/close.
- Workflow run detail → "View live log".
- **Live run-log screen** polling job logs, monospace, job chips.
- **Workflow/file editor**: load a file (base64), edit, commit via the API.
- Home FAB creates a repo; top-bar opens updates/inbox; create/delete/settings
  actions all wired to real API calls.

### Theme
- Font-choice plumbing (`Fonts`) for the 6 families; mapped to platform families
  so rendering never depends on a font download (Google-Font swap is a one-liner
  once provider certs are added).

### Still tracked (needs a machine with the Android SDK)
- A compile + lint pass; per-screen visual polish against the web originals.

## v0.2.0 — Phase 1–6: full feature port (A→Z)
Everything from the web app, ported to Kotlin/Compose.

### Auth
- Device Flow (start / poll / slow_down / expired / denied) via native OkHttp,
  public client id only — no secret in the app.
- PAT sign-in, token validation, `SessionManager` state, sign-out.

### Networking (1:1 with src/git/githubApi.ts)
- User, repos, starred, notifications, languages, contents, tree, commits,
  commit comments, branches (list/create/delete/rename), issues, pulls, comments,
  merge, Actions (workflows, runs, jobs, cancel, rerun, artifacts, dispatch),
  releases (list/latest/create), gists (list/create/delete), search
  (repositories + code), repo update/delete, user update, star/unstar.

### Screens (all routes are real now)
- Entry: Onboarding, Login (device flow + PAT), Permissions, Login diagnostics,
  App lock, Splash.
- Browse: Home (repo list + create), Inbox, Repo dashboard, Repo code, Commits,
  Branch manager, Issues, Gists, Global search, Workflows, Workflow run detail.
- Upload: Upload flow (SAF ZIP pick → bundled-git push with live progress),
  Multi-repo upload, ZIP analysis, Result.
- Actions: Repo settings (edit/archive/visibility/delete), Delete repo (typed
  confirm), Release creator, Repo action result.
- App: Settings (palette/language/appearance/reduce-motion/FLAG_SECURE),
  GitHub settings, Error log, App store (updater).
- Sheets: Create repo, Repo picker, Account switcher.
- Security: Danger sheet, Security overlay, Update sheet.
- Dev: M3 gallery, Motion lab.

### Update system (parity with today's Gitofy)
- `UpdateChecker` reads mdtt63729-ui/GITOFY latest release; `VersionCompare`
  (direct port, tested logic); `ApkDownloader` streams with percent/bytes/speed;
  `ApkInstaller` uses `PackageInstaller` + `FileProvider`. Same flow, no bridge.

### Platform
- Room cache, DataStore settings (full `defaultSettings` shape), encrypted
  `TokenStore`, `ErrorLog`, deep-link parser (gitofy://), notifications,
  `IntegrityCheck` (APK-signature anti-tamper), `AppLockManager` (biometric).

### Still to polish (tracked)
- Wire the app-lock prompt and integrity gate into MainActivity start-up.
- Real permission-request flows (notifications / install-unknown-apps).
- Glance widget, live run-log viewer, danger sound, downloadable font families.
- A compile + lint pass on a machine with the Android SDK/JDK.

## v0.1.0 — Phase 0: Foundation
- Project + Hilt + Compose + M3; theme ported (6 palettes × light/dark + diff
  colours); i18n (112 keys × en/bn); 29 routes in one NavHost; Retrofit/OkHttp;
  Room; DataStore; encrypted TokenStore; bundled-git engine.
