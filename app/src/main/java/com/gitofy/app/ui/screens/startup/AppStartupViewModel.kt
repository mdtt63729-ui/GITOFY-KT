package com.gitofy.app.ui.screens.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.app.auth.AuthRepository
import com.gitofy.app.auth.AuthState
import com.gitofy.app.security.IntegrityCheck
import com.gitofy.app.security.IntegrityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                is AuthState.SignedIn -> Gate.Ready
                else -> Gate.SignedOut
            }
        }
    }
}
