package app.pantry.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.household.JoinHouseholdError
import app.pantry.data.household.JoinHouseholdGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HouseholdOnboardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val joinGateway: JoinHouseholdGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(HouseholdOnboardingUiState())
    val uiState: StateFlow<HouseholdOnboardingUiState> = _state.asStateFlow()

    fun switchMode(mode: HouseholdOnboardingUiState.Mode) = _state.update { it.copy(mode = mode) }

    fun onNameChange(value: String) = _state.update {
        it.copy(
            householdName = value,
            nameError = when {
                value.isBlank() -> null
                value.length > 40 -> "Max 40 characters"
                else -> null
            },
        )
    }

    fun onInviteCodeChange(value: String) = _state.update {
        val cleaned = value.uppercase().take(HouseholdOnboardingUiState.INVITE_CODE_LENGTH)
        it.copy(inviteCode = cleaned, inviteError = null)
    }

    fun submitCreate() {
        val s = _state.value
        if (!s.canSubmitCreate) return
        val profile = auth.currentUser.value ?: return
        _state.update { it.copy(isSubmitting = true, toast = null) }
        viewModelScope.launch {
            households.create(
                name = s.householdName.trim(),
                ownerUid = profile.uid,
                ownerDisplayName = profile.displayName,
                ownerEmail = profile.email,
            ).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHome = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to create household") } },
            )
        }
    }

    fun submitJoin() {
        val s = _state.value
        if (!s.canSubmitJoin) return
        _state.update { it.copy(isSubmitting = true, inviteError = null, toast = null) }
        viewModelScope.launch {
            joinGateway.joinByCode(s.inviteCode).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHome = true) } },
                onFailure = { e ->
                    _state.update {
                        when (e) {
                            JoinHouseholdError.NotFound,
                            JoinHouseholdError.AlreadyMember -> it.copy(isSubmitting = false, inviteError = e.message)
                            JoinHouseholdError.NoNetwork     -> it.copy(isSubmitting = false, toast = e.message)
                            else                             -> it.copy(isSubmitting = false, toast = e.message ?: "Failed to join household")
                        }
                    }
                },
            )
        }
    }

    fun consumeNavigation() = _state.update { it.copy(navigateToHome = false) }
    fun consumeToast() = _state.update { it.copy(toast = null) }
}
