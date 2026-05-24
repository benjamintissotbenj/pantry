package app.pantry.ui.shopping

import app.cash.turbine.test
import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddManualEntryViewModelTest {

    private val hidFlow = MutableStateFlow<String?>("HH")
    private val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hidFlow }
    private val connectivity = mockk<ConnectivityRepository>().also { every { it.isOffline } returns MutableStateFlow(false) }

    @BeforeEach
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `typing filters suggestions case-insensitively, capped at 8`() = runTest {
        val items = (1..12).map {
            StockItem("id$it", "Milk$it", "Dairy", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null)
        }
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping, connectivity)
        vm.onQueryChange("milk")

        vm.uiState.test {
            var s = awaitItem(); while (s.suggestions.isEmpty()) s = awaitItem()
            assertEquals(8, s.suggestions.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `picking a suggestion links the entry`() = runTest {
        val items = listOf(StockItem("a", "Wine", "Other", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null))
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
            coEvery { it.addEntry(any(), any(), any()) } returns Result.success(Unit)
        }
        val vm = AddManualEntryViewModel(household, stock, shopping, connectivity)
        vm.onPickSuggestion(AddManualEntryUiState.Suggestion("a", "Wine"))
        vm.submit()

        coVerify { shopping.addEntry("HH", "Wine", "a") }
    }

    @Test
    fun `submit with no link passes null itemId`() = runTest {
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
            coEvery { it.addEntry(any(), any(), any()) } returns Result.success(Unit)
        }
        val vm = AddManualEntryViewModel(household, stock, shopping, connectivity)
        vm.onQueryChange("Champagne")
        vm.submit()

        coVerify { shopping.addEntry("HH", "Champagne", null) }
    }

    @Test
    fun `onUnlink clears the link`() = runTest {
        val items = listOf(StockItem("a", "Wine", "Other", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null))
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping, connectivity)

        vm.onPickSuggestion(AddManualEntryUiState.Suggestion("a", "Wine"))
        vm.onUnlink()

        vm.uiState.test {
            var s = awaitItem(); while (s.linkedItemId != null) s = awaitItem()
            assertNull(s.linkedItemId)
            assertNull(s.linkedItemName)
            // Query stays as "Wine" — onUnlink should only clear the link, not the text the user already has.
            assertEquals("Wine", s.query)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `editing query after pick clears the link`() = runTest {
        val items = listOf(StockItem("a", "Wine", "Other", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null))
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping, connectivity)
        vm.onPickSuggestion(AddManualEntryUiState.Suggestion("a", "Wine"))
        vm.onQueryChange("Wine for guests")

        vm.uiState.test {
            var s = awaitItem(); while (s.query != "Wine for guests") s = awaitItem()
            assertNull(s.linkedItemId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
