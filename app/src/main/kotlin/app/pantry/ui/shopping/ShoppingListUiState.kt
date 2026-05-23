package app.pantry.ui.shopping

import app.pantry.domain.model.ShoppingEntry

data class ShoppingListUiState(
    val isOffline: Boolean = false,
    val isLoading: Boolean = true,
    val runningLow: List<CategorySubgroup> = emptyList(),
    val manual: List<CategorySubgroup> = emptyList(),
    val finishShoppingPreview: FinishShoppingPreview = FinishShoppingPreview(0, 0, 0),
    val pendingReport: FinishShoppingReport? = null,
    val skippedDialogVisible: Boolean = false,
) {
    val isEmpty: Boolean get() = runningLow.isEmpty() && manual.isEmpty()
}

data class CategorySubgroup(
    val category: String,
    val entries: List<ShoppingEntry>,
)

data class FinishShoppingPreview(
    val restockCount: Int,
    val clearCount: Int,
    val skipCount: Int,
)

data class FinishShoppingReport(
    val restockedCount: Int,
    val clearedCount: Int,
    val skippedCount: Int,
    val skippedNames: List<String>,
)
