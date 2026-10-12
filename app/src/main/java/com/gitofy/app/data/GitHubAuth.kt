package com.gitofy.app.data

/**
 * Minimal GitHub identity: who we are and what token we push with.
 * The full device-flow login lives in the app's auth layer; this is the seam the
 * git engine needs.
 */
data class GitHubIdentity(
    val login: String,
    val token: String,
) {
    /** `https://github.com/<login>/<repo>.git` */
    fun remoteUrl(repo: String): String = "https://github.com/$login/$repo.git"
}

/** Builds the authenticated remote URL for a push/pull. */
fun remoteFor(owner: String, repo: String): String = "https://github.com/$owner/$repo.git"
