package app.pantry.ui.household

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HouseholdOnboardingScreen(
    onCreated: () -> Unit,
    onJoined: () -> Unit,
    viewModel: HouseholdOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) {
            val navigatingFrom = state.mode
            viewModel.consumeNavigation()
            // For both create and join, "navigateToHome" is the signal — caller decides where to go.
            if (navigatingFrom == HouseholdOnboardingUiState.Mode.Create) onCreated() else onJoined()
        }
    }
    LaunchedEffect(state.toast) {
        state.toast?.let { snackbar.showSnackbar(it); viewModel.consumeToast() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Set up your household", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                HouseholdOnboardingUiState.Mode.Welcome -> {
                    Button(
                        onClick = { viewModel.switchMode(HouseholdOnboardingUiState.Mode.Create) },
                        modifier = Modifier.fillMaxWidth().testTag("create_btn"),
                    ) { Text("Create a household") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.switchMode(HouseholdOnboardingUiState.Mode.Join) },
                        modifier = Modifier.testTag("join_btn"),
                    ) { Text("Join with invite code") }
                }
                HouseholdOnboardingUiState.Mode.Create -> {
                    OutlinedTextField(
                        value = state.householdName,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Household name") },
                        isError = state.nameError != null,
                        supportingText = { state.nameError?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_name"),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::submitCreate,
                        enabled = state.canSubmitCreate,
                        modifier = Modifier.fillMaxWidth().testTag("create_submit"),
                    ) { Text(if (state.isSubmitting) "Creating…" else "Create") }
                }
                HouseholdOnboardingUiState.Mode.Join -> {
                    // implemented in US-11
                    Text("Joining (placeholder)")
                }
            }
        }
    }
}
