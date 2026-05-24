# Pantry — Play Store screenshot checklist

Capture 6 phone screenshots and place them in `play-store/screenshots/`
(gitignored). Upload directly to Play Console; do NOT commit the PNG files.

## General rules

- **Capture method**: Power + Volume-Down on the device, OR
  `adb exec-out screencap -p > play-store/screenshots/01-auth.png` from a
  connected computer.
- **Theme**: Light mode (Settings → Display → Light). Play preview defaults
  to light and most users browse the store in light mode.
- **Status bar**: clean — disable notifications during capture, ideally on
  a battery-charged device. Optional: use Android's "Demo mode" via
  `adb shell settings put global sysui_demo_allowed 1` for a fake clean
  status bar; out of scope here.
- **Resolution**: real phone (REA-NX9 → 1080×2400) is fine. Play accepts
  320–3840 px on the long side.

## The 6 shots

### 1. `01-auth.png` — Auth screen

**Prep**: sign out (Settings → Sign out) or fresh install.
**Capture**: the Auth screen showing the 80dp green basket logo, "Pantry"
title, "Continue with Google" + "Sign in with email" + "Create an account".

### 2. `02-stock.png` — Stock list with low-stock indicators

**Prep**: sign in to a household. Make sure you have at least 5 items
spanning 2-3 categories. Specifically:
- One item below threshold (qty < threshold, e.g., Milk: qty=1, threshold=2)
- One item at qty 0 (e.g., Bread: qty=0, threshold=1)
- One normal item (e.g., Apples: qty=10, threshold=3)

**Capture**: Stock tab open, no search active, "All" category chip selected.
Three indicators visible: red ⚠ + red qty for low-stock, greyed/italic row
for qty=0 (which also shows the ⚠ thanks to the Phase 4 fix), normal row.

### 3. `03-add-item.png` — Add item bottom sheet

**Prep**: from Stock, tap the + FAB.
**Capture**: bottom sheet open showing all fields: Name, Quantity + Unit
row, Low-stock threshold, Default restock quantity, Category chips. Type
"Yogurt" in Name so it looks like a real flow.

### 4. `04-shopping.png` — Shopping list

**Prep**: have at least 2 items below threshold (auto entries) + add
1 manual entry via Shopping FAB (e.g., "Wine for guests").
**Capture**: Shopping tab, both sections visible (Running low / Added
manually), bottom Finish shopping button visible.

### 5. `05-finish-shopping-dialog.png` — Finish shopping confirm

**Prep**: from shot 4's state, check at least one auto entry + type a
quantity, then tap the bottom Finish shopping button.
**Capture**: the AlertDialog with title "Finish shopping?" and
"Restock N items, clear M manual entries" text.

### 6. `06-settings.png` — Settings tab

**Prep**: sign in to a household with at least 2 members and 3+ categories.
**Capture**: Settings tab scrolled to top. Should show: Household name,
invite code with copy + share icons + 6-char code, Regenerate button,
Members section with at least 2 rows (one with "(you)" badge), beginning
of Categories section.

## After capture

- Verify each PNG is in `play-store/screenshots/`.
- DO NOT `git add` them — they're gitignored and uploaded directly to
  Play Console.
- Open Play Console → Production release → Store listing → Phone
  screenshots → upload all 6. Order matters: 1 (auth) shows first to
  users browsing.
