package app.pantry.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.pantry.data.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForgotPasswordDialogTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sends_password_reset_and_dismisses() {
        val repo: AuthRepository = mockk(relaxed = true)
        every { repo.currentUser } returns MutableStateFlow(null)
        coEvery { repo.sendPasswordReset("a@b.com") } returns Result.success(Unit)

        composeRule.setContent {
            AuthScreen(onAuthenticated = {}, viewModel = AuthViewModel(repo))
        }

        composeRule.onNodeWithText("Sign in with email").performClick()
        composeRule.onNodeWithTag("forgot_password").performClick()
        // Dialog opens
        composeRule.onNodeWithText("Reset your password").assertIsDisplayed()
        composeRule.onNodeWithTag("reset_email").performTextInput("a@b.com")
        composeRule.onNodeWithTag("reset_send").assertIsEnabled().performClick()
        // Dialog dismisses after successful reset
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("Reset your password").fetchSemanticsNodes().isEmpty()
        }
    }
}
