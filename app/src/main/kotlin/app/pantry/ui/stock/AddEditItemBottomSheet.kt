package app.pantry.ui.stock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pantry.domain.model.StockUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemBottomSheet(
    viewModel: AddEditItemViewModel,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.dismissed) { if (state.dismissed) onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (state.mode == AddEditItemUiState.Mode.Add) "Add item" else "Edit item",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (state.mode == AddEditItemUiState.Mode.Edit) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.testTag("item_delete"),
                    ) { Text("Delete") }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("field_name"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = viewModel::onQuantityChange,
                    label = { Text("Quantity") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(2f).testTag("field_quantity"),
                )
                UnitDropdown(
                    selected = state.unit,
                    onSelect = viewModel::onUnitChange,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.threshold,
                onValueChange = viewModel::onThresholdChange,
                label = { Text("Low-stock threshold") },
                supportingText = { Text("Below this, the item shows on the shopping list") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("field_threshold"),
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingCategories.forEach { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { viewModel.onCategoryChange(cat) },
                        label = { Text(cat) },
                    )
                }
                AssistChip(
                    onClick = { viewModel.onCategoryChange("") },
                    label = { Text("+ New") },
                    modifier = Modifier.testTag("category_new"),
                )
            }
            if (state.category.isEmpty() || state.category !in existingCategories) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = viewModel::onCategoryChange,
                    label = { Text("New category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_category"),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().testTag("item_save"),
            ) { Text(if (state.isSubmitting) "Saving…" else "Save") }
        }
    }

    state.toast?.let { msg ->
        LaunchedEffect(msg) {
            // Surfaced via parent snackbar; consume so we don't fire repeatedly.
            viewModel.consumeToast()
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.name}?") },
            text = { Text("This removes the item from the catalog.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; viewModel.delete() },
                    modifier = Modifier.testTag("delete_confirm"),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selected: StockUnit,
    onSelect: (StockUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected.storageKey.ifEmpty { "count" },
            onValueChange = {},
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("field_unit"),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StockUnit.entries.forEach { u ->
                DropdownMenuItem(
                    text = { Text(if (u == StockUnit.COUNT) "count" else u.storageKey) },
                    onClick = { onSelect(u); expanded = false },
                )
            }
        }
    }
}
