package app.pantry.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
class HomePlaceholderScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sign_out_button_calls_signout_and_navigates() {
        val repo: AuthRepository = mockk(relaxed = true) {
            every { currentUser } returns MutableStateFlow(null)
        }
        coEvery { repo.signOut() } returns Result.success(Unit)

        var signedOut = false
        composeRule.setContent {
            HomePlaceholderScreen(
                onSignedOut = { signedOut = true },
                viewModel = HomeViewModel(repo),
            )
        }
        composeRule.onNodeWithTag("signout").performClick()
        composeRule.waitUntil(2_000) { signedOut }
    }
}
