package app.pantry.domain.model

data class Household(
    val id: String,
    val name: String,
    val memberUids: List<String>,
    val inviteCode: String,
    val createdBy: String,
    val members: Map<String, MemberSummary>,
)
