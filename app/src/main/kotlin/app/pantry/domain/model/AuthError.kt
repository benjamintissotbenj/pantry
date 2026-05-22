package app.pantry.domain.model

sealed class AuthError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    data object InvalidCredentials : AuthError("Email or password is incorrect")
    data object EmailAlreadyInUse : AuthError("That email is already registered")
    data object WeakPassword : AuthError("Password must be at least 6 characters")
    data object InvalidEmail : AuthError("Enter a valid email address")
    data object NoNetwork : AuthError("No internet connection")
    data class Unknown(val rootCause: Throwable) :
        AuthError(rootCause.message ?: "Authentication failed", rootCause)
}
