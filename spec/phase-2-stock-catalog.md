# Phase 2 — Stock Catalog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Pantry build where a user can browse their household's stock catalog, add/edit/delete items, step quantities inline, see low-stock badges, and filter by category and search. Bottom navigation shell hosts Stock (real screen), Shopping (placeholder), Settings (sign-out + household info).

**Architecture:** A single `households/{hid}/items/{itemId}` Firestore subcollection backs all stock items. `StockItemRepository` exposes a `Flow<List<StockItem>>` via `callbackFlow` + Firestore snapshot listeners. `CurrentHouseholdRepository` derives the current household ID from `AuthRepository.currentUser` + `HouseholdRepository.observeUserHouseholds`. The Stock screen consumes a `StockListUiState` produced by a `@HiltViewModel`, with category filtering and search performed client-side over the full item list (households are small; v1 has no pagination). Optimistic UI for the +/- stepper uses `FieldValue.increment(±1)` and reverts on write failure. Bottom sheets for add/edit are full-modal Material 3 `ModalBottomSheet`s with a shared composable that takes either an empty initial state (add) or a pre-filled state (edit + delete).

**Tech stack:** Same as Phase 1 — Kotlin 2.1.0 · AGP 8.7.0 · Compose BoM 2025.01.00 · Material 3 · Hilt 2.53 · Firebase BoM 33.7.0 · Robolectric 4.13 · JUnit 5 · Turbine · MockK · Firebase Local Emulator Suite. No new top-level dependencies needed.

**Spec source:** `spec/android-app-design.md`
**Plan location:** `spec/phase-2-stock-catalog.md`
**Project root:** `Repositories/pantry/`
**Package:** `app.pantry`
**Starts from:** tag `phase-1-complete` (commit `f87ab9d` or later on `main`)

---

## Definition of Done (Phase 2)

- `./gradlew :app:assembleDebug` produces an installable APK
- A signed-in user with a household lands on the Stock tab of a bottom-nav shell (Stock / Shopping / Settings)
- The user can: add an item, edit an item, delete an item with confirmation, step the quantity with `−`/`+`, search by item name, filter by category, see a low-stock ⚠ + red qty for items where `quantity < threshold`, see qty-0 items de-emphasised
- Sign-out lives on the Settings tab (replaces the placeholder Sign-Out button on the Home screen)
- Shopping tab shows a placeholder "Coming soon" screen
- All unit + Compose Robolectric tests pass: `./gradlew :app:test`
- Firestore security rules updated to allow item subcollection writes per household member
- All commits follow Conventional Commits

---

## User stories

| ID | Title | Type |
|---|---|---|
| US-1 | Bottom navigation shell + Settings sign-out | infra |
| US-2 | StockItem + StockUnit domain model | infra |
| US-3 | StockItemRepository interface | infra |
| US-4 | FirestoreStockItemRepository + Hilt + tests | infra |
| US-5 | CurrentHouseholdRepository | infra |
| US-6 | StockListUiState + StockListViewModel | infra |
| US-7 | StockListScreen (basic list) | user-facing |
| US-8 | Add item bottom sheet | user-facing |
| US-9 | Edit item bottom sheet + delete | user-facing |
| US-10 | Inline +/- stepper (optimistic UI) | user-facing |
| US-11 | Search field | user-facing |
| US-12 | Category chips with filter | user-facing |
| US-13 | Low-stock badge + qty-0 styling | user-facing |

---

## US-1: Bottom navigation shell + Settings sign-out

**As a** signed-in user with a household
**I want** a bottom navigation bar with Stock / Shopping / Settings tabs
**So that** I have a stable place for the app's main features and can sign out from a discoverable Settings tab.

### Acceptance criteria

- The `Home` route hosts a `HomeShell` composable with a Material 3 `NavigationBar` at the bottom
- Three tabs: **Stock** (default, selected on entry), **Shopping**, **Settings**
- Tapping a tab switches the screen content above the bar; tab state is preserved across configuration changes via `rememberSaveable`
- The Stock and Shopping tabs render `"Coming soon"` placeholders (real Stock content lands in US-7)
- The Settings tab shows the household name, the invite code (selectable text), and a "Sign out" button
- After sign-out, navigation clears the back stack and returns to Auth (existing behaviour)
- A Compose Robolectric test confirms: shell defaults to Stock, tab clicks switch content, Sign Out fires

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/home/HomeTab.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/settings/SettingsUiState.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingPlaceholderScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt` (Home route now hosts `HomeShell`)
- Delete: `app/src/main/kotlin/app/pantry/ui/home/HomePlaceholderScreen.kt` (replaced by HomeShell)
- Delete: `app/src/main/kotlin/app/pantry/ui/home/HomeViewModel.kt` (moved into `SettingsViewModel`)
- Delete: `app/src/test/kotlin/app/pantry/ui/home/HomePlaceholderScreenTest.kt` (replaced by HomeShellTest + SettingsScreenTest)
- Create: `app/src/test/kotlin/app/pantry/ui/home/HomeShellTest.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/settings/SettingsViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define the tab enum**

Create `app/src/main/kotlin/app/pantry/ui/home/HomeTab.kt`:

```kotlin
package app.pantry.ui.home

enum class HomeTab(val label: String) {
    Stock("Stock"),
    Shopping("Shopping"),
    Settings("Settings"),
    ;

    companion object { val Default = Stock }
}
```

- [ ] **Step 2 — Define `SettingsUiState`**

Create `app/src/main/kotlin/app/pantry/ui/settings/SettingsUiState.kt`:

```kotlin
package app.pantry.ui.settings

data class SettingsUiState(
    val householdName: String = "",
    val inviteCode: String = "",
    val signedOut: Boolean = false,
)
```

- [ ] **Step 3 — Implement `SettingsViewModel`**

Create `app/src/main/kotlin/app/pantry/ui/settings/SettingsViewModel.kt`:

```kotlin
package app.pantry.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firstHousehold = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else households.observeUserHouseholds(profile.uid)
        }
        .map { it.firstOrNull() }

    private val _signedOut = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = firstHousehold
        .map { hh ->
            SettingsUiState(
                householdName = hh?.name.orEmpty(),
                inviteCode = hh?.inviteCode.orEmpty(),
                signedOut = _signedOut.value,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun signOut() {
        viewModelScope.launch {
            val result = auth.signOut()
            if (result.isSuccess) _signedOut.value = true
        }
    }

    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()
}
```

- [ ] **Step 4 — Write failing `SettingsViewModelTest`**

Create `app/src/test/kotlin/app/pantry/ui/settings/SettingsViewModelTest.kt`:

```kotlin
package app.pantry.ui.settings

import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val householdRepo: HouseholdRepository = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepo.currentUser } returns MutableStateFlow(UserProfile("u-1", "Alice", "a@b.com"))
        every { householdRepo.observeUserHouseholds("u-1") } returns
            flowOf(listOf(Household("h-1", "Casa", listOf("u-1"), "ABCDEF")))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes household name and invite code`() = runTest {
        val vm = SettingsViewModel(authRepo, householdRepo)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("Casa", state.householdName)
        assertEquals("ABCDEF", state.inviteCode)
    }

    @Test
    fun `signOut sets signedOut on success`() = runTest {
        coEvery { authRepo.signOut() } returns Result.success(Unit)
        val vm = SettingsViewModel(authRepo, householdRepo)
        vm.signOut()
        advanceUntilIdle()
        assertTrue(vm.signedOut.value)
    }

    @Test
    fun `signOut does not set signedOut on failure`() = runTest {
        coEvery { authRepo.signOut() } returns Result.failure(RuntimeException("network"))
        val vm = SettingsViewModel(authRepo, householdRepo)
        vm.signOut()
        advanceUntilIdle()
        assertEquals(false, vm.signedOut.value)
    }
}
```

- [ ] **Step 5 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.ui.settings.SettingsViewModelTest"
```

Expected: FAIL — `SettingsViewModel` will not compile until `SettingsUiState` and the methods are wired. (After step 3 the class exists; this step re-runs to verify the tests pass.)

- [ ] **Step 6 — Run; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.settings.SettingsViewModelTest"
```

Expected: PASS (3 tests).

- [ ] **Step 7 — Implement `SettingsScreen`**

Create `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`:

```kotlin
package app.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val signedOut by viewModel.signedOut.collectAsState()

    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Household", style = MaterialTheme.typography.labelLarge)
        Text(state.householdName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("household_name"))
        Spacer(Modifier.height(8.dp))
        Text("Invite code", style = MaterialTheme.typography.labelLarge)
        Text(
            state.inviteCode,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.testTag("invite_code"),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::signOut,
            modifier = Modifier.testTag("settings_signout"),
        ) { Text("Sign out") }
    }
}
```

- [ ] **Step 8 — Implement `ShoppingPlaceholderScreen`**

Create `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingPlaceholderScreen.kt`:

```kotlin
package app.pantry.ui.shopping

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ShoppingPlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Shopping — coming soon", style = MaterialTheme.typography.titleLarge)
    }
}
```

- [ ] **Step 9 — Implement `HomeShell`**

Create `app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt`:

```kotlin
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.pantry.ui.settings.SettingsScreen
import app.pantry.ui.shopping.ShoppingPlaceholderScreen

@Composable
fun HomeShell(onSignedOut: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Default) }

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
                HomeTab.Stock -> StockTabPlaceholder()  // replaced by real Stock screen in US-7
                HomeTab.Shopping -> ShoppingPlaceholderScreen()
                HomeTab.Settings -> SettingsScreen(onSignedOut = onSignedOut)
            }
        }
    }
}

@Composable
private fun StockTabPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Stock — coming soon", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    }
}
```

You will also need the Material icons extended library for `Icons.Outlined.Inventory2` and the filled variants. Verify it's present; if not, add to `app/build.gradle.kts`:

```kotlin
implementation("androidx.compose.material:material-icons-extended")
```

(Compose BoM resolves the version.)

- [ ] **Step 10 — Update `PantryNavHost` to host `HomeShell` on the Home route**

In `app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`, replace the `composable(PantryRoute.Home.path)` block with:

```kotlin
composable(PantryRoute.Home.path) {
    HomeShell(
        onSignedOut = {
            navController.navigate(PantryRoute.Auth.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

Remove the old `HomePlaceholderScreen` import.

- [ ] **Step 11 — Delete the old Home placeholder**

```bash
rm app/src/main/kotlin/app/pantry/ui/home/HomePlaceholderScreen.kt
rm app/src/main/kotlin/app/pantry/ui/home/HomeViewModel.kt
rm app/src/test/kotlin/app/pantry/ui/home/HomePlaceholderScreenTest.kt
```

- [ ] **Step 12 — Write `HomeShellTest`**

Create `app/src/test/kotlin/app/pantry/ui/home/HomeShellTest.kt`:

```kotlin
package app.pantry.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeShellTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun defaults_to_stock_tab() {
        composeRule.setContent { HomeShell(onSignedOut = {}) }
        composeRule.onNodeWithText("Stock — coming soon").assertIsDisplayed()
    }

    @Test
    fun tap_shopping_tab_shows_shopping_placeholder() {
        composeRule.setContent { HomeShell(onSignedOut = {}) }
        composeRule.onNodeWithTag("tab_shopping").performClick()
        composeRule.onNodeWithText("Shopping — coming soon").assertIsDisplayed()
    }
}
```

Note: the Settings tab needs a Hilt-injected ViewModel which `createComposeRule` cannot construct, so `HomeShellTest` only tests the Stock and Shopping tabs. The Settings tab is exercised by `SettingsViewModelTest` separately.

- [ ] **Step 13 — Run all tests**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 14 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/home \
        app/src/main/kotlin/app/pantry/ui/settings \
        app/src/main/kotlin/app/pantry/ui/shopping \
        app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt \
        app/src/test/kotlin/app/pantry/ui/home \
        app/src/test/kotlin/app/pantry/ui/settings
git rm app/src/main/kotlin/app/pantry/ui/home/HomePlaceholderScreen.kt
git rm app/src/main/kotlin/app/pantry/ui/home/HomeViewModel.kt
git rm app/src/test/kotlin/app/pantry/ui/home/HomePlaceholderScreenTest.kt
git commit -m "feat(home): add bottom navigation shell with Settings sign-out"
```

---

## US-2: StockItem + StockUnit domain model

**As a** developer
**I want** a pure-Kotlin `StockItem` data class and `StockUnit` enum
**So that** the UI and repository layers depend on a stable contract.

### Acceptance criteria

- `StockItem(id, name, category, unit: StockUnit, quantity: Double, threshold: Double, updatedAt: Instant)` is a pure data class with no Android or Firebase imports
- `StockUnit` enum with values `COUNT, GRAM, KILOGRAM, MILLILITER, LITER` and a `displaySuffix` property used for rendering ("count" maps to empty, others to the unit abbreviation)
- `StockItem` exposes a computed `isLowStock: Boolean` (true when `quantity < threshold`)

### Files

- Create: `app/src/main/kotlin/app/pantry/domain/model/StockItem.kt`
- Create: `app/src/main/kotlin/app/pantry/domain/model/StockUnit.kt`
- Create: `app/src/test/kotlin/app/pantry/domain/model/StockItemTest.kt`

### Tasks

- [ ] **Step 1 — Define `StockUnit`**

Create `app/src/main/kotlin/app/pantry/domain/model/StockUnit.kt`:

```kotlin
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
```

- [ ] **Step 2 — Define `StockItem`**

Create `app/src/main/kotlin/app/pantry/domain/model/StockItem.kt`:

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
) {
    val isLowStock: Boolean get() = quantity < threshold
}
```

- [ ] **Step 3 — Write `StockItemTest`**

Create `app/src/test/kotlin/app/pantry/domain/model/StockItemTest.kt`:

```kotlin
package app.pantry.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class StockItemTest {

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun `isLowStock true when quantity below threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0, now)
        assertTrue(item.isLowStock)
    }

    @Test
    fun `isLowStock false when quantity equals threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.0, 1.0, now)
        assertFalse(item.isLowStock)
    }

    @Test
    fun `isLowStock false when quantity above threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 2.0, 1.0, now)
        assertFalse(item.isLowStock)
    }

    @Test
    fun `StockUnit fromStorageKey defaults to COUNT for unknown input`() {
        assertEquals(StockUnit.COUNT, StockUnit.fromStorageKey("widget"))
        assertEquals(StockUnit.COUNT, StockUnit.fromStorageKey(null))
        assertEquals(StockUnit.LITER, StockUnit.fromStorageKey("L"))
    }
}
```

- [ ] **Step 4 — Run; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.domain.model.StockItemTest"
```

Expected: PASS (4 tests).

- [ ] **Step 5 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/domain/model/StockItem.kt \
        app/src/main/kotlin/app/pantry/domain/model/StockUnit.kt \
        app/src/test/kotlin/app/pantry/domain/model/StockItemTest.kt
git commit -m "feat(stock): add StockItem and StockUnit domain model"
```

---

## US-3: StockItemRepository interface

**As a** developer
**I want** a `StockItemRepository` interface
**So that** ViewModels depend on an abstraction, not Firestore.

### Acceptance criteria

- `StockItemRepository` exposes the observe + CRUD shape needed by the Stock screen:
  - `fun observe(householdId): Flow<List<StockItem>>`
  - `suspend fun create(householdId, name, category, unit, quantity, threshold): Result<StockItem>`
  - `suspend fun update(householdId, itemId, name, category, unit, quantity, threshold): Result<Unit>`
  - `suspend fun delete(householdId, itemId): Result<Unit>`
  - `suspend fun adjustQuantity(householdId, itemId, delta): Result<Unit>` — atomic increment for the stepper

### Files

- Create: `app/src/main/kotlin/app/pantry/data/stock/StockItemRepository.kt`

### Tasks

- [ ] **Step 1 — Define the interface**

Create `app/src/main/kotlin/app/pantry/data/stock/StockItemRepository.kt`:

```kotlin
package app.pantry.data.stock

import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import kotlinx.coroutines.flow.Flow

interface StockItemRepository {
    fun observe(householdId: String): Flow<List<StockItem>>

    suspend fun create(
        householdId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<StockItem>

    suspend fun update(
        householdId: String,
        itemId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<Unit>

    suspend fun delete(householdId: String, itemId: String): Result<Unit>

    /** Atomic relative adjustment for the +/- stepper. */
    suspend fun adjustQuantity(householdId: String, itemId: String, delta: Double): Result<Unit>
}
```

- [ ] **Step 2 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/data/stock/StockItemRepository.kt
git commit -m "feat(stock): add StockItemRepository contract"
```

---

## US-4: FirestoreStockItemRepository + Hilt + tests

**As a** developer
**I want** a Firestore-backed `StockItemRepository` implementation with unit tests and an integration test against the local Firestore emulator
**So that** the Stock screen has working persistence.

### Acceptance criteria

- `FirestoreStockItemRepository` reads/writes `households/{hid}/items/{itemId}`
- `observe()` uses `callbackFlow` + `addSnapshotListener` and closes the flow with the error on listener failure (same pattern as `FirestoreHouseholdRepository`)
- `adjustQuantity()` uses `FieldValue.increment(delta)` for atomic deltas
- `create/update` writes `updatedAt = FieldValue.serverTimestamp()`
- Hilt binds `StockItemRepository` → `FirestoreStockItemRepository`
- Firestore security rules updated to explicitly allow item-subcollection reads/writes for household members
- Unit tests cover create, update, delete, adjustQuantity, and the document-to-domain mapper
- Integration test against Firestore emulator creates an item, observes it, increments quantity, deletes it

### Files

- Create: `app/src/main/kotlin/app/pantry/data/stock/FirestoreStockItemRepository.kt`
- Create: `app/src/main/kotlin/app/pantry/di/StockModule.kt`
- Modify: `firestore.rules` (the existing `/{subcollection}/{doc}` wildcard already covers items, but we'll tighten the path)
- Create: `app/src/test/kotlin/app/pantry/data/stock/FirestoreStockItemRepositoryTest.kt`
- Create: `app/src/test/kotlin/app/pantry/data/stock/FirestoreStockItemRepositoryEmulatorTest.kt`

### Tasks

- [ ] **Step 1 — Implement `FirestoreStockItemRepository`**

Create `app/src/main/kotlin/app/pantry/data/stock/FirestoreStockItemRepository.kt`:

```kotlin
package app.pantry.data.stock

import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreStockItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StockItemRepository {

    private fun itemsCol(householdId: String) =
        firestore.collection("households").document(householdId).collection("items")

    override fun observe(householdId: String): Flow<List<StockItem>> = callbackFlow {
        val reg = itemsCol(householdId).addSnapshotListener { qs, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(qs?.documents.orEmpty().mapNotNull { it.toStockItem() })
        }
        awaitClose { reg.remove() }
    }

    override suspend fun create(
        householdId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<StockItem> = runCatching {
        val doc = itemsCol(householdId).document()
        val data = mapOf(
            "name" to name,
            "category" to category,
            "unit" to unit.storageKey,
            "quantity" to quantity,
            "threshold" to threshold,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        doc.set(data).await()
        StockItem(
            id = doc.id,
            name = name,
            category = category,
            unit = unit,
            quantity = quantity,
            threshold = threshold,
            updatedAt = Instant.now(),
        )
    }

    override suspend fun update(
        householdId: String,
        itemId: String,
        name: String,
        category: String,
        unit: StockUnit,
        quantity: Double,
        threshold: Double,
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "name" to name,
            "category" to category,
            "unit" to unit.storageKey,
            "quantity" to quantity,
            "threshold" to threshold,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        itemsCol(householdId).document(itemId).update(data).await()
    }

    override suspend fun delete(householdId: String, itemId: String): Result<Unit> = runCatching {
        itemsCol(householdId).document(itemId).delete().await()
    }

    override suspend fun adjustQuantity(
        householdId: String,
        itemId: String,
        delta: Double,
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "quantity" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        itemsCol(householdId).document(itemId).update(data).await()
    }

    private fun DocumentSnapshot.toStockItem(): StockItem? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val category = getString("category").orEmpty()
        val unit = StockUnit.fromStorageKey(getString("unit"))
        val quantity = getDouble("quantity") ?: 0.0
        val threshold = getDouble("threshold") ?: 1.0
        val ts = getTimestamp("updatedAt") ?: Timestamp.now()
        return StockItem(
            id = id,
            name = name,
            category = category,
            unit = unit,
            quantity = quantity,
            threshold = threshold,
            updatedAt = Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong()),
        )
    }
}
```

- [ ] **Step 2 — Wire Hilt**

Create `app/src/main/kotlin/app/pantry/di/StockModule.kt`:

```kotlin
package app.pantry.di

import app.pantry.data.stock.FirestoreStockItemRepository
import app.pantry.data.stock.StockItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StockBindings {
    @Binds @Singleton
    abstract fun bindStockItemRepository(impl: FirestoreStockItemRepository): StockItemRepository
}
```

- [ ] **Step 3 — Tighten Firestore rules (items subcollection)**

In `firestore.rules`, the existing `match /{subcollection}/{doc}` wildcard inside `/households/{hid}` already grants member-only access to any subcollection. That is acceptable for v1 — leave it as-is. No code change in this step; only verify the rule still applies after future subcollection additions by re-reading the file.

- [ ] **Step 4 — Write unit tests with mocked Firestore**

Create `app/src/test/kotlin/app/pantry/data/stock/FirestoreStockItemRepositoryTest.kt`:

```kotlin
package app.pantry.data.stock

import app.pantry.domain.model.StockUnit
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirestoreStockItemRepositoryTest {

    private fun mockChain(
        docId: String = "i-new",
    ): Triple<FirebaseFirestore, CollectionReference, DocumentReference> {
        val firestore: FirebaseFirestore = mockk(relaxed = true)
        val householdsCol: CollectionReference = mockk(relaxed = true)
        val householdDoc: DocumentReference = mockk(relaxed = true)
        val itemsCol: CollectionReference = mockk(relaxed = true)
        val itemDoc: DocumentReference = mockk(relaxed = true) {
            every { id } returns docId
        }
        every { firestore.collection("households") } returns householdsCol
        every { householdsCol.document(any<String>()) } returns householdDoc
        every { householdDoc.collection("items") } returns itemsCol
        every { itemsCol.document() } returns itemDoc
        every { itemsCol.document(any<String>()) } returns itemDoc
        return Triple(firestore, itemsCol, itemDoc)
    }

    @Test
    fun `create writes item doc and returns StockItem`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        every { itemDoc.set(any()) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.create(
            householdId = "h-1",
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 1.5,
            threshold = 1.0,
        )

        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("Milk", item.name)
        assertEquals(StockUnit.LITER, item.unit)
        assertEquals(1.5, item.quantity)
        verify { itemDoc.set(any()) }
    }

    @Test
    fun `update writes full field set`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        val dataSlot = slot<Map<String, Any>>()
        every { itemDoc.update(capture(dataSlot)) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.update(
            householdId = "h-1",
            itemId = "i-1",
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 0.5,
            threshold = 1.0,
        )

        assertTrue(result.isSuccess)
        assertEquals("Milk", dataSlot.captured["name"])
        assertEquals("L", dataSlot.captured["unit"])
        assertEquals(0.5, dataSlot.captured["quantity"])
    }

    @Test
    fun `delete deletes the document`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        every { itemDoc.delete() } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.delete("h-1", "i-1")

        assertTrue(result.isSuccess)
        verify { itemDoc.delete() }
    }

    @Test
    fun `adjustQuantity uses FieldValue increment`() = runTest {
        val (firestore, _, itemDoc) = mockChain()
        val dataSlot = slot<Map<String, Any>>()
        every { itemDoc.update(capture(dataSlot)) } returns Tasks.forResult(null)
        val repo = FirestoreStockItemRepository(firestore)

        val result = repo.adjustQuantity("h-1", "i-1", delta = -1.0)

        assertTrue(result.isSuccess)
        // FieldValue.increment is a sentinel; verify the key exists with a non-null value
        assertTrue(dataSlot.captured.containsKey("quantity"))
        assertTrue(dataSlot.captured["quantity"] != null)
    }
}
```

- [ ] **Step 5 — Write emulator integration test**

Create `app/src/test/kotlin/app/pantry/data/stock/FirestoreStockItemRepositoryEmulatorTest.kt`:

```kotlin
package app.pantry.data.stock

import app.cash.turbine.test
import app.pantry.domain.model.StockUnit
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Tag
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Tag("emulator")
class FirestoreStockItemRepositoryEmulatorTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: FirestoreStockItemRepository

    @Before
    fun setUp() {
        assumeTrue(
            "Skipping emulator test — pass -PincludeEmulatorTests with the emulator running",
            System.getProperty("includeEmulatorTests") != null,
        )
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(
                ctx,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("pantry-dev-1922e")
                    .setApiKey("fake-api-key")
                    .build(),
            )
        }
        firestore = FirebaseFirestore.getInstance()
        if (!emulatorConfigured) {
            firestore.useEmulator("localhost", 8080)
            emulatorConfigured = true
        }
        repo = FirestoreStockItemRepository(firestore)
    }

    @Test
    fun `create observe increment delete round-trip`() = runTest {
        val householdId = "test-hh-${System.currentTimeMillis()}"

        val created = repo.create(
            householdId = householdId,
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 1.0,
            threshold = 1.0,
        )
        assertNotNull(created.getOrNull())
        val itemId = created.getOrThrow().id

        repo.observe(householdId).test {
            // First emission: contains the created item
            var items = awaitItem()
            while (items.none { it.id == itemId }) items = awaitItem()
            assertEquals("Milk", items.first { it.id == itemId }.name)

            // Increment by +1
            repo.adjustQuantity(householdId, itemId, 1.0)
            var updated = awaitItem()
            while (updated.first { it.id == itemId }.quantity < 2.0) updated = awaitItem()
            assertEquals(2.0, updated.first { it.id == itemId }.quantity, 0.001)

            // Delete
            repo.delete(householdId, itemId)
            var afterDelete = awaitItem()
            while (afterDelete.any { it.id == itemId }) afterDelete = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private var emulatorConfigured = false
    }
}
```

- [ ] **Step 6 — Run unit tests; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.data.stock.FirestoreStockItemRepositoryTest"
```

Expected: PASS (4 tests).

- [ ] **Step 7 — Run emulator test against running emulator**

In one shell:
```powershell
$env:PATH = "$HOME\.jdks\openjdk-23.0.1\bin;" + $env:PATH
npx firebase-tools@latest emulators:start --only firestore --project pantry-dev-1922e
```

In another:
```bash
./gradlew :app:test --tests "app.pantry.data.stock.FirestoreStockItemRepositoryEmulatorTest" -PincludeEmulatorTests
```

Expected: PASS.

- [ ] **Step 8 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/data/stock \
        app/src/main/kotlin/app/pantry/di/StockModule.kt \
        app/src/test/kotlin/app/pantry/data/stock
git commit -m "feat(stock): add Firestore-backed StockItemRepository with tests"
```

---

## US-5: CurrentHouseholdRepository

**As a** developer
**I want** a single source of truth for the current household ID
**So that** every ViewModel that needs stock items asks one place instead of recomputing `auth + households`.

### Acceptance criteria

- `CurrentHouseholdRepository` exposes `currentHouseholdId: StateFlow<String?>`
- The flow emits `null` when no user is signed in OR when the user has no households
- The flow emits the first household ID otherwise (v1 = one household per user)
- A unit test covers all three states

### Files

- Create: `app/src/main/kotlin/app/pantry/data/household/CurrentHouseholdRepository.kt`
- Modify: `app/src/main/kotlin/app/pantry/di/HouseholdModule.kt` (Hilt provides `@Singleton CurrentHouseholdRepository`)
- Create: `app/src/test/kotlin/app/pantry/data/household/CurrentHouseholdRepositoryTest.kt`

### Tasks

- [ ] **Step 1 — Implement the repository**

Create `app/src/main/kotlin/app/pantry/data/household/CurrentHouseholdRepository.kt`:

```kotlin
package app.pantry.data.household

import app.pantry.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class CurrentHouseholdRepository @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentHouseholdId: StateFlow<String?> = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else households.observeUserHouseholds(profile.uid)
        }
        .map { it.firstOrNull()?.id }
        .stateIn(scope, SharingStarted.Eagerly, null)
}
```

- [ ] **Step 2 — Provide application-scoped `CoroutineScope` and bind the repository in Hilt**

Modify `app/src/main/kotlin/app/pantry/di/HouseholdModule.kt`. Append to the existing module declarations:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {
    @Provides
    @Singleton
    fun provideAppScope(): kotlinx.coroutines.CoroutineScope =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() +
                kotlinx.coroutines.Dispatchers.Default,
        )
}
```

Hilt automatically injects `CurrentHouseholdRepository` via its `@Inject constructor` — no `@Binds` needed since the consumers depend on the concrete class.

- [ ] **Step 3 — Write the failing test**

Create `app/src/test/kotlin/app/pantry/data/household/CurrentHouseholdRepositoryTest.kt`:

```kotlin
package app.pantry.data.household

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentHouseholdRepositoryTest {

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun makeScope() = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Test
    fun `null when no user`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(null) }
        val households: HouseholdRepository = mockk()
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        assertEquals(null, repo.currentHouseholdId.value)
    }

    @Test
    fun `null when user has no households`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk { every { observeUserHouseholds("u-1") } returns flowOf(emptyList()) }
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        repo.currentHouseholdId.test {
            var v = awaitItem()
            while (v != null) v = awaitItem()
            assertEquals(null, v)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits first household id`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk {
            every { observeUserHouseholds("u-1") } returns flowOf(listOf(Household("h-1", "Casa", listOf("u-1"), "ABCDEF")))
        }
        val repo = CurrentHouseholdRepository(auth, households, makeScope())
        repo.currentHouseholdId.test {
            var v = awaitItem()
            while (v == null) v = awaitItem()
            assertEquals("h-1", v)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 4 — Run; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.data.household.CurrentHouseholdRepositoryTest"
```

Expected: PASS (3 tests).

- [ ] **Step 5 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/data/household/CurrentHouseholdRepository.kt \
        app/src/main/kotlin/app/pantry/di/HouseholdModule.kt \
        app/src/test/kotlin/app/pantry/data/household/CurrentHouseholdRepositoryTest.kt
git commit -m "feat(household): add CurrentHouseholdRepository"
```

---

## US-6: StockListUiState + StockListViewModel

**As a** developer
**I want** a ViewModel that exposes the Stock screen's full state (items, search, selected category, loading) as a single `StateFlow<StockListUiState>`
**So that** the screen renders from one source of truth.

### Acceptance criteria

- `StockListUiState` carries: `isLoading`, `allItems: List<StockItem>`, `searchQuery: String`, `selectedCategory: String?` (null = "All"), `errorMessage: String?`, plus computed `categories: List<String>` and `visibleItems: List<StockItem>` (filtered by category then by search)
- `StockListViewModel` observes the current household ID via `CurrentHouseholdRepository` and switches the items flow when household changes
- `onSearchChange(query)`, `onCategorySelect(category)`, `onErrorShown()` update the state
- A unit test covers: filter by category, filter by search, category list derivation, and error propagation when the items flow throws

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/stock/StockListUiState.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/stock/StockListViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define `StockListUiState`**

Create `app/src/main/kotlin/app/pantry/ui/stock/StockListUiState.kt`:

```kotlin
package app.pantry.ui.stock

import app.pantry.domain.model.StockItem

data class StockListUiState(
    val isLoading: Boolean = true,
    val allItems: List<StockItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null, // null means "All"
    val errorMessage: String? = null,
) {
    val categories: List<String>
        get() = allItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val visibleItems: List<StockItem>
        get() {
            val byCategory = selectedCategory?.let { sel -> allItems.filter { it.category == sel } } ?: allItems
            val query = searchQuery.trim()
            return if (query.isEmpty()) byCategory
            else byCategory.filter { it.name.contains(query, ignoreCase = true) }
        }
}
```

- [ ] **Step 2 — Implement `StockListViewModel`**

Create `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StockListViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockListUiState())
    val uiState: StateFlow<StockListUiState> = _state.asStateFlow()

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            currentHousehold.currentHouseholdId
                .flatMapLatest { hid ->
                    if (hid == null) flowOf(emptyList())
                    else stock.observe(hid)
                }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load stock") }
                }
                .collectLatest { items ->
                    _state.update { it.copy(isLoading = false, allItems = items, errorMessage = null) }
                }
        }
    }

    fun onSearchChange(query: String) = _state.update { it.copy(searchQuery = query) }
    fun onCategorySelect(category: String?) = _state.update { it.copy(selectedCategory = category) }
    fun onErrorShown() = _state.update { it.copy(errorMessage = null) }
}
```

- [ ] **Step 3 — Write the failing test**

Create `app/src/test/kotlin/app/pantry/ui/stock/StockListViewModelTest.kt`:

```kotlin
package app.pantry.ui.stock

import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StockListViewModelTest {

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    private fun item(name: String, category: String): StockItem =
        StockItem("id-$name", name, category, StockUnit.COUNT, 1.0, 1.0, now)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun makeVm(items: List<StockItem>, hid: String? = "h-1"): StockListViewModel {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow(hid) }
        val stock: StockItemRepository = mockk { every { observe(any()) } returns flowOf(items) }
        return StockListViewModel(ch, stock)
    }

    @Test
    fun `surfaces items from repository`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Bread", "Pantry")))
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.allItems.size)
    }

    @Test
    fun `categories are distinct sorted non-blank`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Cheese", "Fridge"), item("Bread", "Pantry"), item("Beer", "")))
        advanceUntilIdle()
        assertEquals(listOf("Fridge", "Pantry"), vm.uiState.value.categories)
    }

    @Test
    fun `selectedCategory filter applies`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Bread", "Pantry")))
        advanceUntilIdle()
        vm.onCategorySelect("Fridge")
        assertEquals(listOf("Milk"), vm.uiState.value.visibleItems.map { it.name })
    }

    @Test
    fun `search filter applies and is case-insensitive`() = runTest {
        val vm = makeVm(listOf(item("Milk", "Fridge"), item("Mango", "Fruit"), item("Bread", "Pantry")))
        advanceUntilIdle()
        vm.onSearchChange("m")
        val names = vm.uiState.value.visibleItems.map { it.name }
        assertEquals(setOf("Milk", "Mango"), names.toSet())
    }

    @Test
    fun `emits error message when observe flow throws`() = runTest {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk {
            every { observe("h-1") } returns flow { throw RuntimeException("permission") }
        }
        val vm = StockListViewModel(ch, stock)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage?.contains("permission") == true)
    }
}
```

- [ ] **Step 4 — Run; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.stock.StockListViewModelTest"
```

Expected: PASS (5 tests).

- [ ] **Step 5 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListUiState.kt \
        app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt \
        app/src/test/kotlin/app/pantry/ui/stock/StockListViewModelTest.kt
git commit -m "feat(stock): add StockListViewModel with search/category filter"
```

---

## US-7: StockListScreen (basic list)

**As a** signed-in user with a household
**I want** to see my household's stock items on the Stock tab
**So that** I know what's in the catalog.

### Acceptance criteria

- The Stock tab of `HomeShell` renders `StockListScreen` instead of the "Stock — coming soon" placeholder
- The screen shows one row per item with name + qty/unit (e.g., "Milk · 1.5 L" or "Eggs · 6")
- Loading state shows 3 grey shimmer rows for first paint; switches to either the list or "No items yet" empty state once data lands
- FAB is rendered (does nothing yet — US-8 wires the bottom sheet)
- A Robolectric Compose test confirms: empty state visible when no items; list renders when items present

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt` (Stock tab branch now calls `StockListScreen`)
- Create: `app/src/test/kotlin/app/pantry/ui/stock/StockListScreenTest.kt`

### Tasks

- [ ] **Step 1 — Implement `StockListScreen`**

Create `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.domain.model.StockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    viewModel: StockListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* wired in US-8 */ },
                modifier = Modifier.testTag("stock_fab"),
            ) { Icon(Icons.Filled.Add, contentDescription = "Add item") }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingRows()
                state.visibleItems.isEmpty() -> EmptyState()
                else -> ItemList(state.visibleItems)
            }
        }
    }
}

@Composable
private fun LoadingRows() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                Modifier.fillMaxWidth().height(56.dp).testTag("stock_loading_row"),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No items yet — tap + to add one", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ItemList(items: List<StockItem>) {
    LazyColumn(Modifier.fillMaxSize().testTag("stock_list")) {
        items(items, key = { it.id }) { item ->
            StockRow(item)
            HorizontalDivider()
        }
    }
}

@Composable
private fun StockRow(item: StockItem) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val qtyText = buildString {
            append(formatQuantity(item.quantity))
            if (item.unit.displaySuffix.isNotEmpty()) {
                append(' ')
                append(item.unit.displaySuffix)
            }
        }
        Text(
            "${item.name} · $qtyText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("stock_row_${item.id}"),
        )
    }
}

private fun formatQuantity(q: Double): String {
    // Render integers without trailing ".0"; decimals with up to two fractional digits.
    return if (q % 1.0 == 0.0) q.toLong().toString()
    else "%.2f".format(q).trimEnd('0').trimEnd('.', ',')
}
```

- [ ] **Step 2 — Wire the Stock tab to `StockListScreen`**

In `app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt`, replace the `HomeTab.Stock -> StockTabPlaceholder()` branch with:

```kotlin
HomeTab.Stock -> app.pantry.ui.stock.StockListScreen()
```

Remove the now-unused `StockTabPlaceholder` private composable.

- [ ] **Step 3 — Write `StockListScreenTest`**

Create `app/src/test/kotlin/app/pantry/ui/stock/StockListScreenTest.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StockListScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    private fun makeVm(items: List<StockItem>): StockListViewModel {
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk { every { observe("h-1") } returns flowOf(items) }
        return StockListViewModel(ch, stock)
    }

    @Test
    fun shows_empty_state_when_no_items() {
        composeRule.setContent { StockListScreen(viewModel = makeVm(emptyList())) }
        composeRule.onNodeWithText("No items yet — tap + to add one").assertIsDisplayed()
    }

    @Test
    fun shows_item_rows_when_present() {
        val items = listOf(
            StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0, now),
        )
        composeRule.setContent { StockListScreen(viewModel = makeVm(items)) }
        composeRule.onNodeWithTag("stock_row_i-1").assertIsDisplayed()
    }
}
```

- [ ] **Step 4 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 5 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt \
        app/src/main/kotlin/app/pantry/ui/home/HomeShell.kt \
        app/src/test/kotlin/app/pantry/ui/stock/StockListScreenTest.kt
git commit -m "feat(stock): render basic Stock list on the Stock tab"
```

---

## US-8: Add item bottom sheet

**As a** user
**I want** to tap the FAB and enter a new stock item via a bottom sheet
**So that** I can grow my catalog.

### Acceptance criteria

- Tapping the FAB opens a `ModalBottomSheet` containing the add form
- Form fields: Name (text), Quantity (numeric), Unit (dropdown — COUNT/g/kg/ml/L), Threshold (numeric, default 1), Category (chips for existing categories + a "+ New" chip that reveals a text field)
- Submit calls `StockItemRepository.create(...)`; on success the sheet closes
- On failure, an inline error appears in the sheet (toast)
- Submit is disabled while any required field is blank, or while in-flight
- A Robolectric Compose test covers the happy path: fill all fields → submit → sheet closes

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemBottomSheet.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemViewModel.kt`
- Create: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt` (FAB opens the sheet)
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt` (expose existing categories so the sheet can render chips)
- Create: `app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define `AddEditItemUiState`**

Create `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemUiState.kt`:

```kotlin
package app.pantry.ui.stock

import app.pantry.domain.model.StockUnit

data class AddEditItemUiState(
    val mode: Mode = Mode.Add,
    val itemId: String? = null,
    val name: String = "",
    val quantity: String = "0",         // string-backed so we can render input as-typed
    val unit: StockUnit = StockUnit.COUNT,
    val threshold: String = "1",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val dismissed: Boolean = false,
) {
    enum class Mode { Add, Edit }

    val canSubmit: Boolean
        get() = !isSubmitting
            && name.isNotBlank()
            && quantity.toDoubleOrNull() != null
            && threshold.toDoubleOrNull() != null
}
```

- [ ] **Step 2 — Implement `AddEditItemViewModel`**

Create `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemViewModel.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    private val currentHousehold: CurrentHouseholdRepository,
    private val stock: StockItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditItemUiState())
    val uiState: StateFlow<AddEditItemUiState> = _state.asStateFlow()

    fun beginAdd(prefillCategory: String = "") {
        _state.value = AddEditItemUiState(
            mode = AddEditItemUiState.Mode.Add,
            category = prefillCategory,
        )
    }

    fun beginEdit(
        itemId: String,
        name: String,
        quantity: Double,
        unit: StockUnit,
        threshold: Double,
        category: String,
    ) {
        _state.value = AddEditItemUiState(
            mode = AddEditItemUiState.Mode.Edit,
            itemId = itemId,
            name = name,
            quantity = formatQuantity(quantity),
            unit = unit,
            threshold = formatQuantity(threshold),
            category = category,
        )
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onQuantityChange(v: String) = _state.update { it.copy(quantity = v) }
    fun onUnitChange(u: StockUnit) = _state.update { it.copy(unit = u) }
    fun onThresholdChange(v: String) = _state.update { it.copy(threshold = v) }
    fun onCategoryChange(v: String) = _state.update { it.copy(category = v) }
    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        _state.update { it.copy(isSubmitting = true, toast = null) }
        viewModelScope.launch {
            val result = if (s.mode == AddEditItemUiState.Mode.Add) {
                stock.create(
                    householdId = hid,
                    name = s.name.trim(),
                    category = s.category.trim(),
                    unit = s.unit,
                    quantity = s.quantity.toDoubleOrNull() ?: 0.0,
                    threshold = s.threshold.toDoubleOrNull() ?: 1.0,
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
                )
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, dismissed = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to save") } },
            )
        }
    }

    fun delete() {
        val s = _state.value
        if (s.mode != AddEditItemUiState.Mode.Edit) return
        val itemId = s.itemId ?: return
        val hid = currentHousehold.currentHouseholdId.value ?: return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            stock.delete(hid, itemId).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, dismissed = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to delete") } },
            )
        }
    }

    private fun formatQuantity(q: Double): String =
        if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
}
```

- [ ] **Step 3 — Implement `AddEditItemBottomSheet`**

Create `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemBottomSheet.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pantry.domain.model.StockUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemBottomSheet(
    viewModel: AddEditItemViewModel,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.dismissed) { if (state.dismissed) onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (state.mode == AddEditItemUiState.Mode.Add) "Add item" else "Edit item",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                )
                if (state.mode == AddEditItemUiState.Mode.Edit) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.testTag("item_delete"),
                    ) { Text("Delete") }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("field_name"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = viewModel::onQuantityChange,
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("field_quantity"),
                )
                UnitDropdown(state.unit, viewModel::onUnitChange)
            }

            OutlinedTextField(
                value = state.threshold,
                onValueChange = viewModel::onThresholdChange,
                label = { Text("Low-stock threshold") },
                supportingText = { Text("Below this, the item shows on the shopping list") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("field_threshold"),
            )

            Text("Category", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingCategories.forEach { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { viewModel.onCategoryChange(cat) },
                        label = { Text(cat) },
                    )
                }
                AssistChip(
                    onClick = { viewModel.onCategoryChange("") },
                    label = { Text("+ New") },
                    modifier = Modifier.testTag("category_new"),
                )
            }
            if (state.category.isEmpty() || state.category !in existingCategories) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = viewModel::onCategoryChange,
                    label = { Text("New category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_category"),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().testTag("item_save"),
            ) { Text(if (state.isSubmitting) "Saving…" else "Save") }
        }
    }

    state.toast?.let { msg ->
        LaunchedEffect(msg) {
            // Surfaced via parent snackbar; consume so we don't fire repeatedly.
            viewModel.consumeToast()
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.name}?") },
            text = { Text("This removes the item from the catalog.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; viewModel.delete() },
                    modifier = Modifier.testTag("delete_confirm"),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(selected: StockUnit, onSelect: (StockUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = selected.storageKey.ifEmpty { "count" },
            onValueChange = {},
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().testTag("field_unit"),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StockUnit.entries.forEach { u ->
                DropdownMenuItem(
                    text = { Text(if (u == StockUnit.COUNT) "count" else u.storageKey) },
                    onClick = { onSelect(u); expanded = false },
                )
            }
        }
    }
}
```

- [ ] **Step 4 — Expose `categories` from `StockListViewModel` for the sheet**

The `StockListUiState` already computes `categories` (US-6). No code change here — `StockListScreen` will pass `state.categories` into the sheet.

- [ ] **Step 5 — Wire the FAB to open the sheet from `StockListScreen`**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, modify the screen to host the sheet. Replace the `FloatingActionButton(onClick = { /* wired in US-8 */ })` block and add sheet state at the screen scope:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    viewModel: StockListViewModel = hiltViewModel(),
    sheetViewModel: AddEditItemViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState by sheetViewModel.uiState.collectAsState()
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    sheetViewModel.beginAdd(prefillCategory = state.selectedCategory.orEmpty())
                    sheetOpen = true
                },
                modifier = Modifier.testTag("stock_fab"),
            ) { Icon(Icons.Filled.Add, contentDescription = "Add item") }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingRows()
                state.visibleItems.isEmpty() -> EmptyState()
                else -> ItemList(state.visibleItems)
            }
        }
    }

    if (sheetOpen) {
        AddEditItemBottomSheet(
            viewModel = sheetViewModel,
            existingCategories = state.categories,
            onDismiss = { sheetOpen = false },
        )
    }
}
```

Add the necessary imports at the top of the file: `androidx.compose.runtime.{mutableStateOf, remember, setValue}` and `app.pantry.ui.stock.AddEditItemBottomSheet`.

- [ ] **Step 6 — Write `AddEditItemViewModelTest` (add path)**

Create `app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt`:

```kotlin
package app.pantry.ui.stock

import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditItemViewModelTest {

    private val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
    private val stock: StockItemRepository = mockk(relaxed = true)

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `add path submits and dismisses on success`() = runTest {
        coEvery { stock.create("h-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0) } returns
            Result.success(StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.5, 1.0, Instant.now()))
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onNameChange("Milk")
        vm.onCategoryChange("Fridge")
        vm.onUnitChange(StockUnit.LITER)
        vm.onQuantityChange("1.5")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.dismissed)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `add path surfaces toast on failure`() = runTest {
        coEvery { stock.create(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("network"))
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onNameChange("Milk")
        vm.onQuantityChange("1")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        assertEquals("network", vm.uiState.value.toast)
        assertFalse(vm.uiState.value.dismissed)
    }

    @Test
    fun `submit no-ops when name blank`() = runTest {
        val vm = AddEditItemViewModel(ch, stock)
        vm.beginAdd()
        vm.onQuantityChange("1")
        vm.onThresholdChange("1")
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 0) { stock.create(any(), any(), any(), any(), any(), any()) }
    }
}
```

- [ ] **Step 7 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 8 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/AddEditItemBottomSheet.kt \
        app/src/main/kotlin/app/pantry/ui/stock/AddEditItemViewModel.kt \
        app/src/main/kotlin/app/pantry/ui/stock/AddEditItemUiState.kt \
        app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt \
        app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt
git commit -m "feat(stock): add bottom sheet to create new items"
```

---

## US-9: Edit item bottom sheet + delete

**As a** user
**I want** to tap an existing item to edit it, and to delete it with a confirmation
**So that** I can correct mistakes and prune obsolete items.

### Acceptance criteria

- Tapping a stock row opens the same `AddEditItemBottomSheet` in **Edit** mode, pre-filled
- Edit sheet shows a **Delete** action in the header that opens a confirmation dialog
- Confirming Delete calls `StockItemRepository.delete(...)`; on success the sheet closes
- Add path tests (US-8) remain green; new tests cover the edit + delete happy paths

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt` (row click hooks `beginEdit`)
- Modify: `app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt` (add edit + delete tests)

### Tasks

- [ ] **Step 1 — Hook row click to begin edit**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, modify `ItemList` and `StockRow` to accept and forward a click handler, and have the screen pass one that calls `sheetViewModel.beginEdit(...)`.

Add at the top of the screen function:

```kotlin
val onRowClick: (StockItem) -> Unit = { item ->
    sheetViewModel.beginEdit(
        itemId = item.id,
        name = item.name,
        quantity = item.quantity,
        unit = item.unit,
        threshold = item.threshold,
        category = item.category,
    )
    sheetOpen = true
}
```

Pass `onRowClick` into `ItemList`. Update `ItemList`:

```kotlin
@Composable
private fun ItemList(items: List<StockItem>, onClick: (StockItem) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("stock_list")) {
        items(items, key = { it.id }) { item ->
            StockRow(item, onClick = { onClick(item) })
            HorizontalDivider()
        }
    }
}
```

And `StockRow`:

```kotlin
@Composable
private fun StockRow(item: StockItem, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val qtyText = buildString {
            append(formatQuantity(item.quantity))
            if (item.unit.displaySuffix.isNotEmpty()) { append(' '); append(item.unit.displaySuffix) }
        }
        Text(
            "${item.name} · $qtyText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("stock_row_${item.id}"),
        )
    }
}
```

Add import `androidx.compose.foundation.clickable`.

In the `when` block in the screen, update:

```kotlin
else -> ItemList(state.visibleItems, onClick = onRowClick)
```

- [ ] **Step 2 — Add edit + delete tests to `AddEditItemViewModelTest`**

In `app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt`, append:

```kotlin
@Test
fun `edit path submits update and dismisses`() = runTest {
    coEvery { stock.update("h-1", "i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0) } returns
        Result.success(Unit)
    val vm = AddEditItemViewModel(ch, stock)
    vm.beginEdit(
        itemId = "i-1",
        name = "Milk",
        quantity = 1.5,
        unit = StockUnit.LITER,
        threshold = 1.0,
        category = "Fridge",
    )
    vm.onQuantityChange("0.5")
    vm.submit()
    advanceUntilIdle()
    assertTrue(vm.uiState.value.dismissed)
}

@Test
fun `delete in edit mode calls repository`() = runTest {
    coEvery { stock.delete("h-1", "i-1") } returns Result.success(Unit)
    val vm = AddEditItemViewModel(ch, stock)
    vm.beginEdit(
        itemId = "i-1",
        name = "Milk",
        quantity = 1.5,
        unit = StockUnit.LITER,
        threshold = 1.0,
        category = "Fridge",
    )
    vm.delete()
    advanceUntilIdle()
    coVerify { stock.delete("h-1", "i-1") }
    assertTrue(vm.uiState.value.dismissed)
}

@Test
fun `delete in add mode is a no-op`() = runTest {
    val vm = AddEditItemViewModel(ch, stock)
    vm.beginAdd()
    vm.delete()
    advanceUntilIdle()
    coVerify(exactly = 0) { stock.delete(any(), any()) }
}
```

- [ ] **Step 3 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 4 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt \
        app/src/test/kotlin/app/pantry/ui/stock/AddEditItemViewModelTest.kt
git commit -m "feat(stock): edit item via bottom sheet with delete confirmation"
```

---

## US-10: Inline +/- stepper (optimistic UI)

**As a** user
**I want** `−` and `+` buttons next to each item's quantity
**So that** I can quickly adjust stock without opening the edit sheet.

### Acceptance criteria

- Each item row shows a `−` and a `+` button bracketing the quantity
- Tapping `+` calls `adjustQuantity(+1)`; tapping `−` calls `adjustQuantity(-1)`
- The UI updates the displayed quantity optimistically on tap
- If the Firestore write fails, the optimistic change reverts and a snackbar appears
- The `−` button is disabled when `quantity <= 0`
- A Robolectric Compose test confirms: tapping `+` triggers the repository call

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt` (add `onIncrement` / `onDecrement`)
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt` (render stepper on each row)
- Create: `app/src/test/kotlin/app/pantry/ui/stock/StockStepperTest.kt`

### Tasks

- [ ] **Step 1 — Add optimistic adjust methods to `StockListViewModel`**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt`, add:

```kotlin
fun onIncrement(item: StockItem) = adjust(item, delta = 1.0)
fun onDecrement(item: StockItem) = adjust(item, delta = -1.0)

private fun adjust(item: StockItem, delta: Double) {
    val hid = currentHousehold.currentHouseholdId.value ?: return
    // Optimistic local update: replace the matching item with a new qty
    _state.update { current ->
        val next = current.allItems.map {
            if (it.id == item.id) it.copy(quantity = (it.quantity + delta).coerceAtLeast(0.0)) else it
        }
        current.copy(allItems = next)
    }
    viewModelScope.launch {
        stock.adjustQuantity(hid, item.id, delta).onFailure { e ->
            // Revert
            _state.update { current ->
                val reverted = current.allItems.map {
                    if (it.id == item.id) it.copy(quantity = (it.quantity - delta).coerceAtLeast(0.0)) else it
                }
                current.copy(
                    allItems = reverted,
                    errorMessage = e.message ?: "Failed to update quantity",
                )
            }
        }
    }
}
```

Add the import `app.pantry.domain.model.StockItem` if not already present.

- [ ] **Step 2 — Render the stepper in `StockRow`**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, replace the existing `StockRow` and `ItemList` to accept stepper handlers:

```kotlin
@Composable
private fun ItemList(
    items: List<StockItem>,
    onRowClick: (StockItem) -> Unit,
    onIncrement: (StockItem) -> Unit,
    onDecrement: (StockItem) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().testTag("stock_list")) {
        items(items, key = { it.id }) { item ->
            StockRow(item, onClick = { onRowClick(item) }, onPlus = { onIncrement(item) }, onMinus = { onDecrement(item) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun StockRow(
    item: StockItem,
    onClick: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .testTag("stock_row_${item.id}"),
        )
        IconButton(
            onClick = onMinus,
            enabled = item.quantity > 0.0,
            modifier = Modifier.testTag("stock_minus_${item.id}"),
        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
        Text(
            buildString {
                append(formatQuantity(item.quantity))
                if (item.unit.displaySuffix.isNotEmpty()) { append(' '); append(item.unit.displaySuffix) }
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(
            onClick = onPlus,
            modifier = Modifier.testTag("stock_plus_${item.id}"),
        ) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
    }
}
```

Add the imports: `androidx.compose.foundation.layout.Row`, `androidx.compose.material.icons.filled.Remove`, `androidx.compose.material3.IconButton`, `androidx.compose.ui.Alignment`.

In the screen's `when` block, change `else -> ItemList(state.visibleItems, onClick = onRowClick)` to:

```kotlin
else -> ItemList(
    items = state.visibleItems,
    onRowClick = onRowClick,
    onIncrement = viewModel::onIncrement,
    onDecrement = viewModel::onDecrement,
)
```

- [ ] **Step 3 — Write the stepper test**

Create `app/src/test/kotlin/app/pantry/ui/stock/StockStepperTest.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StockStepperTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun plus_button_invokes_adjustQuantity() {
        val items = listOf(StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.0, 1.0, now))
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk(relaxed = true) {
            every { observe("h-1") } returns flowOf(items)
        }
        coEvery { stock.adjustQuantity("h-1", "i-1", 1.0) } returns Result.success(Unit)
        val sheetVm: AddEditItemViewModel = mockk(relaxed = true) {
            every { uiState } returns MutableStateFlow(AddEditItemUiState())
        }
        composeRule.setContent {
            StockListScreen(
                viewModel = StockListViewModel(ch, stock),
                sheetViewModel = sheetVm,
            )
        }
        composeRule.onNodeWithTag("stock_plus_i-1").performClick()
        coVerify { stock.adjustQuantity("h-1", "i-1", 1.0) }
    }
}
```

- [ ] **Step 4 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 5 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt \
        app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt \
        app/src/test/kotlin/app/pantry/ui/stock/StockStepperTest.kt
git commit -m "feat(stock): add optimistic +/- stepper to item rows"
```

---

## US-11: Search field

**As a** user
**I want** a search field at the top of the Stock screen
**So that** I can filter items by name as I type.

### Acceptance criteria

- A Material 3 `OutlinedTextField` with a magnifying-glass leading icon and a placeholder "Search items" sits under the top bar
- Typing filters `visibleItems` (already implemented in US-6's UiState)
- A "clear" trailing icon appears when the field is non-empty and clears it on tap

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`

### Tasks

- [ ] **Step 1 — Add the search field**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, insert above the `when { ... }` block a `Column`:

```kotlin
Column(Modifier.fillMaxSize().padding(padding)) {
    SearchField(
        query = state.searchQuery,
        onChange = viewModel::onSearchChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingRows()
            state.visibleItems.isEmpty() -> EmptyState()
            else -> ItemList(
                items = state.visibleItems,
                onRowClick = onRowClick,
                onIncrement = viewModel::onIncrement,
                onDecrement = viewModel::onDecrement,
            )
        }
    }
}
```

(Replace the previous `Box(Modifier.fillMaxSize().padding(padding)) { when { ... } }` with the new `Column` wrapper.)

Then add:

```kotlin
@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        placeholder = { Text("Search items") },
        singleLine = true,
        modifier = modifier.testTag("stock_search"),
    )
}
```

Add imports: `androidx.compose.foundation.layout.Column`, `androidx.compose.material.icons.filled.Clear`, `androidx.compose.material.icons.filled.Search`, `androidx.compose.material3.OutlinedTextField`, `androidx.compose.material3.IconButton`.

- [ ] **Step 2 — Build and run all tests**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 3 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt
git commit -m "feat(stock): add search field above the item list"
```

---

## US-12: Category chips with filter

**As a** user
**I want** a horizontal row of category chips above the item list
**So that** I can filter by category with one tap.

### Acceptance criteria

- A `Row` of `FilterChip`s appears between the search field and the list
- Chips: **All** (no filter, default selected) + one chip per distinct category in `state.categories`
- Tapping a chip sets `selectedCategory` (`null` for "All"); the list filters accordingly
- A test verifies: tapping a chip filters the list

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`

### Tasks

- [ ] **Step 1 — Render the chips**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, between the `SearchField` and the `Box(Modifier.fillMaxSize()) { ... }`, insert:

```kotlin
CategoryChipsRow(
    categories = state.categories,
    selected = state.selectedCategory,
    onSelect = viewModel::onCategorySelect,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
)
```

Then add:

```kotlin
@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            modifier = Modifier.testTag("chip_all"),
        )
        categories.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
                modifier = Modifier.testTag("chip_$cat"),
            )
        }
    }
}
```

Imports: `androidx.compose.foundation.horizontalScroll`, `androidx.compose.foundation.layout.Row`, `androidx.compose.foundation.rememberScrollState`, `androidx.compose.material3.FilterChip`.

- [ ] **Step 2 — Run all tests**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 3 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt
git commit -m "feat(stock): add category filter chips"
```

---

## US-13: Low-stock badge + qty-0 styling

**As a** user
**I want** items low on stock to be visually flagged, and items at zero to be de-emphasised
**So that** I see at a glance what I need to buy.

### Acceptance criteria

- When `item.isLowStock` is true (quantity < threshold) AND quantity > 0, render a ⚠ glyph before the quantity and color the quantity red
- When quantity == 0, the row is 50% opacity and the text is italic
- A Compose Robolectric test verifies the warning glyph appears for a low-stock item

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/stock/LowStockBadgeTest.kt`

### Tasks

- [ ] **Step 1 — Style the row by stock state**

In `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`, update `StockRow`'s `Row` and the quantity `Text`:

```kotlin
@Composable
private fun StockRow(
    item: StockItem,
    onClick: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
) {
    val isOutOfStock = item.quantity == 0.0
    val rowAlpha = if (isOutOfStock) 0.5f else 1f
    val isLowAndNotZero = item.isLowStock && !isOutOfStock
    val qtyColor = if (isLowAndNotZero) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val qtyStyle = if (isOutOfStock)
        MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    else MaterialTheme.typography.bodyLarge

    Row(
        Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.name,
            style = qtyStyle,
            modifier = Modifier.weight(1f).clickable { onClick() }.testTag("stock_row_${item.id}"),
        )
        IconButton(
            onClick = onMinus,
            enabled = item.quantity > 0.0,
            modifier = Modifier.testTag("stock_minus_${item.id}"),
        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLowAndNotZero) {
                Text(
                    "⚠ ",
                    color = qtyColor,
                    modifier = Modifier.testTag("low_stock_badge_${item.id}"),
                )
            }
            Text(
                buildString {
                    append(formatQuantity(item.quantity))
                    if (item.unit.displaySuffix.isNotEmpty()) { append(' '); append(item.unit.displaySuffix) }
                },
                style = qtyStyle,
                color = qtyColor,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        IconButton(
            onClick = onPlus,
            modifier = Modifier.testTag("stock_plus_${item.id}"),
        ) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
    }
}
```

Add import: `androidx.compose.ui.draw.alpha`.

- [ ] **Step 2 — Add the badge test**

Create `app/src/test/kotlin/app/pantry/ui/stock/LowStockBadgeTest.kt`:

```kotlin
package app.pantry.ui.stock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.stock.StockItemRepository
import app.pantry.domain.model.StockItem
import app.pantry.domain.model.StockUnit
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LowStockBadgeTest {

    @get:Rule val composeRule = createComposeRule()

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun shows_low_stock_badge_when_below_threshold() {
        // quantity 0.5, threshold 1.0 → low and non-zero → badge expected
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0, now)
        val ch: CurrentHouseholdRepository = mockk { every { currentHouseholdId } returns MutableStateFlow("h-1") }
        val stock: StockItemRepository = mockk { every { observe("h-1") } returns flowOf(listOf(item)) }
        val sheetVm: AddEditItemViewModel = mockk(relaxed = true) {
            every { uiState } returns MutableStateFlow(AddEditItemUiState())
        }
        composeRule.setContent {
            StockListScreen(
                viewModel = StockListViewModel(ch, stock),
                sheetViewModel = sheetVm,
            )
        }
        composeRule.onNodeWithTag("low_stock_badge_i-1").assertIsDisplayed()
    }
}
```

- [ ] **Step 3 — Run all tests**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 4 — Commit**

```bash
git add app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt \
        app/src/test/kotlin/app/pantry/ui/stock/LowStockBadgeTest.kt
git commit -m "feat(stock): low-stock badge and qty-0 visual styling"
```

- [ ] **Step 5 — Tag the Phase 2 completion**

```bash
git tag phase-2-complete
```

---

## Phase 2 done

At this point:

- Bottom navigation shell hosts Stock / Shopping / Settings
- Settings shows household name + invite code + Sign Out
- Stock tab shows a real, filterable, searchable list of items
- Items can be added, edited, deleted, and stepped inline
- Low-stock and out-of-stock items are visually distinguished
- All unit and Compose tests pass

**Next:** write Phase 3 plan (`spec/phase-3-shopping-list.md`) covering the combined auto-derived + manual shopping list and replace the Shopping placeholder.
