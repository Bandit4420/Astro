# First native build verification

Verified on 12 September 2026.

- Source commit: `149b6589aa0b2b995f17548b4b41ffdfc8b7d546`.
- [Successful GitHub Actions run](https://github.com/Bandit4420/Astro/actions/runs/34679645728).
- [APK and checksum ZIP](https://github.com/Bandit4420/Astro/actions/runs/34679645728/artifacts/10293957109).
- [Test and lint reports](https://github.com/Bandit4420/Astro/actions/runs/34679645728/artifacts/10293532752).

## Results

`testDebugUnitTest`, `lintDebug` and `assembleDebug` succeeded. All six unit tests passed. Lint reported zero errors and 15 advisory warnings, mainly dependency updates plus target API, backup rules, KAPT and Kotlin API suggestions. No warnings were suppressed to pass the build.

The downloaded APK passed ZIP integrity checks and its SHA-256 matched the workflow checksum:

```text
b393d8dc805aa96fab12183ac16b2764e1df1ebe2c2467ffeef2c1e37bac98d6
```

APK size: 31,435,558 bytes. This is an unminified universal debug build for Android 12+ (`com.bandit.astro.preview`), not the size of a future optimized release. It includes ARM64, ARMv7, x86 and x86_64 native libraries from dependencies.

The actual Room increment, completion and decrement SQL was also exercised locally against SQLite: known totals clamp, unknown totals advance, completion fills a known total, decrement reopens completed tracking, and progress never drops below zero. API field names were cross-checked against AniList's published reference.

## Still unverified

The app has not been installed on a physical device or emulator in this session. Live API connectivity from the phone, runtime layout, notification delivery under Android power restrictions, and battery/memory performance need device testing. Account login and sync are not implemented in this preview.

GitHub artifacts expire after 14 days. Repository access is required to download. Each clean CI runner uses a temporary debug signing key, so installing a later preview may require an uninstall and erase local tracking data. Stable release signing and backup/export remain required before daily-use migration.
