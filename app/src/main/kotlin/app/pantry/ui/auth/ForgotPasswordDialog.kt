package app.pantry.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ForgotPasswordDialog(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.showResetDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset your password") },
        text = {
            OutlinedTextField(
                value = state.resetEmail,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                isError = state.resetEmailError != null,
                supportingText = { state.resetEmailError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.testTag("reset_email"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSend,
                enabled = !state.isSendingReset && state.resetEmail.isNotBlank() && state.resetEmailError == null,
                modifier = Modifier.testTag("reset_send"),
            ) { Text(if (state.isSendingReset) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
