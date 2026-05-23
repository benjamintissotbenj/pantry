package app.pantry.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class AndroidConnectivityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityRepository {

    private val cm: ConnectivityManager =
        context.getSystemService() ?: error("ConnectivityManager unavailable")

    private val initial: Boolean = !cm.isCurrentlyConnected()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isOffline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val available = mutableSetOf<Network>()
            override fun onAvailable(network: Network) {
                available += network
                trySend(false)
            }
            override fun onLost(network: Network) {
                available -= network
                if (available.isEmpty()) trySend(true)
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (hasInternet) {
                    available += network
                    trySend(false)
                } else {
                    available -= network
                    if (available.isEmpty()) trySend(true)
                }
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        trySend(!cm.isCurrentlyConnected())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.Eagerly, initial)

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        val active = activeNetwork ?: return false
        val caps = getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
