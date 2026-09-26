# Improvements and known issues (scan of 2026-09-26)

Status column: open / done. Code changes of 2026-09-26 were written without a build (no Android SDK available) - build and test on a device before releasing. Update this file when items are fixed.
File references are in `ProjectFiles/app/src/main/java/com/bardsplayground/knappen/`.

## A. Bugs and correctness

| # | Sev | Status | Issue | Suggested fix |
|---|---|---|---|---|
| 1 | High | done (2026-09-26) | After reboot the alarm is re-scheduled with request code -9 (`BootReceiver` intent has no widget id, `getAlarmRequestCode` = `-1*10+1`). Reset from Settings then cancels code `id*10+1`, so the rebooted alarm is NOT cancelled and still fires a notification after a reset. | Store the widget id(s) with the timer state and rebuild request codes from that, or use one fixed request code per timer. |
| 2 | High | decided: shared timer (see architecture.md) | Timer state is global (`PrefsManager`) although README #16 and the commit history claim per-widget timers. Two widgets show one shared countdown; a second widget's tap is ignored while active. | Key prefs by appWidgetId (`timer_trigger_<id>`), put the id as extra on the `Timer` intent, clean up in `onDeleted`. Or decide "one shared timer" and drop the per-widget request codes. |
| 3 | High | done (2026-09-26) | `PermissionActivity` is started from `Timer`/`NotificationHandler` (a BroadcastReceiver, background). Android 10+ background-activity-start limits make this fail silently, so a user without permission never gets the notification. | Ask for the permission from a foreground surface (widget tap or Settings), and in the receiver just skip the notification if not granted. |
| 4 | Med | done (2026-09-26) | Widget can stay stuck on "nå": `isTimerActive()` only looks at stored flags, so if the alarm is late/missed (inexact fallback, Doze, force stop) the widget shows "nå" and the button is not treated as idle until the alarm fires. | Derive state from `trigger <= now` in `updateAppWidget` and `isTimerActive`, and clear the flag lazily. |
| 5 | Med | done (2026-09-26, emulator-verified): `USE_EXACT_ALARM` on API 33+, `SCHEDULE_EXACT_ALARM` maxSdk 32. Found in testing: the inexact fallback fired 45 s late on a 1-min timer (window = 75% of duration, so up to ~3 h on 4.5 h) and the widget showed "Ready in -00:40". SecurityException is still only logged | Exact-alarm handling: `SCHEDULE_EXACT_ALARM` is denied by default on API 33+/34+ installs, so the code silently falls back to inexact `set()` (can be minutes late) and there is no UI to request it. `SecurityException` is swallowed with only a log, leaving no timer and no user feedback. | Either accept inexact (`setAndAllowWhileIdle`) and remove the permission, or add an explanation flow to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Show a toast/notification if scheduling fails. |
| 6 | Med | done (2026-09-26) | Alarm is not restored after an app update (`MY_PACKAGE_REPLACED` not handled; alarms may be dropped on update). Verify on device. | Add `MY_PACKAGE_REPLACED` (and optionally `TIME_SET`/`TIMEZONE_CHANGED`) to the boot receiver. |
| 7 | Med | done (2026-09-26) | Boot after the trigger time passed silently clears state - the user never gets the "clickable again" notification. | If trigger passed during downtime, post the notification (or at least refresh) on boot. |
| 8 | Low | done (2026-09-26) | `onBoot` uses `Handler.postDelayed(500ms)` inside a receiver; the process may be gone before it runs. Also unnecessary (`onUpdate` is called by the system after boot). | Refresh directly or use `goAsync()`. |
| 9 | Low | done (2026-09-26) | Duration can be set to 0h0m (instant timer). Settings change is not reflected in the running timer (by design, but undocumented in the UI). | Enforce a minimum (e.g. 1 min) in `SettingsActivity`. |
| 10 | Low | done (2026-09-26) | Time string floors: 59m59s shows "59m", 1s..59s shows "Zs" then jumps; hours string "1t0m". | Round up (ceil) so the widget never shows less than remaining, or use the chronometer (see C1). |
| 17 | Low | done (2026-09-26, found by lint, untested - no API 24/25 image) | `MainActivity.openNotificationSettings` used `ACTION_APP_NOTIFICATION_SETTINGS` (API 26+) on API 24-25. | Falls back to `ACTION_APPLICATION_DETAILS_SETTINGS` below API 26. |
| 11 | Low | done (2026-09-26) | `PrefsManager` KDoc says default 4 hours but code uses 4.5 h; `SettingsActivity` lazy getters (`_prefsManager ?: PrefsManager(this)`) never cache; typo "Abnryt" ("Avbryt"); `onResetButtonClicked` refreshes widgets twice. | Small cleanups. |

## B. Security / manifest hygiene

| # | Sev | Status | Issue | Suggested fix |
|---|---|---|---|---|
| 12 | Med | done (2026-09-26) | `Timer` receiver is `exported=true` with no filter: any app can trigger "timer finished" (clears timer, posts notification). `PermissionActivity` is exported without an intent filter for no reason. | Set `Timer` and `PermissionActivity` to `exported=false` (AlarmManager PendingIntents still work). |
| 13 | Med | done (2026-09-26) | `MainWidget` intent-filter exposes `BUTTON_CLICKED`, `OPEN_SETTINGS`, `REQUEST_PERMISSION` to other apps (start the timer / open settings). The PendingIntents are already explicit. | Keep only `APPWIDGET_UPDATE` in the filter (and `REQUEST_PERMISSION` is unused - delete). |
| 14 | Low | open | `BootReceiver` exported=true; system broadcasts do not require this. | Try `exported=false` and test boot. |
| 15 | Low | done (2026-09-26) (also drops the saved duration on restore) | `allowBackup=true` with unedited backup rules: restoring onto a new device brings back a stale `timer_active`/trigger with no alarm. | Exclude `widget_prefs` from backup, or set `allowBackup=false`. |
| 16 | Low | partly: fixed id + tap opens app done; dedicated monochrome icon still open | Notification uses the adaptive launcher foreground as small icon, has no `contentIntent`, and a random `currentTimeMillis().toInt()` id (stacks duplicates). | Dedicated monochrome status icon, fixed id, tap opens Settings/app. |

## C. Features / UX (README TODO items)

1. **Live countdown (README 8): done.** `main_countdown` is a `Chronometer` in count-down mode; `main_button` (idle label) and `main_countdown` are toggled with `setViewVisibility` in `updateAppWidget`. A RemoteViews Chronometer cannot stop at 0, so it relies on the alarm firing on time to switch back to idle; that is why exact alarms are required (A5). Only a user on API 31-32 who revokes exact alarms can still see negative time.
2. **Real disabled state: open.** While locked the widget only shows the countdown; a dimmed drawable would make it clearer.
3. **Notifications (README 9, 10): open** - "timer started" notification and optional ongoing countdown notification (`setUsesChronometer` + `setChronometerCountDown`).
4. **Localization (README 15): done** for all app strings. Default = English, `values-nb` = Norwegian. New UI text must go in both files. Since 2026-09-26 the language is chosen by `AppLanguage` (Norwegian language OR in Norway, else English) - see architecture.md. Unit tested (`AppLanguageTest`); on the emulator verified nb-NO, en-US, en-NO and switching back and forth. Not verifiable on the emulator: Norwegian SIM/network (its SIM is `us`) and Nynorsk as first language (not offered as a system language there).
5. **Open Settings without the receiver hop: done** (gear uses `PendingIntent.getActivity`; the old broadcast branch stays for stale widget intents).
6. **Widget config (optional): open** - wire or delete `MainWidgetConfigureActivity`.
7. **Launcher entry: done.** `MainActivity` (view based) explains setup, has "Add widget" (pin request, API 26+), shows notification / exact-alarm status with grant buttons, opens Settings.
9. **Daily limit: done (2026-09-26, emulator-verified incl. a real midnight).** Max Y taps per calendar day (default 4, 1-24 or no limit) on top of the X-hour lock; "x of y left today" on the widget, "0 left today" when used up. Open ideas: an option to also reset today's count from Settings (deliberately not added - for medicine use that would make the cap easy to bypass).
8. **Tester support: done.** Settings has "Use 1 minute (for testing)"; minimum duration is 1 minute.

## D. Code / build cleanup

- Still dead: `Settings.kt`, `ToastActivity.kt`, `MainWidgetConfigureActivity` (+ binding, `appwidget_text` "EXAMPLE", title-pref helpers, `main_widget_configure.xml`), `ui/theme/*`.
- **All Compose dependencies are now unused** (BOM, ui, graphics, tooling, material3 x2, activity-compose, `compose` build feature, Kotlin compose plugin, `ui/theme`). Removing them shrinks the APK; not done because it could not be build-tested here.
- `build.gradle.kts`: duplicate `isMinifyEnabled` (should be `isShrinkResources = true`), hard-coded Material dependency and duplicated material3; `libs.versions.toml` is old (Kotlin 2.0.21, core-ktx 1.10.1, lifecycle 2.6.1, Compose BOM 2024.09).
- `MainButtonHandler` is still constructed per receiver call (cheap now; `updateAppWidget` only creates `PrefsManager`).
- README TODO list is stale; status lives in `overview.md`.
- Unused imports remain in `MainWidgetConfigureActivity`, `Timer`, others.
- Play Console: `USE_EXACT_ALARM` needs the exact-alarm permission declaration; declare Knappen's core function as a timer (see `play-release.md`). If Play rejects it, the fallback is showing a fixed "Ready at HH:MM" instead of the countdown.

## E. Testing

Only the default example tests exist. Cheap wins: unit tests for `PrefsManager` state derivation (`isTimerActive` vs `hasStoredTimer`, min duration clamp) using Robolectric or an injectable clock; an instrumented test for start -> reboot restore -> cancel.

## Manual test checklist for the 2026-09-26 changes
Run 2026-09-26 on the API 36 emulator (`Medium_Phone_API_36.1`) following `testing.md`. Not yet run on API 24-33 or a real device.
1. PASS - Fresh install: setup screen, no edge-to-edge overlap, "Add widget" pins a widget, "Allow notifications" -> system dialog, Deny -> app notification settings.
2. PASS - Tap widget: countdown ticks live, tap while locked does nothing, exactly one alarm.
3. PASS after fix - "Use 1 minute" -> notification, widget back to "Knappen", tapping the notification opens `MainActivity`. First run FAILED: the inexact fallback fired 45 s late and the widget showed "Ready in -00:40" (fixed with `USE_EXACT_ALARM`, see A5; re-run fired 3 ms after trigger).
4. PASS - Permission screen opens from the widget tap on API 36, only once; timer still starts; firing without permission: no crash, no notification.
5. PASS - Reboot while locked: timer restored (one exact alarm), fired and reset. Reset in Settings after reboot: alarm cancelled, nothing fires (bug #1 fixed). Expired while powered off: notification + idle widget after boot. Note: right after a reboot the exact alarm fired 27 s late once (emulator boot noise?) - recheck on a real device.
6. PASS - Update over the committed 1.4 build (HEAD f37e374, built from `git archive`) with a running timer: `MY_PACKAGE_REPLACED` restores it, one alarm, one notification.
7. PASS - API 36: `USE_EXACT_ALARM` granted, no exact-timing row. API 31-32 revoke path not tested (no image).
8. PASS - Norwegian (`cmd locale set-app-locales ... nb`): main screen, settings, widget ("Klar om 00:56"), notification.
9. PARTLY - Four widgets show the same countdown: PASS. Force-stop while locked: alarm dropped, launcher shows grey placeholders while stopped; on next app start Android 15+ sends BOOT_COMPLETED, the app posts the missed notification and goes idle: PASS. Rotating the duration picker: FAILED (dialog closed), fixed (state saved/restored, dismissed in onDestroy), PASS. Remove widget while locked + re-add: not run.
- Cosmetic, open: the 2x1 widget has a translucent grey background, larger sizes are black.

## Suggested next steps
Per-tester flow for the Play closed test (see `play-release.md`), disabled-state visual (C2), remove Compose (D), notifications (C3).
