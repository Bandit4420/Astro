# Physical-device acceptance

These checks remain pending; this checklist is not a record of success.

## Account and sync

Use a disposable AniList account and use Profile → Sign in with AniList → Continue with AniList → Open Astro.

1. Sign in and confirm the account name/avatar and both imported lists.
2. Change episode, chapter, volume, status and rating; sync and compare exact values on AniList.
3. Make offline edits, restart Astro and reconnect. Confirm queued changes survive and upload once.
4. Change the same entry remotely and locally. Verify the conflict screen and both choices using disposable entries.
5. Edit while a sync request is in flight; verify the newer edit remains queued. Test deletion, expired/revoked tokens and cancelling browser login.
6. Switch between two test accounts and guest mode. Confirm collection, private notes, history, quiz scores and notifications remain isolated. A public profile comparison must not sign into that account.

## Backup and upgrades

Export native and website-compatible backups; confirm collection, notes, history and quiz values and absence of credentials. Restore with the matching account. Invalid and different-account backups must be rejected, and merging must preserve existing progress.

The Room migration test passed on Android 12. Upgrading an installed 0.1 APK also requires its original signing key. Do not uninstall 0.1 to resolve a signature mismatch if its local data matters; it has no export UI.

## Notifications and performance

Test notification permission granted/denied on Android 13+, settings on Android 12, one reminder per tracked release and tapping through to its title. Check account switching, sign-out, reboot and Doze. Android controls scheduling and may delay work.

Measure cold start, scrolling, memory after many covers and overnight idle battery on the same physical phone with glass on/off. Record OS, build hash, duration and network conditions. Emulator snapshots cannot establish phone battery usage.

## Visual and catalog review

Check Anime/Manga/Novels, filters and pagination past 80 titles, collection controls, local/IST schedule and calendar export. Review Japanese/English cast/actor facts, roles and pagination, inline/external trailers, OP/ED below characters with source-supplied coverage and missing-data states.

Compare dynamic accents, AMOLED, glass and blur toggles, small screens, large fonts, landscape and Android back navigation. Verify profile images and spoiler reveals, separate quiz modes, recorded-history stats, roulette, compatibility and accessible constellation navigation.
