package app.pantry.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditItemUiState())
    val uiState: StateFlow<AddEditItemUiState> = _state.asStateFlow()

    fun beginAdd(
        prefillName: String = "",
        prefillQuantity: String = "",
        prefillCategory: String = "",
    ) {
        _state.value = AddEditItemUiState(
            mode = AddEditItemUiState.Mode.Add,
            name = prefillName,
            quantity = prefillQuantity,
            category = prefillCategory,
        )
    }

    fun beginEdit(
        itemId: String,
        name: String,
        quantity: Double,
        unit: StockUnit,
        threshold: Double,
        category: String,
        defaultRestockQuantity: Double? = null,
    ) {
        _state.value = AddEditItemUiState(
            mode = AddEditItemUiState.Mode.Edit,
            itemId = itemId,
            name = name,
            quantity = formatQuantity(quantity),
            unit = unit,
            threshold = formatQuantity(threshold),
            defaultRestockQuantity = defaultRestockQuantity?.let { formatQuantity(it) } ?: "",
            category = category,
        )
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onQuantityChange(v: String) = _state.update { it.copy(quantity = v) }
    fun onUnitChange(u: StockUnit) = _state.update { it.copy(unit = u) }
    fun onThresholdChange(v: String) = _state.update { it.copy(threshold = v) }
    fun onDefaultRestockQuantityChange(v: String) = _state.update { it.copy(defaultRestockQuantity = v) }
    fun onCategoryChange(v: String) = _state.update { it.copy(category = v) }
    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        _state.update { it.copy(isSubmitting = true, toast = null) }
        viewModelScope.launch {
            val drq = s.defaultRestockQuantity.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val result = if (s.mode == AddEditItemUiState.Mode.Add) {
                stock.create(
                    householdId = hid,
                    name = s.name.trim(),
                    category = s.category.trim(),
                    unit = s.unit,
                    quantity = s.quantity.toDoubleOrNull() ?: 0.0,
                    threshold = s.threshold.toDoubleOrNull() ?: 1.0,
                    defaultRestockQuantity = drq,
                ).map { Unit }
            } else {
                stock.update(
                    householdId = hid,
                    itemId = s.itemId ?: return@launch,
                    name = s.name.trim(),
                    category = s.category.trim(),
                    unit = s.unit,
                    quantity = s.quantity.toDoubleOrNull() ?: 0.0,
                    threshold = s.threshold.toDoubleOrNull() ?: 1.0,
                    defaultRestockQuantity = drq,
                )
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, dismissed = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to save") } },
            )
        }
    }

    fun delete() {
        val s = _state.value
        if (s.mode != AddEditItemUiState.Mode.Edit) return
        val itemId = s.itemId ?: return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            stock.delete(hid, itemId).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, dismissed = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to delete") } },
            )
        }
    }

    private fun formatQuantity(q: Double): String =
        if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
}
