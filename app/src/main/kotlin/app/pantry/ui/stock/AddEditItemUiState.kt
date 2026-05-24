package app.pantry.ui.stock

import app.pantry.domain.model.StockUnit

data class AddEditItemUiState(
    val mode: Mode = Mode.Add,
    val itemId: String? = null,
    val name: String = "",
    val quantity: String = "",
    val unit: StockUnit = StockUnit.COUNT,
    val threshold: String = "1",
    val defaultRestockQuantity: String = "",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val dismissed: Boolean = false,
    val isOffline: Boolean = false,
) {
    enum class Mode { Add, Edit }

    val canSubmit: Boolean
        get() = !isSubmitting
            && name.isNotBlank()
            && (quantity.isBlank() || quantity.toDoubleOrNull() != null)
            && (threshold.isBlank() || threshold.toDoubleOrNull() != null)
            && (defaultRestockQuantity.isBlank() || defaultRestockQuantity.toDoubleOrNull() != null)
}
