# Changelog

## 1.0.0 — 14 September 2026

- Publish the native app as a regular GitHub release, tagged v1.0 and marked Latest.
- Set the installed app version to 1.0.0 and increment Android versionCode to 3.
- Retain the 0.2 feature set and package identity. No new behavior changes in this release.
- Build and verify the APK version, signature and checksum before publishing; additional phone and live-account tests are deferred until after release at the owner's request.
- Existing debug signing remains in use. Long-term signing, native OAuth configuration and device acceptance remain pending; export a 0.2 backup before an install that could require reinstalling.

## 0.2.0-preview — Native website workflows

- AniList browser OAuth with state validation, encrypted token storage and verified Viewer identity. Requires separate native client configuration.
- Separate guest/account libraries, durable sync queue, conflict choices, revision acknowledgements and opt-in WorkManager auto-sync.
- Chapter/volume tracking, ratings, private notes and recorded activity history.
- Combined OP/ED credits beneath paginated cast, episode coverage counts, compact staff, other works and expanded franchise connections.
- Explore genres, tags, year, season, format, status, score, episode bounds, sort and studio search.
- Profile avatars, about images, provider statistics, collection charts and monthly/yearly recaps.
- Separate anime/manga/novel quiz pools with cover/clue/trivia modes, difficulty, scores, streaks and milestones.
- Planned-title roulette, public profile compatibility and collection constellations.
- Twelve themes, glass opacity, decorative background blur, cover radius/size, artwork and image-transition controls.
- Chibi in-app notices and system notification artwork; multi-week schedule, countdowns and calendar export.
- Account-bound web/native JSON import/export for collection, notes, history and quiz records.
- Android 12 emulator workflow for navigation, migration and concurrent sync acknowledgements; policy tests expanded.

Validation and remaining gates are recorded in docs/PARITY.md. This changelog describes implemented code, not live account acceptance or production readiness.

## 0.1.0-preview — native foundation

- Native Kotlin / Jetpack Compose application for Android 12+.
- Orbit dashboard with continue tracking and local collection statistics.
- AniList anime, manga and novel discovery, search and pagination up to 80 titles.
- Title pages, first 25 cast members, character and voice actor biographies, and external trailers.
- On-device collection with status controls and atomic +1 episode/chapter tracking.
- Weekly schedule, local/IST display, and Only my anime filtering.
- Five dark themes, full-screen artwork accents, and conservative spoiler protection.
- Opt-in Android episode notifications with permission handling, scheduled background work, and deduplication.
- Bounded image/detail caches and cancellable network requests.
- Gradle wrapper, unit checks, lint and APK artifact workflow.

Account login/sync and full website feature parity are not included in this preview. Device testing and release signing are still required.
