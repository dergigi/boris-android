---
phase: 05-resolve-nostr-profile-references
plan: 02
subsystem: nostr
tags: [nostr, nprofile, hinted-relays, nip65, kind-0]

requires:
  - phase: 05-resolve-nostr-profile-references
    provides: NostrTarget.Profile.relays, NostrMentions.rewrite, ReaderLinkAction.OpenProfile
provides:
  - HintedRelays JSON persist per pubkey (D-08)
  - fetchProfile extraRelays union with HintedRelays and NIP-65 read (D-07)
  - remember at rewrite and OpenProfile tap (D-03)
affects:
  - profile-screen kind 0 fetch
  - YouViewModel relay set

actuals:
  tokens: 4624
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "JSON file store next to RelayHealth (not prefs, Room, or DataStore)"
    - "Kind 0 relays = extraRelays UNION HintedRelays.forPubkey UNION fetchRelayList.read"

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/nostr/HintedRelays.kt
    - app/src/test/java/org/dergigi/boris/nostr/HintedRelaysTest.kt
  modified:
    - app/src/main/java/org/dergigi/boris/MainActivity.kt
    - app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt
    - app/src/main/java/org/dergigi/boris/ui/you/YouViewModel.kt
    - app/src/main/java/org/dergigi/boris/data/NostrMentions.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt
    - app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt
    - app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt

key-decisions:
  - "D-08: hinted_relays.json keyed by lowercase 64-char pubkey hex, MAX_HINTS 8, MAX_ENTRIES 500"
  - "D-07: fetchProfileRemote unions extraRelays + HintedRelays.forPubkey + fetchRelayList.read; still calls fetchRelayList"
  - "Persist codec is hand-rolled JSON because org.json.JSONObject.put is stubbed on JVM unit tests"

patterns-established:
  - "Pattern: HintedRelays.remember at rewrite and at OpenProfile tap"
  - "Pattern: hints are extras, never a fake kind 10002 and never a replacement for NIP-65"

requirements-completed: [READ-03, READ-01]

coverage:
  - id: D1
    description: nprofile type-1 hints remembered per pubkey in hinted_relays.json and reloaded after init
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/HintedRelaysTest.kt#reloadsFromSameFileAfterClear
        status: pass
    human_judgment: false
  - id: D2
    description: Kind 0 fetch unions extraRelays, remembered hints, and NIP-65 read without replacing the author list
    requirement: READ-03
    verification:
      - kind: other
        ref: app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt#fetchProfileRemote
        status: pass
    human_judgment: true
    rationale: RelayQuery.query is live network; JVM tests cover HintedRelays.forPubkey and YouViewModel addAll, not the remote kind 0 round trip
  - id: D3
    description: Bad or non-wss hints drop; pubkey still stores the good wss hint
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/HintedRelaysTest.kt#rejectedUrlDroppedWhileGoodHintRemains
        status: pass
    human_judgment: false
  - id: D4
    description: OpenProfile tap remembers nprofile relays so custom-label links persist hints
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt#openProfileRemembersNprofileRelayHints
        status: pass
    human_judgment: false
  - id: D5
    description: Raw nostr:nprofile rewrite remembers type-1 hints
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt#rewriteRemembersNprofileRelayHints
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-08-18
status: complete
---

# Phase 5 Plan 02: Persist nprofile relay hints Summary

**Per-pubkey nprofile relay hints persist in hinted_relays.json and union into kind 0 / profile-screen fetches with NIP-65 read, without replacing the author list.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-08-18T16:07:17Z
- **Completed:** 2026-08-18T16:12:02Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments

- Added `HintedRelays` with sanitize, union, cap 8, pubkey cap 500, and MainActivity `hinted_relays.json` init (D-08, D-06)
- `fetchProfile` / `fetchProfileRemote` take `extraRelays` and union `HintedRelays.forPubkey` with `fetchRelayList.read` (D-07)
- `YouViewModel.refresh` adds remembered hints; rewrite and OpenProfile both call `remember` (D-03, D-08)

## Task Commits

Each task was committed atomically:

1. **Task 1: HintedRelays JSON store and MainActivity init** - `4812d86` (feat)
2. **Task 2: Union hints into fetchProfile, profile screen, rewrite, and tap** - `bc49bfb` (feat)
3. **Task 3: JVM tests for hint persist, cap, and remember call sites** - `d51de79` (test)

**Rule 3 fix:** `c5d2fa1` (fix) — persist without stubbed `org.json`

## Files Created/Modified

- `HintedRelays.kt` - per-pubkey extra relays JSON file
- `MainActivity.kt` - `HintedRelays.init(File(filesDir, "hinted_relays.json"))` after RelayHealth
- `RelayQuery.kt` - extraRelays + HintedRelays + fetchRelayList.read
- `YouViewModel.kt` - addAll(HintedRelays.forPubkey(key)) on refresh
- `NostrMentions.kt` - remember on successful nprofile decode
- `ReaderLinks.kt` - remember on OpenProfile
- `HintedRelaysTest.kt` / `NostrMentionsTest.kt` / `ReaderLinksTest.kt` - persist, cap, rewrite, tap

## Decisions Made

- JSON file analog of ReadingPositionStore, not SessionStore prefs
- Hints are extras; NIP-65 read still queried; no fake kind 10002
- Login is not required for persist or fetch
- Hand-rolled JSON encode/decode so JVM tests can reload from disk (`JSONObject.put` is mocked)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Persist without stubbed org.json**
- **Found during:** Task 3 (reload-from-file test)
- **Issue:** `org.json.JSONObject.put` throws on JVM unit tests; `runCatching` swallowed the write, so `hinted_relays.json` stayed empty and `reloadsFromSameFileAfterClear` failed
- **Fix:** Encode/decode the pubkey-to-relays JSON object by hand; still a JSON file, still `writeText`, still no Room/Hilt/DataStore
- **Files modified:** `app/src/main/java/org/dergigi/boris/nostr/HintedRelays.kt`
- **Verification:** `./gradlew --offline :app:testDebugUnitTest --tests org.dergigi.boris.nostr.HintedRelaysTest --tests org.dergigi.boris.data.NostrMentionsTest --tests org.dergigi.boris.ui.reader.ReaderLinksTest` exit 0
- **Committed in:** `c5d2fa1` (fix commit between Task 2 and Task 3)

---

**Total deviations:** 1 auto-fixed (Rule 3)
**Impact on plan:** Required for the prescribed JVM reload test. On-disk format is still a JSON pubkey-to-relays map.

## Issues Encountered

`org.json` is stubbed on Android JVM unit tests (same as Phase 3 `parseSignedEvent`). JSONObject persist could not round-trip in `HintedRelaysTest`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Phase 5 plans are executed. Mention chrome from 05-01 is unchanged. Device UAT for D-07 (kind 0 actually arriving from a hinted relay) still belongs in verify-work.

## Threat Flags

None - no new endpoints or auth paths. Hints still pass `LocalRelays.resolve`; keys are pubkey hex only; secret-key HRP still excluded (T-05-06, T-05-07, T-05-08).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `app/src/main/java/org/dergigi/boris/nostr/HintedRelays.kt`
- FOUND: `app/src/test/java/org/dergigi/boris/nostr/HintedRelaysTest.kt`
- FOUND: `4812d86`
- FOUND: `bc49bfb`
- FOUND: `c5d2fa1`
- FOUND: `d51de79`
- FOUND: `./gradlew --offline :app:assembleDebug` exit 0
- FOUND: `./gradlew --offline :app:testDebugUnitTest` (HintedRelaysTest, NostrMentionsTest, ReaderLinksTest) exit 0

