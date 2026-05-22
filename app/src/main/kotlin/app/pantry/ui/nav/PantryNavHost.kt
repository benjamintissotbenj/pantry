package app.pantry.ui.nav

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pantry.ui.auth.AuthScreen

@Composable
fun PantryNavHost(
    navController: NavHostController,
    /**
     * The auth screen composable. Defaults to the production [AuthScreen] using Hilt-injected
     * [AuthViewModel]. Overridden in tests that cannot bring up Hilt — see PantryNavHostTest.
     */
    @VisibleForTesting
    authScreen: @Composable (onAuthenticated: () -> Unit) -> Unit = { onAuthenticated ->
        AuthScreen(onAuthenticated = onAuthenticated)
    },
) {
    NavHost(navController = navController, startDestination = PantryRoute.Auth.path) {
        composable(PantryRoute.Auth.path) {
            authScreen {
                navController.navigate(PantryRoute.Household.path) {
                    popUpTo(PantryRoute.Auth.path) { inclusive = true }
                }
            }
        }
        composable(PantryRoute.Household.path) { Centered("Household (placeholder)") }
        composable(PantryRoute.Home.path) { Centered("Home (placeholder)") }
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
