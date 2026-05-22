package app.pantry.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeShellTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun defaults_to_stock_tab() {
        composeRule.setContent { HomeShell(onSignedOut = {}) }
        composeRule.onNodeWithText("Stock — coming soon").assertIsDisplayed()
    }

    @Test
    fun tap_shopping_tab_shows_shopping_placeholder() {
        composeRule.setContent { HomeShell(onSignedOut = {}) }
        composeRule.onNodeWithTag("tab_shopping").performClick()
        composeRule.onNodeWithText("Shopping — coming soon").assertIsDisplayed()
    }
}
