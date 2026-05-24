# Pantry — Android Home Stock Management App

**Status:** Design approved, ready for implementation plan
**Date:** 2026-05-21
**Working name:** Pantry (placeholder, renameable)

## 1. Overview

A native Android app that lets a household share a real-time inventory of groceries and other consumables (toilet paper, soap, cleaning products…). Anyone in the household can fill up the stock when they shop, and anyone can deduct from it when they consume. The app surfaces a shopping list combining items that have dropped below a low-stock threshold and items added manually by household members.

## 2. v1 Scope

**In scope:**

- Per-user accounts with Google sign-in **and** email/password (Tricount-style identity)
- One household per user in v1 UI, but data layer is multi-household-ready
- Catalog items with: name, quantity, unit, category, low-stock threshold
- Hybrid catalog: items persist at qty 0 after consumption; new items can be added on the fly
- User-defined categories (free-text, chips derived from distinct values across current items)
- Shopping list combining auto-entries (qty < threshold) and manual entries
- Read-only offline mode (Firestore cache); writes require connectivity
- Material 3 Compose UI with bottom navigation

**Explicitly deferred to v1.x or later:**

- Receipt scanning (camera + OCR)
- Per-user attribution on stock changes (data model supports easy addition)
- Notifications (local WorkManager or FCM push)
- Multi-household UI (data model already supports it)
- Category management screen (rename, reorder, delete)
- Crashlytics + analytics

## 3. Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| State | ViewModel + StateFlow (UDF) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Backend | Firebase BoM: Auth (Google + email/password), Firestore (KTX) |
| Local cache | Firestore built-in offline persistence |
| Testing | JUnit, Coroutines test, Turbine, MockK, Compose UI test (Robolectric), Firebase Local Emulator Suite |
| Build | Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |
| Min SDK | 26 (Android 8.0) — covers ~98% of active devices |
| Target SDK | latest stable at implementation start |

## 4. Architecture

### Module layout

Single `:app` module, layered by internal package:

```
app/src/main/kotlin/com/<owner>/pantry/
├── data/
│   ├── auth/            (AuthRepository, FirebaseAuth wiring)
│   ├── household/       (HouseholdRepository, members/invite codes)
│   ├── stock/           (StockItemRepository, FieldValue.increment)
│   ├── shopping/        (ShoppingListRepository — manual entries only)
│   ├── connectivity/    (ConnectivityRepository — StateFlow<ConnectivityState>)
│   └── model/           (DTOs for Firestore + mappers to domain)
├── domain/
│   ├── model/           (StockItem, ShoppingEntry, Household, UserProfile, StockUnit)
│   └── usecase/         (pure Kotlin, no Android deps)
├── ui/
│   ├── auth/            (sign-in screen + ViewModel)
│   ├── household/       (create/join screen)
│   ├── stock/           (list screen + add/edit bottom sheet + ViewModel)
│   ├── shopping/        (list screen + add bottom sheet + ViewModel)
│   ├── settings/        (settings list + ViewModel)
│   ├── nav/             (root NavHost + bottom-bar wiring)
│   └── theme/           (Material 3 colour scheme, typography, shapes)
└── di/                  (Hilt modules)
```

### Patterns

- **UDF (Unidirectional Data Flow):** each screen has one immutable `UiState` data class exposed via `StateFlow<UiState>` from its `ViewModel`. User actions are functions on the ViewModel; state changes propagate back through the Flow.
- **Repository layer:** owns Firestore listeners; exposes Kotlin `Flow<List<Domain>>` via `callbackFlow`. All repository methods take `householdId` as a parameter (multi-household ready).
- **`currentHouseholdId`** is held by an `AppStateRepository` (StateFlow). In v1 the only value is "the user's single household"; later it becomes user-selectable.
- **Optimistic UI for stock changes:** stepper `−`/`+` updates Firestore via `FieldValue.increment(±1)`; UI updates immediately on tap, reverts on write failure with snackbar.

## 5. Data Model

### Firestore collections

```
users/{uid}
  displayName: string
  email: string
  households: [householdId]        ← list, single entry in v1
  createdAt: timestamp

households/{householdId}
  name: string
  memberUids: [uid]
  inviteCode: string               ← 6-char shareable code, regeneratable
  createdAt: timestamp
  createdBy: uid

households/{householdId}/items/{itemId}
  name: string
  category: string                 ← free-text, user-defined
  unit: string                     ← "count" | "g" | "kg" | "ml" | "L"
  quantity: number
  threshold: number                ← default 1
  updatedAt: timestamp

households/{householdId}/shoppingList/{entryId}    ← MANUAL entries only
  name: string                     ← free text ("Wine for guests")
  itemId: string | null            ← optional link to a catalog item
  createdAt: timestamp
  checked: boolean                 ← user marked it "got it"
```

**Important:** auto-generated shopping list entries are **not stored** — they are derived client-side by querying `items where quantity < threshold`. Only manual entries live in `shoppingList`. This avoids drift and write amplification.

### Kotlin domain model

```kotlin
data class StockItem(
    val id: String,
    val name: String,
    val category: String,
    val unit: StockUnit,
    val quantity: Double,
    val threshold: Double,
    val updatedAt: Instant,
)

enum class StockUnit { COUNT, GRAM, KILOGRAM, MILLILITER, LITER }

data class ShoppingEntry(
    val id: String,                   // synthetic for auto entries: "auto:${itemId}"
    val itemId: String?,              // null for ad-hoc manual entries
    val name: String,
    val source: Source,
    val checked: Boolean,             // always false for AUTO; persisted for MANUAL
    val createdAt: Instant,           // item.updatedAt for AUTO; doc.createdAt for MANUAL
) {
    enum class Source { AUTO, MANUAL }
}

data class Household(
    val id: String,
    val name: String,
    val memberUids: List<String>,
    val inviteCode: String,
)

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val householdIds: List<String>,
)
```

### Security rules

```
match /users/{uid} {
  allow read, write: if request.auth.uid == uid;
}
match /households/{hid} {
  allow read: if request.auth.uid in resource.data.memberUids;
  allow update, delete: if request.auth.uid in resource.data.memberUids;
  allow create: if request.auth != null
    && request.resource.data.memberUids.size() == 1
    && request.resource.data.memberUids[0] == request.auth.uid;

  match /{subcollection}/{doc} {
    allow read, write: if request.auth.uid in
      get(/databases/$(database)/documents/households/$(hid)).data.memberUids;
  }
}
```

Joining a household: a Cloud Function (or a separate `pendingJoins` collection with relaxed rules) handles "claim by invite code" so we don't expose unrestricted writes to `memberUids`. **Default for v1:** lightweight Cloud Function `joinHousehold(code)` that validates the code and adds the caller to `memberUids` atomically. This is the one piece of backend code we'll need.

The Cloud Function lives in a top-level `functions/` directory (TypeScript, Node 20 runtime), deployed via the Firebase CLI (`firebase deploy --only functions`). It's a callable function — the Android client invokes it with `Firebase.functions.getHttpsCallable("joinHousehold").call(mapOf("code" to code))`. The function emulator runs alongside Auth and Firestore in the Firebase Local Emulator Suite for integration tests.

## 6. UI Flows

### Navigation

Bottom navigation with 3 destinations:

1. **Stock** — main screen, default
2. **Shopping** — combined shopping list
3. **Settings**

Above bottom nav lives the `NavHost` for the active destination. Modal flows (auth, onboarding, item add/edit) sit above the NavHost as full-screens or bottom sheets.

### Stock screen

- Top app bar: title "Stock"
- Search field (Material 3 outlined search) below the app bar
- Horizontal scroll of category chips ("All" + every distinct category from current items, plus "+ New")
- Flat list of items: name + qty/unit + inline `−`/`+` stepper. Low-stock items have a `⚠` glyph and red qty.
- **Items at qty 0** stay in the list but are visually de-emphasised (50% opacity, italic) so the user can see "I have a slot for this; I'm just out of it right now". The `+` button still works to refill.
- FAB (bottom-right, above bottom nav): "+" → opens **Add item bottom sheet**
- Tap a row's name area → opens **Edit item bottom sheet** (same component, pre-filled). The edit sheet has a **Delete** secondary action in the top-right of the sheet header — confirms with a dialog ("Delete *Milk*? This removes it from the catalog.") before deleting.

### Shopping screen

- Top app bar: title "Shopping list"
- Section header "Running low" → auto entries (items where qty < threshold), with helper text showing `(current / threshold)` next to the name
- Section header "Added manually" → manual entries
- **Manual entries** — leading checkbox + name. Tapping the checkbox toggles `checked: boolean` on the Firestore doc (visual strike-through). Persistent across devices and app restarts. A "Clear checked" action in the app bar overflow menu deletes all checked manual entries in one go.
- **Auto entries** — leading checkbox + name + `(current / threshold)` qty hint. Tapping the checkbox does **not** persist anywhere (auto entries have no Firestore doc). Instead, tapping opens the **Edit item bottom sheet** for the corresponding catalog item, pre-focused on the quantity field — i.e. "I bought this, let me set the new stock level". Once the user saves with a quantity ≥ threshold, the auto entry naturally disappears from the list. This makes the shopping flow loop back into the stock-update flow with one extra tap.
- FAB: "+" → opens **Add manual entry bottom sheet** (name field + optional link to existing catalog item)

### Settings screen

- Household name (tappable to rename)
- Invite code (tap to copy / share)
- Members list (display name + email)
- Sign out
- Rename category (admin action that batch-updates items)
- App version

### Auth screen

Single welcome card with two buttons:

- "Continue with Google" — uses Android's Credential Manager API (modern, replaces deprecated GoogleSignIn)
- "Continue with email" — toggles inline form (email + password + "Forgot password?")

"Forgot password?" calls `FirebaseAuth.sendPasswordResetEmail()`; user receives a Firebase-hosted reset link and resumes sign-in.

### Household onboarding (first launch after auth)

Two-button screen:

- "Create a household" → text field for name → creates household, current user becomes sole member
- "Join with invite code" → 6-char text field → calls `joinHousehold` Cloud Function

### Add/edit item bottom sheet (Stock)

Fields:

- Name (text)
- Quantity (numeric) + Unit (dropdown: count / g / kg / ml / L)
- Low-stock threshold (numeric, default 1, helper text explains the shopping-list link)
- Category — horizontal chips with currently-used categories + "+ New" chip that reveals a text input

Save commits to Firestore and dismisses the sheet.

### Add manual entry bottom sheet (Shopping)

Fields:

- Name (text, autocomplete from catalog item names)
- Optional: "link to catalog item" toggle that ties the entry to an existing `itemId`

## 7. Error Handling & Offline Behavior

### Connectivity

A `ConnectivityRepository` exposes `StateFlow<ConnectivityState>` (Online / Offline) using `ConnectivityManager.NetworkCallback`. ViewModels observe and gate the UI: when offline, all write affordances (steppers, FABs, Save) are disabled; a persistent slim banner at the top of every screen reads "Offline — viewing cached data".

### Error surfaces

| Category | Example | Surface |
|---|---|---|
| Validation | empty name, negative qty | Inline `supportingText`; Save disabled |
| Auth | wrong password, email in use | Snackbar with Firebase's message |
| Permission | not a household member | Full-screen error + Sign Out |
| Network | timeout during write | Snackbar with "Retry" action |
| Unknown | uncaught throwable | Snackbar "Something went wrong"; log via `Timber` in debug |

### Concurrency

Two members editing the same item: **last-write-wins at the field level** via Firestore transactions. The stepper uses `FieldValue.increment(±1)`, so simultaneous taps don't lose updates. Non-quantity fields use a transaction that re-reads before writing; on conflict, snackbar "Item changed by another household member" + bottom sheet re-opens with latest data.

### Loading states

- Initial app load: splash → auth check → either auth or household screen
- Per-screen first load: 3–5 grey shimmer rows for the first 300ms; keep skeleton until data arrives
- Background refresh: silent (Firestore listeners push updates)
- Write in flight: optimistic UI; revert + snackbar on failure

## 8. Testing Strategy

### Pyramid

- **Unit (many)** — use cases, mappers, validators (pure Kotlin)
- **Integration (some)** — repositories against Firebase Emulator; ViewModels against fake repositories
- **E2E (few)** — Compose + Espresso against the emulator, 2–3 golden paths

### Tooling

| Layer | Stack |
|---|---|
| Unit | JUnit 5 + Coroutines test + Turbine |
| Mocks | MockK (prefer hand-rolled fakes for repos) |
| Compose UI | `androidx.compose.ui.test.junit4` on Robolectric (fast, no emulator) |
| Repository integration | Firebase Local Emulator Suite (Auth + Firestore) |
| Security rules | `@firebase/rules-unit-testing` against the Firestore emulator |
| E2E | Compose + Espresso against the emulator on an Android emulator |

### CI (GitHub Actions)

On push/PR:

1. `./gradlew lint detekt`
2. `./gradlew test` (unit + Compose UI via Robolectric)
3. Firebase emulator + `./gradlew connectedDebugAndroidTest` (PR-optional, nightly-required)
4. Security rules tests

### Coverage policy

No coverage threshold gate in v1. Contributor checklist in `CONTRIBUTING.md`: every PR must add tests for new domain logic and at least one Compose UI test per new screen.

## 9. Open Questions to Resolve in Planning

These don't change the design but need a one-line decision during implementation planning:

- **Owner package** (`com.<owner>.pantry`) — TBD with final repo name
- **Final app name** (Pantry is a placeholder)
- **CI on every PR vs nightly** for the Android emulator step
- **Logging** — `Timber` in debug only; release builds strip logs entirely

## 10. Out-of-Scope Reminders

These were discussed and deliberately deferred:

- Receipt scanning (camera, OCR, line-item parsing)
- Per-user attribution on stock events
- Notifications (local or push)
- Multi-household switcher in UI
- Category rename / management screen (beyond the settings stub)
- Crashlytics + analytics
- iOS / Compose Multiplatform
- Custom auth email domain
