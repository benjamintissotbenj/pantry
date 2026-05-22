package app.pantry.data.household

import app.pantry.domain.model.Household
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    fun observe(householdId: String): Flow<Household?>
    fun observeUserHouseholds(uid: String): Flow<List<Household>>
    suspend fun create(name: String, ownerUid: String): Result<Household>
    suspend fun rename(householdId: String, newName: String): Result<Unit>
    suspend fun regenerateInviteCode(householdId: String): Result<String>
}
