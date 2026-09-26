# Testing on the Android emulator (for Claude Code running on the user's PC)

Prerequisites: Android SDK with platform-tools (`adb`), `emulator`, one AVD with a Google Play or Google APIs image (API 34+ recommended; also test one API 33 and, if possible, API 24-28). App id: `com.bardsplayground.knappen` (debug builds have the same id). Commands below are shell-neutral; on Windows PowerShell use `gradlew.bat`, `findstr` instead of `grep`, and never redirect binary output with `>` (use `adb pull`).

Repo rules still apply: no git commands that change anything, nothing is uploaded or released from a test run. Record results in `improvements.md` (checklist at the bottom).

## 0. Build checks (no emulator needed)
```
cd ProjectFiles
./gradlew assembleDebug lintDebug testDebugUnitTest
```
Must pass with 0 lint errors. New strings need an nb translation or `translatable="false"` (lint fails on MissingTranslation).

On this PC (Windows): SDK is at `%LOCALAPPDATA%\Android\Sdk` (not on PATH), JDK is Android Studio's `jbr`; set `JAVA_HOME` and `ANDROID_HOME` per command. AVD: `Medium_Phone_API_36.1`. The widget countdown is not in the uiautomator dump (Chronometer) - use screenshots.

## 1. Emulator and install
```
emulator -list-avds
emulator -avd <NAME> -no-snapshot-save &          # background
adb wait-for-device
adb shell getprop sys.boot_completed              # repeat until it prints 1
./gradlew installDebug                            # from ProjectFiles
adb shell am start -n com.bardsplayground.knappen/.MainActivity
```

## 2. Seeing and tapping (how to "use" the emulator)
- Screenshot (then Read the PNG): `adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png .`
- UI tree as text: `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml .` Find the element by `resource-id` (e.g. `com.bardsplayground.knappen:id/btn_open_settings`) or `text`, take the centre of its `bounds="[x1,y1][x2,y2]"`.
- Tap / input: `adb shell input tap X Y`, `input swipe X1 Y1 X2 Y2 300`, `input keyevent KEYCODE_BACK`, `input text`.
- The widget is drawn by the launcher; its text (e.g. "Ready in 0:59") normally shows up in the launcher's uiautomator dump, otherwise use a screenshot.
- Put the widget on the home screen: open `MainActivity`, tap "Add widget to home screen" (id `btn_add_widget`), then confirm the system pin dialog. Fallback: long-press home screen -> Widgets -> Knappen -> drag.

## 3. Inspecting state
- Prefs (debug build): `adb shell run-as com.bardsplayground.knappen cat shared_prefs/widget_prefs.xml` (keys: `timer_active`, `timer_trigger`, `timer_duration`, `notification_permission_prompted`).
- Scheduled alarm: `adb shell "dumpsys alarm | grep -B2 -A8 bardsplayground"`. Expect exactly ONE Knappen alarm while locked, pointing at the `Timer` receiver, and none after reset/fire.
- Notifications: `adb shell "dumpsys notification --noredact | grep -A12 bardsplayground"`.
- Logs: `adb logcat -d -s Knappen:D Timer:I AndroidRuntime:E` (clear first with `adb logcat -c`).
- Force a permission state: `adb shell pm revoke com.bardsplayground.knappen android.permission.POST_NOTIFICATIONS` / `pm grant ...`; exact alarms: `adb shell appops set com.bardsplayground.knappen SCHEDULE_EXACT_ALARM allow|deny`.
- Reset app data: `adb shell pm clear com.bardsplayground.knappen` (this also removes the widget's stored state; re-add the widget afterwards).
- Doze: `adb shell dumpsys deviceidle force-idle` ... `adb shell dumpsys deviceidle unforce`.

## 4. Test scenarios (mirrors the checklist in improvements.md)
1. **Fresh install / launcher screen.** `pm clear`, launch. Expect: title, setup steps, status rows, "Add widget" button (API 26+), no edge-to-edge overlap with status/navigation bars (API 35+ emulator!). Tap "Allow notifications" -> system dialog; Deny -> app notification settings opens.
2. **Start + live countdown.** Set 1 minute in Settings (`settings_testDuration`), tap the widget. Expect: label switches to "Ready in 0:5x" and ticks by itself over 10 s (take two screenshots), one alarm in `dumpsys alarm`, `timer_active=true`. Tap again while locked: nothing changes, no second alarm.
3. **Fire.** Wait about 65 s. Expect: notification "Knappen is clickable again!", widget back to "Knappen", prefs `timer_active=false`, no alarm left. Tap the notification: `MainActivity` opens.
4. **Permission flow.** Revoke notifications, `pm clear` (or set `notification_permission_prompted` false), tap widget: explanation screen appears once (verify that the activity really opens from the widget tap on API 29+; if not, fix and note it in improvements.md), timer still starts. Second start: no screen. Fire with permission revoked: no crash, no notification.
5. **Reboot while locked.** Start a 1-minute timer, `adb reboot`, wait for boot, check widget shows a running countdown and `dumpsys alarm` has one alarm. Then Settings -> Reset: alarm gone, no notification a minute later (this was bug #1). Also let it expire while the emulator is rebooting/off: after boot expect the notification and an idle widget.
6. **Update over old build.** Best: install the 1.4 build (ask the user for the APK/AAB, or export it read-only with `git archive <commit that set versionName "1.4"> | tar -x -C <tmpdir>` and build there), start a timer, then `adb install -r` the new debug build. Expect: no duplicate notification, widget consistent, the old alarm code (`widgetId*10+1`) gone from `dumpsys alarm`. Also `adb install -r` new over new to exercise `MY_PACKAGE_REPLACED`.
7. **Exact alarm.** Fresh install on API 34+: "Exact timing is off" row + button in `MainActivity`; the button opens the system toggle. After granting: row gone, Knappen alarm has `window=0` in `dumpsys alarm`. With `appops set ... SCHEDULE_EXACT_ALARM deny`: timer still fires (late; the widget may show negative time until then).
8. **Localization** (rule: Norwegian language OR in Norway, else English - see architecture.md). `cmd locale set-app-locales` no longer has any effect (the app ignores per-app locales). Use `adb shell am start -a android.settings.LOCALE_SETTINGS`: "Add a language" -> search icon -> "bokm" -> Norge, then the row's menu button (content-desc "Edit system language list, ...") -> "Move to top" -> Change. Check with `adb shell settings get system system_locales`. Cases: nb-NO -> Norwegian; en-US -> English; English + Region Norway (`en-NO`, via "Region") -> Norwegian. Expect the launcher screen, settings, widget ("Klar om ..."), gear content-desc, notification and channel name (`dumpsys notification | grep -o 'mName=[^,]*'`) to switch without reinstalling. Reset the emulator to en-US afterwards.
9. **Robustness extras.** `adb shell am force-stop com.bardsplayground.knappen` while locked (Android drops the alarm on force-stop - the widget must then fall back to idle when `trigger` passes, without stuck "locked" state); rotate the settings dialog; two widgets on the screen show the same countdown; remove the widget while locked and re-add it.

10. **Daily limit.** Settings: 1 minute + limit 2 (tap the value above/below the middle of the picker to step). Tap 1: countdown + "1 of 2 left today"; fire -> notification. Tap 2: widget shows only "0 left today", `dumpsys alarm` has the Timer alarm AND an `RTC` `DAY_CHANGED` alarm at next 00:00; the timer then fires with NO notification; further taps do nothing (`clicks_count` stays). Midnight without waiting: `settings put global auto_time 0`, `adb shell cmd alarm set-time <epoch ms of 23:59:30 local>` (works without root), wait 35 s -> "2 of 2 left today" and the next DAY_CHANGED is scheduled; restore with `settings put global auto_time 1`. "No limit" hides the counter line. Unit tests: `DailyLimitTest`.

## 5. Reporting
After a run, update the status/notes in `improvements.md` (what passed, what failed, exact reproduction) and fix bugs in code. Do not commit; the owner does that.
