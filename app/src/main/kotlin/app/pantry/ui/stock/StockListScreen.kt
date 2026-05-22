package app.pantry.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.domain.model.StockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    viewModel: StockListViewModel = hiltViewModel(),
    sheetViewModel: AddEditItemViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    sheetViewModel.beginAdd(prefillCategory = state.selectedCategory.orEmpty())
                    sheetOpen = true
                },
                modifier = Modifier.testTag("stock_fab"),
            ) { Icon(Icons.Filled.Add, contentDescription = "Add item") }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingRows()
                state.visibleItems.isEmpty() -> EmptyState()
                else -> ItemList(state.visibleItems)
            }
        }
    }

    if (sheetOpen) {
        AddEditItemBottomSheet(
            viewModel = sheetViewModel,
            existingCategories = state.categories,
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun LoadingRows() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                Modifier.fillMaxWidth().height(56.dp).testTag("stock_loading_row"),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No items yet — tap + to add one", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ItemList(items: List<StockItem>) {
    LazyColumn(Modifier.fillMaxSize().testTag("stock_list")) {
        items(items, key = { it.id }) { item ->
            StockRow(item)
            HorizontalDivider()
        }
    }
}

@Composable
private fun StockRow(item: StockItem) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val qtyText = buildString {
            append(formatQuantity(item.quantity))
            if (item.unit.displaySuffix.isNotEmpty()) {
                append(' ')
                append(item.unit.displaySuffix)
            }
        }
        Text(
            "${item.name} · $qtyText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("stock_row_${item.id}"),
        )
    }
}

private fun formatQuantity(q: Double): String {
    // Render integers without trailing ".0"; decimals with up to two fractional digits.
    return if (q % 1.0 == 0.0) q.toLong().toString()
    else "%.2f".format(q).trimEnd('0').trimEnd('.', ',')
}
