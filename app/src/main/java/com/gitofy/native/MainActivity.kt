package com.gitofy.native

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gitofy.native.security.AppLockManager
import com.gitofy.native.security.DangerSound
import com.gitofy.native.settings.GitofySettings
import com.gitofy.native.settings.SettingsStore
import com.gitofy.native.ui.navigation.GitofyNavHost
import com.gitofy.native.ui.navigation.Routes
import com.gitofy.native.ui.screens.entry.AppLockScreen
import com.gitofy.native.ui.screens.misc.SplashScreen
import com.gitofy.native.ui.screens.security.SecurityOverlay
import com.gitofy.native.ui.screens.startup.AppStartupViewModel
import com.gitofy.native.ui.screens.startup.Gate
import com.gitofy.native.ui.theme.GitofyTheme
import com.gitofy.native.ui.theme.Palette
import com.gitofy.native.util.DeepLinks
import com.gitofy.native.util.Notifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var appLock: AppLockManager
    @Inject lateinit var dangerSound: DangerSound
    @Inject lateinit var notifications: Notifications

    private val startup: AppStartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notifications.ensureChannels()

        val deepLinkRoute = DeepLinks.parse(intent?.data?.toString())?.let { dl ->
            when (dl.route) {
                "repo_dashboard" -> "${Routes.REPO_DASHBOARD}/${dl.args["owner"]}/${dl.args["repo"]}"
                else -> null
            }
        }

        setContent {
            val s by settingsStore.settings.collectAsStateWithLifecycle(initialValue = GitofySettings())
            val gate by startup.gate.collectAsStateWithLifecycle()

            val dark = when (s.uiMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Screen-capture protection.
            LaunchedEffect(s.flagSecure) {
                if (s.flagSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            GitofyTheme(darkTheme = dark, palette = Palette.fromId(s.palette)) {
                when (val g = gate) {
                    Gate.Loading -> SplashScreen()

                    is Gate.Tampered -> {
                        // Play the klaxon once, then block.
                        LaunchedEffect(g.message) { dangerSound.play() }
                        SecurityOverlay("The app signature does not match the official build (${g.message}).")
                    }

                    Gate.SignedOut -> LockedThen(
                        locked = false,
                        onUnlock = {},
                    ) { GitofyNavHost(startRoute = Routes.LOGIN) }

                    Gate.Ready -> {
                        var locked by remember { mutableStateOf(s.appLockMode != "off" && appLock.canAuthenticate()) }
                        LockedThen(
                            locked = locked,
                            onUnlock = {
                                appLock.prompt(
                                    activity = this@MainActivity,
                                    onSuccess = { locked = false },
                                    onFailure = { /* keep locked; user can retry */ },
                                )
                            },
                        ) {
                            GitofyNavHost(startRoute = deepLinkRoute ?: Routes.HOME)
                        }
                    }
                }
            }
        }
    }
}

/** Shows the lock screen over the app until unlocked. */
@Composable
private fun LockedThen(locked: Boolean, onUnlock: () -> Unit, content: @Composable () -> Unit) {
    if (locked) {
        AppLockScreen(onUnlock = onUnlock)
        LaunchedEffect(Unit) { onUnlock() }   // auto-prompt on first show
    } else {
        content()
    }
}
