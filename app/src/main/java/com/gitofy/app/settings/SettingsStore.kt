package com.gitofy.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private object K {
        val uiMode = stringPreferencesKey("uiMode")
        val palette = stringPreferencesKey("palette")
        val language = stringPreferencesKey("language")
        val fontScale = floatPreferencesKey("fontScale")
        val cornerRadius = intPreferencesKey("cornerRadius")
        val reduceMotion = booleanPreferencesKey("reduceMotion")
        val haptics = booleanPreferencesKey("haptics")
        val defaultBranch = stringPreferencesKey("defaultBranch")
        val defaultVisibility = stringPreferencesKey("defaultVisibility")
        val stripRootFolder = booleanPreferencesKey("stripRootFolder")
        val githubUsername = stringPreferencesKey("githubUsername")
        val avatarUrl = stringPreferencesKey("avatarUrl")
        val appLockMode = stringPreferencesKey("appLockMode")
        val flagSecure = booleanPreferencesKey("flagSecure")
    }

    val settings: Flow<GitofySettings> = dataStore.data.map { p ->
        val d = GitofySettings()
        d.copy(
            uiMode = p[K.uiMode] ?: d.uiMode,
            palette = p[K.palette] ?: d.palette,
            language = p[K.language] ?: d.language,
            fontScale = p[K.fontScale] ?: d.fontScale,
            cornerRadius = p[K.cornerRadius] ?: d.cornerRadius,
            reduceMotion = p[K.reduceMotion] ?: d.reduceMotion,
            haptics = p[K.haptics] ?: d.haptics,
            defaultBranch = p[K.defaultBranch] ?: d.defaultBranch,
            defaultVisibility = p[K.defaultVisibility] ?: d.defaultVisibility,
            stripRootFolder = p[K.stripRootFolder] ?: d.stripRootFolder,
            githubUsername = p[K.githubUsername] ?: d.githubUsername,
            avatarUrl = p[K.avatarUrl] ?: d.avatarUrl,
            appLockMode = p[K.appLockMode] ?: d.appLockMode,
            flagSecure = p[K.flagSecure] ?: d.flagSecure,
        )
    }

    suspend fun setPalette(v: String) = dataStore.edit { it[K.palette] = v }
    suspend fun setLanguage(v: String) = dataStore.edit { it[K.language] = v }
    suspend fun setUiMode(v: String) = dataStore.edit { it[K.uiMode] = v }
    suspend fun setReduceMotion(v: Boolean) = dataStore.edit { it[K.reduceMotion] = v }
    suspend fun setFlagSecure(v: Boolean) = dataStore.edit { it[K.flagSecure] = v }
    suspend fun setDefaultBranch(v: String) = dataStore.edit { it[K.defaultBranch] = v }
}
