# Pantry

Your household's shared kitchen — track stock and shopping in real time.

![Pantry](play-store/feature-graphic.png)

## Features

- Stock catalog with categories, low-stock alerts, and an optimistic +/- stepper
- Shopping list that combines auto-derived low-stock items with free-text manual entries
- Multi-member households with denormalized member lookups
- Real-time Firestore sync; offline writes are queued automatically
- Material 3 dark green theme; supports both light and dark mode
- Settings: household rename, invite code copy/share/regenerate, creator-only member
  removal, category batch rename, leave household
- Firebase Crashlytics for production error reporting

## Tech Stack

- **Kotlin 2.1** — language
- **Jetpack Compose** — declarative UI
- **Material 3** — design system
- **Hilt 2.53** — dependency injection
- **Firebase BoM 33.7.0** — Auth, Firestore, Functions, Crashlytics
- **JUnit 5 + Robolectric + MockK + Turbine** — unit and Compose UI testing
- **Gradle version catalog** — dependency management

## Build & Install

```bash
git clone https://github.com/benjamintissotbenj/pantry
cd pantry

# 1) Drop your Firebase config:
cp app/google-services.json.sample app/google-services.json
# then fill REPLACE_ME values from your own Firebase project

# 2) For debug builds:
./gradlew :app:installDebug

# 3) For release AAB (requires a signing keystore — see keystore.properties.sample):
cp keystore.properties.sample keystore.properties
# then fill in your keystore details
./gradlew :app:bundleRelease
```

> **Note**: A `google-services.json` connected to a real Firebase project is required for
> the app to run. Create a project at <https://console.firebase.google.com/>, enable
> Authentication (Email/Password), Firestore, and Crashlytics, then download the config file.

## Running Tests

```bash
# Fast unit + Compose UI (Robolectric) tests — no emulator required
./gradlew :app:testDebugUnitTest

# Full suite including emulator integration tests
firebase emulators:exec --only firestore,auth \
  "./gradlew :app:testDebugUnitTest -PincludeEmulatorTests"
```

## Phase Plans

The `spec/` directory contains the implementation plans written before each phase of
development. They document the design decisions and user stories behind the app.

| Phase | File | Topic |
|-------|------|-------|
| 3 | [spec/phase-3-shopping-list.md](spec/phase-3-shopping-list.md) | Shopping list |
| 4 | [spec/phase-4-settings.md](spec/phase-4-settings.md) | Settings + connectivity |
| 5 | [spec/phase-5-release-to-play.md](spec/phase-5-release-to-play.md) | Release to Play Store |

Phases 1 (Bootstrap & Auth) and 2 (Stock catalog) were planned before the repository was
initialised; their design intent is reflected in the codebase itself.

## Privacy Policy

See [PRIVACY.md](PRIVACY.md).

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
