---
phase: 05-resolve-nostr-profile-references
plan: 01
subsystem: reader
tags: [nostr, nprofile, npub, markdown, mentions, nip19]

requires:
  - phase: 04-listen-to-articles
    provides: ReaderScreen markdown pipeline, TtsText paragraphs, ReaderLinks routing
provides:
  - NprofilePointer + nprofileDecode/Encode (D-06)
  - NostrTarget.Profile with relay hints (D-04, D-07, D-08 input)
  - NostrMentions.rewrite for prefixed nprofile/npub (D-01, D-02, D-03, D-09)
  - ReaderLinkAction.OpenProfile tap routing (D-04, READ-01)
  - Exhaustive Profile when arms so profile URIs never become article fetch
affects:
  - 05-02 HintedRelays persist and fetch-union

actuals:
  tokens: 9575
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Pre-process markdown after Footnotes.expand (webapp replaceNostrUrisSafely shape)"
    - "Profile HRPs require nostr: / nostr:// in entityRegex; naddr/note/nevent stay optional-prefix"
    - "OpenProfile before UrlExtractor.articleUrl"

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/data/NostrMentions.kt
    - app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt
  modified:
    - app/src/main/java/org/dergigi/boris/nostr/Nip19.kt
    - app/src/main/java/org/dergigi/boris/data/NostrLink.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt
    - app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsText.kt
    - app/src/test/java/org/dergigi/boris/nostr/Nip19Test.kt
    - app/src/test/java/org/dergigi/boris/data/NostrLinkTest.kt
    - app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt
    - app/src/test/java/org/dergigi/boris/data/UrlExtractorTest.kt
    - app/src/test/java/org/dergigi/boris/tts/TtsTextTest.kt

key-decisions:
  - "Rewrite raw prefixed mentions to [@name](nostr:…) after Footnotes.expand; do not remount markdown on kind 0"
  - "entityRegex requires scheme for nprofile/npub only; bare ids stay plaintext (D-09)"
  - "Invalid decode becomes 20-char truncated plaintext plus ellipsis, not a crash"
  - "Angle-bracket CommonMark autolinks strip <> so they still become @name links"

patterns-established:
  - "Pattern: NostrMentions.protectCode + ](url) range skip for D-03 custom labels"
  - "Pattern: NostrTarget.Profile arms skip article fetch / extract / hydrate"

requirements-completed: [READ-03, READ-01]

coverage:
  - id: D1
    description: Prefixed nostr:nprofile/npub rewrite to @name markdown links using Profile.displayName
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt#rewritesRawPrefixedNprofileToAtNameLink
        status: pass
    human_judgment: false
  - id: D2
    description: Tap OpenProfile routes nprofile/npub to in-app profile without login
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt#nprofileAndNpubOpenProfile
        status: pass
    human_judgment: true
    rationale: Device UAT on the issue 5 repro article still needed for paint color, no avatar, and in-app navigation
  - id: D3
    description: Profile URIs never become article extract or reader fetch
    requirement: READ-01
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/data/UrlExtractorTest.kt#profileUrisAreNotArticles
        status: pass
    human_judgment: false
  - id: D4
    description: nprofile TLV decode keeps pubkey when type-1 hints are garbage; relays carried on Profile
    requirement: READ-03
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/Nip19Test.kt#nprofileGarbageType1StillReturnsPubkey
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-18
status: complete
---

# Phase 5 Plan 01: Prefixed nostr profile mentions Summary

**Raw `nostr:nprofile` / `nostr:npub` in article markdown rewrite to tappable `@name` links that open `Routes.profile`, with profile URIs excluded from article fetch.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-18T15:59:07Z
- **Completed:** 2026-08-18T16:05:33Z
- **Tasks:** 2
- **Files modified:** 23

## Accomplishments

- Added `nprofileDecode` / `nprofileEncode` / `NprofilePointer` and `normalizePubkey(nprofile)` (D-06)
- Added `NostrTarget.Profile`, prefix-required profile parsing, and exhaustive Profile `when` arms (READ-01)
- Added `NostrMentions.rewrite` + `OpenProfile` routing; TTS speaks `@name` after rewrite (D-01–D-05, D-09)

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end prefixed nprofile mention to in-app profile** - `484509e` (feat)
2. **Task 2: JVM tests for decode, rewrite, routing, and extract skip** - `fa46d55` (test)

## Files Created/Modified

- `NostrMentions.kt` - rewrite raw prefixed mentions; skip code and `](url)` destinations
- `Nip19.kt` - nprofile TLV encode/decode
- `NostrLink.kt` - Profile target; scheme required for nprofile/npub
- `ReaderLinks.kt` / `ReaderScreen.kt` - OpenProfile before articleUrl
- `UrlExtractor.kt` + repository/store `when` arms - Profile is not an article
- `TtsText.kt` - rewrite after Footnotes.expand
- JVM tests for Nip19, NostrLink, NostrMentions, ReaderLinks, UrlExtractor, TtsText

## Decisions Made

- Followed D-01..D-09 from 05-CONTEXT: text-only `@name`, custom labels kept, both nprofile and npub, no nsec/bare bech32, TLV hints on `Profile.relays` for 05-02
- Invalid identifiers truncate to 20 chars + `…` (plan acceptance over RESEARCH “leave original”)
- Optional plan-checker fixture: angle-bracket `<nostr:nprofile…>` rewrite covered in NostrMentionsTest

## Deviations from Plan

None - plan executed exactly as written.

Optional extra (plan-checker): angle-bracket rewrite test added; not a blocker.

## Threat Flags

None - no new endpoints, auth paths, or trust-boundary surfaces beyond the plan threat model. Secret-key HRP remains excluded from entityRegex and PROFILE_MENTION (T-05-01).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `app/src/main/java/org/dergigi/boris/data/NostrMentions.kt`
- FOUND: `app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt`
- FOUND: `484509e`
- FOUND: `fa46d55`
- FOUND: `./gradlew --offline :app:assembleDebug` exit 0
- FOUND: `./gradlew --offline :app:testDebugUnitTest` (six test classes) exit 0
