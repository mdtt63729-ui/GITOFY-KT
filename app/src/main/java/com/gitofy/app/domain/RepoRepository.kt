package com.gitofy.app.domain

import com.gitofy.app.data.local.RepoDao
import com.gitofy.app.data.local.toEntity
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.model.Repository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Repo list with a Room-backed offline cache (utils/offlineCache.ts). */
@Singleton
class RepoRepository @Inject constructor(
    private val api: GitHubApiService,
    private val dao: RepoDao,
) {
    fun cached(): Flow<List<com.gitofy.app.data.local.RepoEntity>> = dao.observeAll()

    suspend fun refresh(): List<Repository> {
        val repos = api.userRepos()
        dao.upsertAll(repos.map { it.toEntity() })
        return repos
    }

    suspend fun starred(): List<Repository> = api.starredRepos()

    suspend fun get(owner: String, repo: String): Repository = api.repo(owner, repo)

    suspend fun create(name: String, description: String?, isPrivate: Boolean): Repository =
        api.createRepo(com.gitofy.app.data.remote.dto.CreateRepoRequest(name, description, isPrivate))

    suspend fun delete(owner: String, repo: String) = api.deleteRepo(owner, repo)

    suspend fun setArchived(owner: String, repo: String, archived: Boolean) =
        api.updateRepo(owner, repo, com.gitofy.app.data.remote.dto.UpdateRepoRequest(archived = archived))

    suspend fun setVisibility(owner: String, repo: String, isPrivate: Boolean) =
        api.updateRepo(owner, repo, com.gitofy.app.data.remote.dto.UpdateRepoRequest(isPrivate = isPrivate))

    suspend fun star(owner: String, repo: String, on: Boolean) {
        if (on) api.star(owner, repo) else api.unstar(owner, repo)
    }
}
