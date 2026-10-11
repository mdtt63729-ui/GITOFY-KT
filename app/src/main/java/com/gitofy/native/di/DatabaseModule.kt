package com.gitofy.native.di

import android.content.Context
import androidx.room.Room
import com.gitofy.native.data.local.GitofyDatabase
import com.gitofy.native.data.local.RepoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): GitofyDatabase =
        Room.databaseBuilder(context, GitofyDatabase::class.java, "gitofy.db").build()

    @Provides
    fun repoDao(db: GitofyDatabase): RepoDao = db.repoDao()
}
