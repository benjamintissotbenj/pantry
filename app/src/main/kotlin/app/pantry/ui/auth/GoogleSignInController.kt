package app.pantry.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInController(
    private val context: Context,
    private val webClientId: String,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) {
    /** Returns the Google ID token, or null if the user cancelled, or throws on error. */
    suspend fun requestIdToken(): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val response: GetCredentialResponse = credentialManager.getCredential(context, request)
            extractIdToken(response)
        } catch (_: GetCredentialCancellationException) {
            null
        }
    }

    private fun extractIdToken(response: GetCredentialResponse): String {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}. Check webClientId and Credential Manager configuration.")
    }
}
