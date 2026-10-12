package com.gitofy.app.auth

import com.gitofy.app.data.TokenStore
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.model.GitHubUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates sign-in (device flow / PAT), token validation and sign-out.
 * Ported from src/auth/authRepository.ts.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val deviceFlow: DeviceFlowClient,
    private val api: GitHubApiService,
    private val session: SessionManager,
    private val tokenStore: TokenStore,
) {
    /** Begin the device flow: returns the code the user types on github.com. */
    suspend fun startDeviceFlow(): DeviceCodeResponse = deviceFlow.start()

    /** Poll the device flow; emits Waiting/SlowDown/Approved/Failed. */
    fun pollDeviceFlow(device: DeviceCodeResponse): Flow<DeviceFlowEvent> = deviceFlow.poll(device)

    /** After approval: verify the token and fetch the profile. */
    suspend fun completeSignIn(token: String): GitHubUser {
        val user = withToken(token) { api.currentUser() }
        session.onSignedIn(user, token)
        return user
    }

    suspend fun signInWithPat(pat: String): GitHubUser = completeSignIn(pat.trim())

    /** Validate the stored token on launch; sets state accordingly. */
    suspend fun restore(): AuthState {
        val token = tokenStore.token()
        if (token.isNullOrBlank()) {
            session.markSignedOutIfNoToken()
            return session.state.value
        }
        return runCatching {
            val user = api.currentUser()
            session.onSignedIn(user, token)
            session.state.value
        }.getOrElse {
            session.onExpired()
            session.state.value
        }
    }

    fun signOut() = session.onSignedOut()

    private suspend fun <T> withToken(token: String, block: suspend () -> T): T {
        // Temporarily use the token even before it is persisted.
        tokenStore.save(token)
        return try { block() } catch (t: Throwable) { tokenStore.clear(); throw t }
    }
}
