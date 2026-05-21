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
