package app.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
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
                    SettingsRow(modifier = Modifier.testTag("row_rename_household")) {
                        Text(state.householdName.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                SettingsSection("Invite code") {
                    SettingsRow(modifier = Modifier.testTag("row_invite_code")) {
                        Text(
                            state.inviteCode.ifEmpty { "—" },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* wired in US-10 */ },
                        modifier = Modifier.testTag("btn_regenerate_code"),
                        enabled = !state.isOffline,
                    ) { Text("Regenerate code") }
                }
                SettingsSection("Members (${state.members.size})") {
                    state.members.forEach { m ->
                        SettingsRow(modifier = Modifier.testTag("row_member_${m.uid}")) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    if (m.isYou) "${m.displayName} (you)" else m.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(m.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (state.categories.isNotEmpty()) {
                    SettingsSection("Categories") {
                        state.categories.forEach { cat ->
                            SettingsRow(modifier = Modifier.testTag("row_category_$cat")) {
                                Text(cat, style = MaterialTheme.typography.bodyLarge)
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
                    onClick = { /* wired in US-13 */ },
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
