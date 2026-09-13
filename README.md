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

**Status: 0.2 parity preview in verification.** The major website workflows now have native implementations. Live AniList login needs a dedicated native client ID; stable release signing and phone acceptance remain release gates. See [parity status](docs/PARITY.md).

## In this native preview

| Experience | What is implemented |
| :--- | :--- |
| 🏠 Mission Control | Continue watching and reading, local tracked runtime, completion counts, trending spotlight. |
| 📚 Anime · Manga · Novels | AniList search and discovery, paginated to 80 titles. |
| ⚡ Your collection | Account-separated libraries, episode/chapter/volume tracking, ratings, private notes and conflict-aware AniList auto-sync. |
| 🎭 Beyond the cover | Title details, paginated characters, Japanese voice actors, biographies, and external trailer links. |
| 📅 Your week | Previous/next weeks, Only my anime, local/IST, countdowns and calendar export. |
| 🔔 Episode reminders | Opt-in Android notifications for currently watching titles; background scheduling and duplicate suppression. |
| 🎨 Your atmosphere | 12 presets, artwork accents, glass opacity, decorative backdrop blur, cover radius and size. |
| 🎮 More to explore | Separate cover/clue/trivia quiz, streaks, roulettes, public profile compatibility, galaxy view and Wrapped. |
| 💾 Your data | Web/native JSON backup interchange; private notes, quiz records and history. Login tokens excluded. |
| 🛡️ Spoiler protection | Descriptions, cast and biographies hidden until you choose to reveal them. |

## Built with your battery in mind

Native **Jetpack Compose** screens, **Room** local storage, **DataStore** preferences, and **WorkManager** for opt-in background reminders. No WebView shell or permanent background service.

Artwork caches have explicit limits. Search is debounced, obsolete requests cancel, and discovery loads a page at a time. Reminder checks run roughly hourly when enabled and connected; Android may delay them to save power. Real-device measurements are still needed before claiming lower battery or memory usage. [Android background-work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent).

## Try a development build

**[Download the verified preview APK ZIP](https://github.com/Bandit4420/Astro/actions/runs/34679645728/artifacts/10293957109)** · [Build verification](docs/BUILD_VERIFICATION.md)

Open [GitHub Actions](https://github.com/Bandit4420/Astro/actions), select a **successful Android preview run**, and download its APK artifact. Extract the ZIP to find `app-debug.apk` and its SHA-256 checksum. Private repository access is required. The presence of workflow source alone does not mean a successful APK is available.

These are debug builds with temporary signing identities; a later build may require uninstalling the old one and losing its local data. Backups are implemented in 0.2. Version 0.1 has no export screen: do not uninstall it if you need its local data. Keep your main collection on AniList while live sync validation is pending.

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
- [ ] Configure native OAuth client and verify against a live test account.
- [x] Port OP/ED credits, quiz, advanced filters, profile charts, chibi notices and personal tools.
- [x] Add native/web backup interchange and account-bound imports.
- [ ] Backup/export, accessibility checks and measured battery/memory profiling.
- [ ] Stable release signing and APK distribution through GitHub Releases.

## APK releases

**No production release yet.** Stable releases will include a signed APK, version information, checksum and changelog. Signing keys stay outside Git. Releases in this private repository are available only to people with repository read access. [About GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).

## Credits

Made for **Bandit**. Powered by **AniList**. Anime/manga artwork and characters belong to their respective owners. Astro is an independent project. The website's song credits use Jikan; those credits are not yet ported to this native preview.

<div align="center">

**One story at a time. ✨**

</div>
