# Requirements: Boris

**Defined:** 2026-08-14
**Core Value:** Reading stays first. Login is a stored identity (npub + signer package), never a key in Boris.

## v1 Requirements

### Identity

- [x] **AUTH-01**: User can connect by approving a NIP-55 `get_public_key` request in Amber (or another `nostrsigner` app). Boris does not set `package` on that first intent.
- [x] **AUTH-02**: After a successful connect, Boris stores pubkey hex and signer package, and shows the user's npub. Restarting the app still shows that npub without opening Amber again.
- [x] **AUTH-03**: User can sign out. Boris deletes the stored pair. Amber keeps the key. The next connect is a fresh `get_public_key`.
- [x] **AUTH-04**: If no `nostrsigner` app is installed, Boris says Amber is missing and points the user at Zapstore first (`https://zapstore.dev/apps/com.greenart7c3.nostrsigner`), with F-Droid and GitHub releases as secondary options. Connect does not fail silently.
- [x] **AUTH-05**: User can pair a bunker by pasting a `bunker://` token (or opening one via VIEW). Boris connects as a NIP-46 client, stores the user pubkey, and shows npub. Amber login still works. Boris never holds an `nsec`.

### Reader

- [x] **READ-01**: User can paste, share, or open a URL and read the article while logged out. Login UI sits on Home; it does not replace or block reading.
- [x] **AUTH-06**: User can sign a kind 9802 highlight through the stored session (Amber NIP-55 `sign_event` or bunker NIP-46 `sign_event`). Boris never holds an `nsec`.
- [x] **HIGH-01**: While logged in, the user can select article text, publish a NIP-84 highlight, and see their highlights for that URL painted in the reader. Logged out, the reader is unchanged.

## v2 Requirements

Deferred. Not in this roadmap.

### Reader

- **READ-02**: User can bookmark an article on Android (needs signed events + sync)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Paste or store an `nsec` | Amber holds the key. Boris must never request a private key. |
| Bunker as a signing server | Amber / nsec.app hold the key. Boris is the client. |
| `sign_event` for anything except kind 9802; NIP-04/44 | Highlights are the only signed event this slice. |
| Bookmarks, profiles, feeds, zaps | Not a social client. Companion webapp covers some of this. |
| Amethyst Quartz signer tree | Too large. Copy Dark Wisp's login shape only. |
| Watch-only npub paste | Second login path with no signer package. |
| Multi-account | One stored pair is the whole v1 record. |
| Auth gate / login screen as start destination | Reading is the product. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Implemented |
| AUTH-02 | Phase 1 | Implemented |
| AUTH-03 | Phase 1 | Implemented |
| AUTH-04 | Phase 1 | Implemented |
| READ-01 | Phase 1 | Implemented |
| AUTH-05 | Phase 2 | Complete |
| AUTH-06 | Phase 3 | Complete |
| HIGH-01 | Phase 3 | Complete |

**Coverage:**

- v1 requirements: 8 total
- Mapped to phases: 8
- Unmapped: 0

---
*Requirements defined: 2026-08-14*
*Last updated: 2026-08-14 after promoting AUTH-06 and HIGH-01 to Phase 3*
