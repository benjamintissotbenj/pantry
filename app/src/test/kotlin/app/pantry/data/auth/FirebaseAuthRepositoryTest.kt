package app.pantry.data.auth

import app.pantry.domain.model.AuthError
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Test subclass that stubs out [buildProfileChangeRequest] to avoid calling
 * [com.google.firebase.auth.UserProfileChangeRequest.Builder] which internally uses
 * [android.text.TextUtils] — an Android API unavailable in plain JVM unit tests.
 */
private class TestFirebaseAuthRepository(firebaseAuth: FirebaseAuth) : FirebaseAuthRepository(firebaseAuth) {
    val stubbedProfileRequest: UserProfileChangeRequest = mockk(relaxed = true)
    override fun buildProfileChangeRequest(displayName: String): UserProfileChangeRequest = stubbedProfileRequest
}

class FirebaseAuthRepositoryTest {

    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val repo = TestFirebaseAuthRepository(firebaseAuth)

    // ── signInWithEmail ──────────────────────────────────────────────────────

    @Test
    fun `signInWithEmail returns UserProfile on success`() = runTest {
        val user = mockk<FirebaseUser> {
            every { uid } returns "u-1"
            every { email } returns "alice@example.com"
            every { displayName } returns "Alice"
        }
        val authResult = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        val result = repo.signInWithEmail("alice@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals("u-1", result.getOrThrow().uid)
    }

    @Test
    fun `signInWithEmail maps invalid credentials to AuthError InvalidCredentials`() = runTest {
        val ex = mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(ex)

        val result = repo.signInWithEmail("alice@example.com", "wrong")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.InvalidCredentials::class.java, result.exceptionOrNull())
    }

    // ── signUpWithEmail ──────────────────────────────────────────────────────

    @Test
    fun `signUpWithEmail returns UserProfile on success`() = runTest {
        val voidTask: Task<Void> = Tasks.forResult(null)
        val user = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "u-2"
            every { email } returns "bob@example.com"
            every { displayName } returns "Bob"
            every { updateProfile(any()) } returns voidTask
        }
        val authResult = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        val result = repo.signUpWithEmail("bob@example.com", "password123", "Bob")

        assertTrue(result.isSuccess) { "Expected success but got failure: ${result.exceptionOrNull()}" }
        assertEquals("u-2", result.getOrThrow().uid)
        verify { user.updateProfile(any()) }
    }

    @Test
    fun `signUpWithEmail maps email collision to EmailAlreadyInUse`() = runTest {
        val ex = mockk<FirebaseAuthUserCollisionException>(relaxed = true)
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(ex)

        val result = repo.signUpWithEmail("alice@example.com", "password123", "Alice")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.EmailAlreadyInUse::class.java, result.exceptionOrNull())
    }

    @Test
    fun `signUpWithEmail maps weak password to WeakPassword`() = runTest {
        val ex = mockk<FirebaseAuthWeakPasswordException>(relaxed = true)
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(ex)

        val result = repo.signUpWithEmail("bob@example.com", "123", "Bob")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.WeakPassword::class.java, result.exceptionOrNull())
    }

    // ── sendPasswordReset ────────────────────────────────────────────────────

    @Test
    fun `sendPasswordReset returns success`() = runTest {
        every { firebaseAuth.sendPasswordResetEmail(any()) } returns Tasks.forResult(null)

        val result = repo.sendPasswordReset("alice@example.com")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendPasswordReset maps invalid user to InvalidCredentials`() = runTest {
        val ex = mockk<FirebaseAuthInvalidUserException>(relaxed = true)
        every { firebaseAuth.sendPasswordResetEmail(any()) } returns
            Tasks.forException(ex)

        val result = repo.sendPasswordReset("unknown@example.com")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.InvalidCredentials::class.java, result.exceptionOrNull())
    }

    // ── signInWithGoogle ─────────────────────────────────────────────────────

    @Test
    fun `signInWithGoogle returns UserProfile on success`() = runTest {
        val user = mockk<FirebaseUser> {
            every { uid } returns "u-3"
            every { email } returns "carol@gmail.com"
            every { displayName } returns "Carol"
        }
        val authResult = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(authResult)

        val result = repo.signInWithGoogle("google-id-token")

        assertTrue(result.isSuccess)
        assertEquals("u-3", result.getOrThrow().uid)
    }

    @Test
    fun `signInWithGoogle maps IOException to NoNetwork`() = runTest {
        every { firebaseAuth.signInWithCredential(any()) } returns
            Tasks.forException(IOException("no network"))

        val result = repo.signInWithGoogle("google-id-token")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.NoNetwork::class.java, result.exceptionOrNull())
    }

    // ── signOut ──────────────────────────────────────────────────────────────

    @Test
    fun `signOut returns success`() = runTest {
        every { firebaseAuth.signOut() } returns Unit

        val result = repo.signOut()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `signOut wraps thrown exception in Unknown`() = runTest {
        every { firebaseAuth.signOut() } throws RuntimeException("boom")

        val result = repo.signOut()

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.Unknown::class.java, result.exceptionOrNull())
    }
}
