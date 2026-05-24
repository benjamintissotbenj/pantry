# Play Console submission checklist for Pantry v1.0.0

A 10-step walkthrough. Have these files / tabs ready before you start:

- `play-store/store-listing.md` — copy fields from here
- `play-store/icon-512.png` — hi-res app icon
- `play-store/feature-graphic.png` — banner
- `play-store/screenshots/` — 6 phone screenshots (PNG, ~1080×2400 each)
- `PRIVACY.md` — your repo URL pointing here
- `app/build/outputs/bundle/release/app-release.aab` — produced by
  `./gradlew :app:bundleRelease` (US-14)

## Pre-submission deploy

Before uploading the AAB, deploy the tightened Firestore rules to your
Firebase project. Production users will land on these:

```
firebase deploy --only firestore:rules
```

Verify on the device (still on the dev build) that:
- Settings → rename household → still works
- Settings → regenerate code → still works
- Stock → add/edit/delete item → still works
- Shopping → finish shopping → still works (the items subcollection rule
  is unchanged; only the household doc tightened)

If any of these fail after deploy, ROLL BACK rules to the previous version
via Firebase Console → Firestore → Rules → version history.

## Step 1 — Register Play Console account

1. Open https://play.google.com/console.
2. Sign in with your Google account.
3. Accept the developer agreement.
4. Pay the one-time $25 USD registration fee.
5. Fill in developer profile (your name, contact email, website if any).

Expected time: 10 min. The account is approved instantly.

## Step 2 — Create new app

In Play Console → "All apps" → "Create app":

- **App name**: `Pantry` (or fallback from store-listing.md)
- **Default language**: English (United States)
- **App or game**: App
- **Free or paid**: Free
- **Declarations**:
  - [x] Developer Program Policies
  - [x] US export laws

Click "Create app". You'll land on the app's dashboard.

## Step 3 — Create production release (upload AAB)

In the left nav: "Release" → "Production" → "Create new release".

1. **App Bundles**: click "Upload" and select
   `app/build/outputs/bundle/release/app-release.aab`.
2. **Play App Signing**: Play will prompt to enroll. Accept. This is the
   modern default — you keep the upload key, Google manages the
   distribution key.
3. **Release name**: leave as auto-generated `1.0.0 (1)`.
4. **Release notes** (in `<en-US>` block):
   ```
   Initial public release.
   ```

Don't click "Next" yet — first finish all the App content sections
(Step 4–7). Save the release as a draft.

## Step 4 — App content sections

In the left nav: "Policy" → "App content". Fill each section:

### Privacy policy

URL (paste exactly):
```
https://github.com/benjamintissotbenj/pantry/blob/main/PRIVACY.md
```

### Ads

Answer: "No, my app does not contain ads".

### App access

Answer: "All functionality is available without any special access".

### Content rating

Start questionnaire. Answers from `store-listing.md`:
- Category: select "Utility / Productivity / Lifestyle" (whichever Play
  offers — closest to our actual)
- Violence: None
- Sexual content: None
- Profanity: None
- Controlled substances: None
- Gambling: None
- User-generated content: Limited (free-text item names; no public posts)
- Location: No
- Personal info: Yes — email and name shared with household members
- Web access: No
- Digital purchases: No

Submit. Expect Everyone / 3+ rating.

### Target audience and content

- Target age groups: select "18+" AND "13-17" (Firebase Auth requires 13+).
- Designed for Families: No.
- Appeals to children: No.

### News app

No.

### Government app

No.

### Data safety

Paste each answer from `store-listing.md` § Data safety form. The form
is long — work through each subsection.

Key answers:
- Data is collected: Yes (name, email, app interactions, device IDs)
- Data is shared with third parties: Yes (Google/Firebase)
- All data encrypted in transit: Yes
- Users can request data deletion: Yes (via leaving households + contact)

## Step 5 — Store listing

In the left nav: "Grow" → "Store listing". Fill from `store-listing.md`:

- **App name**: `Pantry`
- **Short description**: paste the short description from `store-listing.md`
- **Full description**: paste the full description from `store-listing.md`

### Graphics

- **App icon**: upload `play-store/icon-512.png` (512×512 PNG)
- **Feature graphic**: upload `play-store/feature-graphic.png` (1024×500)
- **Phone screenshots**: upload all 6 from `play-store/screenshots/`
  (`01-auth.png` first; order matters — first shown to browsers)

### Categorization

- **App category**: Lifestyle
- **Tags**: pantry, household, shopping list, stock, kitchen (max 5)

### Contact details

- **Email**: `benjamin.tisso@live.fr` (matches the address in PRIVACY.md)
- **Phone**: optional, leave blank if you don't want it public
- **Website**: `https://github.com/benjamintissotbenj/pantry`

Save.

## Step 6 — Production release finalize

Back to "Release" → "Production" → the draft you created in Step 3:

1. Verify "Country / region" includes the countries you want to ship to
   (default: all). For a v1 internal launch you may want to limit to
   your country first, then expand. Up to you.
2. Set **Staged rollout** to `5%` (you can ramp this up to 100% later
   from the same screen).
3. Click "Next".
4. Review the summary — Play warns about any missing fields or policy
   issues.
5. Click "Start rollout to production".

## Step 7 — Review submission

Play submits the release for human review. First-time review typically
takes 3–10 days. You'll get an email confirmation.

During review:
- Don't change anything in Play Console — wait for the verdict.
- If approved: the app appears on Play with the 5% staged rollout.
- If rejected: read the rejection reason carefully (usually a content
  rating mismatch or a data safety issue), fix it, resubmit. The next
  review is usually faster (1–3 days).

## Step 8 — Post-launch

Once live:

1. **Test install from Play**: search "Pantry" on your phone's Play
   Store, install. Verify the launcher icon shows, splash screen shows,
   sign-in flow works against the Firebase prod (i.e., the same dev
   project), Crashlytics dashboard shows your install registered.
2. **Watch Crashlytics**: any crash in the first 24h is high priority.
3. **Ramp rollout**: if no crashes / negative feedback in 24-48h, bump
   the rollout to 25% → 50% → 100% across the next few days.

## Step 9 — Issue tracking

- Crash reports: Firebase Console → Crashlytics (under "Quality").
- User reviews: Play Console → Quality → User feedback.
- Sales: Play Console → Statistics (despite the free price, you'll see
  install counts here).

## Step 10 — v1.0.1 release flow (for future reference)

When you ship a patch:

1. Bump `versionCode = 2` and `versionName = "1.0.1"` in
   `app/build.gradle.kts`.
2. Commit + push.
3. Run `./gradlew :app:bundleRelease` — produces a new AAB.
4. Play Console → Release → Production → Create new release → Upload
   new AAB → release notes ("Bug fixes.") → 5% staged rollout → Start.
5. Review is typically <24h for updates (no re-check of policy unless
   you changed the data safety form).

If you also changed PRIVACY.md, the data safety form, or the description
in any meaningful way — those go through their normal review process.

## Troubleshooting

- **"Your APK was signed with a debug key"**: your `keystore.properties`
  is missing or pointing at the wrong file. Verify the path; rerun
  `bundleRelease`.
- **"App is not optimized for tablets"**: Pantry is phone-first; this is
  a non-blocking warning. Accept it.
- **"You haven't provided a privacy policy"**: paste the GitHub URL
  again — Play occasionally fails to fetch the URL initially. Wait
  5 min, refresh, paste it back.
- **"App was rejected: content rating mismatch"**: re-run the
  questionnaire and ensure your answers match the actual app behavior.
- **Crashlytics shows no crashes in 24h**: that's good — but also verify
  the dashboard is wired by triggering a deliberate test crash via
  `FirebaseCrashlytics.getInstance().sendUnsentReports()` after a
  forced crash (only do this on the dev build).
