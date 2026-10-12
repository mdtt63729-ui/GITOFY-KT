package com.gitofy.app.settings

/**
 * Direct port of the web app's GitofySettings (src/theme/tokens.ts → defaultSettings).
 * Persisted in DataStore Preferences — except the token, which lives encrypted in
 * [com.gitofy.app.data.TokenStore] and is never written to DataStore.
 */
data class GitofySettings(
    // Appearance
    val uiMode: String = "light",        // light | dark | system
    val themeMode: String = "light",
    val palette: String = "crimson",     // pink|emerald|indigo|violet|crimson|cyan
    val fontScale: Float = 1.0f,
    val fontFamily: String = "josefin",
    val cornerRadius: Int = 16,
    val uiDensity: String = "normal",    // compact | normal | comfortable

    // Navigation
    val autoHideNav: Boolean = true,
    val navBounce: Boolean = true,
    val fabPosition: String = "right",   // left | right
    val scrollThreshold: Int = 20,

    // Upload defaults
    val autoInitReadme: Boolean = true,
    val defaultBranch: String = "main",
    val defaultVisibility: String = "private",
    val stripRootFolder: Boolean = true,
    val autoTriggerWorkflows: Boolean = false,
    val deleteRemoteOnly: Boolean = false,
    val maxConcurrentUploads: Int = 4,

    // Behaviour
    val language: String = "en",         // en | bn
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val confirmDestructive: Boolean = true,

    // Live run viewer
    val liveRunsPolling: String = "balanced",
    val liveRunsLogFontSize: Int = 11,
    val liveRunsWrap: Boolean = false,
    val liveRunsTimestamps: Boolean = false,
    val liveRunsAutoFollow: Boolean = true,
    val liveRunsDebugLines: Boolean = false,
    val liveRunsCacheDays: Int = 7,
    val instantMode: Boolean = false,

    // Identity (token itself is NOT here)
    val githubClientId: String = "",
    val githubUsername: String = "",
    val avatarUrl: String = "",
    val isDemoMode: Boolean = false,

    // Security
    val appLockMode: String = "off",     // off | biometric | pin
    val lockedScreens: List<String> = emptyList(),
    val flagSecure: Boolean = false,
    val enableWebFlow: Boolean = false,
    val diagnosticsOptIn: Boolean = true,
)
