package com.gitofy.app.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gitofy.app.ui.screens.browse.BranchManagerScreen
import com.gitofy.app.ui.screens.browse.DeleteRepoPickerScreen
import com.gitofy.app.ui.screens.browse.DeleteRepoScreen
import com.gitofy.app.ui.screens.browse.GistsScreen
import com.gitofy.app.ui.screens.browse.GlobalSearchScreen
import com.gitofy.app.ui.screens.browse.InboxScreen
import com.gitofy.app.ui.screens.browse.IssueDetailScreen
import com.gitofy.app.ui.screens.browse.IssuesScreen
import com.gitofy.app.ui.screens.browse.RunLogScreen
import com.gitofy.app.ui.screens.browse.WorkflowEditorScreen
import com.gitofy.app.ui.screens.browse.ReleaseCreatorScreen
import com.gitofy.app.ui.screens.browse.RepoActionScreen
import com.gitofy.app.ui.screens.browse.RepoCodeScreen
import com.gitofy.app.ui.screens.browse.RepoCommitsScreen
import com.gitofy.app.ui.screens.browse.RepoDashboardScreen
import com.gitofy.app.ui.screens.browse.RepoSettingsScreen
import com.gitofy.app.ui.screens.browse.WorkflowRunDetailScreen
import com.gitofy.app.ui.screens.browse.WorkflowRunsScreen
import com.gitofy.app.ui.screens.browse.WorkflowsScreen
import com.gitofy.app.ui.screens.dev.M3GalleryScreen
import com.gitofy.app.ui.screens.dev.MotionLabScreen
import com.gitofy.app.ui.screens.entry.AppLockScreen
import com.gitofy.app.ui.screens.entry.LoginDiagnosticsScreen
import com.gitofy.app.ui.screens.entry.OnboardingScreen
import com.gitofy.app.ui.screens.entry.PermissionsScreen
import com.gitofy.app.ui.screens.home.HomeScreen
import com.gitofy.app.ui.screens.login.LoginScreen
import com.gitofy.app.ui.screens.misc.AppStoreScreen
import com.gitofy.app.ui.screens.misc.ErrorLogScreen
import com.gitofy.app.ui.screens.misc.GitHubSettingsScreen
import com.gitofy.app.ui.screens.misc.ResultScreen
import com.gitofy.app.ui.screens.misc.SettingsScreen
import com.gitofy.app.ui.screens.misc.SplashScreen
import com.gitofy.app.ui.screens.upload.M3UploadFlowScreen
import com.gitofy.app.ui.screens.upload.MultiRepoUploadScreen
import com.gitofy.app.ui.screens.upload.ZipAnalysisScreen

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
    // The web app's page entrance (index.css · gitofyScreenIn): the incoming screen
    // rises 16px and scales from 0.986 while fading in; the outgoing screen is just
    // replaced. No horizontal slide, so a page change reads as the content animating
    // in — not the whole screen being shoved sideways.
    val screenIn = tween<Float>(280, easing = FastOutSlowInEasing)
    val screenInOffset = tween<IntOffset>(280, easing = FastOutSlowInEasing)
    val screenOut = tween<Float>(160, easing = FastOutSlowInEasing)
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            fadeIn(screenIn) + slideInVertically(screenInOffset) { (it * 0.012f).toInt() }
        },
        exitTransition = { fadeOut(screenOut) },
        popEnterTransition = {
            fadeIn(screenIn) + slideInVertically(screenInOffset) { (it * 0.012f).toInt() }
        },
        popExitTransition = { fadeOut(screenOut) },
    ) {
        val back: () -> Unit = { navController.popBackStack() }
        val go: (String) -> Unit = { navController.navigate(it) }

        composable(Routes.SPLASH) { SplashScreen() }
        composable(Routes.ONBOARDING) { OnboardingScreen(onNavigate = go) }
        composable(Routes.LOGIN) { LoginScreen(onNavigate = go) }
        composable(Routes.PERMISSIONS) { PermissionsScreen(back) }
        composable(Routes.LOGIN_DIAGNOSTICS) { LoginDiagnosticsScreen(back, clientId = com.gitofy.app.auth.AuthConfig.CLIENT_ID, signedIn = false) }
        composable(Routes.APP_LOCK) { AppLockScreen(onUnlock = back) }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenRepo = { o, r -> navController.navigate("${Routes.REPO_DASHBOARD}/$o/$r") },
                onNavigate = go,
            )
        }
        composable(Routes.INBOX) { InboxScreen(back, onNavigate = go) }
        composable(Routes.GISTS) { GistsScreen(back) }
        composable(Routes.GLOBAL_SEARCH) { GlobalSearchScreen(back) }
        composable(Routes.SETTINGS) { SettingsScreen(back, onNavigate = go) }
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
            RepoCodeScreen(o, r, e.arguments!!.getString("path") ?: "", back,
                onOpen = { path, isDir ->
                    if (isDir) navController.navigate("${Routes.REPO_FILES}/$o/$r?path=$path")
                    else navController.navigate("${Routes.WORKFLOW_EDITOR}/$o/$r?path=$path")
                })
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
            IssuesScreen(o, r, back, onOpenIssue = { n -> navController.navigate("${Routes.ISSUE_DETAIL}/$o/$r/$n") })
        }
        composable(
            "${Routes.WORKFLOWS}/{owner}/{repo}",
            arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }),
        ) { e ->
            val o = e.arguments!!.getString("owner")!!
            val r = e.arguments!!.getString("repo")!!
            WorkflowsScreen(
                o, r, back,
                onOpenWorkflow = { id, name ->
                    navController.navigate("${Routes.WORKFLOW_RUNS}/$o/$r/$id?name=${android.net.Uri.encode(name)}")
                },
                onOpenRun = { runId -> navController.navigate("${Routes.WORKFLOW_RUN_DETAIL}/$o/$r/$runId") },
            )
        }
        composable(
            "${Routes.WORKFLOW_RUNS}/{owner}/{repo}/{workflowId}?name={name}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("workflowId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { e ->
            val o = e.arguments!!.getString("owner")!!
            val r = e.arguments!!.getString("repo")!!
            WorkflowRunsScreen(
                o, r, e.arguments!!.getLong("workflowId"), e.arguments!!.getString("name") ?: "", back,
                onOpenRun = { runId -> navController.navigate("${Routes.WORKFLOW_RUN_DETAIL}/$o/$r/$runId") },
            )
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
            WorkflowRunDetailScreen(o, r, runId, back, onOpenJob = { jobId -> navController.navigate("${Routes.RUN_LOG}/$o/$r/$runId?jobId=$jobId") })
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
        composable(Routes.DELETE_PICK) {
            DeleteRepoPickerScreen(back) { o, r -> navController.navigate("${Routes.DELETE_REPO}/$o/$r") }
        }

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
            "${Routes.RUN_LOG}/{owner}/{repo}/{runId}?jobId={jobId}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType },
                navArgument("jobId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { e ->
            val jid = e.arguments!!.getLong("jobId")
            RunLogScreen(
                e.arguments!!.getString("owner")!!,
                e.arguments!!.getString("repo")!!,
                e.arguments!!.getLong("runId"),
                back,
                initialJobId = if (jid > 0) jid else null,
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
