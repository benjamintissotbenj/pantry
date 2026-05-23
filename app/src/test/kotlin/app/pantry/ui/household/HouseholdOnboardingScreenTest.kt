package app.pantry.ui.household

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.household.JoinHouseholdGateway
import app.pantry.domain.model.Household
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
class HouseholdOnboardingScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun creates_household_and_navigates() {
        val authRepo: AuthRepository = mockk(relaxed = true)
        val householdRepo: HouseholdRepository = mockk(relaxed = true)
        val joinGateway: JoinHouseholdGateway = mockk(relaxed = true)
        every { authRepo.currentUser } returns MutableStateFlow(UserProfile("u-1", "Alice", "a@b.com"))
        coEvery { householdRepo.create("Casa", "u-1") } returns Result.success(
            Household(id = "h-1", name = "Casa", memberUids = listOf("u-1"), inviteCode = "ABCDEF", createdBy = "", members = emptyMap())
        )

        var created = false
        composeRule.setContent {
            HouseholdOnboardingScreen(
                onCreated = { created = true },
                onJoined = {},
                viewModel = HouseholdOnboardingViewModel(authRepo, householdRepo, joinGateway),
            )
        }
        composeRule.onNodeWithText("Set up your household").assertIsDisplayed()
        composeRule.onNodeWithTag("create_btn").performClick()
        composeRule.onNodeWithTag("create_submit").assertIsNotEnabled() // empty name
        composeRule.onNodeWithTag("create_name").performTextInput("Casa")
        composeRule.onNodeWithTag("create_submit").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { created }
    }
}
