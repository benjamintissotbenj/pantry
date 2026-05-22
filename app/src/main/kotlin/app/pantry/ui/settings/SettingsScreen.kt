package app.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val signedOut by viewModel.signedOut.collectAsState()

    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Household", style = MaterialTheme.typography.labelLarge)
        Text(state.householdName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("household_name"))
        Spacer(Modifier.height(8.dp))
        Text("Invite code", style = MaterialTheme.typography.labelLarge)
        Text(
            state.inviteCode,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.testTag("invite_code"),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::signOut,
            modifier = Modifier.testTag("settings_signout"),
        ) { Text("Sign out") }
    }
}
