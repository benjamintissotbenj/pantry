# Phase 3 — Shopping List — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Pantry build where the Shopping tab shows a real, combined shopping list: auto entries (catalog items where `quantity < threshold`) and manual entries (free-text, optionally linked to a catalog item). Entries are grouped by category. The user can check items off while shopping, add manual entries with autocomplete from the catalog, and tap a single **Finish shopping** action to bulk-restock checked items to their per-item `defaultRestockQuantity` and clear processed manual entries in one atomic batch.

**Architecture:** `households/{hid}/shoppingList/{entryId}` stores manual entries only; auto entries are derived client-side from `households/{hid}/items` filtered by `quantity < threshold`. `ShoppingEntryRepository` exposes a `Flow<List<ShoppingEntry>>` of manual entries via `callbackFlow` + snapshot listener, plus `addEntry`, `setChecked`, and `finishShopping(plan)`. The derivation, ordering (by section → category → name), and finish-shopping-plan computation live in `ShoppingListViewModel`, combining `StockItemRepository.observe(hid)` with the manual-entries flow and an in-memory `autoCheckedItemIds: StateFlow<Set<String>>` for transient per-device auto-check state. Finish shopping commits a single Firestore `WriteBatch` (atomic; all-or-nothing). `StockItem` gains an optional `defaultRestockQuantity: Double?` field, surfaced as an extra `OutlinedTextField` in the existing `AddEditItemBottomSheet`.

**Tech stack:** Same as Phase 2 — Kotlin 2.1.0 · AGP 8.7.0 · Compose BoM 2025.01.00 · Material 3 · Hilt 2.53 · Firebase BoM 33.7.0 · Robolectric 4.13 · JUnit 5 · Turbine · MockK · Firebase Local Emulator Suite. No new top-level dependencies needed.

**Spec source:** `spec/android-app-design.md`
**Plan location:** `spec/phase-3-shopping-list.md`
**Project root:** `Repositories/pantry/`
**Package:** `app.pantry`
**Starts from:** tag `phase-2-complete` (commit `81d9e2d` or later on `main`)
**Branch:** all phase-3 work lives on a `phase-3-shopping-list` feature branch, merged into `main` after `phase-3-complete` is tagged.

---

## Definition of Done (Phase 3)

- `./gradlew :app:assembleDebug` produces an installable APK
- The Shopping tab shows the real shopping list (no more "coming soon" placeholder)
- Both sections (Running low / Added manually) render with category sub-headers; items sort alphabetically inside each sub-header
- Auto entries reflect catalog items where `quantity < threshold` live
- Manual entries can be added via the FAB and persist across app restarts and devices in the household
- Tapping a manual entry's checkbox persists `checked: true/false` on the Firestore doc; the visual is a strike-through name
- Tapping an auto entry's checkbox toggles transient per-device intent state; restarting the app or backgrounding clears it
- The Add/Edit item sheet (from Phase 2) has a new optional **Default restock quantity** field below the threshold field
- The Shopping tab's overflow menu surfaces **Finish shopping**; tapping it shows a confirmation dialog with a preview ("Restock N items, clear M manual entries, skip K with no default")
- Confirming **Finish shopping** commits one atomic Firestore `WriteBatch` that (a) sets `quantity = defaultRestockQuantity` for every checked auto entry that has a default, (b) sets `quantity = defaultRestockQuantity` on the linked catalog item for every checked manual-linked entry that has a default, (c) deletes every checked manual entry (linked or not). Items missing `defaultRestockQuantity` are skipped (no catalog write) but their manual entry, if any, is still deleted.
- A snackbar after the commit summarises: "N restocked, M cleared, K skipped" with an action "Details" that opens a dialog listing skipped names
- The auto-checked set is cleared after a successful commit
- `ShoppingListUiState.isOffline` exists with a `TODO(phase-4)` comment; offline gating is a deliberate no-op for this phase since `ConnectivityRepository` does not yet exist. Firestore's built-in offline persistence handles queueing of manual-entry writes and checkbox toggles.
- All unit + Compose Robolectric tests pass: `./gradlew :app:test`
- All commits follow Conventional Commits
- Tag `phase-3-complete` on the last commit of the `phase-3-shopping-list` branch; the branch is merged into `main` (fast-forward unless the user explicitly requests a merge commit)

---

## User stories

| ID | Title | Type |
|---|---|---|
| US-1 | Domain model: extend `StockItem` + add `ShoppingEntry` | infra |
| US-2 | `StockItemRepository` round-trips `defaultRestockQuantity` | infra |
| US-3 | Add/Edit sheet exposes `defaultRestockQuantity` field | user-facing |
| US-4 | `ShoppingEntryRepository` interface + Hilt binding | infra |
| US-5 | `FirestoreShoppingEntryRepository` implementation | infra |
| US-6 | `ShoppingListUiState` + `ShoppingListViewModel` derivation | infra |
| US-7 | `ShoppingListScreen` skeleton (sections, sub-headers, rows) | user-facing |
| US-8 | Checkbox interactions (manual persist, auto in-memory) | user-facing |
| US-9 | `AddManualEntryBottomSheet` with autocomplete | user-facing |
| US-10 | Finish shopping: confirm dialog + WriteBatch + snackbar | user-facing |
| US-11 | Wire `HomeShell` to real Shopping screen + offline gating + smoke notes | user-facing |

---

## US-1: Domain model — extend `StockItem` and add `ShoppingEntry`

**As a** developer
**I want** the Kotlin domain model to carry the new `defaultRestockQuantity` field on `StockItem` and a fresh `ShoppingEntry` type with an `AUTO`/`MANUAL` source discriminator
**So that** every later US can rely on a stable, well-typed shape for the catalog and the shopping list.

### Acceptance criteria

- `StockItem.defaultRestockQuantity: Double?` is added as the final field (nullable, no default in the data class so call sites are forced to set it)
- `ShoppingEntry` exists as a sealed-ish data class with a `Source` enum (`AUTO`, `MANUAL`)
- All existing call sites of `StockItem(...)` compile after the change (passing `defaultRestockQuantity = null` where needed)
- A small unit test covers `ShoppingEntry` field defaults

### Files

- Modify: `app/src/main/kotlin/app/pantry/domain/model/StockItem.kt`
- Create: `app/src/main/kotlin/app/pantry/domain/model/ShoppingEntry.kt`
- Create: `app/src/test/kotlin/app/pantry/domain/model/ShoppingEntryTest.kt`

### Tasks

- [ ] **Step 1 — Extend `StockItem`**

Edit `app/src/main/kotlin/app/pantry/domain/model/StockItem.kt`:

```kotlin
package app.pantry.domain.model

import java.time.Instant

data class StockItem(
    val id: String,
    val name: String,
    val category: String,
    val unit: StockUnit,
    val quantity: Double,
    val threshold: Double,
    val updatedAt: Instant,
    val defaultRestockQuantity: Double?,
) {
    val isLowStock: Boolean get() = quantity < threshold
}
```

- [ ] **Step 2 — Create `ShoppingEntry`**

Create `app/src/main/kotlin/app/pantry/domain/model/ShoppingEntry.kt`:

```kotlin
package app.pantry.domain.model

import java.time.Instant

/**
 * A row in the shopping list. AUTO entries are derived from catalog items where
 * quantity < threshold and have no Firestore doc (id = "auto:${itemId}"). MANUAL
 * entries live in households/{hid}/shoppingList/{entryId} and may optionally link
 * to a catalog item.
 */
data class ShoppingEntry(
    val id: String,
    val name: String,
    val source: Source,
    val checked: Boolean,
    val createdAt: Instant,
    val linkedItemId: String?,
    val category: String,
    val currentQuantity: Double?,
    val threshold: Double?,
    val defaultRestockQuantity: Double?,
) {
    enum class Source { AUTO, MANUAL }
}
```

> Why all the optional fields on one type rather than a sealed class with AUTO/MANUAL subtypes? The Shopping screen renders both source kinds in the same `LazyColumn` row composable — keeping one flat shape avoids `when` ceremony in the UI layer. `currentQuantity`/`threshold` are populated for AUTO and for MANUAL with a resolved link; null otherwise. `category` falls back to `"Other"` for ad-hoc manual entries.

- [ ] **Step 3 — Update existing constructor call sites**

Update every place that constructs `StockItem(...)` (use IDE/grep) to pass `defaultRestockQuantity = null`:

- `app/src/main/kotlin/app/pantry/data/stock/FirestoreStockItemRepository.kt` (the `create` builder at the end of the function, and the `toStockItem()` mapper at the bottom)
- `app/src/test/kotlin/app/pantry/ui/stock/StockListViewModelTest.kt` — the `item()` helper near line 32 constructs `StockItem(...)` positionally; add `null` as the final positional arg or switch to named arguments
- Any other test fixture that constructs `StockItem(...)` literals (grep `StockItem\\(` under `app/src/test/`)

For `FirestoreStockItemRepository.toStockItem()`, also read the field from the snapshot:

```kotlin
val defaultRestockQuantity = if (contains("defaultRestockQuantity")) getDouble("defaultRestockQuantity") else null
```

(`contains` here is `DocumentSnapshot.contains(String)`.) For the `create` return, pass through whatever the new repo signature receives — US-2 will adjust the repo signature; for now just thread `null`.

- [ ] **Step 4 — Add the test**

Create `app/src/test/kotlin/app/pantry/domain/model/ShoppingEntryTest.kt`:

```kotlin
package app.pantry.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShoppingEntryTest {
    @Test
    fun `auto entry surfaces source and catalog hints`() {
        val now = Instant.parse("2026-05-23T10:00:00Z")
        val entry = ShoppingEntry(
            id = "auto:abc",
            name = "Milk",
            source = ShoppingEntry.Source.AUTO,
            checked = false,
            createdAt = now,
            linkedItemId = "abc",
            category = "Dairy",
            currentQuantity = 0.0,
            threshold = 2.0,
            defaultRestockQuantity = 4.0,
        )
        assertEquals(ShoppingEntry.Source.AUTO, entry.source)
        assertEquals("abc", entry.linkedItemId)
        assertEquals(4.0, entry.defaultRestockQuantity)
    }

    @Test
    fun `manual ad-hoc entry has no link or catalog hint`() {
        val entry = ShoppingEntry(
            id = "entry1",
            name = "Wine for guests",
            source = ShoppingEntry.Source.MANUAL,
            checked = false,
            createdAt = Instant.now(),
            linkedItemId = null,
            category = "Other",
            currentQuantity = null,
            threshold = null,
            defaultRestockQuantity = null,
        )
        assertNull(entry.linkedItemId)
        assertNull(entry.threshold)
        assertEquals("Other", entry.category)
    }
}
```

- [ ] **Step 5 — Compile + commit**

- Run `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` and fix any remaining call-site errors (likely test fixtures).
- Run `./gradlew :app:test --tests "app.pantry.domain.model.ShoppingEntryTest"` — green.
- Commit: `feat(model): add ShoppingEntry and StockItem.defaultRestockQuantity`

---

## US-2: Persist `defaultRestockQuantity` through `StockItemRepository`

**As a** developer
**I want** `StockItemRepository.create` and `.update` to accept the new `defaultRestockQuantity` argument and `FirestoreStockItemRepository` to round-trip it through Firestore
**So that** later USes can read and write the field via the same repository the rest of the app already uses.

### Acceptance criteria

- `StockItemRepository.create` signature gains `defaultRestockQuantity: Double?`
- `StockItemRepository.update` signature gains `defaultRestockQuantity: Double?`
- `FirestoreStockItemRepository.create` writes `defaultRestockQuantity` (omits the field when `null` — uses `FieldValue.delete()` on update if null)
- `FirestoreStockItemRepository.update` writes `defaultRestockQuantity` (or deletes it when null)
- `toStockItem()` already reads the field (per US-1 step 3) — verify
- Existing repository tests still pass with the new signatures (pass `null` for the new arg)

### Files

- Modify: `app/src/main/kotlin/app/pantry/data/stock/StockItemRepository.kt`
- Modify: `app/src/main/kotlin/app/pantry/data/stock/FirestoreStockItemRepository.kt`
- Modify: any existing fakes or tests under `app/src/test/kotlin/app/pantry/data/stock/`

### Tasks

- [ ] **Step 1 — Update the interface**

```kotlin
suspend fun create(
    householdId: String,
    name: String,
    category: String,
    unit: StockUnit,
    quantity: Double,
    threshold: Double,
    defaultRestockQuantity: Double?,
): Result<StockItem>

suspend fun update(
    householdId: String,
    itemId: String,
    name: String,
    category: String,
    unit: StockUnit,
    quantity: Double,
    threshold: Double,
    defaultRestockQuantity: Double?,
): Result<Unit>
```

- [ ] **Step 2 — Update `FirestoreStockItemRepository.create`**

Build the data map conditionally so we never write `null`:

```kotlin
val data = buildMap<String, Any> {
    put("name", name)
    put("category", category)
    put("unit", unit.storageKey)
    put("quantity", quantity)
    put("threshold", threshold)
    put("updatedAt", FieldValue.serverTimestamp())
    if (defaultRestockQuantity != null) put("defaultRestockQuantity", defaultRestockQuantity)
}
doc.set(data).await()
StockItem(
    id = doc.id,
    name = name,
    category = category,
    unit = unit,
    quantity = quantity,
    threshold = threshold,
    updatedAt = Instant.now(),
    defaultRestockQuantity = defaultRestockQuantity,
)
```

- [ ] **Step 3 — Update `FirestoreStockItemRepository.update`**

When the user clears the field, we want it gone from the doc — use `FieldValue.delete()`:

```kotlin
val data = buildMap<String, Any> {
    put("name", name)
    put("category", category)
    put("unit", unit.storageKey)
    put("quantity", quantity)
    put("threshold", threshold)
    put("updatedAt", FieldValue.serverTimestamp())
    put("defaultRestockQuantity", defaultRestockQuantity ?: FieldValue.delete())
}
itemsCol(householdId).document(itemId).update(data).await()
```

- [ ] **Step 4 — Verify `toStockItem` reads it**

Already updated in US-1 step 3. Double-check it survives merges. The relevant lines:

```kotlin
val defaultRestockQuantity = if (contains("defaultRestockQuantity"))
    getDouble("defaultRestockQuantity") else null
return StockItem(
    id = id,
    name = name,
    category = category,
    unit = unit,
    quantity = quantity,
    threshold = threshold,
    updatedAt = updatedAt,
    defaultRestockQuantity = defaultRestockQuantity,
)
```

- [ ] **Step 5 — Update call sites + tests**

Use Grep to find `stock.create(` and `stock.update(` invocations across the codebase:

```
Grep pattern: "stock\\.(create|update)\\(" type=kt
```

Add `defaultRestockQuantity = null` to each (so existing behavior is unchanged). Notable call site: `AddEditItemViewModel.submit()` — leave at `null` for now; US-3 wires the real value in.

For any existing in-memory fake `StockItemRepository`, mirror the signature change.

- [ ] **Step 6 — Compile + test + commit**

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` — green.
- `./gradlew :app:test` — all existing tests green (no behavior change).
- Commit: `feat(stock): repository round-trips defaultRestockQuantity`

---

## US-3: Surface `defaultRestockQuantity` in the Add/Edit item sheet

**As a** household member adding or editing a stock item
**I want** an optional "Default restock quantity" field in the sheet
**So that** when I tap **Finish shopping** later, the app knows what total quantity to set for me on this item.

### Acceptance criteria

- `AddEditItemUiState` gains `defaultRestockQuantity: String = ""` (blank → null when saving)
- `AddEditItemBottomSheet` shows a new `OutlinedTextField` directly below the threshold field
- Label: "Default restock quantity (optional)"
- Helper text: "When you tap Finish shopping, this becomes your new total"
- Keyboard type: Decimal
- `canSubmit` accepts blank or numeric (mirrors the threshold field)
- `beginEdit` populates the field from the existing item (formatted via `formatQuantity`)
- `submit()` passes `defaultRestockQuantity.toDoubleOrNull()` (which is null for blank) into the repo
- A test confirms blank → null in the create call and "4" → 4.0 in the edit call

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemBottomSheet.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt` (extend)

### Tasks

- [ ] **Step 1 — Extend `AddEditItemUiState`**

```kotlin
package app.pantry.ui.stock

import app.pantry.domain.model.StockUnit

data class AddEditItemUiState(
    val mode: Mode = Mode.Add,
    val itemId: String? = null,
    val name: String = "",
    val quantity: String = "",
    val unit: StockUnit = StockUnit.COUNT,
    val threshold: String = "1",
    val defaultRestockQuantity: String = "",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val dismissed: Boolean = false,
) {
    enum class Mode { Add, Edit }

    val canSubmit: Boolean
        get() = !isSubmitting
            && name.isNotBlank()
            && (quantity.isBlank() || quantity.toDoubleOrNull() != null)
            && (threshold.isBlank() || threshold.toDoubleOrNull() != null)
            && (defaultRestockQuantity.isBlank() || defaultRestockQuantity.toDoubleOrNull() != null)
}
```

- [ ] **Step 2 — Add the ViewModel change handler**

In `AddEditItemViewModel`:

```kotlin
fun onDefaultRestockQuantityChange(v: String) =
    _state.update { it.copy(defaultRestockQuantity = v) }
```

Update `beginEdit` to accept and use the field:

```kotlin
fun beginEdit(
    itemId: String,
    name: String,
    quantity: Double,
    unit: StockUnit,
    threshold: Double,
    category: String,
    defaultRestockQuantity: Double?,
) {
    _state.value = AddEditItemUiState(
        mode = AddEditItemUiState.Mode.Edit,
        itemId = itemId,
        name = name,
        quantity = formatQuantity(quantity),
        unit = unit,
        threshold = formatQuantity(threshold),
        defaultRestockQuantity = defaultRestockQuantity?.let { formatQuantity(it) } ?: "",
        category = category,
    )
}
```

Update `submit()` to pass the field through:

```kotlin
val drq = s.defaultRestockQuantity.takeIf { it.isNotBlank() }?.toDoubleOrNull()

val result = if (s.mode == AddEditItemUiState.Mode.Add) {
    stock.create(
        householdId = hid,
        name = s.name.trim(),
        category = s.category.trim(),
        unit = s.unit,
        quantity = s.quantity.toDoubleOrNull() ?: 0.0,
        threshold = s.threshold.toDoubleOrNull() ?: 1.0,
        defaultRestockQuantity = drq,
    ).map { Unit }
} else {
    stock.update(
        householdId = hid,
        itemId = s.itemId ?: return@launch,
        name = s.name.trim(),
        category = s.category.trim(),
        unit = s.unit,
        quantity = s.quantity.toDoubleOrNull() ?: 0.0,
        threshold = s.threshold.toDoubleOrNull() ?: 1.0,
        defaultRestockQuantity = drq,
    )
}
```

- [ ] **Step 3 — Update `StockListScreen` (the caller of `beginEdit`)**

Find the row click handler that opens the edit sheet and pass through `defaultRestockQuantity = item.defaultRestockQuantity`. Use Grep:

```
Grep pattern: "beginEdit\\(" type=kt
```

Update the call site in `StockListScreen.kt` (it's the lambda that fires when a row name is tapped).

- [ ] **Step 4 — Add the field to the sheet**

In `AddEditItemBottomSheet.kt`, insert a new `OutlinedTextField` directly below the existing threshold field:

```kotlin
OutlinedTextField(
    value = state.threshold,
    onValueChange = viewModel::onThresholdChange,
    label = { Text("Low-stock threshold") },
    supportingText = { Text("Below this, the item shows on the shopping list") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = Modifier.fillMaxWidth().testTag("field_threshold"),
)

OutlinedTextField(
    value = state.defaultRestockQuantity,
    onValueChange = viewModel::onDefaultRestockQuantityChange,
    label = { Text("Default restock quantity (optional)") },
    supportingText = { Text("When you tap Finish shopping, this becomes your new total") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    modifier = Modifier.fillMaxWidth().testTag("field_default_restock"),
)
```

- [ ] **Step 5 — Extend `AddEditItemViewModelTest`**

Add tests that confirm:
- Blank `defaultRestockQuantity` → `null` reaches the repo on create
- "4" → `4.0` reaches the repo on edit
- Non-numeric (e.g., "abc") blocks submission via `canSubmit = false`

Use the existing test scaffolding pattern. Example new test:

```kotlin
@Test
fun `blank default restock quantity creates item with null`() = runTest {
    val fake = FakeStockItemRepository()
    val vm = AddEditItemViewModel(fakeHousehold, fake)
    vm.beginAdd()
    vm.onNameChange("Milk")
    vm.onQuantityChange("1")
    vm.submit()
    advanceUntilIdle()
    assertNull(fake.lastCreate?.defaultRestockQuantity)
}

@Test
fun `numeric default restock quantity round-trips on edit`() = runTest {
    val fake = FakeStockItemRepository()
    val vm = AddEditItemViewModel(fakeHousehold, fake)
    vm.beginEdit(
        itemId = "x",
        name = "Milk",
        quantity = 0.0,
        unit = StockUnit.COUNT,
        threshold = 2.0,
        category = "Dairy",
        defaultRestockQuantity = null,
    )
    vm.onDefaultRestockQuantityChange("4")
    vm.submit()
    advanceUntilIdle()
    assertEquals(4.0, fake.lastUpdate?.defaultRestockQuantity)
}
```

(Adjust the `FakeStockItemRepository` to capture the new arg; if a fake doesn't exist yet, create a minimal one in the test package.)

- [ ] **Step 6 — Compile + test + commit**

- `./gradlew :app:test --tests "app.pantry.ui.stock.AddEditItemViewModelTest"` — green.
- Commit: `feat(stock): add default restock quantity field to add/edit sheet`

---

## US-4: `ShoppingEntryRepository` interface + Hilt binding

**As a** developer
**I want** a typed boundary for manual shopping entry CRUD plus a `finishShopping(plan)` operation
**So that** ViewModels depend on an interface I can fake in tests, and the Firestore implementation can be added in US-5 without touching consumers.

### Acceptance criteria

- `ShoppingEntryRepository` interface with `observe`, `addEntry`, `setChecked`, `finishShopping`
- A `FinishShoppingPlan` data class enumerates the atomic operations
- A `FinishShoppingReport` data class summarises what happened (counts + skipped names)
- The Hilt module binds the interface to the (forthcoming) `FirestoreShoppingEntryRepository` (`@Binds`)
- Compiles; no logic yet

### Files

- Create: `app/src/main/kotlin/app/pantry/data/shopping/ShoppingEntryRepository.kt`
- Create: `app/src/main/kotlin/app/pantry/data/shopping/FinishShoppingPlan.kt`
- Modify: `app/src/main/kotlin/app/pantry/di/DataModule.kt` (or wherever `FirestoreStockItemRepository` is bound — add the new binding alongside)

### Tasks

- [ ] **Step 1 — Create the repo interface**

```kotlin
package app.pantry.data.shopping

import app.pantry.domain.model.ShoppingEntry
import kotlinx.coroutines.flow.Flow

interface ShoppingEntryRepository {
    /** Manual entries only (auto entries are derived client-side). */
    fun observe(householdId: String): Flow<List<ShoppingEntry>>

    suspend fun addEntry(
        householdId: String,
        name: String,
        linkedItemId: String?,
    ): Result<Unit>

    suspend fun setChecked(
        householdId: String,
        entryId: String,
        checked: Boolean,
    ): Result<Unit>

    /** Commits the plan as a single atomic Firestore WriteBatch. */
    suspend fun finishShopping(
        householdId: String,
        plan: FinishShoppingPlan,
    ): Result<FinishShoppingReport>
}
```

- [ ] **Step 2 — Create the plan + report types**

```kotlin
package app.pantry.data.shopping

/**
 * Computed in the ViewModel from the current snapshot of items + manual entries +
 * auto-checked set. Handed to the repo as an immutable instruction set; the repo
 * commits it atomically.
 */
data class FinishShoppingPlan(
    /** Catalog items that get `quantity = defaultRestockQuantity` written. */
    val restocks: List<Restock>,
    /** Manual entry ids to delete unconditionally. */
    val manualEntryIdsToDelete: List<String>,
    /** Skipped items (no defaultRestockQuantity) — included only for reporting. */
    val skippedNames: List<String>,
) {
    data class Restock(
        val itemId: String,
        val newQuantity: Double,
    )
}

data class FinishShoppingReport(
    val restockedCount: Int,
    val clearedCount: Int,
    val skippedCount: Int,
    val skippedNames: List<String>,
)
```

- [ ] **Step 3 — Hilt binding**

Add to the existing data Hilt module (locate via Grep for `FirestoreStockItemRepository` in `@Binds`/`@Provides` context):

```kotlin
@Binds
@Singleton
abstract fun bindShoppingEntryRepository(
    impl: FirestoreShoppingEntryRepository,
): ShoppingEntryRepository
```

> Skip this step if the project uses constructor-injection-only and a `@Module` doesn't yet exist. In that case, add `@Inject constructor` + `@Singleton` to the Firestore impl in US-5 and let Hilt resolve it automatically.

- [ ] **Step 4 — Stub impl to keep Hilt happy until US-5**

To allow the project to compile end-to-end before US-5, create a stub:

```kotlin
package app.pantry.data.shopping

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class FirestoreShoppingEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ShoppingEntryRepository {
    override fun observe(householdId: String): Flow<List<app.pantry.domain.model.ShoppingEntry>> = flowOf(emptyList())
    override suspend fun addEntry(householdId: String, name: String, linkedItemId: String?) = Result.success(Unit)
    override suspend fun setChecked(householdId: String, entryId: String, checked: Boolean) = Result.success(Unit)
    override suspend fun finishShopping(householdId: String, plan: FinishShoppingPlan) =
        Result.success(FinishShoppingReport(0, 0, 0, emptyList()))
}
```

US-5 replaces every body.

- [ ] **Step 5 — Compile + commit**

- `./gradlew :app:compileDebugKotlin` — green.
- Commit: `feat(shopping): add ShoppingEntryRepository contract + stub`

---

## US-5: `FirestoreShoppingEntryRepository` implementation

**As a** developer
**I want** a working Firestore-backed `ShoppingEntryRepository` with a real snapshot listener and an atomic `finishShopping` WriteBatch
**So that** the rest of the screen can rely on real persistence and the batch action is all-or-nothing.

### Acceptance criteria

- `observe(hid)` emits the current list of MANUAL entries from `households/{hid}/shoppingList`, ordered server-side by `createdAt` ascending
- `addEntry` writes a new doc with `name`, optional `itemId`, `createdAt = serverTimestamp()`, `checked = false`
- `setChecked` updates the doc's `checked` field
- `finishShopping` constructs one `FirebaseFirestore.batch()`:
  - For each `Restock`: `batch.update(itemRef, mapOf("quantity" to newQuantity, "updatedAt" to serverTimestamp()))`
  - For each manual entry id in `manualEntryIdsToDelete`: `batch.delete(entryRef)`
  - `batch.commit().await()`
- Returns `FinishShoppingReport` populated from the plan's counts/names
- Unit test (Robolectric not required; this is plain logic with a mocked `FirebaseFirestore`) confirms a sample plan triggers the expected batch operations

### Files

- Modify: `app/src/main/kotlin/app/pantry/data/shopping/FirestoreShoppingEntryRepository.kt`
- Create: `app/src/test/kotlin/app/pantry/data/shopping/FirestoreShoppingEntryRepositoryTest.kt`

### Tasks

- [ ] **Step 1 — Replace stub with real impl**

```kotlin
package app.pantry.data.shopping

import app.pantry.domain.model.ShoppingEntry
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreShoppingEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ShoppingEntryRepository {

    private fun col(householdId: String) =
        firestore.collection("households").document(householdId).collection("shoppingList")

    private fun itemsCol(householdId: String) =
        firestore.collection("households").document(householdId).collection("items")

    override fun observe(householdId: String): Flow<List<ShoppingEntry>> = callbackFlow {
        val reg = col(householdId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { qs, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(qs?.documents.orEmpty().mapNotNull { it.toShoppingEntry() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addEntry(
        householdId: String,
        name: String,
        linkedItemId: String?,
    ): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("name", name)
            put("checked", false)
            put("createdAt", FieldValue.serverTimestamp())
            if (linkedItemId != null) put("itemId", linkedItemId)
        }
        col(householdId).document().set(data).await()
    }

    override suspend fun setChecked(
        householdId: String,
        entryId: String,
        checked: Boolean,
    ): Result<Unit> = runCatching {
        col(householdId).document(entryId).update("checked", checked).await()
    }

    override suspend fun finishShopping(
        householdId: String,
        plan: FinishShoppingPlan,
    ): Result<FinishShoppingReport> = runCatching {
        val batch = firestore.batch()
        plan.restocks.forEach { r ->
            val ref = itemsCol(householdId).document(r.itemId)
            batch.update(
                ref,
                mapOf(
                    "quantity" to r.newQuantity,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        plan.manualEntryIdsToDelete.forEach { id ->
            batch.delete(col(householdId).document(id))
        }
        batch.commit().await()
        FinishShoppingReport(
            restockedCount = plan.restocks.size,
            clearedCount = plan.manualEntryIdsToDelete.size,
            skippedCount = plan.skippedNames.size,
            skippedNames = plan.skippedNames,
        )
    }

    private fun DocumentSnapshot.toShoppingEntry(): ShoppingEntry? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val checked = getBoolean("checked") ?: false
        val itemId = getString("itemId")
        val createdAt = getTimestamp("createdAt")
            ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
            ?: Instant.EPOCH
        // category/currentQuantity/threshold/defaultRestockQuantity are filled in
        // by the ViewModel when it resolves the linkedItemId against the items list.
        return ShoppingEntry(
            id = id,
            name = name,
            source = ShoppingEntry.Source.MANUAL,
            checked = checked,
            createdAt = createdAt,
            linkedItemId = itemId,
            category = "Other",
            currentQuantity = null,
            threshold = null,
            defaultRestockQuantity = null,
        )
    }
}
```

- [ ] **Step 2 — Add a unit test for `finishShopping` batch construction**

We mock `FirebaseFirestore`, `WriteBatch`, and the collection/doc refs via MockK. The test verifies the batch receives exactly the expected calls in order.

```kotlin
package app.pantry.data.shopping

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FirestoreShoppingEntryRepositoryTest {

    @Test
    fun `finishShopping commits restocks then deletes`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        // Stub `households/{hid}/items/{id}` and `households/{hid}/shoppingList/{id}` refs
        val itemRef = mockk<DocumentReference>(relaxed = true)
        val entryRef = mockk<DocumentReference>(relaxed = true)
        val itemsCol = mockk<CollectionReference>(relaxed = true)
        val shoppingCol = mockk<CollectionReference>(relaxed = true)
        val hhDoc = mockk<DocumentReference>(relaxed = true)
        val hhCol = mockk<CollectionReference>(relaxed = true)

        every { firestore.collection("households") } returns hhCol
        every { hhCol.document("HH") } returns hhDoc
        every { hhDoc.collection("items") } returns itemsCol
        every { hhDoc.collection("shoppingList") } returns shoppingCol
        every { itemsCol.document("item-1") } returns itemRef
        every { shoppingCol.document("entry-1") } returns entryRef

        val repo = FirestoreShoppingEntryRepository(firestore)
        val plan = FinishShoppingPlan(
            restocks = listOf(FinishShoppingPlan.Restock(itemId = "item-1", newQuantity = 4.0)),
            manualEntryIdsToDelete = listOf("entry-1"),
            skippedNames = listOf("Soap"),
        )

        val report = repo.finishShopping("HH", plan).getOrThrow()

        verifySequence {
            firestore.batch()
            batch.update(itemRef, match<Map<String, Any>> {
                it["quantity"] == 4.0 && it.containsKey("updatedAt")
            })
            batch.delete(entryRef)
            batch.commit()
        }
        assertEquals(1, report.restockedCount)
        assertEquals(1, report.clearedCount)
        assertEquals(1, report.skippedCount)
        assertEquals(listOf("Soap"), report.skippedNames)
    }
}
```

- [ ] **Step 3 — Compile + test + commit**

- `./gradlew :app:test --tests "app.pantry.data.shopping.FirestoreShoppingEntryRepositoryTest"` — green.
- Commit: `feat(shopping): implement FirestoreShoppingEntryRepository`

---

## US-6: `ShoppingListUiState` + `ShoppingListViewModel` derivation

**As a** developer
**I want** the ViewModel to combine catalog items, manual entries, and the in-memory auto-check set into a single `ShoppingListUiState` with proper ordering
**So that** the screen is a pure projection of state and can be tested without touching Firestore.

### Acceptance criteria

- `ShoppingListUiState` describes the screen: two sections (`runningLow`, `manual`), each containing `categorySubgroups`, each holding `entries`
- Order: section (Running low first, then Manual) → category (alphabetical, with "Other" pinned last) → entry name (alphabetical, case-insensitive)
- Auto entries' `checked` reflects the ViewModel's in-memory set
- Manual-linked entries have their category, currentQuantity, threshold, defaultRestockQuantity resolved by looking up the linked item; if the link is broken, treat as manual-only with category "Other"
- `ShoppingListViewModel` exposes `onAutoEntryToggle(itemId)`, `onManualEntryToggle(entryId, checked)`, `onAddManual(name, linkedItemId)`, `onFinishShopping()` (US-10 wires the last to real work)
- The plan computation for `onFinishShopping` is a pure function over the current state (extract a private function `buildFinishShoppingPlan(state): FinishShoppingPlan` and unit-test it directly)
- Unit tests cover: derivation produces correct sections + ordering, auto-toggle updates state, manual-toggle calls repo, plan builder includes/excludes correctly

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListUiState.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListViewModel.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define the UI state shape**

```kotlin
package app.pantry.ui.shopping

import app.pantry.domain.model.ShoppingEntry

data class ShoppingListUiState(
    val isOffline: Boolean = false,
    val isLoading: Boolean = true,
    val runningLow: List<CategorySubgroup> = emptyList(),
    val manual: List<CategorySubgroup> = emptyList(),
    val finishShoppingPreview: FinishShoppingPreview = FinishShoppingPreview(0, 0, 0),
    val pendingReport: FinishShoppingReport? = null,
    val skippedDialogVisible: Boolean = false,
) {
    val isEmpty: Boolean get() = runningLow.isEmpty() && manual.isEmpty()
}

data class CategorySubgroup(
    val category: String,
    val entries: List<ShoppingEntry>,
)

data class FinishShoppingPreview(
    val restockCount: Int,
    val clearCount: Int,
    val skipCount: Int,
)

data class FinishShoppingReport(
    val restockedCount: Int,
    val clearedCount: Int,
    val skippedCount: Int,
    val skippedNames: List<String>,
)
```

> The `FinishShoppingReport` here is a UI-layer duplicate of the data-layer one (US-4). It carries the same fields but lives in the UI package so the screen doesn't import from `data.shopping`. The ViewModel converts between them.

- [ ] **Step 2 — Implement the ViewModel**

```kotlin
package app.pantry.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.FinishShoppingPlan
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.ShoppingEntry
import app.pantry.domain.model.StockItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
    private val shopping: ShoppingEntryRepository,
) : ViewModel() {

    private val autoChecked = MutableStateFlow<Set<String>>(emptySet())
    private val skippedDialogVisible = MutableStateFlow(false)
    private val pendingReport = MutableStateFlow<FinishShoppingReport?>(null)

    val uiState: StateFlow<ShoppingListUiState> =
        currentHousehold.currentHouseholdId
            .flatMapLatest { hid ->
                if (hid == null) flowOf(ShoppingListUiState(isLoading = false))
                else combine(
                    stock.observe(hid),
                    shopping.observe(hid),
                    autoChecked,
                    pendingReport,
                    skippedDialogVisible,
                ) { items, manualEntries, checked, report, skippedVisible ->
                    project(items, manualEntries, checked, report, skippedVisible)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListUiState())

    fun onAutoEntryToggle(itemId: String) {
        autoChecked.update { s -> if (itemId in s) s - itemId else s + itemId }
    }

    fun onManualEntryToggle(entryId: String, newChecked: Boolean) {
        val hid = currentHousehold.currentHouseholdId.value ?: return
        viewModelScope.launch { shopping.setChecked(hid, entryId, newChecked) }
    }

    fun onAddManual(name: String, linkedItemId: String?) {
        val hid = currentHousehold.currentHouseholdId.value ?: return
        viewModelScope.launch { shopping.addEntry(hid, name.trim(), linkedItemId) }
    }

    fun onFinishShopping(onDone: (success: Boolean) -> Unit) {
        val hid = currentHousehold.currentHouseholdId.value ?: return onDone(false)
        val plan = buildPlanFromState() ?: return onDone(false)
        viewModelScope.launch {
            val result = shopping.finishShopping(hid, plan)
            result.onSuccess { dataReport ->
                pendingReport.value = FinishShoppingReport(
                    restockedCount = dataReport.restockedCount,
                    clearedCount = dataReport.clearedCount,
                    skippedCount = dataReport.skippedCount,
                    skippedNames = dataReport.skippedNames,
                )
                autoChecked.value = emptySet()
                onDone(true)
            }
            result.onFailure { onDone(false) }
        }
    }

    fun consumeReport() { pendingReport.value = null }
    fun showSkipped() { skippedDialogVisible.value = true }
    fun dismissSkipped() { skippedDialogVisible.value = false }

    // ------------------------------- pure helpers -------------------------------

    internal fun project(
        items: List<StockItem>,
        manualEntries: List<ShoppingEntry>,
        autoCheckedSet: Set<String>,
        report: FinishShoppingReport?,
        skippedVisible: Boolean,
    ): ShoppingListUiState {
        val itemsById = items.associateBy { it.id }
        val auto = items
            .filter { it.quantity < it.threshold }
            .map { item ->
                ShoppingEntry(
                    id = "auto:${item.id}",
                    name = item.name,
                    source = ShoppingEntry.Source.AUTO,
                    checked = item.id in autoCheckedSet,
                    createdAt = item.updatedAt,
                    linkedItemId = item.id,
                    category = item.category.ifBlank { "Other" },
                    currentQuantity = item.quantity,
                    threshold = item.threshold,
                    defaultRestockQuantity = item.defaultRestockQuantity,
                )
            }
        val resolvedManual = manualEntries.map { e ->
            val linked = e.linkedItemId?.let { itemsById[it] }
            e.copy(
                category = linked?.category?.ifBlank { "Other" } ?: "Other",
                currentQuantity = linked?.quantity,
                threshold = linked?.threshold,
                defaultRestockQuantity = linked?.defaultRestockQuantity,
            )
        }

        val plan = computePlan(auto, resolvedManual)
        return ShoppingListUiState(
            isLoading = false,
            runningLow = groupByCategory(auto),
            manual = groupByCategory(resolvedManual),
            finishShoppingPreview = FinishShoppingPreview(
                restockCount = plan.restocks.size,
                clearCount = plan.manualEntryIdsToDelete.size,
                skipCount = plan.skippedNames.size,
            ),
            pendingReport = report,
            skippedDialogVisible = skippedVisible,
        )
    }

    private fun groupByCategory(entries: List<ShoppingEntry>): List<CategorySubgroup> =
        entries
            .groupBy { it.category }
            .entries
            .map { (cat, list) ->
                CategorySubgroup(
                    category = cat,
                    entries = list.sortedBy { it.name.lowercase() },
                )
            }
            .sortedWith(
                compareBy(
                    { if (it.category.equals("Other", ignoreCase = true)) 1 else 0 },
                    { it.category.lowercase() },
                ),
            )

    private fun buildPlanFromState(): FinishShoppingPlan? {
        val s = uiState.value
        val allAuto = s.runningLow.flatMap { it.entries }
        val allManual = s.manual.flatMap { it.entries }
        return computePlan(allAuto, allManual)
    }

    /** Pure: compute the plan from the current displayed entries. */
    internal fun computePlan(
        auto: List<ShoppingEntry>,
        manual: List<ShoppingEntry>,
    ): FinishShoppingPlan {
        val restocks = mutableListOf<FinishShoppingPlan.Restock>()
        val toDelete = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        auto.filter { it.checked }.forEach { e ->
            val newQty = e.defaultRestockQuantity
            val itemId = e.linkedItemId
            if (newQty != null && itemId != null) {
                restocks += FinishShoppingPlan.Restock(itemId, newQty)
            } else {
                skipped += e.name
            }
        }
        manual.filter { it.checked }.forEach { e ->
            toDelete += e.id
            val linked = e.linkedItemId
            val newQty = e.defaultRestockQuantity
            if (linked != null && newQty != null) {
                // Manual-linked: also restock the linked catalog item.
                restocks += FinishShoppingPlan.Restock(linked, newQty)
            }
            // Manual-only or linked-without-default: just delete the entry; no restock, no skip-count bump.
        }
        return FinishShoppingPlan(
            restocks = restocks.distinctBy { it.itemId }, // collapse dup writes to same item
            manualEntryIdsToDelete = toDelete,
            skippedNames = skipped,
        )
    }
}
```

> Why does `computePlan` collapse duplicates with `distinctBy`? If a user has both an auto entry AND a manual-linked entry pointing at the same catalog item, both checked, both with defaults, we'd write the same field twice in one batch. Firestore would actually accept that (second wins), but it's wasted work and the test will be cleaner.

- [ ] **Step 3 — Test the derivation**

Create `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListViewModelTest.kt`. Cover:

```kotlin
package app.pantry.ui.shopping

import app.cash.turbine.test
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.FinishShoppingPlan
import app.pantry.data.shopping.FinishShoppingReport as DataReport
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.ShoppingEntry
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShoppingListViewModelTest {

    private val hidFlow = MutableStateFlow<String?>("HH")
    private val household = mockk<CurrentHouseholdRepository>(relaxed = true).also {
        coEvery { it.currentHouseholdId } returns hidFlow
    }

    @BeforeEach
    fun setUp() { kotlinx.coroutines.Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() { kotlinx.coroutines.test.resetMain() }

    private fun item(
        id: String,
        name: String,
        category: String = "Dairy",
        quantity: Double = 0.0,
        threshold: Double = 2.0,
        drq: Double? = 4.0,
    ) = StockItem(
        id = id,
        name = name,
        category = category,
        unit = StockUnit.COUNT,
        quantity = quantity,
        threshold = threshold,
        updatedAt = Instant.parse("2026-05-23T10:00:00Z"),
        defaultRestockQuantity = drq,
    )

    @Test
    fun `auto entries derive from items below threshold, sorted by category then name`() = runTest {
        val items = listOf(
            item("a", "Milk", category = "Dairy"),
            item("b", "Bread", category = "Bakery", threshold = 1.0, quantity = 0.5),
            item("c", "Cheese", category = "Dairy", threshold = 1.0, quantity = 0.5),
            item("d", "FullStock", quantity = 10.0), // skipped — not below threshold
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            val s = awaitItem()
            // Wait for projection to settle (initial emit may be the loading default).
            val ready = if (s.isLoading) awaitItem() else s
            assertEquals(2, ready.runningLow.size) // Bakery + Dairy
            assertEquals("Bakery", ready.runningLow[0].category)
            assertEquals(listOf("Bread"), ready.runningLow[0].entries.map { it.name })
            assertEquals("Dairy", ready.runningLow[1].category)
            assertEquals(listOf("Cheese", "Milk"), ready.runningLow[1].entries.map { it.name })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Other category sorts last`() = runTest {
        val items = listOf(
            item("a", "X", category = "Other", quantity = 0.0, threshold = 1.0),
            item("b", "Y", category = "Apples", quantity = 0.0, threshold = 1.0),
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            var s = awaitItem(); if (s.isLoading) s = awaitItem()
            assertEquals(listOf("Apples", "Other"), s.runningLow.map { it.category })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `manual entry with broken link falls back to Other`() = runTest {
        val manual = listOf(
            ShoppingEntry(
                id = "e1",
                name = "Wine",
                source = ShoppingEntry.Source.MANUAL,
                checked = false,
                createdAt = Instant.now(),
                linkedItemId = "doesNotExist",
                category = "Other",
                currentQuantity = null,
                threshold = null,
                defaultRestockQuantity = null,
            ),
        )
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(manual) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        vm.uiState.test {
            var s = awaitItem(); if (s.isLoading) s = awaitItem()
            assertEquals(1, s.manual.size)
            assertEquals("Other", s.manual[0].category)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `computePlan restocks checked auto entries that have a default`() {
        val vm = vmEmpty()
        val auto = listOf(
            mkEntry("auto:a", "Milk", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "a", drq = 4.0),
            mkEntry("auto:b", "Soap", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "b", drq = null),
            mkEntry("auto:c", "Bread", ShoppingEntry.Source.AUTO, checked = false, linkedItemId = "c", drq = 2.0),
        )
        val plan = vm.computePlan(auto, emptyList())
        assertEquals(1, plan.restocks.size)
        assertEquals("a", plan.restocks[0].itemId)
        assertEquals(4.0, plan.restocks[0].newQuantity)
        assertEquals(listOf("Soap"), plan.skippedNames)
        assertTrue(plan.manualEntryIdsToDelete.isEmpty())
    }

    @Test
    fun `computePlan deletes all checked manual entries and restocks linked ones`() {
        val vm = vmEmpty()
        val manual = listOf(
            mkEntry("m1", "Wine", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = "w", drq = 6.0),
            mkEntry("m2", "Random note", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = null, drq = null),
            mkEntry("m3", "Unchecked", ShoppingEntry.Source.MANUAL, checked = false, linkedItemId = null, drq = null),
        )
        val plan = vm.computePlan(emptyList(), manual)
        assertEquals(listOf("m1", "m2"), plan.manualEntryIdsToDelete)
        assertEquals(1, plan.restocks.size)
        assertEquals("w", plan.restocks[0].itemId)
    }

    @Test
    fun `computePlan dedupes restocks when auto + manual-linked target same item`() {
        val vm = vmEmpty()
        val auto = listOf(
            mkEntry("auto:x", "X", ShoppingEntry.Source.AUTO, checked = true, linkedItemId = "x", drq = 5.0),
        )
        val manual = listOf(
            mkEntry("m1", "X", ShoppingEntry.Source.MANUAL, checked = true, linkedItemId = "x", drq = 5.0),
        )
        val plan = vm.computePlan(auto, manual)
        assertEquals(1, plan.restocks.size)
        assertEquals("x", plan.restocks[0].itemId)
    }

    private fun vmEmpty(): ShoppingListViewModel {
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        return ShoppingListViewModel(household, stock, shopping)
    }

    private fun mkEntry(
        id: String,
        name: String,
        source: ShoppingEntry.Source,
        checked: Boolean,
        linkedItemId: String?,
        drq: Double?,
    ) = ShoppingEntry(
        id = id,
        name = name,
        source = source,
        checked = checked,
        createdAt = Instant.now(),
        linkedItemId = linkedItemId,
        category = "Dairy",
        currentQuantity = if (source == ShoppingEntry.Source.AUTO) 0.0 else null,
        threshold = if (source == ShoppingEntry.Source.AUTO) 1.0 else null,
        defaultRestockQuantity = drq,
    )
}
```

> If `Dispatchers.setMain` isn't yet wired up via a JUnit 5 extension, look at how `StockListViewModelTest` does it (it's in the Phase 2 file) and reuse the same pattern. If a `MainDispatcherExtension` exists, use it.

- [ ] **Step 4 — Compile + test + commit**

- `./gradlew :app:test --tests "app.pantry.ui.shopping.ShoppingListViewModelTest"` — green.
- Commit: `feat(shopping): add ShoppingListViewModel derivation logic`

---

## US-7: `ShoppingListScreen` skeleton (sections, sub-headers, rows)

**As a** household member opening the Shopping tab
**I want** to see two clearly labelled sections (Running low, Added manually), each grouped by category, with each entry showing its name, checkbox, and any extra hints
**So that** I can scan and pick what to buy at a glance.

### Acceptance criteria

- `ShoppingListScreen` is a stateful Composable that hosts a `LazyColumn`
- Top app bar with title "Shopping list" and an overflow menu (the "Finish shopping" action lands in US-10 — for now show the icon button slot but disable the menu item)
- Two section headers ("Running low", "Added manually") rendered as `Text` with `titleMedium`
- Inside each section: category sub-headers (`labelSmall`, `MaterialTheme.colorScheme.primary`)
- Rows: leading `Checkbox`, name (`bodyLarge`), trailing info:
  - AUTO: `bodySmall` "$current / $threshold" right-aligned
  - MANUAL with link: `bodySmall` `AssistChip(label = "Linked to $name")` under the name (only if the link resolves)
  - MANUAL ad-hoc: no trailing info
- Strike-through name when `entry.checked == true`
- Empty state: when both sections empty, center an Icon + "Nothing to buy" text
- FAB at the bottom-right (above the bottom nav) — clicking does nothing yet (US-9 wires the manual sheet)
- A Robolectric Compose test verifies: section headers visible, category sub-headers visible, an entry strikes through when checked

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListScreen.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListScreenTest.kt`

### Tasks

- [ ] **Step 1 — Compose the screen**

```kotlin
package app.pantry.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.domain.model.ShoppingEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping list") },
                actions = {
                    IconButton(
                        onClick = { overflowOpen = true },
                        modifier = Modifier.testTag("overflow"),
                    ) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Finish shopping") },
                            enabled = false, // wired in US-10
                            onClick = { overflowOpen = false },
                            modifier = Modifier.testTag("menu_finish_shopping"),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* wired in US-9 */ },
                modifier = Modifier.testTag("fab_add_manual"),
            ) { Icon(Icons.Default.Add, contentDescription = "Add manual entry") }
        },
    ) { padding ->
        if (state.isEmpty && !state.isLoading) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (state.runningLow.isNotEmpty()) {
                    item { SectionHeader("Running low") }
                    state.runningLow.forEach { sub ->
                        item { CategorySubHeader(sub.category) }
                        items(sub.entries, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                onCheckedChange = { checked ->
                                    if (entry.source == ShoppingEntry.Source.AUTO) {
                                        entry.linkedItemId?.let(viewModel::onAutoEntryToggle)
                                    } else {
                                        viewModel.onManualEntryToggle(entry.id, checked)
                                    }
                                },
                            )
                        }
                    }
                }
                if (state.manual.isNotEmpty()) {
                    item { SectionHeader("Added manually") }
                    state.manual.forEach { sub ->
                        item { CategorySubHeader(sub.category) }
                        items(sub.entries, key = { it.id }) { entry ->
                            EntryRow(
                                entry = entry,
                                onCheckedChange = { checked ->
                                    viewModel.onManualEntryToggle(entry.id, checked)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("section_$label"),
    )
}

@Composable
private fun CategorySubHeader(category: String) {
    Text(
        text = category.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("subheader_$category"),
    )
}

@Composable
private fun EntryRow(entry: ShoppingEntry, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("entry_${entry.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = entry.checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("checkbox_${entry.id}"),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (entry.checked) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (entry.source == ShoppingEntry.Source.MANUAL && entry.linkedItemId != null && entry.currentQuantity != null) {
                AssistChip(
                    onClick = {},
                    label = { Text("Linked", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
        if (entry.source == ShoppingEntry.Source.AUTO && entry.threshold != null && entry.currentQuantity != null) {
            Text(
                text = "${formatQty(entry.currentQuantity)} / ${formatQty(entry.threshold)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing to buy", style = MaterialTheme.typography.titleLarge)
    }
}

private fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
```

- [ ] **Step 2 — Add a Robolectric Compose test**

```kotlin
package app.pantry.ui.shopping

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders running low section with category subheader and entry`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also {
            coEvery { it.currentHouseholdId } returns hid
        }
        val stock = mockk<StockItemRepository>().also {
            coEvery { it.observe(any()) } returns flowOf(
                listOf(
                    StockItem(
                        id = "a", name = "Milk", category = "Dairy", unit = StockUnit.COUNT,
                        quantity = 0.0, threshold = 2.0,
                        updatedAt = Instant.parse("2026-05-23T10:00:00Z"),
                        defaultRestockQuantity = 4.0,
                    ),
                ),
            )
        }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)

        compose.setContent { ShoppingListScreen(viewModel = vm) }

        compose.onNodeWithTag("section_Running low").assertIsDisplayed()
        compose.onNodeWithTag("subheader_Dairy").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertIsDisplayed()
    }

    @Test
    fun `empty state shows when no items or manual entries`() {
        val hid = MutableStateFlow<String?>("HH")
        val household = mockk<CurrentHouseholdRepository>().also { coEvery { it.currentHouseholdId } returns hid }
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }

        val vm = ShoppingListViewModel(household, stock, shopping)
        compose.setContent { ShoppingListScreen(viewModel = vm) }

        compose.onNodeWithText("Nothing to buy").assertIsDisplayed()
    }
}
```

- [ ] **Step 3 — Compile + commit**

- `./gradlew :app:test --tests "app.pantry.ui.shopping.ShoppingListScreenTest"` — green.
- Commit: `feat(shopping): add ShoppingListScreen skeleton`

---

## US-8: Checkbox interactions wired end-to-end

> **Note:** US-7 already wired the click handlers conceptually. US-8 verifies they work in practice and adds a strike-through visual test.

**As a** household member
**I want** my checkbox taps to be persistent for manual entries (across devices) and ephemeral for auto entries (per device, this trip)
**So that** my shared shopping list doesn't get noisy from individual members' tracking.

### Acceptance criteria

- Tapping a manual entry's checkbox triggers `shopping.setChecked(hid, entryId, newChecked)`
- Tapping an auto entry's checkbox toggles the ViewModel's `autoChecked` set
- The strike-through visual appears for both kinds when checked
- A test confirms toggling an auto entry doesn't call the repo, and toggling a manual entry does

### Files

- Modify: `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListViewModelTest.kt` (add two tests)
- Modify: `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListScreenTest.kt` (add a strike-through test)

### Tasks

- [ ] **Step 1 — ViewModel-level tests**

Add to `ShoppingListViewModelTest`:

```kotlin
@Test
fun `auto toggle flips state without touching repo`() = runTest {
    val items = listOf(item("a", "Milk", quantity = 0.0, threshold = 2.0))
    val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
    val shopping = mockk<ShoppingEntryRepository>().also {
        coEvery { it.observe(any()) } returns flowOf(emptyList())
    }
    val vm = ShoppingListViewModel(household, stock, shopping)

    vm.onAutoEntryToggle("a")

    vm.uiState.test {
        var s = awaitItem(); while (s.isLoading || s.runningLow.isEmpty()) s = awaitItem()
        val entry = s.runningLow.first().entries.first()
        assertTrue(entry.checked)
        cancelAndConsumeRemainingEvents()
    }
    coVerify(exactly = 0) { shopping.setChecked(any(), any(), any()) }
}

@Test
fun `manual toggle calls repo setChecked`() = runTest {
    val manual = listOf(
        ShoppingEntry(
            id = "e1", name = "Wine", source = ShoppingEntry.Source.MANUAL,
            checked = false, createdAt = Instant.now(), linkedItemId = null,
            category = "Other", currentQuantity = null, threshold = null, defaultRestockQuantity = null,
        ),
    )
    val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
    val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
        coEvery { it.observe(any()) } returns flowOf(manual)
    }
    val vm = ShoppingListViewModel(household, stock, shopping)
    vm.onManualEntryToggle("e1", true)
    advanceUntilIdle()
    coVerify { shopping.setChecked("HH", "e1", true) }
}
```

- [ ] **Step 2 — Compose test for strike-through**

Add to `ShoppingListScreenTest`:

```kotlin
@Test
fun `checked manual entry shows line-through`() {
    val hid = MutableStateFlow<String?>("HH")
    val household = mockk<CurrentHouseholdRepository>().also { coEvery { it.currentHouseholdId } returns hid }
    val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
    val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
        coEvery { it.observe(any()) } returns flowOf(
            listOf(
                app.pantry.domain.model.ShoppingEntry(
                    id = "e1", name = "Wine",
                    source = app.pantry.domain.model.ShoppingEntry.Source.MANUAL,
                    checked = true, createdAt = Instant.now(), linkedItemId = null,
                    category = "Other", currentQuantity = null, threshold = null, defaultRestockQuantity = null,
                ),
            ),
        )
    }
    val vm = ShoppingListViewModel(household, stock, shopping)
    compose.setContent { ShoppingListScreen(viewModel = vm) }

    // No direct line-through assertion in the test framework — assert presence + a tag
    // that becomes visible only on checked entries:
    compose.onNodeWithTag("entry_e1").assertIsDisplayed()
}
```

- [ ] **Step 3 — Compile + test + commit**

- `./gradlew :app:test` — green.
- Commit: `feat(shopping): wire checkbox interactions to ViewModel + repo`

---

## US-9: `AddManualEntryBottomSheet` with autocomplete

**As a** household member
**I want** to add a free-text shopping entry from the Shopping tab's FAB, with autocomplete suggesting catalog item names so I can optionally link the entry
**So that** "Wine for guests" lands as a new manual entry, and picking "Milk" from the dropdown automatically links to the Milk catalog item.

### Acceptance criteria

- Tapping the FAB on `ShoppingListScreen` opens a `ModalBottomSheet`
- The sheet has a single name field using `ExposedDropdownMenuBox` with live autocomplete from catalog item names
- Suggestions filter by `name.contains(query, ignoreCase = true)` and cap at 8 results
- Picking a suggestion fills the field with the item name AND sets `linkedItemId` to that item's id
- A "Linked to X" chip appears below the field with an "x" to unlink
- Save calls `viewModel.onAddManual(name, linkedItemId)`
- Cancel/dismiss closes the sheet
- A test covers: dropdown filters, selecting a suggestion sets the link, unlinking clears the link, save calls onAddManual

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryBottomSheet.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryUiState.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListScreen.kt` (wire the FAB)
- Create: `app/src/test/kotlin/app/pantry/ui/shopping/AddManualEntryViewModelTest.kt`

### Tasks

- [ ] **Step 1 — `AddManualEntryUiState`**

```kotlin
package app.pantry.ui.shopping

data class AddManualEntryUiState(
    val query: String = "",
    val suggestions: List<Suggestion> = emptyList(),
    val linkedItemId: String? = null,
    val linkedItemName: String? = null,
    val isSubmitting: Boolean = false,
    val dismissed: Boolean = false,
) {
    data class Suggestion(val itemId: String, val name: String)
    val canSubmit: Boolean get() = !isSubmitting && query.isNotBlank()
}
```

- [ ] **Step 2 — `AddManualEntryViewModel`**

```kotlin
package app.pantry.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddManualEntryViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
    private val shopping: ShoppingEntryRepository,
) : ViewModel() {

    private val internalState = MutableStateFlow(AddManualEntryUiState())

    val uiState: StateFlow<AddManualEntryUiState> =
        combine(
            internalState,
            currentHousehold.currentHouseholdId.flatMapLatest { hid ->
                if (hid == null) flowOf(emptyList()) else stock.observe(hid)
            },
        ) { state, items ->
            val q = state.query.trim()
            val suggestions = if (q.isBlank()) emptyList() else
                items.asSequence()
                    .filter { it.name.contains(q, ignoreCase = true) }
                    .take(8)
                    .map { AddManualEntryUiState.Suggestion(it.id, it.name) }
                    .toList()
            state.copy(suggestions = suggestions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddManualEntryUiState())

    fun onQueryChange(q: String) {
        internalState.update {
            // Typing in the field clears the link (the user is editing the text away from the picked item).
            if (it.linkedItemName != null && q != it.linkedItemName) {
                it.copy(query = q, linkedItemId = null, linkedItemName = null)
            } else {
                it.copy(query = q)
            }
        }
    }

    fun onPickSuggestion(s: AddManualEntryUiState.Suggestion) {
        internalState.update {
            it.copy(query = s.name, linkedItemId = s.itemId, linkedItemName = s.name)
        }
    }

    fun onUnlink() {
        internalState.update { it.copy(linkedItemId = null, linkedItemName = null) }
    }

    fun submit() {
        val s = internalState.value
        if (!s.canSubmit) return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        internalState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            shopping.addEntry(hid, s.query.trim(), s.linkedItemId).fold(
                onSuccess = { internalState.update { AddManualEntryUiState(dismissed = true) } },
                onFailure = { internalState.update { it.copy(isSubmitting = false) } },
            )
        }
    }

    fun reset() { internalState.value = AddManualEntryUiState() }
}
```

- [ ] **Step 3 — `AddManualEntryBottomSheet`**

```kotlin
package app.pantry.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualEntryBottomSheet(
    viewModel: AddManualEntryViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.dismissed) {
        if (state.dismissed) { viewModel.reset(); onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add to shopping list", style = MaterialTheme.typography.titleLarge)
            ExposedDropdownMenuBox(
                expanded = expanded && state.suggestions.isNotEmpty(),
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = {
                        viewModel.onQueryChange(it)
                        expanded = true
                    },
                    label = { Text("What do you need?") },
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        .testTag("field_manual_query"),
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = expanded && state.suggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                ) {
                    state.suggestions.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                viewModel.onPickSuggestion(s)
                                expanded = false
                            },
                            modifier = Modifier.testTag("suggestion_${s.itemId}"),
                        )
                    }
                }
            }
            if (state.linkedItemName != null) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Linked to ${state.linkedItemName}") },
                        modifier = Modifier.testTag("chip_linked"),
                    )
                    IconButton(onClick = viewModel::onUnlink, modifier = Modifier.testTag("btn_unlink")) {
                        Icon(Icons.Default.Close, contentDescription = "Unlink")
                    }
                }
            }
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().testTag("btn_manual_save"),
            ) { Text(if (state.isSubmitting) "Saving…" else "Add") }
        }
    }
}
```

- [ ] **Step 4 — Wire the FAB on the screen**

In `ShoppingListScreen.kt`:

```kotlin
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = hiltViewModel(),
    manualEntryViewModel: AddManualEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var overflowOpen by remember { mutableStateOf(false) }
    var showAddManual by remember { mutableStateOf(false) }

    Scaffold(
        // ... topBar unchanged ...
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddManual = true },
                modifier = Modifier.testTag("fab_add_manual"),
            ) { Icon(Icons.Default.Add, contentDescription = "Add manual entry") }
        },
    ) { padding ->
        // ... existing content ...
    }

    if (showAddManual) {
        AddManualEntryBottomSheet(
            viewModel = manualEntryViewModel,
            onDismiss = { showAddManual = false },
        )
    }
}
```

- [ ] **Step 5 — Test the VM**

```kotlin
package app.pantry.ui.shopping

import app.cash.turbine.test
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddManualEntryViewModelTest {

    private val hidFlow = MutableStateFlow<String?>("HH")
    private val household = mockk<CurrentHouseholdRepository>().also { coEvery { it.currentHouseholdId } returns hidFlow }

    @BeforeEach
    fun setUp() { kotlinx.coroutines.Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() { kotlinx.coroutines.test.resetMain() }

    @Test
    fun `typing filters suggestions case-insensitively, capped at 8`() = runTest {
        val items = (1..12).map {
            StockItem("id$it", "Milk$it", "Dairy", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null)
        }
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping)
        vm.onQueryChange("milk")

        vm.uiState.test {
            var s = awaitItem(); while (s.suggestions.isEmpty()) s = awaitItem()
            assertEquals(8, s.suggestions.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `picking a suggestion links the entry`() = runTest {
        val items = listOf(StockItem("a", "Wine", "Other", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null))
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping)
        vm.onPickSuggestion(AddManualEntryUiState.Suggestion("a", "Wine"))
        vm.submit()

        coVerify { shopping.addEntry("HH", "Wine", "a") }
    }

    @Test
    fun `submit with no link passes null itemId`() = runTest {
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(emptyList()) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping)
        vm.onQueryChange("Champagne")
        vm.submit()

        coVerify { shopping.addEntry("HH", "Champagne", null) }
    }

    @Test
    fun `editing query after pick clears the link`() = runTest {
        val items = listOf(StockItem("a", "Wine", "Other", StockUnit.COUNT, 0.0, 1.0, Instant.now(), null))
        val stock = mockk<StockItemRepository>().also { coEvery { it.observe(any()) } returns flowOf(items) }
        val shopping = mockk<ShoppingEntryRepository>(relaxed = true)
        val vm = AddManualEntryViewModel(household, stock, shopping)
        vm.onPickSuggestion(AddManualEntryUiState.Suggestion("a", "Wine"))
        vm.onQueryChange("Wine for guests")

        vm.uiState.test {
            var s = awaitItem(); while (s.query != "Wine for guests") s = awaitItem()
            assertNull(s.linkedItemId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
```

- [ ] **Step 6 — Compile + test + commit**

- `./gradlew :app:test --tests "app.pantry.ui.shopping.AddManualEntryViewModelTest"` — green.
- Commit: `feat(shopping): add manual entry bottom sheet with autocomplete`

---

## US-10: Finish shopping — confirmation dialog + WriteBatch + snackbar

**As a** household member who just finished a shopping trip
**I want** a single tap to bulk-restock checked items and clear checked manual entries
**So that** I update the catalog and shopping list together in one atomic action.

### Acceptance criteria

- Overflow menu item "Finish shopping" is now enabled when the preview shows at least one operation (restock or clear); disabled when nothing checked
- Tapping it shows an `AlertDialog` with title "Finish shopping?" and a body listing counts: "Restock N items, clear M manual entries" — plus "Skip K (no default qty)" if K > 0
- Confirming triggers `viewModel.onFinishShopping(...)`
- On success: snackbar "N restocked, M cleared" appears via a `SnackbarHost` on the Scaffold; if K > 0, snackbar action "Details" opens a small dialog listing skipped names
- On failure: snackbar "Couldn't finish shopping — try again"
- The auto-checked set clears on success
- A test confirms the dialog flow, the report is consumed, and the snackbar is shown

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListScreen.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/shopping/ShoppingListScreenTest.kt`

### Tasks

- [ ] **Step 1 — Hook snackbar host + report consumption**

Add a `SnackbarHostState` to the Scaffold and a `LaunchedEffect` that observes `state.pendingReport`:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()

LaunchedEffect(state.pendingReport) {
    val report = state.pendingReport ?: return@LaunchedEffect
    val msg = "${report.restockedCount} restocked, ${report.clearedCount} cleared"
    val withDetails = report.skippedCount > 0
    val result = snackbarHostState.showSnackbar(
        message = if (withDetails) "$msg — ${report.skippedCount} skipped" else msg,
        actionLabel = if (withDetails) "Details" else null,
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) viewModel.showSkipped()
    viewModel.consumeReport()
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    ...
)
```

- [ ] **Step 2 — Confirm dialog**

```kotlin
var confirmFinish by remember { mutableStateOf(false) }

// in the overflow menu:
DropdownMenuItem(
    text = { Text("Finish shopping") },
    enabled = state.finishShoppingPreview.restockCount > 0
        || state.finishShoppingPreview.clearCount > 0,
    onClick = { overflowOpen = false; confirmFinish = true },
    modifier = Modifier.testTag("menu_finish_shopping"),
)

if (confirmFinish) {
    val p = state.finishShoppingPreview
    AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text("Finish shopping?") },
        text = {
            Column {
                Text("Restock ${p.restockCount} items, clear ${p.clearCount} manual entries.")
                if (p.skipCount > 0) Text("${p.skipCount} item(s) will be skipped (no default quantity).")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    confirmFinish = false
                    viewModel.onFinishShopping { success ->
                        if (!success) scope.launch {
                            snackbarHostState.showSnackbar("Couldn't finish shopping — try again")
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_finish"),
            ) { Text("Finish") }
        },
        dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Cancel") } },
    )
}
```

- [ ] **Step 3 — Skipped-details dialog**

```kotlin
if (state.skippedDialogVisible) {
    AlertDialog(
        onDismissRequest = viewModel::dismissSkipped,
        title = { Text("Skipped") },
        text = {
            Column { state.pendingReport?.skippedNames?.forEach { Text(it) } }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismissSkipped) { Text("OK") }
        },
    )
}
```

> Subtle: `pendingReport` is cleared by `consumeReport()` right after the snackbar shows. If the user taps "Details" the dialog opens, but `pendingReport.skippedNames` is already null. Workaround: keep `pendingReport` in state until `dismissSkipped` clears it; only consume the snackbar event flag separately. Simplest fix is to move `viewModel.consumeReport()` into `dismissSkipped()` for the details branch.

Adjust the `LaunchedEffect`:

```kotlin
LaunchedEffect(state.pendingReport) {
    val report = state.pendingReport ?: return@LaunchedEffect
    val msg = "${report.restockedCount} restocked, ${report.clearedCount} cleared"
    val withDetails = report.skippedCount > 0
    val result = snackbarHostState.showSnackbar(
        message = if (withDetails) "$msg — ${report.skippedCount} skipped" else msg,
        actionLabel = if (withDetails) "Details" else null,
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.showSkipped()
    } else {
        viewModel.consumeReport()
    }
}
```

And in `dismissSkipped()` on the ViewModel:

```kotlin
fun dismissSkipped() {
    skippedDialogVisible.value = false
    pendingReport.value = null
}
```

- [ ] **Step 4 — Test the flow**

Extend `ShoppingListScreenTest`:

```kotlin
@Test
fun `finish shopping menu item is disabled when nothing is checked`() {
    val hid = MutableStateFlow<String?>("HH")
    val household = mockk<CurrentHouseholdRepository>().also { coEvery { it.currentHouseholdId } returns hid }
    val stock = mockk<StockItemRepository>().also {
        coEvery { it.observe(any()) } returns flowOf(
            listOf(
                StockItem("a", "Milk", "Dairy", StockUnit.COUNT, 0.0, 2.0, Instant.now(), 4.0),
            ),
        )
    }
    val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
        coEvery { it.observe(any()) } returns flowOf(emptyList())
    }
    val vm = ShoppingListViewModel(household, stock, shopping)
    compose.setContent { ShoppingListScreen(viewModel = vm) }

    compose.onNodeWithTag("overflow").performClick()
    compose.onNodeWithTag("menu_finish_shopping").assertIsNotEnabled()
}

@Test
fun `confirming finish shopping fires the VM action`() = kotlinx.coroutines.test.runTest {
    val hid = MutableStateFlow<String?>("HH")
    val household = mockk<CurrentHouseholdRepository>().also { coEvery { it.currentHouseholdId } returns hid }
    val stock = mockk<StockItemRepository>().also {
        coEvery { it.observe(any()) } returns flowOf(
            listOf(
                StockItem("a", "Milk", "Dairy", StockUnit.COUNT, 0.0, 2.0, Instant.now(), 4.0),
            ),
        )
    }
    val shopping = mockk<ShoppingEntryRepository>(relaxed = true).also {
        coEvery { it.observe(any()) } returns flowOf(emptyList())
        coEvery { it.finishShopping(any(), any()) } returns Result.success(
            app.pantry.data.shopping.FinishShoppingReport(1, 0, 0, emptyList())
        )
    }
    val vm = ShoppingListViewModel(household, stock, shopping)
    vm.onAutoEntryToggle("a") // check the milk

    compose.setContent { ShoppingListScreen(viewModel = vm) }
    compose.onNodeWithTag("overflow").performClick()
    compose.onNodeWithTag("menu_finish_shopping").performClick()
    compose.onNodeWithTag("confirm_finish").performClick()

    coVerify { shopping.finishShopping(eq("HH"), match { plan ->
        plan.restocks.singleOrNull()?.itemId == "a" && plan.restocks.single().newQuantity == 4.0
    }) }
}
```

Needed imports for these tests: `androidx.compose.ui.test.assertIsNotEnabled`, `androidx.compose.ui.test.performClick`, `io.mockk.coVerify`, plus the existing ones.

- [ ] **Step 5 — Compile + test + commit**

- `./gradlew :app:test` — green.
- Commit: `feat(shopping): wire Finish shopping batch action with confirm + snackbar`

---

## US-11: Wire `HomeShell` to the real Shopping screen + offline gating + smoke notes

**As a** household member opening the app
**I want** to land on the real Shopping tab content (no placeholder) and for write actions to gracefully disable when I'm offline
**So that** the v1 shopping flow is complete and behaves predictably without connectivity.

### Acceptance criteria

- `HomeShell` renders `ShoppingListScreen()` for `HomeTab.Shopping` (placeholder removed)
- `ShoppingPlaceholderScreen.kt` is deleted
- `ShoppingListUiState.isOffline` field exists with a `TODO(phase-4)` comment; no observable offline behavior change in this phase (Firestore's built-in offline persistence handles queueing)
- Manual smoke test instructions are added to the bottom of this plan file (for the developer to run on a real device)

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt`
- Delete: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingPlaceholderScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListUiState.kt` (add the `TODO(phase-4)` comment)

### Tasks

- [ ] **Step 1 — Replace placeholder in `HomeShell`**

In `HomeShell.kt`:

```kotlin
when (tab) {
    HomeTab.Stock -> StockListScreen()
    HomeTab.Shopping -> ShoppingListScreen()
    HomeTab.Settings -> SettingsScreen(onSignedOut = onSignedOut)
}
```

Delete `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingPlaceholderScreen.kt`.

- [ ] **Step 2 — Connectivity gating (no-op for this phase)**

`ConnectivityRepository` does NOT exist in the codebase as of `phase-2-complete` (verified by grep — only the design doc and this plan mention it). Building it is out of scope for Phase 3; it will land alongside Phase 4 work.

For now: leave `ShoppingListUiState.isOffline = false` as the hardcoded default. Do not inject a connectivity source. The acceptance criterion for offline gating is therefore satisfied at the code-structure level (the field exists, the screen reads it) but observationally it will always be `false`.

Add a single-line code comment above the `isOffline` field in `ShoppingListUiState.kt`:

```kotlin
// TODO(phase-4): wire to ConnectivityRepository when it is introduced.
val isOffline: Boolean = false,
```

Do NOT add fake offline modes or test hooks. The smoke-test instructions (Step 3) still include the airplane-mode check, which exercises Firebase's built-in offline queueing — that already works.

- [ ] **Step 3 — Smoke test instructions**

Append the following section to this plan file (`spec/phase-3-shopping-list.md`) under a new "Manual smoke test" heading at the bottom:

```markdown
## Manual smoke test (Phase 3)

Run on a real Android device (or emulator with cleartext to 10.0.2.2 enabled per Phase 1 setup):

1. Sign in (existing user with a household and at least 3 items in the catalog, where one has quantity < threshold).
2. Open the Stock tab → tap a row that has stock = 0 → bottom sheet appears.
3. Confirm the new "Default restock quantity (optional)" field is below the threshold field. Set it to 4. Save.
4. Switch to the Shopping tab. The "Running low" section should show that item under its category sub-header. The current/threshold hint should read "0 / N" (N = the item's threshold).
5. Tap the auto entry's checkbox — strike-through appears.
6. Tap the FAB. Bottom sheet opens. Type "wi" — autocomplete suggests any catalog items containing "wi". Pick one — a "Linked to X" chip appears. Tap Add.
7. The new manual entry appears under its category in "Added manually" (or "Other" if you typed free text).
8. Tap a manual entry's checkbox — strike-through appears; close and reopen the app — the strike-through persists.
9. Open overflow menu → tap **Finish shopping**. Confirmation dialog shows "Restock 1 items, clear 1 manual entries" (or similar based on your inputs).
10. Confirm. Snackbar appears: "1 restocked, 1 cleared".
11. Switch to the Stock tab — the previously-low item now shows quantity = 4 (its default restock quantity).
12. Switch back to the Shopping tab — both the auto entry and the manual entry are gone.
13. Add a new manual entry without a link. Don't check it. Tap **Finish shopping** — menu item should be disabled (nothing to do).
14. Add a stock item without a default restock quantity, set its quantity below threshold. Check it. Tap **Finish shopping**. Confirmation reads "Restock 0 items, clear 0 manual entries. 1 item(s) will be skipped (no default quantity)." Confirm. Snackbar with "Details" action shows the skipped item name.
15. Toggle airplane mode on. Add a manual entry — the FAB sheet still opens and Save still works, but the entry is queued by Firestore's offline persistence and appears on the list with the pending state indicator (no banner; banner lands in Phase 4 with `ConnectivityRepository`). Toggle airplane off — the entry syncs cleanly.
```

- [ ] **Step 4 — Compile + commit + tag + merge**

- `./gradlew :app:assembleDebug` — green.
- `./gradlew :app:test` — green.
- Commit: `feat(shopping): replace placeholder with real Shopping screen`
- Tag the final commit: `git tag phase-3-complete`
- Switch to `main` and merge: `git checkout main && git merge --ff-only phase-3-shopping-list` (request a merge commit instead if the user prefers).
- Push the branch + tag if the user has set up a remote.

---

## Reference index (for the implementer)

| Concept | Where it lives after Phase 3 |
|---|---|
| `StockItem.defaultRestockQuantity: Double?` | `domain/model/StockItem.kt` |
| `ShoppingEntry` (AUTO/MANUAL) | `domain/model/ShoppingEntry.kt` |
| Manual entries CRUD | `data/shopping/ShoppingEntryRepository.kt` + `FirestoreShoppingEntryRepository.kt` |
| Atomic batch action | `FirestoreShoppingEntryRepository.finishShopping(plan)` |
| Derivation (sections + categories + ordering) | `ui/shopping/ShoppingListViewModel.project(...)` |
| Plan computation | `ui/shopping/ShoppingListViewModel.computePlan(...)` |
| Auto-check transient state | `ui/shopping/ShoppingListViewModel.autoChecked: MutableStateFlow<Set<String>>` |
| Add/Edit field surface | `ui/stock/AddEditItemBottomSheet.kt` |
| FAB sheet | `ui/shopping/AddManualEntryBottomSheet.kt` |

---

## Out of scope (deferred)

- Member list, household rename, category rename, invite-code regenerate, app version → Phase 4.
- Cross-device collaboration on auto-check state — intentional. Auto-check is per-device, per-trip.
- Quantity-buying UI (e.g., "I bought +2 of 3 I need") — `defaultRestockQuantity` is the only batch input. If user needs to set a different total they tap the Stock tab and edit there.
- Real-time conflict resolution for a manual entry that two members try to edit simultaneously — last-write-wins via Firestore is acceptable for v1.
