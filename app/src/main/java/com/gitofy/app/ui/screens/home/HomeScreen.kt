package com.gitofy.app.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.app.model.Repository
import com.gitofy.app.ui.components.EmptyState
import com.gitofy.app.ui.components.ErrorState
import com.gitofy.app.ui.components.GitofyTopBar
import com.gitofy.app.ui.components.RepoCard
import com.gitofy.app.ui.components.SkeletonList
import com.gitofy.app.ui.navigation.Routes
import com.gitofy.app.ui.screens.sheets.CreateRepoSheet
import com.gitofy.app.ui.state.UiState

@Composable
fun HomeScreen(
    onOpenRepo: (String, String) -> Unit,
    onNavigate: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GitofyTopBar("Gitofy", actions = {
                IconButton(onClick = { onNavigate(Routes.APP_STORE) }) { Icon(Icons.Filled.SystemUpdate, "Updates") }
                IconButton(onClick = { onNavigate(Routes.INBOX) }) { Icon(Icons.Filled.MailOutline, "Inbox") }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, "New repo") }
        },
    ) { padding ->
        when (val st = s) {
            is UiState.Loading -> SkeletonList(modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Error -> ErrorState(st.message, onRetry = { vm.refresh() }, modifier = Modifier.fillMaxSize().padding(padding))
            is UiState.Content ->
                if (st.data.isEmpty()) {
                    EmptyState(
                        title = "No repositories yet",
                        body = "Create your first repository to get started.",
                        icon = Icons.Outlined.FolderOpen,
                        actionLabel = "New repository",
                        onAction = { showCreate = true },
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(st.data, key = { it.id }) { r: Repository ->
                            RepoCard(r) { onOpenRepo(r.ownerLogin, r.name) }
                        }
                    }
                }
        }
    }

    if (showCreate) {
        CreateRepoSheet(
            onDismiss = { showCreate = false },
            onCreate = { n, d, p -> vm.create(n, d, p); showCreate = false },
        )
    }
}
