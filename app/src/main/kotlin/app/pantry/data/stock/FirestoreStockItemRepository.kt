package app.pantry.data.stock

import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreStockItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StockItemRepository {

    private fun itemsCol(householdId: String) =
        firestore.collection("households").document(householdId).collection("items")

    override fun observe(householdId: String): Flow<List<StockItem>> = callbackFlow {
        val reg = itemsCol(householdId).addSnapshotListener { qs, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(qs?.documents.orEmpty().mapNotNull { it.toStockItem() })
        }
        awaitClose { reg.remove() }
    }

    override suspend fun create(
        householdId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<StockItem> = runCatching {
        val doc = itemsCol(householdId).document()
        val data = mapOf(
            "name" to name,
            "category" to category,
            "unit" to unit.storageKey,
            "quantity" to quantity,
            "threshold" to threshold,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        doc.set(data).await()
        StockItem(
            id = doc.id,
            name = name,
            category = category,
            unit = unit,
            quantity = quantity,
            threshold = threshold,
            updatedAt = Instant.now(),
        )
    }

    override suspend fun update(
        householdId: String,
        itemId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "name" to name,
            "category" to category,
            "unit" to unit.storageKey,
            "quantity" to quantity,
            "threshold" to threshold,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        itemsCol(householdId).document(itemId).update(data).await()
    }

    override suspend fun delete(householdId: String, itemId: String): Result<Unit> = runCatching {
        itemsCol(householdId).document(itemId).delete().await()
    }

    override suspend fun adjustQuantity(
        householdId: String,
        itemId: String,
        delta: Double,
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "quantity" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        itemsCol(householdId).document(itemId).update(data).await()
    }

    private fun DocumentSnapshot.toStockItem(): StockItem? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val category = getString("category").orEmpty()
        val unit = StockUnit.fromStorageKey(getString("unit"))
        val quantity = getDouble("quantity") ?: 0.0
        val threshold = getDouble("threshold") ?: 1.0
        val ts = getTimestamp("updatedAt") ?: Timestamp.now()
        return StockItem(
            id = id,
            name = name,
            category = category,
            unit = unit,
            quantity = quantity,
            threshold = threshold,
            updatedAt = Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong()),
        )
    }
}
