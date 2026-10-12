package com.gitofy.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LogEntry(val time: Long, val level: String, val tag: String, val message: String) {
    fun formatted(): String {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(time))
        return "$ts [$level] $tag — $message"
    }
}

/** In-app diagnostics log (utils/errorLog.ts). Keeps the last 500 entries. */
@Singleton
class ErrorLog @Inject constructor() {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun info(tag: String, message: String) = add("INFO", tag, message)
    fun warn(tag: String, message: String) = add("WARN", tag, message)
    fun error(tag: String, message: String) = add("ERROR", tag, message)

    fun clear() { _entries.value = emptyList() }

    private fun add(level: String, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        _entries.value = (listOf(entry) + _entries.value).take(500)
    }
}
