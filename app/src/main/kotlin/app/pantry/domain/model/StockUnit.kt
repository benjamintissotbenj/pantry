package app.pantry.domain.model

enum class StockUnit(val storageKey: String, val displaySuffix: String) {
    COUNT(storageKey = "count", displaySuffix = ""),
    GRAM(storageKey = "g", displaySuffix = "g"),
    KILOGRAM(storageKey = "kg", displaySuffix = "kg"),
    MILLILITER(storageKey = "ml", displaySuffix = "ml"),
    LITER(storageKey = "L", displaySuffix = "L"),
    ;

    companion object {
        /** Maps a Firestore-stored key back to the enum. Defaults to COUNT for unknown values. */
        fun fromStorageKey(key: String?): StockUnit = entries.firstOrNull { it.storageKey == key } ?: COUNT
    }
}
