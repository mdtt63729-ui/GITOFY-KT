package com.gitofy.native.git

/** Raised when the bundled git exits non-zero. Carries the tail of stderr. */
class GitException(
    val exitCode: Int,
    val command: List<String>,
    val stderrTail: String,
) : RuntimeException(
    "git ${command.joinToString(" ")} failed (exit $exitCode): ${stderrTail.takeLast(600)}"
)
