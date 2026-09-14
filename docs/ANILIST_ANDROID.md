# AniList Android setup

Keep the website client **50387** unchanged. The Android app needs a separate native redirect.

1. Open https://anilist.co/settings/developer as the app owner.
2. Create **Astro Android** with redirect URI **`astro://oauth/anilist`**.
3. In Astro → Profile → Set up native login, enter the new **public client ID**.
4. Tap Save & sign in, approve on AniList, then return to Astro. Do not enter a client secret.
5. Tap Sync now to import anime and manga lists. Enable Auto-sync when ready.

The implicit flow is AniList's documented approach for mobile clients and does not ship a client secret. See https://github.com/AniList/docs/blob/master/docs/guide/auth/index.md and https://github.com/AniList/docs/blob/master/docs/guide/auth/implicit.md.

A custom redirect and random state mitigate accidental/malicious callbacks. AniList does not currently offer refresh tokens in its documented authentication flow. Reauthentication is required after expiry. Tokens are stored as AES-GCM ciphertext using an Android Keystore key; platform backup is disabled.

Account libraries are separate databases (`astro-account-ID.db`). Guest progress remains in `astro.db` and is never silently uploaded to a new account. Import/export is account-bound. Private notes remain local and are excluded from AniList mutations.

Sync fetches both complete list collections, checks the authenticated Viewer, uploads dirty records with revision acknowledgements, and merges clean remote records. Changed remote progress triggers a user choice; no automatic last-write-wins. WorkManager requires connectivity and uses backoff for transient failures; the OS may delay background work.
