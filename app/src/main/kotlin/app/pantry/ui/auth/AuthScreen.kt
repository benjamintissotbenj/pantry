package app.pantry.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.navigateToHousehold) {
        if (state.navigateToHousehold) {
            viewModel.consumeNavigation()
            onAuthenticated()
        }
    }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbar.showSnackbar(it); viewModel.consumeToast() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding: PaddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pantry", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                AuthUiState.Mode.Welcome -> WelcomeButtons(
                    onPickEmail = { viewModel.switchMode(AuthUiState.Mode.EmailSignIn) },
                    onPickSignUp = { viewModel.switchMode(AuthUiState.Mode.EmailSignUp) },
                )
                AuthUiState.Mode.EmailSignIn,
                AuthUiState.Mode.EmailSignUp -> EmailForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun WelcomeButtons(onPickEmail: () -> Unit, onPickSignUp: () -> Unit) {
    Button(onClick = onPickEmail, modifier = Modifier.fillMaxWidth().testTag("welcome_signin")) {
        Text("Sign in with email")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onPickSignUp, modifier = Modifier.testTag("welcome_signup")) {
        Text("Create an account")
    }
}

@Composable
private fun EmailForm(state: AuthUiState, viewModel: AuthViewModel) {
    if (state.mode == AuthUiState.Mode.EmailSignUp) {
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onDisplayNameChange,
            label = { Text("Your name") },
            isError = state.displayNameError != null,
            supportingText = { state.displayNameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().testTag("field_name"),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
    }
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("Email") },
        isError = state.emailError != null,
        supportingText = { state.emailError?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth().testTag("field_email"),
        singleLine = true,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        label = { Text("Password") },
        isError = state.passwordError != null,
        supportingText = { state.passwordError?.let { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("field_password"),
        singleLine = true,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = viewModel::submit,
        enabled = state.canSubmit && !state.isSubmitting,
        modifier = Modifier.fillMaxWidth().testTag("submit"),
    ) {
        if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp))
        else Text(if (state.mode == AuthUiState.Mode.EmailSignIn) "Sign in" else "Create account")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = {
            viewModel.switchMode(
                if (state.mode == AuthUiState.Mode.EmailSignIn) AuthUiState.Mode.EmailSignUp
                else AuthUiState.Mode.EmailSignIn
            )
        }
    ) {
        Text(if (state.mode == AuthUiState.Mode.EmailSignIn) "Need an account? Sign up" else "Have an account? Sign in")
    }
}
