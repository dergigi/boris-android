# Phase 4: Listen to articles - Context

**Gathered:** 2026-08-18
**Status:** Ready for planning

<domain>
## Phase Boundary

User can listen to the currently open article with on-device Android TTS. Playback continues in the background (screen off, lock screen, browsing other screens) until pause or the article ends. One article speaks at a time. Settings match the webapp TTS section and sync over the existing NIP-78 user-settings event. Listening works logged in or out. Reading stays ungated. No cloud TTS. No nsec. No new Activity.

</domain>

<decisions>
## Implementation Decisions

### Playback
- **D-01:** Background playback. Speech continues with the screen off, with a media notification and lock-screen / headset controls. — **Reversibility:** costly — needs a foreground service, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and a `MediaSession`.
- **D-02:** Podcast-like: keep speaking when the user leaves the reader for Home or another screen, until they pause or the article ends.
- **D-03:** One article speaking at a time. Browsing other articles does not interrupt. Pressing play on a different article stops the current one and starts the new one.
- **D-04:** Media notification / lock screen: play, pause, stop, skip to next paragraph, skip to previous paragraph.

### Voice engine
- **D-05:** On-device `android.speech.tts.TextToSpeech` only. No cloud TTS, no API keys, no network required for speech.
- **D-06:** Match the webapp Text-to-Speech settings: default playback speed, speaker language, and the preview paragraph with play.
- **D-07:** Sync speed and language through the existing NIP-78 settings JSON. Keys already in Android defaults: `ttsDefaultSpeed`, `ttsLanguageMode`, `ttsUseSystemLanguage`, `ttsDetectContentLanguage`.
- **D-08:** Speed presets are the webapp list: `0.8, 1, 1.2, 1.4, 1.6, 1.8, 2, 2.1, 2.4, 2.8, 3`. Default `2.1`. Cycle button writes `ttsDefaultSpeed`.
- **D-09:** Speaker language matches the webapp dropdown: System Language, Content (auto-detect), then `en-US`, `en-GB`, `zh`, `es`, `hi`, `ar`, `fr`, `pt`, `de`, `ja`, `ru`. Stored as `ttsLanguageMode`.
- **D-10:** Preview sentence is exactly: `Boris aims to be a calm reader app with clean typography, beautiful design, and a focus on readability. Boris does not and will never have ads, trackers, paywalls, subscriptions, or any other distractions.`
- **D-11:** If TTS is missing or the language cannot be spoken, show a short message and a link to system TTS settings. Reading still works. Do not hide the listen control.

### Follow-along
- **D-12:** While speaking, highlight the current paragraph and auto-scroll it on screen (reuse reader jump / find-mark machinery).
- **D-13:** Follow-along is on by default and can be turned off in settings.
- **D-14:** Play starts at the paragraph nearest the saved reading position (`ReadingPositionStore`). From the top if there is none.
- **D-15:** If the user scrolls the article while it is speaking, stop auto-scrolling until they tap play again or skip. Keep speaking.

### Listen control
- **D-16:** Play / pause lives in the reader top bar, not only in the overflow menu.
- **D-17:** When listening and the user is not on that article, show a slim in-app mini player (title + play/pause/skip). Tapping the title opens that article.
- **D-18:** Speed cycle is on the in-app player as well as in Settings (same presets, same setting key).
- **D-19:** While speaking, volume keys change volume. They do not scroll the article and they do not skip paragraphs.

### Claude's Discretion
- How the spoken-paragraph mark looks versus NIP-84 highlights and find marks (must stay distinct).
- Mini-player placement (bottom bar vs other chrome) as long as it is slim and always reachable while speaking.
- Whether TTS settings is a new Settings category or a section under Reading / Media. Shape must match the webapp section.
- Follow-along setting key name; persist in `UserSettings` / NIP-78 if a new key is added.
- Mapping webapp 3x speed onto Android `setSpeechRate` (engines often clamp).
- How to chunk the article into paragraphs for skip and follow-along.
- How to strip markdown / images / code so spoken text is clean.
- Language auto-detect implementation (webapp uses `tinyld`).
- Standard Android audio focus: pause for calls / other apps, resume when appropriate.
- Foreground service + `MediaSession` wiring. No second Activity.
- Exact top-bar play icon and notification metadata (title, author if known).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product
- `.planning/PROJECT.md` — reading first, no nsec, offline-friendly
- `.planning/REQUIREMENTS.md` — READ-01 must not regress; TTS-01 is this phase
- `.planning/ROADMAP.md` — Phase 4
- `.planning/codebase/CONVENTIONS.md` — Kotlin/Compose style, no DI
- `.planning/codebase/ARCHITECTURE.md` — ReaderViewModel owns reader state, no second Activity
- `.planning/codebase/STACK.md` — minSdk 26, no Media3 today

### Android reader
- `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt` — `body` is markdown or stripped HTML
- `app/src/main/java/org/dergigi/boris/data/UserSettings.kt` — TTS keys already in `DEFAULT_JSON`, no typed accessors yet
- `app/src/main/java/org/dergigi/boris/data/MarkdownInline.kt` — flatten / plain text
- `app/src/main/java/org/dergigi/boris/data/ReadingPositionStore.kt` — saved scroll fraction
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` — article load / UI state
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` — top bar + overflow
- `app/src/main/java/org/dergigi/boris/ui/reader/HighlightJump.kt` — scroll to a stop
- `app/src/main/java/org/dergigi/boris/ui/reader/ArticleFind.kt` — transient find highlight
- `app/src/main/java/org/dergigi/boris/ui/reader/HighlightMarks.kt` — painted spans
- `app/src/main/java/org/dergigi/boris/ui/reader/VolumeKeys.kt` — volume-button scroll
- `app/src/main/java/org/dergigi/boris/ui/settings/SettingsScreen.kt` — settings categories
- `app/src/main/java/org/dergigi/boris/ui/settings/SettingsViewModel.kt` — NIP-78 publish/fetch

### Webapp (match this, do not invent a second settings contract)
- `/Users/gigi/Development/vibe/boris/src/components/Settings/TTSSettings.tsx` — section UI, speed cycle, language dropdown, preview sentence
- `/Users/gigi/Development/vibe/boris/src/components/TTSControls.tsx` — play/pause + speed cycle, language resolution order
- `/Users/gigi/Development/vibe/boris/src/hooks/useTextToSpeech.ts` — web SpeechSynthesis
- `/Users/gigi/Development/vibe/boris/src/services/settingsService.ts` — `ttsDefaultSpeed`, `ttsLanguageMode`, identifier `com.dergigi.boris.user-settings`

### Platform
- Android `TextToSpeech` / `UtteranceProgressListener` — on-device speech
- Android `MediaSession` + foreground service media playback — background / lock screen

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ReadableContent.body` + `MarkdownInline.plain`: starting point for spoken text after block-level cleanup.
- `UserSettings` already ships the four TTS keys in `DEFAULT_JSON` and syncs the whole JSON via NIP-78. Add typed accessors; do not invent new key names for speed/language.
- `ReadingPositionStore.fraction(url)`: map to a paragraph index for D-14.
- `HighlightJump` + `ArticleFind` painted spans: follow-along can be a transient mark, not a NIP-84 highlight.
- `VolumeKeys.Handle`: disable while speaking so volume keys reach the system (D-19).
- `SettingsViewModel`: same publish path when speed/language/follow-along change.

### Established Patterns
- ViewModel `StateFlow` + `*Content` composable. TTS playback state should not live only inside a Composable.
- JVM JUnit 4 tests for pure helpers (spoken-text cleanup, paragraph chunking, language mode resolution, speed presets).
- No Hilt, no Room, no DataStore, no new Activity.
- SharedPreferences / existing stores for local bits; settings blob stays NIP-78.

### Integration Points
- Reader top bar: play/pause (D-16). Overflow unchanged.
- `BorisApp` / shell: mini player when a session is active and the current route is not that article (D-17).
- Settings: new Text-to-Speech section matching the webapp.
- `AndroidManifest`: foreground service + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+).
- `VolumeKeys` in the reader: off while speaking.

</code_context>

<specifics>
## Specific Ideas

- Webapp TTS settings screenshot referenced during discuss: speed, speaker language, preview paragraph with play.
- Preview copy must match the webapp string in `TTSSettings.tsx` exactly.
- Language resolution order from webapp `TTSControls.tsx`: specific locale > content detect > system language.
- User wants this to feel like a podcast while staying a reader: browse freely, one article speaking, notification skip by paragraph.

</specifics>

<deferred>
## Deferred Ideas

- Cloud / neural TTS voices
- Playlist / queue of articles
- Sleep timer
- Download spoken audio files
- Syncing playback position itself over Nostr (reading-position sync already exists as scroll; spoken-offset sync is out of this phase)

None of these were requested as this phase.

</deferred>

---

*Phase: 4-Listen to articles*
*Context gathered: 2026-08-18*
