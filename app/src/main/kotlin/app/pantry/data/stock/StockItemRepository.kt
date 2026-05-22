package app.pantry.data.stock

import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import kotlinx.coroutines.flow.Flow

interface StockItemRepository {
    fun observe(householdId: String): Flow<List<StockItem>>

    suspend fun create(
        householdId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<StockItem>

    suspend fun update(
        householdId: String,
        itemId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<Unit>

    suspend fun delete(householdId: String, itemId: String): Result<Unit>

    /** Atomic relative adjustment for the +/- stepper. */
    suspend fun adjustQuantity(householdId: String, itemId: String, delta: Double): Result<Unit>
}
