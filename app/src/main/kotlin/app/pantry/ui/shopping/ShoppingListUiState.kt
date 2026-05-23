package app.pantry.ui.shopping

import app.pantry.domain.model.ShoppingEntry

data class ShoppingListUiState(
    // TODO(phase-4): wire to ConnectivityRepository when it is introduced.
    val isOffline: Boolean = false,
    val isLoading: Boolean = true,
    val runningLow: List<CategorySubgroup> = emptyList(),
    val manual: List<CategorySubgroup> = emptyList(),
    val boughtQuantities: Map<String, String> = emptyMap(), // entryId -> typed string
    val finishShoppingPreview: FinishShoppingPreview = FinishShoppingPreview(0, 0, 0),
    val canFinish: Boolean = false,
    val pendingReport: FinishShoppingReport? = null,
    val skippedDialogVisible: Boolean = false,
    val pendingPromotion: PendingPromotion? = null,
    val existingCategories: List<String> = emptyList(),
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

data class PendingPromotion(val name: String, val quantity: Double)
