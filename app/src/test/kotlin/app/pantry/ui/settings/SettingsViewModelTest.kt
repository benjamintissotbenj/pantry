package app.pantry.ui.settings

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val householdRepo: HouseholdRepository = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepo.currentUser } returns MutableStateFlow(UserProfile("u-1", "Alice", "a@b.com"))
        every { householdRepo.observeUserHouseholds("u-1") } returns
            flowOf(listOf(Household(id = "h-1", name = "Casa", memberUids = listOf("u-1"), inviteCode = "ABCDEF", createdBy = "", members = emptyMap())))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes household name and invite code`() = runTest {
        val vm = SettingsViewModel(authRepo, householdRepo)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("Casa", state.householdName)
        assertEquals("ABCDEF", state.inviteCode)
    }

    @Test
    fun `signOut sets signedOut on success`() = runTest {
        coEvery { authRepo.signOut() } returns Result.success(Unit)
        val vm = SettingsViewModel(authRepo, householdRepo)
        vm.signOut()
        advanceUntilIdle()
        assertTrue(vm.signedOut.value)
    }

    @Test
    fun `signOut does not set signedOut on failure`() = runTest {
        coEvery { authRepo.signOut() } returns Result.failure(RuntimeException("network"))
        val vm = SettingsViewModel(authRepo, householdRepo)
        vm.signOut()
        advanceUntilIdle()
        assertEquals(false, vm.signedOut.value)
    }
}
