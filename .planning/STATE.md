---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 06
current_phase_name: On-device article extraction
status: planning
stopped_at: Completed 06-01-PLAN.md
last_updated: "2026-08-20T07:33:08.833Z"
last_activity: 2026-08-20
last_activity_desc: Phase 6 plans written (06-01/02/03); READ-04 added
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 11
  completed_plans: 9
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-14)

**Core value:** Reading stays first. Login is a stored identity, never a user key in Boris.
**Current focus:** Phase 6 On-device article extraction — 3 plans written, ready to execute

## Current Position

Phase: 06 of 6 (On-device article extraction)
Plan: 1 of 3
Status: Planning complete — ready to execute
Last activity: 2026-08-20 — Phase 6 plans 06-01/02/03 written; READ-04 added

Progress: [████████░░] 82%

## Performance Metrics

**Velocity:**

- Total plans completed: 2
- Average duration: —
- Total execution time: —

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Amber login | 1 | 1 | — |
| 2. Bunker login (NIP-46) | 0 | 1 | — |
| 03 | 1 | - | - |

**Recent Trend:**

- Last 5 plans: 01-01 complete
- Trend: —

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 02-bunker-login-nip-46 P01 | 12min | 3 tasks | 23 files |
| Phase 03-nostr-highlights P01 | 8min | 3 tasks | 21 files |
| Phase 05 P01 | 6min | 2 tasks | 23 files |
| Phase 05 P02 | 5min | 3 tasks | 9 files |
| Phase 06 P01 | 50min | 2 tasks | 8 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Phase 1: Amber / NIP-55; Dark Wisp login shape; done and UAT'd on device
- Phase 2: Follow Amethyst bunker login (`bunker://`, connect, get_public_key). Not Quartz. Not Dark Wisp.
- Phase 2: Paste + VIEW only. No nostrconnect://, no camera QR, no sign_event
- One identity: Amber or bunker replaces the other
- [Phase ?]: Pinned secp256k1-kmp 0.22.0 because 0.24.0 is Kotlin 2.3 metadata and this app compiles with Kotlin 2.1.21
- [Phase ?]: mavenCentral() first so the ACINQ AAR resolves; Huawei mirror had the POM without the AAR
- [Phase ?]: Success wraps the pair's clientPrivkey; Connecting(prior) keeps Amber chrome; refresh skips only while the pair Job isActive
- [Phase ?]: D-08 locked: webapp NIP-84 tags (kind 9802, r, optional context, Android alt). No Lantern selector tags.
- [Phase ?]: parseSignedEvent accepts a parsed Nip01Event on the JVM because org.json is stubbed in unit tests.
- [Phase ?]: AuthViewModel handles SignerResult.Signed as a no-op so the sealed when still compiles.
- [Phase 5]: Rewrite raw prefixed nprofile/npub after Footnotes.expand; markdown keyed only on content.body
- [Phase 5]: entityRegex requires nostr scheme for nprofile/npub; bare ids stay plaintext (D-09)
- [Phase 5]: Profile.relays carries decoded nprofile hints for 05-02 persist/fetch-union
- [Phase ?]: D-08: hinted_relays.json keyed by lowercase 64-char pubkey hex, MAX_HINTS 8, MAX_ENTRIES 500
- [Phase ?]: D-07: fetchProfileRemote unions extraRelays + HintedRelays.forPubkey + fetchRelayList.read; still calls fetchRelayList
- [Phase ?]: Persist codec is hand-rolled JSON because org.json.JSONObject.put is stubbed on JVM unit tests
- [Phase ?]: Phase 6: parse falls back to full-document HtmlToMarkdown when ArticleExtractor returns null; 500-char thin-extract fail lands in 06-02
- [Phase ?]: Phase 6: BORIS_UA is a private ReaderRepository companion val; shared HttpUserAgents object arrives with the 06-02 UA retry

### Roadmap Evolution

- Phase 2 added: Bunker login (NIP-46)
- Phase 3 added: Nostr highlights
- Backlog 999.1–999.3 added: reading-progress sync, search, home settings/sections (2026-08-16)
- Backlog 999.4 added: NIP-50 relay search; 999.2 notes local-cache MVP first (2026-08-16)
- Backlog 999.5 added: zap people, articles, and highlights (2026-08-16)
- Backlog 999.6 added: friends-of-friends scope (2026-08-17)
- Phase 4 added: Listen to articles
- Phase 5 added: Resolve nostr profile references (GitHub #5)
- Phase 6 added: On-device article extraction (GitHub #54)

### Pending Todos

None yet. Future work is parked in ROADMAP.md Backlog (999.x).

### Blockers/Concerns

None yet.

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Identity | Bunker / NIP-46 | Active (Phase 2) | 2026-08-14 init |
| Identity | nostrconnect:// / camera QR | Deferred | 2026-08-14 Phase 2 discuss |
| Identity | sign_event / NIP-44 | Deferred | 2026-08-14 init |
| Reader | Bookmarks / highlights on Android | Complete (Phase 3: highlights) | 2026-08-14 init |
| Sync | Reading progress via Nostr | Backlog 999.1 | 2026-08-16 |
| Search | Local-cache MVP shipped (v0.50); NIP-50 later | Backlog 999.2 / 999.4 | 2026-08-16 |
| Home | Settings + Continue Reading / Most highlighted sections | Backlog 999.3 | 2026-08-16 |
| Zaps | Zap people, articles, and highlights | Backlog 999.5 | 2026-08-16 |
| Scope | Friends-of-friends (beyond you / friends / nostrverse) | Backlog 999.6 | 2026-08-17 |

## Session Continuity

Last session: 2026-08-20T07:33:08.825Z
Stopped at: Completed 06-01-PLAN.md
Resume file: None
