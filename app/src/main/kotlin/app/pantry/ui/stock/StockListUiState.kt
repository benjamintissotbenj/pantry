package app.pantry.ui.stock

import app.pantry.domain.model.StockItem

data class StockListUiState(
    val isLoading: Boolean = true,
    val allItems: List<StockItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null, // null means "All"
    val errorMessage: String? = null,
) {
    val categories: List<String>
        get() = allItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val visibleItems: List<StockItem>
        get() {
            val byCategory = selectedCategory?.let { sel -> allItems.filter { it.category == sel } } ?: allItems
            val query = searchQuery.trim()
            return if (query.isEmpty()) byCategory
            else byCategory.filter { it.name.contains(query, ignoreCase = true) }
        }
}
