package app.pantry.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    if (!isOffline) return
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth().testTag("offline_banner"),
    ) {
        Text(
            text = "Offline — changes will sync when you reconnect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
