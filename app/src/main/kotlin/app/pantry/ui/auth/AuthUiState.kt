package app.pantry.ui.auth

import app.pantry.domain.model.AuthError

data class AuthUiState(
    val mode: Mode = Mode.Welcome,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val isSubmitting: Boolean = false,
    val toastMessage: String? = null,        // transient errors / confirmations
    val navigateToHousehold: Boolean = false,
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetEmailError: String? = null,
    val isSendingReset: Boolean = false,
) {
    enum class Mode { Welcome, EmailSignIn, EmailSignUp }

    val canSubmit: Boolean
        get() = when (mode) {
            Mode.EmailSignIn -> emailError == null && passwordError == null && email.isNotBlank() && password.isNotBlank()
            Mode.EmailSignUp -> emailError == null && passwordError == null && displayNameError == null
                && email.isNotBlank() && password.isNotBlank() && displayName.isNotBlank()
            Mode.Welcome -> false
        }

    fun withErrorMessage(error: AuthError): AuthUiState = copy(
        isSubmitting = false,
        toastMessage = when (error) {
            AuthError.InvalidCredentials -> "Email or password is incorrect"
            AuthError.EmailAlreadyInUse -> "That email is already registered"
            AuthError.WeakPassword -> "Password must be at least 6 characters"
            AuthError.InvalidEmail -> "Enter a valid email address"
            AuthError.NoNetwork -> "No internet connection"
            is AuthError.Unknown -> "Something went wrong. Try again."
        }
    )
}
