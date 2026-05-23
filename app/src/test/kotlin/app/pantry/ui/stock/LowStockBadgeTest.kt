package app.pantry.ui.stock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
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
class LowStockBadgeTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun shows_low_stock_badge_when_below_threshold() {
        // quantity 0.5, threshold 1.0 → low and non-zero → badge expected
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0, now, null)
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk { every { observe("h-1") } returns flowOf(listOf(item)) }
        val sheetVm: AddEditItemViewModel = mockk(relaxed = true) {
            every { uiState } returns MutableStateFlow(AddEditItemUiState())
        }
        composeRule.setContent {
            StockListScreen(
                viewModel = StockListViewModel(ch, stock),
                sheetViewModel = sheetVm,
            )
        }
        composeRule.onNodeWithTag("low_stock_badge_i-1").assertIsDisplayed()
    }

    @Test
    fun shows_low_stock_badge_when_quantity_is_zero() {
        // quantity 0, threshold 1.0 → still below threshold → badge expected
        val item = StockItem("i-2", "Milk", "Fridge", StockUnit.LITER, 0.0, 1.0, now, null)
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk { every { observe("h-1") } returns flowOf(listOf(item)) }
        val sheetVm: AddEditItemViewModel = mockk(relaxed = true) {
            every { uiState } returns MutableStateFlow(AddEditItemUiState())
        }
        composeRule.setContent {
            StockListScreen(
                viewModel = StockListViewModel(ch, stock),
                sheetViewModel = sheetVm,
            )
        }
        composeRule.onNodeWithTag("low_stock_badge_i-2").assertIsDisplayed()
    }
}
