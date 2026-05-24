package app.pantry.ui.stock

import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StockListViewModelTest {

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    private fun item(name: String, category: String): StockItem =
        StockItem("id-$name", name, category, StockUnit.COUNT, 1.0, 1.0, now, null)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun fakeConnectivity() = mockk<ConnectivityRepository>().also {
        every { it.isOffline } returns MutableStateFlow(false)
    }

    private fun makeVm(items: List<StockItem>, hid: String? = "h-1"): StockListViewModel {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow(hid) }
        val stock: StockItemRepository = mockk { every { observe(any()) } returns flowOf(items) }
        return StockListViewModel(ch, stock, fakeConnectivity())
    }

    @Test
    fun `surfaces items from repository`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Bread", "Pantry")))
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.allItems.size)
    }

    @Test
    fun `categories are distinct sorted non-blank`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Cheese", "Fridge"), item("Bread", "Pantry"), item("Beer", "")))
        advanceUntilIdle()
        assertEquals(listOf("Fridge", "Pantry"), vm.uiState.value.categories)
    }

    @Test
    fun `selectedCategory filter applies`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Bread", "Pantry")))
        advanceUntilIdle()
        vm.onCategorySelect("Fridge")
        assertEquals(listOf("Milk"), vm.uiState.value.visibleItems.map { it.name })
    }

    @Test
    fun `search filter applies and is case-insensitive`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Mango", "Fruit"), item("Bread", "Pantry")))
        advanceUntilIdle()
        vm.onSearchChange("m")
        val names = vm.uiState.value.visibleItems.map { it.name }
        assertEquals(setOf("Milk", "Mango"), names.toSet())
    }

    @Test
    fun `emits error message when observe flow throws`() = runTest {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk {
            every { observe("h-1") } returns flow { throw RuntimeException("permission") }
        }
        val vm = StockListViewModel(ch, stock, fakeConnectivity())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage?.contains("permission") == true)
    }
}
