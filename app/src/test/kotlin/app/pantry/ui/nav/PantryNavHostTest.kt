package app.pantry.ui.nav

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.compose.rememberNavController
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
        composeRule.setContent {
            PantryNavHost(navController = rememberNavController())
        }
        composeRule.onNodeWithText("Sign in (placeholder)").assertIsDisplayed()
    }
}
