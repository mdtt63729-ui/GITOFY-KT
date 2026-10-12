package com.gitofy.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class BranchDto(val name: String, val commit: CommitRef = CommitRef())
@Serializable data class CommitRef(val sha: String = "", val url: String = "")

@Serializable
data class CommitDto(
    val sha: String,
    val commit: CommitDetail = CommitDetail(),
    val author: UserRef? = null,
)

@Serializable data class CommitDetail(
    val message: String = "",
    val author: CommitAuthor = CommitAuthor(),
)
@Serializable data class CommitAuthor(val name: String = "", val email: String = "", val date: String = "")
@Serializable data class UserRef(val login: String = "", @SerialName("avatar_url") val avatarUrl: String = "")

@Serializable
data class ContentDto(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    val type: String = "file",          // file | dir
    @SerialName("download_url") val downloadUrl: String? = null,
    val content: String? = null,
    val encoding: String? = null,
)

@Serializable
data class TreeResponse(val sha: String = "", val tree: List<TreeEntry> = emptyList(), val truncated: Boolean = false)
@Serializable data class TreeEntry(
    val path: String = "", val mode: String = "", val type: String = "",
    val sha: String = "", val size: Long? = null,
)

@Serializable
data class IssueDto(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String = "open",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val user: UserRef? = null,
    val labels: List<LabelDto> = emptyList(),
    @SerialName("comments") val commentsCount: Int = 0,
    @SerialName("pull_request") val pullRequest: PullRef? = null,
)
@Serializable data class LabelDto(val name: String = "", val color: String = "cccccc")
@Serializable data class PullRef(val url: String = "", @SerialName("html_url") val htmlUrl: String = "")
@Serializable data class IssueCommentDto(
    val id: Long, val body: String = "", val user: UserRef? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class GistDto(
    val id: String,
    val description: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val files: Map<String, GistFile> = emptyMap(),
)
@Serializable data class GistFile(val filename: String = "", val size: Long = 0, val content: String? = null)

@Serializable
data class JobDto(
    val id: Long, val name: String = "", val status: String = "", val conclusion: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val steps: List<StepDto> = emptyList(),
)
@Serializable data class StepDto(val name: String = "", val status: String = "", val conclusion: String? = null, val number: Int = 0)
@Serializable data class JobsResponse(val total_count: Int = 0, val jobs: List<JobDto> = emptyList())

@Serializable
data class ArtifactDto(
    val id: Long, val name: String = "", val size: Long = 0,
    @SerialName("archive_download_url") val archiveDownloadUrl: String = "",
    val expired: Boolean = false,
)
@Serializable data class ArtifactsResponse(val total_count: Int = 0, val artifacts: List<ArtifactDto> = emptyList())

@Serializable
data class NotificationDto(
    val id: String,
    val unread: Boolean = false,
    val reason: String = "",
    val subject: NotificationSubject = NotificationSubject(),
    @SerialName("updated_at") val updatedAt: String = "",
    val repository: NotificationRepo? = null,
)
@Serializable data class NotificationSubject(val title: String = "", val type: String = "", val url: String? = null)
@Serializable data class NotificationRepo(@SerialName("full_name") val fullName: String = "")

@Serializable
data class SearchRepositoriesResponse(val total_count: Int = 0, val items: List<com.gitofy.app.model.Repository> = emptyList())
@Serializable
data class SearchCodeResponse(val total_count: Int = 0, val items: List<CodeItem> = emptyList())
@Serializable data class CodeItem(val name: String = "", val path: String = "", @SerialName("html_url") val htmlUrl: String = "", val repository: CodeRepo = CodeRepo())
@Serializable data class CodeRepo(@SerialName("full_name") val fullName: String = "")

// --- Write request bodies ---------------------------------------------------

@Serializable data class CreateRepoRequest(
    val name: String, val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = true,
    @SerialName("auto_init") val autoInit: Boolean = true,
)
@Serializable data class UpdateRepoRequest(
    val name: String? = null, val description: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
    val archived: Boolean? = null, @SerialName("default_branch") val defaultBranch: String? = null,
)
@Serializable data class CreateBranchRequest(val ref: String, val sha: String)
@Serializable data class RenameBranchRequest(@SerialName("new_name") val newName: String)
@Serializable data class CreateIssueRequest(val title: String, val body: String? = null)
@Serializable data class UpdateIssueRequest(val state: String)
@Serializable data class CommentRequest(val body: String)
@Serializable data class CreateGistRequest(val description: String? = null, @SerialName("public") val isPublic: Boolean = false, val files: Map<String, GistFileContent>)
@Serializable data class GistFileContent(val content: String)
@Serializable data class DispatchRequest(val ref: String, val inputs: Map<String, String> = emptyMap())
@Serializable data class MergePullRequest(val commit_title: String? = null, val merge_method: String = "merge")
@Serializable data class CreateReleaseRequest(
    @SerialName("tag_name") val tagName: String, val name: String? = null,
    val body: String? = null, val draft: Boolean = false, val prerelease: Boolean = false,
    @SerialName("target_commitish") val targetCommitish: String? = null,
)
@Serializable data class UpdateFileRequest(val message: String, val content: String, val sha: String? = null, val branch: String? = null)
@Serializable data class UpdateUserRequest(val name: String? = null, val bio: String? = null, val company: String? = null, val location: String? = null, val blog: String? = null)
