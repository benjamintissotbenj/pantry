package app.pantry.ui.common

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineBannerTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `shows banner when offline`() {
        compose.setContent { OfflineBanner(isOffline = true) }
        compose.onNodeWithTag("offline_banner").assertIsDisplayed()
        compose.onNodeWithText("Offline — changes will sync when you reconnect.").assertIsDisplayed()
    }

    @Test
    fun `hides banner when online`() {
        compose.setContent { OfflineBanner(isOffline = false) }
        compose.onNodeWithTag("offline_banner").assertDoesNotExist()
    }
}
