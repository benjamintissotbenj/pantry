package app.pantry.ui.shopping

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders running low section with category subheader and entry`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also {
            every { it.currentHouseholdId } returns hid
        }
        val stock = mockk<StockItemRepository>().also {
            coEvery { it.observe(any()) } returns flowOf(
                listOf(
                    StockItem(
                        id = "a", name = "Milk", category = "Dairy", unit = StockUnit.COUNT,
                        quantity = 0.0, threshold = 2.0,
                        updatedAt = Instant.parse("2026-05-23T10:00:00Z"),
                        defaultRestockQuantity = 4.0,
                    ),
                ),
            )
        }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        compose.setContent { ShoppingListScreen(viewModel = vm) }

        compose.onNodeWithTag("section_Running low").assertIsDisplayed()
        compose.onNodeWithTag("subheader_Dairy").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertIsDisplayed()
    }

    @Test
    fun `checked manual entry is displayed`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
            coEvery { it.observe(any()) } returns flowOf(
                listOf(
                    app.pantry.domain.model.ShoppingEntry(
                        id = "e1", name = "Wine",
                        source = app.pantry.domain.model.ShoppingEntry.Source.MANUAL,
                        checked = true, createdAt = Instant.now(), linkedItemId = null,
                        category = "Other", currentQuantity = null, threshold = null, defaultRestockQuantity = null,
                    ),
                ),
            )
        }
        val vm = ShoppingListViewModel(household, stock, shopping)
        compose.setContent { ShoppingListScreen(viewModel = vm) }

        // Compose has no direct LineThrough matcher — verify the entry's row tag renders, which is the
        // wire we care about (the strike-through is a visual decoration we trust the Composable applies).
        compose.onNodeWithTag("entry_e1").assertIsDisplayed()
    }

    @Test
    fun `empty state shows when no items or manual entries`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)
        compose.setContent { ShoppingListScreen(viewModel = vm) }

        compose.onNodeWithText("Nothing to buy").assertIsDisplayed()
    }
}
