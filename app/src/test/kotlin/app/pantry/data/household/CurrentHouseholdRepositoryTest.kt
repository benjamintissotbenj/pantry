package app.pantry.data.household

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentHouseholdRepositoryTest {

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun makeScope() = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Test
    fun `null when no user`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(null) }
        val households: HouseholdRepository = mockk()
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        assertEquals(null, repo.currentHouseholdId.value)
    }

    @Test
    fun `null when user has no households`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk { every { observeUserHouseholds("u-1") } returns flowOf(emptyList()) }
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        repo.currentHouseholdId.test {
            var v = awaitItem()
            while (v != null) v = awaitItem()
            assertEquals(null as String?, v)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits first household id`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk {
            every { observeUserHouseholds("u-1") } returns flowOf(listOf(Household("h-1", "Casa", listOf("u-1"), "ABCDEF")))
        }
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        repo.currentHouseholdId.test {
            var v = awaitItem()
            while (v == null) v = awaitItem()
            assertEquals("h-1", v)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
