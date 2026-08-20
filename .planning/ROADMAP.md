# Roadmap: Boris

## Overview

Boris already reads articles. This milestone adds optional identity on Home (Amber, then bunker) and NIP-84 highlights in the reader. Reading stays ungated.

## Phases

- [x] **Phase 1: Amber login** - Connect via Amber, persist npub, sign out, missing-signer UX
- [x] **Phase 2: Bunker login (NIP-46)** - Paste `bunker://`, persist npub, sign out, Amber path unchanged
- [x] **Phase 3: Nostr highlights** - Create and show kind 9802 highlights in the article, like Lantern (completed 2026-08-16)
- [ ] **Phase 4: Listen to articles** - Speak the current article from the reader
- [ ] **Phase 5: Resolve nostr profile references** - Turn `nostr:nprofile` mentions in articles into profile links
- [ ] **Phase 6: On-device article extraction** - Fetch web HTML on-device and extract readable content without `r.jina.ai`

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
Phases execute in numeric order: 1, 2, 3, 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Amber login | 1/1 | Complete | 2026-08-14 |
| 2. Bunker login (NIP-46) | 1/1 | Complete | 2026-08-14 |
| 3. Nostr highlights | 1/1 | Complete    | 2026-08-16 |
| 4. Listen to articles | 3/3 | Verifying | |
| 5. Resolve nostr profile references | 2/2 | In Progress|  |
| 6. On-device article extraction | 1/3 | In Progress|  |

### Phase 3: Nostr highlights

**Goal:** Logged-in user can create a NIP-84 highlight from selected article text and see their highlights for that URL painted in the reader, without an nsec in Boris.
**Mode:** mvp
**Depends on:** Phase 2
**Requirements:** HIGH-01, AUTH-06, READ-01
**Success Criteria** (what must be TRUE):

  1. Logged-in user can select title or body text, tap Highlight next to Copy, approve in Amber or bunker, and see the quote marked in the article
  2. After killing and reopening Boris while still logged in, opening the same article still shows those highlights
  3. Logged out, the article looks like today: no marks, no Highlight action, reading still works
  4. A highlight created on the webapp for the same URL (same npub) paints on Android when the quote is in the fetched body
  5. Relays failing does not block reading

**Plans:** 1/1 plans complete

Plans:

- [x] 03-01-PLAN.md — NIP-84 highlights in the reader

### Phase 4: Listen to articles

**Goal:** User can listen to the currently open article with on-device TTS, including background playback, webapp-matched settings, and follow-along in the reader.
**Depends on:** Phase 3
**Requirements:** TTS-01, READ-01
**Success Criteria** (what must be TRUE):

  1. User can play, pause, and stop the current article from the reader top bar
  2. Speech continues with the screen off and while browsing other screens; lock-screen / notification can play, pause, stop, and skip paragraph
  3. Play on a different article switches; browsing without play does not interrupt
  4. Settings match the webapp TTS section (speed, language, preview sentence) and sync via existing NIP-78 keys
  5. Follow-along highlights and auto-scrolls the spoken paragraph, on by default, disableable; play starts near the saved reading position
  6. Listening works logged out; reading still works if TTS is missing

**Plans:** 3 plans

Plans:
**Wave 1**

- [x] 04-01-PLAN.md — Listen from the reader through on-device TTS and a media foreground service (2026-08-18)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 04-02-PLAN.md — Webapp-matched TTS settings with NIP-78 sync and preview (2026-08-18)
- [x] 04-03-PLAN.md — Follow-along mark and in-app mini player (2026-08-18)

### Phase 5: Resolve nostr profile references

**Goal:** `nostr:nprofile` references in article Markdown render as tappable profile links that open the in-app profile screen, without breaking existing note, event, and article nostr links.
**Depends on:** Phase 4
**Requirements:** READ-03, READ-01
**Success Criteria** (what must be TRUE):

  1. `nostr:nprofile...` in article Markdown renders as a profile link, not raw plaintext
  2. Tapping the link opens the in-app profile screen for that pubkey
  3. Relay hints in `nprofile` are parsed without breaking the profile target
  4. Existing note, event, and article nostr links keep working
  5. Reading still works logged out

**Plans:** 2/2 plans executed

Plans:

- [x] 05-01-PLAN.md — Decode nprofile/npub, rewrite raw prefixed mentions to @name, route OpenProfile, skip article fetch
- [x] 05-02-PLAN.md — Persist nprofile relay hints and union them into profile fetches

## Backlog

### Phase 999.1: Sync reading progress via Nostr (BACKLOG)

**Goal:** Reading progress currently lives only on-device; sync it over Nostr so progress follows the user across devices.
**Requirements:** TBD (spec choice still open — local-first today; evaluate existing NIPs vs a Boris-specific approach)
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.2: Implement search (BACKLOG)

**Goal:** In-app search for highlights, writings, bookmarks, and related content.
**Notes:** Local-cache MVP ships first (search EventCache only). Relay-backed discovery is Phase 999.4.
**Requirements:** TBD
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.3: Home screen settings and sections (BACKLOG)

**Goal:** Proper Home settings, plus richer Home sections such as Continue Reading and Most highlighted this week (and room for more).
**Requirements:** TBD
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.4: NIP-50 relay search (BACKLOG)

**Goal:** Extend search beyond the local EventCache using NIP-50 (`search` filter field) on supporting relays.
**Requirements:** TBD
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.5: Zap people, articles, and highlights (BACKLOG)

**Goal:** Let users zap people, articles, and highlights from within Boris (Lightning / NIP-57), without turning the app into a full social client.
**Requirements:** TBD
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.6: Friends-of-friends scope (BACKLOG)

**Goal:** Extend you / friends / nostrverse with a friends-of-friends level (follow-of-follow), for highlights, feeds, and related coloring/visibility.
**Requirements:** TBD
**Plans:** 0 plans

Plans:

- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 6: On-device article extraction

**Goal:** Ordinary http(s) articles load by fetching the page on-device and extracting readable Markdown locally, so reading does not depend on `r.jina.ai` being available or authenticated. Nostr and RSS paths stay as they are. Offline cache still works. GitHub issue #54.
**Requirements**: READ-04, READ-01
**Depends on:** Phase 5
**Plans:** 1/3 plans executed

Plans:
**Wave 1**

- [x] 06-01-PLAN.md — Origin fetch plus on-device extract tracer

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 06-02-PLAN.md — Jina-like Markdown quality and thin-extract bar

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 06-03-PLAN.md — UA retry, two reader errors, origin cache, leftover cleanup

---
*Roadmap created: 2026-08-14*
