# Pantry — Play Store listing reference

Paste fields from here into Play Console as you fill out the store
listing and the various declarations.

## App name

**Primary**: `Pantry`
**Fallback if taken**: `Pantry — Shared Kitchen`
(Check availability on Play first; if "Pantry" is taken, use the fallback.)

## Short description (max 80 characters)

```
Track your household's stock and shopping list together, in real time.
```
(72 chars — leaves headroom.)

## Full description (max 4000 characters)

```
Pantry is the open-source app for households that want to share their
kitchen.

Stop arguing about who used the last roll of toilet paper. Pantry lets
everyone in the house track stock and shopping together — in real time,
from any phone signed in to the household.

# Features

• Stock catalog. Add what you keep in the house. Each item has a
  quantity, a unit (count, grams, liters), a low-stock threshold, and
  an optional default restock quantity. Step the count up or down with
  one tap as you use things.

• Shopping list that builds itself. Items below their threshold auto-
  appear under "Running low". You can also add free-text entries
  ("Wine for guests"). When you check items off in the store and tap
  Finish shopping, the catalog updates and the manual entries clear.

• Multi-member households. Invite anyone with a 6-character code.
  Names and emails of all members appear in Settings. The creator
  can remove members; everyone can leave on their own.

• Categories with batch rename. Group items by category (Dairy,
  Cleaning, …). Renaming a category atomically moves every item.

• Real-time everywhere. Firebase Firestore keeps every device in
  sync the second something changes. Works offline (changes queue
  and apply when you reconnect).

• Material 3 dark green theme. Clean Compose UI, supports both
  light and dark mode, no dynamic Material You override.

• Privacy by design. We collect only what's needed to make the app
  work (email + display name + the items you create). No analytics,
  no ads, no third-party SDKs other than Firebase. Crash reports via
  Crashlytics carry no PII.

• Open source. The whole codebase is at github.com/<USER>/<REPO>.
  Read the source, file issues, send PRs.

# Who it's for

Couples, roommates, families — anyone who shares a kitchen with at
least one other person. If you've ever bought a third bottle of
soy sauce because nobody knew there were already two, this is for you.

# Free, ad-free, no premium tier

Pantry is free to use. There is no paid tier, no ads, no in-app
purchases. The backend runs on Firebase's free + low-cost tier.

# Open source

Source code, design docs (phase-by-phase development history),
and contribution guide all at github.com/<USER>/<REPO>.

# Privacy

See the in-repo privacy policy:
github.com/<USER>/<REPO>/blob/main/PRIVACY.md
```

(~2300 chars — well under the 4000 limit, leaves room for future feature additions.)

## Category

**Primary category**: `Lifestyle`

(More discoverable than "Food & Drink" — household management apps cluster
under Lifestyle.)

## Tags (up to 5)

`pantry`, `household`, `shopping list`, `stock`, `kitchen`

## App access declaration

> All app functionality is available without any restricted access.

(Translation for the form: no special test credentials needed; the Play
reviewer will sign in via Google or create an email account themselves.)

## Ads declaration

> No, my app does not contain ads.

## Content rating questionnaire (IARC)

For each question, the answer:

- Violence: **None**
- Sexual content: **None**
- Profanity: **None**
- Controlled substances: **None**
- Gambling: **None**
- User-generated content: **Yes, limited** (the household chat-like ability
  to type item names. Free-text fields where users type product names.
  No public sharing — content is only visible to household members.)
- Location sharing: **No**
- Personal information sharing: **Yes — email and display name shared
  among household members** (limited to households the user joins)
- Web access: **No**
- Digital purchases: **No**

**Expected rating**: Everyone / 3+ (IARC tier 1)

## Target audience and content

- Target age groups: **Adults (18+) and 13+** (Firebase Auth requires 13+
  per COPPA).
- Designed for Families program: **No**.

## Data safety form

Pantry collects the following data:

### Personal info

- **Name (display name)**: collected, shared with household members.
  Purpose: app functionality. Optional? No (required at sign-up).
- **Email**: collected, shared with household members.
  Purpose: app functionality (authentication, member list).
  Optional? No.

### App activity

- **App interactions (Crashlytics)**: collected.
  Purpose: analytics + crash reporting.
  Optional? No, but anonymous (device-level identifiers only).

### Device or other IDs

- **Device IDs (for Crashlytics)**: collected.
  Purpose: analytics + crash reporting.
  Optional? No.

### Encryption in transit

> Yes, all data is encrypted in transit (HTTPS/TLS via Firebase SDKs).

### Data deletion

> Users can request their data be deleted by leaving all households via
> Settings → Leave household, then deleting their Firebase Auth account
> (currently by contacting the developer at <CONTACT EMAIL>; in-app
> account deletion is planned for v2).

### Data shared with third parties

- **Google (Firebase)**: yes, all data passes through Firebase services
  (Auth, Firestore, Cloud Functions, Crashlytics). Google's privacy policy
  applies to this transit and storage.

No other third parties.

## Government app declaration

> No, this is not a government app.

## News app declaration

> No, this is not a news app.

## Privacy policy URL

(Fill in after the repo is public:)

```
https://github.com/<USER>/<REPO>/blob/main/PRIVACY.md
```

## Notes

- Replace `<USER>` and `<REPO>` everywhere with the actual GitHub username
  and repo name once you push.
- The `<CONTACT EMAIL>` placeholder should match the one in PRIVACY.md.
- Update this file if your app description, category, or data collection
  changes between releases. Play requires consistency between this file's
  content and the live Play listing.
