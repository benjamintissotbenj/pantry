package app.pantry.data.auth

import app.pantry.domain.model.AuthError
import app.pantry.domain.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

@Singleton
open class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser?.toProfile())
    override val currentUser: StateFlow<UserProfile?> = _currentUser

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser?.toProfile()
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> =
        runAuth { firebaseAuth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile> =
        runAuth {
            val res = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            res.user?.updateProfile(buildProfileChangeRequest(displayName))?.await()
            res
        }

    protected open fun buildProfileChangeRequest(displayName: String): UserProfileChangeRequest =
        UserProfileChangeRequest.Builder().setDisplayName(displayName).build()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(mapAuthError(t))
        }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> =
        runAuth {
            val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
        }

    override suspend fun signOut(): Result<Unit> =
        try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(AuthError.Unknown(rootCause = t))
        }

    private inline fun runAuth(block: () -> com.google.firebase.auth.AuthResult): Result<UserProfile> =
        try {
            val user = block().user
                ?: return Result.failure(
                    AuthError.Unknown(rootCause = IllegalStateException("Null FirebaseUser after auth call"))
                )
            Result.success(user.toProfile())
        } catch (t: Throwable) {
            Result.failure(mapAuthError(t))
        }

    private fun mapAuthError(t: Throwable): AuthError = when (t) {
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
        is IOException -> AuthError.NoNetwork
        else -> AuthError.Unknown(rootCause = t)
    }

    private fun FirebaseUser.toProfile(): UserProfile = UserProfile(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = email.orEmpty(),
        householdIds = emptyList(), // populated by HouseholdRepository in US-9
    )
}
