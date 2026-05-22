package app.pantry.ui.home

enum class HomeTab(val label: String) {
    Stock("Stock"),
    Shopping("Shopping"),
    Settings("Settings"),
    ;

    companion object { val Default = Stock }
}
