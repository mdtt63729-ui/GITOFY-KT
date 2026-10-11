package com.gitofy.native.data.remote

import com.gitofy.native.data.remote.dto.*
import com.gitofy.native.model.GitHubRelease
import com.gitofy.native.model.GitHubUser
import com.gitofy.native.model.Repository
import com.gitofy.native.model.WorkflowItem
import com.gitofy.native.model.WorkflowRun
import okhttp3.ResponseBody
import retrofit2.http.*

/** Full GitHub REST v3 surface — a 1:1 port of src/git/githubApi.ts. */
interface GitHubApiService {

    // --- User ---------------------------------------------------------------
    @GET("user") suspend fun currentUser(): GitHubUser
    @PATCH("user") suspend fun updateUser(@Body body: UpdateUserRequest): GitHubUser

    @GET("user/repos")
    suspend fun userRepos(
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member",
    ): List<Repository>

    @GET("user/starred")
    suspend fun starredRepos(@Query("per_page") perPage: Int = 100): List<Repository>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun star(@Path("owner") owner: String, @Path("repo") repo: String)

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstar(@Path("owner") owner: String, @Path("repo") repo: String)

    @GET("notifications")
    suspend fun notifications(@Query("all") all: Boolean = true, @Query("per_page") perPage: Int = 30): List<NotificationDto>

    // --- Repos --------------------------------------------------------------
    @GET("repos/{owner}/{repo}")
    suspend fun repo(@Path("owner") owner: String, @Path("repo") repo: String): Repository

    @POST("user/repos") suspend fun createRepo(@Body body: CreateRepoRequest): Repository

    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepo(@Path("owner") owner: String, @Path("repo") repo: String, @Body body: UpdateRepoRequest): Repository

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepo(@Path("owner") owner: String, @Path("repo") repo: String)

    @GET("repos/{owner}/{repo}/languages")
    suspend fun languages(@Path("owner") owner: String, @Path("repo") repo: String): Map<String, Long>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun contents(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("path", encoded = true) path: String = "",
        @Query("ref") ref: String? = null,
    ): List<ContentDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun file(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null,
    ): ContentDto

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFile(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("path", encoded = true) path: String, @Body body: UpdateFileRequest,
    ): ContentDto

    @GET("repos/{owner}/{repo}/git/trees/{sha}")
    suspend fun tree(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("sha") sha: String, @Query("recursive") recursive: Int = 1,
    ): TreeResponse

    @GET("repos/{owner}/{repo}/commits")
    suspend fun commits(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Query("sha") sha: String? = null, @Query("per_page") perPage: Int = 50,
    ): List<CommitDto>

    @POST("repos/{owner}/{repo}/commits/{sha}/comments")
    suspend fun commitComment(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("sha") sha: String, @Body body: CommentRequest,
    ): IssueCommentDto

    @GET("repos/{owner}/{repo}/branches")
    suspend fun branches(@Path("owner") owner: String, @Path("repo") repo: String, @Query("per_page") perPage: Int = 100): List<BranchDto>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createBranch(@Path("owner") owner: String, @Path("repo") repo: String, @Body body: CreateBranchRequest): BranchDto

    @DELETE("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun deleteBranch(@Path("owner") owner: String, @Path("repo") repo: String, @Path("branch") branch: String)

    @POST("repos/{owner}/{repo}/branches/{branch}/rename")
    suspend fun renameBranch(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("branch") branch: String, @Body body: RenameBranchRequest,
    ): BranchDto

    // --- Issues & PRs -------------------------------------------------------
    @GET("repos/{owner}/{repo}/issues")
    suspend fun issues(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Query("state") state: String = "open", @Query("per_page") perPage: Int = 50,
        @Query("sort") sort: String = "updated",
    ): List<IssueDto>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun pulls(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Query("state") state: String = "open", @Query("per_page") perPage: Int = 50,
    ): List<IssueDto>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(@Path("owner") owner: String, @Path("repo") repo: String, @Body body: CreateIssueRequest): IssueDto

    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun setIssueState(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("number") number: Int, @Body body: UpdateIssueRequest,
    ): IssueDto

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun issueComments(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("number") number: Int, @Query("per_page") perPage: Int = 100,
    ): List<IssueCommentDto>

    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun addIssueComment(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("number") number: Int, @Body body: CommentRequest,
    ): IssueCommentDto

    @PUT("repos/{owner}/{repo}/pulls/{number}/merge")
    suspend fun mergePull(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("number") number: Int, @Body body: MergePullRequest,
    ): ResponseBody

    // --- Actions ------------------------------------------------------------
    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun workflows(@Path("owner") owner: String, @Path("repo") repo: String): WorkflowsResponse

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun runs(@Path("owner") owner: String, @Path("repo") repo: String, @Query("per_page") perPage: Int = 30): RunsResponse

    @GET("repos/{owner}/{repo}/actions/workflows/{id}/runs")
    suspend fun workflowRuns(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("id") id: Long, @Query("per_page") perPage: Int = 20,
    ): RunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{id}")
    suspend fun run(@Path("owner") owner: String, @Path("repo") repo: String, @Path("id") id: Long): WorkflowRun

    @GET("repos/{owner}/{repo}/actions/runs/{id}/jobs")
    suspend fun jobs(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("id") id: Long, @Query("per_page") perPage: Int = 100,
    ): JobsResponse

    @POST("repos/{owner}/{repo}/actions/runs/{id}/cancel")
    suspend fun cancelRun(@Path("owner") owner: String, @Path("repo") repo: String, @Path("id") id: Long)

    @POST("repos/{owner}/{repo}/actions/runs/{id}/rerun")
    suspend fun rerun(@Path("owner") owner: String, @Path("repo") repo: String, @Path("id") id: Long)

    @Streaming
    @GET("repos/{owner}/{repo}/actions/jobs/{jobId}/logs")
    suspend fun jobLogs(
        @Path("owner") owner: String, @Path("repo") repo: String, @Path("jobId") jobId: Long,
    ): ResponseBody

    @GET("repos/{owner}/{repo}/actions/runs/{id}/artifacts")
    suspend fun artifacts(@Path("owner") owner: String, @Path("repo") repo: String, @Path("id") id: Long): ArtifactsResponse

    @POST("repos/{owner}/{repo}/actions/workflows/{id}/dispatches")
    suspend fun dispatch(
        @Path("owner") owner: String, @Path("repo") repo: String,
        @Path("id") id: String, @Body body: DispatchRequest,
    ): ResponseBody

    // --- Releases -----------------------------------------------------------
    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(@Path("owner") owner: String, @Path("repo") repo: String): List<GitHubRelease>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repo: String): GitHubRelease

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(@Path("owner") owner: String, @Path("repo") repo: String, @Body body: CreateReleaseRequest): GitHubRelease

    // --- Gists --------------------------------------------------------------
    @GET("gists") suspend fun gists(@Query("per_page") perPage: Int = 50): List<GistDto>
    @POST("gists") suspend fun createGist(@Body body: CreateGistRequest): GistDto
    @DELETE("gists/{id}") suspend fun deleteGist(@Path("id") id: String)

    // --- Search -------------------------------------------------------------
    @GET("search/repositories")
    suspend fun searchRepositories(@Query("q") q: String, @Query("per_page") perPage: Int = 20): SearchRepositoriesResponse

    @GET("search/code")
    suspend fun searchCode(@Query("q") q: String, @Query("per_page") perPage: Int = 20): SearchCodeResponse
}

@kotlinx.serialization.Serializable
data class WorkflowsResponse(val total_count: Int = 0, val workflows: List<WorkflowItem> = emptyList())

@kotlinx.serialization.Serializable
data class RunsResponse(val total_count: Int = 0, val workflow_runs: List<WorkflowRun> = emptyList())
