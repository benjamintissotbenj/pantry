package app.pantry.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val householdIds: List<String> = emptyList(),
)
