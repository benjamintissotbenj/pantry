package app.pantry.ui.shopping

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import app.pantry.ui.stock.AddEditItemViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun makeManualEntryViewModel(hid: MutableStateFlow<String?>): AddManualEntryViewModel {
    val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
    val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
    val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
    return AddManualEntryViewModel(household, stock, shopping)
}

private fun makePromoteItemViewModel(hid: MutableStateFlow<String?>): AddEditItemViewModel {
    val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
    val stock = mockk<StockItemRepository>(relaxed = true)
    return AddEditItemViewModel(household, stock)
}

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
        val manualVm = makeManualEntryViewModel(hid)

        compose.setContent { ShoppingListScreen(viewModel = vm, manualEntryViewModel = manualVm, promoteItemViewModel = makePromoteItemViewModel(hid)) }

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
        val manualVm = makeManualEntryViewModel(hid)
        compose.setContent { ShoppingListScreen(viewModel = vm, manualEntryViewModel = manualVm, promoteItemViewModel = makePromoteItemViewModel(hid)) }

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
        val manualVm = makeManualEntryViewModel(hid)
        compose.setContent { ShoppingListScreen(viewModel = vm, manualEntryViewModel = manualVm, promoteItemViewModel = makePromoteItemViewModel(hid)) }

        compose.onNodeWithText("Nothing to buy").assertIsDisplayed()
    }

    @Test
    fun `finish shopping bottom button is disabled when nothing is checked`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
        val stock = mockk<StockItemRepository>().also {
            coEvery { it.observe(any()) } returns flowOf(
                listOf(
                    StockItem("a", "Milk", "Dairy", StockUnit.COUNT, 0.0, 2.0, Instant.now(), 4.0),
                ),
            )
        }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
            coEvery { it.observe(any()) } returns flowOf(emptyList())
        }
        val vm = ShoppingListViewModel(household, stock, shopping)
        val manualVm = makeManualEntryViewModel(hid)
        compose.setContent { ShoppingListScreen(viewModel = vm, manualEntryViewModel = manualVm, promoteItemViewModel = makePromoteItemViewModel(hid)) }

        compose.onNodeWithTag("btn_finish_shopping").assertIsNotEnabled()
    }

    @Test
    fun `tapping finish shopping bottom button fires the VM action`() = kotlinx.coroutines.test.runTest(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also { every { it.currentHouseholdId } returns hid }
        val stock = mockk<StockItemRepository>().also {
            coEvery { it.observe(any()) } returns flowOf(
                listOf(
                    StockItem("a", "Milk", "Dairy", StockUnit.COUNT, 0.0, 2.0, Instant.now(), 4.0),
                ),
            )
        }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
            coEvery { it.observe(any()) } returns flowOf(emptyList())
            coEvery { it.finishShopping(any(), any()) } returns Result.success(
                app.pantry.data.shopping.FinishShoppingReport(1, 0, 0, emptyList())
            )
        }
        val vm = ShoppingListViewModel(household, stock, shopping)
        vm.onAutoEntryToggle("a")

        val manualVm = makeManualEntryViewModel(hid)
        compose.setContent { ShoppingListScreen(viewModel = vm, manualEntryViewModel = manualVm, promoteItemViewModel = makePromoteItemViewModel(hid)) }
        compose.onNodeWithTag("btn_finish_shopping").performClick()
        compose.onNodeWithTag("confirm_finish").performClick()

        advanceUntilIdle()

        coVerify { shopping.finishShopping(eq("HH"), match { plan ->
            plan.restocks.singleOrNull()?.itemId == "a" && plan.restocks.single().newQuantity == 4.0
        }) }
    }
}
