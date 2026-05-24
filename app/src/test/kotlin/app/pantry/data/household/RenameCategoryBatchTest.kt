package app.pantry.data.household

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RenameCategoryBatchTest {

    @Test
    fun `renameCategory commits one update per matching item`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val batch = mockk<WriteBatch>(relaxed = true)
        val itemsCol = mockk<CollectionReference>(relaxed = true)
        val hhCol = mockk<CollectionReference>(relaxed = true)
        val hhDoc = mockk<DocumentReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val qs = mockk<QuerySnapshot>(relaxed = true)
        val doc1 = mockk<DocumentSnapshot>(relaxed = true)
        val doc2 = mockk<DocumentSnapshot>(relaxed = true)
        val ref1 = mockk<DocumentReference>(relaxed = true)
        val ref2 = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection("households") } returns hhCol
        every { hhCol.document("HH") } returns hhDoc
        every { hhDoc.collection("items") } returns itemsCol
        every { itemsCol.whereEqualTo("category", "Dairy") } returns query
        every { query.get() } returns Tasks.forResult(qs)
        every { qs.size() } returns 2
        every { qs.documents } returns listOf(doc1, doc2)
        every { doc1.reference } returns ref1
        every { doc2.reference } returns ref2
        every { firestore.batch() } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        val repo = FirestoreHouseholdRepository(firestore, mockk(relaxed = true))
        val result = repo.renameCategory("HH", "Dairy", "Fridge")

        assertEquals(2, result.getOrThrow())
        coVerify { batch.update(ref1, match<Map<String, Any>> { it["category"] == "Fridge" && it.containsKey("updatedAt") }) }
        coVerify { batch.update(ref2, match<Map<String, Any>> { it["category"] == "Fridge" && it.containsKey("updatedAt") }) }
        coVerify { batch.commit() }
    }

    @Test
    fun `renameCategory fails when more than 450 items match`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val itemsCol = mockk<CollectionReference>(relaxed = true)
        val hhCol = mockk<CollectionReference>(relaxed = true)
        val hhDoc = mockk<DocumentReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val qs = mockk<QuerySnapshot>(relaxed = true)

        every { firestore.collection("households") } returns hhCol
        every { hhCol.document("HH") } returns hhDoc
        every { hhDoc.collection("items") } returns itemsCol
        every { itemsCol.whereEqualTo("category", "Big") } returns query
        every { query.get() } returns Tasks.forResult(qs)
        every { qs.size() } returns 451

        val repo = FirestoreHouseholdRepository(firestore, mockk(relaxed = true))
        val result = repo.renameCategory("HH", "Big", "Bigger")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `renameCategory returns 0 without committing when nothing matches`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val itemsCol = mockk<CollectionReference>(relaxed = true)
        val hhCol = mockk<CollectionReference>(relaxed = true)
        val hhDoc = mockk<DocumentReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val qs = mockk<QuerySnapshot>(relaxed = true)

        every { firestore.collection("households") } returns hhCol
        every { hhCol.document("HH") } returns hhDoc
        every { hhDoc.collection("items") } returns itemsCol
        every { itemsCol.whereEqualTo("category", "Empty") } returns query
        every { query.get() } returns Tasks.forResult(qs)
        every { qs.size() } returns 0

        val repo = FirestoreHouseholdRepository(firestore, mockk(relaxed = true))
        val result = repo.renameCategory("HH", "Empty", "EmptyNew")

        assertEquals(0, result.getOrThrow())
    }
}
