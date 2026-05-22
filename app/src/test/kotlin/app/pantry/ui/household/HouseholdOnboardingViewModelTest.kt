package app.pantry.ui.household

import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdOnboardingViewModelTest {

    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val householdRepo: HouseholdRepository = mockk(relaxed = true)
    private lateinit var vm: HouseholdOnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepo.currentUser } returns MutableStateFlow(UserProfile("u-1", "Alice", "a@b.com"))
        vm = HouseholdOnboardingViewModel(authRepo, householdRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `create flow navigates to home on success`() = runTest {
        coEvery { householdRepo.create("Casa", "u-1") } returns Result.success(
            Household("h-1", "Casa", listOf("u-1"), "ABCDEF")
        )
        vm.switchMode(HouseholdOnboardingUiState.Mode.Create)
        vm.onNameChange("Casa")
        vm.submitCreate()
        assertTrue(vm.uiState.value.navigateToHome)
    }

    @Test
    fun `create flow shows toast and clears submitting on failure`() = runTest {
        coEvery { householdRepo.create("Casa", "u-1") } returns Result.failure(Exception("Network error"))
        vm.switchMode(HouseholdOnboardingUiState.Mode.Create)
        vm.onNameChange("Casa")
        vm.submitCreate()
        val s = vm.uiState.value
        assertFalse(s.isSubmitting)
        assertFalse(s.navigateToHome)
        assertEquals("Network error", s.toast)
    }
}
