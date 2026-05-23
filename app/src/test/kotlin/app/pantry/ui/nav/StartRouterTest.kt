package app.pantry.ui.nav

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class StartRouterTest {

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `routes to Auth when no user`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(null) }
        val households: HouseholdRepository = mockk()
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            // first emission may be Loading then Auth; drain until non-Loading
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Auth, route)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routes to Household when user has no households`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk { every { observeUserHouseholds("u-1") } returns flowOf(emptyList()) }
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Household, route)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routes to Home when user has a household`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk {
            every { observeUserHouseholds("u-1") } returns flowOf(listOf(Household(id = "h-1", name = "Casa", memberUids = listOf("u-1"), inviteCode = "ABCDEF", createdBy = "", members = emptyMap())))
        }
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Home, route)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
