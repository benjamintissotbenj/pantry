package app.pantry.ui.stock

import app.pantry.domain.model.StockUnit

data class AddEditItemUiState(
    val mode: Mode = Mode.Add,
    val itemId: String? = null,
    val name: String = "",
    val quantity: String = "0",         // string-backed so we can render input as-typed
    val unit: StockUnit = StockUnit.COUNT,
    val threshold: String = "1",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val dismissed: Boolean = false,
) {
    enum class Mode { Add, Edit }

    val canSubmit: Boolean
        get() = !isSubmitting
            && name.isNotBlank()
            && quantity.toDoubleOrNull() != null
            && threshold.toDoubleOrNull() != null
}
