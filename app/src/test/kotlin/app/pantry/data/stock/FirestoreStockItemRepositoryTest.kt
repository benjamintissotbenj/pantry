package app.pantry.data.stock

import app.pantry.domain.model.StockUnit
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirestoreStockItemRepositoryTest {

    private fun mockChain(
        docId: String = "i-new",
    ): Triple<FirebaseFirestore, CollectionReference, DocumentReference> {
        val firestore: FirebaseFirestore = mockk(relaxed = true)
        val householdsCol: CollectionReference = mockk(relaxed = true)
        val householdDoc: DocumentReference = mockk(relaxed = true)
        val itemsCol: CollectionReference = mockk(relaxed = true)
        val itemDoc: DocumentReference = mockk(relaxed = true) {
            every { id } returns docId
        }
        every { firestore.collection("households") } returns householdsCol
        every { householdsCol.document(any<String>()) } returns householdDoc
        every { householdDoc.collection("items") } returns itemsCol
        every { itemsCol.document() } returns itemDoc
        every { itemsCol.document(any<String>()) } returns itemDoc
        return Triple(firestore, itemsCol, itemDoc)
    }

    @Test
    fun `create writes item doc and returns StockItem`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        val dataSlot = slot<Map<String, Any>>()
        every { itemDoc.set(capture(dataSlot)) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.create(
            householdId = "h-1",
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 1.5,
            threshold = 1.0,
        )

        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("Milk", item.name)
        assertEquals(StockUnit.LITER, item.unit)
        assertEquals(1.5, item.quantity)
        assertEquals("Milk", dataSlot.captured["name"])
        assertEquals("L", dataSlot.captured["unit"])
        assertEquals(1.5, dataSlot.captured["quantity"])
        assertEquals(1.0, dataSlot.captured["threshold"])
        assertTrue(dataSlot.captured["updatedAt"] is FieldValue)
    }

    @Test
    fun `update writes full field set`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        val dataSlot = slot<Map<String, Any>>()
        every { itemDoc.update(capture(dataSlot)) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.update(
            householdId = "h-1",
            itemId = "i-1",
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 0.5,
            threshold = 1.0,
        )

        assertTrue(result.isSuccess)
        assertEquals("Milk", dataSlot.captured["name"])
        assertEquals("L", dataSlot.captured["unit"])
        assertEquals(0.5, dataSlot.captured["quantity"])
    }

    @Test
    fun `delete deletes the document`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        every { itemDoc.delete() } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.delete("h-1", "i-1")

        assertTrue(result.isSuccess)
        verify { itemDoc.delete() }
    }

    @Test
    fun `adjustQuantity uses FieldValue increment`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        val dataSlot = slot<Map<String, Any>>()
        every { itemDoc.update(capture(dataSlot)) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.adjustQuantity("h-1", "i-1", delta = -1.0)

        assertTrue(result.isSuccess)
        assertTrue(dataSlot.captured["quantity"] is FieldValue)
        assertTrue(dataSlot.captured["updatedAt"] is FieldValue)
    }
}
