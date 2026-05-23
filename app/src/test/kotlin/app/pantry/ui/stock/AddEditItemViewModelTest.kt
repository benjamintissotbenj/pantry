package app.pantry.ui.stock

import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditItemViewModelTest {

    private val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
    private val stock: StockItemRepository = mockk(relaxed = true)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `add path submits and dismisses on success`() = runTest {
        coEvery { stock.create("h-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0) } returns
            Result.success(StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0, Instant.now(), null))
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onNameChange("Milk")
        vm.onCategoryChange("Fridge")
        vm.onUnitChange(StockUnit.LITER)
        vm.onQuantityChange("1.5")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.dismissed)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `add path surfaces toast on failure`() = runTest {
        coEvery { stock.create(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("network"))
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onNameChange("Milk")
        vm.onQuantityChange("1")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        assertEquals("network", vm.uiState.value.toast)
        assertFalse(vm.uiState.value.dismissed)
    }

    @Test
    fun `submit no-ops when name blank`() = runTest {
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onQuantityChange("1")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 0) { stock.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `edit path submits update and dismisses`() = runTest {
        coEvery { stock.update("h-1", "i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0) } returns
            Result.success(Unit)
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginEdit(
            itemId = "i-1",
            name = "Milk",
            quantity = 1.5,
            unit = StockUnit.LITER,
            threshold = 1.0,
            category = "Fridge",
        )
        vm.onQuantityChange("0.5")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.dismissed)
    }

    @Test
    fun `delete in edit mode calls repository`() = runTest {
        coEvery { stock.delete("h-1", "i-1") } returns Result.success(Unit)
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginEdit(
            itemId = "i-1",
            name = "Milk",
            quantity = 1.5,
            unit = StockUnit.LITER,
            threshold = 1.0,
            category = "Fridge",
        )
        vm.delete()
        advanceUntilIdle()
        coVerify { stock.delete("h-1", "i-1") }
        assertTrue(vm.uiState.value.dismissed)
    }

    @Test
    fun `delete in add mode is a no-op`() = runTest {
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.delete()
        advanceUntilIdle()
        coVerify(exactly = 0) { stock.delete(any(), any()) }
    }
}
