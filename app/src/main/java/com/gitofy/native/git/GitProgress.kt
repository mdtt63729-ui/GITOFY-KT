package com.gitofy.native.git

/**
 * One progress tick parsed from git's stderr. git writes lines such as:
 *
 *   Enumerating objects: 42, done.
 *   Compressing objects:  60% (60/100)
 *   Writing objects:  45% (45/100), 1.20 MiB | 2.30 MiB/s
 *   Receiving objects:  78% (780/1000), 3.10 MiB | 1.90 MiB/s
 *
 * We normalise those into a small immutable record the UI can render.
 */
data class GitProgress(
    val phase: Phase,
    val percent: Int?,
    val transferredBytes: Long?,
    val speed: String?,
    val raw: String,
) {
    enum class Phase { COUNTING, COMPRESSING, WRITING, RECEIVING, RESOLVING, OTHER }

    companion object {
        private val PCT = Regex("""(\d{1,3})%""")
        private val SPEED = Regex("""([\d.]+\s*[KMG]i?B/s)""")
        private val BYTES = Regex("""([\d.]+\s*[KMG]i?B)(?=\s*\|)""")
        private val SIZE = Regex("""([\d.]+)\s*([KMG])i?B""")

        fun parse(line: String): GitProgress? {
            val l = line.trim()
            if (l.isEmpty()) return null

            val phase = when {
                l.startsWith("Enumerating") || l.startsWith("Counting") -> Phase.COUNTING
                l.startsWith("Compressing") -> Phase.COMPRESSING
                l.startsWith("Writing") -> Phase.WRITING
                l.startsWith("Receiving") -> Phase.RECEIVING
                l.startsWith("Resolving") -> Phase.RESOLVING
                else -> Phase.OTHER
            }
            val percent = PCT.find(l)?.groupValues?.get(1)?.toIntOrNull()
            val speed = SPEED.find(l)?.value?.replace(Regex("\\s+"), "")
            val bytes = parseSize(BYTES.find(l)?.value)

            if (percent == null && speed == null && phase == Phase.OTHER) return null
            return GitProgress(phase, percent, bytes, speed, l)
        }

        private fun parseSize(s: String?): Long? {
            if (s == null) return null
            val m = SIZE.find(s) ?: return null
            val n = m.groupValues[1].toDoubleOrNull() ?: return null
            val mult = when (m.groupValues[2]) {
                "K" -> 1024.0
                "M" -> 1024.0 * 1024
                "G" -> 1024.0 * 1024 * 1024
                else -> 1.0
            }
            return (n * mult).toLong()
        }
    }
}
