package com.gitofy.app.util

/** Human-readable size, matching the web app's formatter. */
fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble() / 1024
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}

/** Bytes/second → "2.3 MB/s". */
fun formatSpeed(bps: Long): String = if (bps <= 0) "—" else "${formatSize(bps)}/s"

/** Compact relative time from an ISO-8601 timestamp. */
fun relativeTime(iso: String, now: Long = System.currentTimeMillis()): String {
    val t = runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrNull() ?: return ""
    val diff = (now - t) / 1000
    return when {
        diff < 60 -> "${diff}s ago"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 2592000 -> "${diff / 86400}d ago"
        else -> "${diff / 2592000}mo ago"
    }
}
