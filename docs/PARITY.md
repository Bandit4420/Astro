# Website → Android parity

The 0.2 preview ports the main website workflows to native Kotlin/Compose. This is a feature implementation milestone, **not a claim of production readiness or pixel-for-pixel parity**.

| Website feature | Android 0.2 implementation | Limits / verification still needed |
|---|---|---|
| AniList login | Browser OAuth implicit grant; state/age validation, Viewer verification; AES-GCM token encryption with Android Keystore | Owner must create native client for `astro://oauth/anilist`; end-to-end account test pending |
| Account ownership | Separate Room file per AniList user, guest file retained | Public comparison never signs in |
| Auto-sync | Debounced work, hourly constrained refresh, persistent dirty records, revision acknowledgements, conflict UI, bounded retries | Android scheduling is inexact; live account tests pending |
| Discover | Anime/manga/novels, trending, search, filters, studio search, 80-title pagination | Studio filters apply after fetching each page |
| Mission Control | Greeting, continue, runtime/completion cards, next broadcast, recommendations | Manga chapter release timestamps aren't supplied by AniList |
| Collection | Episode/chapter/volume +/- controls, ratings, status, private notes | Initial remote imports don't fabricate historical viewing events |
| Schedule | Local/IST, previous/next weeks, Only My Anime, calendar export | Broadcast time is not streaming availability |
| Profiles | Account avatar, about images, hours/episodes/chapters/volumes and genre charts | Signed-in account statistics require configured login |
| Details | Paginated main→support cast, actors, biographies/works, combined OP/ED below cast, coverage counts, compact staff, relations, trailers | Includes direct relations and one further connection; dates may be incomplete |
| Themes | 12 presets, dynamic accent, opacity, decorative backdrop blur, radius, cover size, image transitions, artwork toggle | Native material navigation surfaces aren't CSS backdrop filters; phone visual review pending |
| Spoiler protection | Manual reveals for synopsis, cast, biographies, relations | AniList has no reliable episode-safe boundaries |
| Quiz | Separate cover/clue/format trivia screen, difficulty, scores, streak, milestones | Metadata questions include genre, year, format and length; pools for anime, manga and novels |
| Stats / Wrapped | Status/genre/studio bars, recorded history, monthly/yearly recap, weekday patterns | App-recorded changes only; no invented historical totals |
| Roulette / compatibility | Planned titles filtered by genre/time; public account comparison and shared rating differences | Comparison percentage is explicitly an agreement heuristic |
| Constellations | Up to 100 stars, genre groups, rating size, shared-genre links and accessible title list | Layout is bounded and static to save battery |
| Notifications | Chibi in-app notices; native scheduled episode reminders | System delivery still needs phone verification; OS controls notification layout |
| Backup | Account-bound JSON collection/notes/history/quiz, web/native import/export, merge missing titles and fill empty notes only, tokens excluded | Web score conversion 0–10 ↔ 0–100; invalid/cross-account backups rejected |
| Installability | Native Android 12+ APK | PWA is replaced by native installation |

## Remaining release gates

- Configure the native AniList client and test login, token expiry, cross-device conflict, deletion, offline edits, sign-out and account switching against a test account.
- Establish stable release signing. CI debug keys may change between builds. Do not uninstall the first preview if its local progress matters; export/migrate before a destructive reinstall.
- Android 12 emulator navigation, Room v1→v2 migration and concurrent-sync acknowledgement tests passed on 14 September 2026. Physical-phone layout and notification checks on Android 12 and Android 13+ remain pending.
- Measure startup, scrolling, image memory budget and idle battery; compare glass on/off on OnePlus Nord 5.
- Refine visual parity and recommendations after real account/device review.

No account access, stable signing, device QA or full parity is implied solely by a successful APK build.
