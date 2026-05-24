package app.pantry.ui.shopping

data class AddManualEntryUiState(
    val query: String = "",
    val suggestions: List<Suggestion> = emptyList(),
    val linkedItemId: String? = null,
    val linkedItemName: String? = null,
    val isSubmitting: Boolean = false,
    val dismissed: Boolean = false,
    val isOffline: Boolean = false,
) {
    data class Suggestion(val itemId: String, val name: String)
    val canSubmit: Boolean get() = !isSubmitting && query.isNotBlank()
}
