# Phase 4: Listen to articles - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-18
**Phase:** 4-Listen to articles
**Areas discussed:** Playback scope, Voice engine, Follow-along, Listen control

---

## Playback scope

| Option | Description | Selected |
|--------|-------------|----------|
| In-reader only | Play/pause while the article is open; stop on leave/lock | |
| Background playback | Screen off, media notification, lock-screen controls | ✓ |
| You decide | Smallest slice that still feels like listening | |

**User's choice:** Background playback
**Notes:** Podcast-like: keep speaking after leaving the reader. Browsing other articles is fine. Play on a different article switches. Notification gets play/pause/stop plus skip next/prev paragraph.

---

## Voice engine

| Option | Description | Selected |
|--------|-------------|----------|
| On-device Android TTS | Offline, no keys, device voice | ✓ |
| Cloud TTS | Nicer voices, network, cost | |
| System TTS settings only | No in-app language UI | |
| Match webapp settings | Speed, language, preview sentence, NIP-78 sync | ✓ |

**User's choice:** On-device engine; Android settings match the webapp TTS section and sync `ttsDefaultSpeed` / `ttsLanguageMode`.
**Notes:** User pointed at the webapp settings (speed cycle, speaker language, preview sentence). Android already stores those keys. If TTS or a language is missing, toast and link to system TTS settings.

---

## Follow-along

| Option | Description | Selected |
|--------|-------------|----------|
| Highlight + auto-scroll | Paint spoken paragraph and keep it on screen | ✓ |
| Audio only | No extra marks | |
| On by default | Follow-along unless turned off | ✓ |
| From reading position | Start near saved scroll fraction | ✓ |
| Let them scroll | Manual scroll pauses follow-along, keeps speaking | ✓ |

**User's choice:** Highlight and auto-scroll, disableable in settings, on by default.
**Notes:** Start from reading position. Do not fight the user if they scroll away.

---

## Listen control

| Option | Description | Selected |
|--------|-------------|----------|
| Top bar play/pause | Always visible in the reader | ✓ |
| Overflow only | Extra tap to start | |
| Mini player while browsing | Slim bar with title + controls | ✓ |
| Notification only | No in-app mini player | |
| Speed cycle on player | Same presets as settings | ✓ |
| Volume keys | While speaking, control volume (not scroll, not skip) | ✓ |

**User's choice:** Top-bar play, mini player when away from the article, speed cycle on the player, volume keys control volume while speaking.

---

## Claude's Discretion

- Spoken-paragraph mark vs highlight/find marks
- Mini-player placement
- Settings category vs section
- Follow-along key name
- Speech-rate mapping, chunking, spoken-text cleanup
- Language detect implementation
- Audio focus and MediaSession wiring

## Deferred Ideas

- Cloud TTS
- Playlist / queue
- Sleep timer
- Downloaded audio files
- Spoken-offset sync over Nostr
