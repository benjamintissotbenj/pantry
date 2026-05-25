package app.pantry.data.household

interface JoinHouseholdGateway {
    suspend fun joinByCode(code: String): Result<String>  // returns householdId on success
}

sealed class JoinHouseholdError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    data object NotFound : JoinHouseholdError("No household found for that code.")
    data object AlreadyMember : JoinHouseholdError("You're already in this household.")
    data object NotAuthenticated : JoinHouseholdError("Authentication error — please sign out and sign back in.")
    data object NoNetwork : JoinHouseholdError("No internet connection")
    data class Unknown(val throwable: Throwable) :
        JoinHouseholdError(throwable.message ?: "Failed to join household", throwable)
}
