package app.pantry.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.ui.common.OfflineBanner

@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    onLeft: () -> Unit = onSignedOut,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var renameHouseholdOpen by remember { mutableStateOf(false) }
    var regenerateConfirmOpen by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<MemberRow?>(null) }
    var leaveOpen by remember { mutableStateOf(false) }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) { onSignedOut(); viewModel.consumeSignedOut() }
    }
    LaunchedEffect(state.pendingPostLeaveNav) {
        if (!state.pendingPostLeaveNav) return@LaunchedEffect
        onLeft()
        viewModel.consumeNav()
    }
    LaunchedEffect(state.pendingSnackbar) {
        val msg = state.pendingSnackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }
    LaunchedEffect(state.pendingClipboard) {
        state.pendingClipboard?.let { code ->
            clipboardManager.setText(AnnotatedString(code))
            viewModel.consumeClipboard()
        }
    }
    LaunchedEffect(state.pendingShareCode) {
        state.pendingShareCode?.let { code ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join my Pantry household with code: $code")
            }
            context.startActivity(Intent.createChooser(intent, "Share invite code"))
            viewModel.consumeShareCode()
        }
    }

    if (regenerateConfirmOpen) {
        AlertDialog(
            onDismissRequest = { regenerateConfirmOpen = false },
            title = { Text("Generate a new invite code?") },
            text = { Text("Anyone holding the current code won't be able to join after this.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        regenerateConfirmOpen = false
                        viewModel.onRegenerateCode()
                    },
                    modifier = Modifier.testTag("btn_regenerate_confirm"),
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { regenerateConfirmOpen = false }) { Text("Cancel") }
            },
        )
    }

    if (renameHouseholdOpen) {
        var input by rememberSaveable { mutableStateOf(state.householdName) }
        AlertDialog(
            onDismissRequest = { renameHouseholdOpen = false },
            title = { Text("Rename household") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_household_name"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameHouseholdOpen = false
                        viewModel.onRenameHousehold(input)
                    },
                    enabled = input.trim().isNotEmpty() && input.trim() != state.householdName,
                    modifier = Modifier.testTag("btn_rename_save"),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameHouseholdOpen = false }) { Text("Cancel") }
            },
        )
    }

    memberToRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove ${m.displayName}?") },
            text = { Text("They'll lose access to the household.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onRemoveMember(m.uid); memberToRemove = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_remove_confirm"),
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } },
        )
    }

    categoryToRename?.let { old ->
        var input by rememberSaveable(key = old) { mutableStateOf(old) }
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text("Rename category") },
            text = {
                Column {
                    Text("All items in $old will move to the new name.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("field_category_name"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = input.trim()
                        categoryToRename = null
                        viewModel.onRenameCategory(old, newName)
                    },
                    enabled = input.trim().isNotEmpty() && input.trim() != old,
                    modifier = Modifier.testTag("btn_category_save"),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { categoryToRename = null }) { Text("Cancel") } },
        )
    }

    if (leaveOpen) {
        val isLastMember = state.members.size == 1
        AlertDialog(
            onDismissRequest = { leaveOpen = false },
            title = { Text("Leave ${state.householdName}?") },
            text = {
                Column {
                    Text("You'll lose access to the shared stock.")
                    if (isLastMember) {
                        Spacer(Modifier.height(8.dp))
                        Text("The household and its stock will be deleted.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { leaveOpen = false; viewModel.onLeaveHousehold() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_leave_confirm"),
                ) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { leaveOpen = false }) { Text("Cancel") } },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            OfflineBanner(isOffline = state.isOffline)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection("Household") {
                    SettingsRow(
                        modifier = Modifier
                            .testTag("row_rename_household")
                            .clickable(enabled = !state.isOffline) { renameHouseholdOpen = true },
                    ) {
                        Text(state.householdName.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                SettingsSection("Invite code") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("row_invite_code")
                            .clickable(enabled = !state.isOffline) { viewModel.onCopyCodeRequested() },
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(Modifier.width(12.dp))
                        Text(
                            state.inviteCode.ifEmpty { "—" },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { viewModel.onShareCodeRequested() },
                            enabled = !state.isOffline,
                            modifier = Modifier.testTag("btn_share_code"),
                        ) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { regenerateConfirmOpen = true },
                        enabled = !state.isOffline,
                        modifier = Modifier.testTag("btn_regenerate_code"),
                    ) { Text("Regenerate code") }
                }
                SettingsSection("Members (${state.members.size})") {
                    state.members.forEach { m ->
                        SettingsRow(modifier = Modifier.testTag("row_member_${m.uid}")) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (m.isYou) "${m.displayName} (you)" else m.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(m.email, style = MaterialTheme.typography.bodySmall)
                                }
                                if (m.canRemove) {
                                    IconButton(
                                        onClick = { memberToRemove = m },
                                        enabled = !state.isOffline,
                                        modifier = Modifier.testTag("btn_remove_${m.uid}"),
                                    ) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                                }
                            }
                        }
                    }
                }
                if (state.categories.isNotEmpty()) {
                    SettingsSection("Categories") {
                        state.categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .testTag("row_category_$cat"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(cat, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { categoryToRename = cat },
                                    enabled = !state.isOffline,
                                    modifier = Modifier.testTag("btn_rename_category_$cat"),
                                ) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
                            }
                        }
                    }
                }
                SettingsSection("About") {
                    SettingsRow {
                        Text(
                            "Pantry · v${state.appVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("text_app_version"),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::onSignOut,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth().testTag("settings_signout"),
                ) { Text("Sign out") }
                Button(
                    onClick = { leaveOpen = true },
                    enabled = !state.isOffline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("btn_leave_household"),
                ) { Text("Leave household") }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        content()
    }
}
