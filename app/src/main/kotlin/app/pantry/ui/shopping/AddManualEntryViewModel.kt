package app.pantry.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddManualEntryViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
    private val shopping: ShoppingEntryRepository,
) : ViewModel() {

    private val internalState = MutableStateFlow(AddManualEntryUiState())

    val uiState: StateFlow<AddManualEntryUiState> =
        combine(
            internalState,
            currentHousehold.currentHouseholdId.flatMapLatest { hid ->
                if (hid == null) flowOf(emptyList()) else stock.observe(hid)
            },
        ) { state, items ->
            val q = state.query.trim()
            val suggestions = if (q.isBlank()) emptyList() else
                items.asSequence()
                    .filter { it.name.contains(q, ignoreCase = true) }
                    .take(8)
                    .map { AddManualEntryUiState.Suggestion(it.id, it.name) }
                    .toList()
            state.copy(suggestions = suggestions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddManualEntryUiState())

    fun onQueryChange(q: String) {
        internalState.update {
            // Typing in the field clears the link (the user is editing the text away from the picked item).
            if (it.linkedItemName != null && q != it.linkedItemName) {
                it.copy(query = q, linkedItemId = null, linkedItemName = null)
            } else {
                it.copy(query = q)
            }
        }
    }

    fun onPickSuggestion(s: AddManualEntryUiState.Suggestion) {
        internalState.update {
            it.copy(query = s.name, linkedItemId = s.itemId, linkedItemName = s.name)
        }
    }

    fun onUnlink() {
        internalState.update { it.copy(linkedItemId = null, linkedItemName = null) }
    }

    fun submit() {
        val s = internalState.value
        if (!s.canSubmit) return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        internalState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            shopping.addEntry(hid, s.query.trim(), s.linkedItemId).fold(
                onSuccess = { internalState.update { AddManualEntryUiState(dismissed = true) } },
                onFailure = { internalState.update { it.copy(isSubmitting = false) } },
            )
        }
    }

    fun reset() { internalState.value = AddManualEntryUiState() }
}
