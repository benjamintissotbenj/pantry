package app.pantry.data.household

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
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
import kotlin.random.Random

class FirestoreHouseholdRepositoryTest {

    private fun makeRepo(
        codes: InviteCodeGenerator = InviteCodeGenerator(Random(seed = 42)),
    ): Pair<FirestoreHouseholdRepository, FirebaseFirestore> {
        val firestore: FirebaseFirestore = mockk(relaxed = true)
        val repo = FirestoreHouseholdRepository(firestore, codes)
        return repo to firestore
    }

    @Test
    fun `create writes batch and returns Household`() = runTest {
        val (repo, firestore) = makeRepo()

        val householdsCol: CollectionReference = mockk(relaxed = true)
        val newDoc: DocumentReference = mockk(relaxed = true) {
            every { id } returns "h-new"
        }

        every { firestore.collection("households") } returns householdsCol
        every { householdsCol.document() } returns newDoc
        every { firestore.runBatch(any()) } returns Tasks.forResult(null)

        val result = repo.create("Casa", ownerUid = "u-1", ownerDisplayName = "Test User", ownerEmail = "test@example.com")

        assertTrue(result.isSuccess)
        assertEquals("h-new", result.getOrThrow().id)
        assertEquals("Casa", result.getOrThrow().name)
        assertEquals(listOf("u-1"), result.getOrThrow().memberUids)
        assertEquals(6, result.getOrThrow().inviteCode.length)
        verify { firestore.runBatch(any()) }
    }

    @Test
    fun `rename updates name field`() = runTest {
        val (repo, firestore) = makeRepo()

        val docRef: DocumentReference = mockk(relaxed = true) {
            every { update("name", "NewName") } returns Tasks.forResult(null)
        }
        every { firestore.collection("households").document("h-1") } returns docRef

        val result = repo.rename("h-1", "NewName")

        assertTrue(result.isSuccess)
        verify { docRef.update("name", "NewName") }
    }

    @Test
    fun `regenerateInviteCode writes new code and returns it`() = runTest {
        val (repo, firestore) = makeRepo()

        val docRef: DocumentReference = mockk(relaxed = true)
        every { firestore.collection("households").document("h-1") } returns docRef

        val codeSlot = slot<String>()
        every { docRef.update("inviteCode", capture(codeSlot)) } returns Tasks.forResult(null)

        val result = repo.regenerateInviteCode("h-1")

        assertTrue(result.isSuccess)
        assertEquals(6, codeSlot.captured.length)
        assertTrue(codeSlot.captured.all { it.isUpperCase() || it.isDigit() })
        assertEquals(codeSlot.captured, result.getOrThrow())
    }
}
