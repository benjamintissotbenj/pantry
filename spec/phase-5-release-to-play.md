# Phase 5 — Release to Google Play — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Pantry to the Google Play Store production track with a staged 1–5% rollout against the existing `pantry-dev-1922e` Firebase project. The same effort opens the source on GitHub: a public repository hosting the privacy policy markdown (cited from Play), a README, and an MIT license. Code work covers release signing, R8 / ProGuard rules, Crashlytics, Firestore rules tightening (Phase 4 watch-item), and a version bump. Asset work covers a high-res icon (512×512), feature graphic (1024×500), draft store-listing copy, and a screenshot capture checklist for the user. The user owns the Play Console registration, the keystore generation, screenshot capture, AAB upload, data-safety / content-rating / target-audience forms, and the final submission.

**Architecture:** No new architectural primitives — this phase is release engineering, not feature work. The Gradle release build type already has `isMinifyEnabled = true` and `proguardFiles` configured (Phase 1 left the wiring in place); this phase fills in the actual ProGuard rules and replaces the debug-signing config with a real release signing config that reads from a gitignored `keystore.properties` at the repo root. Crashlytics is added as a Firebase BoM extension — one new plugin, one new dependency, and a one-line init in `PantryApp.onCreate()` gated by `BuildConfig.DEBUG`. The Firestore rules tightening uses Firestore's `diff(resource.data).affectedKeys().hasOnly([...])` predicate to restrict client-side household updates to a safe-fields allow-list (`name`, `inviteCode`); all sensitive mutations (memberUids, createdBy, members map) become admin-SDK-only via the existing `removeMember`, `leaveHousehold`, and `joinHousehold` Cloud Functions. Open-source prep adds standard top-level files (`README.md`, `LICENSE`, `PRIVACY.md`, `CONTRIBUTING.md`, expanded `.gitignore`) and a `play-store/` directory holding the generated assets (icon-512.png, feature-graphic.png, drafted-copy.md) — the screenshots themselves stay out of git (the user takes them from their device and uploads directly to Play Console).

**Tech stack additions:**
- `com.google.firebase:firebase-crashlytics-ktx` (via BoM 33.7.0 — already on classpath)
- `com.google.firebase.crashlytics` Gradle plugin
- Bumped `versionName` from `0.1.0` → `1.0.0`; `versionCode` stays `1` (first Play upload)
- No new Kotlin dependencies, no new Compose dependencies

**Spec source:** the brainstorm in this conversation. No higher-level design doc.
**Plan location:** `spec/phase-5-release-to-play.md`
**Project root:** `Repositories/pantry/`
**Package:** `app.pantry`
**Starts from:** `main` (tag `phase-4-complete` plus the Firebase-runtime bump + theme/brand commits up to `1f5c41c`).
**Branch:** all phase-5 work lives on a `phase-5-release-to-play` feature branch, merged into `main` after `phase-5-complete` is tagged. Per saved memory `feedback_branching`.

---

## Definition of Done (Phase 5)

- `./gradlew :app:bundleRelease` produces a signed AAB at `app/build/outputs/bundle/release/app-release.aab`, signed with the user's locally-generated upload key (Play App Signing model — Google re-signs for distribution).
- `app/build.gradle.kts` has a `signingConfigs { create("release") { ... } }` block that reads from `rootProject.file("keystore.properties")` (gitignored). Release build type uses this config (not the debug one). `keystore.properties` is gitignored; `keystore.properties.sample` is committed as a template.
- `app/proguard-rules.pro` contains R8-survival rules for our domain model classes (`Household`, `MemberSummary`, `StockItem`, `StockUnit`, `ShoppingEntry`, `UserProfile`), Hilt-generated factories (auto via consumer rules — verified, no extra rules needed), Firebase serialisation, kotlinx coroutines (consumer rules cover these), and Compose preview / tooling classes.
- `versionCode = 1`, `versionName = "1.0.0"` in `app/build.gradle.kts`.
- Firebase Crashlytics is integrated: plugin applied in `app/build.gradle.kts`, dependency added, R8 mapping file upload happens automatically on `:app:bundleRelease` and `:app:assembleRelease`. `PantryApp.onCreate()` calls `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)` so debug runs don't pollute the dashboard.
- `firestore.rules` restricts client-side `update` on `households/{hid}` to the safe fields (`name`, `inviteCode`) via `affectedKeys().hasOnly([...])`. Client-side `delete` is `if false` — only the `leaveHousehold` Cloud Function (admin SDK) can delete. Creation requires `createdBy == request.auth.uid` AND `memberUids == [request.auth.uid]` (already enforced; verified to still pass).
- `firebase deploy --only firestore:rules` succeeds against `pantry-dev-1922e`. A manual smoke test on the device confirms Settings rename-household and regenerate-code still work after the rules change.
- Open-source prep: `README.md`, `LICENSE` (MIT), `CONTRIBUTING.md`, `PRIVACY.md` at repo root. `.gitignore` covers `keystore.properties`, `*.jks`, `*.keystore`, `app/google-services.json`, `play-store/screenshots/`, and existing entries. A `google-services.json.sample` committed with placeholder values.
- A `secret-grep` pass over the entire repo (`git ls-files | xargs grep -l <patterns>`) surfaces zero hits for `apiKey`, `password`, `private_key`, `client_secret`, BEGIN PRIVATE KEY, etc.
- `play-store/` directory contains: `icon-512.png` (512×512 PNG, sRGB, no transparency, derived from `logo_pantry.xml`), `feature-graphic.png` (1024×500 PNG, sRGB, logo + word-mark + tagline), and `store-listing.md` (drafted app name, short description ≤80 chars, full description ≤4000 chars, category, content rating answers, data safety answers, target audience answers).
- A `play-store/screenshot-checklist.md` lists the 6 recommended phone-screen captures and the testTags / app states needed for each. The screenshots themselves are NOT committed.
- The repository has been pushed to a public GitHub remote with a passing `:app:bundleRelease` + the full test suite green.
- `phase-5-complete` tag on the last commit of the `phase-5-release-to-play` branch; branch is merged into `main`.

The Play Console submission itself (form completion, AAB upload, screenshot upload, review submission) is a manual user workflow guided by `play-store/play-console-checklist.md`. It does not gate the `phase-5-complete` tag — once the AAB builds, the rules deploy, the assets render, and the repo is public, the phase is technically complete; the rest is paperwork.

---

## User stories

| ID | Title | Type |
|---|---|---|
| US-1 | Version bump to 1.0.0 | infra |
| US-2 | Release signing config + keystore.properties machinery | infra |
| US-3 | ProGuard / R8 rules for our domain + libraries | infra |
| US-4 | Firestore rules tightening (Phase 4 watch-item) | infra |
| US-5 | Firebase Crashlytics integration | infra |
| US-6 | `.gitignore` audit + secret grep pass | infra |
| US-7 | README + LICENSE + CONTRIBUTING for open-source push | infra |
| US-8 | PRIVACY.md (markdown content) | infra |
| US-9 | Hi-res icon — `play-store/icon-512.png` | asset |
| US-10 | Feature graphic — `play-store/feature-graphic.png` | asset |
| US-11 | Screenshot capture checklist | docs |
| US-12 | Store listing copy (`store-listing.md`) | docs |
| US-13 | Play Console step-by-step checklist | docs |
| US-14 | Verify release AAB builds, tag + final ceremony | infra |

---

## US-1: Version bump to 1.0.0

**As a** developer about to publish the first Play release
**I want** `versionName = "1.0.0"` so the public Play listing carries a sensible semantic-version line
**So that** future minor / patch releases (1.0.1, 1.1.0) have an obvious bump path.

### Acceptance criteria

- `app/build.gradle.kts` shows `versionCode = 1` and `versionName = "1.0.0"`.
- `./gradlew :app:assembleDebug` still builds. The Settings "About" row shows "Pantry · v1.0.0" on the device.

### Files

- Modify: `app/build.gradle.kts`

### Tasks

- [ ] **Step 1 — Bump versionName**

Replace `versionName = "0.1.0"` with `versionName = "1.0.0"`. Leave `versionCode = 1` unchanged.

- [ ] **Step 2 — Verify + commit**

Run `./gradlew :app:assembleDebug` (green). Commit: `chore(release): bump versionName to 1.0.0 for first Play release`.

---

## US-2: Release signing config + keystore.properties machinery

**As a** developer about to publish a signed AAB
**I want** the Gradle release build to read signing credentials from a gitignored `keystore.properties` file
**So that** I can sign AABs locally without committing secrets and so the build fails loudly if the file is missing.

### Acceptance criteria

- `app/build.gradle.kts` defines a `signingConfigs { create("release") { ... } }` block that:
  - Reads `keystore.properties` from the project root.
  - If the file is missing, the block leaves `storeFile = null` (Gradle will then fail loudly during `:app:bundleRelease` — desired behavior; we never want a silent fallback to debug-key).
- `release` buildType uses `signingConfig = signingConfigs.getByName("release")` (not `getByName("debug")`).
- `keystore.properties.sample` committed at repo root with placeholder values + a header comment explaining the contract.
- `.gitignore` adds: `keystore.properties`, `*.jks`, `*.keystore`.
- User runs `keytool -genkeypair -alias pantry-upload -keyalg RSA -keysize 2048 -validity 9125 -keystore upload-keystore.jks` (commands documented in `keystore.properties.sample`) to generate the actual upload keystore. The keystore lives next to the project (gitignored) or wherever the user prefers; `keystore.properties` points at its path.

### Files

- Modify: `app/build.gradle.kts`
- Create: `keystore.properties.sample`
- Modify: `.gitignore`

### Tasks

- [ ] **Step 1 — Update `app/build.gradle.kts`**

Add a `java.util.Properties` import at the top:

```kotlin
import java.util.Properties
```

Add a `signingConfigs` block inside the `android { }` block, BEFORE `buildTypes`:

```kotlin
signingConfigs {
    create("release") {
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            val props = Properties().apply { keystorePropsFile.inputStream().use(::load) }
            storeFile = rootProject.file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
        // When the file is missing, storeFile stays null and any release-signed
        // task fails with "Keystore file null not found". That's the contract:
        // no silent fallback to the debug key.
    }
}
```

Then update the `release` build type to:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    signingConfig = signingConfigs.getByName("release")
}
```

(Adds `isShrinkResources = true` alongside the existing `isMinifyEnabled = true` — together these halve the AAB size by stripping unused resources and code.)

- [ ] **Step 2 — Create `keystore.properties.sample`**

`keystore.properties.sample` at repo root:

```properties
# Pantry release signing config — copy to `keystore.properties` and fill in.
# This file is committed; `keystore.properties` itself is gitignored.
#
# Generate the upload keystore once with:
#   keytool -genkeypair -alias pantry-upload -keyalg RSA -keysize 2048 \
#       -validity 9125 -keystore upload-keystore.jks
# (Use a strong passphrase. Store it somewhere safe — a password manager,
# not on a sticky note.)

storeFile=upload-keystore.jks
storePassword=your-keystore-password-here
keyAlias=pantry-upload
keyPassword=your-key-password-here
```

- [ ] **Step 3 — Update `.gitignore`**

Append to `.gitignore`:

```
# Release signing
keystore.properties
*.jks
*.keystore
upload-keystore.jks
```

- [ ] **Step 4 — Verify + commit**

- Run `./gradlew :app:assembleDebug` (still works — debug doesn't use the new signing config).
- DO NOT yet run `:app:bundleRelease` (no keystore exists; would fail). US-14 verifies that end-to-end after the user has generated their keystore.
- Commit: `feat(release): release signing config reading from keystore.properties`

---

## US-3: ProGuard / R8 rules

**As a** developer about to ship a minified release AAB
**I want** R8 to keep the Firebase / Compose / domain-model classes that reflection touches
**So that** the release build doesn't crash on first launch with a missing-class error.

### Acceptance criteria

- `app/proguard-rules.pro` contains explicit `-keep` rules for:
  - `app.pantry.domain.model.**` — Firestore serialisation reflects on field names.
  - `app.pantry.BuildConfig` — Crashlytics reads `VERSION_NAME` / `VERSION_CODE` via reflection.
  - Hilt-generated factories — covered by Hilt's consumer rules; no extra rule needed (verify).
  - Firebase — covered by firebase-bom consumer rules (verify).
- Optionally: `-dontwarn` for Compose preview annotations that R8 can't resolve in release.
- The release AAB builds clean: `./gradlew :app:bundleRelease` succeeds with no `R8: Missing class` warnings escalated to errors. Warnings are acceptable; errors block.
- Once the user generates their keystore, an installed release AAB launches and reaches the auth screen.

### Files

- Modify: `app/proguard-rules.pro`

### Tasks

- [ ] **Step 1 — Write the rules**

Replace contents of `app/proguard-rules.pro` with:

```proguard
# Pantry release R8 / ProGuard rules.
# Most rules are inherited from library consumer rules (Hilt, Firebase BoM,
# kotlinx-coroutines, Compose). The rules below cover what those don't.

# Domain model: Firestore (and the manual toMap()/toX() converters in the
# repos) rely on field names. R8 normally renames everything; keep the
# entire domain.model package as-is.
-keep class app.pantry.domain.model.** { *; }

# BuildConfig: Crashlytics' build-tool reflection reads versionName / versionCode
# off this class. Without this, the Crashlytics dashboard shows blank versions.
-keep class app.pantry.BuildConfig { *; }

# Compose preview & tooling classes that the release variant doesn't need but
# that R8 sees referenced from debug-only paths. Silence the warnings.
-dontwarn androidx.compose.ui.tooling.preview.**

# Coroutines uses ServiceLoader; the consumer rule covers this but we restate
# it defensively because broken coroutines is a particularly nasty crash mode.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# Firebase Functions: the callable contract relies on Map<String, *> shapes
# that R8 sees as Any. The consumer rule in firebase-functions handles this,
# but we add an explicit -dontwarn for the okhttp transitive dep that R8
# sometimes flags noisily.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
```

- [ ] **Step 2 — Partial verify (keystore-independent)**

Run `./gradlew :app:assembleRelease -x validateSigningRelease 2>&1 | grep -i "missing class\|error"` — this performs R8 against the release variant and only fails on actual minification errors, skipping the signing step. Any "Missing class" R8 error indicates a missing `-keep` rule; add it and retry.

US-14 runs the full `:app:bundleRelease` once the user's keystore is in place.

- [ ] **Step 3 — Commit**

Commit: `feat(release): R8 / ProGuard rules for domain model + Crashlytics + warnings`

---

## US-4: Firestore rules tightening (Phase 4 watch-item)

**As a** household owner
**I want** the Firestore rules to prevent any member from directly overwriting `memberUids`, `createdBy`, or `members` via the client SDK
**So that** member removal and leaving the household can only happen through the audited Cloud Functions (`removeMember`, `leaveHousehold`).

### Acceptance criteria

- `firestore.rules` updates the `households/{hid}` rule so:
  - `update` is allowed only if `request.resource.data.diff(resource.data).affectedKeys().hasOnly(['name', 'inviteCode'])` — restricts client writes to those two safe fields.
  - `delete` is `if false` — only `leaveHousehold` (admin SDK) can delete.
  - `create` additionally validates `request.resource.data.createdBy == request.auth.uid` (already implicit but now explicit).
- The `users/{uid}` and `households/{hid}/{subcollection}/{doc}` rules are unchanged.
- After deploy, the Settings rename-household flow still works (writes only `name`).
- After deploy, the Settings regenerate-invite-code flow still works (writes only `inviteCode`).
- A direct client write to `memberUids` from another user's perspective FAILS — verified by inspection (a malicious client would now get `PERMISSION_DENIED`).
- `firebase deploy --only firestore:rules` is run by the user after merge; not gated by the spec.

### Files

- Modify: `firestore.rules`

### Tasks

- [ ] **Step 1 — Update the rules**

Replace the `households/{hid}` block with:

```
match /households/{hid} {
  allow read: if request.auth != null
    && request.auth.uid in resource.data.memberUids;

  allow update: if request.auth != null
    && request.auth.uid in resource.data.memberUids
    && request.resource.data.diff(resource.data).affectedKeys()
        .hasOnly(['name', 'inviteCode']);

  allow delete: if false;

  allow create: if request.auth != null
    && request.resource.data.memberUids.size() == 1
    && request.resource.data.memberUids[0] == request.auth.uid
    && request.resource.data.createdBy == request.auth.uid;

  match /{subcollection}/{doc} {
    allow read, write: if request.auth != null
      && request.auth.uid in
        get(/databases/$(database)/documents/households/$(hid)).data.memberUids;
  }
}
```

- [ ] **Step 2 — Document the deploy step**

Add a `# To deploy: firebase deploy --only firestore:rules` comment at the top of the file as a reminder.

- [ ] **Step 3 — Commit**

Commit: `feat(security): restrict client household updates to name and inviteCode only`

**Post-merge deploy** (run by user after PR merged, BEFORE the AAB upload): `firebase deploy --only firestore:rules`. Verify on the device that Settings rename + regenerate still work; verify that the Stock / Shopping screens still read items + shopping entries correctly.

---

## US-5: Firebase Crashlytics integration

**As a** developer about to ship to real users
**I want** Crashlytics capturing crashes from release builds
**So that** I learn about device-specific issues without the user having to report them.

### Acceptance criteria

- `app/build.gradle.kts` applies the `com.google.firebase.crashlytics` Gradle plugin (alongside the existing `google-services`).
- `gradle/libs.versions.toml` adds:
  - `firebase-crashlytics-plugin` plugin reference (version `3.0.2` or current)
  - `firebase-crashlytics-ktx` library (no version — comes from BoM)
- The plugin is applied conditionally (same pattern as `google-services` — only if `google-services.json` exists, so a fresh clone without Firebase config still builds).
- `app/src/main/kotlin/app/pantry/PantryApp.kt` adds:
  ```kotlin
  FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
  ```
  in `onCreate()`, so debug builds never write to Crashlytics.
- Release builds automatically upload R8 mapping files to Crashlytics (the plugin handles this — no manual gradle task needed).
- No new tests required; this is a release-only feature.

### Files

- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/app/pantry/PantryApp.kt`

### Tasks

- [ ] **Step 1 — Add the plugin and dependency to `libs.versions.toml`**

In the `[versions]` block, add:
```toml
firebaseCrashlyticsPlugin = "3.0.2"
```

In the `[libraries]` block, add:
```toml
firebase-crashlytics-ktx = { module = "com.google.firebase:firebase-crashlytics-ktx" }
```

In the `[plugins]` block, add:
```toml
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsPlugin" }
```

- [ ] **Step 2 — Apply the plugin in `app/build.gradle.kts`**

Top of file (next to `google-services`):

```kotlin
alias(libs.plugins.firebase.crashlytics) apply false
```

Then below the existing `google-services` conditional apply:

```kotlin
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
```

In the `dependencies { }` block add:

```kotlin
implementation(libs.firebase.crashlytics.ktx)
```

- [ ] **Step 3 — Init in `PantryApp.kt`**

Find `PantryApp.onCreate()`. Add:

```kotlin
import app.pantry.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

// ... existing onCreate body ...

FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
```

If `BuildConfig` is already imported, skip the import line.

- [ ] **Step 4 — Verify + commit**

Run `./gradlew :app:assembleDebug` (green). The full unit-test suite should still pass — Crashlytics is initialised at runtime, not at compile time, so no test interaction.

Commit: `feat(observability): integrate Firebase Crashlytics with debug-build opt-out`

---

## US-6: `.gitignore` audit + secret grep pass

**As a** developer about to publish the repo open-source
**I want** to verify no secrets are tracked
**So that** the public push doesn't accidentally expose credentials.

### Acceptance criteria

- `.gitignore` covers (in addition to existing entries):
  - `keystore.properties`
  - `*.jks`
  - `*.keystore`
  - `upload-keystore.jks`
  - `app/google-services.json`
  - `play-store/screenshots/`
  - `local.properties` (already present, verify)
- `app/google-services.json.sample` is committed with the JSON structure but ALL secret-looking values (API key, OAuth client IDs, project numbers) replaced with `REPLACE_ME` strings — so a fresh clone has a template.
- A `git ls-files | xargs grep -i -l <secret-pattern>` pass across the working tree (NOT history — history rewrite is out of scope) surfaces zero matches for: `BEGIN PRIVATE KEY`, `BEGIN RSA PRIVATE KEY`, `AIza` (Google API keys often start with this), `client_secret`, `oauth2_client_id` patterns specific to credentials.

  Note: the project number and the `default_web_client_id` Android OAuth value in `app/google-services.json` are NOT secrets per Google's docs (they're embedded in the APK anyway), so the sample file may legitimately retain a placeholder pattern for them. The API key field starting with `AIza` IS sensitive (per Google's API key restrictions doc — if not domain-restricted, it can be used by anyone). Replace with `REPLACE_ME`.

### Files

- Modify: `.gitignore`
- Create: `app/google-services.json.sample` (with placeholder values)
- Do NOT remove `app/google-services.json` — it stays GITIGNORED but the current copy on disk is what the developer needs locally. The sample shows the shape.

### Tasks

- [ ] **Step 1 — Verify `.gitignore` coverage**

Read current `.gitignore` and ensure all entries listed in acceptance criteria are present. Add any that are missing.

If `app/google-services.json` is currently tracked (i.e., shows up in `git ls-files`), `git rm --cached app/google-services.json` and confirm the file remains on disk. Then add it to `.gitignore` and commit.

- [ ] **Step 2 — Generate the sample**

Read the actual `app/google-services.json` to understand its structure. Create `app/google-services.json.sample` with:
- The same JSON shape.
- Project name / number: kept as `REPLACE_ME_PROJECT` etc. (since the real values WILL leak when the developer commits their own google-services.json locally — but that's gitignored).
- API key (`current_key.api_key`): `REPLACE_ME_API_KEY` for any value starting with `AIza`.
- OAuth client ID (`oauth_client[].client_id`): `REPLACE_ME_OAUTH_CLIENT_ID`.
- Storage bucket, App ID: keep placeholders.

A separate README block in the sample comments on how to regenerate from the Firebase Console (project settings → "your apps" → download).

- [ ] **Step 3 — Secret grep pass**

Run (Bash):
```bash
git ls-files | xargs grep -l -i -E "BEGIN[ _-]?PRIVATE[ _-]?KEY|AIza[A-Za-z0-9_-]{30,}|client_secret" 2>/dev/null
```

Expected output: empty. If anything matches, investigate and either redact or add to `.gitignore` and `git rm --cached`.

- [ ] **Step 4 — Commit**

Commit: `chore(release): .gitignore audit + google-services.json.sample for open-source`

---

## US-7: README + LICENSE + CONTRIBUTING for open-source push

**As a** developer publishing the repo to GitHub
**I want** a professional-looking landing page that explains what Pantry is, how to build it, and how to contribute
**So that** the open-source push doesn't look abandoned and contributors know where to start.

### Acceptance criteria

- `README.md` at repo root contains, in order:
  - 1-paragraph intro: what Pantry is, who it's for
  - Screenshot or feature graphic (`![Pantry](play-store/feature-graphic.png)` after US-10)
  - **Features** section: bulleted list — Stock catalog, Shopping list with auto-derived low-stock entries + manual entries, household members, Firebase-backed real-time sync, dark/light Material 3 theme with Forest Green palette
  - **Tech stack** section: Kotlin 2.1, Jetpack Compose, Material 3, Hilt, Firebase (Auth + Firestore + Functions + Crashlytics), JUnit 5, Robolectric, MockK, Turbine
  - **Build & install** section: clone, `keystore.properties` setup (only if you want release builds), `google-services.json` setup, `./gradlew :app:installDebug`
  - **Phase plans** section: links to each `spec/phase-N-*.md` so curious readers can follow the development history
  - **Privacy policy** link to `PRIVACY.md`
  - **License** badge / link
- `LICENSE` at repo root: MIT license with the current year and user's name (we'll use `Benjamin Tissot` based on git config, or ask).
- `CONTRIBUTING.md` at repo root: ~50 lines covering branch naming (`feat/`, `fix/`, `chore/`), Conventional Commits format, "open an issue first for non-trivial changes", "all tests must pass: `./gradlew :app:testDebugUnitTest`".

### Files

- Create: `README.md`
- Create: `LICENSE`
- Create: `CONTRIBUTING.md`

### Tasks

- [ ] **Step 1 — Write README.md**

Draft with placeholders for the user's GitHub username (which we'll find from git config or ask). Include the project number and Firebase project name only at the level needed for documentation (e.g., "this project is named pantry-dev-1922e on Firebase Console" is fine; the API key value is not).

- [ ] **Step 2 — Write LICENSE**

Standard MIT license. Two placeholders: year and copyright holder name. Default: `2026 Benjamin Tissot` (based on git config; verify via `git config user.name`).

- [ ] **Step 3 — Write CONTRIBUTING.md**

Topics:
- Branching: `feat/...`, `fix/...`, `chore/...`, `docs/...`
- Commit style: Conventional Commits (scope is one of `stock`, `shopping`, `settings`, `auth`, `functions`, `theme`, `brand`, `release`, etc.)
- Testing: `./gradlew :app:testDebugUnitTest`, all green before PR
- Architecture: domain → data → ui layering, UDF state via StateFlow + Compose

- [ ] **Step 4 — Commit**

Commit: `docs: add README, LICENSE (MIT), CONTRIBUTING for open-source push`

---

## US-8: PRIVACY.md

**As a** Play submission
**I need** a privacy policy URL accessible to all users
**So that** I can complete the data safety form and pass Play review.

### Acceptance criteria

- `PRIVACY.md` at repo root, ~300 lines of plain-English markdown.
- Covers exactly what Pantry collects:
  - **Account**: email + display name via Firebase Auth (Google or email/password)
  - **Stock data**: items you create with name, category, quantity, threshold, optional default restock quantity, updatedAt timestamps — stored in your household's Firestore subcollection
  - **Shopping list**: free-text entries you add manually + optional links to catalog items — stored in your household's Firestore subcollection
  - **Members map**: display name + email of each household member, denormalised onto the household doc (populated by `joinHousehold` Cloud Function from each user's profile)
  - **Crash reports** via Firebase Crashlytics: device model, OS version, stack traces, app version, locale. Excludes PII.
- Explicitly states what we DON'T collect: location, contacts, photos, ads identifiers, microphone, camera, biometrics, financial data, health, payment info.
- Lists third parties: Firebase (Google) — auth, Firestore, Cloud Functions, Crashlytics. No analytics provider beyond Crashlytics. No advertising network.
- Data retention: "as long as your account exists in Firebase Auth; deleted when you leave all households AND delete your Firebase Auth account (via Settings → Sign out + leave). The household is recursively deleted when its last member leaves."
- User rights: leave the household (deletes their entry from the members map; the household and its stock are deleted recursively if they were the last member), sign out, delete account (Firebase Auth admin via password change / account closure).
- Contact: `your-email@example.com` placeholder — user fills in with their preferred contact email.
- Last updated: 2026-05-24 (today's date at spec time; bump on every PRIVACY.md edit).

### Files

- Create: `PRIVACY.md`

### Tasks

- [ ] **Step 1 — Write PRIVACY.md**

Use plain English, second person, no legalese boilerplate. Structure:
1. Who this policy is for
2. What we collect (one paragraph per category)
3. What we don't collect
4. Who we share with
5. Where data is stored
6. How long we keep it
7. What rights you have
8. Cookies / tracking (none)
9. Children (we don't knowingly collect from under 13)
10. Changes to this policy
11. Contact

Mark the GitHub Pages or raw GitHub URL as the canonical link Play will use: `https://github.com/<user>/<repo>/blob/main/PRIVACY.md`.

- [ ] **Step 2 — Commit**

Commit: `docs: add PRIVACY.md for Play submission`

---

## US-9: Hi-res icon — `play-store/icon-512.png`

**As a** Play submission
**I need** a 512×512 PNG icon (no transparency, sRGB)
**So that** I can fill the "high-res icon" slot on the store listing.

### Acceptance criteria

- `play-store/icon-512.png` exists at exactly 512×512 px, sRGB color space, no alpha channel.
- Visual content matches `app/src/main/res/drawable/logo_pantry.xml` — basket of three fruits on forest green background. No drop shadow, no extra padding (the launcher itself adds rounding; we provide the raw square).
- Output file is ≤ 1 MB.

### Files

- Create: `play-store/icon-512.png`

### Tasks

- [ ] **Step 1 — Render the vector to PNG**

Probe for tools (`magick --version`, `rsvg-convert --version`, `inkscape --version`); pick whichever exists. Approach options:

**A. ImageMagick** (likely on Windows via `magick.exe`):
```bash
magick -background "#2E7D32" -size 512x512 -density 600 logo_pantry.svg play-store/icon-512.png
```
(Convert `logo_pantry.xml` → `logo_pantry.svg` first — the `<path>` data is identical, just change root element and namespace.)

**B. rsvg-convert** (often on macOS via librsvg):
```bash
rsvg-convert -w 512 -h 512 logo_pantry.svg -o play-store/icon-512.png
```

**C. Pure-Kotlin fallback** — write `scripts/render-icon.kt` using `java.awt.Graphics2D` to paint the same shapes (basket trapezoid + 3 circles + leaf) on a `BufferedImage(512, 512, TYPE_INT_RGB)`, save via `ImageIO.write(img, "png", file)`. No external dependencies — JDK is always available.

**D. Manual fallback** — if nothing else works, write `play-store/README.md` documenting the required dimensions and color values, and ask the user to render externally (Figma, Photopea, or a vector renderer of their choice).

Pick the first option that works on the user's machine. The Kotlin fallback (C) is guaranteed to work since the JDK ships with `javax.imageio`.

- [ ] **Step 2 — Verify dimensions**

```bash
file play-store/icon-512.png  # should report 512 x 512
```

- [ ] **Step 3 — Commit**

Commit: `chore(release): hi-res app icon (512x512) for Play store listing`

---

## US-10: Feature graphic — `play-store/feature-graphic.png`

**As a** Play submission
**I need** a 1024×500 PNG feature graphic
**So that** I can fill the prominently-displayed banner slot on the store listing.

### Acceptance criteria

- `play-store/feature-graphic.png` exists at exactly 1024×500 px, sRGB, no alpha.
- Visual content: forest-green background, the basket-of-fruits logo on the left (vertically centred, ~300 px wide), "Pantry" word-mark in cream at ~96 pt centred-right, tagline "Your household's shared kitchen" in smaller text below.
- Output file is ≤ 1 MB.

### Files

- Create: `play-store/feature-graphic.png`

### Tasks

- [ ] **Step 1 — Compose the graphic**

Either:

**A. Draw it programmatically** — write a small Kotlin script using java.awt that:
1. Creates a 1024×500 image filled with `#2E7D32`
2. Loads `logo_pantry.png` (from US-9) and draws it scaled at the left
3. Draws "Pantry" text at a large font, cream color
4. Draws the tagline below it at smaller size
5. Saves as PNG

**B. Use SVG composer** (Inkscape / Figma / Sketch) — open a 1024×500 canvas, drop the logo + word-mark, export PNG.

**C. CLI approach with ImageMagick**:
```bash
magick -size 1024x500 xc:#2E7D32 \
    \( play-store/icon-512.png -resize 300x300 \) -geometry +100+100 -composite \
    -font DejaVu-Sans-Bold -pointsize 96 -fill "#FFF8E1" -gravity center \
    -draw "text 150,-40 'Pantry'" \
    -font DejaVu-Sans -pointsize 36 -fill "#E8DCB8" \
    -draw "text 150,50 'Your households shared kitchen'" \
    play-store/feature-graphic.png
```

(Apostrophe escaping varies by shell; verify the output text says "household's".)

If none of A/B/C work, document the dimensions in `play-store/README.md` and defer to the user.

- [ ] **Step 2 — Verify dimensions**

```bash
file play-store/feature-graphic.png
```

- [ ] **Step 3 — Commit**

Commit: `chore(release): Play store feature graphic (1024x500)`

---

## US-11: Screenshot capture checklist

**As a** Play submission
**I need** at least 2 (preferably 6) phone screenshots
**So that** users browsing the listing can see what the app does before installing.

### Acceptance criteria

- `play-store/screenshot-checklist.md` documents the 6 recommended captures:
  1. **Auth screen** — logo prominent, "Continue with Google" + email options visible
  2. **Stock list with items** — at least 3 items visible, one in low-stock (red ⚠ + red qty), one at qty 0 (greyed out + low-stock badge), one normal
  3. **Add item bottom sheet** — opened on the Stock tab, with the new item's Name + Quantity + Unit + Threshold + Default restock quantity fields visible
  4. **Shopping list** — Running low section with 2 entries, Added manually with 1 entry, the "Finish shopping (N)" bottom button enabled
  5. **Finish shopping confirm dialog** — opened on Shopping, showing the restock/clear/skip counts
  6. **Settings tab** — Household name, invite code with copy/share icons visible, Members list with 2 members, Categories list
- For each shot: the device state needed (signed-in user, household with X items in Y categories), the testTags or visual cues to look for, and any prep steps.
- Recommendation to capture in light mode (default Play preview is light).
- Recommendation to use a real phone screenshot (Power + Volume-Down) — Android Studio's screen capture works too.
- Mention that `play-store/screenshots/` is gitignored — the screenshots are uploaded directly to Play Console, not committed.

### Files

- Create: `play-store/screenshot-checklist.md`

### Tasks

- [ ] **Step 1 — Write the checklist**

Format as numbered list with prep + capture + filename suggestion (e.g., `01-auth.png`, `02-stock.png`, ...).

- [ ] **Step 2 — Commit**

Commit: `docs(release): screenshot capture checklist for Play store listing`

---

## US-12: Store listing copy

**As a** Play submission
**I need** drafted text for app name, short description, full description, category, and content rating answers
**So that** filling in the Play Console forms is mostly copy-paste.

### Acceptance criteria

- `play-store/store-listing.md` contains:
  - **App name** (max 30 chars): "Pantry" (verify availability on Play; suggest "Pantry — Shared Kitchen" as a fallback if "Pantry" is taken)
  - **Short description** (max 80 chars): drafted, mentions "household stock + shopping list, real-time sync"
  - **Full description** (max 4000 chars): ~400 words covering features, free, open source, Firebase-backed, privacy-first
  - **Category**: "Lifestyle" (more discoverable than "Food & Drink" for a stock-tracking app)
  - **Tags**: pantry, household, shopping list, stock, grocery, kitchen, share, food, real-time
  - **Content rating answers**: pre-drafted Q&A for each questionnaire item (no violence, no controversial content → rated Everyone / 3+)
  - **Data safety answers**: pre-drafted Q&A for each form item, declaring exactly what's in PRIVACY.md (email, name, in-app messages, crash logs → encrypted in transit, can request deletion via leaving the household)
  - **Target audience**: 13+ (Firebase Auth requires 13+ COPPA-compliance; no need to opt into designed-for-families program)
  - **Ads declaration**: "No, my app does not contain ads"
  - **App access**: "All functionality is available without special access" (no test credentials needed — Play reviewers will sign in via Google or create an email account)
  - **Government apps**: No

### Files

- Create: `play-store/store-listing.md`

### Tasks

- [ ] **Step 1 — Draft the copy**

Tone: friendly, concrete, no marketing fluff. Lead with the user benefit ("never run out of milk again"). The full description should mention "open source" as a differentiator — most household-stock apps are not.

- [ ] **Step 2 — Commit**

Commit: `docs(release): Play store listing copy (descriptions, content rating, data safety)`

---

## US-13: Play Console step-by-step checklist

**As a** developer about to navigate the Play Console for the first time
**I want** a step-by-step checklist that interleaves form-filling with file uploads
**So that** I can submit without missing steps and without context-switching to the Play docs.

### Acceptance criteria

- `play-store/play-console-checklist.md` is a numbered walkthrough covering:
  1. **Register Play Console account** ($25 one-time, accept developer agreement)
  2. **Create app**: name, language, free vs paid, app vs game, declarations
  3. **Set up release**: Production track → Create new release → Upload AAB from `app/build/outputs/bundle/release/app-release.aab`
  4. **Enroll in Play App Signing** (one-time, on AAB upload Play prompts)
  5. **App content section**: privacy policy URL, ads declaration, app access, content rating, target audience, government app, news app — fill each, copy answers from `store-listing.md`
  6. **Store listing**: app name, short description, full description, category, tags, hi-res icon, feature graphic, screenshots
  7. **Production release**: rollout percentage (recommend 1% initially, ramp to 100% over a week), release notes ("Initial public release.")
  8. **Submit for review**: green button at the bottom of the release dashboard
  9. **Post-submission**: monitor the email for any review issues; expect 3–10 days for first review
  10. **Approval / rejection**: if rejected, address the reason and resubmit; if approved, the app appears on the Play store within hours
- The checklist also notes "Firestore rules deploy step" (`firebase deploy --only firestore:rules`) BEFORE AAB upload, so production users aren't running against old rules.

### Files

- Create: `play-store/play-console-checklist.md`

### Tasks

- [ ] **Step 1 — Write the checklist**

Reference all `store-listing.md` answers inline (so the user doesn't have to flip back and forth).

- [ ] **Step 2 — Commit**

Commit: `docs(release): Play Console step-by-step submission checklist`

---

## US-14: Verify release AAB builds + tag + final ceremony

**As a** developer wrapping up Phase 5
**I want** to verify the entire pipeline works end-to-end on the user's machine with their actual keystore
**So that** the merge to main and the tag represent a known-working release.

### Acceptance criteria

- The user has generated their upload keystore and populated `keystore.properties`.
- `./gradlew :app:bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`.
- The AAB is signed (jarsigner -verify reports OK).
- The full test suite `./gradlew :app:testDebugUnitTest` is green.
- Tag `phase-5-complete` on the final commit of `phase-5-release-to-play`.
- Merge into `main`.

### Files

- No code files modified. This is verification + ceremony.

### Tasks

- [ ] **Step 1 — Build the release AAB**

```bash
./gradlew :app:bundleRelease
```

If this surfaces R8 errors, return to US-3 and add targeted `-keep` / `-dontwarn` rules.

- [ ] **Step 2 — Verify signing**

```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Expected: `jar verified.`

- [ ] **Step 3 — Confirm full test suite still green**

```bash
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 4 — Tag + merge**

```bash
git tag phase-5-complete
git checkout main
git merge --no-ff phase-5-release-to-play -m "merge: phase 5 — release to Google Play"
```

- [ ] **Step 5 — Push to public GitHub remote**

The user has created the public GitHub repo. Add it as `origin`:

```bash
git remote add origin git@github.com:<user>/<repo>.git
git push -u origin main
git push origin --tags
```

(Or `https://github.com/...` URL form if you're not using SSH.)

The user can now reference `https://github.com/<user>/<repo>/blob/main/PRIVACY.md` as the privacy policy URL in Play Console.

- [ ] **Step 6 — Hand off to user for Play submission**

Follow `play-store/play-console-checklist.md`. This is a 30–60 minute manual workflow.

---

## Manual smoke test (Phase 5)

Run after `firebase deploy --only firestore:rules` AND after installing the release AAB on the device:

1. Open the app. The launcher icon and splash show the green basket-of-fruits.
2. Sign in (existing user with a household). Auth screen shows the logo header.
3. Tap Stock tab → tap any row to open the Edit sheet → change `name` → Save. Should succeed (the Firestore rules tightening still allows `name` writes on the household doc... wait, `name` is on the item subdoc, not the household — verify the subcollection rule still allows writes). Should succeed.
4. Open Settings → tap household name → rename → Save. Should succeed (allow-list permits `name`).
5. Open Settings → tap Regenerate code → confirm. Should succeed (allow-list permits `inviteCode`).
6. Open Settings → if you are the creator and there's another member, tap Remove → confirm. Should succeed (admin SDK path via `removeMember` Cloud Function).
7. Open Settings → Leave household → confirm. If last member, the household is deleted. Should succeed (admin SDK path via `leaveHousehold`).
8. Force a crash (long-press an item that would throw — only as a deliberate test): the next launch should write the crash to Crashlytics dashboard.
9. App version in Settings About reads "Pantry · v1.0.0".

---

## Reference index

| Concept | Where it lives after Phase 5 |
|---|---|
| Release signing config | `app/build.gradle.kts` + `keystore.properties` (gitignored) |
| ProGuard rules | `app/proguard-rules.pro` |
| Firestore safe-field rules | `firestore.rules` |
| Crashlytics integration | `app/build.gradle.kts` + `PantryApp.kt` |
| Privacy policy | `PRIVACY.md` (at repo root, referenced from Play) |
| Store assets | `play-store/icon-512.png`, `feature-graphic.png` |
| Store copy + form answers | `play-store/store-listing.md` |
| Play submission walkthrough | `play-store/play-console-checklist.md` |
| README / LICENSE / CONTRIBUTING | repo root |

---

## Out of scope (deferred)

- `leaveHousehold` non-atomicity fix (Phase 4 watch-item — UI tolerates it).
- Separate prod Firebase project (stay on `pantry-dev-1922e` for v1.0.0).
- CI/CD pipeline (manual release for now; revisit if release cadence picks up).
- Localization (English-only v1).
- In-app updates via Play Core.
- Analytics beyond Crashlytics (no Firebase Analytics, no third-party tools).
- App-in-app browsing for the GitHub repo / changelog (future feature).
- Family Library / Education / Government program enrolment.
