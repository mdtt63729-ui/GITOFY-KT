package com.gitofy.native.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted-at-rest store for the GitHub token (OAuth device-flow access token
 * or a PAT). Backed by EncryptedSharedPreferences with an AES-256-GCM master key
 * held in the hardware-backed Keystore.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "gitofy_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)
    fun save(token: String) { prefs.edit().putString(KEY_TOKEN, token.trim()).apply() }
    fun clear() { prefs.edit().remove(KEY_TOKEN).apply() }

    private companion object { const val KEY_TOKEN = "github_token" }
}
