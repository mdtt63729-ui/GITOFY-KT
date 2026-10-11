package com.gitofy.native.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gitofy.native.ui.screens.browse.BranchManagerScreen
import com.gitofy.native.ui.screens.browse.DeleteRepoScreen
import com.gitofy.native.ui.screens.browse.GistsScreen
import com.gitofy.native.ui.screens.browse.GlobalSearchScreen
import com.gitofy.native.ui.screens.browse.InboxScreen
import com.gitofy.native.ui.screens.browse.IssueDetailScreen
import com.gitofy.native.ui.screens.browse.IssuesScreen
import com.gitofy.native.ui.screens.browse.RunLogScreen
import com.gitofy.native.ui.screens.browse.WorkflowEditorScreen
import com.gitofy.native.ui.screens.browse.ReleaseCreatorScreen
import com.gitofy.native.ui.screens.browse.RepoActionScreen
import com.gitofy.native.ui.screens.browse.RepoCodeScreen
import com.gitofy.native.ui.screens.browse.RepoCommitsScreen
import com.gitofy.native.ui.screens.browse.RepoDashboardScreen
import com.gitofy.native.ui.screens.browse.RepoSettingsScreen
import com.gitofy.native.ui.screens.browse.WorkflowRunDetailScreen
import com.gitofy.native.ui.screens.browse.WorkflowsScreen
import com.gitofy.native.ui.screens.dev.M3GalleryScreen
import com.gitofy.native.ui.screens.dev.MotionLabScreen
import com.gitofy.native.ui.screens.entry.AppLockScreen
import com.gitofy.native.ui.screens.entry.LoginDiagnosticsScreen
import com.gitofy.native.ui.screens.entry.OnboardingScreen
import com.gitofy.native.ui.screens.entry.PermissionsScreen
import com.gitofy.native.ui.screens.home.HomeScreen
import com.gitofy.native.ui.screens.login.LoginScreen
import com.gitofy.native.ui.screens.misc.AppStoreScreen
import com.gitofy.native.ui.screens.misc.ErrorLogScreen
import com.gitofy.native.ui.screens.misc.GitHubSettingsScreen
import com.gitofy.native.ui.screens.misc.ResultScreen
import com.gitofy.native.ui.screens.misc.SettingsScreen
import com.gitofy.native.ui.screens.misc.SplashScreen
import com.gitofy.native.ui.screens.upload.M3UploadFlowScreen
import com.gitofy.native.ui.screens.upload.MultiRepoUploadScreen
import com.gitofy.native.ui.screens.upload.ZipAnalysisScreen

/**
 * One NavHost for every destination — the 29 AppScreen routes plus the app-store,
 * gallery, motion-lab and splash screens. Each screen is the real one from its
 * phase; nothing is a placeholder anymore.
 */
@Composable
fun GitofyNavHost(
    startRoute: String = Routes.HOME,
    navController: NavHostController = rememberNavController(),
) {
    val dur = 300
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(dur)) + fadeIn(tween(dur))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(dur)) + fadeOut(tween(dur))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(dur)) + fadeIn(tween(dur))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(dur)) + fadeOut(tween(dur))
        },
    ) {
        val back: () -> Unit = { navController.popBackStack() }
        val go: (String) -> Unit = { navController.navigate(it) }

        composable(Routes.SPLASH) { SplashScreen() }
        composable(Routes.ONBOARDING) { OnboardingScreen(onNavigate = go) }
        composable(Routes.LOGIN) { LoginScreen(onNavigate = go) }
        composable(Routes.PERMISSIONS) { PermissionsScreen(back) }
        composable(Routes.LOGIN_DIAGNOSTICS) { LoginDiagnosticsScreen(back, clientId = com.gitofy.native.auth.AuthConfig.CLIENT_ID, signedIn = false) }
        composable(Routes.APP_LOCK) { AppLockScreen(onUnlock = back) }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenRepo = { o, r -> navController.navigate("${Routes.REPO_DASHBOARD}/$o/$r") },
                onNavigate = go,
            )
        }
        composable(Routes.INBOX) { InboxScreen(back) }
        composable(Routes.GISTS) { GistsScreen(back) }
        composable(Routes.GLOBAL_SEARCH) { GlobalSearchScreen(back) }
        composable(Routes.SETTINGS) { SettingsScreen(back) }
        composable(Routes.GITHUB_SETTINGS) { GitHubSettingsScreen(back, onSignedOut = { navController.navigate(Routes.LOGIN) }) }
        composable(Routes.ERROR_LOG) { ErrorLogScreen(back) }
        composable(Routes.APP_STORE) { AppStoreScreen(back) }
        composable(Routes.M3_GALLERY) { M3GalleryScreen(back) }
        composable(Routes.MOTION_LAB) { MotionLabScreen(back) }
        composable(Routes.MULTI_UPLOAD) { MultiRepoUploadScreen(back) }
        composable(Routes.UPLOAD) { M3UploadFlowScreen(back, go) }
        composable(Routes.UPLOAD_FLOW) { M3UploadFlowScreen(back, go) }
        composable(Routes.ZIP_ANALYSIS) { ZipAnalysisScreen(back) }
        composable(Routes.RESULT) { ResultScreen(back, success = true, sha = "", files = 0, bytes = 0, speed = "") }

        composable(
            "${Routes.REPO_DASHBOARD}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            RepoDashboardScreen(e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back, go)
        }
        composable(
            "${Routes.REPO_FILES}/{owner}/{repo}?path={path}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { e ->
            val o = e.arguments!!.getString("owner")!!
            val r = e.arguments!!.getString("repo")!!
            RepoCodeScreen(o, r, e.arguments!!.getString("path") ?: "", back) { path, isDir ->
                if (isDir) navController.navigate("${Routes.REPO_FILES}/$o/$r?path=$path")
                else navController.navigate("${Routes.WORKFLOW_EDITOR}/$o/$r?path=$path")
            }
        }
        composable(
            "${Routes.REPO_COMMITS}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            RepoCommitsScreen(e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back)
        }
        composable(
            "${Routes.BRANCH_MANAGER}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            BranchManagerScreen(e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back)
        }
        composable(
            "${Routes.ISSUES}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            val o = e.arguments!!.getString("owner")!!
            val r = e.arguments!!.getString("repo")!!
            IssuesScreen(o, r, back) { n -> navController.navigate("${Routes.ISSUE_DETAIL}/$o/$r/$n") }
        }
        composable(
            "${Routes.WORKFLOWS}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            WorkflowsScreen(e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back)
        }
        composable(
            "${Routes.WORKFLOW_RUN_DETAIL}/{owner}/{repo}/{runId}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType },
            ),
        ) { e ->
            val o = e.arguments!!.getString("owner")!!
            val r = e.arguments!!.getString("repo")!!
            val runId = e.arguments!!.getLong("runId")
            WorkflowRunDetailScreen(o, r, runId, back) { navController.navigate("${Routes.RUN_LOG}/$o/$r/$runId") }
        }
        composable(
            "${Routes.RELEASE_CREATOR}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            ReleaseCreatorScreen(e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back)
        }
        composable(
            "${Routes.REPO_SETTINGS}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            RepoSettingsScreen(
                e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back,
                onDeleted = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
            )
        }
        composable(
            "${Routes.DELETE_REPO}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            DeleteRepoScreen(
                e.arguments!!.getString("owner")!!, e.arguments!!.getString("repo")!!, back,
                onConfirm = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
            )
        }
        composable(Routes.REPO_ACTION) { RepoActionScreen("Action", "Done.", back) }

        composable(
            "${Routes.WORKFLOW_EDITOR}/{owner}/{repo}?path={path}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { e ->
            WorkflowEditorScreen(
                e.arguments!!.getString("owner")!!,
                e.arguments!!.getString("repo")!!,
                e.arguments!!.getString("path") ?: "",
                back,
            )
        }

        composable(
            "${Routes.RUN_LOG}/{owner}/{repo}/{runId}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType },
            ),
        ) { e ->
            RunLogScreen(
                e.arguments!!.getString("owner")!!,
                e.arguments!!.getString("repo")!!,
                e.arguments!!.getLong("runId"),
                back,
            )
        }

        composable(
            "${Routes.ISSUE_DETAIL}/{owner}/{repo}/{number}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("number") { type = NavType.IntType },
            ),
        ) { e ->
            IssueDetailScreen(
                e.arguments!!.getString("owner")!!,
                e.arguments!!.getString("repo")!!,
                e.arguments!!.getInt("number"),
                back,
            )
        }
    }
}
