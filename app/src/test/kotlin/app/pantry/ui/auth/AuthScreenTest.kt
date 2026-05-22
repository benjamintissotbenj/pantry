package app.pantry.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.UserProfile
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
class AuthScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun signs_in_with_email_and_navigates() {
        val repo: AuthRepository = mockk(relaxed = true)
        every { repo.currentUser } returns MutableStateFlow(null)
        coEvery { repo.signInWithEmail("a@b.com", "secret123") } returns Result.success(UserProfile("u-1", "A", "a@b.com"))

        var authenticated = false
        composeRule.setContent {
            AuthScreen(
                onAuthenticated = { authenticated = true },
                viewModel = AuthViewModel(repo),
            )
        }
        composeRule.onNodeWithText("Sign in with email").performClick()
        composeRule.onNodeWithTag("submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("field_email").performTextInput("a@b.com")
        composeRule.onNodeWithTag("field_password").performTextInput("secret123")
        composeRule.onNodeWithTag("submit").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { authenticated }
    }
}
