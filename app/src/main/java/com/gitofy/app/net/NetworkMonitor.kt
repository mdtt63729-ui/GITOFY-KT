package com.gitofy.app.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Reports whether the current connection is metered and how fast it is upstream. */
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) {

    data class Info(val metered: Boolean, val upstreamKbps: Int, val connected: Boolean)

    fun current(): Info {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return Info(false, 0, false)
        val net = cm.activeNetwork ?: return Info(false, 0, false)
        val caps = cm.getNetworkCapabilities(net) ?: return Info(false, 0, false)
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val up = caps.linkUpstreamBandwidthKbps
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return Info(metered, up, connected)
    }

    /**
     * Emits whenever the default network changes (Wi-Fi <-> cellular) or its
     * capabilities change, so a running upload can re-tune its transport.
     */
    fun changes(): Flow<Info> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        trySend(current())
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onLost(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
        }
        runCatching { cm?.registerDefaultNetworkCallback(cb) }
        awaitClose { runCatching { cm?.unregisterNetworkCallback(cb) } }
    }.distinctUntilChanged()
}
