package com.gitofy.app.ui.navigation

/**
 * The 29 navigation destinations, ported 1:1 from the web app's AppScreen union.
 * Kept as plain route strings so deep links (gitofy://…) can target them later.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val PERMISSIONS = "permissions"
    const val LOGIN_DIAGNOSTICS = "login_diagnostics"
    const val APP_LOCK = "app_lock"
    const val HOME = "home"
    const val INBOX = "inbox"
    const val REPO_DASHBOARD = "repo_dashboard"
    const val ZIP_ANALYSIS = "zip_analysis"
    const val UPLOAD = "upload"
    const val UPLOAD_FLOW = "upload_flow"
    const val RESULT = "result"
    const val WORKFLOWS = "workflows"
    const val WORKFLOW_RUN_DETAIL = "workflow_run_detail"
    const val SETTINGS = "settings"
    const val GITHUB_SETTINGS = "github_settings"
    const val REPO_SETTINGS = "repo_settings"
    const val DELETE_REPO = "delete_repo"
    const val REPO_ACTION = "repo_action"
    const val REPO_FILES = "repo_files"
    const val REPO_COMMITS = "repo_commits"
    const val BRANCH_MANAGER = "branch_manager"
    const val GISTS = "gists"
    const val GLOBAL_SEARCH = "global_search"
    const val WORKFLOW_EDITOR = "workflow_editor"
    const val RELEASE_CREATOR = "release_creator"
    const val ISSUES = "issues"
    const val MULTI_UPLOAD = "multi_upload"
    const val ERROR_LOG = "error_log"

    // Screens that exist in the web app but are outside the AppScreen union:
    const val APP_STORE = "app_store"
    const val M3_GALLERY = "m3_gallery"
    const val MOTION_LAB = "motion_lab"
    const val SPLASH = "splash"
    const val RUN_LOG = "run_log"
    const val ISSUE_DETAIL = "issue_detail"

    /** All routes, in the same order as the web AppScreen union. */
    val ALL = listOf(
        ONBOARDING, LOGIN, PERMISSIONS, LOGIN_DIAGNOSTICS, APP_LOCK, HOME, INBOX,
        REPO_DASHBOARD, ZIP_ANALYSIS, UPLOAD, UPLOAD_FLOW, RESULT, WORKFLOWS,
        WORKFLOW_RUN_DETAIL, SETTINGS, GITHUB_SETTINGS, REPO_SETTINGS, DELETE_REPO,
        REPO_ACTION, REPO_FILES, REPO_COMMITS, BRANCH_MANAGER, GISTS, GLOBAL_SEARCH,
        WORKFLOW_EDITOR, RELEASE_CREATOR, ISSUES, MULTI_UPLOAD, ERROR_LOG,
    )
}
