package com.gitofy.native.git

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One compression level's measured cost + estimated whole-push time. */
data class BenchmarkLevel(
    val level: Int,
    val compressMs: Double,
    val ratio: Double,
    val estimatedTotalSec: Double,
    val chosen: Boolean,
)

/** The last benchmark run, for the Settings chart. */
data class BenchmarkResult(
    val levels: List<BenchmarkLevel>,
    val upstreamKbps: Int,
    val totalBytes: Long,
    val profileName: String,
    val at: Long,
)

/** Keeps the most recent benchmark so Settings can chart it. */
@Singleton
class BenchmarkStore @Inject constructor() {
    private val _last = MutableStateFlow<BenchmarkResult?>(null)
    val last: StateFlow<BenchmarkResult?> = _last.asStateFlow()
    fun record(result: BenchmarkResult) { _last.value = result }
}
