package app.pantry.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.pantry.ui.settings.SettingsScreen
import app.pantry.ui.shopping.ShoppingPlaceholderScreen
import app.pantry.ui.stock.StockListScreen

@Composable
fun HomeShell(onSignedOut: () -> Unit) {
    val tabSaver = Saver<HomeTab, String>(
        save = { it.name },
        restore = { HomeTab.valueOf(it) },
    )
    var tab by rememberSaveable(stateSaver = tabSaver) { mutableStateOf(HomeTab.Default) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HomeTab.Stock,
                    onClick = { tab = HomeTab.Stock },
                    icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                    label = { Text(HomeTab.Stock.label) },
                    modifier = Modifier.testTag("tab_stock"),
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Shopping,
                    onClick = { tab = HomeTab.Shopping },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    label = { Text(HomeTab.Shopping.label) },
                    modifier = Modifier.testTag("tab_shopping"),
                )
                NavigationBarItem(
                    selected = tab == HomeTab.Settings,
                    onClick = { tab = HomeTab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(HomeTab.Settings.label) },
                    modifier = Modifier.testTag("tab_settings"),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                HomeTab.Stock -> StockListScreen()
                HomeTab.Shopping -> ShoppingPlaceholderScreen()
                HomeTab.Settings -> SettingsScreen(onSignedOut = onSignedOut)
            }
        }
    }
}
