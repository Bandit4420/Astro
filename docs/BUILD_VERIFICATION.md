# Native build verification

## Android 0.2 preview — 14 September 2026

Source: `cd60f74eedf9dcecc52f98b8457e9f29b1ba0b99`.

[Android 12 emulator checks](https://github.com/Bandit4420/Astro/actions/runs/34737224639) passed all three tests: primary navigation, Room v1→v2 data preservation and concurrent edits remaining queued after synchronization acknowledgement. Five screenshots, cold-launch output and a memory snapshot were collected in the [device evidence artifact](https://github.com/Bandit4420/Astro/actions/runs/34737224639/artifacts/10332928402). This is emulator evidence, not a physical-phone performance or battery benchmark.

The [APK build](https://github.com/Bandit4420/Astro/actions/runs/34737224594) was retried after its first queued attempt was cancelled. Publication is gated on successful unit tests, lint, APK assembly, the exact source revision and checksum/archive verification. The release workflow also checks the downloaded reports for 15 passing unit tests and zero lint errors. The final APK hash and size are recorded in the release notes.

Live native OAuth/account synchronization, notification delivery under phone power restrictions, stable signing and visual/idle-battery checks remain acceptance gates. See [PARITY.md](PARITY.md) and [ANILIST_ANDROID.md](ANILIST_ANDROID.md).

---

## Historical: first native build verification

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
