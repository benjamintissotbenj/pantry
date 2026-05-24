package app.pantry.data.household

import app.pantry.domain.model.Household
import app.pantry.domain.model.MemberSummary
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
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
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toHousehold())
            }
        awaitClose { reg.remove() }
    }

    override fun observeUserHouseholds(uid: String): Flow<List<Household>> = callbackFlow {
        val reg = firestore.collection("households")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { qs, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(qs?.documents.orEmpty().mapNotNull { it.toHousehold() })
            }
        awaitClose { reg.remove() }
    }

    // Note: invite codes are NOT collision-checked client-side. The Firestore read rule
    // restricts households to members, so a global "where inviteCode == X" query is denied
    // for non-members. With 36^6 ≈ 2.2B possible codes, accidental collisions for a
    // personal-scale app are vanishingly rare; if uniqueness ever matters strictly, add
    // a Cloud Function that runs the check with admin privileges.
    override suspend fun create(
        name: String,
        ownerUid: String,
        ownerDisplayName: String,
        ownerEmail: String,
    ): Result<Household> = runCatching {
        val code = codes.next()
        val doc = firestore.collection("households").document()
        val data = mapOf(
            "name" to name,
            "memberUids" to listOf(ownerUid),
            "inviteCode" to code,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to ownerUid,
            "members" to mapOf(
                ownerUid to mapOf(
                    "displayName" to ownerDisplayName,
                    "email" to ownerEmail,
                ),
            ),
        )
        firestore.runBatch { batch ->
            batch.set(doc, data)
            batch.set(
                firestore.collection("users").document(ownerUid),
                mapOf("households" to FieldValue.arrayUnion(doc.id)),
                SetOptions.merge(),
            )
        }.await()
        Household(
            id = doc.id,
            name = name,
            memberUids = listOf(ownerUid),
            inviteCode = code,
            createdBy = ownerUid,
            members = mapOf(ownerUid to MemberSummary(ownerDisplayName, ownerEmail)),
        )
    }

    override suspend fun rename(householdId: String, newName: String): Result<Unit> = runCatching {
        firestore.collection("households").document(householdId).update("name", newName).await()
    }

    override suspend fun regenerateInviteCode(householdId: String): Result<String> = runCatching {
        val code = codes.next()
        firestore.collection("households").document(householdId).update("inviteCode", code).await()
        code
    }

    override suspend fun removeMember(householdId: String, uid: String): Result<Unit> = runCatching {
        val functions = FirebaseFunctions.getInstance("europe-west1")
        functions.getHttpsCallable("removeMember")
            .call(mapOf("hid" to householdId, "uid" to uid))
            .await()
        Unit
    }

    override suspend fun renameCategory(
        householdId: String,
        oldName: String,
        newName: String,
    ): Result<Int> = runCatching {
        val itemsCol = firestore.collection("households").document(householdId).collection("items")
        val snapshot = itemsCol.whereEqualTo("category", oldName).get().await()
        val count = snapshot.size()
        if (count > 450) {
            throw IllegalStateException("Too many items to rename in one batch — try v2")
        }
        if (count == 0) return@runCatching 0
        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, mapOf(
                "category" to newName,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
        }
        batch.commit().await()
        count
    }

    override suspend fun leaveHousehold(householdId: String): Result<Unit> =
        Result.failure(NotImplementedError("US-13 will implement"))

    private fun DocumentSnapshot.toHousehold(): Household? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        val memberUids = (get("memberUids") as? List<String>).orEmpty()
        val createdBy = getString("createdBy").orEmpty()
        @Suppress("UNCHECKED_CAST")
        val rawMembers = (get("members") as? Map<String, Map<String, Any?>>).orEmpty()
        val members = rawMembers.mapValues { (_, m) ->
            MemberSummary(
                displayName = m["displayName"] as? String ?: "",
                email = m["email"] as? String ?: "",
            )
        }
        return Household(
            id = id,
            name = getString("name").orEmpty(),
            memberUids = memberUids,
            inviteCode = getString("inviteCode").orEmpty(),
            createdBy = createdBy,
            members = members,
        )
    }

}
