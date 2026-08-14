# Roadmap: Boris

## Overview

Boris already reads articles. This milestone adds optional Amber identity on Home: connect, see npub, sign out. One vertical phase. Reading stays ungated.

## Phases

- [ ] **Phase 1: Amber login** - Connect via Amber, persist npub, sign out, missing-signer UX (code done; device UAT pending)

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

## Progress

**Execution Order:**
Phases execute in numeric order: 1

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Amber login | 1/1 | Executed (UAT pending) | 2026-08-14 |

---
*Roadmap created: 2026-08-14*
