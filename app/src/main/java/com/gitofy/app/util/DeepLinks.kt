package com.gitofy.app.util

/** A parsed gitofy:// deep link. Ported from utils/deepLinks.ts. */
data class DeepLink(val route: String, val args: Map<String, String>)

object DeepLinks {
    /** Parses gitofy://callback (oauth) and gitofy://repo/<owner>/<name>[...]. */
    fun parse(url: String?): DeepLink? {
        if (url.isNullOrBlank()) return null
        val u = runCatching { java.net.URI(url) }.getOrNull() ?: return null
        if (u.scheme != "gitofy") return null
        val host = u.host ?: return null
        if (host == "callback") return DeepLink("oauth_callback", parseQuery(u.query))

        val parts = (u.path ?: "").trim('/').split('/').filter { it.isNotBlank() }
        return when (host) {
            "repo" -> {
                if (parts.size < 2) return null
                DeepLink("repo_dashboard", mapOf("owner" to parts[0], "repo" to parts[1]))
            }
            "upload" -> DeepLink("upload", mapOf("repo" to (parts.getOrNull(0) ?: "")))
            else -> null
        }
    }

    private fun parseQuery(q: String?): Map<String, String> =
        q?.split('&')?.mapNotNull {
            val kv = it.split('=', limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }?.toMap() ?: emptyMap()
}
