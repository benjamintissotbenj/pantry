package app.pantry.domain.model

import java.time.Instant

data class StockItem(
    val id: String,
    val name: String,
    val category: String,
    val unit: StockUnit,
    val quantity: Double,
    val threshold: Double,
    val updatedAt: Instant,
    val defaultRestockQuantity: Double?,
) {
    val isLowStock: Boolean get() = quantity < threshold
}
