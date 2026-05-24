package app.pantry.ui.settings

data class SettingsUiState(
    val isOffline: Boolean = false,
    val householdId: String? = null,
    val householdName: String = "",
    val inviteCode: String = "",
    val currentUserUid: String = "",
    val isCreator: Boolean = false,
    val members: List<MemberRow> = emptyList(),
    val categories: List<String> = emptyList(),
    val appVersion: String = "",
    val pendingSnackbar: String? = null,
    val pendingClipboard: String? = null,
    val pendingShareCode: String? = null,
    val pendingPostLeaveNav: Boolean = false,
    val signedOut: Boolean = false,
)

data class MemberRow(
    val uid: String,
    val displayName: String,
    val email: String,
    val isYou: Boolean,
    val canRemove: Boolean,
)
