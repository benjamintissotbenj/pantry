package app.pantry.data.household

import app.pantry.domain.model.Household
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    fun observe(householdId: String): Flow<Household?>
    fun observeUserHouseholds(uid: String): Flow<List<Household>>
    suspend fun create(
        name: String,
        ownerUid: String,
        ownerDisplayName: String,
        ownerEmail: String,
    ): Result<Household>
    suspend fun rename(householdId: String, newName: String): Result<Unit>
    suspend fun regenerateInviteCode(householdId: String): Result<String>
    suspend fun removeMember(householdId: String, uid: String): Result<Unit>
    suspend fun renameCategory(householdId: String, oldName: String, newName: String): Result<Int>
    suspend fun leaveHousehold(householdId: String): Result<Unit>
}
