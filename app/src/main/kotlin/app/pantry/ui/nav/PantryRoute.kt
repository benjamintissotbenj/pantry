package app.pantry.ui.nav

sealed interface PantryRoute {
    val path: String
    data object Auth : PantryRoute { override val path = "auth" }
    data object Household : PantryRoute { override val path = "household" }
    data object Home : PantryRoute { override val path = "home" }
}
