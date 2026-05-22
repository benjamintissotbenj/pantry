package app.pantry.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
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
}
