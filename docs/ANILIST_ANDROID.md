# AniList Android login

Since 1.0.1, normal login requires no user-provided client ID.

Open Profile → Sign in with AniList → Continue with AniList in the browser → authorize → Open Astro → Sync now. Auto-sync can then be enabled.

Astro starts https://astro-anime-bandit.bandit44.chatgpt.site/auth/android with a random app state in the URL fragment. The browser creates a separate tab-bound OAuth state and uses the website's existing AniList client. Its registered callback validates that browser state and returns only to the fixed astro://oauth/anilist endpoint. The app validates its own single-use state and expiry and verifies AniList Viewer before saving a session. Tokens are not sent in HTTP query strings or stored in a web session by the Android handoff. They are encrypted with Android Keystore on the phone.

The website handoff must stay available. Website client 50387 and its registered redirect are unchanged. No client secret is shipped.

For a separately registered Android client, Advanced login settings still supports a public client ID with redirect astro://oauth/anilist. This is optional.

Account libraries remain separate; guest data is retained, public profile lookups do not sign in, and private notes are excluded from AniList mutations. Live account and phone verification remain necessary.
