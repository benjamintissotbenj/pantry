package app.pantry.data.shopping

import app.pantry.domain.model.ShoppingEntry
import kotlinx.coroutines.flow.Flow

interface ShoppingEntryRepository {
    /** Manual entries only (auto entries are derived client-side). */
    fun observe(householdId: String): Flow<List<ShoppingEntry>>

    suspend fun addEntry(
        householdId: String,
        name: String,
        linkedItemId: String?,
    ): Result<Unit>

    suspend fun setChecked(
        householdId: String,
        entryId: String,
        checked: Boolean,
    ): Result<Unit>

    /** Commits the plan as a single atomic Firestore WriteBatch. */
    suspend fun finishShopping(
        householdId: String,
        plan: FinishShoppingPlan,
    ): Result<FinishShoppingReport>
}
