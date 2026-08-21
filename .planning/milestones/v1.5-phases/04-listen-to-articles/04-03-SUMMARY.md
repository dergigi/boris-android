---
phase: 04-listen-to-articles
plan: 03
subsystem: ui
tags: [tts, compose, follow-along, mini-player, reader]

requires:
  - phase: 04-01
    provides: TtsPlayback session StateFlow (url/index/paragraphs/playing/followAlongPaused, setFollowAlongPaused/setRate/skip/pause/resume), TtsSpeed presets/cycle, UserSettings.ttsFollowAlong, tts_* strings, reader top-bar Listen
  - phase: 04-02
    provides: TtsSection speed chip pattern and ttsDefaultSpeed write path via SettingsViewModel.update
provides:
  - SpokenMark color token and PaintedHighlight.spoken flag
  - Spoken fill paint pass in HighlightMarks at FindMarkAlpha (filled, never underline)
  - ArticleFind.SPOKEN_ID + paintedSpoken transient mark (never published)
  - Follow-along auto-scroll in ReaderScreen with followAlongScrolling guard and D-15 user-scroll pause
  - TtsMiniPlayer + TtsMiniPlayerHost slim 56dp in-app player with speed cycle
  - BorisApp wiring: mini player above BorisBottomBar on tabs, overlay above gesture insets off-tab, hidden on the speaking article
affects: [phase-4 verification, release UAT]

actuals:
  tokens: 5400
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Transient reader mark: SPOKEN_ID PaintedHighlight appended outside visibleFor, same as Find"
    - "Programmatic-scroll guard: followAlongScrolling flag around animateScrollTo so isScrollInProgress only pauses follow-along on real user scrolls"
    - "Mini player host collects TtsPlayback.session itself and keeps the last session for the 200ms fade-out"

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/ui/shell/TtsMiniPlayer.kt
  modified:
    - app/src/main/java/org/dergigi/boris/ui/theme/Color.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ArticleFind.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt
    - app/src/main/java/org/dergigi/boris/ui/BorisApp.kt

key-decisions:
  - "User-scroll detection uses scrollState.isScrollInProgress gated by followAlongScrolling and positionRestored, so auto-scroll and the reading-position restore never count as user scrolls (D-15)"
  - "TtsPlayback.resume() clears followAlongPaused so play resumes auto-scroll from any surface, including the notification"
  - "Mini-player visibility compares the reader route url in raw, normalized, and nostr-uri form against session.url because content.url is canonicalized during fetch"
  - "Live settings.ttsFollowAlong governs the mark (not the session snapshot), so toggling the checkbox mid-playback takes effect immediately (D-13)"

patterns-established:
  - "Spoken pass paints first and NIP-84 passes exclude it.spoken, keeping highlight colors intact on overlap"

requirements-completed: [TTS-01, READ-01]

coverage:
  - id: D1
    description: "Spoken-paragraph follow-along: teal SpokenMark fill at 0.38f on the current paragraph, painted even with showHighlights off, auto-scroll via HighlightJump; user scroll pauses auto-scroll only; checkbox off removes mark and scroll; no highlight event published"
    requirement: TTS-01
    verification:
      - kind: other
        ref: "./gradlew --offline :app:assembleDebug + acceptance greps (SpokenMark/0xFF2DD4BF in Color.kt; spoken: Boolean in ReaderViewModel.kt; it.spoken in HighlightMarks.kt; SPOKEN_ID in ArticleFind.kt; followAlong in ReaderScreen.kt; non-comment Nip84 count 0 in follow-along files)"
        status: pass
    human_judgment: true
    rationale: "On-screen fill, auto-scroll feel, and D-15 scroll-pause behavior need a device with a TTS engine; device verification is deferred to 1.4.0 release testing per user decision at the checkpoint"
  - id: D2
    description: "Slim in-app mini player (56dp, title + speed cycle + prev/play/next) above the bottom bar on tabs and overlaid above gesture insets off-tab; hidden on the speaking article; title tap opens the article; speed chip writes ttsDefaultSpeed and applies the rate (D-17, D-18)"
    requirement: TTS-01
    verification:
      - kind: other
        ref: "./gradlew --offline :app:assembleDebug + acceptance greps (TtsMiniPlayer/SkipPrevious/ttsDefaultSpeed/isNotBlank/56 in TtsMiniPlayer.kt; TtsMiniPlayer in BorisApp.kt)"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsSpeedTest.kt"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/data/UserSettingsTest.kt"
        status: pass
    human_judgment: true
    rationale: "Visibility across routes, fade, RTL mirroring, and transport behavior need a device with a TTS engine; device verification is deferred to 1.4.0 release testing per user decision at the checkpoint"
  - id: D3
    description: "Full listen-chrome device UAT (background playback, lock-screen transport, article switching, volume keys, engine-missing path) across 04-01/04-02/04-03"
    requirement: TTS-01
    verification: []
    human_judgment: true
    rationale: "Checkpoint approved with device verification explicitly deferred to the 1.4.0 release build; ROADMAP success criteria 1-6 remain to be confirmed on hardware"

duration: 11min
completed: 2026-08-18
status: complete
---

# Phase 4 Plan 03: Follow-Along and Mini Player Summary

**Teal spoken-paragraph fill with HighlightJump auto-scroll (user scroll pauses scroll only, D-15) plus a slim 56dp in-app mini player with title, speed cycle on ttsDefaultSpeed, and paragraph transport, hidden on the speaking article**

## Performance

- **Duration:** ~11 min
- **Started:** 2026-08-18T10:55Z
- **Completed:** 2026-08-18T11:06Z
- **Tasks:** 2 auto + 1 human-verify checkpoint
- **Files modified:** 8

## Accomplishments
- Follow-along mark: `SpokenMark` (#2DD4BF at FindMarkAlpha 0.38f), `PaintedHighlight.spoken`, `ArticleFind.paintedSpoken` appended outside `visibleFor` so it paints even with highlights hidden; NIP-84 passes exclude spoken items
- Auto-scroll reuses `HighlightJump.awaitStop`/`scrollTarget`/`animateScrollTo`; a `followAlongScrolling` flag plus `positionRestored` gate keeps programmatic motion from counting as user scroll; real user scrolls call `setFollowAlongPaused(true)` while speech continues and the mark stays (D-15)
- `TtsMiniPlayer`: 56dp bar (surface + 1dp top outline, 200ms fade) with ellipsized title that opens the article, `{rate}x` chip cycling the shared presets into `ttsDefaultSpeed` via `SettingsViewModel.update` plus `TtsPlayback.setRate` (D-18), and 48dp prev/play/next transport; no progress bar, artwork, author line, or stop button
- `BorisApp` composes the player above `BorisBottomBar` on tab routes and as a bottom overlay above gesture insets on off-tab routes; hidden on the speaking article via raw/normalized/nostr-uri route comparison; previews never show it (session stays null)

## Task Commits

1. **Task 1: Spoken-paragraph follow-along mark and auto-scroll** - `379d7f8` (feat)
2. **Task 2: Slim in-app mini player with speed cycle** - `409f2cd` (feat)

## Files Created/Modified
- `app/src/main/java/org/dergigi/boris/ui/shell/TtsMiniPlayer.kt` - mini player bar, speed chip, and visibility host with fade
- `app/src/main/java/org/dergigi/boris/ui/theme/Color.kt` - `SpokenMark` token
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` - `PaintedHighlight.spoken` flag
- `app/src/main/java/org/dergigi/boris/ui/reader/ArticleFind.kt` - `SPOKEN_ID` + `paintedSpoken` factory
- `app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt` - spoken fill pass at FindMarkAlpha
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` - spoken mark append, follow-along auto-scroll, D-15 user-scroll pause
- `app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt` - `resume()` clears `followAlongPaused`
- `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` - mini player wiring for tab and off-tab routes

## Decisions Made
- `isScrollInProgress` (not raw scroll values) drives the D-15 pause, gated by `followAlongScrolling` and `positionRestored`, so neither follow-along motion nor the reading-position restore pauses auto-scroll
- The live `settings.ttsFollowAlong` value governs mark and scroll instead of the session's start-time snapshot, so the Settings checkbox takes effect mid-playback
- The mini player keeps its last session in state so the 200ms fade-out still has content after the session ends

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] TtsPlayback.resume() clears followAlongPaused**
- **Found during:** Task 1 (follow-along auto-scroll)
- **Issue:** D-15 requires "play or skip" to resume auto-scroll; `skip()` already reset `followAlongPaused` but `resume()` did not, so tapping play after a user scroll never resumed auto-scroll (including resume from the notification)
- **Fix:** `resume()` now copies the session with `followAlongPaused = false`
- **Files modified:** app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt
- **Verification:** Build passes; auto-scroll effect keys on `followAlongPaused` so the reset re-triggers it
- **Committed in:** 379d7f8

**2. [Rule 2 - Missing Critical] Route-vs-session url comparison handles canonicalized urls**
- **Found during:** Task 2 (mini player visibility)
- **Issue:** The plan compares the current route to the session url, but `content.url` (which the session carries) is canonicalized during fetch (`UrlExtractor.normalize`, nostr `article.uri`, RSS `item.link`); a raw route comparison could show the mini player on the speaking article, violating the plan's explicit prohibition
- **Fix:** `isSameArticle` in TtsMiniPlayer.kt compares the route url raw, normalized, and as a parsed nostr uri
- **Files modified:** app/src/main/java/org/dergigi/boris/ui/shell/TtsMiniPlayer.kt
- **Verification:** Build passes; the mini player's own title-tap navigation uses `session.url` so the canonical route always matches
- **Committed in:** 409f2cd

---

**Total deviations:** 2 auto-fixed (2 missing critical)
**Impact on plan:** Both close gaps between the plan's stated contracts (D-15 resume, speaking-article prohibition) and the code as specified. No scope creep.

## Issues Encountered
None. Builds ran with `--offline` throughout because dl.google.com is unreachable from this machine (known since 04-01); all dependencies were cached.

## User Setup Required
A device or emulator with a system TTS engine installed, for the deferred listen-chrome UAT (carried over from 04-01; no new setup).

## Next Phase Readiness
- Phase 4 code is complete: engine + service (04-01), settings section (04-02), follow-along and mini player (04-03)
- Device UAT (ROADMAP success criteria 1-6) was explicitly deferred at the checkpoint to hands-on testing with the 1.4.0 release build; the coverage block routes those deliverables to human verification
- Task 2 listed ReaderScreen.kt in its files but no change was needed there: the 04-01 top bar already owns play/pause on the speaking article and the off-tab overlay lives in BorisApp

---
*Phase: 04-listen-to-articles*
*Completed: 2026-08-18*

## Self-Check: PASSED

- TtsMiniPlayer.kt exists; task commits 379d7f8 and 409f2cd in log; SUMMARY on disk
- ./gradlew --offline :app:assembleDebug and TtsSpeedTest/UserSettingsTest exit 0; all task acceptance greps pass
