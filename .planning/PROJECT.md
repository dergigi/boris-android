# Boris

## What This Is

Boris is a native Android reader. Paste a URL, share a link, or open http(s) in the app, and it fetches a clean markdown article for calm reading. The next slice adds optional bunker identity (`bunker://` / NIP-46) beside Amber, so Boris can know who you are when the key lives on another device. Boris never holds an `nsec`.

## Core Value

Reading stays first. Login is a stored identity (npub plus Amber package or bunker connection), never a user key in Boris.

## Requirements

### Validated

- ✓ Paste a URL and read the article via Jina Reader (`r.jina.ai`) — existing
- ✓ Android share (`ACTION_SEND`) and open-in-app (`ACTION_VIEW`) for http(s) — existing
- ✓ Markdown article view with Source Serif 4, reading time, open original — existing
- ✓ Selectable article text — existing
- ✓ Image gallery (pinch-zoom, swipe, download, share) — existing
- ✓ Share the current article from the reader toolbar — existing

### Active

- [x] Connect via Amber (NIP-55 `get_public_key`)
- [x] Persist pubkey hex and signer package; show npub while logged in
- [x] Sign out clears that stored identity
- [x] If Amber is missing, say so and point the user at Zapstore first, then F-Droid / GitHub
- [ ] Pair a bunker (`bunker://` / NIP-46) and show npub

### Out of Scope

- Importing or storing an `nsec` in Boris
- Signing events, encrypt/decrypt, bookmarks, highlights
- `nostrconnect://`, camera QR, Boris as a bunker server
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
- **Compatibility**: `minSdk` 26. Amber login needs a `nostrsigner` app. Bunker login needs a `bunker://` token and network.
- **Placement**: New code under `app/src/main/java/org/dergigi/boris/` (e.g. `data/` or a small `nostr/` package). Match existing file-per-concern style.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Amber / NIP-55 only for Phase 1 | Native Android signer; bunker later | Done |
| Login only (npub + sign out) | Smallest useful Nostr identity | Done |
| Follow Dark Wisp's smaller model | Amber login without Amethyst's KMP signer tree | Done |
| No `nsec` in Boris | Key stays in Amber or the bunker | Done |
| Missing-Amber install: Zapstore first | Boris already lives on Zapstore; F-Droid and GitHub are backups | Done |
| Follow Amethyst bunker login for Phase 2 | Dark Wisp has no NIP-46; copy `BunkerLoginUseCase` shape, not Quartz | Pending |
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
