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
    private val boughtQuantities = MutableStateFlow<Map<String, String>>(emptyMap())
    private val pendingPromotions = MutableStateFlow<List<PendingPromotion>>(emptyList())

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
                    boughtQuantities,
                    pendingPromotions,
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    project(
                        items = values[0] as List<StockItem>,
                        manualEntries = values[1] as List<ShoppingEntry>,
                        autoCheckedSet = values[2] as Set<String>,
                        report = values[3] as FinishShoppingReport?,
                        skippedVisible = values[4] as Boolean,
                        boughtQuantitiesMap = values[5] as Map<String, String>,
                        pendingPromos = values[6] as List<PendingPromotion>,
                    )
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

    fun onQuantityChange(entryId: String, value: String) {
        boughtQuantities.update { it + (entryId to value) }
    }

    fun completePromotion() {
        pendingPromotions.update { it.drop(1) }
    }

    fun onFinishShopping(onDone: (success: Boolean) -> Unit) {
        val hid = currentHousehold.currentHouseholdId.value ?: return onDone(false)
        val plan = buildPlanFromState() ?: return onDone(false)
        // Capture promotions BEFORE the entries are deleted by the batch.
        val s = uiState.value
        val allManual = s.manual.flatMap { it.entries }
        val promotions = allManual
            .filter { it.checked && it.linkedItemId == null }
            .mapNotNull { e ->
                val typed = s.boughtQuantities[e.id]?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                if (typed != null) PendingPromotion(name = e.name, quantity = typed) else null
            }
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
                boughtQuantities.value = emptyMap()
                pendingPromotions.value = promotions
                onDone(true)
            }
            result.onFailure { onDone(false) }
        }
    }

    fun consumeReport() { pendingReport.value = null }
    fun showSkipped() { skippedDialogVisible.value = true }
    fun dismissSkipped() {
        skippedDialogVisible.value = false
        pendingReport.value = null
    }

    // ------------------------------- pure helpers -------------------------------

    internal fun project(
        items: List<StockItem>,
        manualEntries: List<ShoppingEntry>,
        autoCheckedSet: Set<String>,
        report: FinishShoppingReport?,
        skippedVisible: Boolean,
        boughtQuantitiesMap: Map<String, String> = emptyMap(),
        pendingPromos: List<PendingPromotion> = emptyList(),
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

        val plan = computePlan(auto, resolvedManual, boughtQuantitiesMap)
        val canFinish = !hasBlockingMissingQty(auto, resolvedManual, boughtQuantitiesMap)
        val categories = items
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }

        return ShoppingListUiState(
            isLoading = false,
            runningLow = groupByCategory(auto),
            manual = groupByCategory(resolvedManual),
            boughtQuantities = boughtQuantitiesMap,
            finishShoppingPreview = FinishShoppingPreview(
                restockCount = plan.restocks.size,
                clearCount = plan.manualEntryIdsToDelete.size,
                skipCount = plan.skippedNames.size,
            ),
            canFinish = canFinish,
            pendingReport = report,
            skippedDialogVisible = skippedVisible,
            pendingPromotion = pendingPromos.firstOrNull(),
            existingCategories = categories,
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
        return computePlan(allAuto, allManual, s.boughtQuantities)
    }

    /** Pure: compute the plan from the current displayed entries + typed quantities. */
    internal fun computePlan(
        auto: List<ShoppingEntry>,
        manual: List<ShoppingEntry>,
        bought: Map<String, String> = emptyMap(),
    ): FinishShoppingPlan {
        val restocks = mutableListOf<FinishShoppingPlan.Restock>()
        val toDelete = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        auto.filter { it.checked }.forEach { e ->
            val typed = bought[e.id]?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val effective = typed ?: e.defaultRestockQuantity
            val itemId = e.linkedItemId
            if (effective != null && itemId != null) {
                restocks += FinishShoppingPlan.Restock(itemId, effective)
            } else {
                skipped += e.name
            }
        }
        manual.filter { it.checked }.forEach { e ->
            toDelete += e.id
            val linked = e.linkedItemId
            val typed = bought[e.id]?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val effective = typed ?: e.defaultRestockQuantity
            if (linked != null && effective != null) {
                restocks += FinishShoppingPlan.Restock(linked, effective)
            }
        }
        return FinishShoppingPlan(
            restocks = restocks.distinctBy { it.itemId },
            manualEntryIdsToDelete = toDelete,
            skippedNames = skipped,
        )
    }

    /** True if any checked AUTO or MANUAL-linked entry has neither a typed qty nor a defaultRestockQuantity. */
    private fun hasBlockingMissingQty(
        auto: List<ShoppingEntry>,
        manual: List<ShoppingEntry>,
        bought: Map<String, String>,
    ): Boolean {
        fun missing(e: ShoppingEntry): Boolean {
            val typed = bought[e.id]?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            return typed == null && e.defaultRestockQuantity == null
        }
        val autoBlocked = auto.any { it.checked && missing(it) }
        val manualLinkedBlocked = manual.any { it.checked && it.linkedItemId != null && missing(it) }
        return autoBlocked || manualLinkedBlocked
    }
}
