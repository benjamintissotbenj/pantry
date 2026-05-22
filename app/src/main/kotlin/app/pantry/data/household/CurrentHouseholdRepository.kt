package app.pantry.data.household

import app.pantry.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class CurrentHouseholdRepository @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentHouseholdId: StateFlow<String?> = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else households.observeUserHouseholds(profile.uid)
        }
        .map { it.firstOrNull()?.id }
        .stateIn(scope, SharingStarted.Eagerly, null)
}
