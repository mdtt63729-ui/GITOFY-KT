package com.gitofy.native.update

/** Direct port of the web app's parseVersion / compareVersions / isUpdateAvailable. */
object VersionCompare {
    fun parse(v: String): List<Int> =
        v.replace(Regex("^v", RegexOption.IGNORE_CASE), "")
            .split(Regex("[.\\-+]"))
            .map { it.toIntOrNull() ?: 0 }

    /** >0 if a is newer than b, <0 if older, 0 if equal. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a); val pb = parse(b)
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val d = (pa.getOrNull(i) ?: 0) - (pb.getOrNull(i) ?: 0)
            if (d != 0) return if (d > 0) 1 else -1
        }
        return 0
    }

    fun isUpdateAvailable(latest: String, current: String): Boolean =
        latest.isNotBlank() && current.isNotBlank() && compare(latest, current) > 0
}
