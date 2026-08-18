---
phase: 04-listen-to-articles
plan: 02
subsystem: ui
tags: [tts, settings, compose, nip-78]

requires:
  - phase: 04-01
    provides: TtsPlayback.preview/setRate/session, TtsSpeed, TtsLanguage, TtsPreview.EXAMPLE_TEXT, UserSettings typed TTS accessors, tts_* strings, openTtsSettings fallback chain
provides:
  - TtsSection inside SettingsCategory.Reading (heading, speed cycle chip, speaker-language dropdown, locked preview with one-shot play, follow-along checkbox)
  - Language select writes ttsLanguageMode + ttsUseSystemLanguage + ttsDetectContentLanguage (webapp three-key contract)
  - D-11 error notice with Open settings deep link that never hides the controls
  - TtsPlayback.previewing/previewError StateFlows, stopPreview(), applyLanguage() live-locale pass
affects: [04-03 follow-along and mini player]

actuals:
  tokens: 3300
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Settings section composable mirrors ReadingSection: SettingRow/SettingCheckbox + ExposedDropdownMenuBox like FontDropdown"
    - "Preview state lives on TtsPlayback (StateFlow), never in the composable"

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/ui/settings/TtsSection.kt
  modified:
    - app/src/main/java/org/dergigi/boris/ui/settings/SettingsScreen.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsPlaybackService.kt

key-decisions:
  - "Preview icon state and preview errors ride two new TtsPlayback StateFlows (previewing, previewError) because previews never create a session to carry them"
  - "Live language switch goes through a new Engine.applyLanguage() that clears the per-url locale cache and QUEUE_FLUSHes the current paragraph; no second TextToSpeech"
  - "Settings error notice reuses the reader's openTtsSettings fallback chain instead of duplicating the intent list"

patterns-established:
  - "Speed chip: 40dp chip inside a 48dp touch box, Icons.Filled.Speed 18dp + {rate}x labelLarge"

requirements-completed: [TTS-01, READ-01]

coverage:
  - id: D1
    description: "Reading settings shows a Text-to-Speech block (heading, speed cycle, language dropdown, locked preview sentence, follow-along) between ReadingSection and ReadingPreview, writing only the existing NIP-78 keys"
    requirement: TTS-01
    verification:
      - kind: other
        ref: "./gradlew --offline :app:assembleDebug + acceptance greps (ttsDefaultSpeed/ttsLanguageMode/ttsUseSystemLanguage/ttsDetectContentLanguage/ttsFollowAlong/EXAMPLE_TEXT in TtsSection.kt; TtsSection in SettingsScreen.kt)"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsSpeedTest.kt"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsLanguageTest.kt"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/data/UserSettingsTest.kt"
        status: pass
    human_judgment: false
  - id: D2
    description: "Preview play speaks the locked sentence one-shot (session stays null, no mini player), pause stops it, and speed/language changes apply to a live article session"
    requirement: TTS-01
    verification: []
    human_judgment: true
    rationale: "Audible one-shot playback, live rate/locale switching, and dropdown/chip visuals need a device or emulator with a TTS engine; JVM tests cannot assert them"
  - id: D3
    description: "Missing engine or language keeps every settings control visible and shows error copy plus an Open settings deep link into system TTS settings"
    requirement: TTS-01
    verification: []
    human_judgment: true
    rationale: "Requires a device without a TTS engine/voice to trigger the failure path end-to-end"
---

# Phase 4 Plan 02: TTS Settings Section Summary

**Webapp-matching Text-to-Speech settings under Reading: speed cycle chip, speaker-language dropdown writing the three NIP-78 keys, locked preview sentence with one-shot play, follow-along checkbox, and a D-11 error notice with Open settings**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-08-18T10:46Z
- **Completed:** 2026-08-18T10:56Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- `TtsSection` composed inside `SettingsCategory.Reading` between `ReadingSection` and `ReadingPreview` (not a new category, not under Media)
- Speed cycle chip (`Icons.Filled.Speed` + `{rate}x`, 40dp chip / 48dp touch) cycles `TtsSpeed` presets, writes `ttsDefaultSpeed`, and applies the rate to a live session
- Speaker-language dropdown (FontDropdown-style `ExposedDropdownMenuBox`, divider after Content) writes `ttsLanguageMode` plus `ttsUseSystemLanguage`/`ttsDetectContentLanguage`, and re-resolves a live session's locale through the one engine
- Preview box holds exactly `TtsPreview.EXAMPLE_TEXT` (16sp/1.5, surfaceVariant, 12dp/8dp); play is one-shot with session left null; pause stops the utterance
- Engine/language failures keep all controls visible and add error copy plus Open settings via the reader's existing fallback chain

## Task Commits

1. **Task 1: TtsSection matching the webapp TTS settings** - `cd5579d` (feat)
2. **Task 2: Error notice + Open settings deep link** - `b7866ba` (feat)

## Files Created/Modified
- `app/src/main/java/org/dergigi/boris/ui/settings/TtsSection.kt` - the whole section incl. chip, dropdown, preview box, error notice
- `app/src/main/java/org/dergigi/boris/ui/settings/SettingsScreen.kt` - Reading now composes ReadingSection → TtsSection → ReadingPreview
- `app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt` - previewing/previewError StateFlows, stopPreview(), applyLanguage(), Engine.applyLanguage
- `app/src/main/java/org/dergigi/boris/tts/TtsPlaybackService.kt` - applyLanguage impl (clears locale cache, re-speaks current paragraph), preview-done callback

## Decisions Made
- Preview state and preview errors live on `TtsPlayback` as StateFlows because D-10 forbids previews from creating a session; the composable stays stateless
- Language changes during playback flow through a new `Engine.applyLanguage()` rather than a second `TextToSpeech` instance
- Reused `openTtsSettings` from the reader (internal, same module) instead of duplicating the three-intent fallback chain

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Preview play/pause state and preview error surface added to TtsPlayback**
- **Found during:** Task 1 (preview icon) and Task 2 (error notice)
- **Issue:** The plan's UI contract needs the preview button to flip to Pause and the error notice to fire after a failed preview, but previews leave `session` null, so neither state existed anywhere collectable
- **Fix:** Added `previewing` and `previewError` StateFlows, `stopPreview()`, and `onPreviewFinished()` to `TtsPlayback`; service reports preview completion and errors route to `previewError` when no session exists
- **Files modified:** TtsPlayback.kt, TtsPlaybackService.kt
- **Verification:** `./gradlew --offline :app:assembleDebug` exits 0; state transitions covered by code paths exercised in existing flows
- **Committed in:** cd5579d (previewing/stopPreview), b7866ba (previewError)

**2. [Rule 3 - Blocking] Engine.applyLanguage() added for the live locale switch**
- **Found during:** Task 1 (language dropdown)
- **Issue:** The plan requires "setLanguage + QUEUE_FLUSH current paragraph through TtsPlayback", but no such pass-through existed and the service caches the resolved locale per url
- **Fix:** New `Engine.applyLanguage()` clears `resolvedLanguageUrl` and re-speaks the current paragraph when playing; `TtsPlayback.applyLanguage()` exposes it to the UI
- **Files modified:** TtsPlayback.kt, TtsPlaybackService.kt
- **Verification:** Build passes; speakCurrent re-resolves language because the url no longer matches the cache
- **Committed in:** cd5579d

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 blocking)
**Impact on plan:** Both were the small 04-01 interface gaps the plan itself anticipated ("do not re-own these files unless a one-line call is missing"). No scope creep; no new keys, files, or categories beyond the plan.

## Issues Encountered
None. Builds ran with `--offline` throughout because dl.google.com is unreachable from this machine (known from 04-01); all dependencies were cached.

## User Setup Required
A device or emulator with a system TTS engine installed, for the preview-play human check (carried over from 04-01; no new setup).

## Next Phase Readiness
- 04-03 (follow-along + mini player) can rely on `TtsPlayback.setRate`/`applyLanguage` for its speed chip and on `previewing` never colliding with sessions
- Settings round-trip with readwithboris.com uses only the existing NIP-78 keys; nothing new to sync

## Self-Check: PASSED

- TtsSection.kt exists; SUMMARY exists; task commits cd5579d and b7866ba in log
- SettingsScreen Reading order verified: ReadingSection → TtsSection → ReadingPreview
- `./gradlew --offline :app:assembleDebug` and the three test suites exit 0
- TtsPreview.EXAMPLE_TEXT unchanged; no EXAMPLE_TEXT in strings.xml; no new NIP-78 keys

---
*Phase: 04-listen-to-articles*
*Completed: 2026-08-18*
