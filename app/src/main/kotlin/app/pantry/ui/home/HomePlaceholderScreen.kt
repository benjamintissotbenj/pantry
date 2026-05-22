package app.pantry.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomePlaceholderScreen(
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val signedOut by viewModel.signedOut.collectAsState()
    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Coming next: stock catalog", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::signOut, modifier = Modifier.testTag("signout")) {
            Text("Sign out")
        }
    }
}
