# Roadmap: Boris

## Overview

Boris already reads articles. This milestone adds optional identity on Home: Amber first, then bunker. Reading stays ungated.

## Phases

- [x] **Phase 1: Amber login** - Connect via Amber, persist npub, sign out, missing-signer UX
- [x] **Phase 2: Bunker login (NIP-46)** - Paste `bunker://`, persist npub, sign out, Amber path unchanged
- [ ] **Phase 3: Nostr highlights** - Create and show kind 9802 highlights in the article, like Lantern

## Phase Details

### Phase 1: Amber login

**Goal:** User can connect with Amber, see their npub after restart, and sign out, without ever giving Boris an nsec.
**Mode:** mvp
**Depends on:** Nothing (first phase)
**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, READ-01
**Success Criteria** (what must be TRUE):

  1. User can tap Connect, approve in Amber, and see their npub on Home
  2. After killing and reopening Boris, the same npub is still shown and Amber does not open again
  3. User can sign out; Home no longer shows the npub; a later Connect is a fresh Amber prompt
  4. With Amber uninstalled, Home says Amber is missing and points at Zapstore first, with F-Droid and GitHub as secondary options; Connect does not fail silently
  5. User can still paste, share, or open a URL and read while logged out

**Plans:** 1 plan

Plans:

- [x] 01-01: Amber login (manifest queries, get_public_key, session store, Home chrome)

### Phase 2: Bunker login (NIP-46)

**Goal:** User can pair a bunker from Home, see their npub after restart, and sign out, without ever giving Boris an nsec.
**Mode:** mvp
**Depends on:** Phase 1
**Requirements:** AUTH-05, AUTH-01, AUTH-02, AUTH-03, AUTH-04, READ-01
**Success Criteria** (what must be TRUE):

  1. User can paste a valid `bunker://` token (or open one via VIEW), approve the connect on the remote signer, and see their npub on Home
  2. After killing and reopening Boris, the same npub is still shown without pasting the bunker URI again
  3. User can sign out; Home no longer shows the npub; a later bunker connect is a fresh pairing
  4. Amber Connect / missing-Amber install links still work; a successful Amber or bunker login replaces the other
  5. User can still paste, share, or open a URL and read while logged out

**Plans:** 1/1 plans executed

Plans:

- [x] 02-01-PLAN.md — Bunker login (NIP-46) on Home

## Progress

**Execution Order:**
Phases execute in numeric order: 1, 2, 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Amber login | 1/1 | Complete | 2026-08-14 |
| 2. Bunker login (NIP-46) | 1/1 | Complete | 2026-08-14 |
| 3. Nostr highlights | 0/0 | Not started | |

### Phase 3: Nostr highlights

**Goal:** [To be planned]
**Requirements**: TBD
**Depends on:** Phase 2
**Plans:** 0 plans

Plans:

- [ ] TBD (run /gsd-plan-phase 3 to break down)

---
*Roadmap created: 2026-08-14*
