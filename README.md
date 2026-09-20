<div align="center">

<img src="https://astro-anime-bandit.bandit44.chatgpt.site/chibi/astro-mascot.webp" width="128" alt="Astro's chibi space explorer" />

# 🪐 ASTRO · Android

**Your anime. Your manga. Your orbit.**

A native Android home for your stories. Built for the small moments between episodes.

**Android 12+** · **Kotlin + Jetpack Compose** · **AniList**

[Releases](https://github.com/Bandit4420/Astro/releases) · [Web edition](https://astro-anime-bandit.bandit44.chatgpt.site) · [Builds](https://github.com/Bandit4420/Astro/actions) · [Changelog](CHANGELOG.md) · [Development](docs/DEVELOPMENT.md)

</div>

---

## Welcome to your pocket universe

Astro is moving from the web into native Android: midnight-black screens, artwork-inspired accents, quick progress updates, and notifications that belong on your phone.

**Version 1.2 — Make yourself at home.** New appearance controls, independent AMOLED and accent colors, floating navigation, readable linked biographies, voice portraits and resilient profile/schedule requests. Bandit confirmed login and two-way/automatic sync on 1.1.1; changed live-service screens and long-term release signing still need acceptance.

## Inside Astro

| Experience | What is implemented |
| :--- | :--- |
| 🏠 Mission Control | Continue watching and reading, local tracked runtime, completion counts, trending spotlight. |
| 📚 Anime · Manga · Novels | AniList search and discovery, continuous pagination. |
| ⚡ Your collection | Account-separated libraries, episode/chapter/volume tracking, ratings, private notes and conflict-aware AniList auto-sync. |
| 🎭 Beyond the cover | Title details, paginated characters, Japanese/English voice actors, detailed biographies, voiced roles and optional inline YouTube trailers. |
| 📅 Your week | Previous/next weeks, Only my anime, local/IST, countdowns and calendar export. |
| 🔔 Episode reminders | Opt-in Android notifications for currently watching titles; background scheduling and duplicate suppression. |
| 🎨 Your atmosphere | 12 presets, independent AMOLED and interface/button/navigation accents, navigation backdrop blur, typography, motion, cover radius and size. |
| 🎮 More to explore | Separate cover/clue/trivia quiz, streaks, roulettes, public profile compatibility, galaxy view and Wrapped. |
| 💾 Your data | Web/native JSON backup interchange; private notes, quiz records and history. Login tokens excluded. |
| 🛡️ Spoiler protection | Descriptions, cast and biographies hidden until you choose to reveal them. |

## Built with your battery in mind

Native **Jetpack Compose** screens, **Room** local storage, **DataStore** preferences, and **WorkManager** for opt-in background reminders. No WebView shell or permanent background service.

Artwork caches have explicit limits. Search is debounced, obsolete requests cancel, and discovery loads a page at a time. Reminder checks run roughly hourly when enabled and connected; Android may delay them to save power. Real-device measurements are still needed before claiming lower battery or memory usage. [Android background-work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent).

## Download Astro 1.2

**[Download Astro 1.2](https://github.com/Bandit4420/Astro/releases/tag/v1.2.0)** · [Release notes](docs/releases/v1.2.0.md)

Download `Astro-1.2.0.apk` from Assets. Android 12+ and private repository access are required.

This is a regular GitHub release built with the existing debug signing pipeline. Signing identities can differ between clean CI runners, so seamless upgrades are not guaranteed. Export a backup from 0.2 before installing. Version 0.1 has no export screen: do not uninstall it if you need its local progress.

To build locally:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Requires JDK 17 and Android SDK 35. See [development notes](docs/DEVELOPMENT.md) for setup, limitations and device checks.

## Native roadmap

- [x] Private repository and project overview.
- [x] Native Android 12+ scaffold and first app screens.
- [x] Public AniList browsing and local collection tracking.
- [x] Weekly schedule and opt-in system reminder implementation.
- [x] Gradle wrapper and Android CI workflow source.
- [x] Compile the first APK, pass six unit tests, and finish lint with no errors.
- [ ] Test installation, screens and reminders on Android devices.
- [x] Implement secure AniList login, account separation and durable auto-sync.
- [x] Owner verified browser sign-in, bidirectional sync and auto-sync on 1.1.1.
- [x] Port OP/ED credits, quiz, advanced filters, profile charts, chibi notices and personal tools.
- [x] Add native/web backup interchange and account-bound imports.
- [ ] Backup/export, accessibility checks and measured battery/memory profiling.
- [x] APK distribution through GitHub Releases.
- [ ] Long-term release signing for seamless updates.

## APK releases

Version 1.2 is distributed as a regular release with an APK, version information, checksum and changelog. The regular-release label does not certify the remaining phone, live-account or battery checks. Signing keys stay outside Git. Releases in this private repository require repository read access.

## Credits

Made for **Bandit**. Powered by **AniList**. Anime/manga artwork and characters belong to their respective owners. Astro is an independent project. Song credits are provided through Jikan where available. Appearance patterns draw inspiration from [Mihon](https://github.com/mihonapp/mihon) and [Dartotsu](https://github.com/aayush2622/Dartotsu). Native blur uses [Haze](https://github.com/chrisbanes/haze); rich text uses [Markwon](https://github.com/noties/Markwon), both Apache-2.0.

<div align="center">

**One story at a time. ✨**

</div>
