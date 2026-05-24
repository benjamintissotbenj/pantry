package app.pantry.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
            Image(
                painter = painterResource(R.drawable.logo_pantry),
                contentDescription = null, // decorative; the "Pantry" text below labels the brand
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("auth_logo"),
            )
            Spacer(Modifier.height(12.dp))
            Text("Pantry", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                AuthUiState.Mode.Welcome -> WelcomeButtons(
                    viewModel = viewModel,
                    onPickEmail = { viewModel.switchMode(AuthUiState.Mode.EmailSignIn) },
                    onPickSignUp = { viewModel.switchMode(AuthUiState.Mode.EmailSignUp) },
                )
                AuthUiState.Mode.EmailSignIn,
                AuthUiState.Mode.EmailSignUp -> EmailForm(state, viewModel)
            }
        }
        ForgotPasswordDialog(
            state = state,
            onEmailChange = viewModel::onResetEmailChange,
            onSend = viewModel::sendPasswordReset,
            onDismiss = viewModel::closeResetDialog,
        )
    }
}

@Composable
private fun WelcomeButtons(
    viewModel: AuthViewModel,
    onPickEmail: () -> Unit,
    onPickSignUp: () -> Unit,
) {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    val controller = remember(context, webClientId) { GoogleSignInController(context, webClientId) }

    Button(
        onClick = {
            scope.launch {
                try {
                    val token = controller.requestIdToken() ?: return@launch
                    viewModel.signInWithGoogle(token)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    viewModel.onGoogleSignInError(t)
                }
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("welcome_google"),
    ) { Text("Continue with Google") }
    Spacer(Modifier.height(8.dp))
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
    if (state.mode == AuthUiState.Mode.EmailSignIn) {
        TextButton(
            onClick = { viewModel.openResetDialog(prefill = state.email) },
            modifier = Modifier.testTag("forgot_password"),
        ) { Text("Forgot password?") }
    }
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
