# Developing Astro Android

## Build

Install JDK 17 and Android SDK Platform 35 + Build Tools 35.0.0. Set `ANDROID_HOME` to your SDK directory, or use Android Studio's generated `local.properties`.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows: use `gradlew.bat` with the same tasks. The Gradle 8.11.1 wrapper and its distribution checksum are committed. Dependencies are pinned; the Android Gradle Plugin is 8.9.2, Kotlin is 2.1.20, and the minimum OS is API 31 (Android 12). This is a sideload preview targeting API 35, not a Play Store release.

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The package name is `com.bandit.astro.preview`, leaving the future release package independent.

## GitHub builds

The **Android preview** workflow tests, lints and builds the debug APK on main-branch pushes, pull requests, or a manual run. A successful run uploads the APK, SHA-256 checksum, test/lint reports, and generated Room schema. Download artifacts while signed into a GitHub account with access to this private repository. Artifacts expire after 14 days.

CI uses a temporary Android debug signing identity. APKs from separate clean runners may require an uninstall before installation, which deletes local progress. Do not rely on preview builds as your only collection backup. Export a backup from the installed build before moving between differently signed builds. Version 0.1 has no export UI: retain that installation if its data matters until a safe migration is available. Stable release signing remains a release gate.

No signing keys, account tokens, passwords, or AniList client secret belong in Git.

## Data and power

- Public browsing is anonymous. Mutations require browser OAuth, verified Viewer ownership and an unexpired encrypted session. See ANILIST_ANDROID.md for client setup.
- Each account has its own Room database, plus the original guest database. Local edits are transactional, with durable dirty records and revision-based sync acknowledgements. Private notes are excluded from remote mutations.
- DataStore stores theme, spoiler, timezone, and reminder preferences.
- Discovery loads 20 titles at a time until the provider is exhausted, with a 350 ms search debounce and cancellation.
- The detail cache retains 12 titles. Coil's disk cache is capped at 64 MB and its memory cache at 12% of the app's memory budget.
- An hourly WorkManager request is registered only when the user enables reminders or account auto-sync. It uses a connected-network constraint, retries transient failures with backoff, and checks the preceding six hours of releases for currently watching titles.
- Alerts are approximate, not exact alarms. No persistent service, exact-alarm permission, foreground polling, analytics or advertising SDK is added.
- Notification taps open the matching title through an explicit, immutable PendingIntent. Permission is requested on Android 13+ when enabling reminders.
- Runtime statistics use only known AniList durations. They are local tracked runtime, not verified viewing time.

## Spoilers and parity

AniList has no dependable episode-by-episode spoiler boundaries. Protection conservatively hides descriptions, cast and biographies. Reveals are scoped to the current title/person visit and reset when protection changes.

This is a native parity preview. OP/ED credits, paginated cast, profiles, advanced filters, quiz, constellation, Wrapped, web/native backup conversion, chibi notices and appearance controls are implemented. See [PARITY.md](PARITY.md) for limits. Trailers offer an opt-in inline player and external YouTube fallback.

Default sign-in uses the website handoff with independent browser/app states; optional custom native clients use `astro://oauth/anilist`. Guest data is never silently uploaded to a newly signed-in account.

The `Android device checks` workflow exercises v1→v2 migration, concurrent sync acknowledgements and primary navigation on an Android 12 emulator. Successful CI is separate from live account testing and physical-device battery measurements.

## Device acceptance before release

- Android 12 and Android 13+: install, navigate, deny/grant notification permission.
- OnePlus Nord 5: system bars, large fonts, back navigation, background restrictions.
- Network loss: collection remains available; retry restores discovery without duplicates.
- Rotate or recreate: no duplicate requests or lost saved collection edits.
- Increment at known/unknown totals; rapid taps; remove while detail is open.
- Reminders: watched/completed titles stay quiet; duplicate runs do not repeat alerts; disabling cancels work.
- Measure startup, scroll jank, idle battery and memory on a physical device before claiming efficiency improvements.
