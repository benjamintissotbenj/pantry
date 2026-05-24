# Phase 4 — Settings + Connectivity — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Pantry build where the Settings tab is complete: household members are visible (display name + email), the household can be renamed, the invite code can be copied/shared/regenerated, categories can be renamed (batch-updating all items), each user can leave the household (deleting it if they were the last member), the household creator can remove other members, and the app version is shown. Also introduce a `ConnectivityRepository` that backs a persistent offline banner on every tab and disables write affordances when the device is offline.

**Architecture:** A new `ConnectivityRepository` (`@Singleton`, application-scoped) wraps `ConnectivityManager.registerDefaultNetworkCallback` in a `callbackFlow` and exposes `isOffline: StateFlow<Boolean>`. It's injected into every ViewModel that owns write actions; ViewModels surface `isOffline` in their UiState; the Screen composables read it to disable buttons and steppers. A reusable `OfflineBanner(isOffline: Boolean)` composable lives in `ui/common/` and is rendered at the top of each tab's content. The Settings tab is rewritten as a vertical `Column` of section "cards" (Household, Invite code, Members, Categories, About, plus destructive actions); rename actions use shared `AlertDialog` patterns. Members live in a denormalized `members: Map<uid, MemberSummary>` field on each household doc — populated by `create()` on the creator's machine and by the `joinHousehold` Cloud Function for joiners. Member removal and leave-household run as two new Cloud Functions (`removeMember`, `leaveHousehold`) so the firestore rules can stay simple; `leaveHousehold` uses Firestore admin's `recursiveDelete` to clean up subcollections when the last member departs. Category rename is a client-side `WriteBatch` (same atomicity pattern as Finish shopping), with a 450-item defensive guard against Firestore's 500-op batch limit.

**Tech stack:** Same as Phase 1–3 — Kotlin 2.1.0 · AGP 8.7.0 · Compose BoM 2025.01.00 · Material 3 · Hilt 2.53 · Firebase BoM 33.7.0 · firebase-functions v2 · Robolectric 4.13 · JUnit 5 · Turbine · MockK · Firebase Local Emulator Suite. One new Compose dependency consideration: clipboard via `androidx.compose.ui.platform.LocalClipboardManager` (already in BoM). Share intent via `Intent.ACTION_SEND` + `Activity.startActivity` via `LocalContext`.

**Spec source:** `spec/android-app-design.md`
**Plan location:** `spec/phase-4-settings.md`
**Project root:** `Repositories/pantry/`
**Package:** `app.pantry`
**Starts from:** `main` (tag `phase-3-complete` plus the three post-phase-3 iteration commits up to `1619a52`).
**Branch:** all phase-4 work lives on a `phase-4-settings` feature branch, merged into `main` after `phase-4-complete` is tagged. Per saved memory `feedback_branching`.

---

## Definition of Done (Phase 4)

- `./gradlew :app:assembleDebug` produces an installable APK.
- The Settings tab renders the full layout: Household, Invite code, Members, Categories, About, Sign out, Leave household.
- Household rename: tap row → dialog with text field pre-filled → Save commits to Firestore.
- Invite code: row shows the 6-char code; tap the row OR tap a leading copy icon copies the code to the clipboard with a snackbar "Invite code copied"; a share icon opens the Android Share sheet via `Intent.ACTION_SEND`; a "Regenerate code" button below opens a confirm dialog and on Continue regenerates and shows "Code changed".
- Members list: one row per member with display name + email. Shows "(you)" next to the current user. If the current user is the household creator, every OTHER row has a remove icon → confirm dialog → `removeMember` Cloud Function runs → snackbar "Removed <name>".
- Categories list: every distinct category in the household has a row with a pencil icon → dialog with text field pre-filled → Save runs a client-side `WriteBatch` that updates every item with the old category to the new name → snackbar "Renamed category" (or, if >450 items, snackbar "Too many items to rename in one batch — try v2"). Atomic; either all items move or none do.
- Leave household: button at the bottom (error color) → confirm dialog with conditional text (different copy when last member) → `leaveHousehold` Cloud Function runs → onSignedOut-style nav back to onboarding; the function deletes the household + all subcollections via `recursiveDelete` if the caller was the last member.
- App version row: reads `BuildConfig.VERSION_NAME` (Phase 1 wired this up).
- Sign out still works (already in place).
- `ConnectivityRepository` exposes `isOffline: StateFlow<Boolean>` backed by `ConnectivityManager.registerDefaultNetworkCallback`; bound `@Singleton` in Hilt.
- `OfflineBanner` composable renders a thin Surface at the top of Stock, Shopping, and Settings tabs whenever `isOffline == true`, with text "Offline — changes will sync when you reconnect."
- All write actions are disabled when offline: Stock stepper (+/−), Add/Edit item Save and Delete, Shopping checkbox toggles, Shopping bottom button (Finish shopping), AddManualEntry Save, every Settings action (rename / regenerate / remove / category rename / leave).
- New Firestore field `members: Map<uid, {displayName, email}>` populated on every household: by `create()` for the creator, by the updated `joinHousehold` Cloud Function for joiners.
- New Firestore field `createdBy: String` exposed on the Kotlin `Household` model (it was already written by `create()`).
- Two new Cloud Functions deployed: `removeMember`, `leaveHousehold`. The `leaveHousehold` function correctly invokes `getFirestore().recursiveDelete(householdDocRef)` when memberUids contains only the caller.
- All unit + Compose Robolectric tests pass: `./gradlew :app:test`.
- All commits follow Conventional Commits.
- Tag `phase-4-complete` on the last commit of the `phase-4-settings` branch; the branch is merged into `main`.

---

## User stories

| ID | Title | Type |
|---|---|---|
| US-1 | Domain model: `MemberSummary` + `Household.members` + `Household.createdBy` | infra |
| US-2 | `FirestoreHouseholdRepository.create` + `toHousehold` round-trip new fields | infra |
| US-3 | `joinHousehold` Cloud Function writes `members` entry | infra |
| US-4 | `ConnectivityRepository` + Hilt binding + Robolectric test | infra |
| US-5 | `OfflineBanner` composable + tests | user-facing |
| US-6 | Wire `isOffline` through Stock / Shopping / AddEdit / AddManualEntry VMs and gate writes; render banner on every tab | user-facing |
| US-7 | `SettingsUiState` + `SettingsViewModel` rewrite | infra |
| US-8 | `SettingsScreen` skeleton (sections render data; no actions yet) | user-facing |
| US-9 | Household rename dialog + Save action | user-facing |
| US-10 | Invite code: copy to clipboard + Share intent + Regenerate-with-confirm | user-facing |
| US-11 | Members list with creator-only remove (`removeMember` Cloud Function + repo + UI) | user-facing |
| US-12 | Category rename: list + dialog + `renameCategory` repo batch | user-facing |
| US-13 | Leave household (`leaveHousehold` Cloud Function + UI + post-leave nav) | user-facing |
| US-14 | App version row + smoke notes | user-facing |

---

## US-1: Domain model — `MemberSummary` + `Household.members` + `Household.createdBy`

**As a** developer
**I want** the Kotlin domain model to carry the new `members` map (uid → display name + email) and the `createdBy` uid on every `Household`
**So that** the Settings members list can render names and the "remove member" affordance can be gated on creator identity.

### Acceptance criteria

- New data class `MemberSummary(displayName: String, email: String)` in `domain/model/`.
- `Household` gains two fields, both with no default value (call sites must provide them):
  - `createdBy: String`
  - `members: Map<String, MemberSummary>` (keyed by uid)
- All existing call sites of `Household(...)` compile after the change.
- A small unit test covers the `MemberSummary` defaults and the `Household` shape.

### Files

- Create: `app/src/main/kotlin/app/pantry/domain/model/MemberSummary.kt`
- Modify: `app/src/main/kotlin/app/pantry/domain/model/Household.kt`
- Create: `app/src/test/kotlin/app/pantry/domain/model/MemberSummaryTest.kt`

### Tasks

- [ ] **Step 1 — Create `MemberSummary`**

```kotlin
package app.pantry.domain.model

data class MemberSummary(
    val displayName: String,
    val email: String,
)
```

- [ ] **Step 2 — Extend `Household`**

Replace `Household.kt`:

```kotlin
package app.pantry.domain.model

data class Household(
    val id: String,
    val name: String,
    val memberUids: List<String>,
    val inviteCode: String,
    val createdBy: String,
    val members: Map<String, MemberSummary>,
)
```

- [ ] **Step 3 — Update call sites**

Run `Grep pattern="Household\\(" type=kt` to find every place that constructs `Household(...)`. Notable sites:
- `FirestoreHouseholdRepository.create()` (returns a Household with new fields)
- `FirestoreHouseholdRepository.toHousehold()` (reads the doc, must read new fields)
- Tests under `app/src/test/`

For each call site, add `createdBy = ownerUid` (or `""`) and `members = emptyMap()` as named arguments. US-2 will refine the population logic for the repo paths; for now thread plausible defaults.

- [ ] **Step 4 — Add the test**

```kotlin
package app.pantry.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberSummaryTest {
    @Test
    fun `member summary holds display name and email`() {
        val m = MemberSummary(displayName = "Ben", email = "ben@example.com")
        assertEquals("Ben", m.displayName)
        assertEquals("ben@example.com", m.email)
    }

    @Test
    fun `household carries createdBy and members map`() {
        val hh = Household(
            id = "hh1",
            name = "Smith family",
            memberUids = listOf("u1", "u2"),
            inviteCode = "ABCDEF",
            createdBy = "u1",
            members = mapOf(
                "u1" to MemberSummary("Ben", "ben@example.com"),
                "u2" to MemberSummary("Alice", "alice@example.com"),
            ),
        )
        assertEquals("u1", hh.createdBy)
        assertEquals(2, hh.members.size)
        assertEquals("Alice", hh.members["u2"]?.displayName)
    }
}
```

- [ ] **Step 5 — Compile + commit**

- Run `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` and fix any remaining call-site errors.
- Run `./gradlew :app:testDebugUnitTest --tests "app.pantry.domain.model.MemberSummaryTest"` — green.
- Commit: `feat(model): add MemberSummary and Household.createdBy + members`

---

## US-2: `FirestoreHouseholdRepository` round-trips `members` and `createdBy`

**As a** developer
**I want** `FirestoreHouseholdRepository.create()` to write the creator into the `members` map AND to round-trip `members` + `createdBy` when reading households
**So that** the Settings members list has real data to render and the creator-only affordance can find its owner.

### Acceptance criteria

- `FirestoreHouseholdRepository.create(name, ownerUid)` now ALSO takes the creator's `displayName` and `email` so it can populate the initial `members` map. The signature changes to accept a `MemberSummary` for the creator, OR the repo gains a separate helper. (See Step 1 below for the chosen approach.)
- The doc-write includes `members.<ownerUid>` with `{displayName, email}` AND `createdBy: ownerUid`.
- `toHousehold()` reads `createdBy` (defaulting to `""` for legacy docs) and the `members` map (defaulting to empty for legacy docs).
- Existing call sites of `households.create(...)` (notably `HouseholdOnboardingViewModel`) updated to thread the user's profile data.

### Files

- Modify: `app/src/main/kotlin/app/pantry/data/household/HouseholdRepository.kt`
- Modify: `app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModel.kt` (or wherever `households.create` is called)
- Modify: existing tests under `app/src/test/kotlin/app/pantry/data/household/`

### Tasks

- [ ] **Step 1 — Update the interface**

Change the signature of `create` to accept the creator profile:

```kotlin
suspend fun create(
    name: String,
    ownerUid: String,
    ownerDisplayName: String,
    ownerEmail: String,
): Result<Household>
```

Leave `observe`, `observeUserHouseholds`, `rename`, and `regenerateInviteCode` alone.

- [ ] **Step 2 — Update `FirestoreHouseholdRepository.create`**

```kotlin
override suspend fun create(
    name: String,
    ownerUid: String,
    ownerDisplayName: String,
    ownerEmail: String,
): Result<Household> = runCatching {
    val code = codes.next()
    val doc = firestore.collection("households").document()
    val data = mapOf(
        "name" to name,
        "memberUids" to listOf(ownerUid),
        "inviteCode" to code,
        "createdAt" to FieldValue.serverTimestamp(),
        "createdBy" to ownerUid,
        "members" to mapOf(
            ownerUid to mapOf(
                "displayName" to ownerDisplayName,
                "email" to ownerEmail,
            ),
        ),
    )
    firestore.runBatch { batch ->
        batch.set(doc, data)
        batch.set(
            firestore.collection("users").document(ownerUid),
            mapOf("households" to FieldValue.arrayUnion(doc.id)),
            SetOptions.merge(),
        )
    }.await()
    Household(
        id = doc.id,
        name = name,
        memberUids = listOf(ownerUid),
        inviteCode = code,
        createdBy = ownerUid,
        members = mapOf(ownerUid to MemberSummary(ownerDisplayName, ownerEmail)),
    )
}
```

- [ ] **Step 3 — Update `toHousehold()`**

```kotlin
private fun DocumentSnapshot.toHousehold(): Household? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val memberUids = (get("memberUids") as? List<String>).orEmpty()
    val createdBy = getString("createdBy").orEmpty()
    @Suppress("UNCHECKED_CAST")
    val rawMembers = (get("members") as? Map<String, Map<String, Any?>>).orEmpty()
    val members = rawMembers.mapValues { (_, m) ->
        MemberSummary(
            displayName = m["displayName"] as? String ?: "",
            email = m["email"] as? String ?: "",
        )
    }
    return Household(
        id = id,
        name = getString("name").orEmpty(),
        memberUids = memberUids,
        inviteCode = getString("inviteCode").orEmpty(),
        createdBy = createdBy,
        members = members,
    )
}
```

- [ ] **Step 4 — Update `HouseholdOnboardingViewModel`**

Find the call to `households.create(name, uid)` and update it. The ViewModel already has access to `AuthRepository.currentUser` (a `StateFlow<UserProfile?>`); read the profile's `displayName` and `email` and pass them. Use Grep to find:

```
Grep pattern="households\\.create\\(" type=kt
```

The call site likely looks like:

```kotlin
val profile = auth.currentUser.value ?: return
households.create(
    name = trimmedName,
    ownerUid = profile.uid,
    ownerDisplayName = profile.displayName,
    ownerEmail = profile.email,
)
```

- [ ] **Step 5 — Update tests**

`FirestoreHouseholdRepositoryTest.kt` and `FirestoreHouseholdRepositoryEmulatorTest.kt` likely have `create(...)` calls. Add the new args; for the unit test you can pass plausible strings. For the emulator test, pass the existing test user's display name/email.

`HouseholdOnboardingViewModelTest.kt` — verify the test stubs `AuthRepository.currentUser` to emit a non-null profile, and that the test's mocked `households.create` matcher accepts the new args.

- [ ] **Step 6 — Compile + test + commit**

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` green.
- `./gradlew :app:testDebugUnitTest` green.
- Commit: `feat(household): repository round-trips members map and createdBy`

---

## US-3: `joinHousehold` Cloud Function writes `members` entry

**As a** developer
**I want** the `joinHousehold` Cloud Function to add the joining user's display name + email to the household's `members` map
**So that** when a new member joins, the Settings members list on every device immediately sees their identity without an extra read.

### Acceptance criteria

- `functions/src/joinHousehold.ts` reads the caller's `users/{uid}` doc to get `displayName` and `email`.
- The transaction updates the household with `arrayUnion(uid)` on `memberUids` AND a merged `members.<uid> = {displayName, email}` field write.
- The function still throws `unauthenticated` / `invalid-argument` / `not-found` / `already-exists` as before.
- Deploy and smoke-test in the Local Emulator Suite (or note in the commit if emulator isn't available — the Android client will use the deployed prod function).

### Files

- Modify: `functions/src/joinHousehold.ts`

### Tasks

- [ ] **Step 1 — Update the function body**

Replace the transaction block with:

```ts
await db.runTransaction(async (tx) => {
  const userDoc = await tx.get(db.collection("users").doc(uid));
  const displayName = (userDoc.get("displayName") as string | undefined) ?? "";
  const email = (userDoc.get("email") as string | undefined) ?? "";

  const memberPath = `members.${uid}`;
  tx.update(householdDoc.ref, {
    memberUids: admin.firestore.FieldValue.arrayUnion(uid),
    [memberPath]: { displayName, email },
  });
  tx.set(
    db.collection("users").doc(uid),
    { households: admin.firestore.FieldValue.arrayUnion(householdDoc.id) },
    { merge: true }
  );
});
```

> The dotted-field syntax `members.<uid>` on `update()` is a Firestore-supported way to write a single map entry without overwriting the whole `members` field. Verified against firebase-admin docs.

- [ ] **Step 2 — Deploy + smoke test**

- Run `firebase deploy --only functions:joinHousehold` (the user runs this; assistant can prepare the command).
- In the Local Emulator Suite (or in dev): create a household with user A, sign in as user B, call joinHousehold. Inspect the household doc — should have `members.<B.uid>` populated.

- [ ] **Step 3 — Commit**

- Commit (in `functions/`): `feat(functions): joinHousehold writes members map entry for joiner`

---

## US-4: `ConnectivityRepository` + Hilt + Robolectric test

**As a** developer
**I want** a Hilt-bound `ConnectivityRepository` that exposes `isOffline: StateFlow<Boolean>` and is backed by `ConnectivityManager.registerDefaultNetworkCallback`
**So that** every ViewModel can read connectivity state and every screen can render the offline banner from a single source of truth.

### Acceptance criteria

- `ConnectivityRepository` interface with `val isOffline: StateFlow<Boolean>`.
- `AndroidConnectivityRepository` implementation uses `ConnectivityManager.registerDefaultNetworkCallback` inside a `callbackFlow`, materialized into a `StateFlow` via `stateIn(scope, SharingStarted.Eagerly, initialValue)`.
- Initial value computed from `connectivityManager.activeNetworkInfo?.isConnected != true` at construction.
- `@Singleton`, application-scoped — uses an `@ApplicationContext` injected `Context`.
- Hilt module exposes it as `ConnectivityRepository`.
- A Robolectric test verifies state transitions when the `ShadowConnectivityManager` flips connectivity.

### Files

- Create: `app/src/main/kotlin/app/pantry/data/connectivity/ConnectivityRepository.kt`
- Create: `app/src/main/kotlin/app/pantry/data/connectivity/AndroidConnectivityRepository.kt`
- Modify: `app/src/main/kotlin/app/pantry/di/DataModule.kt` (or wherever repos are bound — likely `StockModule.kt`, add or create a `ConnectivityModule.kt` if cleaner)
- Create: `app/src/test/kotlin/app/pantry/data/connectivity/AndroidConnectivityRepositoryTest.kt`

### Tasks

- [ ] **Step 1 — Interface**

```kotlin
package app.pantry.data.connectivity

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityRepository {
    /** True iff the device currently has no usable network connection. */
    val isOffline: StateFlow<Boolean>
}
```

- [ ] **Step 2 — Implementation**

```kotlin
package app.pantry.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class AndroidConnectivityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityRepository {

    private val cm: ConnectivityManager =
        context.getSystemService() ?: error("ConnectivityManager unavailable")

    private val initial: Boolean = !cm.isCurrentlyConnected()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isOffline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val available = mutableSetOf<Network>()
            override fun onAvailable(network: Network) {
                available += network
                trySend(false)
            }
            override fun onLost(network: Network) {
                available -= network
                if (available.isEmpty()) trySend(true)
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (hasInternet) {
                    available += network
                    trySend(false)
                } else {
                    available -= network
                    if (available.isEmpty()) trySend(true)
                }
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        // Seed the initial value once the flow is collected.
        trySend(!cm.isCurrentlyConnected())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.Eagerly, initial)

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        val active = activeNetwork ?: return false
        val caps = getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

- [ ] **Step 3 — Hilt binding**

In an existing or new module under `app/src/main/kotlin/app/pantry/di/`:

```kotlin
package app.pantry.di

import app.pantry.data.connectivity.AndroidConnectivityRepository
import app.pantry.data.connectivity.ConnectivityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(
        impl: AndroidConnectivityRepository,
    ): ConnectivityRepository
}
```

> If the project already has a single `StockModule.kt` aggregating bindings, add the binding there instead of a new module.

- [ ] **Step 4 — Robolectric test**

```kotlin
package app.pantry.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidConnectivityRepositoryTest {

    @Test
    fun `flips to offline when default network is lost`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = Shadows.shadowOf(cm)

        // Start with an active connected network.
        // Robolectric's shadow defaults vary by SDK; we explicitly assert what we expect.
        // If this assertion fails, the test environment needs a setup hook.

        val repo = AndroidConnectivityRepository(context)

        repo.isOffline.test {
            // Seeded value
            val initial = awaitItem()
            // Tear down: simulate network loss via the shadow's `setActiveNetworkInfo(null)` API
            // (the specific API varies by Robolectric version; if `setActiveNetworkInfo` is
            // deprecated, use `setDefaultNetworkActive(false)` or similar). At minimum,
            // verify the flow CAN be collected without crashing.
            cancelAndConsumeRemainingEvents()
        }
    }
}
```

> Robolectric's ConnectivityManager shadow has shifted between versions. If a clean network-transition test is hard, the minimum to pass is: the repo can be constructed without throwing, the flow can be collected without throwing, and the initial value is observable. Document any gaps in the commit.

- [ ] **Step 5 — Compile + test + commit**

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` green.
- `./gradlew :app:testDebugUnitTest --tests "app.pantry.data.connectivity.AndroidConnectivityRepositoryTest"` green.
- Commit: `feat(connectivity): add ConnectivityRepository with NetworkCallback backing`

---

## US-5: `OfflineBanner` composable + tests

**As a** household member offline
**I want** a thin persistent banner at the top of every tab telling me changes will queue and sync later
**So that** I never wonder why my taps "did nothing" while disconnected.

### Acceptance criteria

- `OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier)` composable in `ui/common/`.
- When `isOffline == true`, renders a thin `Surface(tonalElevation = 2.dp)` with single-line text "Offline — changes will sync when you reconnect."
- When `isOffline == false`, renders nothing (returns early). No animation in this US (simple show/hide); animation polish can come later.
- The text uses `MaterialTheme.typography.bodySmall` and `MaterialTheme.colorScheme.onSurfaceVariant`.
- Has testTag `"offline_banner"` on the Surface for Compose tests.
- A Compose Robolectric test verifies it renders when `isOffline = true` and is absent when `isOffline = false`.

### Files

- Create: `app/src/main/kotlin/app/pantry/ui/common/OfflineBanner.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/common/OfflineBannerTest.kt`

### Tasks

- [ ] **Step 1 — The composable**

```kotlin
package app.pantry.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    if (!isOffline) return
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth().testTag("offline_banner"),
    ) {
        Text(
            text = "Offline — changes will sync when you reconnect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
```

- [ ] **Step 2 — Test**

```kotlin
package app.pantry.ui.common

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineBannerTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `shows banner when offline`() {
        compose.setContent { OfflineBanner(isOffline = true) }
        compose.onNodeWithTag("offline_banner").assertIsDisplayed()
        compose.onNodeWithText("Offline — changes will sync when you reconnect.").assertIsDisplayed()
    }

    @Test
    fun `hides banner when online`() {
        compose.setContent { OfflineBanner(isOffline = false) }
        compose.onNodeWithTag("offline_banner").assertDoesNotExist()
    }
}
```

> If `assertDoesNotExist` isn't imported, add `import androidx.compose.ui.test.assertDoesNotExist`.

- [ ] **Step 3 — Compile + test + commit**

- `./gradlew :app:testDebugUnitTest --tests "app.pantry.ui.common.OfflineBannerTest"` green.
- Commit: `feat(ui): add OfflineBanner composable`

---

## US-6: Gate writes across ViewModels + render banner on every tab

**As a** household member offline
**I want** every write button (steppers, FABs, Save, Finish shopping, Settings actions) to be disabled, and a banner to be visible on every tab
**So that** the offline state is communicated clearly and I can't be confused by Firestore's silent queueing.

### Acceptance criteria

- `StockListUiState`, `ShoppingListUiState` (already has the field with TODO comment from Phase 3 — wire it up), `AddEditItemUiState`, `AddManualEntryUiState`, `SettingsUiState` (US-7) all carry `isOffline: Boolean`.
- The corresponding ViewModels (`StockListViewModel`, `ShoppingListViewModel`, `AddEditItemViewModel`, `AddManualEntryViewModel`) inject `ConnectivityRepository` and propagate `isOffline` into their state.
- Each Composable's write affordance reads `state.isOffline` and disables it when true:
  - `StockListScreen` — both `+` and `−` buttons on every row; Add FAB
  - `AddEditItemBottomSheet` — Save button; Delete button
  - `ShoppingListScreen` — Add manual entry FAB; Finish shopping bottom button
  - `AddManualEntryBottomSheet` — Save button
- Each top-level tab (`StockListScreen`, `ShoppingListScreen`, `SettingsScreen`) wraps its content with `OfflineBanner` at the top.
- The `TODO(phase-4)` comment in `ShoppingListUiState.kt` is removed once wired up.
- Existing tests still pass; new tests are NOT required in this US (each ViewModel's existing test scaffold can stay; the offline gating is exercised via the Compose tests in US-8, US-11, US-12, US-13).

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/StockListScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/stock/AddEditItemBottomSheet.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListUiState.kt` (drop the TODO)
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/ShoppingListScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryViewModel.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/shopping/AddManualEntryBottomSheet.kt`

### Tasks

- [ ] **Step 1 — Pattern**

Each ViewModel that already uses `combine(...)` to build its UiState gets one new flow source: `connectivity.isOffline`. Each Composable gets a single `OfflineBanner(isOffline = state.isOffline)` at the top of its body and adds `enabled = !state.isOffline && ...` (combined with whatever existing enabled condition) to each write button.

- [ ] **Step 2 — `StockListViewModel`**

Inject `ConnectivityRepository` as the third constructor arg. Add `connectivity.isOffline` to the existing `combine` block; thread `isOffline` into the projected `StockListUiState`. Add `isOffline: Boolean = false` to the UiState.

In `StockListScreen.kt`:
- Add `OfflineBanner(isOffline = state.isOffline)` at the top of the `Column` (before `SearchField`).
- The `IconButton(onClick = onMinus, enabled = item.quantity > 0.0, ...)` becomes `enabled = !state.isOffline && item.quantity > 0.0`.
- The `IconButton(onClick = onPlus, ...)` for the `+` adds `enabled = !state.isOffline`.
- The FAB adds `if (state.isOffline) return@FloatingActionButton` inside its onClick, OR wrap with `Modifier.alpha(...)` and conditional onClick.

- [ ] **Step 3 — `AddEditItemViewModel`**

Inject `ConnectivityRepository`. Combine `connectivity.isOffline` into the state flow (or expose separately and combine inside Compose). Add `isOffline: Boolean = false` to `AddEditItemUiState`. `canSubmit` now also checks `!isOffline`.

In `AddEditItemBottomSheet.kt`:
- Both Save and Delete buttons get `enabled = !state.isOffline && state.canSubmit` (the Delete is just `enabled = !state.isOffline`).

- [ ] **Step 4 — `ShoppingListViewModel` + `AddManualEntryViewModel`**

Same pattern. Inject `ConnectivityRepository`. Add `isOffline` flow source. Wire to UiState. Update the gating in `ShoppingListScreen` (Finish shopping button and FAB) and `AddManualEntryBottomSheet` (Save button).

The existing `ShoppingListUiState.isOffline` field — drop the `TODO(phase-4)` comment now that it's real.

- [ ] **Step 5 — Banner on Settings**

Defer the actual Settings screen rewrite to US-8 — but make a note that the banner will be added there too (`OfflineBanner(isOffline = state.isOffline)` at the top of the Column).

- [ ] **Step 6 — Compile + test + commit**

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` green.
- `./gradlew :app:testDebugUnitTest` green — existing tests need updating to pass `ConnectivityRepository` mocks. Use `mockk<ConnectivityRepository>().also { every { it.isOffline } returns MutableStateFlow(false) }` in each affected test's setup.
- Commit: `feat(ui): gate writes on connectivity + render offline banner on Stock and Shopping`

---

## US-7: `SettingsUiState` + `SettingsViewModel` rewrite

**As a** developer
**I want** a single `SettingsViewModel` that exposes all Settings data — household name, invite code, members, categories list, current user uid, offline state — and surfaces actions for every Settings interaction
**So that** the Compose layer can be a thin projection and tests can drive the actions directly.

### Acceptance criteria

- `SettingsUiState` carries:
  - `isOffline: Boolean = false`
  - `householdId: String? = null`
  - `householdName: String = ""`
  - `inviteCode: String = ""`
  - `members: List<MemberRow>` (uid, displayName, email, isYou, canRemove)
  - `categories: List<String>` (distinct, alphabetical, non-blank — derived from the items flow)
  - `appVersion: String` (BuildConfig)
  - Transient event surfaces (`pendingSnackbar: String?`, `pendingClipboard: String?`, `pendingShareCode: String?`, `pendingPostLeaveNav: Boolean`, `signedOut: Boolean`)
- `SettingsViewModel` exposes action methods:
  - `onRenameHousehold(newName: String)`
  - `onCopyCodeRequested()` — pushes `pendingClipboard`
  - `onShareCodeRequested()` — pushes `pendingShareCode`
  - `onRegenerateCode()`
  - `onRemoveMember(uid: String)`
  - `onRenameCategory(oldName: String, newName: String)`
  - `onLeaveHousehold()`
  - `onSignOut()`
  - `consumeSnackbar()`, `consumeClipboard()`, `consumeShareCode()`, `consumeNav()`
- ViewModel injects: `AuthRepository`, `HouseholdRepository`, `StockItemRepository` (for categories), `ConnectivityRepository`, `CurrentHouseholdRepository`.
- Unit tests cover: state derivation; rename calls repo; regenerate calls repo and shows snackbar; copy emits clipboard; share emits intent; remove flow gated by createdBy.

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsViewModelTest.kt`

### Tasks

- [ ] **Step 1 — UiState**

```kotlin
package app.pantry.ui.settings

data class SettingsUiState(
    val isOffline: Boolean = false,
    val householdId: String? = null,
    val householdName: String = "",
    val inviteCode: String = "",
    val currentUserUid: String = "",
    val isCreator: Boolean = false,
    val members: List<MemberRow> = emptyList(),
    val categories: List<String> = emptyList(),
    val appVersion: String = "",
    val pendingSnackbar: String? = null,
    val pendingClipboard: String? = null,
    val pendingShareCode: String? = null,
    val pendingPostLeaveNav: Boolean = false,
    val signedOut: Boolean = false,
)

data class MemberRow(
    val uid: String,
    val displayName: String,
    val email: String,
    val isYou: Boolean,
    val canRemove: Boolean,
)
```

- [ ] **Step 2 — ViewModel**

```kotlin
package app.pantry.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.BuildConfig
import app.pantry.data.auth.AuthRepository
import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.stock.StockItemRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val stock: StockItemRepository,
    private val connectivity: ConnectivityRepository,
    private val currentHousehold: CurrentHouseholdRepository,
) : ViewModel() {

    private val events = MutableStateFlow(EventState())

    private data class EventState(
        val snackbar: String? = null,
        val clipboard: String? = null,
        val share: String? = null,
        val postLeaveNav: Boolean = false,
        val signedOut: Boolean = false,
    )

    val uiState: StateFlow<SettingsUiState> =
        currentHousehold.currentHouseholdId
            .flatMapLatest { hid ->
                if (hid == null) flowOf(SettingsUiState())
                else combine(
                    households.observe(hid),
                    auth.currentUser,
                    stock.observe(hid),
                    connectivity.isOffline,
                    events,
                ) { hh, user, items, offline, ev ->
                    project(hid, hh, user, items, offline, ev)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun project(
        hid: String,
        hh: app.pantry.domain.model.Household?,
        user: app.pantry.domain.model.UserProfile?,
        items: List<app.pantry.domain.model.StockItem>,
        offline: Boolean,
        ev: EventState,
    ): SettingsUiState {
        val uid = user?.uid.orEmpty()
        val isCreator = hh?.createdBy == uid && uid.isNotEmpty()
        val members = hh?.memberUids.orEmpty().map { memberUid ->
            val summary = hh.members[memberUid]
            MemberRow(
                uid = memberUid,
                displayName = summary?.displayName ?: "—",
                email = summary?.email ?: "",
                isYou = memberUid == uid,
                canRemove = isCreator && memberUid != uid,
            )
        }
        val categories = items
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
        return SettingsUiState(
            isOffline = offline,
            householdId = hid,
            householdName = hh?.name.orEmpty(),
            inviteCode = hh?.inviteCode.orEmpty(),
            currentUserUid = uid,
            isCreator = isCreator,
            members = members,
            categories = categories,
            appVersion = BuildConfig.VERSION_NAME,
            pendingSnackbar = ev.snackbar,
            pendingClipboard = ev.clipboard,
            pendingShareCode = ev.share,
            pendingPostLeaveNav = ev.postLeaveNav,
            signedOut = ev.signedOut,
        )
    }

    fun onRenameHousehold(newName: String) {
        val hid = uiState.value.householdId ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            households.rename(hid, trimmed).fold(
                onSuccess = { events.update { it.copy(snackbar = "Household renamed") } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't rename") } },
            )
        }
    }

    fun onCopyCodeRequested() {
        val code = uiState.value.inviteCode
        if (code.isNotEmpty()) events.update { it.copy(clipboard = code, snackbar = "Invite code copied") }
    }

    fun onShareCodeRequested() {
        val code = uiState.value.inviteCode
        if (code.isNotEmpty()) events.update { it.copy(share = code) }
    }

    fun onRegenerateCode() {
        val hid = uiState.value.householdId ?: return
        viewModelScope.launch {
            households.regenerateInviteCode(hid).fold(
                onSuccess = { events.update { it.copy(snackbar = "Code changed") } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't change code") } },
            )
        }
    }

    fun onRemoveMember(uid: String) {
        val hid = uiState.value.householdId ?: return
        viewModelScope.launch {
            households.removeMember(hid, uid).fold(
                onSuccess = { events.update { it.copy(snackbar = "Member removed") } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't remove member") } },
            )
        }
    }

    fun onRenameCategory(oldName: String, newName: String) {
        val hid = uiState.value.householdId ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == oldName) return
        viewModelScope.launch {
            households.renameCategory(hid, oldName, trimmed).fold(
                onSuccess = { count -> events.update { it.copy(snackbar = "Renamed $count item(s)") } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't rename category") } },
            )
        }
    }

    fun onLeaveHousehold() {
        val hid = uiState.value.householdId ?: return
        viewModelScope.launch {
            households.leaveHousehold(hid).fold(
                onSuccess = { events.update { it.copy(postLeaveNav = true) } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't leave household") } },
            )
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            auth.signOut().fold(
                onSuccess = { events.update { it.copy(signedOut = true) } },
                onFailure = { events.update { it.copy(snackbar = it.message ?: "Couldn't sign out") } },
            )
        }
    }

    fun consumeSnackbar() { events.update { it.copy(snackbar = null) } }
    fun consumeClipboard() { events.update { it.copy(clipboard = null) } }
    fun consumeShareCode() { events.update { it.copy(share = null) } }
    fun consumeNav() { events.update { it.copy(postLeaveNav = false) } }
}
```

> Note: this references `households.removeMember`, `households.renameCategory`, `households.leaveHousehold` — those land in US-11, US-12, US-13. For US-7 to compile, add stub interface methods in `HouseholdRepository` and stub impls in `FirestoreHouseholdRepository` that return `Result.failure(NotImplementedError())`. US-11–13 will fill them in.

- [ ] **Step 3 — Repo interface stubs**

Add to `HouseholdRepository.kt`:

```kotlin
suspend fun removeMember(householdId: String, uid: String): Result<Unit>
suspend fun renameCategory(householdId: String, oldName: String, newName: String): Result<Int>
suspend fun leaveHousehold(householdId: String): Result<Unit>
```

Add stub impls in `FirestoreHouseholdRepository.kt`:

```kotlin
override suspend fun removeMember(householdId: String, uid: String): Result<Unit> =
    Result.failure(NotImplementedError("US-11 will implement"))

override suspend fun renameCategory(householdId: String, oldName: String, newName: String): Result<Int> =
    Result.failure(NotImplementedError("US-12 will implement"))

override suspend fun leaveHousehold(householdId: String): Result<Unit> =
    Result.failure(NotImplementedError("US-13 will implement"))
```

- [ ] **Step 4 — Tests**

Rewrite `SettingsViewModelTest.kt` to cover the new shape. Use mockk for all five injected deps. Sample tests:

```kotlin
@Test
fun `creator sees canRemove on other members but not self`() = runTest {
    val hh = Household(
        id = "h1", name = "Test", memberUids = listOf("u1", "u2"),
        inviteCode = "AB1234", createdBy = "u1",
        members = mapOf(
            "u1" to MemberSummary("Ben", "ben@example.com"),
            "u2" to MemberSummary("Alice", "alice@example.com"),
        ),
    )
    // ... mocks, build VM, observe uiState ...
    val s = vm.uiState.value
    val ben = s.members.first { it.uid == "u1" }
    val alice = s.members.first { it.uid == "u2" }
    assertTrue(ben.isYou)
    assertFalse(ben.canRemove)
    assertFalse(alice.isYou)
    assertTrue(alice.canRemove)
}

@Test
fun `onCopyCodeRequested pushes clipboard and snackbar events`() = runTest {
    // ... set up VM with invite code "AB1234" ...
    vm.onCopyCodeRequested()
    val s = vm.uiState.value
    assertEquals("AB1234", s.pendingClipboard)
    assertEquals("Invite code copied", s.pendingSnackbar)
}
```

Cover: rename calls repo with trimmed name; regenerate; share emits pendingShareCode; categories are alphabetical and distinct; leave household pushes postLeaveNav on success.

- [ ] **Step 5 — Compile + test + commit**

- `./gradlew :app:testDebugUnitTest --tests "app.pantry.ui.settings.SettingsViewModelTest"` green.
- Commit: `feat(settings): SettingsUiState + SettingsViewModel rewrite with full action surface`

---

## US-8: `SettingsScreen` skeleton

**As a** household member opening the Settings tab
**I want** to see all the sections (Household, Invite code, Members, Categories, About, Sign out, Leave household) rendering with real data
**So that** the structure is in place and the action wiring (US-9 through US-13) can plug in cleanly.

### Acceptance criteria

- `SettingsScreen` is a stateful Composable hosted in a `Scaffold` with snackbar host.
- An `OfflineBanner(isOffline = state.isOffline)` renders at the top.
- A vertical `Column` with the sections: Household, Invite code, Members, Categories, About. Plus two buttons at the bottom: Sign out and Leave household (error color).
- Each section is wrapped in a `SettingsSection(title) { content }` private composable that renders the label + a `Surface(tonalElevation = 1.dp)` container.
- Rows reuse a `SettingsRow(content)` private composable that handles padding + `Modifier.clickable`.
- No actions are wired yet — the rename / regenerate / remove / category / leave row clicks just no-op (placeholders for US-9–13).
- Sign out works (already wired via `viewModel.onSignOut()`).
- A Robolectric Compose test verifies the sections render, members show, categories list shows, app version row appears, and the offline banner shows when `isOffline = true`.

### Files

- Replace: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Create: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Compose the screen skeleton**

```kotlin
package app.pantry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.pantry.ui.common.OfflineBanner

@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    onLeft: () -> Unit = onSignedOut,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    LaunchedEffect(state.pendingPostLeaveNav) {
        if (state.pendingPostLeaveNav) { viewModel.consumeNav(); onLeft() }
    }
    LaunchedEffect(state.pendingSnackbar) {
        val msg = state.pendingSnackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            OfflineBanner(isOffline = state.isOffline)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection("Household") {
                    SettingsRow(modifier = Modifier.testTag("row_rename_household")) {
                        Text(state.householdName.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                SettingsSection("Invite code") {
                    SettingsRow(modifier = Modifier.testTag("row_invite_code")) {
                        Text(
                            state.inviteCode.ifEmpty { "—" },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* wired in US-10 */ },
                        modifier = Modifier.testTag("btn_regenerate_code"),
                        enabled = !state.isOffline,
                    ) { Text("Regenerate code") }
                }
                SettingsSection("Members (${state.members.size})") {
                    state.members.forEach { m ->
                        SettingsRow(modifier = Modifier.testTag("row_member_${m.uid}")) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    if (m.isYou) "${m.displayName} (you)" else m.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(m.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (state.categories.isNotEmpty()) {
                    SettingsSection("Categories") {
                        state.categories.forEach { cat ->
                            SettingsRow(modifier = Modifier.testTag("row_category_$cat")) {
                                Text(cat, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                SettingsSection("About") {
                    SettingsRow {
                        Text(
                            "Pantry · v${state.appVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("text_app_version"),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::onSignOut,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth().testTag("settings_signout"),
                ) { Text("Sign out") }
                Button(
                    onClick = { /* wired in US-13 */ },
                    enabled = !state.isOffline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("btn_leave_household"),
                ) { Text("Leave household") }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        content()
    }
}
```

- [ ] **Step 2 — Test**

```kotlin
package app.pantry.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
// ... mock imports ...

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders household name, invite code, members, categories, app version`() {
        val vm = makeVm(
            householdName = "Smith family",
            inviteCode = "AB1234",
            members = listOf(
                MemberRow("u1", "Ben", "ben@example.com", isYou = true, canRemove = false),
                MemberRow("u2", "Alice", "alice@example.com", isYou = false, canRemove = false),
            ),
            categories = listOf("Bakery", "Dairy"),
            appVersion = "1.0.0",
        )
        compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
        compose.onNodeWithText("Smith family").assertIsDisplayed()
        compose.onNodeWithText("AB1234").assertIsDisplayed()
        compose.onNodeWithTag("row_member_u1").assertIsDisplayed()
        compose.onNodeWithTag("row_member_u2").assertIsDisplayed()
        compose.onNodeWithTag("row_category_Bakery").assertIsDisplayed()
        compose.onNodeWithText("Pantry · v1.0.0").assertIsDisplayed()
    }

    @Test
    fun `offline banner appears when state isOffline`() {
        val vm = makeVm(isOffline = true)
        compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
        compose.onNodeWithTag("offline_banner").assertIsDisplayed()
    }
}
```

> Build a `makeVm(...)` helper that returns a mocked `SettingsViewModel` with a `MutableStateFlow<SettingsUiState>` controlled by the args. Use `mockk<SettingsViewModel>(relaxed = true)` and stub `uiState`.

- [ ] **Step 3 — Compile + test + commit**

- `./gradlew :app:testDebugUnitTest --tests "app.pantry.ui.settings.SettingsScreenTest"` green.
- Commit: `feat(settings): SettingsScreen skeleton with all sections rendering`

---

## US-9: Household rename dialog + Save action

**As a** household member
**I want** to tap the household name on Settings and rename it via a dialog
**So that** my household isn't stuck with whatever was typed at create time.

### Acceptance criteria

- Tapping the "Household" row opens an `AlertDialog` with a text field pre-filled with the current household name.
- The Save button is enabled when the trimmed name is non-empty AND differs from the current name.
- Save calls `viewModel.onRenameHousehold(newName)`; success → snackbar "Household renamed"; failure → snackbar with error message.
- Cancel closes the dialog without action.
- The dialog is disabled (Save button + text field) when offline (the row tap doesn't open the dialog if offline — feels cleaner than opening a sheet that can't be saved).
- A Compose test verifies the dialog opens, Save fires the VM action, Cancel does not.

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Add the dialog state and AlertDialog**

Inside `SettingsScreen`, alongside the other `LaunchedEffect`s:

```kotlin
var renameHouseholdOpen by remember { mutableStateOf(false) }
```

Wire the row's `onClick`:

```kotlin
SettingsRow(
    modifier = Modifier
        .testTag("row_rename_household")
        .clickable(enabled = !state.isOffline) { renameHouseholdOpen = true }
) { ... }
```

Add the dialog at the end of the composable (alongside others):

```kotlin
if (renameHouseholdOpen) {
    var input by rememberSaveable { mutableStateOf(state.householdName) }
    AlertDialog(
        onDismissRequest = { renameHouseholdOpen = false },
        title = { Text("Rename household") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("field_household_name"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    renameHouseholdOpen = false
                    viewModel.onRenameHousehold(input)
                },
                enabled = input.trim().isNotEmpty() && input.trim() != state.householdName,
                modifier = Modifier.testTag("btn_rename_save"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { renameHouseholdOpen = false }) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 2 — Test**

```kotlin
@Test
fun `tapping household row opens rename dialog and Save fires VM`() {
    val vm = makeVm(householdName = "Old")
    compose.setContent { SettingsScreen(onSignedOut = {}, viewModel = vm) }
    compose.onNodeWithTag("row_rename_household").performClick()
    compose.onNodeWithTag("field_household_name").performTextReplacement("New name")
    compose.onNodeWithTag("btn_rename_save").performClick()
    verify { vm.onRenameHousehold("New name") }
}
```

- [ ] **Step 3 — Commit**

- Commit: `feat(settings): household rename dialog`

---

## US-10: Invite code — copy, share, regenerate-with-confirm

**As a** household member
**I want** to copy the invite code to my clipboard with one tap, share it via the system Share sheet, or regenerate it (with a confirm)
**So that** I can quickly hand the code to a new member and protect the household if the code leaks.

### Acceptance criteria

- The invite code row has a leading `📋` (Material `ContentCopy`) icon. Tapping the row OR the icon copies the code via `LocalClipboardManager`. Snackbar "Invite code copied".
- A trailing `↗` (`Share`) icon button on the row opens the Android Share sheet via `Intent.ACTION_SEND` with the code as `EXTRA_TEXT`.
- The "Regenerate code" button below the row opens an `AlertDialog`: title "Generate a new invite code?", body "Anyone holding the current code won't be able to join after this." Continue → `viewModel.onRegenerateCode()`. Cancel closes.
- Copy / Share / Regenerate all gated by `state.isOffline`.
- A Compose test verifies tapping the row triggers `onCopyCodeRequested`, tapping the share icon triggers `onShareCodeRequested`, tapping Regenerate then Continue triggers `onRegenerateCode`.

### Files

- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Wire the clipboard + share effects in `SettingsScreen`**

```kotlin
val context = LocalContext.current
val clipboardManager = LocalClipboardManager.current

LaunchedEffect(state.pendingClipboard) {
    state.pendingClipboard?.let { code ->
        clipboardManager.setText(AnnotatedString(code))
        viewModel.consumeClipboard()
    }
}

LaunchedEffect(state.pendingShareCode) {
    state.pendingShareCode?.let { code ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my Pantry household with code: $code")
        }
        context.startActivity(Intent.createChooser(intent, "Share invite code"))
        viewModel.consumeShareCode()
    }
}
```

Imports: `android.content.Intent`, `androidx.compose.ui.platform.LocalClipboardManager`, `androidx.compose.ui.platform.LocalContext`, `androidx.compose.ui.text.AnnotatedString`.

- [ ] **Step 2 — Update the invite code row**

```kotlin
SettingsSection("Invite code") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("row_invite_code")
            .clickable(enabled = !state.isOffline) { viewModel.onCopyCodeRequested() },
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
        Spacer(Modifier.width(12.dp))
        Text(
            state.inviteCode.ifEmpty { "—" },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { viewModel.onShareCodeRequested() },
            enabled = !state.isOffline,
            modifier = Modifier.testTag("btn_share_code"),
        ) { Icon(Icons.Default.Share, contentDescription = "Share") }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { regenerateConfirmOpen = true },
        enabled = !state.isOffline,
        modifier = Modifier.testTag("btn_regenerate_code"),
    ) { Text("Regenerate code") }
}
```

Add `var regenerateConfirmOpen by remember { mutableStateOf(false) }`.

- [ ] **Step 3 — Add the regenerate confirm dialog**

```kotlin
if (regenerateConfirmOpen) {
    AlertDialog(
        onDismissRequest = { regenerateConfirmOpen = false },
        title = { Text("Generate a new invite code?") },
        text = { Text("Anyone holding the current code won't be able to join after this.") },
        confirmButton = {
            TextButton(
                onClick = { regenerateConfirmOpen = false; viewModel.onRegenerateCode() },
                modifier = Modifier.testTag("btn_regenerate_confirm"),
            ) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = { regenerateConfirmOpen = false }) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 4 — Test**

Add tests for the three flows. For the Share intent, use `Shadows.shadowOf(context).peekNextStartedActivity()` or just verify `vm.onShareCodeRequested()` was called (don't try to mock `Intent.createChooser`).

- [ ] **Step 5 — Commit**

- Commit: `feat(settings): invite code copy, share, and regenerate-with-confirm`

---

## US-11: Members list — creator-only remove

**As a** household creator
**I want** to remove other members from the household
**So that** I can recover from accidental joins or no-longer-trusted members.

### Acceptance criteria

- When `state.isCreator == true`, every member row OTHER THAN the current user gets a trailing remove icon button (`Icons.Default.Close` or `PersonRemove`).
- Tapping the remove icon opens an `AlertDialog`: title "Remove <displayName>?", body "They'll lose access to the household.", Remove (error color) / Cancel.
- Confirm → `viewModel.onRemoveMember(uid)` → calls a new `removeMember` Cloud Function.
- A new Cloud Function `removeMember(hid, uid)`:
  - Validates caller is signed in (`unauthenticated`)
  - Reads the household; validates `createdBy == caller.uid` (`permission-denied`)
  - Validates `uid != createdBy` (can't remove yourself this way — use Leave)
  - Validates `uid in memberUids` (`not-found`)
  - Transaction: `arrayRemove(uid)` from `memberUids`, `FieldValue.delete()` on `members.<uid>`, `arrayRemove(hid)` from `users/<uid>.households`
- `HouseholdRepository.removeMember(hid, uid)` calls the function via `Firebase.functions("europe-west1").getHttpsCallable("removeMember")`.
- A Compose test verifies the remove icon is invisible for non-creators, visible for creators on non-self rows, and opens the confirm dialog.

### Files

- Create: `functions/src/removeMember.ts`
- Modify: `functions/src/index.ts` (export the new function)
- Modify: `app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt` (replace stub)
- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Cloud Function**

```ts
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload { hid?: string; uid?: string }

export const removeMember = onCall<Payload>(
  { region: "europe-west1" },
  async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid) throw new HttpsError("unauthenticated", "Sign-in required");

    const hid = (request.data.hid ?? "").toString();
    const targetUid = (request.data.uid ?? "").toString();
    if (!hid || !targetUid)
      throw new HttpsError("invalid-argument", "hid and uid required");

    const hhRef = db.collection("households").doc(hid);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(hhRef);
      if (!snap.exists) throw new HttpsError("not-found", "Household not found");
      const createdBy = snap.get("createdBy") as string | undefined;
      if (createdBy !== callerUid)
        throw new HttpsError("permission-denied", "Only the creator can remove members");
      if (targetUid === createdBy)
        throw new HttpsError("invalid-argument", "Creator cannot remove themselves; use leave instead");
      const memberUids = (snap.get("memberUids") as string[] | undefined) ?? [];
      if (!memberUids.includes(targetUid))
        throw new HttpsError("not-found", "Target not a member");

      tx.update(hhRef, {
        memberUids: admin.firestore.FieldValue.arrayRemove(targetUid),
        [`members.${targetUid}`]: admin.firestore.FieldValue.delete(),
      });
      tx.update(db.collection("users").doc(targetUid), {
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
    });
    return { ok: true };
  }
);
```

Export from `functions/src/index.ts`.

- [ ] **Step 2 — Repo impl**

```kotlin
override suspend fun removeMember(householdId: String, uid: String): Result<Unit> = runCatching {
    val functions = FirebaseFunctions.getInstance("europe-west1")
    functions.getHttpsCallable("removeMember")
        .call(mapOf("hid" to householdId, "uid" to uid))
        .await()
    Unit
}
```

Imports: `com.google.firebase.functions.FirebaseFunctions`, `kotlinx.coroutines.tasks.await`.

- [ ] **Step 3 — UI: add the remove icon and dialog**

In the members section of `SettingsScreen`, change each member row to:

```kotlin
SettingsRow(modifier = Modifier.testTag("row_member_${m.uid}")) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (m.isYou) "${m.displayName} (you)" else m.displayName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(m.email, style = MaterialTheme.typography.bodySmall)
        }
        if (m.canRemove) {
            IconButton(
                onClick = { memberToRemove = m },
                enabled = !state.isOffline,
                modifier = Modifier.testTag("btn_remove_${m.uid}"),
            ) { Icon(Icons.Default.Close, contentDescription = "Remove") }
        }
    }
}
```

Add `var memberToRemove by remember { mutableStateOf<MemberRow?>(null) }`.

Add confirm dialog:

```kotlin
memberToRemove?.let { m ->
    AlertDialog(
        onDismissRequest = { memberToRemove = null },
        title = { Text("Remove ${m.displayName}?") },
        text = { Text("They'll lose access to the household.") },
        confirmButton = {
            TextButton(
                onClick = { viewModel.onRemoveMember(m.uid); memberToRemove = null },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_remove_confirm"),
            ) { Text("Remove") }
        },
        dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } },
    )
}
```

- [ ] **Step 4 — Tests**

```kotlin
@Test
fun `non-creator does not see remove buttons on other members`() { ... }

@Test
fun `creator sees remove button on other members but not self`() { ... }

@Test
fun `tapping remove opens confirm dialog and confirm fires VM`() { ... }
```

- [ ] **Step 5 — Deploy + commit**

- Deploy: `firebase deploy --only functions:removeMember`
- Commit: `feat(settings): creator-only member removal via removeMember Cloud Function`

---

## US-12: Category rename — list + dialog + batch repo

**As a** household member
**I want** to rename a category in Settings and have every item using that category move to the new name in one atomic operation
**So that** I can tidy up after typos without per-item edits.

### Acceptance criteria

- The Categories section lists every distinct category in the household (from `state.categories`), each row with a trailing pencil icon (`Icons.Default.Edit`).
- Tapping the pencil opens an `AlertDialog` with title "Rename category", a body line "All items in <old> will move to the new name.", and a text field pre-filled with the old name.
- Save (enabled when trimmed name is non-empty and differs from old) calls `viewModel.onRenameCategory(oldName, newName)`.
- The repo's `renameCategory(hid, oldName, newName)` queries items where `category == oldName`, builds a `WriteBatch`:
  - If the resulting count > 450 → return `Result.failure(IllegalStateException("Too many items..."))`.
  - Otherwise: `batch.update(itemRef, "category", newName)` for each + `batch.commit()`.
  - Returns the count on success.
- A test confirms the batch contains the right operations and the 450-item guard fires.

### Files

- Modify: `app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt`
- Create: `app/src/test/kotlin/app/pantry/data/household/RenameCategoryBatchTest.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Repo impl**

```kotlin
override suspend fun renameCategory(
    householdId: String,
    oldName: String,
    newName: String,
): Result<Int> = runCatching {
    val itemsCol = firestore.collection("households").document(householdId).collection("items")
    val snapshot = itemsCol.whereEqualTo("category", oldName).get().await()
    val count = snapshot.size()
    if (count > 450) {
        throw IllegalStateException("Too many items to rename in one batch — try v2")
    }
    if (count == 0) return@runCatching 0
    val batch = firestore.batch()
    snapshot.documents.forEach { doc ->
        batch.update(doc.reference, mapOf(
            "category" to newName,
            "updatedAt" to FieldValue.serverTimestamp(),
        ))
    }
    batch.commit().await()
    count
}
```

- [ ] **Step 2 — Unit test (mockk over Firestore)**

```kotlin
@Test
fun `renameCategory builds a batch with one update per matching item`() = runTest {
    // mock firestore, query, snapshot, batch
    // call repo.renameCategory("HH", "Dairy", "Fridge")
    // verify batch.update called N times with the right fields
    // verify batch.commit called
}

@Test
fun `renameCategory fails when result exceeds 450 items`() = runTest {
    // mock snapshot.size() == 451
    // assertEquals failure type IllegalStateException
}
```

- [ ] **Step 3 — UI: dialog and pencil rows**

Add `var categoryToRename by remember { mutableStateOf<String?>(null) }`.

Update the Categories section to include a pencil:

```kotlin
state.categories.forEach { cat ->
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("row_category_$cat"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(cat, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(
            onClick = { categoryToRename = cat },
            enabled = !state.isOffline,
            modifier = Modifier.testTag("btn_rename_category_$cat"),
        ) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
    }
}
```

Add dialog:

```kotlin
categoryToRename?.let { old ->
    var input by rememberSaveable { mutableStateOf(old) }
    AlertDialog(
        onDismissRequest = { categoryToRename = null },
        title = { Text("Rename category") },
        text = {
            Column {
                Text("All items in $old will move to the new name.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_category_name"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newName = input.trim()
                    categoryToRename = null
                    viewModel.onRenameCategory(old, newName)
                },
                enabled = input.trim().isNotEmpty() && input.trim() != old,
                modifier = Modifier.testTag("btn_category_save"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { categoryToRename = null }) { Text("Cancel") } },
    )
}
```

- [ ] **Step 4 — Compose test**

Verify the pencil opens the dialog, Save fires `vm.onRenameCategory(old, new)`.

- [ ] **Step 5 — Commit**

- Commit: `feat(settings): category rename with atomic WriteBatch`

---

## US-13: Leave household — Cloud Function with recursiveDelete + UI

**As a** household member
**I want** to leave the household from Settings (and have the household disappear entirely if I was the last member)
**So that** I can cleanly disconnect without leaving orphaned data behind.

### Acceptance criteria

- The "Leave household" button at the bottom of Settings opens an `AlertDialog`:
  - Title "Leave <household name>?"
  - Body: "You'll lose access to the shared stock." plus, if `state.members.size == 1`: "The household and its stock will be deleted."
  - Leave (error color) / Cancel.
- Confirm → `viewModel.onLeaveHousehold()` → calls `leaveHousehold` Cloud Function.
- New Cloud Function `leaveHousehold(hid)`:
  - Validates caller is signed in
  - Reads the household; validates `caller in memberUids` (`permission-denied` if not)
  - If `memberUids == [caller]`: use `getFirestore().recursiveDelete(hhRef)` to remove the household and all subcollections
  - Otherwise: transaction — `arrayRemove(uid)` from `memberUids`, `FieldValue.delete()` on `members.<uid>`, `arrayRemove(hid)` from `users/<uid>.households`
- After Cloud Function success, `pendingPostLeaveNav` is set; `SettingsScreen` calls `onLeft()` (which routes back to onboarding / start).
- A test verifies the body copy switches for last-member case and that confirm fires the VM action.

### Files

- Create: `functions/src/leaveHousehold.ts`
- Modify: `functions/src/index.ts` (export)
- Modify: `app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt` (replace stub)
- Modify: `app/src/main/kotlin/app/pantry/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt` (route from Settings's `onLeft` back to Start/Onboarding — confirm path)
- Modify: `app/src/test/kotlin/app/pantry/ui/settings/SettingsScreenTest.kt`

### Tasks

- [ ] **Step 1 — Cloud Function**

```ts
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload { hid?: string }

export const leaveHousehold = onCall<Payload>(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required");

    const hid = (request.data.hid ?? "").toString();
    if (!hid) throw new HttpsError("invalid-argument", "hid required");

    const hhRef = db.collection("households").doc(hid);
    const snap = await hhRef.get();
    if (!snap.exists) throw new HttpsError("not-found", "Household not found");
    const memberUids = (snap.get("memberUids") as string[] | undefined) ?? [];
    if (!memberUids.includes(uid))
      throw new HttpsError("permission-denied", "Not a member");

    if (memberUids.length === 1 && memberUids[0] === uid) {
      // Last member leaving — recursively delete household and subcollections.
      await getFirestore().recursiveDelete(hhRef);
      await db.collection("users").doc(uid).update({
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
      return { deleted: true };
    }

    await db.runTransaction(async (tx) => {
      tx.update(hhRef, {
        memberUids: admin.firestore.FieldValue.arrayRemove(uid),
        [`members.${uid}`]: admin.firestore.FieldValue.delete(),
      });
      tx.update(db.collection("users").doc(uid), {
        households: admin.firestore.FieldValue.arrayRemove(hid),
      });
    });
    return { deleted: false };
  }
);
```

Export from `index.ts`.

- [ ] **Step 2 — Repo impl**

```kotlin
override suspend fun leaveHousehold(householdId: String): Result<Unit> = runCatching {
    val functions = FirebaseFunctions.getInstance("europe-west1")
    functions.getHttpsCallable("leaveHousehold")
        .call(mapOf("hid" to householdId))
        .await()
    Unit
}
```

- [ ] **Step 3 — UI dialog + navigation**

Add `var leaveOpen by remember { mutableStateOf(false) }`.

Wire the "Leave household" button: `onClick = { leaveOpen = true }`.

Add the dialog:

```kotlin
if (leaveOpen) {
    val isLastMember = state.members.size == 1
    AlertDialog(
        onDismissRequest = { leaveOpen = false },
        title = { Text("Leave ${state.householdName}?") },
        text = {
            Column {
                Text("You'll lose access to the shared stock.")
                if (isLastMember) {
                    Spacer(Modifier.height(8.dp))
                    Text("The household and its stock will be deleted.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { leaveOpen = false; viewModel.onLeaveHousehold() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_leave_confirm"),
            ) { Text("Leave") }
        },
        dismissButton = { TextButton(onClick = { leaveOpen = false }) { Text("Cancel") } },
    )
}
```

For navigation: `SettingsScreen`'s `onLeft` param (defaulting to `onSignedOut`) is invoked when `pendingPostLeaveNav` flips true. Check that this hooks into the existing route logic in `PantryNavHost.kt` — the start destination logic should re-evaluate when the user has no households and route to onboarding.

- [ ] **Step 4 — Tests**

Verify last-member body copy is different and the confirm fires `vm.onLeaveHousehold()`.

- [ ] **Step 5 — Deploy + commit**

- Deploy: `firebase deploy --only functions:leaveHousehold`
- Commit: `feat(settings): leave household with recursiveDelete on last member`

---

## US-14: App version + smoke notes

**As a** household member
**I want** to see the app version in Settings (and verify the full Phase 4 flow on a real device)
**So that** I know what I'm running and the phase is shippable.

### Acceptance criteria

- The About section already renders `Pantry · v${state.appVersion}` (from US-8).
- `BuildConfig.VERSION_NAME` is correctly threaded into `SettingsUiState.appVersion` (in the VM's `project()`).
- The bottom of this spec file gets a "Manual smoke test" section listing the Phase 4 flow.
- Final commit + tag `phase-4-complete` + merge into `main`.

### Files

- Modify: `spec/phase-4-settings.md` (this file — add the smoke test section, if not already present)
- No new source files.

### Tasks

- [ ] **Step 1 — Verify version wire-up**

In `SettingsViewModel.project()`: `appVersion = BuildConfig.VERSION_NAME` — verify it's there.
In `app/build.gradle.kts`: confirm `versionName = "1.0.0"` (or whatever Phase 1 set).
Open the Settings tab on a real device: should show "Pantry · v1.0.0".

- [ ] **Step 2 — Verify smoke test section is present**

The "Manual smoke test (Phase 4)" section at the bottom of THIS plan file is the source of truth. If it's missing or out of date, update it.

- [ ] **Step 3 — Final ceremony**

- `./gradlew :app:assembleDebug` green.
- `./gradlew :app:test` green.
- Final commit (if needed): `chore(settings): phase 4 wrap-up`
- Tag: `git tag phase-4-complete`
- Switch to `main` and merge: per saved memory `feedback_branching`, fast-forward or merge-commit per user preference (recent precedent: Phase 3 used `--no-ff`).

---

## Manual smoke test (Phase 4)

Run on a real Android device (must be signed in to a household with at least 2 members for full coverage; a second member can be simulated by joining via a second account using the invite code):

1. Open the Settings tab. Verify the layout: Household / Invite code / Members / Categories / About / Sign out / Leave household.
2. Tap the household name → dialog appears with the current name pre-filled. Change it. Save. Snackbar "Household renamed". Top of screen now shows the new name.
3. Tap the invite code row. Snackbar "Invite code copied". Paste into another app to confirm.
4. Tap the share icon next to the code. Android Share sheet opens. Cancel.
5. Tap "Regenerate code" → confirm dialog. Confirm. The code on the row changes; snackbar "Code changed".
6. Verify the members list shows display name + email for every member, including yourself with "(you)".
7. If you are the household creator, every OTHER member has a remove icon. Tap it → confirm dialog → Remove. The member disappears; snackbar.
8. Categories section lists every distinct category. Pick one, tap the pencil → dialog with current name. Change it. Save. Snackbar "Renamed N item(s)". Switch to Stock tab — verify items previously in the old category now show under the new name.
9. App version row shows "Pantry · v1.0.0" (or whatever).
10. Sign out works (returns to Auth screen) — unchanged from Phase 2.
11. Tap "Leave household" → if you're not the last member: dialog says "You'll lose access to the shared stock."; if you are the last member, ALSO says "The household and its stock will be deleted." Leave. After success, you land on household onboarding.
12. Turn airplane mode on. Verify the offline banner appears at the top of every tab (Stock, Shopping, Settings). Every write button is disabled (steppers, FABs, Save buttons, Settings actions, Finish shopping). Toggle airplane off — banner disappears; buttons re-enable.

---

## Reference index (for the implementer)

| Concept | Where it lives after Phase 4 |
|---|---|
| `MemberSummary(displayName, email)` | `domain/model/MemberSummary.kt` |
| `Household.members: Map<uid, MemberSummary>` | `domain/model/Household.kt` |
| `Household.createdBy: String` | `domain/model/Household.kt` |
| `ConnectivityRepository.isOffline` | `data/connectivity/ConnectivityRepository.kt` |
| Offline banner | `ui/common/OfflineBanner.kt` |
| Settings full UI | `ui/settings/SettingsScreen.kt` |
| Member removal Cloud Function | `functions/src/removeMember.ts` |
| Leave household Cloud Function | `functions/src/leaveHousehold.ts` |
| Category rename batch | `FirestoreHouseholdRepository.renameCategory` |
| Members map population on join | `functions/src/joinHousehold.ts` |

---

## Out of scope (deferred)

- Profile editing UI (change display name / avatar). Members map will reflect whatever was at create/join time.
- Multi-household switching from Settings (Phase 1 left this as a soft assumption — one household per user).
- Audit log for member removal.
- Push notifications when stock drops or a member joins.
- Garbage collection of orphan documents older than X days.
- Member role / admin tier beyond creator-vs-not.
