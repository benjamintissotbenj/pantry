package app.pantry.ui.stock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.pantry.data.connectivity.ConnectivityRepository
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
class StockListScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    private fun fakeConnectivity() = mockk<ConnectivityRepository>().also {
        every { it.isOffline } returns MutableStateFlow(false)
    }

    private fun makeVm(items: List<StockItem>): StockListViewModel {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk { every { observe("h-1") } returns flowOf(items) }
        return StockListViewModel(ch, stock, fakeConnectivity())
    }

    private fun makeSheetVm(): AddEditItemViewModel {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk(relaxed = true)
        return AddEditItemViewModel(ch, stock, fakeConnectivity())
    }

    @Test
    fun shows_empty_state_when_no_items() {
        composeRule.setContent { StockListScreen(viewModel = makeVm(emptyList()), sheetViewModel = makeSheetVm()) }
        composeRule.onNodeWithText("No items yet — tap + to add one").assertIsDisplayed()
    }

    @Test
    fun shows_item_rows_when_present() {
        val items = listOf(
            StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0, now, null),
        )
        composeRule.setContent { StockListScreen(viewModel = makeVm(items), sheetViewModel = makeSheetVm()) }
        composeRule.onNodeWithTag("stock_row_i-1").assertIsDisplayed()
    }
}
