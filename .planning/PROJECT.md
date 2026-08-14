# Boris

## What This Is

Boris is a native Android reader. Paste a URL, share a link, or open http(s) in the app, and it fetches a clean markdown article for calm reading. The next slice adds Nostr identity through Amber, so Boris can know who you are without ever holding an `nsec`.

## Core Value

Reading stays first. Login is a stored identity (npub + signer package), never a key in Boris.

## Requirements

### Validated

- ✓ Paste a URL and read the article via Jina Reader (`r.jina.ai`) — existing
- ✓ Android share (`ACTION_SEND`) and open-in-app (`ACTION_VIEW`) for http(s) — existing
- ✓ Markdown article view with Source Serif 4, reading time, open original — existing
- ✓ Selectable article text — existing
- ✓ Image gallery (pinch-zoom, swipe, download, share) — existing
- ✓ Share the current article from the reader toolbar — existing

### Active

- [ ] Connect via Amber (NIP-55 `get_public_key`)
- [ ] Persist pubkey hex and signer package; show npub while logged in
- [ ] Sign out clears that stored identity
- [ ] If Amber is missing, say so and point the user at Zapstore first, then F-Droid / GitHub

### Out of Scope

- Bunker / NIP-46: later, after Amber login works
- Importing or storing an `nsec` in Boris: Amber holds the key
- Signing events, encrypt/decrypt, bookmarks, highlights: login only this slice
- Amethyst-sized signer stack: too large for v1; follow Dark Wisp instead
- Relays, profiles, feeds, zaps: not a social client

## Context

Shipped reader lives in `org.dergigi.boris` (`HomeScreen`, `ReaderScreen`, `ReaderRepository`). No accounts, DataStore, or DI yet. Leftover `com.readwithboris` sources exist; do not extend them.

Amber is `com.greenart7c3.nostrsigner`. Protocol is NIP-55 (`nostrsigner:` intents, then ContentResolver for remembered permissions).

Reference implementations:
- Primary: [Dark Wisp](https://github.com/barrydeen/dark-wisp-android) (`RemoteSignerBridge`, `SignerIntentBridge`, `RemoteSigner`)
- Fallback if Dark Wisp is unclear: [Amethyst](https://github.com/vitorpamplona/amethyst) (`ExternalSignerLogin`, `NostrSignerExternal`)

Dark Wisp login shape to copy:
1. `PackageManager` query for `nostrsigner:` (manifest `<queries>`)
2. One `get_public_key` intent at login; do not set `package` on that first call
3. Store pubkey + signer package; do not call `get_public_key` again while logged in
4. Sign out deletes that pair
5. Compose owns `ActivityResultLauncher`; domain code must not launch activities

v1 does not need `sign_event` or NIP-44. Keep a thin login helper, not a full `NostrSigner` interface, until something actually signs.

Companion webapp (bookmarks, highlights, Nostr) is `/Users/gigi/Development/vibe/boris`. Android stays a reading MVP plus this login.

## Constraints

- **Stack**: Kotlin, Jetpack Compose, no Hilt/Koin, no Room. Prefer DataStore or SharedPreferences for the tiny login record.
- **Versions**: Stay on `0.x.y` until 1.0.0 is explicitly requested.
- **Security**: Never log or persist private keys. Boris must not request an `nsec`.
- **Compatibility**: `minSdk` 26. Amber must be installed for login to succeed.
- **Placement**: New code under `app/src/main/java/org/dergigi/boris/` (e.g. `data/` or a small `nostr/` package). Match existing file-per-concern style.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Amber / NIP-55 only for this slice | Native Android signer; bunker later | Pending |
| Login only (npub + sign out) | Smallest useful Nostr identity | Pending |
| Follow Dark Wisp's smaller model | Clear login + intent bridge without Amethyst's KMP signer tree | Pending |
| No `nsec` in Boris | Key stays in Amber | Pending |
| Missing-Amber install: Zapstore first | Boris already lives on Zapstore; F-Droid and GitHub are backups | Pending |
| Kotlin + Compose | Already the app stack | Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? Move to Out of Scope with reason
2. Requirements validated? Move to Validated with phase reference
3. New requirements emerged? Add to Active
4. Decisions to log? Add to Key Decisions
5. "What This Is" still accurate? Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check
3. Audit Out of Scope
4. Update Context with current state

---
*Last updated: 2026-08-14 after initialization*
