# Requirements: Boris

**Defined:** 2026-08-14
**Core Value:** Reading stays first. Login is a stored identity (npub + signer package), never a key in Boris.

## v1 Requirements

### Identity

- [ ] **AUTH-01**: User can connect by approving a NIP-55 `get_public_key` request in Amber (or another `nostrsigner` app). Boris does not set `package` on that first intent.
- [ ] **AUTH-02**: After a successful connect, Boris stores pubkey hex and signer package, and shows the user's npub. Restarting the app still shows that npub without opening Amber again.
- [ ] **AUTH-03**: User can sign out. Boris deletes the stored pair. Amber keeps the key. The next connect is a fresh `get_public_key`.
- [ ] **AUTH-04**: If no `nostrsigner` app is installed, Boris says Amber is missing and points the user at Zapstore first (`https://zapstore.dev/apps/com.greenart7c3.nostrsigner`), with F-Droid and GitHub releases as secondary options. Connect does not fail silently.

### Reader

- [ ] **READ-01**: User can paste, share, or open a URL and read the article while logged out. Login UI sits on Home; it does not replace or block reading.

## v2 Requirements

Deferred. Not in this roadmap.

### Identity

- **AUTH-05**: User can pair a bunker (NIP-46) after Amber login works
- **AUTH-06**: User can sign a Nostr event through the stored signer when a later feature needs a signature

### Reader

- **READ-02**: User can bookmark or highlight an article on Android (needs signed events + sync)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Paste or store an `nsec` | Amber holds the key. Boris must never request a private key. |
| Bunker / NIP-46 this slice | Different protocol. After Amber login works. |
| `sign_event`, NIP-04/44, ContentResolver signing | Nothing to publish or encrypt yet. No `permissions` JSON on login. |
| Bookmarks, highlights, relays, profiles, feeds, zaps | Not a social client. Companion webapp covers some of this. |
| Amethyst Quartz signer tree | Too large. Copy Dark Wisp's login shape only. |
| Watch-only npub paste | Second login path with no signer package. |
| Multi-account | One stored pair is the whole v1 record. |
| Auth gate / login screen as start destination | Reading is the product. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Pending |
| AUTH-02 | Phase 1 | Pending |
| AUTH-03 | Phase 1 | Pending |
| AUTH-04 | Phase 1 | Pending |
| READ-01 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 5 total
- Mapped to phases: 5
- Unmapped: 0

---
*Requirements defined: 2026-08-14*
*Last updated: 2026-08-14 after roadmap creation*
