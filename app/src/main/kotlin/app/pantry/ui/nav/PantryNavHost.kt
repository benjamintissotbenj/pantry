package app.pantry.ui.nav

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pantry.ui.auth.AuthScreen
import app.pantry.ui.home.HomePlaceholderScreen
import app.pantry.ui.household.HouseholdOnboardingScreen

@Composable
fun PantryNavHost(
    navController: NavHostController,
    startDestination: String = PantryRoute.Auth.path,
    /**
     * The auth screen composable. Defaults to the production [AuthScreen] using Hilt-injected
     * [AuthViewModel]. Overridden in tests that cannot bring up Hilt — see PantryNavHostTest.
     */
    @VisibleForTesting
    authScreen: @Composable (onAuthenticated: () -> Unit) -> Unit = { onAuthenticated ->
        AuthScreen(onAuthenticated = onAuthenticated)
    },
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(PantryRoute.Auth.path) {
            authScreen {
                navController.navigate(PantryRoute.Household.path) {
                    popUpTo(PantryRoute.Auth.path) { inclusive = true }
                }
            }
        }
        composable(PantryRoute.Household.path) {
            HouseholdOnboardingScreen(
                onCreated = { navController.navigate(PantryRoute.Home.path) { popUpTo(PantryRoute.Household.path) { inclusive = true } } },
                onJoined = { navController.navigate(PantryRoute.Home.path) { popUpTo(PantryRoute.Household.path) { inclusive = true } } },
            )
        }
        composable(PantryRoute.Home.path) {
            HomePlaceholderScreen(
                onSignedOut = {
                    navController.navigate(PantryRoute.Auth.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
