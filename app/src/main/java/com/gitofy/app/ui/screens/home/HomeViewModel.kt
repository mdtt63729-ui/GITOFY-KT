package com.gitofy.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitofy.app.domain.RepoRepository
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: RepoRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Repository>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Repository>>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        runCatching { repo.refresh() }
            .onSuccess { _state.value = UiState.Content(it) }
            .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load repositories") }
    }

    fun create(name: String, description: String, isPrivate: Boolean) = viewModelScope.launch {
        runCatching { repo.create(name, description, isPrivate) }.onSuccess { refresh() }
    }
}
