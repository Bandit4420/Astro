# Website → Android parity

Version 1.1 ports the main website workflows to native Kotlin/Compose. This is a feature implementation milestone, **not a claim of production readiness or pixel-for-pixel parity**.

| Website feature | Android 1.1 implementation | Limits / verification still needed |
|---|---|---|
| AniList login | Browser OAuth implicit grant; state/age validation, Viewer verification; AES-GCM token encryption with Android Keystore | Default website login handoff requires no custom client; end-to-end account test pending |
| Account ownership | Separate Room file per AniList user, guest file retained | Public comparison never signs in |
| Auto-sync | Debounced work, hourly constrained refresh, persistent dirty records, revision acknowledgements, conflict UI, bounded retries | Android scheduling is inexact; live account tests pending |
| Discover | Anime/manga/novels, trending, search, filters, studio search, continuous pagination and quick-add | Studio filters apply after fetching each page |
| Mission Control | Greeting, separate watching/reading, runtime/completion cards, today's radar, rotating spotlight and selectable genre/tag recommendations | Manga chapter release timestamps aren't supplied by AniList |
| Collection | Search, media-type and status filters, title/score/recent sorting, episode/chapter/volume +/- controls, ratings, private notes | Initial remote imports don't fabricate historical viewing events |
| Schedule | Local/IST, previous/next weeks, Only My Anime, calendar export | Broadcast time is not streaming availability |
| Profiles | Account avatar, about images, totals, status donut, genre count/hours, score/year charts and anime/character/staff favorites | Signed-in account statistics require verified login |
| Details | Paginated main→support cast, Japanese/English voice credits, facts, biographies/works and voiced roles, combined OP/ED below cast, coverage counts, compact staff, relations, optional inline YouTube trailers with external fallback | Includes direct relations and one further connection; dates may be incomplete |
| Themes | 12 presets, dynamic accent, opacity, decorative backdrop blur, radius, cover size, image transitions, artwork toggle | Native material navigation surfaces aren't CSS backdrop filters; phone visual review pending |
| Spoiler protection | Manual reveals for synopsis, cast, biographies, relations | AniList has no reliable episode-safe boundaries |
| Quiz | Separate cover/clue/format trivia screen, difficulty, scores, streak, milestones | Metadata questions include genre, year, format and length; pools for anime, manga and novels |
| Stats / Wrapped | Status/genre/studio bars, recorded history, monthly/yearly recap, weekday patterns | App-recorded changes only; no invented historical totals |
| Roulette / compatibility | Planned titles filtered by genre/time; public account comparison and shared rating differences | Comparison percentage is explicitly an agreement heuristic |
| Constellations | Up to 240 stars, title search, zoom/scroll, genre groups, rating size, nearby-title inspector and accessible title list | Layout is bounded and static to save battery |
| Notifications | Rich chibi in-app notices and schedule action; native scheduled episode reminders | System delivery still needs phone verification; OS controls notification layout |
| Backup | Account-bound JSON collection/notes/history/quiz, web/native import/export, merge missing titles and fill empty notes only, tokens excluded | Web score conversion 0–10 ↔ 0–100; invalid/cross-account backups rejected |
| Installability | Native Android 12+ APK | PWA is replaced by native installation |

## Remaining release gates

- Test the default browser login handoff, token expiry, cross-device conflict, deletion, offline edits, sign-out and account switching against a test account.
- Establish stable release signing. CI debug keys may change between builds. Do not uninstall the first preview if its local progress matters; export/migrate before a destructive reinstall.
- Android 12 emulator navigation, Room v1→v2 migration and concurrent-sync acknowledgement tests passed on 14 September 2026. Physical-phone layout and notification checks on Android 12 and Android 13+ remain pending.
- Measure startup, scrolling, image memory budget and idle battery; compare glass on/off on OnePlus Nord 5.
- Refine visual parity and recommendations after real account/device review.

No account access, stable signing, device QA or full parity is implied solely by a successful APK build.
