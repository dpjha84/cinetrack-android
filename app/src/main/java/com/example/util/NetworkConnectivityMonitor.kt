package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observes active internet connectivity using Android's [ConnectivityManager.NetworkCallback].
 */
class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = hasUsableInternet(networkCapabilities) || checkCurrentlyOnline()
        }

        override fun onUnavailable() {
            _isOnline.value = false
        }
    }

    fun start() {
        val cm = connectivityManager ?: return
        if (isRegistered) return
        _isOnline.value = checkCurrentlyOnline()
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        }
    }

    fun stop() {
        val cm = connectivityManager ?: return
        if (!isRegistered) return
        runCatching {
            cm.unregisterNetworkCallback(networkCallback)
        }
        isRegistered = false
    }

    private fun checkCurrentlyOnline(): Boolean {
        val cm = connectivityManager ?: return true
        return runCatching {
            val active = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
            hasUsableInternet(caps)
        }.getOrDefault(true)
    }

    private fun hasUsableInternet(caps: NetworkCapabilities): Boolean {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return hasInternet && isValidated
    }
}
