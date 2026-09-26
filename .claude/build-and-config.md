# Build and configuration

- Open `ProjectFiles/` in Android Studio. Gradle wrapper 8.13, single module `:app`, version catalog `gradle/libs.versions.toml`.
- `app/build.gradle.kts`: applicationId/namespace `com.bardsplayground.knappen`, minSdk 24, target/compileSdk 36, versionCode 7 / versionName "1.5.1" (2026-09-26: replaced `USE_EXACT_ALARM` with `SCHEDULE_EXACT_ALARM` after Play Console flagged it; 1.5 = code 6 = exact-alarm fix, language rule and daily limit, uploaded but not released; 1.4 = code 5 = commit f37e374). Release build type sets `ndk { debugSymbolLevel = "SYMBOL_TABLE" }`, Java 11, `viewBinding` + `compose` enabled.
- Release build: `isMinifyEnabled = true` (declared twice; the second was meant to be `isShrinkResources`), proguard `proguard-android-optimize.txt` + `proguard-rules.pro`. A signed `app/release/app-release.aab` exists locally (gitignored via `*.aab`).
- Dependencies: core-ktx 1.10.1, appcompat 1.7.1, `com.google.android.material:material:1.13.0` (hard-coded, not in catalog), Compose BOM 2024.09.00 + material3 (declared twice: BOM-managed and 1.4.0), lifecycle-runtime-ktx 2.6.1, activity-compose 1.8.0. Tests: only Android Studio example tests.
- `local.properties` is gitignored (SDK path). Logs use the tags `Knappen`, `Button handler`, `Timer`, `PermissionActivity`.
- `allowBackup=true` (see the backup line below).
- Widget metadata: 2x1 target (API 31+), min 109x56 dp below, resize both directions, `updatePeriodMillis` 86400000, `widgetCategory=home_screen`.
- Backup: `widget_prefs.xml` is excluded from both backup rule files (timer state must not be restored onto another device).
- Themes: `Theme.Knappen.Main` (launcher screen, dark MaterialComponents), `Theme.Knappen.Dialog` (settings/permission).
- 2026-09-26 (evening): `assembleDebug lintDebug testDebugUnitTest` pass (0 lint errors) and the app runs on an API 36 emulator. See `testing.md` for the local SDK/JDK setup.
