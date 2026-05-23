package app.pantry.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.domain.model.ShoppingEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping list") },
                actions = {
                    IconButton(
                        onClick = { overflowOpen = true },
                        modifier = Modifier.testTag("overflow"),
                    ) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Finish shopping") },
                            enabled = false, // wired in US-10
                            onClick = { overflowOpen = false },
                            modifier = Modifier.testTag("menu_finish_shopping"),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* wired in US-9 */ },
                modifier = Modifier.testTag("fab_add_manual"),
            ) { Icon(Icons.Default.Add, contentDescription = "Add manual entry") }
        },
    ) { padding ->
        if (state.isEmpty) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (state.runningLow.isNotEmpty()) {
                    item { SectionHeader("Running low") }
                    for (sub in state.runningLow) {
                        item { CategorySubHeader(sub.category) }
                        items(sub.entries, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                onCheckedChange = { checked ->
                                    if (entry.source == ShoppingEntry.Source.AUTO) {
                                        entry.linkedItemId?.let(viewModel::onAutoEntryToggle)
                                    } else {
                                        viewModel.onManualEntryToggle(entry.id, checked)
                                    }
                                },
                            )
                        }
                    }
                }
                if (state.manual.isNotEmpty()) {
                    item { SectionHeader("Added manually") }
                    for (sub in state.manual) {
                        item { CategorySubHeader(sub.category) }
                        items(sub.entries, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                onCheckedChange = { checked ->
                                    viewModel.onManualEntryToggle(entry.id, checked)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("section_$label"),
    )
}

@Composable
private fun CategorySubHeader(category: String) {
    Text(
        text = category.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("subheader_$category"),
    )
}

@Composable
private fun EntryRow(entry: ShoppingEntry, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("entry_${entry.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = entry.checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("checkbox_${entry.id}"),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (entry.checked) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (entry.source == ShoppingEntry.Source.MANUAL && entry.linkedItemId != null && entry.currentQuantity != null) {
                AssistChip(
                    onClick = {},
                    label = { Text("Linked", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
        if (entry.source == ShoppingEntry.Source.AUTO && entry.threshold != null && entry.currentQuantity != null) {
            Text(
                text = "${formatQty(entry.currentQuantity)} / ${formatQty(entry.threshold)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing to buy", style = MaterialTheme.typography.titleLarge)
    }
}

private fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
