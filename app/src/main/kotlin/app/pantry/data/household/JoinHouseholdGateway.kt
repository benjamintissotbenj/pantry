package app.pantry.data.household

interface JoinHouseholdGateway {
    suspend fun joinByCode(code: String): Result<String>  // returns householdId on success
}

sealed class JoinHouseholdError(message: String) : Throwable(message) {
    data object NotFound : JoinHouseholdError("No household found for that code.")
    data object AlreadyMember : JoinHouseholdError("You're already in this household.")
    data object NoNetwork : JoinHouseholdError("No internet connection")
    data class Unknown(val throwable: Throwable) : JoinHouseholdError(throwable.message ?: "Failed to join household")
}
