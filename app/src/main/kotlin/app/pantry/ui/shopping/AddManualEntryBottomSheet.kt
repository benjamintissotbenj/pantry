package app.pantry.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualEntryBottomSheet(
    viewModel: AddManualEntryViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.dismissed) {
        if (state.dismissed) { viewModel.reset(); onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add to shopping list", style = MaterialTheme.typography.titleLarge)
            ExposedDropdownMenuBox(
                expanded = expanded && state.suggestions.isNotEmpty(),
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = {
                        viewModel.onQueryChange(it)
                        expanded = true
                    },
                    label = { Text("What do you need?") },
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        .testTag("field_manual_query"),
                )
                ExposedDropdownMenu(
                    expanded = expanded && state.suggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                ) {
                    state.suggestions.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                viewModel.onPickSuggestion(s)
                                expanded = false
                            },
                            modifier = Modifier.testTag("suggestion_${s.itemId}"),
                        )
                    }
                }
            }
            if (state.linkedItemName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Linked to ${state.linkedItemName}") },
                        modifier = Modifier.testTag("chip_linked"),
                    )
                    IconButton(onClick = viewModel::onUnlink, modifier = Modifier.testTag("btn_unlink")) {
                        Icon(Icons.Default.Close, contentDescription = "Unlink")
                    }
                }
            }
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().testTag("btn_manual_save"),
            ) { Text(if (state.isSubmitting) "Saving…" else "Add") }
        }
    }
}
