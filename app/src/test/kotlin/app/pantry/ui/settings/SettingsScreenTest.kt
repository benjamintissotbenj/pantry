package app.pantry.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
class SettingsScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun makeVm(
        householdName: String = "",
        inviteCode: String = "",
        members: List<MemberRow> = emptyList(),
        categories: List<String> = emptyList(),
        appVersion: String = "",
        isOffline: Boolean = false,
    ): SettingsViewModel {
        val state = SettingsUiState(
            householdName = householdName,
            inviteCode = inviteCode,
            members = members,
            categories = categories,
            appVersion = appVersion,
            isOffline = isOffline,
        )
        return mockk<SettingsViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(state)
        }
    }

    @Test
    fun `renders household name, invite code, members, categories, app version`() {
        val vm = makeVm(
            householdName = "Smith family",
            inviteCode = "AB1234",
            members = listOf(
                MemberRow("u1", "Ben", "ben@example.com", isYou = true, canRemove = false),
                MemberRow("u2", "Alice", "alice@example.com", isYou = false, canRemove = false),
            ),
            categories = listOf("Bakery", "Dairy"),
            appVersion = "1.0.0",
        )
        compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
        // Items near the top of the scroll are displayed
        compose.onNodeWithText("Smith family").assertIsDisplayed()
        compose.onNodeWithText("AB1234").assertIsDisplayed()
        // Items deeper in the scroll exist in the composition tree even if not in viewport
        compose.onNodeWithTag("row_member_u1").assertExists()
        compose.onNodeWithTag("row_member_u2").assertExists()
        compose.onNodeWithTag("row_category_Bakery").assertExists()
        compose.onNodeWithTag("text_app_version").assertExists()
    }

    @Test
    fun `offline banner appears when state isOffline`() {
        val vm = makeVm(isOffline = true)
        compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
        compose.onNodeWithTag("offline_banner").assertIsDisplayed()
    }

    @Test
    fun `tapping household row opens rename dialog and Save fires VM`() {
        val vm = makeVm(householdName = "Old")
        compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
        compose.onNodeWithTag("row_rename_household").performClick()
        compose.onNodeWithTag("field_household_name").performTextReplacement("New name")
        compose.onNodeWithTag("btn_rename_save").performClick()
        io.mockk.verify { vm.onRenameHousehold("New name") }
    }
}
