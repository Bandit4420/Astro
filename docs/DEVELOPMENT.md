# Developing Astro Android

## Build

Install JDK 17 and Android SDK Platform 35 + Build Tools 35.0.0. Set `ANDROID_HOME` to your SDK directory, or use Android Studio's generated `local.properties`.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows: use `gradlew.bat` with the same tasks. The Gradle 8.11.1 wrapper and its distribution checksum are committed. Dependencies are pinned; the Android Gradle Plugin is 8.9.2, Kotlin is 2.1.20, and the minimum OS is API 31 (Android 12). This is a sideload preview targeting API 35, not a Play Store release.

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The package name is `com.bandit.astro.preview`, leaving the future release package independent.

## GitHub builds

The **Android preview** workflow tests, lints and builds the debug APK on branch pushes, pull requests, or a manual run. A successful run uploads the APK, SHA-256 checksum, test/lint reports, and generated Room schema. Download artifacts while signed into a GitHub account with access to this private repository. Artifacts expire after 14 days.

CI uses a temporary Android debug signing identity. APKs from separate clean runners may require an uninstall before installation, which deletes local progress. Do not rely on preview builds as your only collection backup. Persistent release signing and export/import must be implemented before migration from the web app.

No signing keys, account tokens, passwords, or AniList client secret belong in Git.

## Data and power

- The preview calls AniList's public GraphQL API. It does not impersonate a username or modify an AniList account.
- Room holds local collection entries and notification deduplication. Increment updates are atomic SQL operations.
- DataStore stores theme, spoiler, timezone, and reminder preferences.
- Discovery loads 20 titles at a time up to 80, with a 350 ms search debounce and cancellation.
- The detail cache retains 12 titles. Coil's disk cache is capped at 64 MB and its memory cache at 12% of the app's memory budget.
- An hourly WorkManager request is registered only when the user enables reminders. It uses a connected-network constraint, retries transient failures with backoff, and checks the preceding six hours of releases for currently watching titles.
- Alerts are approximate, not exact alarms. No persistent service, exact-alarm permission, foreground polling, analytics or advertising SDK is added.
- Notification taps open the matching title through an explicit, immutable PendingIntent. Permission is requested on Android 13+ when enabling reminders.
- Runtime statistics use only known AniList durations. They are local tracked runtime, not verified viewing time.

## Spoilers and parity

AniList has no dependable episode-by-episode spoiler boundaries. Protection conservatively hides descriptions, cast and biographies. Reveals are scoped to the current title/person visit and reset when protection changes.

This is a first native milestone, not full parity with the website. Secure AniList OAuth and sync, OP/ED credits, advanced discovery filters, quiz, constellation, Wrapped, local backup, chibi artwork, and glass rendering remain to be ported. Character details currently show the first 25 cast members. Trailers open in the installed YouTube app or browser.

Before account sync: register a dedicated native OAuth client/callback without changing the website callback, verify Viewer ownership, store credentials with Android Keystore protection, partition Room records by account, and add a durable revision-aware outgoing queue. Do not silently upload the local preview collection after login.

## Device acceptance before release

- Android 12 and Android 13+: install, navigate, deny/grant notification permission.
- OnePlus Nord 5: system bars, large fonts, back navigation, background restrictions.
- Network loss: collection remains available; retry restores discovery without duplicates.
- Rotate or recreate: no duplicate requests or lost saved collection edits.
- Increment at known/unknown totals; rapid taps; remove while detail is open.
- Reminders: watched/completed titles stay quiet; duplicate runs do not repeat alerts; disabling cancels work.
- Measure startup, scroll jank, idle battery and memory on a physical device before claiming efficiency improvements.
