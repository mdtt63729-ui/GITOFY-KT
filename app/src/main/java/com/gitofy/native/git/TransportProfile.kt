package com.gitofy.native.git

/**
 * Transport tuning chosen from the live connection. On a fast link, spending CPU
 * on compression is wasted — the bytes go out quickly anyway — so we compress
 * less and give the packer a big window. On a slow/metered link, fewer bytes is
 * what matters, so we compress harder. HTTP/1.1 is kept everywhere because it is
 * git's (and Termux's) default and does not stall large pack uploads the way
 * HTTP/2 flow control can.
 */
data class TransportProfile(
    val name: String,
    val compression: Int,       // core.compression
    val packWindow: Int,        // pack.window
    val windowMemory: String,   // pack.windowMemory
    val postBuffer: String,     // http.postBuffer
    val httpVersion: String,    // http.version
)

object TransportProfiles {
    /** Wi-Fi / unmetered, fast upstream. */
    val fast = TransportProfile(
        name = "fast",
        compression = 1, packWindow = 250, windowMemory = "512m",
        postBuffer = "1048576000", httpVersion = "HTTP/1.1",
    )

    /** Default: Wi-Fi or a decent link. */
    val balanced = TransportProfile(
        name = "balanced",
        compression = 6, packWindow = 100, windowMemory = "256m",
        postBuffer = "524288000", httpVersion = "HTTP/1.1",
    )

    /** Metered / slow cellular — compress hard to move fewer bytes. */
    val slow = TransportProfile(
        name = "slow",
        compression = 9, packWindow = 50, windowMemory = "128m",
        postBuffer = "524288000", httpVersion = "HTTP/1.1",
    )

    /** Picks a profile from the network's metered flag and upstream bandwidth. */
    fun forNetwork(metered: Boolean, upstreamKbps: Int): TransportProfile = when {
        metered && upstreamKbps in 1..2000 -> slow
        !metered && upstreamKbps >= 20000 -> fast
        else -> balanced
    }
}
