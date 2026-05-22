package app.pantry.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StockListViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockListUiState())
    val uiState: StateFlow<StockListUiState> = _state.asStateFlow()

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            currentHousehold.currentHouseholdId
                .flatMapLatest { hid ->
                    if (hid == null) flowOf(emptyList())
                    else stock.observe(hid)
                }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load stock") }
                }
                .collectLatest { items ->
                    _state.update { it.copy(isLoading = false, allItems = items, errorMessage = null) }
                }
        }
    }

    fun onSearchChange(query: String) = _state.update { it.copy(searchQuery = query) }
    fun onCategorySelect(category: String?) = _state.update { it.copy(selectedCategory = category) }
    fun onErrorShown() = _state.update { it.copy(errorMessage = null) }

    fun onIncrement(item: StockItem) = adjust(item, delta = 1.0)
    fun onDecrement(item: StockItem) = adjust(item, delta = -1.0)

    private fun adjust(item: StockItem, delta: Double) {
        val hid = currentHousehold.currentHouseholdId.value ?: return
        // Optimistic local update: replace the matching item with a new qty
        _state.update { current ->
            val next = current.allItems.map {
                if (it.id == item.id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(0.0)) else it
            }
            current.copy(allItems = next)
        }
        viewModelScope.launch {
            stock.adjustQuantity(hid, item.id, delta).onFailure { e ->
                // Revert
                _state.update { current ->
                    val reverted = current.allItems.map {
                        if (it.id == item.id) it.copy(quantity = (it.quantity - delta).coerceAtLeast(0.0)) else it
                    }
                    current.copy(
                        allItems = reverted,
                        errorMessage = e.message ?: "Failed to update quantity",
                    )
                }
            }
        }
    }
}
