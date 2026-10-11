# CI — build APKs on GitHub (no local toolchain)

Three workflows, run from the **Actions** tab.

## 1. `bootstrap-signing.yml` — run ONCE, permanent
Generates the Android release keystore inside Actions and writes the four
repository secrets, so **every future release is signed with the same permanent
identity**.

- Refuses to overwrite an existing identity unless you tick **force** (that would
  break updates for installed apps).
- To let it create the secrets automatically, add a repository secret named
  **`GH_SECRETS_PAT`** (fine-grained token with *Secrets: Read and write*, or a
  classic token with the *repo* scope). Without it, the keystore is still generated
  and offered as a backup artifact for manual setup.
- Secrets created: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- It always uploads a **keystore backup artifact** (90 days). Tick
  `export_credentials` to also include the passwords in that artifact — then keep
  it very private, because it can sign apps as Gitofy.

## 2. `release-signed-apk.yml` — the signed APK
Builds a **signed** release APK from the permanent secrets and uploads it as the
`gitofy-release-signed` artifact. Runs on a `v*` tag (and then also attaches the
APK to the Release) or manually with an optional `version`.

## 3. `build-signed-apk-with-git.yml` — signed APK, fully working
Same permanent signing secrets, but it **also cross-compiles the bundled git
binary**, so the APK's upload feature works out of the box. Runs on push to `main`
and on demand; artifact **`gitofy-signed-full`**. If the NDK git build fails it
still produces a signed APK and warns you.

## 4. `build-unsigned-apk.yml` — the unsigned APK
Builds an **unsigned** release APK (`gitofy-release-unsigned`) plus a debug APK
(`gitofy-debug`, installable for testing). Optional `build_git` input cross-compiles
the bundled git binary with the NDK.

## Recommended order
1. Run **Bootstrap signing secrets** once (add `GH_SECRETS_PAT` first for auto-setup).
2. Run **Build signed release APK** — download `gitofy-release-signed`.
3. Install it, or push a `v1.2.0` tag to also publish a Release.

## Note on the bundled git binary
`release-signed-apk.yml` and `build-unsigned-apk.yml` do **not** build the git
binary by default; the APK still runs, but the upload feature is disabled until you
build it (`build_git: true` on the unsigned workflow, or
`tools/build-git-android.sh` locally).
