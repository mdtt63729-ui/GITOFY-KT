package com.gitofy.native.git

import java.util.zip.Deflater

/**
 * Picks the compression level that finishes the push soonest, by measuring it.
 *
 * Total time for a push is roughly
 *
 *     compress_time(level)  +  bytes_after_compression(level) / uplink_bandwidth
 *
 * so the best level depends on both the CPU and the link. We compress a ~5 MB
 * sample of the project at levels 1 / 6 / 9, measure how long each takes and how
 * small it gets, extrapolate both to the whole project, and choose the level with
 * the smallest estimated total. On a fast link that favours level 1 (skip the
 * CPU); on a slow link it favours level 9 (send fewer bytes).
 */
object TransportBenchmark {

    private val LEVELS = listOf(1, 6, 9)

    /**
     * Runs the benchmark and returns every level's cost plus which one won — the
     * whole result feeds the Settings chart, and the winning level feeds the
     * transport profile.
     */
    fun analyze(
        sample: ByteArray,
        totalBytes: Long,
        upstreamKbps: Int,
        profileName: String = "balanced",
    ): BenchmarkResult {
        if (sample.isEmpty() || totalBytes <= 0 || upstreamKbps <= 0) {
            val fallback = LEVELS.map { BenchmarkLevel(it, 0.0, 1.0, 0.0, it == 6) }
            return BenchmarkResult(fallback, upstreamKbps, totalBytes, profileName, System.currentTimeMillis())
        }
        val bytesPerSec = upstreamKbps * 1000.0 / 8.0
        val scale = totalBytes.toDouble() / sample.size

        val measured = LEVELS.map { level ->
            val t0 = System.nanoTime()
            val compressed = deflateSize(sample, level)
            val compressSec = ((System.nanoTime() - t0) / 1e9) * scale
            val ratio = compressed.toDouble() / sample.size
            val netSec = (totalBytes * ratio) / bytesPerSec
            Triple(level, Triple(compressSec, ratio, compressSec + netSec), 0)
        }
        val bestLevel = measured.minByOrNull { it.second.third }?.first ?: 6

        val levels = measured.map { (level, t, _) ->
            BenchmarkLevel(
                level = level,
                compressMs = t.first * 1000.0,
                ratio = t.second,
                estimatedTotalSec = t.third,
                chosen = level == bestLevel,
            )
        }
        return BenchmarkResult(levels, upstreamKbps, totalBytes, profileName, System.currentTimeMillis())
    }

    /** Convenience: just the winning level. */
    fun chooseCompression(sample: ByteArray, totalBytes: Long, upstreamKbps: Int): Int =
        analyze(sample, totalBytes, upstreamKbps).levels.firstOrNull { it.chosen }?.level ?: 6

    private fun deflateSize(data: ByteArray, level: Int): Int {
        val d = Deflater(level)
        try {
            d.setInput(data); d.finish()
            val buf = ByteArray(1 shl 16)
            var total = 0
            while (!d.finished()) total += d.deflate(buf)
            return total
        } finally {
            d.end()
        }
    }
}
