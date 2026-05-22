package app.pantry.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.AuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _state.asStateFlow()

    fun switchMode(mode: AuthUiState.Mode) = _state.update { it.copy(mode = mode) }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = if (value.isBlank() || EMAIL_REGEX.matches(value)) null else "Enter a valid email address")
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = if (value.isBlank() || value.length >= 6) null else "Password must be at least 6 characters")
    }

    fun onDisplayNameChange(value: String) = _state.update {
        it.copy(displayName = value, displayNameError = if (value.isBlank()) "Required" else null)
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit || s.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, toastMessage = null) }
        viewModelScope.launch {
            val result = when (s.mode) {
                AuthUiState.Mode.EmailSignIn -> auth.signInWithEmail(s.email, s.password)
                AuthUiState.Mode.EmailSignUp -> auth.signUpWithEmail(s.email, s.password, s.displayName)
                AuthUiState.Mode.Welcome -> return@launch
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHousehold = true) } },
                onFailure = { e -> _state.update { it.withErrorMessage(e as? AuthError ?: AuthError.Unknown(e)) } },
            )
        }
    }

    fun consumeNavigation() = _state.update { it.copy(navigateToHousehold = false) }
    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
