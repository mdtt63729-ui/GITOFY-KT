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
        val autoHideNav = booleanPreferencesKey("autoHideNav")
        val githubClientId = stringPreferencesKey("githubClientId")
        val fontFamily = stringPreferencesKey("fontFamily")
        val fontWeight = stringPreferencesKey("fontWeight")
        val dynamicColor = booleanPreferencesKey("dynamicColor")
        val backgroundHex = stringPreferencesKey("backgroundHex")
        val uiDensity = stringPreferencesKey("uiDensity")
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
            autoHideNav = p[K.autoHideNav] ?: d.autoHideNav,
            githubClientId = p[K.githubClientId] ?: d.githubClientId,
            fontFamily = p[K.fontFamily] ?: d.fontFamily,
            fontWeight = p[K.fontWeight] ?: d.fontWeight,
            dynamicColor = p[K.dynamicColor] ?: d.dynamicColor,
            backgroundHex = p[K.backgroundHex] ?: d.backgroundHex,
            uiDensity = p[K.uiDensity] ?: d.uiDensity,
        )
    }

    suspend fun setPalette(v: String) = dataStore.edit { it[K.palette] = v }
    suspend fun setLanguage(v: String) = dataStore.edit { it[K.language] = v }
    suspend fun setUiMode(v: String) = dataStore.edit { it[K.uiMode] = v }
    suspend fun setReduceMotion(v: Boolean) = dataStore.edit { it[K.reduceMotion] = v }
    suspend fun setFlagSecure(v: Boolean) = dataStore.edit { it[K.flagSecure] = v }
    suspend fun setDefaultBranch(v: String) = dataStore.edit { it[K.defaultBranch] = v }
    suspend fun setHaptics(v: Boolean) = dataStore.edit { it[K.haptics] = v }
    suspend fun setAutoHideNav(v: Boolean) = dataStore.edit { it[K.autoHideNav] = v }
    suspend fun setDefaultVisibility(v: String) = dataStore.edit { it[K.defaultVisibility] = v }
    suspend fun setStripRootFolder(v: Boolean) = dataStore.edit { it[K.stripRootFolder] = v }
    suspend fun setAppLockMode(v: String) = dataStore.edit { it[K.appLockMode] = v }
    suspend fun setFontFamily(v: String) = dataStore.edit { it[K.fontFamily] = v }
    suspend fun setFontWeight(v: String) = dataStore.edit { it[K.fontWeight] = v }
    suspend fun setDynamicColor(v: Boolean) = dataStore.edit { it[K.dynamicColor] = v }
    suspend fun setBackgroundHex(v: String) = dataStore.edit { it[K.backgroundHex] = v }
    suspend fun setFontScale(v: Float) = dataStore.edit { it[K.fontScale] = v }
    suspend fun setCornerRadius(v: Int) = dataStore.edit { it[K.cornerRadius] = v }
    suspend fun setUiDensity(v: String) = dataStore.edit { it[K.uiDensity] = v }

    /** Cache the signed-in GitHub account so every top bar can show its avatar. */
    suspend fun setGithubIdentity(login: String, avatar: String) = dataStore.edit {
        it[K.githubUsername] = login
        it[K.avatarUrl] = avatar
    }
}
