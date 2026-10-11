package com.gitofy.native.update

import com.gitofy.native.data.remote.GitHubApiService
import com.gitofy.native.model.GitHubRelease
import javax.inject.Inject
import javax.inject.Singleton

/** Release info the updater needs, mirroring updater.ts ReleaseInfo. */
data class ReleaseInfo(
    val tag: String,
    val version: String,
    val apkName: String,
    val downloadUrl: String,
    val apiAssetUrl: String,
    val notes: String,
    val publishedAt: String,
)

@Singleton
class UpdateChecker @Inject constructor(private val api: GitHubApiService) {

    companion object { const val RELEASE_REPO_OWNER = "mdtt63729-ui"; const val RELEASE_REPO_NAME = "GITOFY" }

    /** Reads the latest release of the app's own repo (same as the web app). */
    suspend fun fetchLatest(): ReleaseInfo? = runCatching {
        val r: GitHubRelease = api.latestRelease(RELEASE_REPO_OWNER, RELEASE_REPO_NAME)
        val asset = r.assets.firstOrNull { it.name.lowercase().endsWith(".apk") } ?: return null
        ReleaseInfo(
            tag = r.tagName,
            version = r.tagName.replace(Regex("^v", RegexOption.IGNORE_CASE), ""),
            apkName = asset.name,
            downloadUrl = asset.browserDownloadUrl,
            apiAssetUrl = "https://api.github.com/repos/$RELEASE_REPO_OWNER/$RELEASE_REPO_NAME/releases/assets/${asset.id}",
            notes = r.body.orEmpty(),
            publishedAt = r.publishedAt,
        )
    }.getOrNull()
}
