# Architecture and runtime flows

(Updated 2026-09-26 after the tester-readiness changes. Written without a build - verify on a device.)

## Components (manifest)
- `MainActivity` - launcher activity (view based, theme `Theme.Knappen.Main`).
- `MainWidget` (receiver, exported) - widget provider; intent filter only has `APPWIDGET_UPDATE`. Custom actions are sent with explicit intents.
- `Timer` (receiver, NOT exported) - alarm target.
- `BootReceiver` (receiver, exported) - `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
- `PermissionActivity` (not exported, dialog theme), `SettingsActivity` (not exported, dialog theme, noHistory).
- Permissions: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM` (API 33+, auto-granted), `SCHEDULE_EXACT_ALARM` (maxSdk 32), `RECEIVE_BOOT_COMPLETED`. Exact alarms are needed because the widget Chronometer only returns to idle when the alarm fires; an inexact alarm leaves it counting negative.

## Design decision: ONE shared timer
State is global (not per widget). All widgets show the same countdown and there is exactly one alarm (`MainButtonHandler.ALARM_REQUEST_CODE = 42`). README item 16 (unique timer per widget) is intentionally not implemented; changing this means per-widget prefs keys + widget id as extra on the `Timer` intent.

## Persisted state
`SharedPreferences("widget_prefs")` via `PrefsManager`:
- `timer_active` (bool), `timer_trigger` (epoch ms, -1 = none), `timer_duration` (ms, default 4.5 h, minimum 1 min), `notification_permission_prompted` (bool).
- `hasStoredTimer()` = active flag && trigger != -1. `isTimerActive()` = `hasStoredTimer()` && trigger > now (derived from the clock, so a late/missed alarm cannot leave the widget stuck).
- The file is excluded from cloud backup / device transfer (`backup_rules.xml`, `data_extraction_rules.xml`), so a restored phone never gets a timer without an alarm (this also drops the saved duration).
- Separate prefs file `com.bardsplayground.knappen.MainWidget` holds a leftover per-widget "title" (dead feature).

## Flow: tap the button
1. `updateAppWidget()` attaches a broadcast PendingIntent (explicit -> `MainWidget`, action `BUTTON_CLICKED`, requestCode `widgetId*10+1`) to both `main_button` and `main_countdown`.
2. `MainWidget.onReceive` -> `MainButtonHandler.onMainButtonClicked()`.
3. If `isTimerActive()`: only refresh widgets. Otherwise: `NotificationHandler.promptForPermissionOnce()` (opens `PermissionActivity` once if notifications are not allowed; behaviour of starting an activity from the receiver on a widget tap should be verified on Android 10+), then `startTimer`.
4. `startTimer`: trigger = now + duration; cancels legacy alarms; exact alarm (`setExactAndAllowWhileIdle`) unless API 31+ and `canScheduleExactAlarms()` is false, then `setAndAllowWhileIdle` (only reachable on API 31-32 after the user revoked it; can be very late). Stores trigger + active, refreshes widgets.
5. Widget rendering: idle -> `main_button` TextView ("Knappen"); locked -> `main_countdown` Chronometer in count-down mode with base = `elapsedRealtime + remaining` and format "Ready in %s". The Chronometer ticks by itself, no periodic refresh needed.

## Flow: alarm fires
`Timer.onReceive` -> `onTimerTriggered()` -> notification (only if permission granted; never opens an activity from the receiver) -> `setTimerActive(false)` -> refresh widgets.

## Flow: reboot / app update
`BootReceiver` -> `onBootOrUpdate()`: if a timer is stored and in the future, re-schedule it with the same fixed request code; if it ran out while the phone was off, call `onTimerTriggered()` (notification + reset); then refresh widgets.

## Flow: settings
Gear -> `PendingIntent.getActivity` -> `SettingsActivity`. Also reachable from `MainActivity`. Options: change duration (hours/minutes pickers, saved immediately, does not affect a running timer), "Use 1 minute (for testing)", "Reset running timer" (cancel alarm + clear state + refresh). A legacy `OPEN_SETTINGS` broadcast branch remains in `MainWidget.onReceive` for stale widget intents from older versions.

## Flow: notification permission
- `PermissionActivity` (explanation + system dialog, denial -> app notification settings) is shown once, from the first widget tap.
- `MainActivity` shows the current permission state with an "Allow notifications" button at any time; only when exact alarms are off (API 31-32 only) it shows an "Exact timing is off" row with a button to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

## Legacy alarms
Versions up to 1.4 used request code `widgetId*10+1` (and -9 after a reboot). `cancelLegacyAlarms()` cancels those (`FLAG_NO_CREATE`) whenever a timer starts or is reset, so an old alarm cannot fire twice after an update.

## Strings
All user-visible text is in `res/values/strings.xml` (English default) and `res/values-nb/strings.xml` (Norwegian). Add new text to both (or mark it `translatable="false"`, otherwise lint fails).

Language rule (`AppLanguage.kt`, owner's decision 2026-09-26): Norwegian if the phone's first language is nb/nn/no OR the phone is in Norway (SIM or network country `no`, or phone region Norway, e.g. `en-NO`); English otherwise. Android does not do this by itself, so:
- Every activity overrides `attachBaseContext` with `AppLanguage.localized(newBase)` - new activities must do the same.
- Receiver-side text (widget countdown format + gear content description in `updateAppWidget`, notification title/text/channel name) is fetched via `AppLanguage.localized(context).getString(...)`. Plain `context.getString` there would follow the phone language instead.
- `@string` inside `main_widget.xml` is resolved by the launcher with the phone locale; only language-neutral text ("Knappen") may stay there, anything else is set in code.
- `BootReceiver` handles `LOCALE_CHANGED`: refreshes widgets and renames the notification channel.
- Not covered: the widget picker description (`app_widget_description`) and app name are resolved by the system, so they follow the phone language. A SIM/network change has no broadcast; text updates on the next widget refresh or app start.
- Gotcha: TelephonyManager must come from `applicationContext` - from the unattached activity in `attachBaseContext` it crashes (NPE).
