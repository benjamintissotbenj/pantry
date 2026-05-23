package app.pantry.data.shopping

import app.pantry.domain.model.ShoppingEntry
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class FirestoreShoppingEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ShoppingEntryRepository {
    override fun observe(householdId: String): Flow<List<ShoppingEntry>> = flowOf(emptyList())
    override suspend fun addEntry(householdId: String, name: String, linkedItemId: String?) = Result.success(Unit)
    override suspend fun setChecked(householdId: String, entryId: String, checked: Boolean) = Result.success(Unit)
    override suspend fun finishShopping(householdId: String, plan: FinishShoppingPlan) =
        Result.success(FinishShoppingReport(0, 0, 0, emptyList()))
}
