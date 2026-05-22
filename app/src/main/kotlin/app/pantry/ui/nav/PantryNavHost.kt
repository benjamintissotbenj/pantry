package app.pantry.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun PantryNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = PantryRoute.Auth.path) {
        composable(PantryRoute.Auth.path) { AuthPlaceholder() }
        composable(PantryRoute.Household.path) { HouseholdPlaceholder() }
        composable(PantryRoute.Home.path) { HomePlaceholder() }
    }
}

@Composable private fun AuthPlaceholder() = Centered("Sign in (placeholder)")
@Composable private fun HouseholdPlaceholder() = Centered("Household (placeholder)")
@Composable private fun HomePlaceholder() = Centered("Home (placeholder)")

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
