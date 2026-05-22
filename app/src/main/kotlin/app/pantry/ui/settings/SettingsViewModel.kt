package app.pantry.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firstHousehold = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else households.observeUserHouseholds(profile.uid)
        }
        .map { it.firstOrNull() }

    private val _signedOut = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = firstHousehold
        .map { hh ->
            SettingsUiState(
                householdName = hh?.name.orEmpty(),
                inviteCode = hh?.inviteCode.orEmpty(),
                signedOut = _signedOut.value,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun signOut() {
        viewModelScope.launch {
            val result = auth.signOut()
            if (result.isSuccess) _signedOut.value = true
        }
    }

    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()
}
