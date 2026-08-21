# Boris

## What This Is

Boris is a native Android reader. Paste a URL, share a link, or open http(s) in the app, and it fetches a clean markdown article for calm reading. Optional Amber or bunker login identifies you without ever holding an `nsec`. Logged-in users can highlight, archive, and listen. Web pages extract on-device.

## Core Value

Reading stays first. Login is a stored identity (npub plus Amber package or bunker connection), never a user key in Boris.

## Current State

Shipped **v1.5.0** on 2026-08-21. Phases 1–6 are done: Amber, bunker, NIP-84 highlights, TTS, nprofile links, on-device extract. The live app is `org.dergigi.boris` (Kotlin, Compose, no Hilt/Koin/Room).

## Next Milestone Goals

Pick from the 999.x backlog when ready: reading-progress sync, search (local already exists), richer Home sections, NIP-50, zaps, friends-of-friends.

## Requirements

### Validated

- ✓ Paste a URL and read the article — v1.5 (on-device extract; no `r.jina.ai`)
- ✓ Android share (`ACTION_SEND`) and open-in-app (`ACTION_VIEW`) for http(s)
- ✓ Markdown article view with Source Serif 4, reading time, open original
- ✓ Selectable article text, image gallery, share from the reader
- ✓ Amber login, persist npub, sign out, missing-signer UX — v1.5
- ✓ Bunker login (NIP-46) — v1.5
- ✓ NIP-84 highlights in the reader — v1.5
- ✓ On-device TTS with follow-along and background playback — v1.5
- ✓ `nostr:nprofile` profile links — v1.5

### Active

None. Next milestone starts with `/gsd-new-milestone`.

### Out of Scope

- Importing or storing an `nsec` in Boris
- Encrypt/decrypt, `nostrconnect://`, camera QR, Boris as a bunker server
- Full social client (profiles/feeds/zaps stay backlog 999.5)
- Watch-only npub paste, multi-account, auth as start destination

## Context

Shipped reader lives in `org.dergigi.boris`. Companion webapp is `/Users/gigi/Development/vibe/boris`.

Amber is `com.greenart7c3.nostrsigner` (NIP-55). Bunker is NIP-46. Highlights are kind 9802. Settings sync uses NIP-78.

## Constraints

- **Stack**: Kotlin, Jetpack Compose, no Hilt/Koin, no Room.
- **Versions**: Semantic versioning. 1.5.0 is this milestone ship.
- **Security**: Never log or persist private keys. Boris must not request an `nsec`.
- **Compatibility**: `minSdk` 26.
- **Placement**: New code under `app/src/main/java/org/dergigi/boris/`.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Amber / NIP-55 for Phase 1 | Native Android signer | Good |
| Follow Dark Wisp login shape | Smaller than Amethyst Quartz | Good |
| No `nsec` in Boris | Key stays in Amber or the bunker | Good |
| Missing-Amber install: Zapstore first | Boris already lives on Zapstore | Good |
| Amethyst-shaped bunker login | Dark Wisp has no NIP-46 | Good |
| On-device extract (jsoup), not Jina | Offline and no remote proxy | Good |
| TTS on-device with NIP-78 speed/language | Match the webapp | Good |
| Kotlin + Compose | Already the app stack | Good |

---
*Last updated: 2026-08-21 after v1.5 milestone*
