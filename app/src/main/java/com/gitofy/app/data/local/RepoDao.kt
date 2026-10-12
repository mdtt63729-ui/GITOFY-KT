package com.gitofy.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RepoEntity>>

    @Upsert
    suspend fun upsertAll(repos: List<RepoEntity>)

    @Query("DELETE FROM repos")
    suspend fun clear()
}
