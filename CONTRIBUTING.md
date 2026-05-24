# Contributing to Pantry

Thank you for your interest in contributing. These guidelines keep the codebase
consistent and make reviews easier for everyone.

## Branching

| Prefix | When to use |
|--------|-------------|
| `feat/<scope>` | New user-visible functionality |
| `fix/<scope>` | Bug fixes |
| `chore/<scope>` | Build, tooling, dependency updates |
| `docs/<scope>` | Documentation-only changes |

Long-lived branches follow the pattern `phase-N-<description>` and are merged into
`main` after the corresponding `phase-N-complete` tag is created.

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
feat(stock): add low-stock badge to item row
fix(auth): handle sign-in timeout on slow connections
chore(release): bump versionCode to 2
docs(readme): update build instructions
```

Commonly used scopes: `model`, `stock`, `shopping`, `settings`, `auth`, `household`,
`connectivity`, `functions`, `theme`, `brand`, `release`, `ui`, `security`.

Breaking changes must include a `BREAKING CHANGE:` footer in the commit body.

## Testing

The full local test suite must be green before opening a PR:

```bash
./gradlew :app:testDebugUnitTest
```

Compose + Robolectric tests run without a device or emulator. Tests annotated with
`@EmulatorTest` (or gated on `-PincludeEmulatorTests`) are skipped unless you start
the Firebase Local Emulator Suite:

```bash
firebase emulators:start --only firestore,auth,functions
```

Write tests for new business logic in `app/src/test/`. Compose UI tests go in the
same directory and use the Robolectric runner — no emulator required.

## Architecture

Follow the existing layering:

- **domain** — plain Kotlin models and repository interfaces
- **data** — Firebase/Firestore implementations of those interfaces
- **ui** — Compose screens and ViewModels

State flows downward via `StateFlow`; events flow upward via sealed classes.
ViewModels expose a single `UiState` and a single `onEvent(UiEvent)` entry point
(Unidirectional Data Flow).

Dependency injection is handled entirely by Hilt. Firebase is the only backend;
there is no REST API.

## Code Review

Benjamin reviews all PRs. For non-trivial changes, please open an issue or start a
design discussion before writing code — it prevents wasted effort and keeps the
architecture coherent.

When your PR is ready:

1. Ensure `./gradlew :app:testDebugUnitTest` passes locally.
2. Rebase onto `main` (or the relevant phase branch) and resolve any conflicts.
3. Fill in the PR description with a summary of what changed and why.
4. Request a review from `@benjamintissot`.
