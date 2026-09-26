# Google Play release notes

Decisions (2026-09-26): personal developer account (no registered company, so no organization account). Goal: publish Knappen and show it on https://www.bardsplayground.no as portfolio for recruiters. The first closed test (10 friends/family) failed: fewer than 12 testers and little usage.

## Requirements (personal accounts created after 13 Nov 2023, per Play Console Help "App testing requirements for new personal developer accounts")
- Closed test with at least 12 testers opted in continuously for 14 days before applying for production access.
- The application asks about tester engagement, feature coverage, recruitment, and changes made from feedback; review takes about 7 days. Google looks at real usage, so ship at least one update during the 14 days and keep concrete feedback notes with build numbers.
- Opt-outs break the count: recruit 15-20 people for a target of 12.

## Preparing the test build
- Bump `versionCode` (currently 6, versionName 1.5) and `versionName` in `app/build.gradle.kts` for every upload.
- Build a signed `.aab` (owner keeps the key; never commit keystores). Use Play App Signing.
- Tester-friendly features already in the code: launcher screen with setup steps and pin-widget button, live countdown, 1-minute test duration in Settings, notification permission status.
- Play Console: privacy policy URL (needed; the app collects nothing, only local prefs), data safety form, app content declarations (the exact-alarm declaration IS required: the app uses `USE_EXACT_ALARM`, which Play only allows for alarm/timer apps - describe Knappen as a timer whose core function is the "ready again" alert at an exact time), store listing graphics from `Logos/`.

## Open work
- Tester recruitment post + tester guide + feedback form + 14-day plan (not written yet).
- Website: Knappen page and privacy policy on bardsplayground.no.
- Alternative if 12 testers cannot be reached: GitHub Releases APK, IzzyOnDroid / F-Droid (no tester requirement).
