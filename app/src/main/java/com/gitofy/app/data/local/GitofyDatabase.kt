package com.gitofy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RepoEntity::class], version = 1, exportSchema = false)
abstract class GitofyDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
}
