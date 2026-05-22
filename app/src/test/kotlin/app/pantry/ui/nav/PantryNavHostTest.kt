package app.pantry.ui.nav

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.UserProfile
import app.pantry.ui.auth.AuthScreen
import app.pantry.ui.auth.AuthViewModel
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
class PantryNavHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun starts_on_auth_placeholder() {
        val repo: AuthRepository = mockk(relaxed = true)
        every { repo.currentUser } returns MutableStateFlow(null)
        val vm = AuthViewModel(repo)

        composeRule.setContent {
            PantryNavHost(
                navController = rememberNavController(),
                authScreen = { onAuthenticated ->
                    AuthScreen(onAuthenticated = onAuthenticated, viewModel = vm)
                },
            )
        }
        composeRule.onNodeWithText("Pantry").assertIsDisplayed()
    }
}
