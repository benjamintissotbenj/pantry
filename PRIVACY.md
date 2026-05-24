# Pantry Privacy Policy

_Last updated: 2026-05-24_

---

## Introduction

Pantry is a household stock and shopping app that lets you track items and collaborate
with the people you live with. This policy describes what data Pantry collects, why it
collects it, who it is shared with, and what rights you have over your own data.

The entire Pantry application — Android app, Cloud Functions, and Firestore security rules —
is open source. Everything stated in this policy can be verified against the source code in
this repository. There is no hidden telemetry.

---

## What We Collect

### 1. Account Data (Firebase Authentication)

When you sign in — either with Google or with an email address and password — Firebase Auth
creates an account that stores:

- Your **email address**
- Your **display name** (taken from your Google profile, or the name you enter when you register
  with email/password)

Pantry itself reads your email and display name from the Firebase Auth token to populate your
profile inside the app. No other identity information is collected.

### 2. Stock and Shopping Data (Firestore)

When you use Pantry, the content you create is stored in Cloud Firestore under a household
document (`households/{householdId}`). Specifically:

**Stock items** — stored in the `households/{hid}/items` subcollection. Each item document
contains:

- Name (free text you type)
- Category (selected from a predefined list)
- Current quantity and unit
- Low-stock threshold quantity
- Optional default restock quantity
- A timestamp of when the item was last updated (`updatedAt`)

**Shopping list entries** — stored in the `households/{hid}/shoppingList` subcollection.
Each entry contains:

- A free-text label (e.g. "Milk — 2 litres")
- An optional link to the corresponding stock item in the `items` subcollection
- A `checked` flag indicating whether you have ticked it off

This data is yours. It is only used to power the app's functionality. It is not analysed,
profiled, or sold.

### 3. Members Map (Household Document)

When you join a household, your **display name** and **email address** are written into the
`members` map on the household document. This is a deliberate denormalisation: it lets the
Settings screen display a list of household members without the app having to make a
separate read to Firebase Auth for each member.

The trade-off is that if you change your display name later, the copy stored in the `members`
map is not updated automatically — it reflects the name you had at the time you joined. The
updated name takes effect in the members map only if you leave the household and rejoin.

This is a known v1 limitation. The `members` map holds no data beyond display name and email.

### 4. Crash Reports (Firebase Crashlytics)

If the app crashes, Firebase Crashlytics automatically sends a crash report. Each report
contains:

- Device model (e.g. "Pixel 8")
- Android OS version
- Pantry app version
- Device locale (e.g. "en-GB")
- The stack trace of the crash

Crash reports do **not** contain your email address, display name, household ID, item names,
shopping list content, or any other data you have entered in the app. The reports are used
solely to identify and fix bugs.

---

## What We Don't Collect

Pantry does not collect, request, or store any of the following:

- Location data (GPS or network-based)
- Contacts from your address book
- Photos or camera output
- Microphone or audio
- Biometric data of any kind
- Financial data or payment information
- Health or fitness data
- Advertising identifiers (GAID / IDFA)
- SMS messages or call logs
- Clipboard contents
- Browser history or cross-app activity

The app requests no sensitive Android permissions beyond internet access.

---

## Who We Share Data With

Pantry shares data only with **Google**, via the following Firebase services:

### Firebase Authentication (Google LLC)

Manages sign-in. Your email and display name are stored in Firebase Auth. Google's Firebase
Auth data is processed under Google's standard terms of service. Firebase Auth stores data
in the US by default, but the tokens that Pantry uses are resolved from the nearest Google
edge.

### Cloud Firestore (Google LLC)

Stores all household data (stock items, shopping list, members map) described above. Firestore
is configured with security rules that restrict reads and writes to authenticated members of
the relevant household. Only members of your household can read or write your household's data.

### Cloud Functions for Firebase (Google LLC)

Backend logic — joining a household, leaving a household, and household deletion — runs as
Cloud Functions deployed to the **europe-west1** region (Belgium). These functions access
Firestore and Firebase Auth on your behalf, under the same data restrictions described above.

### Firebase Crashlytics (Google LLC)

Receives crash reports as described in the "What We Collect — Crash Reports" section above.

**There are no advertising networks, marketing platforms, analytics providers, or other
third-party SDKs in Pantry.** The only external dependency is Firebase (Google).

---

## Where Data Is Stored

- **Cloud Functions** run in Google's **europe-west1** region (Belgium).
- **Cloud Firestore** for this project is provisioned in Google's **eur3** multi-region
  (which covers European data centres). Your household data does not leave the European
  Economic Area.
- **Firebase Authentication** and **Crashlytics** are global Firebase services operated by
  Google LLC under their standard infrastructure.

If you require more detail about Google's data centre locations, refer to
[Google Cloud's infrastructure documentation](https://cloud.google.com/about/locations).

---

## How Long We Keep Your Data

**Account data** (email, display name) is kept in Firebase Auth for as long as your Firebase
Auth account exists.

**Household data** (stock items, shopping list, members map) is kept for as long as at least
one member remains in the household.

When you leave a household via **Settings → Leave household**:

- Your entry is removed from the `memberUids` array and the `members` map on the household
  document immediately.
- If you were the **last member** of the household, the entire household document and all of
  its subcollections (`items`, `shoppingList`) are recursively deleted by a Cloud Function.

When you leave all households, no household data that references you remains. Your Firebase
Auth account (email, display name) continues to exist until you request its deletion (see
"Your Rights" below).

Crashlytics crash reports are retained according to
[Firebase's default retention settings](https://firebase.google.com/support/privacy).

---

## Your Rights

### Leave a household

You can leave any household at any time via **Settings → Leave household**. Your name and
email are removed from the members map immediately. If you are the last member, the entire
household — including all stock items and shopping list entries — is permanently deleted.

### Sign out

You can sign out via **Settings → Sign out**. This ends your local session and clears your
credentials from the device. Your household data persists in Firestore until another member
acts on it (or until you sign back in and leave the household yourself).

### Delete your account

Account deletion is not yet available as an in-app action. This is a known v1 limitation;
a self-service account deletion screen is planned for a future release.

In the meantime, if you want your Firebase Auth account deleted, contact the developer at
the address in the "Contact" section below. The developer will delete the account manually
using the Firebase console, which also invalidates your sign-in credentials. Any households
you are still a member of will continue to exist for the remaining members.

---

## Cookies and Tracking

Pantry does not use browser cookies. Firebase Auth stores a session token in the app's
local (sandboxed) storage on your Android device. This token is used only to authenticate
your requests to Firestore and Cloud Functions. It is not a tracking cookie and is not
accessible to third parties or to other apps on your device.

Pantry contains no analytics SDKs, no in-app advertising, and no cross-app tracking.

---

## Children

Pantry is not directed at children under the age of 13. Firebase Authentication's terms of
service require users to be at least 13 years old in accordance with COPPA (Children's Online
Privacy Protection Act). Pantry does not knowingly collect personal data from anyone under 13.

If you are a parent or guardian and believe your child under 13 has created a Pantry account,
please contact us at the address below. We will delete the account and any associated data
promptly.

---

## Changes to This Policy

When the Pantry app begins collecting new data, sharing data with a new party, or materially
changes how existing data is used, this file is updated in the repository and the "last
updated" date at the top is changed. The repository's commit history serves as a full audit
trail of all changes to this policy.

There is no separate mailing list or notification mechanism for policy changes. If you rely
on this policy, you are encouraged to watch the repository for changes to this file.

---

## Contact

If you have a question about this privacy policy, want to request deletion of your account,
or believe there is an inaccuracy in this policy, please contact:

**`<REPLACE WITH YOUR CONTACT EMAIL>`**

This address is maintained by the repository owner. Requests sent to this address are
handled personally and are not routed through any third-party ticketing system.
