package app.pantry.ui.household

data class HouseholdOnboardingUiState(
    val mode: Mode = Mode.Welcome,
    val householdName: String = "",
    val inviteCode: String = "",
    val nameError: String? = null,
    val inviteError: String? = null,
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToAuth: Boolean = false,
) {
    enum class Mode { Welcome, Create, Join }
    val canSubmitCreate: Boolean
        get() = householdName.length in 1..40 && nameError == null && !isSubmitting
    val canSubmitJoin: Boolean
        get() = inviteCode.length == INVITE_CODE_LENGTH && inviteError == null && !isSubmitting
    companion object { const val INVITE_CODE_LENGTH = 6 }
}
