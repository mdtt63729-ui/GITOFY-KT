package com.gitofy.native.auth

import com.gitofy.native.data.TokenStore
import com.gitofy.native.model.GitHubUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Sealed auth state (src/auth/sessionManager.ts). */
sealed interface AuthState {
    data object Unknown : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: GitHubUser, val token: String) : AuthState
    data object Expired : AuthState
}

@Singleton
class SessionManager @Inject constructor(private val tokenStore: TokenStore) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Unknown)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun token(): String? = tokenStore.token()

    fun onSignedIn(user: GitHubUser, token: String) {
        tokenStore.save(token)
        _state.value = AuthState.SignedIn(user, token)
    }

    fun onSignedOut() {
        tokenStore.clear()
        _state.value = AuthState.SignedOut
    }

    fun onExpired() {
        _state.value = AuthState.Expired
    }

    fun markSignedOutIfNoToken() {
        _state.value = if (tokenStore.token().isNullOrBlank()) AuthState.SignedOut else AuthState.Unknown
    }
}
