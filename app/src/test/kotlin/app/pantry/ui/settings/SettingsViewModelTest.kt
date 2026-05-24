package app.pantry.ui.settings

import app.pantry.data.auth.AuthRepository
import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.MemberSummary
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private val auth = mockk<AuthRepository>()
    private val households = mockk<HouseholdRepository>(relaxed = true)
    private val stock = mockk<StockItemRepository>()
    private val connectivity = mockk<ConnectivityRepository>()
    private val currentHousehold = mockk<CurrentHouseholdRepository>()

    private val defaultHousehold = Household(
        id = "h1",
        name = "TestHome",
        memberUids = listOf("u1", "u2"),
        inviteCode = "ABC123",
        createdBy = "u1",
        members = mapOf(
            "u1" to MemberSummary("Ben", "ben@example.com"),
            "u2" to MemberSummary("Alice", "alice@example.com"),
        ),
    )

    private fun item(name: String, category: String) =
        StockItem("id-$name", name, category, StockUnit.COUNT, 1.0, 0.5, now, null)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { auth.currentUser } returns MutableStateFlow(UserProfile("u1", "Ben", "ben@example.com"))
        every { currentHousehold.currentHouseholdId } returns MutableStateFlow("h1")
        every { households.observe("h1") } returns flowOf(defaultHousehold)
        every { stock.observe("h1") } returns flowOf(emptyList())
        every { connectivity.isOffline } returns MutableStateFlow(false)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun makeVm() = SettingsViewModel(auth, households, stock, connectivity, currentHousehold)

    // ── State derivation ─────────────────────────────────────────────────────

    @Test
    fun `exposes household name and invite code`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        assertEquals("TestHome", vm.uiState.value.householdName)
        assertEquals("ABC123", vm.uiState.value.inviteCode)
    }

    @Test
    fun `creator sees canRemove on other members but not self`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        val rows = vm.uiState.value.members
        val self = rows.first { it.uid == "u1" }
        val other = rows.first { it.uid == "u2" }
        assertTrue(vm.uiState.value.isCreator, "u1 should be creator")
        assertFalse(self.canRemove, "creator cannot remove themselves")
        assertTrue(other.canRemove, "creator can remove other member")
        assertTrue(self.isYou, "self row should have isYou=true")
        assertFalse(other.isYou, "other row should have isYou=false")
    }

    @Test
    fun `non-creator sees canRemove false on every row`() = runTest {
        // Sign in as u2, who is not the creator
        every { auth.currentUser } returns MutableStateFlow(UserProfile("u2", "Alice", "alice@example.com"))
        val vm = makeVm()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isCreator, "u2 is not creator")
        vm.uiState.value.members.forEach { row ->
            assertFalse(row.canRemove, "non-creator: canRemove should be false for ${row.uid}")
        }
    }

    @Test
    fun `categories list is distinct and sorted`() = runTest {
        every { stock.observe("h1") } returns flowOf(
            listOf(
                item("Milk", "Fridge"),
                item("Cheese", "fridge"),  // should deduplicate when distinct (different case but same normalized)
                item("Bread", "Pantry"),
                item("Beer", ""),          // blank — excluded
                item("Eggs", "Fridge"),    // duplicate — deduplicated
            )
        )
        val vm = makeVm()
        advanceUntilIdle()
        // distinct() preserves first occurrence; "Fridge" comes before "fridge" before "Pantry"
        // After distinct: ["Fridge", "fridge", "Pantry"], sorted by lowercase: Fridge/fridge tie then Pantry
        val cats = vm.uiState.value.categories
        // all non-blank, no exact duplicates
        assertTrue(cats.none { it.isBlank() })
        assertEquals(cats.size, cats.distinct().size)
        // sorted by lowercase
        assertEquals(cats, cats.sortedBy { it.lowercase() })
    }

    @Test
    fun `isOffline propagated from ConnectivityRepository`() = runTest {
        val offlineFlow = MutableStateFlow(false)
        every { connectivity.isOffline } returns offlineFlow
        val vm = makeVm()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isOffline)
        offlineFlow.value = true
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isOffline)
    }

    // ── Event actions ─────────────────────────────────────────────────────────

    @Test
    fun `onCopyCodeRequested pushes clipboard and snackbar events`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        vm.onCopyCodeRequested()
        val state = vm.uiState.value
        assertEquals("ABC123", state.pendingClipboard)
        assertEquals("Invite code copied", state.pendingSnackbar)
    }

    @Test
    fun `consumeClipboard clears clipboard event`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        vm.onCopyCodeRequested()
        vm.consumeClipboard()
        assertNull(vm.uiState.value.pendingClipboard)
    }

    @Test
    fun `onShareCodeRequested pushes share code event`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        vm.onShareCodeRequested()
        assertEquals("ABC123", vm.uiState.value.pendingShareCode)
    }

    @Test
    fun `consumeShareCode clears share code event`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        vm.onShareCodeRequested()
        vm.consumeShareCode()
        assertNull(vm.uiState.value.pendingShareCode)
    }

    // ── Rename household ─────────────────────────────────────────────────────

    @Test
    fun `onRenameHousehold calls repo with trimmed name`() = runTest {
        coEvery { households.rename("h1", "New Name") } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRenameHousehold("  New Name  ")
        advanceUntilIdle()
        assertEquals("Household renamed", vm.uiState.value.pendingSnackbar)
    }

    @Test
    fun `onRenameHousehold with blank name is a no-op`() = runTest {
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRenameHousehold("   ")
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingSnackbar)
    }

    // ── Regenerate code ───────────────────────────────────────────────────────

    @Test
    fun `onRegenerateCode calls repo and surfaces snackbar`() = runTest {
        coEvery { households.regenerateInviteCode("h1") } returns Result.success("NEW999")
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRegenerateCode()
        advanceUntilIdle()
        assertEquals("Code changed", vm.uiState.value.pendingSnackbar)
    }

    @Test
    fun `onRegenerateCode on failure surfaces error snackbar`() = runTest {
        coEvery { households.regenerateInviteCode("h1") } returns Result.failure(RuntimeException("network"))
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRegenerateCode()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingSnackbar)
    }

    // ── Leave household ───────────────────────────────────────────────────────

    @Test
    fun `onLeaveHousehold success sets pendingPostLeaveNav`() = runTest {
        coEvery { households.leaveHousehold("h1") } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.onLeaveHousehold()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.pendingPostLeaveNav)
    }

    @Test
    fun `onLeaveHousehold failure shows snackbar`() = runTest {
        coEvery { households.leaveHousehold("h1") } returns Result.failure(RuntimeException("denied"))
        val vm = makeVm()
        advanceUntilIdle()
        vm.onLeaveHousehold()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.pendingPostLeaveNav)
        assertNotNull(vm.uiState.value.pendingSnackbar)
    }

    @Test
    fun `consumeNav clears pendingPostLeaveNav`() = runTest {
        coEvery { households.leaveHousehold("h1") } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.onLeaveHousehold()
        advanceUntilIdle()
        vm.consumeNav()
        assertFalse(vm.uiState.value.pendingPostLeaveNav)
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    @Test
    fun `onSignOut sets signedOut on success`() = runTest {
        coEvery { auth.signOut() } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.onSignOut()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.signedOut)
    }

    @Test
    fun `onSignOut does not set signedOut on failure`() = runTest {
        coEvery { auth.signOut() } returns Result.failure(RuntimeException("network"))
        val vm = makeVm()
        advanceUntilIdle()
        vm.onSignOut()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.signedOut)
        assertNotNull(vm.uiState.value.pendingSnackbar)
    }

    // ── onRemoveMember ────────────────────────────────────────────────────────

    @Test
    fun `onRemoveMember success surfaces snackbar`() = runTest {
        coEvery { households.removeMember("h1", "u2") } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRemoveMember("u2")
        advanceUntilIdle()
        assertEquals("Member removed", vm.uiState.value.pendingSnackbar)
    }

    @Test
    fun `onRemoveMember failure surfaces error snackbar`() = runTest {
        coEvery { households.removeMember("h1", "u2") } returns Result.failure(RuntimeException("nope"))
        val vm = makeVm()
        advanceUntilIdle()
        vm.onRemoveMember("u2")
        advanceUntilIdle()
        assertEquals("nope", vm.uiState.value.pendingSnackbar)
    }

    // ── Backwards-compat shim ─────────────────────────────────────────────────

    @Test
    fun `signOut shim delegates to onSignOut`() = runTest {
        coEvery { auth.signOut() } returns Result.success(Unit)
        val vm = makeVm()
        advanceUntilIdle()
        vm.signOut()
        advanceUntilIdle()
        assertTrue(vm.signedOut.value)
    }
}
