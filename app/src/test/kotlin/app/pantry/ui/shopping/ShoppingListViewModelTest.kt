package app.pantry.ui.shopping

import app.cash.turbine.test
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.FinishShoppingPlan
import app.pantry.data.shopping.FinishShoppingReport as DataReport
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.ShoppingEntry
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShoppingListViewModelTest {

    private val hidFlow = MutableStateFlow<String?>("HH")
    private val household = mockk<CurrentHouseholdRepository>(relaxed = true).also {
        every { it.currentHouseholdId } returns hidFlow
    }

    @BeforeEach
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun item(
        id: String,
        name: String,
        category: String = "Dairy",
        quantity: Double = 0.0,
        threshold: Double = 2.0,
        drq: Double? = 4.0,
    ) = StockItem(
        id = id,
        name = name,
        category = category,
        unit = StockUnit.COUNT,
        quantity = quantity,
        threshold = threshold,
        updatedAt = Instant.parse("2026-05-23T10:00:00Z"),
        defaultRestockQuantity = drq,
    )

    @Test
    fun `auto entries derive from items below threshold, sorted by category then name`() = runTest {
        val items = listOf(
            item("a", "Milk", category = "Dairy"),
            item("b", "Bread", category = "Bakery", threshold = 1.0, quantity = 0.5),
            item("c", "Cheese", category = "Dairy", threshold = 1.0, quantity = 0.5),
            item("d", "FullStock", quantity = 10.0), // skipped — not below threshold
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            val s = awaitItem()
            // Wait for projection to settle (initial emit may be the loading default).
            val ready = if (s.isLoading) awaitItem() else s
            assertEquals(2, ready.runningLow.size) // Bakery + Dairy
            assertEquals("Bakery", ready.runningLow[0].category)
            assertEquals(listOf("Bread"), ready.runningLow[0].entries.map { it.name })
            assertEquals("Dairy", ready.runningLow[1].category)
            assertEquals(listOf("Cheese", "Milk"), ready.runningLow[1].entries.map { it.name })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Other category sorts last`() = runTest {
        val items = listOf(
            item("a", "X", category = "Other", quantity = 0.0, threshold = 1.0),
            item("b", "Y", category = "Apples", quantity = 0.0, threshold = 1.0),
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            var s = awaitItem(); if (s.isLoading) s = awaitItem()
            assertEquals(listOf("Apples", "Other"), s.runningLow.map { it.category })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `manual entry with broken link falls back to Other`() = runTest {
        val manual = listOf(
            ShoppingEntry(
                id = "e1",
                name = "Wine",
                source = ShoppingEntry.Source.MANUAL,
                checked = false,
                createdAt = Instant.now(),
                linkedItemId = "doesNotExist",
                category = "Other",
                currentQuantity = null,
                threshold = null,
                defaultRestockQuantity = null,
            ),
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(manual) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            var s = awaitItem(); if (s.isLoading) s = awaitItem()
            assertEquals(1, s.manual.size)
            assertEquals("Other", s.manual[0].category)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `computePlan restocks checked auto entries that have a default`() {
        val vm = vmEmpty()
        val auto = listOf(
            mkEntry("auto:a", "Milk", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "a", drq = 4.0),
            mkEntry("auto:b", "Soap", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "b", drq = null),
            mkEntry("auto:c", "Bread", ShoppingEntry.Source.AUTO, checked = false, linkedItemId = "c", drq = 2.0),
        )
        val plan = vm.computePlan(auto, emptyList())
        assertEquals(1, plan.restocks.size)
        assertEquals("a", plan.restocks[0].itemId)
        assertEquals(4.0, plan.restocks[0].newQuantity)
        assertEquals(listOf("Soap"), plan.skippedNames)
        assertTrue(plan.manualEntryIdsToDelete.isEmpty())
    }

    @Test
    fun `computePlan deletes all checked manual entries and restocks linked ones`() {
        val vm = vmEmpty()
        val manual = listOf(
            mkEntry("m1", "Wine", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = "w", drq = 6.0),
            mkEntry("m2", "Random note", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = null, drq = null),
            mkEntry("m3", "Unchecked", ShoppingEntry.Source.MANUAL, checked = false, linkedItemId = null, drq = null),
        )
        val plan = vm.computePlan(emptyList(), manual)
        assertEquals(listOf("m1", "m2"), plan.manualEntryIdsToDelete)
        assertEquals(1, plan.restocks.size)
        assertEquals("w", plan.restocks[0].itemId)
    }

    @Test
    fun `computePlan dedupes restocks when auto + manual-linked target same item`() {
        val vm = vmEmpty()
        val auto = listOf(
            mkEntry("auto:x", "X", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "x", drq = 5.0),
        )
        val manual = listOf(
            mkEntry("m1", "X", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = "x", drq = 5.0),
        )
        val plan = vm.computePlan(auto, manual)
        assertEquals(1, plan.restocks.size)
        assertEquals("x", plan.restocks[0].itemId)
    }

    private fun vmEmpty(): ShoppingListViewModel {
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        return ShoppingListViewModel(household, stock, shopping)
    }

    private fun mkEntry(
        id: String,
        name: String,
        source: ShoppingEntry.Source,
        checked: Boolean,
        linkedItemId: String?,
        drq: Double?,
    ) = ShoppingEntry(
        id = id,
        name = name,
        source = source,
        checked = checked,
        createdAt = Instant.now(),
        linkedItemId = linkedItemId,
        category = "Dairy",
        currentQuantity = if (source == ShoppingEntry.Source.AUTO) 0.0 else null,
        threshold = if (source == ShoppingEntry.Source.AUTO) 1.0 else null,
        defaultRestockQuantity = drq,
    )
}
