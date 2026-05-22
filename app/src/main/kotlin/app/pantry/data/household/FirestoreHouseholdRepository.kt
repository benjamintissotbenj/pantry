package app.pantry.data.household

import app.pantry.domain.model.Household
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreHouseholdRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val codes: InviteCodeGenerator,
) : HouseholdRepository {

    override fun observe(householdId: String): Flow<Household?> = callbackFlow {
        val reg = firestore.collection("households").document(householdId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toHousehold()) }
        awaitClose { reg.remove() }
    }

    override fun observeUserHouseholds(uid: String): Flow<List<Household>> = callbackFlow {
        val reg = firestore.collection("households")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { qs, _ ->
                trySend(qs?.documents.orEmpty().mapNotNull { it.toHousehold() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun create(name: String, ownerUid: String): Result<Household> = runCatching {
        val code = uniqueCode()
        val doc = firestore.collection("households").document()
        val data = mapOf(
            "name" to name,
            "memberUids" to listOf(ownerUid),
            "inviteCode" to code,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to ownerUid,
        )
        firestore.runBatch { batch ->
            batch.set(doc, data)
            batch.update(firestore.collection("users").document(ownerUid), "households", FieldValue.arrayUnion(doc.id))
        }.await()
        Household(id = doc.id, name = name, memberUids = listOf(ownerUid), inviteCode = code)
    }

    override suspend fun rename(householdId: String, newName: String): Result<Unit> = runCatching {
        firestore.collection("households").document(householdId).update("name", newName).await()
    }

    override suspend fun regenerateInviteCode(householdId: String): Result<String> = runCatching {
        val code = uniqueCode()
        firestore.collection("households").document(householdId).update("inviteCode", code).await()
        code
    }

    private suspend fun uniqueCode(): String {
        repeat(MAX_ATTEMPTS) {
            val candidate = codes.next()
            val q = firestore.collection("households").whereEqualTo("inviteCode", candidate).limit(1).get().await()
            if (q.isEmpty) return candidate
        }
        error("Could not allocate a unique invite code after $MAX_ATTEMPTS attempts")
    }

    private fun DocumentSnapshot.toHousehold(): Household? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        val members = (get("memberUids") as? List<String>).orEmpty()
        return Household(
            id = id,
            name = getString("name").orEmpty(),
            memberUids = members,
            inviteCode = getString("inviteCode").orEmpty(),
        )
    }

    companion object { private const val MAX_ATTEMPTS = 5 }
}
