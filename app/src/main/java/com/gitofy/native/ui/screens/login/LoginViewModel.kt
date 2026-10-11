package com.gitofy.native.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.native.auth.AuthConfig
import com.gitofy.native.auth.AuthRepository
import com.gitofy.native.auth.DeviceCodeResponse
import com.gitofy.native.auth.DeviceFlowEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUi {
    data object Idle : LoginUi
    data class Waiting(val code: DeviceCodeResponse, val secondsLeft: Int, val note: String) : LoginUi
    data object Success : LoginUi
    data class Failed(val message: String) : LoginUi
}

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow<LoginUi>(LoginUi.Idle)
    val ui: StateFlow<LoginUi> = _ui.asStateFlow()

    val clientId: String = AuthConfig.CLIENT_ID
    val verificationUri: String = AuthConfig.DEVICE_VERIFICATION
    private var pollJob: Job? = null

    fun startDeviceFlow() {
        viewModelScope.launch {
            runCatching { auth.startDeviceFlow() }
                .onSuccess { code ->
                    _ui.value = LoginUi.Waiting(code, code.expiresIn, "Waiting for approval…")
                    poll(code)
                }
                .onFailure { _ui.value = LoginUi.Failed(it.message ?: "Could not start login") }
        }
    }

    private fun poll(code: DeviceCodeResponse) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            auth.pollDeviceFlow(code).collect { ev ->
                when (ev) {
                    is DeviceFlowEvent.Waiting -> {
                        val left = (_ui.value as? LoginUi.Waiting)?.secondsLeft ?: code.expiresIn
                        _ui.value = LoginUi.Waiting(code, (left - ev.interval).coerceAtLeast(0), "Waiting for approval…")
                    }
                    DeviceFlowEvent.SlowDown -> _ui.value = LoginUi.Waiting(code, code.expiresIn, "Slowing down…")
                    is DeviceFlowEvent.Approved ->
                        runCatching { auth.completeSignIn(ev.token) }
                            .onSuccess { _ui.value = LoginUi.Success }
                            .onFailure { _ui.value = LoginUi.Failed(it.message ?: "Sign-in failed") }
                    is DeviceFlowEvent.Failed -> _ui.value = LoginUi.Failed(ev.error)
                }
            }
        }
    }

    fun signInWithPat(pat: String) {
        viewModelScope.launch {
            runCatching { auth.signInWithPat(pat) }
                .onSuccess { _ui.value = LoginUi.Success }
                .onFailure { _ui.value = LoginUi.Failed(it.message ?: "Invalid token") }
        }
    }
}
