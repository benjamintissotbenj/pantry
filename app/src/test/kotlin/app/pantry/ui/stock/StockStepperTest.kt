package app.pantry.ui.stock

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StockStepperTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun plus_button_invokes_adjustQuantity() {
        val items = listOf(StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.0, 1.0, now))
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk(relaxed = true) {
            every { observe("h-1") } returns flowOf(items)
        }
        coEvery { stock.adjustQuantity("h-1", "i-1", 1.0) } returns Result.success(Unit)
        val sheetVm: AddEditItemViewModel = mockk(relaxed = true) {
            every { uiState } returns MutableStateFlow(AddEditItemUiState())
        }
        composeRule.setContent {
            StockListScreen(
                viewModel = StockListViewModel(ch, stock),
                sheetViewModel = sheetVm,
            )
        }
        composeRule.onNodeWithTag("stock_plus_i-1").performClick()
        coVerify { stock.adjustQuantity("h-1", "i-1", 1.0) }
    }
}
