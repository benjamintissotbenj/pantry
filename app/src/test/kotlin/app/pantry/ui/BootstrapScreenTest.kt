package app.pantry.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.pantry.BootstrapScreenForTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BootstrapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shows_placeholder_text() {
        composeRule.setContent { MaterialTheme { BootstrapScreenForTest() } }
        composeRule.onNodeWithText("Pantry — bootstrap OK").assertIsDisplayed()
    }
}
