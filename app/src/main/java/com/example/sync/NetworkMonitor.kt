package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkOverrideMode {
    SYSTEM_REAL,        // Follows physical Android network connectivity
    SIMULATE_OFFLINE,   // Forces offline status for testing offline capture
    SIMULATE_ONLINE,    // Forces online status
    SIMULATE_SERVER_500 // Online network, but backend returns 500 to test retry logic
}

enum class ConnectionStatus {
    ONLINE,
    OFFLINE
}

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _overrideMode = MutableStateFlow(NetworkOverrideMode.SYSTEM_REAL)
    val overrideMode: StateFlow<NetworkOverrideMode> = _overrideMode.asStateFlow()

    private val _isSystemOnline = MutableStateFlow(checkInitialConnectivity())
    
    private val _effectiveStatus = MutableStateFlow(computeEffectiveStatus(_overrideMode.value, _isSystemOnline.value))
    val effectiveStatus: StateFlow<ConnectionStatus> = _effectiveStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isSystemOnline.value = true
            recompute()
        }

        override fun onLost(network: Network) {
            _isSystemOnline.value = checkInitialConnectivity()
            recompute()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isSystemOnline.value = hasInternet
            recompute()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Fallback if permission or sandbox restrictions
            _isSystemOnline.value = true
            recompute()
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private fun computeEffectiveStatus(mode: NetworkOverrideMode, systemOnline: Boolean): ConnectionStatus {
        return when (mode) {
            NetworkOverrideMode.SYSTEM_REAL -> if (systemOnline) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE
            NetworkOverrideMode.SIMULATE_OFFLINE -> ConnectionStatus.OFFLINE
            NetworkOverrideMode.SIMULATE_ONLINE, NetworkOverrideMode.SIMULATE_SERVER_500 -> ConnectionStatus.ONLINE
        }
    }

    private fun recompute() {
        _effectiveStatus.value = computeEffectiveStatus(_overrideMode.value, _isSystemOnline.value)
    }

    fun setOverrideMode(mode: NetworkOverrideMode) {
        _overrideMode.value = mode
        recompute()
    }

    val isOnline: Boolean
        get() = _effectiveStatus.value == ConnectionStatus.ONLINE
}
