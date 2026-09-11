<div align="center">

<img src="https://astro-anime-bandit.bandit44.chatgpt.site/chibi/astro-mascot.webp" width="128" alt="Astro's silver-haired chibi space explorer" />

# 🪐 ASTRO · Android

**Your anime. Your manga. Your orbit.**

A native Android home for your AniList collection.

**Android 12+** · **Kotlin + Jetpack Compose** · **AniList** · **In planning**

[Explore Astro on the web](https://astro-anime-bandit.bandit44.chatgpt.site) · [The native roadmap](#native-roadmap) · [APK releases](#apk-releases)

</div>

---

## A little space for your stories

Astro brings tracking, discovery, schedules, and personal stats into one midnight-black universe. This repository is intended for its native Android edition, carrying the web experience into an app designed around Android navigation, local storage, background work, and system notifications.

**Current status:** private repository initialized; native app in product planning. The Android application, build pipeline, and first APK are not available yet. Features listed below describe the planned native app; they are not claims of completed Android functionality.

## Your orbit, on Android

| Experience | Planned native behavior |
| :--- | :--- |
| 🏠 Mission Control | Continue watching and reading, upcoming episodes, and progress at a glance. |
| 📚 Anime · Manga · Novels | Discovery, filters, title details, and episode/chapter/volume tracking. |
| 🔄 AniList sync | Verified account sign-in, durable pending edits, connection-aware retries, and protection for newer changes. |
| 🔔 Airing notifications | Android notification channels, configurable reminders, and direct links to the relevant title. |
| 🎨 Your atmosphere | Astro and AMOLED themes, artwork accents, optional glass effects, and reduced motion. |
| 🌟 Chibi moments | Astro's mascot in in-app notices, with useful actions and dismiss controls. |
| 🎭 Beyond the cover | Characters, voice actors, staff, trailers, and combined OP/ED credits with available episode coverage. |
| 📊 Your story so far | Statistics, notes, quiz progress, and collection visualizations. |

## Built with battery and memory in mind

The planned foundation is **Kotlin + Jetpack Compose**, with native screens and lifecycle-aware state. Performance is a design goal that must be measured on real devices before making improvement claims.

- **Room** for the collection and a durable queue of unsent edits.
- **DataStore** for preferences; account credentials handled separately using Android-backed protection.
- **WorkManager** for deferred sync with network constraints and exponential retry delays.
- Bounded image caching, appropriately sized artwork, and paginated discovery.
- Batched requests and event-driven updates instead of permanent polling loops.
- Optional blur and animation, with background work stopped or deferred when appropriate.

Android recommends WorkManager for persistent work and supports constraints that respect power-saving behavior. Its execution is scheduled by the system, so background sync is not an exact timer. [Android background-work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent).

## Notifications that belong on your phone

The native plan includes separate channels for airing reminders and sync issues, plus notification actions that open the relevant screen. Astro's chibi cards remain part of the in-app design; system notifications follow Android's layout and accessibility rules.

Notification permission will be requested when reminders are enabled on Android 13 and later. Reminder timing must account for device power restrictions; exact scheduling will be evaluated separately from routine sync. [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Native roadmap

- [x] Create the private GitHub repository and publish this README.
- [ ] Scaffold the native application with Android 12 / API 31 as the minimum supported version.
- [ ] Implement AniList sign-in, account separation, local storage, and the sync queue.
- [ ] Build Mission Control, collection tracking, discovery, and title details.
- [ ] Add notification channels, airing reminders, and navigation from notifications.
- [ ] Port the remaining Astro features and accessibility preferences.
- [ ] Measure startup time, scrolling, memory, network use, and idle battery impact.
- [ ] Configure CI, secure release signing, and publish the first tested APK.

## APK releases

**No APK has been released yet.**

The intended distribution channel is GitHub Releases. Each release should include:

1. A signed release APK with a version name and version code.
2. A SHA-256 checksum.
3. Release notes covering changes, supported Android versions, and known issues.

Release signing keys and passwords must stay outside the repository. Updates must preserve signing identity so existing installations can upgrade normally.

GitHub Releases can carry downloadable binaries. Access to releases in a private repository is limited to people with repository read access; public APK distribution would require a separate agreed distribution channel. [GitHub Releases documentation](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).

## Credits

Created for **Bandit**. Anime, manga, and profile data are powered by **AniList**; available song credits use **MyAnimeList data through Jikan**. Artwork and character rights belong to their respective owners. Astro is an independent project.

<div align="center">

**One story at a time. ✨**

</div>
