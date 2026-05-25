package app.pantry.data.household

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseJoinHouseholdGateway @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firebaseAuth: FirebaseAuth,
) : JoinHouseholdGateway {

    override suspend fun joinByCode(code: String): Result<String> {
        // Guard: new users can reach this screen while Firebase Auth's token cache is still
        // initialising. If currentUser is already null here, the callable would be sent
        // without a token and the Cloud Function would throw UNAUTHENTICATED.
        if (firebaseAuth.currentUser == null) return Result.failure(JoinHouseholdError.NotAuthenticated)
        return joinByCodeInternal(code)
    }

    private suspend fun joinByCodeInternal(code: String): Result<String> = try {
        val result = functions.getHttpsCallable("joinHousehold").call(mapOf("code" to code)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as Map<String, Any?>
        val householdId = data["householdId"] as? String
            ?: return Result.failure(JoinHouseholdError.Unknown(IllegalStateException("Missing householdId")))
        Result.success(householdId)
    } catch (e: FirebaseFunctionsException) {
        Result.failure(
            when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> JoinHouseholdError.NotFound
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> JoinHouseholdError.AlreadyMember
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> JoinHouseholdError.NotAuthenticated
                else -> JoinHouseholdError.Unknown(e)
            }
        )
    } catch (e: IOException) {
        Result.failure(JoinHouseholdError.NoNetwork)
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure(JoinHouseholdError.Unknown(e))
    }
}
