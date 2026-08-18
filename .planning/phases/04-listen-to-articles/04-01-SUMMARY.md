---
phase: 04-listen-to-articles
plan: 01
subsystem: tts
tags: [texttospeech, mediasession, foreground-service, audio-focus, compose]

requires: []
provides:
  - TtsPlayback process singleton with StateFlow<TtsSession?> (start/pause/resume/stop/skip/setRate/preview/setFollowAlongPaused)
  - TtsPlaybackService mediaPlayback foreground service with MediaSession, audio focus, and lock-screen notification
  - TtsText.paragraphs + startIndex + chunks (speakable text pipeline)
  - TtsSpeed presets + snap + cycle (webapp list, default 2.1)
  - TtsLanguage mode/resolveLanguage/detectHeuristic (webapp resolution order)
  - TtsPreview.EXAMPLE_TEXT (locked D-10 sentence)
  - UserSettings typed TTS accessors incl. ttsFollowAlong
  - Reader top-bar Listen with empty/error snackbars and TTS-settings deep link
affects: [04-02 settings section, 04-03 follow-along and mini player]

actuals:
  tokens: 12000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Process-wide playback state as object + StateFlow (mirrors SettingsSync)"
    - "Engine interface between TtsPlayback and the service to avoid binding"

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/tts/TtsPlayback.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsPlaybackService.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsText.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsSpeed.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsLanguage.kt
    - app/src/main/java/org/dergigi/boris/tts/TtsPreview.kt
    - app/src/test/java/org/dergigi/boris/tts/TtsTextTest.kt
    - app/src/test/java/org/dergigi/boris/tts/TtsSpeedTest.kt
    - app/src/test/java/org/dergigi/boris/tts/TtsLanguageTest.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/org/dergigi/boris/data/UserSettings.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/dergigi/boris/data/UserSettingsTest.kt

key-decisions:
  - "TtsSession.started flag distinguishes engine init from actual speech so duplicate taps during init are ignored without a spinner"
  - "TTS settings deep link uses com.android.settings.TTS_SETTINGS with fallback to ACTION_INSTALL_TTS_DATA then generic Settings (no SDK constant exists)"
  - "Preview never creates a TtsSession; pendingPreview hand-off covers service cold start"

patterns-established:
  - "TtsPlayback.Engine interface: service registers itself; UI never binds the service"
  - "Utterance ids p{i}.{j} carry paragraph index for progress callbacks"

requirements-completed: [TTS-01, READ-01]

coverage:
  - id: D1
    description: "Speakable-text pipeline: title/summary/body paragraphs, code/image/table dropping, chunking, start index from reading position"
    requirement: TTS-01
    verification:
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsTextTest.kt"
        status: pass
    human_judgment: false
  - id: D2
    description: "Speed presets/cycle and language resolution match the webapp contract"
    requirement: TTS-01
    verification:
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsSpeedTest.kt"
        status: pass
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/tts/TtsLanguageTest.kt"
        status: pass
    human_judgment: false
  - id: D3
    description: "TTS defaults synced via NIP-78 keys (ttsDefaultSpeed 2.1, ttsLanguageMode content, ttsFollowAlong true)"
    requirement: TTS-01
    verification:
      - kind: unit
        ref: "app/src/test/java/org/dergigi/boris/data/UserSettingsTest.kt#defaultsMatchWebappReadingDisplay"
        status: pass
    human_judgment: false
  - id: D4
    description: "Listen from the reader top bar speaks on-device audio; playback survives leaving the reader and screen-off; lock-screen play/pause/skip/stop; play on another article switches; volume keys change volume while speaking; error path keeps Listen visible with Open settings"
    requirement: TTS-01
    verification: []
    human_judgment: true
    rationale: "Requires a device/emulator with a TTS engine; audio output, lock-screen transport, and audio focus cannot be asserted in JVM tests"
---

# Phase 4 Plan 01: TTS Engine Tracer Summary

**On-device TTS tracer: TtsPlayback singleton + mediaPlayback foreground service with MediaSession, plus a reader top-bar Listen button that starts speech at the saved reading position**

## Performance

- **Duration:** ~60 min (including recovery from an aborted executor and a hung Gradle daemon)
- **Completed:** 2026-08-18
- **Tasks:** 3
- **Files modified:** 14

## Accomplishments
- Full speech backbone: `TtsPlayback` (process-wide session StateFlow) + `TtsPlaybackService` (FGS type mediaPlayback, MediaSession transport, audio focus with pause-not-duck, offline-voice preference)
- Speakable-text pipeline dropping code fences, images, and tables; paragraph skip units; engine-limit chunking with `p{i}.{j}` utterance ids; start index from `ReadingPositionStore` fraction
- Reader top-bar Listen (Ready state only): play/pause/resume, empty-article snackbar, D-11 error snackbar with Open settings fallback chain, POST_NOTIFICATIONS one-time request, volume keys pass through while speaking
- Webapp-locked contracts: speed presets with default 2.1, language modes and resolution order, preview sentence pinned by test

## Task Commits

1. **Task 1 + 3: Tracer + engine edge behavior** - `674bdfe` (feat) — task 3's article-switch, skip-edge, and error-path behavior was built directly into the tracer files
2. **Task 2: JVM tests** - `43d7b43` (test)

## Files Created/Modified
- `app/src/main/java/org/dergigi/boris/tts/*` - engine, service, text/speed/language/preview helpers
- `app/src/main/AndroidManifest.xml` - FGS permissions, TTS_SERVICE query, service declaration (exported=false)
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` - Listen button, snackbars, volume-key guard
- `app/src/main/java/org/dergigi/boris/data/UserSettings.kt` - typed TTS accessors, ttsFollowAlong default
- `app/src/main/res/values/strings.xml` - full copywriting contract from 04-UI-SPEC

## Decisions Made
- `started` flag on TtsSession for init-vs-speaking; duplicate taps during init are no-ops (plan's loading state)
- Settings deep link fallback chain since the SDK exposes no `ACTION_TEXT_TO_SPEECH_SETTINGS` constant
- Preview keeps `session` null the whole time so the (future) mini player never appears for previews

## Deviations from Plan

None affecting scope. Execution note: the original executor agent was aborted mid-run; work was completed inline by the orchestrator. Builds ran with `--offline` because dl.google.com was unreachable and hung dependency resolution; all dependencies were already cached.

## Issues Encountered
- Gradle daemon hung twice on a HEAD request to dl.google.com (TCP SYN_SENT, no response). Resolved by killing the daemon and building offline.

## User Setup Required
A device or emulator with a system TTS engine installed, for the human check of speech.

## Next Phase Readiness
- 04-02 (settings section) can use TtsSpeed/TtsLanguage/TtsPreview and `TtsPlayback.preview` directly
- 04-03 (follow-along + mini player) can collect `TtsPlayback.session`; `followAlongEnabled`/`followAlongPaused` fields already exist

---
*Phase: 04-listen-to-articles*
*Completed: 2026-08-18*
