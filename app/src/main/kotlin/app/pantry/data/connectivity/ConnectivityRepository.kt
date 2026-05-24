package app.pantry.data.connectivity

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityRepository {
    /** True iff the device currently has no usable network connection. */
    val isOffline: StateFlow<Boolean>
}
