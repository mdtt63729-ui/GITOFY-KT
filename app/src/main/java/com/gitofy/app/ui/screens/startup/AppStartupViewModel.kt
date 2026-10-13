package com.gitofy.app.ui.screens.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.app.auth.AuthRepository
import com.gitofy.app.auth.AuthState
import com.gitofy.app.domain.RepoRepository
import com.gitofy.app.security.IntegrityCheck
import com.gitofy.app.security.IntegrityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** What the app is waiting on at launch. */
sealed interface Gate {
    data object Loading : Gate
    data class Tampered(val message: String) : Gate
    data object SignedOut : Gate
    data object Ready : Gate
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val integrity: IntegrityCheck,
    private val auth: AuthRepository,
    private val repo: RepoRepository,
) : ViewModel() {

    private val _gate = MutableStateFlow<Gate>(Gate.Loading)
    val gate: StateFlow<Gate> = _gate.asStateFlow()

    init { run() }

    fun run() {
        viewModelScope.launch {
            val report = integrity.verify()
            if (report.status == IntegrityStatus.TAMPERED) {
                _gate.value = Gate.Tampered(report.reason ?: "signature mismatch")
                return@launch
            }
            _gate.value = when (auth.restore()) {
                is AuthState.SignedIn -> {
                    // Warm the repo cache while the splash is on screen, so Home opens
                    // straight into content with no skeleton. Bounded so a slow network
                    // never holds the splash.
                    runCatching { withTimeoutOrNull(3500) { repo.refresh() } }
                    Gate.Ready
                }
                else -> Gate.SignedOut
            }
        }
    }
}
