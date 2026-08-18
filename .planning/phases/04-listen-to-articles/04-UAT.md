---
status: testing
phase: 04-listen-to-articles
source: [04-VERIFICATION.md]
started: 2026-08-18T11:15:00Z
updated: 2026-08-18T11:15:00Z
---

## Current Test

number: 1
name: Core listen flow
expected: |
  On a Ready article, Listen speaks audible on-device speech. Pause and Resume work
  (resume restarts the current paragraph). Speech survives screen-off and leaving the
  reader. Lock-screen shows play/pause and paragraph skip; Stop is on the notification,
  not the top bar.
awaiting: user response

## Tests

### 1. Core listen flow
expected: Listen speaks; pause/resume; screen-off survival; lock-screen play/pause/skip; Stop on the notification
result: [pending]

### 2. Article switching
expected: Play on a second article stops the first and starts the second; browsing or opening articles without tapping play never interrupts speech
result: [pending]

### 3. Follow-along
expected: Teal fill on the spoken paragraph with auto-scroll; manual scrolling pauses auto-scroll but not speech; skip or play resumes it; the Settings checkbox turns follow-along off; playback starts near the saved reading position
result: [pending]

### 4. Settings parity and preview
expected: Text-to-Speech section matches the webapp (speed cycle, speaker language dropdown, preview paragraph with play); speed chips in Settings and the mini player show the same rate and write ttsDefaultSpeed; preview speaks only the locked Boris sentence with no mini player
result: [pending]

### 5. Degraded paths
expected: Volume keys change volume (not scroll) while speaking; empty article shows "Nothing to read aloud"; missing engine/language keeps Listen visible, shows error copy, Open settings reaches system TTS settings, reading still works logged out
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
