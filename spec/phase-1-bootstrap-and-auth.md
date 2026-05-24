# Phase 1 — Bootstrap & Auth — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Pantry build where a user signs in (Google or email/password), creates or joins a household, and lands on a placeholder home screen. After Phase 1, the foundations for Phase 2 (Stock catalog) are fully in place.

**Architecture:** Single `:app` Gradle module with internal layered packaging. UDF + StateFlow + Hilt + Coroutines/Flow. Firebase Auth (Google via Credential Manager + email/password) + Firestore for household membership. One callable Cloud Function (`joinHousehold`) deployed via Firebase CLI.

**Tech stack:** Kotlin 2.1.0 · AGP 8.7.0 · Compose BoM 2025.01.00 · Material 3 · Hilt 2.53 · Firebase BoM 33.7.0 · Credential Manager 1.3.0 · Robolectric 4.13 · JUnit 5.11 · Turbine 1.2.0 · MockK 1.13.13 · Firebase Local Emulator Suite · Node 20 Cloud Functions (TypeScript).

**Spec source:** `spec/android-app-design.md`
**Plan location:** `pantry/spec/phase-1-bootstrap-and-auth.md`
**Project root:** `Repositories/pantry/`
**Package:** `app.pantry`

---

## Definition of Done (Phase 1)

- `./gradlew :app:assembleDebug` produces an APK installable on a real device
- A new user can launch the app, sign in with Google or create an email/password account, and either:
  - create a new household (auto-becomes sole member), **or**
  - join an existing household via a 6-character invite code
- After auth + household, the user lands on a placeholder `HomeScreen` saying "Coming next: stock catalog"
- Sign out returns to the welcome screen
- Unit tests, Robolectric Compose tests, and Firebase Emulator integration tests all pass: `./gradlew test`
- `joinHousehold` Cloud Function deploys and is callable from the emulator
- All commits follow Conventional Commits (`feat:`, `chore:`, `test:`, `docs:` …)

---

## User stories

| ID | Title | Type |
|---|---|---|
| US-1 | Project scaffolding | infra |
| US-2 | Firebase project & local emulator | infra |
| US-3 | Testing infrastructure | infra |
| US-4 | Material 3 theme & app shell | infra |
| US-5 | Auth domain layer | infra |
| US-6 | Sign up / sign in with email + password | user-facing |
| US-7 | Forgot password | user-facing |
| US-8 | Sign in with Google | user-facing |
| US-9 | Household domain layer | infra |
| US-10 | Create household | user-facing |
| US-11 | Join household with invite code | user-facing |
| US-12 | Cloud Function `joinHousehold` | backend |
| US-13 | Sign out | user-facing |
| US-14 | First-launch routing & placeholder home | infra |

---

## US-1: Project scaffolding

**As a** developer
**I want** an empty Android project with the canonical modern stack wired up
**So that** I can start building features without re-deciding plumbing.

### Acceptance criteria

- Project lives at `Repositories/pantry/` and coexists with the pre-existing `spec/` folder
- `./gradlew :app:assembleDebug` succeeds and produces an APK
- Version catalog at `gradle/libs.versions.toml` is the single source of truth for versions
- Detekt and ktlint enforced on `./gradlew check`
- Empty `MainActivity` launches and shows "Pantry — bootstrap OK" in Compose
- Project has a top-level `README.md`, `.gitignore`, and `.editorconfig`
- A first git commit `chore: initial project scaffold` is created on a fresh `main` branch

### Files

- Create: `pantry/.gitignore`
- Create: `pantry/.editorconfig`
- Create: `pantry/README.md`
- Create: `pantry/settings.gradle.kts`
- Create: `pantry/build.gradle.kts`
- Create: `pantry/gradle/libs.versions.toml`
- Create: `pantry/gradle.properties`
- Create: `pantry/app/build.gradle.kts`
- Create: `pantry/app/proguard-rules.pro`
- Create: `pantry/app/src/main/AndroidManifest.xml`
- Create: `pantry/app/src/main/kotlin/app/pantry/PantryApp.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`
- Create: `pantry/config/detekt/detekt.yml`

### Tasks

- [ ] **Step 1 — Initialise git and create `.gitignore`**

Create `pantry/.gitignore`:

```
.gradle/
build/
local.properties
.idea/
*.iml
captures/
.cxx/
.kotlin/
google-services.json
GoogleService-Info.plist
*.jks
*.keystore
.DS_Store
```

Run from `pantry/`:

```bash
git init
git checkout -b main
git add .gitignore
git commit -m "chore: initial gitignore"
```

- [ ] **Step 2 — Add `.editorconfig`**

Create `pantry/.editorconfig`:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{yml,yaml,toml,json}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

- [ ] **Step 3 — Create version catalog**

Create `pantry/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.0"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
hilt = "2.53"
composeBom = "2025.01.00"
firebaseBom = "33.7.0"
credentialManager = "1.3.0"
googleId = "1.1.1"
coroutines = "1.9.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
navigationCompose = "2.8.5"
material3 = "1.3.1"
junitJupiter = "5.11.3"
turbine = "1.2.0"
mockk = "1.13.13"
robolectric = "4.13"
detekt = "1.23.7"
ktlint = "12.1.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx" }
firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx" }
firebase-functions-ktx = { module = "com.google.firebase:firebase-functions-ktx" }
credentials = { module = "androidx.credentials:credentials", version.ref = "credentialManager" }
credentials-play-services-auth = { module = "androidx.credentials:credentials-play-services-auth", version.ref = "credentialManager" }
googleid = { module = "com.google.android.libraries.identity.googleid:googleid", version.ref = "googleId" }
junit-jupiter-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junitJupiter" }
junit-jupiter-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junitJupiter" }
junit-jupiter-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junitJupiter" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version = "1.2.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

- [ ] **Step 4 — Create root Gradle files**

Create `pantry/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Pantry"
include(":app")
```

Create `pantry/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = true
}
```

Create `pantry/gradle.properties`:

```
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
ksp.useKSP2=true
```

- [ ] **Step 5 — Create `:app` module Gradle file**

Create `pantry/app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "app.pantry"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.pantry"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // replace before Play release
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.useJUnitPlatform() }
    }

    sourceSets {
        getByName("main").kotlin.srcDir("src/main/kotlin")
        getByName("test").kotlin.srcDir("src/test/kotlin")
        getByName("androidTest").kotlin.srcDir("src/androidTest/kotlin")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

Create empty `pantry/app/proguard-rules.pro`:

```
# Keep Firestore data classes
-keepclassmembers class app.pantry.data.** {
  <init>(...);
}
```

- [ ] **Step 6 — Create `AndroidManifest.xml`**

Create `pantry/app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".PantryApp"
        android:label="Pantry"
        android:icon="@android:drawable/sym_def_app_icon"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7 — Create `PantryApp` and `MainActivity`**

Create `pantry/app/src/main/kotlin/app/pantry/PantryApp.kt`:

```kotlin
package app.pantry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PantryApp : Application()
```

Create `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`:

```kotlin
package app.pantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BootstrapScreen()
                }
            }
        }
    }
}

@Composable
private fun BootstrapScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Pantry — bootstrap OK")
    }
}
```

- [ ] **Step 8 — Create Detekt config and README**

Create `pantry/config/detekt/detekt.yml`:

```yaml
build:
  maxIssues: 0

complexity:
  LongMethod:
    threshold: 80
  LongParameterList:
    functionThreshold: 8
    constructorThreshold: 10
  TooManyFunctions:
    thresholdInClasses: 20

style:
  MagicNumber:
    active: false
  ReturnCount:
    max: 3

naming:
  FunctionNaming:
    functionPattern: '[a-zA-Z][a-zA-Z0-9_]*'  # allow Composables to start with uppercase
```

Create `pantry/README.md`:

````markdown
# Pantry

Native Android home stock management app. Kotlin + Compose + Material 3 + Firebase.

## Project layout

```
pantry/
├── app/                # Android :app module
├── functions/          # Firebase Cloud Functions (TypeScript)
├── spec/               # User-story implementation plans
└── gradle/             # Version catalog
```

## Setup

1. Install JDK 17, Android Studio Ladybug+, Node 20, Firebase CLI
2. `firebase login`
3. `./gradlew :app:assembleDebug` — sanity check the build
4. `firebase emulators:start` — run Auth + Firestore + Functions locally
5. `./gradlew test` — run unit + Robolectric tests

## Spec

See `spec/` for phase plans. Active phase: **Phase 1 — Bootstrap & Auth**.
````

- [ ] **Step 9 — Generate the Gradle wrapper**

Run from `pantry/`:

```bash
gradle wrapper --gradle-version=8.10.2 --distribution-type=bin
```

If `gradle` is not on PATH, use Android Studio's bundled Gradle: open the project → File → Project Structure → Gradle, then re-export the wrapper.

- [ ] **Step 10 — Verify the build**

Run from `pantry/`:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. An APK appears under `app/build/outputs/apk/debug/`.

- [ ] **Step 11 — Commit**

```bash
git add .
git commit -m "chore: scaffold Android project with Compose, Hilt, Firebase"
```

---

## US-2: Firebase project & local emulator

**As a** developer
**I want** a Firebase project configured for the app and a local emulator suite
**So that** I can develop offline against realistic Auth, Firestore, and Functions without burning quota or polluting prod data.

### Acceptance criteria

- A Firebase project (name: `pantry-dev`) exists; `google-services.json` is downloaded and placed in `pantry/app/`
- Email/password and Google sign-in are enabled in the Firebase console
- Firestore is provisioned in *test mode* initially (rules locked down in US-9)
- `firebase init` is run in `pantry/`, generating `firebase.json`, `firestore.rules`, `firestore.indexes.json`, and a `functions/` directory (TypeScript)
- Running `firebase emulators:start --only auth,firestore,functions` boots all three on localhost
- The app, when run in debug, connects to the emulators instead of production (via a debug-only initializer)

### Files

- Create: `pantry/firebase.json`
- Create: `pantry/.firebaserc`
- Create: `pantry/firestore.rules`
- Create: `pantry/firestore.indexes.json`
- Create: `pantry/functions/package.json`
- Create: `pantry/functions/tsconfig.json`
- Create: `pantry/functions/src/index.ts`
- Create: `pantry/app/google-services.json` (placeholder + real)
- Create: `pantry/app/src/main/kotlin/app/pantry/data/firebase/FirebaseInitializer.kt`
- Create: `pantry/app/src/debug/kotlin/app/pantry/data/firebase/EmulatorInitializer.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/PantryApp.kt`

### Tasks

- [ ] **Step 1 — Create the Firebase project**

In a browser, go to https://console.firebase.google.com → Add project → name `pantry-dev` → enable Google Analytics: **no**.

In the project:
1. Authentication → Sign-in method → enable "Email/Password" and "Google" (use the project support email).
2. Firestore Database → Create database → Start in **test mode** (we lock it down in US-9) → choose region (e.g. `europe-west1`).
3. Project settings (gear) → **Add app** → Android → package `app.pantry` (and a second app entry for `app.pantry.debug` since debug builds add a suffix) → SHA-1: run `./gradlew signingReport` and paste the debug SHA-1 for both → download `google-services.json` → save to `pantry/app/google-services.json`.

- [ ] **Step 2 — Install Firebase CLI and init project**

```bash
npm install -g firebase-tools
firebase login
cd pantry
firebase init
```

Choose: Firestore + Functions + Emulators.
- Use existing project: `pantry-dev`
- Firestore rules file: `firestore.rules` (accept default)
- Firestore indexes file: `firestore.indexes.json`
- Functions language: **TypeScript**
- ESLint: **yes**
- Install dependencies: **yes**
- Emulators: Auth, Firestore, Functions (accept default ports)
- Download emulators: **yes**

- [ ] **Step 3 — Verify `firebase.json` includes Auth emulator**

Edit `pantry/firebase.json` so the `emulators` block has all three services:

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "functions": [
    {
      "source": "functions",
      "codebase": "default",
      "ignore": ["node_modules", ".git", "firebase-debug.log", "firebase-debug.*.log"],
      "predeploy": ["npm --prefix \"$RESOURCE_DIR\" run lint", "npm --prefix \"$RESOURCE_DIR\" run build"]
    }
  ],
  "emulators": {
    "auth": { "port": 9099 },
    "firestore": { "port": 8080 },
    "functions": { "port": 5001 },
    "ui": { "enabled": true, "port": 4000 },
    "singleProjectMode": true
  }
}
```

- [ ] **Step 4 — Add the debug-only emulator initializer**

Create `pantry/app/src/debug/kotlin/app/pantry/data/firebase/EmulatorInitializer.kt`:

```kotlin
package app.pantry.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

object EmulatorInitializer {
    private const val EMULATOR_HOST = "10.0.2.2" // Android emulator -> host loopback
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080
    private const val FUNCTIONS_PORT = 5001

    fun initialise() {
        FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_PORT)
        FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        FirebaseFunctions.getInstance().useEmulator(EMULATOR_HOST, FUNCTIONS_PORT)
    }
}
```

Create `pantry/app/src/main/kotlin/app/pantry/data/firebase/FirebaseInitializer.kt` (no-op shim used by release builds):

```kotlin
package app.pantry.data.firebase

object FirebaseInitializer {
    /** Wire-in point for build-variant-specific setup. Release builds do nothing extra. */
    fun maybeInitEmulators() {
        // overridden in src/debug by EmulatorInitializer
    }
}
```

Create `pantry/app/src/debug/kotlin/app/pantry/data/firebase/FirebaseInitializer.kt` (debug override):

```kotlin
package app.pantry.data.firebase

object FirebaseInitializer {
    fun maybeInitEmulators() = EmulatorInitializer.initialise()
}
```

> Note: Kotlin lets us replace the `main` source set's class entirely from `debug` because Android's variant-aware compilation drops the `main` version when `debug` provides one with the same FQN. If your IDE complains, switch the `debug` version to a separate object and call it conditionally from `PantryApp.onCreate()` guarded by `BuildConfig.DEBUG`.

Update `pantry/app/src/main/kotlin/app/pantry/PantryApp.kt`:

```kotlin
package app.pantry

import android.app.Application
import app.pantry.data.firebase.FirebaseInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PantryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.maybeInitEmulators()
    }
}
```

- [ ] **Step 5 — Boot the emulator and verify**

```bash
cd pantry
firebase emulators:start --only auth,firestore,functions
```

Expected: emulator UI available at `http://localhost:4000` showing Auth, Firestore, Functions tabs.

Build and run the app in an Android emulator. In `adb logcat | grep -i firebase`, expect no "production endpoint" warnings — Firestore should log emulator connection. Check the Auth tab of the emulator UI; after US-6 we'll see new users land there.

- [ ] **Step 6 — Commit**

```bash
git add .
git commit -m "chore: wire Firebase project and local emulator suite"
```

---

## US-3: Testing infrastructure

**As a** developer
**I want** unit, Robolectric Compose, and Firebase emulator integration tests runnable from Gradle
**So that** every later user story can be implemented test-first.

### Acceptance criteria

- `./gradlew :app:test` runs and includes JUnit 5, Turbine, MockK, and Robolectric
- A first failing test scaffold (`SmokeTest.kt`) exists and passes
- A first Compose Robolectric test (`BootstrapScreenTest.kt`) renders the placeholder text
- A first Firebase Emulator integration test (`EmulatorSmokeTest.kt`) writes and reads a doc against the local emulator
- A README section documents the test commands

### Files

- Create: `pantry/app/src/test/kotlin/app/pantry/SmokeTest.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/BootstrapScreenTest.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/data/firebase/EmulatorSmokeTest.kt`
- Modify: `pantry/README.md`

### Tasks

- [ ] **Step 1 — Write the first JUnit 5 test**

Create `pantry/app/src/test/kotlin/app/pantry/SmokeTest.kt`:

```kotlin
package app.pantry

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun `JUnit 5 platform is available`() {
        assertTrue(true, "JUnit 5 should be on the classpath")
    }
}
```

- [ ] **Step 2 — Run it and verify it passes**

```bash
./gradlew :app:test --tests "app.pantry.SmokeTest"
```

Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 3 — Add the Compose Robolectric smoke test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/BootstrapScreenTest.kt`:

```kotlin
package app.pantry.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BootstrapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shows_placeholder_text() {
        composeRule.setContent { MaterialTheme { app.pantry.BootstrapScreenForTest() } }
        composeRule.onNodeWithText("Pantry — bootstrap OK").assertIsDisplayed()
    }
}
```

Make `BootstrapScreen` accessible to tests — in `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`, rename `BootstrapScreen` to `internal fun BootstrapScreenForTest()` and call it from `MainActivity`:

```kotlin
@Composable
internal fun BootstrapScreenForTest() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Pantry — bootstrap OK")
    }
}
```

Then update `MainActivity.onCreate` to call `BootstrapScreenForTest()`.

> Note: this Compose test uses the JUnit 4 runner because Robolectric + Compose UI test infrastructure is JUnit 4. JUnit 5 is fine for plain Kotlin tests; Compose UI tests stay JUnit 4. Both run via `./gradlew test`.

In `pantry/app/build.gradle.kts`, add JUnit 4 to test deps:

```kotlin
testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 4 — Run the Compose test**

```bash
./gradlew :app:test --tests "app.pantry.ui.BootstrapScreenTest"
```

Expected: PASS.

- [ ] **Step 5 — Add the Firebase Emulator integration smoke test**

Create `pantry/app/src/test/kotlin/app/pantry/data/firebase/EmulatorSmokeTest.kt`:

```kotlin
package app.pantry.data.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmulatorSmokeTest {

    @BeforeAll
    fun setUp() {
        if (FirebaseApp.getApps(androidx.test.core.app.ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setApplicationId("pantry-dev")
                    .setProjectId("pantry-dev")
                    .setApiKey("fake-api-key")
                    .build(),
            )
        }
        FirebaseFirestore.getInstance().useEmulator("localhost", 8080)
    }

    @Test
    fun `write and read a doc against firestore emulator`() = runTest {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("smoke").document("ping")
        docRef.set(mapOf("ok" to true)).await()
        val snap = docRef.get().await()
        assertEquals(true, snap.getBoolean("ok"))
    }
}
```

This test requires the emulator to be running. We tag it so unit-only CI runs skip it.

- [ ] **Step 6 — Tag the emulator test and run it**

Annotate the class with `@org.junit.jupiter.api.Tag("emulator")`. In `pantry/app/build.gradle.kts`, add:

```kotlin
android {
    testOptions {
        unitTests.all { test ->
            test.useJUnitPlatform { excludeTags = setOf("emulator") }
        }
    }
}
```

Override locally:

```bash
firebase emulators:exec --only firestore,auth "./gradlew :app:test -PincludeEmulatorTests"
```

Where the Gradle property toggles the `excludeTags` set to empty. Add to `app/build.gradle.kts`:

```kotlin
val includeEmulator = providers.gradleProperty("includeEmulatorTests").isPresent
unitTests.all { test ->
    test.useJUnitPlatform {
        if (!includeEmulator) excludeTags = setOf("emulator")
    }
}
```

- [ ] **Step 7 — Verify emulator integration test passes**

In one terminal:

```bash
firebase emulators:start --only firestore,auth
```

In another:

```bash
./gradlew :app:test --tests "app.pantry.data.firebase.EmulatorSmokeTest" -PincludeEmulatorTests
```

Expected: PASS.

- [ ] **Step 8 — Update the README and commit**

Append to `pantry/README.md`:

```markdown
## Testing

- `./gradlew :app:test` — fast unit + Compose UI (Robolectric) tests
- `firebase emulators:exec --only firestore,auth "./gradlew :app:test -PincludeEmulatorTests"` — full suite including emulator integration tests
```

```bash
git add .
git commit -m "test: wire JUnit 5, Robolectric Compose, and Firebase emulator tests"
```

---

## US-4: Material 3 theme & app shell

**As a** developer
**I want** a Material 3 theme (dynamic colour with fallback) and a Compose `NavHost`-based app shell
**So that** every subsequent screen plugs into a consistent visual + navigation foundation.

### Acceptance criteria

- `Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt` defined
- Dynamic colour used on Android 12+; a hand-defined Material 3 palette is the fallback (uses the purple from the spec mockups)
- A `PantryNavHost` with three named routes (`auth`, `household`, `home`) — only `auth` reachable for now
- A Robolectric test asserts `PantryNavHost` starts on the `auth` route
- `MainActivity` now hosts the NavHost instead of `BootstrapScreen`

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/theme/Color.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/theme/Type.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/theme/Shape.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/theme/Theme.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryRoute.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/nav/PantryNavHostTest.kt`

### Tasks

- [ ] **Step 1 — Define the colour palette**

Create `pantry/app/src/main/kotlin/app/pantry/ui/theme/Color.kt`:

```kotlin
package app.pantry.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Purple40 = Color(0xFF6750A4)
private val Purple80 = Color(0xFFD0BCFF)
private val PurpleGrey40 = Color(0xFF625B71)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink40 = Color(0xFF7D5260)
private val Pink80 = Color(0xFFEFB8C8)

val PantryLightScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

val PantryDarkScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)
```

- [ ] **Step 2 — Define typography and shapes**

Create `pantry/app/src/main/kotlin/app/pantry/ui/theme/Type.kt`:

```kotlin
package app.pantry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PantryTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
)
```

Create `pantry/app/src/main/kotlin/app/pantry/ui/theme/Shape.kt`:

```kotlin
package app.pantry.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PantryShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)
```

- [ ] **Step 3 — Define the theme composable**

Create `pantry/app/src/main/kotlin/app/pantry/ui/theme/Theme.kt`:

```kotlin
package app.pantry.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun PantryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> PantryDarkScheme
        else -> PantryLightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PantryTypography,
        shapes = PantryShapes,
        content = content,
    )
}
```

- [ ] **Step 4 — Define routes**

Create `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryRoute.kt`:

```kotlin
package app.pantry.ui.nav

sealed interface PantryRoute {
    val path: String
    data object Auth : PantryRoute { override val path = "auth" }
    data object Household : PantryRoute { override val path = "household" }
    data object Home : PantryRoute { override val path = "home" }
}
```

- [ ] **Step 5 — Write the failing NavHost test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/nav/PantryNavHostTest.kt`:

```kotlin
package app.pantry.ui.nav

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PantryNavHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun starts_on_auth_placeholder() {
        composeRule.setContent {
            PantryNavHost(navController = rememberNavController())
        }
        composeRule.onNodeWithText("Sign in (placeholder)").assertIsDisplayed()
    }
}
```

- [ ] **Step 6 — Run the test; verify it fails (PantryNavHost not defined)**

```bash
./gradlew :app:test --tests "app.pantry.ui.nav.PantryNavHostTest"
```

Expected: FAIL — `Unresolved reference: PantryNavHost`.

- [ ] **Step 7 — Implement `PantryNavHost`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`:

```kotlin
package app.pantry.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun PantryNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = PantryRoute.Auth.path) {
        composable(PantryRoute.Auth.path) { AuthPlaceholder() }
        composable(PantryRoute.Household.path) { HouseholdPlaceholder() }
        composable(PantryRoute.Home.path) { HomePlaceholder() }
    }
}

@Composable private fun AuthPlaceholder() = Centered("Sign in (placeholder)")
@Composable private fun HouseholdPlaceholder() = Centered("Household (placeholder)")
@Composable private fun HomePlaceholder() = Centered("Home (placeholder)")

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
```

- [ ] **Step 8 — Wire NavHost into MainActivity**

Replace `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`:

```kotlin
package app.pantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.pantry.ui.nav.PantryNavHost
import app.pantry.ui.theme.PantryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PantryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantryNavHost(navController = rememberNavController())
                }
            }
        }
    }
}
```

The earlier `BootstrapScreenForTest()` helper can be removed; update `BootstrapScreenTest.kt` to use one of the placeholder strings or delete the test (it's been replaced by `PantryNavHostTest`).

- [ ] **Step 9 — Run all tests**

```bash
./gradlew :app:test
```

Expected: PASS (including the new NavHost test).

- [ ] **Step 10 — Commit**

```bash
git add .
git commit -m "feat(ui): add Material 3 theme and NavHost shell"
```

---

## US-5: Auth domain layer

**As a** developer
**I want** an `AuthRepository` interface and a Firebase-backed implementation that exposes the current user as a `StateFlow`
**So that** UI ViewModels depend on a pure-Kotlin contract rather than directly on `FirebaseAuth`.

### Acceptance criteria

- `UserProfile` domain model exists (uid, displayName, email, householdIds)
- `AuthRepository` interface with: `currentUser: StateFlow<UserProfile?>`, `signInWithEmail`, `signUpWithEmail`, `sendPasswordReset`, `signInWithGoogle`, `signOut`
- A `FirebaseAuthRepository` implementation
- Hilt module binds the implementation
- All repository methods return `Result<T>` for explicit error handling at the UI layer
- Unit tests cover happy path + at least one error path per method, using MockK fakes for `FirebaseAuth`

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/domain/model/UserProfile.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/domain/model/AuthError.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/auth/AuthRepository.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/auth/FirebaseAuthRepository.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/di/AuthModule.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/data/auth/FirebaseAuthRepositoryTest.kt`

### Tasks

- [ ] **Step 1 — Define domain model and error type**

Create `pantry/app/src/main/kotlin/app/pantry/domain/model/UserProfile.kt`:

```kotlin
package app.pantry.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val householdIds: List<String> = emptyList(),
)
```

Create `pantry/app/src/main/kotlin/app/pantry/domain/model/AuthError.kt`:

```kotlin
package app.pantry.domain.model

sealed class AuthError(message: String) : Throwable(message) {
    data object InvalidCredentials : AuthError("Email or password is incorrect")
    data object EmailAlreadyInUse : AuthError("That email is already registered")
    data object WeakPassword : AuthError("Password must be at least 6 characters")
    data object InvalidEmail : AuthError("Enter a valid email address")
    data object NoNetwork : AuthError("No internet connection")
    data class Unknown(val cause: Throwable) : AuthError(cause.message ?: "Authentication failed")
}
```

- [ ] **Step 2 — Define the repository interface**

Create `pantry/app/src/main/kotlin/app/pantry/data/auth/AuthRepository.kt`:

```kotlin
package app.pantry.data.auth

import app.pantry.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
}
```

- [ ] **Step 3 — Write the failing repository tests**

Create `pantry/app/src/test/kotlin/app/pantry/data/auth/FirebaseAuthRepositoryTest.kt`:

```kotlin
package app.pantry.data.auth

import app.pantry.domain.model.AuthError
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseAuthRepositoryTest {

    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val repo = FirebaseAuthRepository(firebaseAuth)

    @Test
    fun `signInWithEmail returns UserProfile on success`() = runTest {
        val user = mockk<FirebaseUser> {
            every { uid } returns "u-1"
            every { email } returns "alice@example.com"
            every { displayName } returns "Alice"
        }
        val authResult = mockk<AuthResult> { every { this@mockk.user } returns user }
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

        val result = repo.signInWithEmail("alice@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals("u-1", result.getOrThrow().uid)
    }

    @Test
    fun `signInWithEmail maps invalid credentials to AuthError InvalidCredentials`() = runTest {
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(FirebaseAuthInvalidCredentialsException("WRONG_PASSWORD", "bad"))

        val result = repo.signInWithEmail("alice@example.com", "wrong")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.InvalidCredentials::class.java, result.exceptionOrNull())
    }

    @Test
    fun `signUpWithEmail maps email collision to EmailAlreadyInUse`() = runTest {
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(FirebaseAuthUserCollisionException("EMAIL_EXISTS", "exists"))

        val result = repo.signUpWithEmail("alice@example.com", "password123", "Alice")

        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.EmailAlreadyInUse::class.java, result.exceptionOrNull())
    }
}
```

- [ ] **Step 4 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.data.auth.FirebaseAuthRepositoryTest"
```

Expected: FAIL — `FirebaseAuthRepository` not found.

- [ ] **Step 5 — Implement the repository**

Create `pantry/app/src/main/kotlin/app/pantry/data/auth/FirebaseAuthRepository.kt`:

```kotlin
package app.pantry.data.auth

import app.pantry.domain.model.AuthError
import app.pantry.domain.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser?.toProfile())
    override val currentUser: StateFlow<UserProfile?> = _currentUser

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser?.toProfile()
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> =
        runAuth { firebaseAuth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile> =
        runAuth {
            val res = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            res.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build())?.await()
            res
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(mapAuthError(t))
        }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> =
        runAuth {
            val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
        }

    override suspend fun signOut(): Result<Unit> =
        try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(AuthError.Unknown(t))
        }

    private inline fun runAuth(block: () -> com.google.firebase.auth.AuthResult): Result<UserProfile> =
        try {
            val user = block().user ?: return Result.failure(AuthError.Unknown(IllegalStateException("Null FirebaseUser after auth call")))
            Result.success(user.toProfile())
        } catch (t: Throwable) {
            Result.failure(mapAuthError(t))
        }

    private fun mapAuthError(t: Throwable): AuthError = when (t) {
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is IOException -> AuthError.NoNetwork
        else -> AuthError.Unknown(t)
    }

    private fun FirebaseUser.toProfile(): UserProfile = UserProfile(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = email.orEmpty(),
        householdIds = emptyList(), // populated by HouseholdRepository in US-9
    )
}
```

- [ ] **Step 6 — Run the tests; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.data.auth.FirebaseAuthRepositoryTest"
```

Expected: PASS.

- [ ] **Step 7 — Wire Hilt**

Create `pantry/app/src/main/kotlin/app/pantry/di/AuthModule.kt`:

```kotlin
package app.pantry.di

import app.pantry.data.auth.AuthRepository
import app.pantry.data.auth.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindings {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth
}
```

- [ ] **Step 8 — Commit**

```bash
git add .
git commit -m "feat(auth): add AuthRepository contract and Firebase implementation"
```

---

## US-6: Sign up / sign in with email + password

**As a** new user
**I want** to sign in or sign up using an email and password
**So that** I can access my household's stock from any phone.

### Acceptance criteria

- `AuthScreen` shows a "Continue with email" button that toggles to inline email + password fields
- "Sign up" mode adds a "Display name" field
- Submit button shows a progress indicator while the request is in-flight
- Validation: email format, password ≥ 6 chars, displayName non-empty; "Submit" disabled until all valid; errors shown inline
- On success, the navigation moves to `household` route
- On failure (wrong password, email taken, etc.), an inline error appears under the corresponding field or as a snackbar
- A Robolectric Compose test covers: empty state → fill → submit → success-state transition with a fake `AuthRepository`

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthScreen.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthViewModel.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthUiState.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/auth/AuthViewModelTest.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/auth/AuthScreenTest.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`

### Tasks

- [ ] **Step 1 — Define `AuthUiState`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthUiState.kt`:

```kotlin
package app.pantry.ui.auth

import app.pantry.domain.model.AuthError

data class AuthUiState(
    val mode: Mode = Mode.Welcome,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val isSubmitting: Boolean = false,
    val toastMessage: String? = null,        // transient errors / confirmations
    val navigateToHousehold: Boolean = false,
) {
    enum class Mode { Welcome, EmailSignIn, EmailSignUp }

    val canSubmit: Boolean
        get() = when (mode) {
            Mode.EmailSignIn -> emailError == null && passwordError == null && email.isNotBlank() && password.isNotBlank()
            Mode.EmailSignUp -> emailError == null && passwordError == null && displayNameError == null
                && email.isNotBlank() && password.isNotBlank() && displayName.isNotBlank()
            Mode.Welcome -> false
        }

    fun withErrorMessage(error: AuthError): AuthUiState = copy(
        isSubmitting = false,
        toastMessage = when (error) {
            AuthError.InvalidCredentials -> "Email or password is incorrect"
            AuthError.EmailAlreadyInUse -> "That email is already registered"
            AuthError.WeakPassword -> "Password must be at least 6 characters"
            AuthError.InvalidEmail -> "Enter a valid email address"
            AuthError.NoNetwork -> "No internet connection"
            is AuthError.Unknown -> "Something went wrong. Try again."
        }
    )
}
```

- [ ] **Step 2 — Write the failing ViewModel test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/auth/AuthViewModelTest.kt`:

```kotlin
package app.pantry.ui.auth

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.AuthError
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val repo: AuthRepository = mockk(relaxed = true)
    private lateinit var vm: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // currentUser StateFlow must be present
        io.mockk.every { repo.currentUser } returns MutableStateFlow(null)
        vm = AuthViewModel(repo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `email format validation rejects invalid email`() = runTest {
        vm.onEmailChange("not-an-email")
        assertEquals("Enter a valid email address", vm.uiState.value.emailError)
    }

    @Test
    fun `successful sign-in transitions to navigateToHousehold = true`() = runTest {
        coEvery { repo.signInWithEmail("a@b.com", "secret123") } returns
            Result.success(UserProfile("u-1", "Alice", "a@b.com"))

        vm.uiState.test {
            awaitItem() // initial
            vm.switchMode(AuthUiState.Mode.EmailSignIn)
            vm.onEmailChange("a@b.com")
            vm.onPasswordChange("secret123")
            assertTrue(awaitItem().canSubmit || awaitItem().canSubmit)
            vm.submit()
            // drain to the "navigate" state
            var state = awaitItem()
            while (!state.navigateToHousehold && state.toastMessage == null) {
                state = awaitItem()
            }
            assertTrue(state.navigateToHousehold)
            assertFalse(state.isSubmitting)
        }
    }

    @Test
    fun `invalid credentials sets toastMessage and stops submitting`() = runTest {
        coEvery { repo.signInWithEmail(any(), any()) } returns Result.failure(AuthError.InvalidCredentials)
        vm.switchMode(AuthUiState.Mode.EmailSignIn)
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("nope")
        vm.submit()
        // wait a tick
        val state = vm.uiState.value
        assertEquals("Email or password is incorrect", state.toastMessage)
        assertFalse(state.isSubmitting)
    }
}
```

- [ ] **Step 3 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.ui.auth.AuthViewModelTest"
```

Expected: FAIL — `AuthViewModel` not found.

- [ ] **Step 4 — Implement `AuthViewModel`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthViewModel.kt`:

```kotlin
package app.pantry.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.AuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _state.asStateFlow()

    fun switchMode(mode: AuthUiState.Mode) = _state.update { it.copy(mode = mode) }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = if (value.isBlank() || EMAIL_REGEX.matches(value)) null else "Enter a valid email address")
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = if (value.isBlank() || value.length >= 6) null else "Password must be at least 6 characters")
    }

    fun onDisplayNameChange(value: String) = _state.update {
        it.copy(displayName = value, displayNameError = if (value.isBlank()) "Required" else null)
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit || s.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, toastMessage = null) }
        viewModelScope.launch {
            val result = when (s.mode) {
                AuthUiState.Mode.EmailSignIn -> auth.signInWithEmail(s.email, s.password)
                AuthUiState.Mode.EmailSignUp -> auth.signUpWithEmail(s.email, s.password, s.displayName)
                AuthUiState.Mode.Welcome -> return@launch
            }
            result.fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHousehold = true) } },
                onFailure = { e -> _state.update { it.withErrorMessage(e as? AuthError ?: AuthError.Unknown(e)) } },
            )
        }
    }

    fun consumeNavigation() = _state.update { it.copy(navigateToHousehold = false) }
    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
```

- [ ] **Step 5 — Run the ViewModel test; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.auth.AuthViewModelTest"
```

Expected: PASS.

- [ ] **Step 6 — Implement `AuthScreen` composable**

Create `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthScreen.kt`:

```kotlin
package app.pantry.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.navigateToHousehold) {
        if (state.navigateToHousehold) {
            viewModel.consumeNavigation()
            onAuthenticated()
        }
    }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbar.showSnackbar(it); viewModel.consumeToast() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding: PaddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pantry", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                AuthUiState.Mode.Welcome -> WelcomeButtons(
                    onPickEmail = { viewModel.switchMode(AuthUiState.Mode.EmailSignIn) },
                    onPickSignUp = { viewModel.switchMode(AuthUiState.Mode.EmailSignUp) },
                )
                AuthUiState.Mode.EmailSignIn,
                AuthUiState.Mode.EmailSignUp -> EmailForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun WelcomeButtons(onPickEmail: () -> Unit, onPickSignUp: () -> Unit) {
    Button(onClick = onPickEmail, modifier = Modifier.fillMaxWidth().testTag("welcome_signin")) {
        Text("Sign in with email")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onPickSignUp, modifier = Modifier.testTag("welcome_signup")) {
        Text("Create an account")
    }
}

@Composable
private fun EmailForm(state: AuthUiState, viewModel: AuthViewModel) {
    if (state.mode == AuthUiState.Mode.EmailSignUp) {
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onDisplayNameChange,
            label = { Text("Your name") },
            isError = state.displayNameError != null,
            supportingText = { state.displayNameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().testTag("field_name"),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
    }
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("Email") },
        isError = state.emailError != null,
        supportingText = { state.emailError?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth().testTag("field_email"),
        singleLine = true,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        label = { Text("Password") },
        isError = state.passwordError != null,
        supportingText = { state.passwordError?.let { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().testTag("field_password"),
        singleLine = true,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = viewModel::submit,
        enabled = state.canSubmit && !state.isSubmitting,
        modifier = Modifier.fillMaxWidth().testTag("submit"),
    ) {
        if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.height(20.dp))
        else Text(if (state.mode == AuthUiState.Mode.EmailSignIn) "Sign in" else "Create account")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = {
            viewModel.switchMode(
                if (state.mode == AuthUiState.Mode.EmailSignIn) AuthUiState.Mode.EmailSignUp
                else AuthUiState.Mode.EmailSignIn
            )
        }
    ) {
        Text(if (state.mode == AuthUiState.Mode.EmailSignIn) "Need an account? Sign up" else "Have an account? Sign in")
    }
}
```

- [ ] **Step 7 — Wire `AuthScreen` into `PantryNavHost`**

Replace `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`:

```kotlin
package app.pantry.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pantry.ui.auth.AuthScreen

@Composable
fun PantryNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = PantryRoute.Auth.path) {
        composable(PantryRoute.Auth.path) {
            AuthScreen(onAuthenticated = {
                navController.navigate(PantryRoute.Household.path) {
                    popUpTo(PantryRoute.Auth.path) { inclusive = true }
                }
            })
        }
        composable(PantryRoute.Household.path) { Centered("Household (placeholder)") }
        composable(PantryRoute.Home.path) { Centered("Home (placeholder)") }
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
```

Delete `PantryNavHostTest`'s "Sign in (placeholder)" assertion — replace with a check for "Pantry" headline:

```kotlin
composeRule.onNodeWithText("Pantry").assertIsDisplayed()
```

- [ ] **Step 8 — Add `AuthScreen` Compose Robolectric test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/auth/AuthScreenTest.kt`:

```kotlin
package app.pantry.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun signs_in_with_email_and_navigates() {
        val repo: AuthRepository = mockk(relaxed = true)
        every { repo.currentUser } returns MutableStateFlow(null)
        coEvery { repo.signInWithEmail("a@b.com", "secret123") } returns Result.success(UserProfile("u-1", "A", "a@b.com"))

        var authenticated = false
        composeRule.setContent {
            AuthScreen(
                onAuthenticated = { authenticated = true },
                viewModel = AuthViewModel(repo),
            )
        }
        composeRule.onNodeWithText("Sign in with email").performClick()
        composeRule.onNodeWithTag("submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("field_email").performTextInput("a@b.com")
        composeRule.onNodeWithTag("field_password").performTextInput("secret123")
        composeRule.onNodeWithTag("submit").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { authenticated }
    }
}
```

- [ ] **Step 9 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 10 — Run the app on a real device or emulator and verify the flow manually**

1. Start `firebase emulators:start --only auth,firestore,functions`
2. Run the app in Android Studio
3. Tap "Sign in with email" → "Need an account? Sign up" → enter name + email + password ≥ 6 chars → "Create account"
4. App moves to the Household placeholder
5. Check the Firebase Emulator UI (`http://localhost:4000`) → Auth tab → see the new user

- [ ] **Step 11 — Commit**

```bash
git add .
git commit -m "feat(auth): add email/password sign-in and sign-up flow"
```

---

## US-7: Forgot password

**As a** user who has forgotten my password
**I want** to receive a password reset email
**So that** I can regain access to my account.

### Acceptance criteria

- On the email sign-in form, a "Forgot password?" `TextButton` appears under the password field
- Tapping it opens a small dialog with one email field (pre-filled if available) and a "Send" button
- Tapping "Send" calls `AuthRepository.sendPasswordReset` and shows a snackbar "Check your inbox to reset your password."
- Errors (invalid email, no network) show as inline supportingText on the dialog
- Closing the dialog returns to the sign-in form unchanged
- Robolectric Compose test covers the happy path

### Files

- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthUiState.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthViewModel.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthScreen.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/auth/ForgotPasswordDialog.kt`
- Modify: `pantry/app/src/test/kotlin/app/pantry/ui/auth/AuthViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Extend `AuthUiState` with reset dialog state**

In `AuthUiState.kt`, add fields:

```kotlin
data class AuthUiState(
    // existing fields...
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetEmailError: String? = null,
    val isSendingReset: Boolean = false,
) {
    // existing helpers...
}
```

- [ ] **Step 2 — Write the failing ViewModel test**

In `AuthViewModelTest.kt` add:

```kotlin
@Test
fun `sendPasswordReset shows confirmation toast`() = runTest {
    coEvery { repo.sendPasswordReset("a@b.com") } returns Result.success(Unit)
    vm.openResetDialog(prefill = "a@b.com")
    vm.sendPasswordReset()
    val state = vm.uiState.value
    assertEquals("Check your inbox to reset your password.", state.toastMessage)
    assertFalse(state.showResetDialog)
}
```

- [ ] **Step 3 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.ui.auth.AuthViewModelTest.sendPasswordReset*"
```

Expected: FAIL.

- [ ] **Step 4 — Extend `AuthViewModel`**

Add to `AuthViewModel`:

```kotlin
fun openResetDialog(prefill: String) = _state.update {
    it.copy(showResetDialog = true, resetEmail = prefill, resetEmailError = null)
}

fun onResetEmailChange(value: String) = _state.update {
    it.copy(resetEmail = value, resetEmailError = if (value.isBlank() || EMAIL_REGEX.matches(value)) null else "Enter a valid email address")
}

fun closeResetDialog() = _state.update { it.copy(showResetDialog = false, resetEmailError = null, isSendingReset = false) }

fun sendPasswordReset() {
    val s = _state.value
    if (s.resetEmail.isBlank() || s.resetEmailError != null || s.isSendingReset) return
    _state.update { it.copy(isSendingReset = true) }
    viewModelScope.launch {
        auth.sendPasswordReset(s.resetEmail).fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        isSendingReset = false,
                        showResetDialog = false,
                        toastMessage = "Check your inbox to reset your password.",
                    )
                }
            },
            onFailure = { e -> _state.update { it.copy(isSendingReset = false).withErrorMessage(e as? AuthError ?: AuthError.Unknown(e)) } },
        )
    }
}
```

- [ ] **Step 5 — Run the test; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.auth.AuthViewModelTest"
```

Expected: PASS.

- [ ] **Step 6 — Implement `ForgotPasswordDialog`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/auth/ForgotPasswordDialog.kt`:

```kotlin
package app.pantry.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ForgotPasswordDialog(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.showResetDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset your password") },
        text = {
            OutlinedTextField(
                value = state.resetEmail,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                isError = state.resetEmailError != null,
                supportingText = { state.resetEmailError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.testTag("reset_email"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSend,
                enabled = !state.isSendingReset && state.resetEmail.isNotBlank() && state.resetEmailError == null,
                modifier = Modifier.testTag("reset_send"),
            ) { Text(if (state.isSendingReset) "Sending…" else "Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
```

- [ ] **Step 7 — Wire the dialog into `AuthScreen`**

In `AuthScreen.kt`, inside the email-form branch, after the password field add a "Forgot password?" `TextButton`:

```kotlin
TextButton(
    onClick = { viewModel.openResetDialog(prefill = state.email) },
    modifier = Modifier.testTag("forgot_password"),
) { Text("Forgot password?") }
```

And after the form, render the dialog:

```kotlin
ForgotPasswordDialog(
    state = state,
    onEmailChange = viewModel::onResetEmailChange,
    onSend = viewModel::sendPasswordReset,
    onDismiss = viewModel::closeResetDialog,
)
```

- [ ] **Step 8 — Manual verification + commit**

Run the app, open sign-in, tap "Forgot password?", enter an email, hit Send → snackbar appears → emulator UI Auth tab shows the email reset event (or in prod, the email arrives).

```bash
git add .
git commit -m "feat(auth): add forgot-password flow"
```

---

## US-8: Sign in with Google

**As a** user with a Google account on this device
**I want** a one-tap "Continue with Google" option
**So that** I don't need to remember a password.

### Acceptance criteria

- A "Continue with Google" button appears on the Welcome screen
- Tapping it opens the Credential Manager bottom sheet
- On selection, the resulting Google ID token is forwarded to `AuthRepository.signInWithGoogle`
- On success, navigation moves to `household`
- On cancellation, the welcome screen is unchanged
- On error, a snackbar appears
- A Robolectric test covers the bridging code that converts a `GetCredentialResponse` to an ID token call (using a hand-rolled fake Credential Manager)

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/auth/GoogleSignInController.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthViewModel.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/auth/AuthScreen.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/auth/GoogleSignInControllerTest.kt`
- Modify: `pantry/app/src/main/AndroidManifest.xml` (add `web_client_id` string)
- Create: `pantry/app/src/main/res/values/strings.xml`

### Tasks

- [ ] **Step 1 — Add the web client ID resource**

In the Firebase console → project settings → general → under "Your apps", find the "Web client ID" used by Google sign-in (auto-generated when you enabled Google in US-2). Copy it.

Create `pantry/app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Pantry</string>
    <string name="default_web_client_id" translatable="false">REPLACE_WITH_WEB_CLIENT_ID</string>
</resources>
```

Replace the placeholder with the actual ID. Add `strings.xml` to `.gitignore` if you want to keep the ID out of source control — otherwise leave it in (the ID is not secret).

- [ ] **Step 2 — Create `GoogleSignInController`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/auth/GoogleSignInController.kt`:

```kotlin
package app.pantry.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInController(
    private val context: Context,
    private val webClientId: String,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) {
    /** Returns the Google ID token, or null if the user cancelled, or throws on error. */
    suspend fun requestIdToken(): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val response: GetCredentialResponse = credentialManager.getCredential(context, request)
            extractIdToken(response)
        } catch (_: GetCredentialCancellationException) {
            null
        }
    }

    private fun extractIdToken(response: GetCredentialResponse): String? {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        return null
    }
}
```

- [ ] **Step 3 — Extend `AuthViewModel` with `signInWithGoogle`**

Add to `AuthViewModel`:

```kotlin
fun signInWithGoogle(idToken: String) {
    if (_state.value.isSubmitting) return
    _state.update { it.copy(isSubmitting = true, toastMessage = null) }
    viewModelScope.launch {
        auth.signInWithGoogle(idToken).fold(
            onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHousehold = true) } },
            onFailure = { e -> _state.update { it.withErrorMessage(e as? AuthError ?: AuthError.Unknown(e)) } },
        )
    }
}
```

- [ ] **Step 4 — Add a "Continue with Google" button in `AuthScreen` Welcome**

In `WelcomeButtons`, before the email button:

```kotlin
val context = androidx.compose.ui.platform.LocalContext.current
val webClientId = stringResource(R.string.default_web_client_id)
val scope = rememberCoroutineScope()
val controller = remember(context, webClientId) { GoogleSignInController(context, webClientId) }

Button(
    onClick = {
        scope.launch {
            try {
                val token = controller.requestIdToken() ?: return@launch
                viewModel.signInWithGoogle(token)
            } catch (t: Throwable) {
                viewModel.uiState.value.withErrorMessage(AuthError.Unknown(t))
            }
        }
    },
    modifier = Modifier.fillMaxWidth().testTag("welcome_google"),
) { Text("Continue with Google") }
Spacer(Modifier.height(8.dp))
```

Imports to add at the top of `AuthScreen.kt`:

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import app.pantry.R
import kotlinx.coroutines.launch
```

- [ ] **Step 5 — Write a controller test using a fake CredentialManager**

Create `pantry/app/src/test/kotlin/app/pantry/ui/auth/GoogleSignInControllerTest.kt`:

```kotlin
package app.pantry.ui.auth

import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInControllerTest {

    @Test
    fun returns_null_on_cancellation() = runTest {
        val cm: CredentialManager = mockk()
        coEvery { cm.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } throws
            GetCredentialCancellationException("user cancelled")
        val controller = GoogleSignInController(
            context = ApplicationProvider.getApplicationContext(),
            webClientId = "test",
            credentialManager = cm,
        )
        assertNull(controller.requestIdToken())
    }

    @Test
    fun extracts_id_token_from_response() = runTest {
        val bundle = Bundle().apply {
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", "fake-id-token")
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "alice@example.com")
        }
        val credential = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, bundle)
        val cm: CredentialManager = mockk()
        coEvery { cm.getCredential(any<android.content.Context>(), any<GetCredentialRequest>()) } returns
            GetCredentialResponse(credential)
        val controller = GoogleSignInController(
            context = ApplicationProvider.getApplicationContext(),
            webClientId = "test",
            credentialManager = cm,
        )
        assertEquals("fake-id-token", controller.requestIdToken())
    }
}
```

- [ ] **Step 6 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 7 — Manual verification on a real device**

Google sign-in requires Play Services and Google account on the device — Robolectric can't fully exercise the UI. Install on a real Android device (or emulator with Play Services), tap "Continue with Google", pick an account, end up on the Household placeholder. Verify in the Firebase Auth emulator UI that a user was created.

- [ ] **Step 8 — Commit**

```bash
git add .
git commit -m "feat(auth): add Google sign-in via Credential Manager"
```

---

## US-9: Household domain layer

**As a** developer
**I want** a `HouseholdRepository` interface and a Firestore-backed implementation
**So that** UI ViewModels can create/observe households without touching Firestore directly.

### Acceptance criteria

- `Household` domain model: `id`, `name`, `memberUids: List<String>`, `inviteCode: String`
- `HouseholdRepository` interface: `observe(householdId): Flow<Household>`, `observeUserHouseholds(uid): Flow<List<Household>>`, `create(name, ownerUid): Result<Household>`, `regenerateInviteCode(householdId): Result<String>`, `rename(householdId, name): Result<Unit>`
- A `FirestoreHouseholdRepository` implementation reading from `households/{id}` and writing the membership atomically with the user doc update (`households` list on `users/{uid}`)
- Invite codes are 6-character uppercase alphanumeric, collision-checked at creation
- Unit tests use a fake Firestore via `mockk` for the happy + collision paths
- Integration test against the Firestore emulator covers a real create + observe round-trip
- Firestore security rules (`firestore.rules`) updated for the schema in the design spec

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/domain/model/Household.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/household/HouseholdRepository.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/household/InviteCodeGenerator.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/di/HouseholdModule.kt`
- Modify: `pantry/firestore.rules`
- Create: `pantry/app/src/test/kotlin/app/pantry/data/household/InviteCodeGeneratorTest.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/data/household/FirestoreHouseholdRepositoryEmulatorTest.kt`

### Tasks

- [ ] **Step 1 — Define the model**

Create `pantry/app/src/main/kotlin/app/pantry/domain/model/Household.kt`:

```kotlin
package app.pantry.domain.model

data class Household(
    val id: String,
    val name: String,
    val memberUids: List<String>,
    val inviteCode: String,
)
```

- [ ] **Step 2 — Define the repository interface**

Create `pantry/app/src/main/kotlin/app/pantry/data/household/HouseholdRepository.kt`:

```kotlin
package app.pantry.data.household

import app.pantry.domain.model.Household
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    fun observe(householdId: String): Flow<Household?>
    fun observeUserHouseholds(uid: String): Flow<List<Household>>
    suspend fun create(name: String, ownerUid: String): Result<Household>
    suspend fun rename(householdId: String, newName: String): Result<Unit>
    suspend fun regenerateInviteCode(householdId: String): Result<String>
}
```

- [ ] **Step 3 — Write the failing invite code test**

Create `pantry/app/src/test/kotlin/app/pantry/data/household/InviteCodeGeneratorTest.kt`:

```kotlin
package app.pantry.data.household

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class InviteCodeGeneratorTest {
    @Test
    fun `generates a 6-character uppercase alphanumeric code`() {
        val code = InviteCodeGenerator(random = Random(seed = 42)).next()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isUpperCase() || it.isDigit() })
    }

    @Test
    fun `consecutive calls produce different codes`() {
        val gen = InviteCodeGenerator(random = Random(seed = 42))
        val a = gen.next()
        val b = gen.next()
        assertTrue(a != b)
    }
}
```

- [ ] **Step 4 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.data.household.InviteCodeGeneratorTest"
```

Expected: FAIL.

- [ ] **Step 5 — Implement `InviteCodeGenerator`**

Create `pantry/app/src/main/kotlin/app/pantry/data/household/InviteCodeGenerator.kt`:

```kotlin
package app.pantry.data.household

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class InviteCodeGenerator @Inject constructor(
    private val random: Random = Random.Default,
) {
    private val alphabet = ('A'..'Z') + ('0'..'9')

    fun next(length: Int = CODE_LENGTH): String =
        buildString(length) { repeat(length) { append(alphabet.random(random)) } }

    companion object { const val CODE_LENGTH = 6 }
}
```

- [ ] **Step 6 — Run test; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.data.household.InviteCodeGeneratorTest"
```

Expected: PASS.

- [ ] **Step 7 — Implement the Firestore repository**

Create `pantry/app/src/main/kotlin/app/pantry/data/household/FirestoreHouseholdRepository.kt`:

```kotlin
package app.pantry.data.household

import app.pantry.domain.model.Household
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreHouseholdRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val codes: InviteCodeGenerator,
) : HouseholdRepository {

    override fun observe(householdId: String): Flow<Household?> = callbackFlow {
        val reg = firestore.collection("households").document(householdId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toHousehold()) }
        awaitClose { reg.remove() }
    }

    override fun observeUserHouseholds(uid: String): Flow<List<Household>> = callbackFlow {
        val reg = firestore.collection("households")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { qs, _ ->
                trySend(qs?.documents.orEmpty().mapNotNull { it.toHousehold() })
            }
        awaitClose { reg.remove() }
    }

    override suspend fun create(name: String, ownerUid: String): Result<Household> = runCatching {
        val code = uniqueCode()
        val doc = firestore.collection("households").document()
        val data = mapOf(
            "name" to name,
            "memberUids" to listOf(ownerUid),
            "inviteCode" to code,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to ownerUid,
        )
        firestore.runBatch { batch ->
            batch.set(doc, data)
            batch.update(firestore.collection("users").document(ownerUid), "households", FieldValue.arrayUnion(doc.id))
        }.await()
        Household(id = doc.id, name = name, memberUids = listOf(ownerUid), inviteCode = code)
    }

    override suspend fun rename(householdId: String, newName: String): Result<Unit> = runCatching {
        firestore.collection("households").document(householdId).update("name", newName).await()
    }

    override suspend fun regenerateInviteCode(householdId: String): Result<String> = runCatching {
        val code = uniqueCode()
        firestore.collection("households").document(householdId).update("inviteCode", code).await()
        code
    }

    private suspend fun uniqueCode(): String {
        repeat(MAX_ATTEMPTS) {
            val candidate = codes.next()
            val q = firestore.collection("households").whereEqualTo("inviteCode", candidate).limit(1).get().await()
            if (q.isEmpty) return candidate
        }
        error("Could not allocate a unique invite code after $MAX_ATTEMPTS attempts")
    }

    private fun DocumentSnapshot.toHousehold(): Household? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        val members = (get("memberUids") as? List<String>).orEmpty()
        return Household(
            id = id,
            name = getString("name").orEmpty(),
            memberUids = members,
            inviteCode = getString("inviteCode").orEmpty(),
        )
    }

    companion object { private const val MAX_ATTEMPTS = 5 }
}
```

- [ ] **Step 8 — Add Hilt bindings**

Create `pantry/app/src/main/kotlin/app/pantry/di/HouseholdModule.kt`:

```kotlin
package app.pantry.di

import app.pantry.data.household.FirestoreHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HouseholdBindings {
    @Binds @Singleton
    abstract fun bindHouseholdRepository(impl: FirestoreHouseholdRepository): HouseholdRepository
}

@Module
@InstallIn(SingletonComponent::class)
object HouseholdModule {
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = Firebase.firestore
}
```

- [ ] **Step 9 — Update Firestore security rules**

Replace `pantry/firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }

    match /households/{hid} {
      allow read: if isMember(hid);
      allow update, delete: if isMember(hid);
      allow create: if request.auth != null
        && request.resource.data.memberUids.size() == 1
        && request.resource.data.memberUids[0] == request.auth.uid;

      match /{subcollection}/{doc} {
        allow read, write: if isMember(hid);
      }
    }

    function isMember(hid) {
      return request.auth.uid in get(/databases/$(database)/documents/households/$(hid)).data.memberUids;
    }
  }
}
```

- [ ] **Step 10 — Write the emulator integration test**

Create `pantry/app/src/test/kotlin/app/pantry/data/household/FirestoreHouseholdRepositoryEmulatorTest.kt`:

```kotlin
package app.pantry.data.household

import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Tag("emulator")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirestoreHouseholdRepositoryEmulatorTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: FirestoreHouseholdRepository

    @BeforeAll
    fun setUp() {
        if (FirebaseApp.getApps(androidx.test.core.app.ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setApplicationId("pantry-dev")
                    .setProjectId("pantry-dev")
                    .setApiKey("fake")
                    .build(),
            )
        }
        firestore = FirebaseFirestore.getInstance()
        firestore.useEmulator("localhost", 8080)
        repo = FirestoreHouseholdRepository(firestore, InviteCodeGenerator())
    }

    @Test
    fun `create then observe round-trip`() = runTest {
        val result = repo.create("Alice's House", ownerUid = "u-1")
        assertTrue(result.isSuccess)
        val household = result.getOrThrow()

        repo.observe(household.id).test {
            val first = awaitItem()
            assertEquals("Alice's House", first?.name)
            assertEquals(listOf("u-1"), first?.memberUids)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 11 — Run emulator test**

In one terminal: `firebase emulators:start --only firestore,auth`
In another:

```bash
./gradlew :app:test --tests "app.pantry.data.household.*" -PincludeEmulatorTests
```

Expected: PASS.

- [ ] **Step 12 — Commit**

```bash
git add .
git commit -m "feat(household): add HouseholdRepository with Firestore implementation and rules"
```

---

## US-10: Create household

**As a** signed-in user with no household
**I want** to enter a household name and create a fresh household
**So that** I become its sole member and can start tracking stock.

### Acceptance criteria

- After auth, if the user's `households: []` is empty, the app navigates to `household` route
- `HouseholdOnboardingScreen` shows two primary actions: "Create a household" and "Join with invite code"
- Choosing "Create" reveals an inline text field for the household name + a "Create" button
- Submit calls `HouseholdRepository.create(name, currentUid)` and on success navigates to `home`
- Validation: name must be 1–40 characters
- Robolectric Compose test covers the create happy path with fake repositories

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingScreen.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModel.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingUiState.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define UI state**

Create `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingUiState.kt`:

```kotlin
package app.pantry.ui.household

data class HouseholdOnboardingUiState(
    val mode: Mode = Mode.Welcome,
    val householdName: String = "",
    val inviteCode: String = "",
    val nameError: String? = null,
    val inviteError: String? = null,
    val isSubmitting: Boolean = false,
    val toast: String? = null,
    val navigateToHome: Boolean = false,
) {
    enum class Mode { Welcome, Create, Join }
    val canSubmitCreate: Boolean
        get() = householdName.length in 1..40 && nameError == null && !isSubmitting
    val canSubmitJoin: Boolean
        get() = inviteCode.length == INVITE_CODE_LENGTH && inviteError == null && !isSubmitting
    companion object { const val INVITE_CODE_LENGTH = 6 }
}
```

- [ ] **Step 2 — Write failing ViewModel test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModelTest.kt`:

```kotlin
package app.pantry.ui.household

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdOnboardingViewModelTest {

    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val householdRepo: HouseholdRepository = mockk(relaxed = true)
    private lateinit var vm: HouseholdOnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepo.currentUser } returns MutableStateFlow(UserProfile("u-1", "Alice", "a@b.com"))
        vm = HouseholdOnboardingViewModel(authRepo, householdRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `create flow navigates to home on success`() = runTest {
        coEvery { householdRepo.create("Casa", "u-1") } returns Result.success(
            Household("h-1", "Casa", listOf("u-1"), "ABCDEF")
        )
        vm.switchMode(HouseholdOnboardingUiState.Mode.Create)
        vm.onNameChange("Casa")
        vm.submitCreate()
        assertTrue(vm.uiState.value.navigateToHome)
    }
}
```

- [ ] **Step 3 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.ui.household.HouseholdOnboardingViewModelTest"
```

Expected: FAIL.

- [ ] **Step 4 — Implement ViewModel**

Create `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModel.kt`:

```kotlin
package app.pantry.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HouseholdOnboardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HouseholdOnboardingUiState())
    val uiState: StateFlow<HouseholdOnboardingUiState> = _state.asStateFlow()

    fun switchMode(mode: HouseholdOnboardingUiState.Mode) = _state.update { it.copy(mode = mode) }

    fun onNameChange(value: String) = _state.update {
        it.copy(
            householdName = value,
            nameError = when {
                value.isBlank() -> null
                value.length > 40 -> "Max 40 characters"
                else -> null
            },
        )
    }

    fun onInviteCodeChange(value: String) = _state.update {
        val cleaned = value.uppercase().take(HouseholdOnboardingUiState.INVITE_CODE_LENGTH)
        it.copy(
            inviteCode = cleaned,
            inviteError = if (cleaned.length == HouseholdOnboardingUiState.INVITE_CODE_LENGTH || cleaned.isEmpty()) null else null,
        )
    }

    fun submitCreate() {
        val s = _state.value
        if (!s.canSubmitCreate) return
        val uid = auth.currentUser.value?.uid ?: return
        _state.update { it.copy(isSubmitting = true, toast = null) }
        viewModelScope.launch {
            households.create(s.householdName.trim(), uid).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHome = true) } },
                onFailure = { e -> _state.update { it.copy(isSubmitting = false, toast = e.message ?: "Failed to create household") } },
            )
        }
    }

    fun submitJoin() {
        // implemented in US-11
    }

    fun consumeNavigation() = _state.update { it.copy(navigateToHome = false) }
    fun consumeToast() = _state.update { it.copy(toast = null) }
}
```

- [ ] **Step 5 — Run the test; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.household.HouseholdOnboardingViewModelTest"
```

Expected: PASS.

- [ ] **Step 6 — Implement `HouseholdOnboardingScreen`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingScreen.kt`:

```kotlin
package app.pantry.ui.household

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HouseholdOnboardingScreen(
    onCreated: () -> Unit,
    onJoined: () -> Unit,
    viewModel: HouseholdOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) {
            viewModel.consumeNavigation()
            // For both create and join, "navigateToHome" is the signal — caller decides where to go.
            if (state.mode == HouseholdOnboardingUiState.Mode.Create) onCreated() else onJoined()
        }
    }
    LaunchedEffect(state.toast) {
        state.toast?.let { snackbar.showSnackbar(it); viewModel.consumeToast() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Set up your household", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                HouseholdOnboardingUiState.Mode.Welcome -> {
                    Button(
                        onClick = { viewModel.switchMode(HouseholdOnboardingUiState.Mode.Create) },
                        modifier = Modifier.fillMaxWidth().testTag("create_btn"),
                    ) { Text("Create a household") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.switchMode(HouseholdOnboardingUiState.Mode.Join) },
                        modifier = Modifier.testTag("join_btn"),
                    ) { Text("Join with invite code") }
                }
                HouseholdOnboardingUiState.Mode.Create -> {
                    OutlinedTextField(
                        value = state.householdName,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Household name") },
                        isError = state.nameError != null,
                        supportingText = { state.nameError?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_name"),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::submitCreate,
                        enabled = state.canSubmitCreate,
                        modifier = Modifier.fillMaxWidth().testTag("create_submit"),
                    ) { Text(if (state.isSubmitting) "Creating…" else "Create") }
                }
                HouseholdOnboardingUiState.Mode.Join -> {
                    // implemented in US-11
                    Text("Joining (placeholder)")
                }
            }
        }
    }
}
```

- [ ] **Step 7 — Wire into NavHost**

In `PantryNavHost.kt`, replace the `Household` route:

```kotlin
composable(PantryRoute.Household.path) {
    HouseholdOnboardingScreen(
        onCreated = { navController.navigate(PantryRoute.Home.path) { popUpTo(PantryRoute.Household.path) { inclusive = true } } },
        onJoined = { navController.navigate(PantryRoute.Home.path) { popUpTo(PantryRoute.Household.path) { inclusive = true } } },
    )
}
```

- [ ] **Step 8 — Manual verification + commit**

Run the app, sign in, type "Casa" → tap Create → land on Home placeholder. In the emulator UI Firestore tab, see `households/<id>` with `memberUids: ["u-1"]`.

```bash
git add .
git commit -m "feat(household): add create-household flow"
```

---

## US-11: Join household with invite code

**As a** signed-in user
**I want** to join an existing household by entering its 6-character invite code
**So that** I can share stock with the people I live with.

### Acceptance criteria

- The Welcome mode of `HouseholdOnboardingScreen` shows "Join with invite code"
- Tapping it reveals a single 6-char text field (uppercase, monospace) and a "Join" button
- Tapping "Join" calls a `joinHousehold` callable Cloud Function with `{ code: <value> }`
- On success, navigation moves to `home`
- On `not-found` (code doesn't match), inline error: "No household found for that code."
- On `already-member`, inline error: "You're already in this household."
- On network error: snackbar
- Cloud Function implementation lives in US-12; for US-11 we treat it as an injectable `JoinHouseholdGateway` interface so the UI can be tested without Firebase Functions

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/data/household/JoinHouseholdGateway.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/data/household/FirebaseJoinHouseholdGateway.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/di/HouseholdModule.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModel.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/household/HouseholdOnboardingScreen.kt`
- Modify: `pantry/app/src/test/kotlin/app/pantry/ui/household/HouseholdOnboardingViewModelTest.kt`

### Tasks

- [ ] **Step 1 — Define `JoinHouseholdGateway`**

Create `pantry/app/src/main/kotlin/app/pantry/data/household/JoinHouseholdGateway.kt`:

```kotlin
package app.pantry.data.household

interface JoinHouseholdGateway {
    suspend fun joinByCode(code: String): Result<String>  // returns householdId on success
}

sealed class JoinHouseholdError(message: String) : Throwable(message) {
    data object NotFound : JoinHouseholdError("No household found for that code.")
    data object AlreadyMember : JoinHouseholdError("You're already in this household.")
    data object NoNetwork : JoinHouseholdError("No internet connection")
    data class Unknown(val cause: Throwable) : JoinHouseholdError(cause.message ?: "Failed to join household")
}
```

- [ ] **Step 2 — Implement Firebase Functions backing**

Create `pantry/app/src/main/kotlin/app/pantry/data/household/FirebaseJoinHouseholdGateway.kt`:

```kotlin
package app.pantry.data.household

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseJoinHouseholdGateway @Inject constructor(
    private val functions: FirebaseFunctions,
) : JoinHouseholdGateway {

    override suspend fun joinByCode(code: String): Result<String> = try {
        val result = functions.getHttpsCallable("joinHousehold").call(mapOf("code" to code)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any?>
        val householdId = data["householdId"] as? String
            ?: return Result.failure(JoinHouseholdError.Unknown(IllegalStateException("Missing householdId")))
        Result.success(householdId)
    } catch (e: FirebaseFunctionsException) {
        Result.failure(
            when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> JoinHouseholdError.NotFound
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> JoinHouseholdError.AlreadyMember
                else -> JoinHouseholdError.Unknown(e)
            }
        )
    } catch (e: IOException) {
        Result.failure(JoinHouseholdError.NoNetwork)
    } catch (e: Throwable) {
        Result.failure(JoinHouseholdError.Unknown(e))
    }
}
```

- [ ] **Step 3 — Bind via Hilt**

In `HouseholdModule.kt`, add:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class JoinHouseholdBindings {
    @Binds @Singleton
    abstract fun bindJoinGateway(impl: FirebaseJoinHouseholdGateway): JoinHouseholdGateway
}

@Module
@InstallIn(SingletonComponent::class)
object FunctionsModule {
    @Provides @Singleton fun provideFunctions(): com.google.firebase.functions.FirebaseFunctions =
        com.google.firebase.functions.FirebaseFunctions.getInstance()
}
```

- [ ] **Step 4 — Write failing ViewModel test for join**

In `HouseholdOnboardingViewModelTest.kt`, add the gateway to the constructor and test:

```kotlin
private val joinGateway: JoinHouseholdGateway = mockk(relaxed = true)
// recreate vm with all three deps

@Test
fun `join flow navigates to home on success`() = runTest {
    coEvery { joinGateway.joinByCode("ABCDEF") } returns Result.success("h-2")
    vm.switchMode(HouseholdOnboardingUiState.Mode.Join)
    vm.onInviteCodeChange("ABCDEF")
    vm.submitJoin()
    assertTrue(vm.uiState.value.navigateToHome)
}

@Test
fun `join with unknown code shows inline error`() = runTest {
    coEvery { joinGateway.joinByCode("XXXXXX") } returns Result.failure(JoinHouseholdError.NotFound)
    vm.switchMode(HouseholdOnboardingUiState.Mode.Join)
    vm.onInviteCodeChange("XXXXXX")
    vm.submitJoin()
    assertEquals("No household found for that code.", vm.uiState.value.inviteError)
}
```

(Update the `@BeforeEach` to construct `HouseholdOnboardingViewModel(authRepo, householdRepo, joinGateway)`.)

- [ ] **Step 5 — Run; verify FAIL**

```bash
./gradlew :app:test --tests "app.pantry.ui.household.HouseholdOnboardingViewModelTest"
```

Expected: FAIL — `submitJoin` is a stub.

- [ ] **Step 6 — Wire `submitJoin`**

In `HouseholdOnboardingViewModel`, replace the constructor and method:

```kotlin
@HiltViewModel
class HouseholdOnboardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val joinGateway: JoinHouseholdGateway,
) : ViewModel() {

    // ...existing code...

    fun submitJoin() {
        val s = _state.value
        if (!s.canSubmitJoin) return
        _state.update { it.copy(isSubmitting = true, inviteError = null, toast = null) }
        viewModelScope.launch {
            joinGateway.joinByCode(s.inviteCode).fold(
                onSuccess = { _state.update { it.copy(isSubmitting = false, navigateToHome = true) } },
                onFailure = { e ->
                    _state.update {
                        when (e) {
                            JoinHouseholdError.NotFound -> it.copy(isSubmitting = false, inviteError = "No household found for that code.")
                            JoinHouseholdError.AlreadyMember -> it.copy(isSubmitting = false, inviteError = "You're already in this household.")
                            JoinHouseholdError.NoNetwork -> it.copy(isSubmitting = false, toast = "No internet connection")
                            else -> it.copy(isSubmitting = false, toast = e.message ?: "Failed to join household")
                        }
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 7 — Run the tests; verify PASS**

```bash
./gradlew :app:test --tests "app.pantry.ui.household.HouseholdOnboardingViewModelTest"
```

Expected: PASS.

- [ ] **Step 8 — Add the Join UI in the screen**

In `HouseholdOnboardingScreen.kt`, replace the `Mode.Join` branch:

```kotlin
HouseholdOnboardingUiState.Mode.Join -> {
    OutlinedTextField(
        value = state.inviteCode,
        onValueChange = viewModel::onInviteCodeChange,
        label = { Text("Invite code") },
        isError = state.inviteError != null,
        supportingText = { state.inviteError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("join_code"),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            letterSpacing = androidx.compose.ui.unit.sp(6).value.let { androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp) },
        ),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = viewModel::submitJoin,
        enabled = state.canSubmitJoin,
        modifier = Modifier.fillMaxWidth().testTag("join_submit"),
    ) { Text(if (state.isSubmitting) "Joining…" else "Join") }
}
```

- [ ] **Step 9 — Commit**

```bash
git add .
git commit -m "feat(household): add join-with-invite-code flow"
```

---

## US-12: Cloud Function `joinHousehold`

**As a** backend
**I want** a callable Cloud Function that atomically adds the caller to a household given its invite code
**So that** household membership stays consistent without exposing `memberUids` writes to clients.

### Acceptance criteria

- Function lives at `functions/src/joinHousehold.ts`, exported from `functions/src/index.ts`
- Validates: `code` is a 6-character string; caller is authenticated
- Looks up the unique household by `inviteCode`; throws `not-found` if absent
- Throws `already-exists` if caller is already in `memberUids`
- Otherwise transactionally adds the caller to `memberUids` and adds the household id to the caller's `users/{uid}.households` array
- Returns `{ householdId: string }`
- A Jest test runs against the Firebase emulator and exercises all branches

### Files

- Create: `pantry/functions/src/joinHousehold.ts`
- Modify: `pantry/functions/src/index.ts`
- Create: `pantry/functions/test/joinHousehold.test.ts`
- Modify: `pantry/functions/package.json` (add Jest)
- Modify: `pantry/functions/tsconfig.json` (include test config)

### Tasks

- [ ] **Step 1 — Implement the function**

Create `pantry/functions/src/joinHousehold.ts`:

```typescript
import * as functions from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) admin.initializeApp();
const db = admin.firestore();

interface Payload { code?: string }

export const joinHousehold = onCall<Payload>({ region: "europe-west1" }, async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required");

    const code = (request.data.code ?? "").toString().toUpperCase().trim();
    if (code.length !== 6) throw new HttpsError("invalid-argument", "Invite code must be 6 characters");

    const matches = await db.collection("households").where("inviteCode", "==", code).limit(1).get();
    if (matches.empty) throw new HttpsError("not-found", "No household for that code");
    const householdDoc = matches.docs[0];
    const memberUids: string[] = householdDoc.get("memberUids") ?? [];
    if (memberUids.includes(uid)) throw new HttpsError("already-exists", "Already a member");

    await db.runTransaction(async (tx) => {
        tx.update(householdDoc.ref, { memberUids: admin.firestore.FieldValue.arrayUnion(uid) });
        tx.set(
            db.collection("users").doc(uid),
            { households: admin.firestore.FieldValue.arrayUnion(householdDoc.id) },
            { merge: true },
        );
    });

    return { householdId: householdDoc.id };
});
```

Modify `pantry/functions/src/index.ts`:

```typescript
export { joinHousehold } from "./joinHousehold";
```

- [ ] **Step 2 — Install Jest and Firebase rules unit testing**

```bash
cd pantry/functions
npm install --save-dev jest ts-jest @types/jest firebase-functions-test
```

Add to `pantry/functions/package.json` `scripts`:

```json
"test": "jest",
"build": "tsc",
"lint": "eslint --ext .ts src/"
```

And `jest` config (`jest.config.js`):

```javascript
module.exports = {
    preset: "ts-jest",
    testEnvironment: "node",
    testMatch: ["**/test/**/*.test.ts"],
};
```

- [ ] **Step 3 — Write the test**

Create `pantry/functions/test/joinHousehold.test.ts`:

```typescript
import * as admin from "firebase-admin";
import { joinHousehold } from "../src/joinHousehold";

const PROJECT_ID = "pantry-dev";
process.env.GCLOUD_PROJECT = PROJECT_ID;
process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";

if (admin.apps.length === 0) admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();

function callable(impl: any) {
    // helper to invoke the v2 callable handler directly (firebase-functions-test v3 style)
    return async (data: any, auth?: { uid: string }) => {
        return impl.run({ data, auth });
    };
}

describe("joinHousehold", () => {
    const callJoin = callable(joinHousehold);

    beforeEach(async () => {
        // wipe firestore
        const cols = await db.listCollections();
        await Promise.all(cols.map(c => deleteCollection(c)));
    });

    test("rejects unauthenticated callers", async () => {
        await expect(callJoin({ code: "ABCDEF" })).rejects.toMatchObject({ code: "unauthenticated" });
    });

    test("rejects invalid code length", async () => {
        await expect(callJoin({ code: "ABC" }, { uid: "u-1" })).rejects.toMatchObject({ code: "invalid-argument" });
    });

    test("returns not-found when code does not match", async () => {
        await expect(callJoin({ code: "AAAAAA" }, { uid: "u-1" })).rejects.toMatchObject({ code: "not-found" });
    });

    test("adds the user to the household on first join", async () => {
        const ref = await db.collection("households").add({ name: "Casa", inviteCode: "ABCDEF", memberUids: ["u-1"] });
        const result = await callJoin({ code: "ABCDEF" }, { uid: "u-2" });
        expect(result).toEqual({ householdId: ref.id });
        const fresh = await ref.get();
        expect(fresh.get("memberUids")).toContain("u-2");
        const user = await db.collection("users").doc("u-2").get();
        expect(user.get("households")).toContain(ref.id);
    });

    test("returns already-exists if already a member", async () => {
        await db.collection("households").add({ name: "Casa", inviteCode: "ABCDEF", memberUids: ["u-1"] });
        await expect(callJoin({ code: "ABCDEF" }, { uid: "u-1" })).rejects.toMatchObject({ code: "already-exists" });
    });
});

async function deleteCollection(col: FirebaseFirestore.CollectionReference) {
    const docs = await col.listDocuments();
    await Promise.all(docs.map(d => d.delete()));
}
```

- [ ] **Step 4 — Run the Function test**

In one terminal:

```bash
firebase emulators:start --only firestore
```

In another:

```bash
cd pantry/functions
npm test
```

Expected: 5 tests pass.

- [ ] **Step 5 — Deploy to production**

```bash
cd pantry
firebase deploy --only functions:joinHousehold
```

Expected: function deployed; URL appears in the Firebase console under "Functions".

- [ ] **Step 6 — End-to-end check**

Run the Android app, sign in on Device A, create a household → copy invite code from Firestore emulator UI. On Device B (or a second emulator), sign up with a different email → tap "Join with invite code" → paste code → land on Home. Verify in Firestore UI that `memberUids` on the household now includes Device B's uid.

- [ ] **Step 7 — Commit**

```bash
git add .
git commit -m "feat(functions): add joinHousehold callable Cloud Function"
```

---

## US-13: Sign out

**As a** signed-in user
**I want** to sign out from a Settings entry
**So that** I can hand the phone to someone else without exposing my data.

### Acceptance criteria

- The Home placeholder screen has a "Sign out" button (this becomes the Settings entry in Phase 4)
- Tapping it calls `AuthRepository.signOut` and clears the navigation back to the Auth route
- After sign-out, re-launching the app starts at Auth (no auto-login because `currentUser` is null)
- A Compose Robolectric test confirms the button calls the ViewModel's `signOut`

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/home/HomePlaceholderScreen.kt`
- Create: `pantry/app/src/main/kotlin/app/pantry/ui/home/HomeViewModel.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/ui/nav/PantryNavHost.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/home/HomePlaceholderScreenTest.kt`

### Tasks

- [ ] **Step 1 — Implement `HomeViewModel`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/home/HomeViewModel.kt`:

```kotlin
package app.pantry.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut = _signedOut.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _signedOut.value = true
        }
    }
}
```

- [ ] **Step 2 — Implement the placeholder screen**

Create `pantry/app/src/main/kotlin/app/pantry/ui/home/HomePlaceholderScreen.kt`:

```kotlin
package app.pantry.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomePlaceholderScreen(
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val signedOut by viewModel.signedOut.collectAsState()
    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Coming next: stock catalog", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::signOut, modifier = Modifier.testTag("signout")) {
            Text("Sign out")
        }
    }
}
```

- [ ] **Step 3 — Wire into NavHost**

In `PantryNavHost.kt`, replace the `Home` route:

```kotlin
composable(PantryRoute.Home.path) {
    HomePlaceholderScreen(
        onSignedOut = {
            navController.navigate(PantryRoute.Auth.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

- [ ] **Step 4 — Compose test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/home/HomePlaceholderScreenTest.kt`:

```kotlin
package app.pantry.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.pantry.data.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomePlaceholderScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sign_out_button_calls_signout_and_navigates() {
        val repo: AuthRepository = mockk(relaxed = true) {
            every { currentUser } returns MutableStateFlow(null)
        }
        coEvery { repo.signOut() } returns Result.success(Unit)

        var signedOut = false
        composeRule.setContent {
            HomePlaceholderScreen(
                onSignedOut = { signedOut = true },
                viewModel = HomeViewModel(repo),
            )
        }
        composeRule.onNodeWithTag("signout").performClick()
        composeRule.waitUntil(2_000) { signedOut }
    }
}
```

- [ ] **Step 5 — Run tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 6 — Commit**

```bash
git add .
git commit -m "feat(home): add placeholder home and sign-out"
```

---

## US-14: First-launch routing & placeholder home

**As a** developer
**I want** the app to route correctly on every launch based on auth + household state
**So that** returning users skip past the auth screen.

### Acceptance criteria

- On cold launch, the app checks: no current user → `auth`; user exists but no household → `household`; user has at least one household → `home`
- Routing logic lives in a single place (a `StartRouter` composable / function) — not duplicated across ViewModels
- A Compose Robolectric test covers all three branches with fake repositories
- The transition from `auth` → `household` → `home` is smooth (no flash of the welcome screen between sign-in and household onboarding)

### Files

- Create: `pantry/app/src/main/kotlin/app/pantry/ui/nav/StartRouter.kt`
- Modify: `pantry/app/src/main/kotlin/app/pantry/MainActivity.kt`
- Create: `pantry/app/src/test/kotlin/app/pantry/ui/nav/StartRouterTest.kt`

### Tasks

- [ ] **Step 1 — Implement `StartRouter`**

Create `pantry/app/src/main/kotlin/app/pantry/ui/nav/StartRouter.kt`:

```kotlin
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
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModel
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

enum class StartRoute(val path: String) { Loading("loading"), Auth("auth"), Household("household"), Home("home") }

@HiltViewModel
class StartRouterViewModel @Inject constructor(
    auth: AuthRepository,
    households: HouseholdRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val initialRoute: StateFlow<StartRoute> = auth.currentUser
        .flatMapLatest { profile ->
            if (profile == null) flowOf(StartRoute.Auth)
            else households.observeUserHouseholds(profile.uid).let { stream ->
                kotlinx.coroutines.flow.map(stream) { list ->
                    if (list.isEmpty()) StartRoute.Household else StartRoute.Home
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartRoute.Loading)
}

@Composable
fun StartRouter(viewModel: StartRouterViewModel = hiltViewModel()) {
    val route by viewModel.initialRoute.collectAsState()
    when (route) {
        StartRoute.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else -> PantryNavHost(navController = rememberNavController(), startDestination = route.path)
    }
}
```

Update `PantryNavHost` signature:

```kotlin
@Composable
fun PantryNavHost(navController: NavHostController, startDestination: String = PantryRoute.Auth.path) {
    NavHost(navController = navController, startDestination = startDestination) {
        // composable definitions unchanged
    }
}
```

- [ ] **Step 2 — Update `MainActivity`**

```kotlin
setContent {
    PantryTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            app.pantry.ui.nav.StartRouter()
        }
    }
}
```

- [ ] **Step 3 — Write the routing test**

Create `pantry/app/src/test/kotlin/app/pantry/ui/nav/StartRouterTest.kt`:

```kotlin
package app.pantry.ui.nav

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.domain.model.Household
import app.pantry.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class StartRouterTest {

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `routes to Auth when no user`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(null) }
        val households: HouseholdRepository = mockk()
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            // first emission may be Loading then Auth; drain until non-Loading
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Auth, route)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routes to Household when user has no households`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk { every { observeUserHouseholds("u-1") } returns flowOf(emptyList()) }
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Household, route)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `routes to Home when user has a household`() = runTest {
        val auth: AuthRepository = mockk { every { currentUser } returns MutableStateFlow(UserProfile("u-1", "A", "a@b.com")) }
        val households: HouseholdRepository = mockk {
            every { observeUserHouseholds("u-1") } returns flowOf(listOf(Household("h-1", "Casa", listOf("u-1"), "ABCDEF")))
        }
        val vm = StartRouterViewModel(auth, households)
        vm.initialRoute.test {
            var route = awaitItem()
            while (route == StartRoute.Loading) route = awaitItem()
            assertEquals(StartRoute.Home, route)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 4 — Run all tests; verify PASS**

```bash
./gradlew :app:test
```

Expected: PASS.

- [ ] **Step 5 — Manual end-to-end on emulator**

1. Sign up with a new email → land on Household (since no households)
2. Create "Casa" → land on Home placeholder
3. Force-stop the app, reopen → directly on Home placeholder (auto-routed)
4. Sign out → on Auth
5. Sign back in → directly on Home

- [ ] **Step 6 — Commit & tag**

```bash
git add .
git commit -m "feat(nav): add StartRouter for first-launch routing"
git tag phase-1-complete
```

---

## Phase 1 done

At this point:

- Sign in/up with email + password and Google works
- Forgot password works
- A user can create or join a household
- A signed-in user with a household lands on the placeholder home
- Sign-out returns to auth
- All tests pass; `joinHousehold` Cloud Function is deployed

**Next:** write the Phase 2 plan (`pantry/spec/phase-2-stock-catalog.md`) covering the Stock screen.

