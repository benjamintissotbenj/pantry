package app.pantry.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class StartRoute(val path: String) {
    Loading("loading"),
    Auth(PantryRoute.Auth.path),
    Household(PantryRoute.Household.path),
    Home(PantryRoute.Home.path),
}

@HiltViewModel
class StartRouterViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val initialRoute: StateFlow<StartRoute> = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(StartRoute.Auth)
            else households.observeUserHouseholds(profile.uid).map { list ->
                if (list.isEmpty()) StartRoute.Household else StartRoute.Home
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartRoute.Loading)
}

@Composable
fun StartRouter(viewModel: StartRouterViewModel = hiltViewModel()) {
    val route by viewModel.initialRoute.collectAsState()
    when (route) {
        StartRoute.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        // startDestination is read once by NavHost. Mid-session transitions are driven
        // by in-screen navController.navigate(...) calls, not by changes to `route` here.
        else -> PantryNavHost(
            navController = rememberNavController(),
            startDestination = route.path,
        )
    }
}
