package app.pantry.domain.model

import java.time.Instant

/**
 * A row in the shopping list. AUTO entries are derived from catalog items where
 * quantity < threshold and have no Firestore doc (id = "auto:${itemId}"). MANUAL
 * entries live in households/{hid}/shoppingList/{entryId} and may optionally link
 * to a catalog item.
 */
data class ShoppingEntry(
    val id: String,
    val name: String,
    val source: Source,
    val checked: Boolean,
    val createdAt: Instant,
    val linkedItemId: String?,
    val category: String,
    val currentQuantity: Double?,
    val threshold: Double?,
    val defaultRestockQuantity: Double?,
) {
    enum class Source { AUTO, MANUAL }
}
