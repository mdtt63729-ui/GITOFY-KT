package com.gitofy.native.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ported from the web app's src/types/index.ts. */

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
)

@Serializable
data class Repository(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    val fork: Boolean = false,
    val archived: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    val language: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val size: Long? = null,
    val owner: Owner? = null,
) {
    /** The web app's repoOwnerLogin() helper. */
    val ownerLogin: String get() = owner?.login ?: fullName.substringBefore('/')
}

// --- Upload / diff ----------------------------------------------------------

enum class DiffStatus { ADDED, MODIFIED, DELETED, UNCHANGED }

data class DiffItem(
    val path: String,
    val status: DiffStatus,
    val size: Long = 0,
    val blobSha: String? = null,
    val remoteSha: String? = null,
    val linesAdded: Int? = null,
    val linesRemoved: Int? = null,
)

data class DiffSummary(
    val added: Int,
    val modified: Int,
    val deleted: Int,
    val unchanged: Int,
    val totalFiles: Int,
    val totalSize: Long,
    val items: List<DiffItem>,
)

enum class UploadPhase {
    IDLE, VALIDATING, EXTRACTING, INDEXING, HASHING, DIFFING, GIT_PREP,
    UPLOADING, COMMITTING, PUSHING, VERIFYING, COMPLETED, PAUSED, ERROR, CANCELLED
}

data class UploadState(
    val phase: UploadPhase = UploadPhase.IDLE,
    val progress: Int = 0,
    val currentFile: String = "",
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val uploadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val speed: String = "",
    val eta: String? = null,
    val diffSummary: DiffSummary? = null,
    val commitSha: String? = null,
    val verified: Boolean? = null,
    val errorMessage: String? = null,
)

// --- Workflows --------------------------------------------------------------

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String = "",
    @SerialName("workflow_id") val workflowId: Long = 0,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String = "",
    val event: String = "",
    val status: String = "",
    val conclusion: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("run_number") val runNumber: Int = 0,
    val actor: Owner? = null,
)

@Serializable
data class WorkflowItem(
    val id: Long,
    val name: String,
    val path: String = "",
    val state: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
)

data class InboxItem(
    val id: String,
    val title: String,
    val repo: String,
    val summary: String,
    val type: String,            // push | workflow | repo_action | github
    val status: String? = null,  // success | failure | info
    val timestamp: String,
    val read: Boolean = false,
    val details: String? = null,
    val commitSha: String? = null,
    val branch: String? = null,
    val linkUrl: String? = null,
)

// --- Releases ---------------------------------------------------------------

@Serializable
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("content_type") val contentType: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class GitHubRelease(
    val id: Long,
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val author: Owner? = null,
    val assets: List<ReleaseAsset> = emptyList(),
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long = 0,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String = "",
)
