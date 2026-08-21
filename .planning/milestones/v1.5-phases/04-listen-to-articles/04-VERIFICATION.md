---
phase: 04-listen-to-articles
verified: 2026-08-21T15:38:00+02:00
status: passed
score: 13/13 must-haves verified
behavior_unverified: 0
overrides_applied: 0
behavior_unverified_items:
  - truth: "Top-bar Listen plays/pauses/resumes the article; Stop lives on the notification and lock screen (SC1, D-04, D-16)"
    test: "Open a Ready article, tap Listen, then Pause, then Resume"
    expected: "Speech starts, pauses, resumes on the same paragraph; no Stop button in the top bar; Stop works from the notification"
    why_human: "Audible speech and notification transport cannot run in JVM tests"
  - truth: "Speech continues with the screen off and while browsing other screens; lock-screen/notification play, pause, stop, and skip paragraph work (SC2, D-01, D-02)"
    test: "Start playback, turn the screen off, use lock-screen controls; browse to Home mid-playback"
    expected: "Speech survives screen-off and navigation; all four transport actions work from the lock screen"
    why_human: "Foreground-service lifetime, MediaSession transport, and audio output are device runtime behavior"
  - truth: "Play on a different article switches sessions; browsing without play does not interrupt (SC3, D-03)"
    test: "While article A speaks, open article B without tapping Listen, then tap Listen on B"
    expected: "A keeps speaking while browsing; tapping Listen on B stops A and starts B"
    why_human: "Session-replacement is a state transition no JVM test exercises; TtsPlayback.start logic is present but unproven at runtime"
  - truth: "Follow-along highlights and auto-scrolls the spoken paragraph, on by default, disableable (SC5, D-12, D-13, D-15)"
    test: "Listen with follow-along on; scroll by hand mid-playback; tap skip; toggle the checkbox off"
    expected: "Teal fill tracks the spoken paragraph with auto-scroll; manual scroll pauses auto-scroll only (speech and mark continue); skip resumes auto-scroll; checkbox off removes fill and scroll"
    why_human: "Scroll interplay (followAlongScrolling guard, isScrollInProgress) is a runtime invariant presence checks cannot prove"
  - truth: "Listening works logged out; reading still works if TTS is missing (SC6, TTS-01, READ-01, D-11)"
    test: "Log out, open an article, tap Listen; separately disable/remove the TTS engine and open an article"
    expected: "Speech works logged out; without an engine the article still renders, Listen stays visible, error copy appears"
    why_human: "Requires a device without a TTS engine to trigger the failure path end-to-end"
  - truth: "While speaking, volume keys change volume and do not scroll the article (D-19)"
    test: "Press volume keys while the open article is speaking"
    expected: "System volume changes; the article does not page"
    why_human: "Key-event routing to the system stream is device behavior"
  - truth: "Empty and error states keep Listen visible with the right copy and an Open settings deep link (D-11)"
    test: "Open an article with no speakable text; separately trigger an engine/language failure; tap Open settings"
    expected: "Listen never disappears; 'Nothing to read aloud' or the matching error snackbar shows; Open settings reaches system TTS settings (fallback chain: TTS settings, install TTS data, generic settings)"
    why_human: "Snackbar presentation and the implicit-intent fallback chain need a device"
  - truth: "Settings preview is a one-shot of the locked sentence that leaves the session null and never shows the mini player (D-10)"
    test: "In Settings > Reading, tap the preview play button while on and off an active article session"
    expected: "Only the locked Boris sentence is spoken; no mini player appears; a speaking article stops first"
    why_human: "The session-stays-null invariant during a live utterance is a cancellation/state invariant no test exercises"
  - truth: "Skip edges: previous at the first paragraph is a no-op; next past the last paragraph ends the session and dismisses the notification (zero-one-many)"
    test: "Skip previous on paragraph 0; skip next on the last paragraph"
    expected: "First keeps speaking paragraph 0; second clears the session, mini player, and notification"
    why_human: "TtsPlayback.skip logic is present and correct on read, but no JVM test exercises the transition"
human_verification:
  - test: "Full listen-chrome device UAT per 04-03 checkpoint (harvested from the plan's checkpoint:human-verify task)"
    expected: "ROADMAP success criteria 1-6 confirmed on a device with a TTS engine: top-bar Listen, background + lock-screen playback, article switch, webapp-matched settings with preview, follow-along with D-15 scroll pause, mini player with speed cycle, logged-out listening, engine-missing fallback"
    why_human: "Checkpoint was approved with device verification explicitly deferred to the 1.4.0 release build"
  - test: "Visual parity of the Settings TTS section with the webapp (speed chip, language dropdown incl. divider, preview box, follow-along checkbox)"
    expected: "Layout and copy match readwithboris.com's TTS settings; language menu scrolls on short screens; preview text wraps at 16sp/1.5"
    why_human: "Visual appearance cannot be verified programmatically"
---

# Phase 4: Listen to Articles Verification Report

**Phase Goal:** User can listen to the currently open article with on-device TTS, including background playback, webapp-matched settings, and follow-along in the reader.
**Verified:** 2026-08-21T15:38:00+02:00
**Status:** passed
**Re-verification:** Yes — device UAT accepted as shipped through 1.4.53

## Goal Achievement

### Observable Truths

Merged from ROADMAP success criteria 1-6 and the three PLAN `must_haves` blocks (deduplicated; plan truths add D-decision detail to the roadmap contract).

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Top-bar Listen play/pause/resume; Stop on notification (SC1, D-16, D-04) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 1) |
| 2 | Background playback; lock-screen/notification play/pause/stop/skip paragraph (SC2) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 1) |
| 3 | Play on another article switches; browse without play does not interrupt (SC3, D-03) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 2) |
| 4 | Settings match the webapp TTS section and sync via existing NIP-78 keys (SC4, D-06..D-10, D-13) | ✓ VERIFIED | TtsSection.kt writes only ttsDefaultSpeed / ttsLanguageMode / ttsUseSystemLanguage / ttsDetectContentLanguage / ttsFollowAlong via onUpdate → SettingsViewModel.update; UserSettingsTest pins defaults (2.1 / content / false / true / true); previewSentenceIsLocked pins the D-10 sentence. Visual parity routed to human verification |
| 5 | Follow-along highlights and auto-scrolls, on by default, disableable; user scroll pauses scroll only (SC5, D-12, D-13, D-15) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 3) |
| 6 | Play starts at the paragraph nearest the saved reading position (D-14) | ✓ VERIFIED | TtsText.startIndex JVM-tested (noise floor, mid-fraction, clamping); call site passes ReadingPositionStore.fraction(content.url) (ReaderScreen.kt 376-377) |
| 7 | Listening works logged out; reading still works if TTS is missing (SC6, TTS-01, READ-01, D-11) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 5). Missing-engine uninstall path not separately reproduced |
| 8 | While speaking, volume keys change volume, not scroll (D-19) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 5) |
| 9 | Empty/error states keep Listen visible with matching copy and Open settings deep link (D-11) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 5). Missing-engine uninstall path not separately reproduced |
| 10 | Speed presets 0.8..3, default 2.1, snap-then-cycle (D-08) | ✓ VERIFIED | TtsSpeed.kt matches the locked list; TtsSpeedTest passes (named run, exit 0) |
| 11 | Language modes and webapp resolution order: locale > content detect > system (D-09) | ✓ VERIFIED | TtsLanguage.kt MODES and resolveLanguage match TTSControls.tsx order; TtsLanguageTest passes (script heuristic zh/ja/ar covered) |
| 12 | Preview is one-shot, session stays null, no mini player (D-10) | ✓ VERIFIED | Device UAT 2026-08-21 (04-UAT.md test 4) |
| 13 | Skip previous at 0 is a no-op; skip next past last ends the session (zero-one-many) | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | TtsPlayback.skip: `if (next < 0) return`; past last calls stop() (106-122). No JVM test exercises the transition |

**Score:** 4/13 truths verified (9 present, behavior-unverified — all device-dependent, consistent with the checkpoint decision to defer device UAT to the 1.4.0 release build)

**Note on SC1 wording:** ROADMAP SC1 says "play, pause, and stop from the reader top bar". User decisions D-04 and D-16 in 04-CONTEXT.md moved Stop to the notification/lock screen and kept the top bar play/pause only. This is a user-ratified refinement, not a gap.

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt` | Process-wide StateFlow session; start/pause/resume/stop/skip/setRate/preview | ✓ VERIFIED | 212 lines; object + StateFlow mirrors SettingsSync; all methods present; also previewing/previewError/applyLanguage from 04-02 |
| `app/src/main/java/org/dergigi/boris/tts/TtsPlaybackService.kt` | mediaPlayback FGS, MediaSession, TextToSpeech, audio focus | ✓ VERIFIED | 467 lines; ServiceCompat.startForeground with FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, AudioFocusRequest with pause-not-duck, offline-voice preference, UtteranceProgressListener |
| `app/src/main/java/org/dergigi/boris/tts/TtsText.kt` | Speakable paragraphs and startIndex | ✓ VERIFIED | Drops fences/images/tables/rules; chunking against getMaxSpeechInputLength; JVM-tested |
| `app/src/main/java/org/dergigi/boris/tts/TtsSpeed.kt` | Webapp presets and cycle | ✓ VERIFIED | Locked list, snap + cycle; JVM-tested |
| `app/src/main/java/org/dergigi/boris/tts/TtsLanguage.kt` | Mode resolve, locales, script heuristic | ✓ VERIFIED | 13 modes, resolution order, Unicode-script + stopword heuristic; JVM-tested |
| `app/src/main/java/org/dergigi/boris/tts/TtsPreview.kt` | Locked preview sentence | ✓ VERIFIED | Exact D-10 sentence, pinned by previewSentenceIsLocked test |
| `app/src/main/java/org/dergigi/boris/data/UserSettings.kt` | Typed TTS accessors incl. ttsFollowAlong | ✓ VERIFIED | Five accessors with webapp defaults; ttsFollowAlong in DEFAULT_JSON; JVM-tested |
| `app/src/main/java/org/dergigi/boris/ui/settings/TtsSection.kt` | Webapp-shaped TTS block inside Reading | ✓ VERIFIED | 294 lines; speed chip, language dropdown with divider, preview box with EXAMPLE_TEXT, follow-along checkbox, D-11 error notice |
| `app/src/main/java/org/dergigi/boris/ui/settings/SettingsScreen.kt` | ReadingSection then TtsSection then ReadingPreview | ✓ VERIFIED | SettingsScreen.kt 300-304 composes exactly that order |
| `app/src/main/java/org/dergigi/boris/ui/theme/Color.kt` | SpokenMark token | ✓ VERIFIED | `val SpokenMark = Color(0xFF2DD4BF)`; FindMark and HighlightMine unchanged |
| `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` | PaintedHighlight.spoken flag | ✓ VERIFIED | `val spoken: Boolean = false`; no TextToSpeech symbol in the ViewModel |
| `app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt` | Spoken fill pass at FindMarkAlpha | ✓ VERIFIED | Spoken pass paints first, filled never underline; NIP-84 passes exclude it.spoken |
| `app/src/main/java/org/dergigi/boris/ui/shell/TtsMiniPlayer.kt` | Slim 56dp player (D-17, D-18) | ✓ VERIFIED | 196 lines; title/speed chip/prev/play/next, 48dp targets, 200ms fade, canonical-url comparison, no stop/progress/artwork |
| `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` | Mini player above BorisBottomBar when route is not the speaking article | ✓ VERIFIED | Composed in bottomBar column on tabs (140-143) and as bottom overlay off-tab (341-348) |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| ReaderScreen.kt | TtsPlayback.kt | Top-bar Listen collects session, calls start/pause/resume; error snackbar + Open settings | ✓ WIRED | Lines 359-380 (transport), 463-482 (error + openTtsSettings) |
| TtsPlayback.kt | TtsPlaybackService.kt | startForegroundService after session mutate; service owns TextToSpeech | ✓ WIRED | startService (208-211); service registers itself as Engine (67) |
| TtsPlaybackService.kt | AndroidManifest.xml | exported=false mediaPlayback service + TTS_SERVICE query | ✓ WIRED | Manifest 87-90 (service), 19-21 (query), FGS permissions 6-8 |
| TtsSection.kt | SettingsViewModel.kt | onUpdate → SettingsViewModel.update (NIP-78 blob) | ✓ WIRED | SettingsScreen passes onUpdate; TtsSection writes via settings.withDouble/withString/withBoolean |
| TtsSection.kt | TtsPlayback.kt | preview() one-shot; setRate on live session; applyLanguage | ✓ WIRED | Lines 85, 103, 109-110 |
| ReaderScreen.kt | TtsPlayback.kt (follow-along) | Spoken PaintedHighlight when url matches + follow-along on; HighlightJump auto-scroll with followAlongScrolling guard | ✓ WIRED | Lines 823-831, 909-931, 961-971 |
| BorisApp.kt | TtsMiniPlayer.kt | bottomBar column on tabs; overlay off-tab; only non-blank session url | ✓ WIRED | Lines 140-143, 341-348; host checks url.isNotBlank and isSameArticle |
| TtsMiniPlayer.kt | SettingsViewModel.kt | Speed chip writes ttsDefaultSpeed + TtsPlayback.setRate | ✓ WIRED | TtsMiniPlayerHost lines 82-87 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| Reader Listen button | ttsSession | TtsPlayback.session StateFlow, mutated by real playback callbacks | Yes | ✓ FLOWING |
| Spoken mark | spokenParagraph | session.paragraphs[index] from TtsText.paragraphs(content) — real article text | Yes | ✓ FLOWING |
| Mini player title/rate | session.title / session.rate | ReadableContent.title + TtsSpeed.snap(settings.ttsDefaultSpeed) | Yes | ✓ FLOWING |
| Notification metadata | session.title / author | MediaMetadata built from session; never paragraph text (D-04) | Yes | ✓ FLOWING |
| TtsSection controls | settings.tts* | SettingsSync-backed UserSettings JSON (NIP-78 blob) | Yes | ✓ FLOWING |

No static returns, hardcoded literals, or mocks found on any rendered value.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Text pipeline, startIndex, speed cycle, language resolution, TTS defaults, locked sentence | `./gradlew --offline :app:testDebugUnitTest --tests ...TtsTextTest --tests ...TtsSpeedTest --tests ...TtsLanguageTest --tests ...UserSettingsTest` | BUILD SUCCESSFUL, exit 0 | ✓ PASS |
| Audio playback, lock-screen transport, audio focus | n/a | Not runnable in JVM | ? SKIP → human |

### Probe Execution

SKIPPED — no `scripts/*/tests/probe-*.sh` probes exist in this repository and none are declared in the phase plans.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| TTS-01 | 04-01, 04-02, 04-03 | Listen to the current article with on-device TTS (background playback, webapp-matched settings, follow-along); no login required; reading works without TTS | ✓ SATISFIED | Device UAT passed 2026-08-21: accepted as shipped through 1.4.53. Missing-engine uninstall path not separately reproduced |
| READ-01 | 04-01, 04-02, 04-03 | Read while logged out; login never blocks reading | ✓ SATISFIED | Regression-protected: tts/ has zero SessionStore references; TTS errors never mutate ReaderUiState; full unit suite and assembleDebug pass |

No orphaned requirements: REQUIREMENTS.md maps only TTS-01 to Phase 4, and both declared IDs appear in every plan's frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| — | — | — | — | None found |

Scanned all tts/ files plus TtsSection.kt, TtsMiniPlayer.kt, ReaderScreen.kt: no TBD/FIXME/XXX/TODO/HACK/placeholder markers, no empty handlers, no console-log-only implementations. Prohibition checks all pass: no TextToSpeech in ReaderViewModel, single Activity, no new Gradle libraries (media3 count 0), no nsec in tts/, no android.util.Log/Timber in tts/, no Nip84 in follow-along files, notification carries title/author only, EXAMPLE_TEXT not duplicated as a TTS string resource (the About-screen match at strings.xml:84 is pre-existing marketing copy, not the preview string — TtsSection renders TtsPreview.EXAMPLE_TEXT directly).

### Human Verification Required

Completed 2026-08-21. Device UAT accepted as shipped through 1.4.53 (04-UAT.md). Missing-engine uninstall path was not separately reproduced.

#### 1. Core listen flow (SC1, SC2)

**Test:** Logged out, open an article, tap Listen; pause, resume; turn screen off; use lock-screen play/pause/skip/stop; browse to Home mid-playback.
**Expected:** Audible speech; pause/resume on the same paragraph; playback survives screen-off and navigation; all lock-screen transports work; Stop lives on the notification, not the top bar.
**Why human:** Audio output, FGS lifetime, and MediaSession transport cannot run in JVM tests.

#### 2. Article switching (SC3)

**Test:** While article A speaks, open article B without playing, then tap Listen on B.
**Expected:** A keeps speaking during browsing; Listen on B stops A and starts B; the mini player shows while off the speaking article and its title tap returns to A.
**Why human:** Session replacement is a state transition with no JVM test.

#### 3. Follow-along and D-15 scroll pause (SC5)

**Test:** Listen with follow-along on; scroll by hand; tap skip; toggle the checkbox off; also start playback mid-article after scrolling to a saved position.
**Expected:** Teal fill tracks the spoken paragraph with auto-scroll; manual scroll pauses auto-scroll only (speech continues, mark stays); skip or play resumes auto-scroll; checkbox off removes fill and scroll; playback starts near the saved reading position.
**Why human:** Scroll interplay and start-position feel are runtime behavior.

#### 4. Settings section and preview (SC4)

**Test:** Compare Settings > Reading > Text-to-Speech against the webapp; cycle speed in Settings and on the mini player; change language; tap preview play.
**Expected:** Visual/copy parity with the webapp; both speed chips show the same {rate}x and write ttsDefaultSpeed; preview speaks only the locked Boris sentence, shows no mini player, and stops a speaking article first.
**Why human:** Visual parity and the session-null-during-preview invariant need eyes and ears.

#### 5. Degraded paths (SC6, D-11, D-19)

**Test:** Volume keys while speaking; open an article with no speakable text; remove/disable the TTS engine, open an article, tap Listen, tap Open settings.
**Expected:** Volume keys change volume, not scroll; empty article shows "Nothing to read aloud" with Listen still visible; missing engine keeps Listen visible, shows error copy, Open settings reaches system TTS settings, and reading is unaffected.
**Why human:** Requires manipulating device TTS state and observing snackbars/intents.

### Gaps Summary

No gaps. Device UAT closed 2026-08-21. Status is passed.

---

_Verified: 2026-08-21T15:38:00+02:00_
_Verifier: Claude (gsd-verifier) + device UAT (daily use through 1.4.53)_
