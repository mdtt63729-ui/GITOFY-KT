package com.gitofy.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.TokenStore
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.domain.RepoRepository
import com.gitofy.app.model.Repository
import com.gitofy.app.settings.SettingsStore
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: RepoRepository,
    private val tokenStore: TokenStore,
    private val api: GitHubApiService,
    settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Repository>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Repository>>> = _state.asStateFlow()

    private val _hasToken = MutableStateFlow(!tokenStore.token().isNullOrBlank())
    val hasToken: StateFlow<Boolean> = _hasToken.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl: StateFlow<String> = _avatarUrl.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            settings.settings.collect { s ->
                if (s.githubUsername.isNotBlank()) _username.value = s.githubUsername
                if (s.avatarUrl.isNotBlank()) _avatarUrl.value = s.avatarUrl
            }
        }
        // Load the real account avatar (the web app shows the GitHub account icon).
        viewModelScope.launch {
            if (_avatarUrl.value.isBlank()) {
                runCatching { api.currentUser() }.onSuccess { me ->
                    _username.value = me.login
                    _avatarUrl.value = me.avatarUrl
                    runCatching { settings.setGithubIdentity(me.login, me.avatarUrl) }
                }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _hasToken.value = !tokenStore.token().isNullOrBlank()
        _state.value = UiState.Loading
        runCatching { repo.refresh() }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load repositories") }
    }

    fun create(name: String, description: String, isPrivate: Boolean) = viewModelScope.launch {
        runCatching { repo.create(name, description, isPrivate) }.onSuccess { refresh() }
    }
}
