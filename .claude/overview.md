# Overview

## Purpose
A single home-screen widget ("Knappen" = "the button"). Tap it and it locks for a user-defined duration (default 4.5 h) while showing the remaining time. When the time is up an alarm fires, a notification says "Knappen er klikkbar igjen!" and the button is active again. A small gear on the widget opens a settings dialog (change duration, reset timer).

## Stack
Kotlin, AGP 8.13.2, Kotlin 2.0.21, compileSdk/targetSdk 36, minSdk 24, AppWidgetProvider + RemoteViews, AlarmManager, SharedPreferences, NotificationCompat. Compose/Material3 are still declared in Gradle but no longer used by any code.

## Feature status (real state vs README TODO) - as of 2026-09-26
| Feature | Status |
|---|---|
| Alarm starts on tap, survives reboot and app update | Done (fixed request code, `BootReceiver`) |
| Notification permission request + explanation screen | Done (once from widget tap, and from the launcher screen) |
| Notification when timer ends | Done (tapping it opens the app) |
| Remaining time on the button, auto-refreshing (README 7, 8) | Done - live `Chronometer` countdown |
| Settings activity + editable duration (README 3, 6) | Done (hours 0-23, minutes 0-59, minimum 1 min) + "1 minute" test option |
| Reset timer from settings | Done |
| Max Y taps per day, resets at midnight, "x of y left today" on the widget | Done (default 4, 1-24 or no limit, set in Settings) |
| Settings gear on widget | Done |
| Launcher screen with setup help, pin-widget button, permission/exact-alarm status | Done (new) |
| Persistent notification with countdown (9) / "timer started" notification (10) | NOT done |
| Read/write all options from prefs (14) | Partly (timer state, duration, permission-prompted flag) |
| Localization en/nb (15) | Done for all strings (English default, `values-nb`); Norwegian when the phone language is Norwegian or the phone is in Norway (`AppLanguage.kt`) |
| Unique timer per widget instance (16) | Dropped: one shared timer by design (see architecture.md) |
| Real disabled-state visual | NOT done |

## Source file map (`ProjectFiles/app/src/main/java/com/bardsplayground/knappen/`)
| File | Role |
|---|---|
| `AppLanguage.kt` | Chooses nb/en (Norwegian language OR in Norway) and wraps contexts; used by all activities, the widget and notifications. |
| `MainActivity.kt` | Launcher screen: setup steps, pin-widget button, notification / exact-alarm status with grant buttons, link to settings. |
| `MainWidget.kt` | `AppWidgetProvider` + top-level `updateAppWidget()` (idle label vs live Chronometer, PendingIntents). |
| `MainButtonHandler.kt` | Core logic: click handling, start/cancel alarm (fixed request code + legacy cleanup), boot/update restore, refresh all widgets. |
| `PrefsManager.kt` | SharedPreferences `widget_prefs` (timer state, duration, daily limit + today's count, permission-prompted flag), duration/limit constants and the pure day helpers (`dayKey`, `nextMidnight`, `clicksLeft`). |
| `Timer.kt` | BroadcastReceiver fired by AlarmManager -> `onTimerTriggered()`. |
| `BootReceiver.kt` | `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` -> `onBootOrUpdate()`. |
| `NotificationHandler.kt` | Channel `knappen_channel`, posts the "ready" notification (fixed id, opens app), `promptForPermissionOnce()`. |
| `PermissionActivity.kt` / `PermissionUtils.kt` | Explanation screen + runtime permission request (API 33+). |
| `SettingsActivity.kt` | Dialog-themed settings: duration picker, daily-limit picker ("No limit" = 0), 1-minute test option, reset. Pickers survive rotation. |
| `helpers/LongValues.kt` | ms constants + `convertLongToStrings()` -> h/m/s. |
| `MainWidgetConfigureActivity.kt` | Template leftover, not registered -> dead. |
| `Settings.kt`, `ToastActivity.kt`, `ui/theme/*` | Unused (Compose theme too; all Compose dependencies are now removable). |

## Resources of note
`res/layout/main_widget.xml` (TextView `main_button` idle label, Chronometer `main_countdown`, ImageButton `btn_settings`), `activity_main.xml`, `activity_settings.xml`, `dialog_duration_picker.xml`, `activity_permission.xml`, `xml/main_widget_info.xml` and `xml-v31/main_widget_info.xml` (2x1 target cells, 24h `updatePeriodMillis`), `strings.xml` + `values-nb/strings.xml` (all UI text).
