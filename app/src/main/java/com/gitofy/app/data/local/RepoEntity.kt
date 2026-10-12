package com.gitofy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gitofy.app.model.Repository

/** Offline cache row for a repository (the web app's offlineCache). */
@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val isPrivate: Boolean,
    val archived: Boolean,
    val language: String?,
    val stargazers: Int,
    val defaultBranch: String,
    val ownerLogin: String,
    val ownerAvatar: String,
    val updatedAt: String?,
    val cachedAt: Long,
)

fun Repository.toEntity(now: Long = System.currentTimeMillis()) = RepoEntity(
    id = id, name = name, fullName = fullName, description = description,
    isPrivate = isPrivate, archived = archived, language = language,
    stargazers = stargazersCount, defaultBranch = defaultBranch,
    ownerLogin = ownerLogin, ownerAvatar = owner?.avatarUrl ?: "",
    updatedAt = updatedAt, cachedAt = now,
)
