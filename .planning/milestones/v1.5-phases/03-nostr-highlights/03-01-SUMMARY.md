---
phase: 03-nostr-highlights
plan: 01
subsystem: nostr
tags: [nip-84, nip-55, nip-46, nip-65, compose, highlights]

requires:
  - phase: 02-bunker-login-nip-46
    provides: Session Amber/Bunker, SecretBox, BunkerClient pair, RelaySocket
  - phase: 01-amber-login
    provides: RemoteSignerBridge get_public_key, SignerResults.parse, reader SelectionContainer
provides:
  - Own NIP-84 kind 9802 create/fetch/paint in the reader
  - Amber sign_event and bunker signEvent on the stored Session
  - ArticleUrl.normalize, QuoteMatch, RelayList, RelayQuery
affects: [reader, home-copy-marks]

actuals:
  tokens: 15454
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - Custom LocalTextToolbar Copy plus logged-in Highlight
    - Shared HighlightMarks alpha drawer on Home and reader
    - Sign then paint then publish; highlight IO never blocks Ready

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/data/ArticleUrl.kt
    - app/src/main/java/org/dergigi/boris/nostr/Nip84.kt
    - app/src/main/java/org/dergigi/boris/nostr/QuoteMatch.kt
    - app/src/main/java/org/dergigi/boris/nostr/RelayList.kt
    - app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/HighlightTextToolbar.kt
    - app/src/test/java/org/dergigi/boris/data/ArticleUrlTest.kt
    - app/src/test/java/org/dergigi/boris/nostr/Nip84Test.kt
    - app/src/test/java/org/dergigi/boris/nostr/QuoteMatchTest.kt
    - app/src/test/java/org/dergigi/boris/nostr/RelayListTest.kt
  modified:
    - app/src/main/java/org/dergigi/boris/nostr/RemoteSignerBridge.kt
    - app/src/main/java/org/dergigi/boris/nostr/SignerResult.kt
    - app/src/main/java/org/dergigi/boris/nostr/BunkerClient.kt
    - app/src/main/java/org/dergigi/boris/nostr/Nip01Event.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt
    - app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt
    - app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/dergigi/boris/nostr/SignerResultTest.kt

key-decisions:
  - "D-08 locked: webapp NIP-84 tags (kind 9802, r, optional context, Android alt). No Lantern selector tags."
  - "parseSignedEvent accepts a parsed Nip01Event on the JVM because org.json is stubbed in unit tests."
  - "AuthViewModel handles SignerResult.Signed as a no-op so the sealed when still compiles."

patterns-established:
  - "Pattern 1: HighlightTextToolbar via LocalTextToolbar around ArticleBody SelectionContainer"
  - "Pattern 2: HighlightMarks.highlightRects plus HighlightMarkAlpha 0.45 on Home and reader"
  - "Pattern 3: Sign through stored Session, paint, then publish on IO"

requirements-completed: [HIGH-01, AUTH-06, READ-01]

coverage:
  - id: D1
    description: ArticleUrl.normalize collapses www, http, trailing slash, query, and fragment
    requirement: HIGH-01
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/data/ArticleUrlTest.kt#normalizeCollapsesAllDecorations
        status: pass
    human_judgment: false
  - id: D2
    description: NIP-84 tags are r, optional context, and locked Android alt
    requirement: HIGH-01
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/Nip84Test.kt#createdTagsExcludeLanternSelectors
        status: pass
    human_judgment: false
  - id: D3
    description: QuoteMatch finds exact hits and two ranges for duplicate quotes
    requirement: HIGH-01
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/QuoteMatchTest.kt#occurrencesReturnsTwoRangesForTwoIdenticalQuotes
        status: pass
    human_judgment: false
  - id: D4
    description: RelayList parses NIP-65 read/write markers and falls back to the four hardcoded relays
    requirement: HIGH-01
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/RelayListTest.kt#emptyListUsesFallbackRelays
        status: pass
    human_judgment: false
  - id: D5
    description: parseSignedEvent accepts a verified kind 9802 and rejects cancel, reject, wrong kind, and bad sig
    requirement: AUTH-06
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/SignerResultTest.kt#parseSignedEventAcceptsValidHighlight
        status: pass
    human_judgment: false
  - id: D6
    description: Logged-in Highlight signs through Amber or bunker, paints the quote, and fetches own 9802s
    requirement: HIGH-01
    verification:
      - kind: other
        ref: ./gradlew :app:assembleDebug
        status: pass
    human_judgment: true
    rationale: Device UAT needs Amber or a live bunker session and network to confirm paint after sign and after relaunch
  - id: D7
    description: Logged-out reader still reads with no Highlight item and no marks; relay miss does not block Ready
    requirement: READ-01
    verification:
      - kind: other
        ref: ./gradlew :app:assembleDebug
        status: pass
    human_judgment: true
    rationale: Ready-without-relays is a device/network behavior; unit tests cannot open Jina plus relays together

duration: 8min
completed: 2026-08-14
status: complete
---

# Phase 3 Plan 01: Own NIP-84 highlights Summary

**Logged-in select → Highlight → Amber or bunker sign_event → kind 9802 paint, with own 9802s fetched per URL and reading still ungated**

## Performance

- **Duration:** 8 min
- **Started:** 2026-08-14T22:17:56Z
- **Completed:** 2026-08-14T22:25:30Z
- **Tasks:** 3
- **Files modified:** 21

## Accomplishments

- Reader toolbar adds Highlight next to Copy when a Session exists; logged out stays Copy-only
- Kind 9802 uses webapp tags (`r`, optional `context`, Android `alt`) and signs through Amber `sign_event` or bunker `signEvent`
- Own highlights paint via shared `HighlightMarks` alpha; load still reaches Ready without waiting on relays

## Task Commits

Each task was committed atomically:

1. **Task 1: Confirm NIP-84 event shape** - approved as `option-webapp-nip84` (D-08); no code commit
2. **Task 2: End-to-end own highlights in the reader** - `9674c1a` (feat)
3. **Task 2 follow-up: context punctuation + JVM signed parse** - `d120b51` (fix)
4. **Task 3: JVM tests for URL, tags, match, relays, and signed extras** - `4030fbd` (test)

**Plan metadata:** pending docs commit

## Files Created/Modified

- `app/src/main/java/org/dergigi/boris/data/ArticleUrl.kt` - D-04 URL normalize for `r` match
- `app/src/main/java/org/dergigi/boris/nostr/Nip84.kt` - kind 9802 tags, unsigned JSON, context slice
- `app/src/main/java/org/dergigi/boris/nostr/QuoteMatch.kt` - exact quote occurrences
- `app/src/main/java/org/dergigi/boris/nostr/RelayList.kt` - NIP-65 parse and fallback relays
- `app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt` - REQ/EVENT/EOSE/OK on RelaySocket
- `app/src/main/java/org/dergigi/boris/nostr/RemoteSignerBridge.kt` - `buildSignEventIntent`
- `app/src/main/java/org/dergigi/boris/nostr/SignerResult.kt` - `Signed` and `parseSignedEvent`
- `app/src/main/java/org/dergigi/boris/nostr/BunkerClient.kt` - `signEvent` plus `BunkerSignResult`
- `app/src/main/java/org/dergigi/boris/nostr/Nip01Event.kt` - `KIND_HIGHLIGHT` and `KIND_RELAY_LIST`
- `app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt` - shared alpha marker drawer
- `app/src/main/java/org/dergigi/boris/ui/reader/HighlightTextToolbar.kt` - Copy plus logged-in Highlight
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` - AndroidViewModel sign/paint/publish/fetch
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` - reader launcher, toolbar, marks
- `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt` - Home copy uses HighlightMarks alpha
- `app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt` - exhaustive `SignerResult.Signed` branch
- `app/src/main/res/values/strings.xml` - highlight action and toast copy
- JVM tests for ArticleUrl, Nip84, QuoteMatch, RelayList, and signed extras

## Decisions Made

- D-08: publish webapp NIP-84 bytes (kind 9802, content = quote, `r` = opened URL, optional context, Android alt). Not Lantern selector tags.
- `parseSignedEvent` has a `Nip01Event` overload so JVM tests can cover verify/kind/pubkey without `org.json` (Android stubs that class).
- One unused `SignerResult.Signed` branch in AuthViewModel so login `when` stays exhaustive.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Context slice dropped sentence punctuation**
- **Found during:** Task 3
- **Issue:** Kotlin `Regex.split` does not keep capture groups, so `extractContext` joined sentences without periods
- **Fix:** Split while keeping delimiter matches, then reattach punctuation like the webapp
- **Files modified:** `app/src/main/java/org/dergigi/boris/nostr/Nip84.kt`
- **Verification:** `Nip84Test.extractContextUsesNeighborSentences` passes
- **Committed in:** `d120b51`

**2. [Rule 3 - Blocking] `org.json.JSONObject` is stubbed on JVM unit tests**
- **Found during:** Task 3
- **Issue:** `Nip01Event.toJsonString()` / `JSONObject.put` throw `Method put in org.json.JSONObject not mocked`
- **Fix:** `parseSignedEvent` validates a parsed `Nip01Event`; Intent/JSON path still uses `JSONObject` on device
- **Files modified:** `app/src/main/java/org/dergigi/boris/nostr/SignerResult.kt`, `SignerResultTest.kt`
- **Verification:** signed-event unit tests pass; login parse cases stay green
- **Committed in:** `d120b51`, `4030fbd`

**3. [Rule 3 - Blocking] Sealed `SignerResult.Signed` broke AuthViewModel**
- **Found during:** Task 2
- **Issue:** Plan forbids editing AuthViewModel, but adding `Signed` makes the login `when` non-exhaustive
- **Fix:** Empty `is SignerResult.Signed` branch; login parse never returns it
- **Files modified:** `app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt`
- **Verification:** `:app:assembleDebug` exits 0
- **Committed in:** `9674c1a`

---

**Total deviations:** 3 auto-fixed (1 bug, 2 blocking)
**Impact on plan:** Needed for correct context bytes, JVM tests, and compile. No scope creep.

## Issues Encountered

- Compose `ClipboardManager` / `LocalClipboardManager` are deprecated on this BOM; left as planned (no BOM bump).
- Task 3 is `tdd="true"` after the tracer already shipped the parsers, so RED was not a failing-first cycle.

## User Setup Required

Device UAT still needs:

- Amber installed for the Highlight `sign_event` path
- A live bunker session for the bunker sign path
- Network so kind 10002 and kind 9802 can reach the fallback relays

## Next Phase Readiness

- HIGH-01 / AUTH-06 / READ-01 are implemented in code; device UAT is the remaining gate
- No further plans in this phase

---
*Phase: 03-nostr-highlights*
*Completed: 2026-08-14*

## Self-Check: PASSED
