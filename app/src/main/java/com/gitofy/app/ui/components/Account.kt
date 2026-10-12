package com.gitofy.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gitofy.app.data.remote.GitHubApiService
import com.gitofy.app.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the signed-in GitHub account (login + avatar). Reads the cached value from
 * DataStore and, if it is missing, fetches it once from `GET /user` and caches it so
 * every top bar shows the real account icon instead of a letter.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val api: GitHubApiService,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _login = MutableStateFlow("")
    val login: StateFlow<String> = _login.asStateFlow()
    private val _avatar = MutableStateFlow("")
    val avatar: StateFlow<String> = _avatar.asStateFlow()

    init {
        viewModelScope.launch {
            val s = runCatching { settings.settings.first() }.getOrNull()
            if (s != null) {
                _login.value = s.githubUsername
                _avatar.value = s.avatarUrl
                if (s.avatarUrl.isBlank()) fetch()
            } else {
                fetch()
            }
        }
    }

    private suspend fun fetch() {
        runCatching { api.currentUser() }.onSuccess { me ->
            _login.value = me.login
            _avatar.value = me.avatarUrl
            runCatching { settings.setGithubIdentity(me.login, me.avatarUrl) }
        }
    }
}

/** The signed-in GitHub account's avatar — drop into any top bar. */
@Composable
fun AccountAvatar(size: Dp = 34.dp, modifier: Modifier = Modifier) {
    val vm: AccountViewModel = hiltViewModel()
    val url by vm.avatar.collectAsStateWithLifecycle()
    val login by vm.login.collectAsStateWithLifecycle()
    Avatar(url.ifBlank { null }, login.ifBlank { "U" }, size = size, modifier = modifier)
}
