package app.pantry.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.FinishShoppingPlan
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.ShoppingEntry
import app.pantry.domain.model.StockItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
    private val shopping: ShoppingEntryRepository,
) : ViewModel() {

    private val autoChecked = MutableStateFlow<Set<String>>(emptySet())
    private val skippedDialogVisible = MutableStateFlow(false)
    private val pendingReport = MutableStateFlow<FinishShoppingReport?>(null)

    val uiState: StateFlow<ShoppingListUiState> =
        currentHousehold.currentHouseholdId
            .flatMapLatest { hid ->
                if (hid == null) flowOf(ShoppingListUiState(isLoading = false))
                else combine(
                    stock.observe(hid),
                    shopping.observe(hid),
                    autoChecked,
                    pendingReport,
                    skippedDialogVisible,
                ) { items, manualEntries, checked, report, skippedVisible ->
                    project(items, manualEntries, checked, report, skippedVisible)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListUiState())

    fun onAutoEntryToggle(itemId: String) {
        autoChecked.update { s -> if (itemId in s) s - itemId else s + itemId }
    }

    fun onManualEntryToggle(entryId: String, newChecked: Boolean) {
        val hid = currentHousehold.currentHouseholdId.value ?: return
        viewModelScope.launch { shopping.setChecked(hid, entryId, newChecked) }
    }

    fun onAddManual(name: String, linkedItemId: String?) {
        val hid = currentHousehold.currentHouseholdId.value ?: return
        viewModelScope.launch { shopping.addEntry(hid, name.trim(), linkedItemId) }
    }

    fun onFinishShopping(onDone: (success: Boolean) -> Unit) {
        val hid = currentHousehold.currentHouseholdId.value ?: return onDone(false)
        val plan = buildPlanFromState() ?: return onDone(false)
        viewModelScope.launch {
            val result = shopping.finishShopping(hid, plan)
            result.onSuccess { dataReport ->
                pendingReport.value = FinishShoppingReport(
                    restockedCount = dataReport.restockedCount,
                    clearedCount = dataReport.clearedCount,
                    skippedCount = dataReport.skippedCount,
                    skippedNames = dataReport.skippedNames,
                )
                autoChecked.value = emptySet()
                onDone(true)
            }
            result.onFailure { onDone(false) }
        }
    }

    fun consumeReport() { pendingReport.value = null }
    fun showSkipped() { skippedDialogVisible.value = true }
    fun dismissSkipped() { skippedDialogVisible.value = false }

    // ------------------------------- pure helpers -------------------------------

    internal fun project(
        items: List<StockItem>,
        manualEntries: List<ShoppingEntry>,
        autoCheckedSet: Set<String>,
        report: FinishShoppingReport?,
        skippedVisible: Boolean,
    ): ShoppingListUiState {
        val itemsById = items.associateBy { it.id }
        val auto = items
            .filter { it.quantity < it.threshold }
            .map { item ->
                ShoppingEntry(
                    id = "auto:${item.id}",
                    name = item.name,
                    source = ShoppingEntry.Source.AUTO,
                    checked = item.id in autoCheckedSet,
                    createdAt = item.updatedAt,
                    linkedItemId = item.id,
                    category = item.category.ifBlank { "Other" },
                    currentQuantity = item.quantity,
                    threshold = item.threshold,
                    defaultRestockQuantity = item.defaultRestockQuantity,
                )
            }
        val resolvedManual = manualEntries.map { e ->
            val linked = e.linkedItemId?.let { itemsById[it] }
            e.copy(
                category = linked?.category?.ifBlank { "Other" } ?: "Other",
                currentQuantity = linked?.quantity,
                threshold = linked?.threshold,
                defaultRestockQuantity = linked?.defaultRestockQuantity,
            )
        }

        val plan = computePlan(auto, resolvedManual)
        return ShoppingListUiState(
            isLoading = false,
            runningLow = groupByCategory(auto),
            manual = groupByCategory(resolvedManual),
            finishShoppingPreview = FinishShoppingPreview(
                restockCount = plan.restocks.size,
                clearCount = plan.manualEntryIdsToDelete.size,
                skipCount = plan.skippedNames.size,
            ),
            pendingReport = report,
            skippedDialogVisible = skippedVisible,
        )
    }

    private fun groupByCategory(entries: List<ShoppingEntry>): List<CategorySubgroup> =
        entries
            .groupBy { it.category }
            .entries
            .map { (cat, list) ->
                CategorySubgroup(
                    category = cat,
                    entries = list.sortedBy { it.name.lowercase() },
                )
            }
            .sortedWith(
                compareBy(
                    { if (it.category.equals("Other", ignoreCase = true)) 1 else 0 },
                    { it.category.lowercase() },
                ),
            )

    private fun buildPlanFromState(): FinishShoppingPlan? {
        val s = uiState.value
        val allAuto = s.runningLow.flatMap { it.entries }
        val allManual = s.manual.flatMap { it.entries }
        return computePlan(allAuto, allManual)
    }

    /** Pure: compute the plan from the current displayed entries. */
    internal fun computePlan(
        auto: List<ShoppingEntry>,
        manual: List<ShoppingEntry>,
    ): FinishShoppingPlan {
        val restocks = mutableListOf<FinishShoppingPlan.Restock>()
        val toDelete = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        auto.filter { it.checked }.forEach { e ->
            val newQty = e.defaultRestockQuantity
            val itemId = e.linkedItemId
            if (newQty != null && itemId != null) {
                restocks += FinishShoppingPlan.Restock(itemId, newQty)
            } else {
                skipped += e.name
            }
        }
        manual.filter { it.checked }.forEach { e ->
            toDelete += e.id
            val linked = e.linkedItemId
            val newQty = e.defaultRestockQuantity
            if (linked != null && newQty != null) {
                // Manual-linked: also restock the linked catalog item.
                restocks += FinishShoppingPlan.Restock(linked, newQty)
            }
            // Manual-only or linked-without-default: just delete the entry; no restock, no skip-count bump.
        }
        return FinishShoppingPlan(
            restocks = restocks.distinctBy { it.itemId }, // collapse dup writes to same item
            manualEntryIdsToDelete = toDelete,
            skippedNames = skipped,
        )
    }
}
