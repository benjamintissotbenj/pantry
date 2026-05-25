package app.pantry.data.shopping

import app.pantry.domain.model.ShoppingEntry
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreShoppingEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ShoppingEntryRepository {

    private fun col(householdId: String) =
        firestore.collection("households").document(householdId).collection("shoppingList")

    private fun itemsCol(householdId: String) =
        firestore.collection("households").document(householdId).collection("items")

    override fun observe(householdId: String): Flow<List<ShoppingEntry>> = callbackFlow {
        val reg = col(householdId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { qs, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) { trySend(emptyList()); close() }
                    else close(error)
                    return@addSnapshotListener
                }
                trySend(qs?.documents.orEmpty().mapNotNull { it.toShoppingEntry() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addEntry(
        householdId: String,
        name: String,
        linkedItemId: String?,
    ): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("name", name)
            put("checked", false)
            put("createdAt", FieldValue.serverTimestamp())
            if (linkedItemId != null) put("itemId", linkedItemId)
        }
        col(householdId).document().set(data).await()
    }

    override suspend fun setChecked(
        householdId: String,
        entryId: String,
        checked: Boolean,
    ): Result<Unit> = runCatching {
        col(householdId).document(entryId).update("checked", checked).await()
    }

    override suspend fun finishShopping(
        householdId: String,
        plan: FinishShoppingPlan,
    ): Result<FinishShoppingReport> = runCatching {
        val batch = firestore.batch()
        plan.restocks.forEach { r ->
            val ref = itemsCol(householdId).document(r.itemId)
            batch.update(
                ref,
                mapOf(
                    "quantity" to r.newQuantity,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        plan.manualEntryIdsToDelete.forEach { id ->
            batch.delete(col(householdId).document(id))
        }
        batch.commit().await()
        FinishShoppingReport(
            restockedCount = plan.restocks.size,
            clearedCount = plan.manualEntryIdsToDelete.size,
            skippedCount = plan.skippedNames.size,
            skippedNames = plan.skippedNames,
        )
    }

    private fun DocumentSnapshot.toShoppingEntry(): ShoppingEntry? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val checked = getBoolean("checked") ?: false
        val itemId = getString("itemId")
        val createdAt = getTimestamp("createdAt")
            ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
            ?: Instant.EPOCH
        // category/currentQuantity/threshold/defaultRestockQuantity are filled in
        // by the ViewModel when it resolves the linkedItemId against the items list.
        return ShoppingEntry(
            id = id,
            name = name,
            source = ShoppingEntry.Source.MANUAL,
            checked = checked,
            createdAt = createdAt,
            linkedItemId = itemId,
            category = "Other",
            currentQuantity = null,
            threshold = null,
            defaultRestockQuantity = null,
        )
    }
}
