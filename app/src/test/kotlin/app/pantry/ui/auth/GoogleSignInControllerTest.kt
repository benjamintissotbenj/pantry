package app.pantry.ui.auth

import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInControllerTest {

    @Test
    fun returns_null_on_cancellation() = runTest {
        val cm: CredentialManager = mockk()
        coEvery { cm.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } throws
            GetCredentialCancellationException("user cancelled")
        val controller = GoogleSignInController(
            context = ApplicationProvider.getApplicationContext(),
            webClientId = "test",
            credentialManager = cm,
        )
        assertNull(controller.requestIdToken())
    }

    @Test
    fun extracts_id_token_from_response() = runTest {
        val bundle = Bundle().apply {
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", "fake-id-token")
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "alice@example.com")
        }
        val credential = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, bundle)
        val cm: CredentialManager = mockk()
        coEvery { cm.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } returns
            GetCredentialResponse(credential)
        val controller = GoogleSignInController(
            context = ApplicationProvider.getApplicationContext(),
            webClientId = "test",
            credentialManager = cm,
        )
        assertEquals("fake-id-token", controller.requestIdToken())
    }

    @Test
    fun `throws on unexpected credential type`() = runTest {
        val credential = CustomCredential("com.some.other.type", android.os.Bundle())
        val cm: CredentialManager = mockk()
        coEvery { cm.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } returns
            GetCredentialResponse(credential)
        val controller = GoogleSignInController(
            context = ApplicationProvider.getApplicationContext(),
            webClientId = "test",
            credentialManager = cm,
        )
        try {
            controller.requestIdToken()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Unexpected credential type") == true)
        }
    }
}
