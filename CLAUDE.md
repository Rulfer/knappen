# Knappen – Claude Index

Android home-screen widget with one button that is only "clickable" again X hours/minutes after the last tap.
Package `com.bardsplayground.knappen`, Kotlin, views/RemoteViews based (Compose dependencies are still in Gradle but unused).

## Rules for Claude in this repo
- NEVER run git commands that change anything (add, commit, push, remove, ...). Reading git history is fine.
- Code changes are fine. There is no DB in this project.
- Whenever notable changes are made, update the relevant document below so it stays current across sessions.
- Docs live in `.claude/`; this file is only the index.

## Documentation index
| Document | Contents |
|---|---|
| [.claude/overview.md](.claude/overview.md) | What the app does, feature status, file map |
| [.claude/architecture.md](.claude/architecture.md) | Runtime flows: click, alarm, boot, notifications, permissions, persisted state |
| [.claude/build-and-config.md](.claude/build-and-config.md) | Gradle, SDK levels, dependencies, manifest, resources, release |
| [.claude/testing.md](.claude/testing.md) | Emulator/adb recipes and the scenario list for testing the app (build, install, tap, inspect alarms/notifications, reboot) |
| [.claude/play-release.md](.claude/play-release.md) | Google Play closed-test requirements, release checklist, open publishing work |
| [.claude/improvements.md](.claude/improvements.md) | Prioritised list of bugs, risks and improvement ideas (from the 2026-09-26 scan) |

## Repo layout (short)
- `README.md` – owner's rough TODO list (partly outdated, see overview.md for real status)
- `Logos/` – store/promo graphics
- `ProjectFiles/` – the Gradle project (open this folder in Android Studio); single module `app`
- `ProjectFiles/app/src/main/java/com/bardsplayground/knappen/` – all Kotlin sources (flat, 1 helper)

## Maintenance
Last full scan: 2026-09-26 (app versionName 1.4 / versionCode 5). Same day: tester-readiness code changes were made (see improvements.md). Built and emulator-tested the same evening (API 36); test progress is in the checklist at the bottom of improvements.md. Then version bumped to 1.5 / code 6 (exact alarms, language rule, daily limit).
