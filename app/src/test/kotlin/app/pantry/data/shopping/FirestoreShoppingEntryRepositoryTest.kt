package app.pantry.data.shopping

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FirestoreShoppingEntryRepositoryTest {

    @Test
    fun `finishShopping commits restocks then deletes`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        val itemRef = mockk<DocumentReference>(relaxed = true)
        val entryRef = mockk<DocumentReference>(relaxed = true)
        val itemsCol = mockk<CollectionReference>(relaxed = true)
        val shoppingCol = mockk<CollectionReference>(relaxed = true)
        val hhDoc = mockk<DocumentReference>(relaxed = true)
        val hhCol = mockk<CollectionReference>(relaxed = true)

        every { firestore.collection("households") } returns hhCol
        every { hhCol.document("HH") } returns hhDoc
        every { hhDoc.collection("items") } returns itemsCol
        every { hhDoc.collection("shoppingList") } returns shoppingCol
        every { itemsCol.document("item-1") } returns itemRef
        every { shoppingCol.document("entry-1") } returns entryRef

        val repo = FirestoreShoppingEntryRepository(firestore)
        val plan = FinishShoppingPlan(
            restocks = listOf(FinishShoppingPlan.Restock(itemId = "item-1", newQuantity = 4.0)),
            manualEntryIdsToDelete = listOf("entry-1"),
            skippedNames = listOf("Soap"),
        )

        val report = repo.finishShopping("HH", plan).getOrThrow()

        verifyOrder {
            firestore.batch()
            batch.update(itemRef, match<Map<String, Any>> {
                it["quantity"] == 4.0 && it.containsKey("updatedAt")
            })
            batch.delete(entryRef)
            batch.commit()
        }
        assertEquals(1, report.restockedCount)
        assertEquals(1, report.clearedCount)
        assertEquals(1, report.skippedCount)
        assertEquals(listOf("Soap"), report.skippedNames)
    }
}
