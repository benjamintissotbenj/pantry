package app.pantry.data.shopping

/**
 * Computed in the ViewModel from the current snapshot of items + manual entries +
 * auto-checked set. Handed to the repo as an immutable instruction set; the repo
 * commits it atomically.
 */
data class FinishShoppingPlan(
    /** Catalog items that get `quantity = defaultRestockQuantity` written. */
    val restocks: List<Restock>,
    /** Manual entry ids to delete unconditionally. */
    val manualEntryIdsToDelete: List<String>,
    /** Skipped items (no defaultRestockQuantity) — included only for reporting. */
    val skippedNames: List<String>,
) {
    data class Restock(
        val itemId: String,
        val newQuantity: Double,
    )
}

data class FinishShoppingReport(
    val restockedCount: Int,
    val clearedCount: Int,
    val skippedCount: Int,
    val skippedNames: List<String>,
)
